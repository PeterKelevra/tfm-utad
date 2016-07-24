package org.utad.tfm.ppena.jobs

import org.apache.spark.SparkContext
import org.elasticsearch.spark._
import org.joda.time.LocalDate
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Event
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.elastic.ElasticSearchManager
import org.utad.tfm.ppena.meetup.MeetupApi

/**
  * Proceso encargado de obtener el numero final de respuestas de los eventos que han finalizado, el día anterior
  * Este proceso se lanzará como proceso cron, todos los días
  */
object RetrieveFinalizedEventResponses {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    val sc = new SparkContext(conf)
    val clusterConfig = sc.broadcast(getElasticSearchClusterConfig(conf))

    //Recuperamos sólo los eventos del día anterior
    //val fromDate = new LocalDate().minusDays(1).toString(DATE_FORMAT)
    val fromDate = new LocalDate().minusDays(10).toString(DATE_FORMAT)
    val toDate = new LocalDate().toString(DATE_FORMAT)

    //"?q=enriched:false"
    val events = sc.esJsonRDD(ES_EVENTS_INDEX,
      s"""{
              "query": {
                  "range" : {
                      "time" : {
                          "gte": "$fromDate",
                          "lt": "$toDate",
                          "format": "$DATE_FORMAT"
                      }
                  }
              }
          }""")

    events.foreach(k => {
      val esClient = new ElasticSearchManager(clusterConfig.value)
      try {
        val event = jsonToObject(k._2, classOf[Event])
        val eventInfo = MeetupApi.getEventInfo(event.group.urlname, event.id)
        if (eventInfo.isDefined) {
          val updatedEvent = jsonToObject(eventInfo.get, classOf[Event])
          if (event.yes_rsvp_count != updatedEvent.yes_rsvp_count) {
            println("yes responses. Current/Update", event.yes_rsvp_count, updatedEvent.yes_rsvp_count)
            esClient.updateEvent(event.id, {
              "yes_rsvp_count" -> updatedEvent.yes_rsvp_count
            })
          }
        }
      } catch {
        case e: Exception => println("Process Error", e.getMessage)
      } finally {
        esClient.closeClient()
      }
    })
  }

}
