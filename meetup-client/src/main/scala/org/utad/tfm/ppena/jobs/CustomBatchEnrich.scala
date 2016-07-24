package org.utad.tfm.ppena.jobs

import java.util.Locale

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.HitAs
import com.sksamuel.elastic4s.RichSearchHit
import org.joda.time.DateTimeConstants
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.Seconds
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.ElasticSearchClusterConfig
import org.utad.tfm.ppena.core.Event
import org.utad.tfm.ppena.core.Group
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.elastic.ElasticSearchManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
  * Created by Peter on 14/7/16.
  */
object CustomBatchEnrich extends App {


  val esClient = new ElasticSearchManager(new ElasticSearchClusterConfig("production", "elasticsearch://sandbox:9300"))

  //val esClient = new ElasticSearchManager(new ElasticSearchClusterConfig("elasticsearch", "elasticsearch://localhost:9300"))


  implicit object RsvpHitAs extends HitAs[Rsvp] {
    override def as(hit: RichSearchHit): Rsvp = mapToObject(hit.sourceAsMap, classOf[Rsvp])
  }

  case class RsvpAndGroup(rsvp: Rsvp, group: Group)

  val PAGE_SIZE = 604
  var count = 0
  var countDeleted = 0
  var page = 0
  //Math.abs(Random.nextInt())
  var empty = false
  val init = new LocalDateTime()
  println(page)
  while (!empty) {
    val resp = esClient.client.execute {
      search in ES_RSVPS_INDEX query "_missing_:week_day" from (page % 10000) size 1
    }.await

    empty = resp.isEmpty
    page += 1

    val rsvps: Seq[Rsvp] = resp.as[Rsvp]
    rsvps.foreach(rsvp => {
      try {
        val event = esClient.getEvent(rsvp.event.event_id)
        if (event.isEmpty) {
          val event = esClient.retrieveAndIndexEvent(rsvp.group.group_urlname, rsvp.event.event_id, None)
          if (event.isDefined) {
            //TODO: A veces el meetup api devuelve un id diferente de evento del que has preguntado?!?!?!. Pero en realidad la info es la misma
            event.get.id = rsvp.event.event_id
            updateAllEventRsvps(event.get)
          } else {
            deleteAllEventRsvps(rsvp.event.event_id)
          }
        } else {
          updateAllEventRsvps(event.get)
        }
      } catch {
        case e: Exception => {
          println("Iteration error", e.getMessage, e.printStackTrace())
          esClient.client.execute {
            delete id rsvp.rsvp_id from ES_RSVPS_INDEX
          }.await
        }
      }
    })
  }

  def updateAllEventRsvps(event: Event) = {
    val resp = esClient.client.execute {
      search in ES_RSVPS_INDEX query s"event.event_id:${event.id}" size PAGE_SIZE
    }.await


    val rsvps = resp.as[Rsvp]
    val updateFields = rsvps.par.map(rsvp => {
      val localDate = new LocalDateTime(rsvp.mtime, DateTimeZone.forID(event.group.timezone))
      (rsvp.rsvp_id, event.group.members, event.group.localized_country_name, localDate.toDateTime.getMillis, localDate.getHourOfDay,
        localDate.dayOfWeek().getAsText(Locale.ENGLISH).capitalize)
    }).toArray

    count += updateFields.length

    println("Rspvs to update", updateFields.length, "totalUpdated", count,
      Seconds.secondsBetween(init, new LocalDateTime()).getSeconds / (DateTimeConstants.SECONDS_PER_MINUTE * 1.0))
    esClient.client.execute {
      bulk(
        updateFields.map {
          f => update(f._1).in(ES_RSVPS_INDEX).doc(
            "group_members" -> f._2,
            "localized_country_name" -> f._3,
            "local_date" -> f._4,
            "local_hour" -> f._5,
            "week_day" -> f._6
          )
        }
      )
    }
  }

  def deleteAllEventRsvps(eventId: String) = {
    Future {
      val resp = esClient.client.execute {
        search in ES_RSVPS_INDEX query s"event.event_id:$eventId" size PAGE_SIZE
      }.await

      val rsvps: Seq[Rsvp] = resp.as[Rsvp]
      countDeleted += rsvps.size
      println("Rspvs to delete", rsvps.length, "totalDeleted", countDeleted)
      esClient.client.execute {
        bulk(
          rsvps.map {
            rsvp => delete id rsvp.rsvp_id from ES_RSVPS_INDEX
          }
        )
      }
    }
  }

}
