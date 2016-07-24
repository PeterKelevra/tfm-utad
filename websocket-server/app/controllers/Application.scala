package controllers

import java.util.Properties
import javax.inject.Inject

import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import kafka.consumer.Consumer
import kafka.consumer.ConsumerConfig
import kafka.consumer.ConsumerConnector
import kafka.consumer.Whitelist
import kafka.serializer.StringDecoder
import play.api.data.Form
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws._
import play.api.mvc._


import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(ws: WSClient, configuration: play.api.Configuration) extends Controller {

  val KAFKA_CONSUMER = "kafka.consumer"

  val ALERTS_TOPIC = "alerts_topic"

  val TRENDING_TOPS_TOPIC = "trending_tops_topic"

  val RESPONSES_LIMIT = "MESSAGES_PER_MINUTE"

  val redis = new RedisClient(configuration.underlying.getString("redis.host"), configuration.underlying.getInt("redis.port"))

  // http endpoint to check that the server is running
  def index = Action {
    Ok("I'm alive!\n")
  }

  /**
    * Devolvemos el valor actual configurado para el número de respuestas por minuto
    *
    */
  def getResponsesLimit = Action {
    Ok(redis.get(RESPONSES_LIMIT).get)
  }

  /**
    * Fija en redis el valor del limite de respuestas por minuto para el que enviamos notificaciones
    *
    * @return
    */
  def setResponsesLimit(limit: Long) = Action {
    println(s"new limit $limit")
    redis.set(RESPONSES_LIMIT, limit)
    Ok(redis.get(RESPONSES_LIMIT).get)
  }

  def createConsumerConfig = {
    new ConsumerConfig(ConfigFactory.load().getConfig(KAFKA_CONSUMER).entrySet().foldRight(new Properties) {
      (item, props) =>
        props.setProperty(item.getKey, item.getValue.unwrapped().toString)
        props
    })
  }

  private def consume(topic: String, connection: ConsumerConnector) =
    connection.createMessageStreamsByFilter(new Whitelist(topic), 1, new StringDecoder, new StringDecoder).headOption.map(_.toStream)

  /**
    * Se conecta al topic kafka mediante el cual recibimos los eventos que están recibiendo muchas respuestas por minuto
    *
    * @return
    */
  def eventAlerts() = WebSocket.using[String] { _ =>

    val connection = Consumer.create(createConsumerConfig)

    val endOnDisconnection = closeConnection(connection)

    val pipeFromKafka = consume(ALERTS_TOPIC, connection)
      .map(Enumerator.unfold(_) { s => Some(s.tail, s.head.message()) })
      .getOrElse(Enumerator.eof[String])

    endOnDisconnection -> pipeFromKafka

  }


  /**
    * Se conecta al topic kafka mediante el cual recibimos los trendingTops (eventos con más respuestas) por país
    *
    * @return
    */
  def trendingTops() = WebSocket.using[String] { _ =>

    val connection = Consumer.create(createConsumerConfig)

    val endOnDisconnection = closeConnection(connection)

    val pipeFromKafka = consume(TRENDING_TOPS_TOPIC, connection)
      .map(Enumerator.unfold(_) { s => Some(s.tail, s.head.message()) })
      .getOrElse(Enumerator.eof[String])

    endOnDisconnection -> pipeFromKafka

  }

  def closeConnection(connection: ConsumerConnector) = {
    Iteratee.foreach[String](println).map { _ =>
      connection.shutdown()
    }
  }

}
