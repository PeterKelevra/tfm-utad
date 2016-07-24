package org.utad.tfm.ppena.jobs

import kafka.serializer.StringDecoder
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka.KafkaUtils
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.elastic.ElasticSearchManager

/**
  * Proceso encargado de enriquecer las respuestas cuyo id recibimos mediante kafka
  */
object EnrichJob {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    // Create context with 1 second batch interval
    val ssc = new StreamingContext(conf, Seconds(1))

    val clusterConfig = ssc.sparkContext.broadcast(getElasticSearchClusterConfig(conf))

    // Create direct kafka stream with brokers and topics
    val brokers = conf.get("spark.metadata.broker.list", s"$MASTER_HOST:$KAFKA_PORT")
    val topicsSet = Set(KAFKA_ENRICH_TOPIC)
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    messages.map(_._2).foreachRDD(rdd => {
      rdd.foreach(rsvp_id => {
        val esClient = new ElasticSearchManager(clusterConfig.value)
        val rsvp = esClient.getRsvp(Integer.getInteger(rsvp_id))
        if (rsvp.isDefined) {
          esClient.enrichRsvps(rsvp.get)
        } else {
          println(s"Error getting rspv_id: $rsvp_id in eslasticsearch")
        }
      })
    })

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }

}
