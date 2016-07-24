package org.utad.tfm.ppena.jobs

import java.util
import java.util.UUID

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.streaming.Minutes
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.elasticsearch.spark._
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.EventCounter
import org.utad.tfm.ppena.core.Location
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.inputimport.RsvpsReceiver


/**
  * Proceso spark streaming encargaro de conectarse a la fuente de entrada para obtener las respuestas de Meetup.
  * Una vez recuperadas.
  * 1. Se almacenan en elasticsearch
  * 2. Se envian a un topic kafka para que se realize su enriquecimiento
  * 3. Se realizan los calculos mediante ventanas de tiempo, para detectar.
  * - 3.1. Eventos que reciben cierta cantidad de respuestas por minuto
  * - 3.2. Eventos "trending" (con mayor número de respuestas) agrupados por país
  */
object ProcessInputJob {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    val ssc = new StreamingContext(conf, Seconds(1))

    //Obtenemos las propiedades de configuración de redis
    val redisHost = ssc.sparkContext.broadcast(conf.get("spark.redis.host", MASTER_HOST))
    val redisPort = ssc.sparkContext.broadcast(conf.get("spark.redis.port", DEFAULT_REDIS_PORT).toInt)

    //Obtenemos el valor configurado para los kafka broker
    val kafkaBrokers = ssc.sparkContext.broadcast(conf.get("spark.metadata.broker.list", s"$MASTER_HOST:$KAFKA_PORT"))

    println(">>> StreamingContext created")
    val rsvpStream = ssc.receiverStream(new RsvpsReceiver)


    println(">>> useRDD")

    //Añadimos las coordenadas longitud, latitud del grupo como localización del evento, para visualizarlo posteriormente en kibana
    val rsvpStreamMap = rsvpStream.map(json => {
      val rsvp = objectMapper.readValue(json, classOf[Rsvp])
      rsvp.location = Location(rsvp.group.group_lat, rsvp.group.group_lon)
      rsvp
    })

    saveInElastic(rsvpStreamMap)

    retrieveEventAlerts(rsvpStreamMap)

    retrieveTrendingTops

    /**
      * Guarda las respuestas del stream en elasticSearch
      *
      * @param dStream
      */
    def saveInElastic(dStream: DStream[Rsvp]) = {
      //Guardando mediante la libreria de elasticsearch
      rsvpStreamMap.foreachRDD(rdd => {
        rdd.saveToEs(ES_RSVPS_INDEX, Map(ES_MAPPING_ID -> "rsvp_id"))
        //Enviamos a kafka los id's de las respuestas para enriquecerlas
        rdd.map(_.rsvp_id).foreach(sendToKafka)
      })
    }

    /**
      * Ventana para obtener las respuestas que se reciben para cada evento. Si el numero de respuestas supera el configurado
      * enviamos notificación mediante kafka
      *
      * @param dStream
      */
    def retrieveEventAlerts(dStream: DStream[Rsvp]) = {
      dStream.window(Minutes(1), Seconds(40)).foreachRDD(rdd => {
        rdd.map(rsvp => {
          (rsvp.event.event_id, new EventCounter(rsvp.event, 1))
        }).reduceByKey((e1, e2) => {
          new EventCounter(e1.event, e1.count + e2.count)
        }).filter(r => {
          val num = redisClient(redisHost.value, redisPort.value).get(MESSAGES_PER_MINUTE)
          val limit = if (num.isDefined) num.get.toInt else DEFAULT_MESSAGES_PER_MINUTE_LIMIT
          r._2.count > limit
        }).foreach(r => sendAlertToKafka(r._2))
      })
    }

    /**
      * Obtiene la lista de eventos con mas respuestas por país
      * y los envía al topic kafka preparado a tal efecto
      */
    def retrieveTrendingTops = {
      //Utilzamos HiveContext para poder usar las window function
      val sqlContext = new HiveContext(ssc.sparkContext)

      rsvpStream.window(Minutes(60), Minutes(1)).foreachRDD(rdd => {
        sqlContext.read.json(rdd).registerTempTable("rsvps")

        //Agrupamos por evento
        sqlContext.sql(
          """SELECT event.event_id as id, first(event.event_name) as name, first(event.time) as time,
             first(group.group_country) as country, count(rsvp_id) as responses
            FROM rsvps GROUP BY event.event_id""")
          .registerTempTable("rsvps_by_event")

        //Obtenemos las 10 eventos con mas respuestas por país
        val trendingTops = sqlContext.sql(
          s"""SELECT * FROM (
             SELECT *, row_number() OVER (PARTITION BY country ORDER BY responses DESC, name) as rank
             FROM rsvps_by_event where responses > 1) tmp WHERE rank <= ${TOP_TRENDING_EVENTS}""")
          .map(_.getValuesMap[Any](List("id", "name", "time", "country", "responses"))).collect()

        sendTrendingTopsToKafka(trendingTops)

      })

    }


    def buildKafkaStringProducer = {
      val props = new util.HashMap[String, Object]()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers.value)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
      new KafkaProducer[String, String](props)
    }

    /**
      * Envia el objeto recibido por parámetro en formato json al topic kafka de notificación de eventos con muchas respuestas.
      *
      * @param record
      */
    def sendAlertToKafka(record: EventCounter) = {
      println(s"Event: ${record.event.event_name} - Responses: ${record.count}")
      val message = new ProducerRecord[String, String](KAFKA_ALERT_TOPIC, record.event.event_id, objectToJson(record))
      val producer = buildKafkaStringProducer
      producer.send(message)
      producer.close()
    }

    /**
      * Envia a la cola kafka el id del rsvp a enriquecer
      *
      * @param id
      */
    def sendToKafka(id: Int) = {
      val message = new ProducerRecord[String, String](KAFKA_ENRICH_TOPIC, id.toString, id.toString)
      val producer = buildKafkaStringProducer
      producer.send(message)
      producer.close()
    }

    /**
      * Envia a la cola kafka la lista de eventos con mas respuestas por país
      *
      * @param trendingTops
      */
    def sendTrendingTopsToKafka(trendingTops: Any) = {
      val message = new ProducerRecord[String, String](KAFKA_TRENDING_TOPS_TOPIC, UUID.randomUUID().toString, objectToJson(trendingTops))
      val producer = buildKafkaStringProducer
      producer.send(message)
      producer.close()
    }


    /**
      * Proceso de streaming mediante data frames.
      * Finalmente se han utilizado RDD en formato json que posteriormente transformamos a objetos
      * para un manejo mas sencillo de la información
      */
    def retrieveEventResponsesWithDataFrames = {
      println(">>> useDataFrames")
      //Guardando mediante DataFrames
      val sqlContext = new SQLContext(ssc.sparkContext)

      rsvpStream.foreachRDD(rdd => {
        sqlContext.read.json(rdd)
          .write.format("org.elasticsearch.spark.sql")
          .mode(SaveMode.Append)
          .save(ES_RSVPS_INDEX)
      })

      rsvpStream.window(Minutes(1), Seconds(40)).foreachRDD(rdd => {
        val json = sqlContext.read.json(rdd)

        json.registerTempTable("rsvps")
        val wordCountsDataFrame =
          sqlContext.sql("select event.event_id, count(*) as total from rsvps group by event.event_id order by total desc")
        println(wordCountsDataFrame.show())
        val rsvpsCount = sqlContext.sql("select count(*) as total from rsvps")
        println(rsvpsCount.show())
      })
    }

    ssc.checkpoint("target/checkpoints")

    ssc.start
    ssc.awaitTermination()

  }
}
