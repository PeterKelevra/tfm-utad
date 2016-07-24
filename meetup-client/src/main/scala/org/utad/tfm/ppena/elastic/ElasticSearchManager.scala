package org.utad.tfm.ppena.elastic

import java.util.Locale

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import org.elasticsearch.common.settings.Settings
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.ElasticSearchClusterConfig
import org.utad.tfm.ppena.core.Event
import org.utad.tfm.ppena.core.Group
import org.utad.tfm.ppena.core.Location
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.meetup.MeetupApi

/**
  * Clase para manejar operaciones contra elasticsearch
  */
@SerialVersionUID(1L)
class ElasticSearchManager(config: ElasticSearchClusterConfig) extends Serializable {

  val settings = Settings.settingsBuilder().put("cluster.name", config.name).build()
  val client = ElasticClient.transport(settings, ElasticsearchClientUri(config.uri))

  /**
    * Realiza el proceso de "enriquecimiento" de un rsvp. Añadiendo el local date y hour en función del timeZone del grupo
    * Así como el country name localizado.
    *
    * @param rsvp  Respuesta meetup a enriquecer
    * @param close Indica si cerramos la conexión tras la operación
    *
    */
  def enrichRsvps(rsvp: Rsvp, close: Boolean = true) = {
    try {
      var group = getGroup(rsvp.group.group_id)
      if (!group.isDefined) {
        group = retriveAndIndexGroup(rsvp.group.group_urlname)
      }

      //Recuperamos tmb el evento
      if (!getEvent(rsvp.event.event_id).isDefined) {
        retrieveAndIndexEvent(rsvp.group.group_urlname, rsvp.event.event_id, group)
      }

      if (group.isDefined) {
        val localDate = new LocalDateTime(rsvp.mtime, DateTimeZone.forID(group.get.timezone))
        client.execute {
          update(rsvp.rsvp_id).in(ES_RSVPS_INDEX).doc(
            "group_members" -> group.get.members,
            "localized_country_name" -> group.get.localized_country_name,
            "local_date" -> localDate.toDateTime.getMillis,
            "local_hour" -> localDate.getHourOfDay,
            "week_day" -> localDate.dayOfWeek().getAsText(Locale.ENGLISH).capitalize
          )
        }
      }
    } catch {
      case e: Exception => println("Error enrich rsvp", rsvp, e.getMessage)
    }
    closeClient(close)
  }


  def enrichEventDate(event: Event) = {
    val localDate = new LocalDateTime(event.time + event.utc_offset)
    event.local_hour = localDate.getHourOfDay
    event.local_time = localDate.toString("HH:mm")
    event.week_day = localDate.dayOfWeek().getAsText(Locale.ENGLISH).capitalize
    event.month = localDate.monthOfYear().getAsText(Locale.ENGLISH).capitalize
  }

  /**
    * Obtiene información de un grupo de meetup y lo indexa en elasticsearch
    *
    * @param groupUrlName url name del grupo
    * @return
    */
  def retriveAndIndexGroup(groupUrlName: String) = {
    var group: Option[Group] = None
    val jsonGroup = MeetupApi.getGroupInfo(groupUrlName)
    if (jsonGroup.isDefined) {
      group = Some(Util.jsonToObject(jsonGroup.get, classOf[Group]))
      group.get.group_location = new Location(group.get.lat, group.get.lon)
      indexGroup(group.get)
    }
    group
  }

  /**
    * Obtiene información del evento de meetup lo enriquece y lo indexa en elasticsearch
    *
    * @param groupUrlName Nombre del grupo del evento
    * @param eventId      Id del evento
    */
  def retrieveAndIndexEvent(groupUrlName: String, eventId: String, group: Option[Group]): Option[Event] = {
    var eventResponse: Option[Event] = None
    val info = MeetupApi.getEventInfo(groupUrlName, eventId)
    if (info.isDefined) {
      try {
        val event = Util.jsonToObject(info.get, classOf[Event])
        enrichEventDate(event)

        //Enriquecemos la información del grupo del evento con la que tenemos en elastic
        event.group = if (group.isDefined) group.get else retriveAndIndexGroup(groupUrlName).get
        indexEvent(event)
        eventResponse = Some(event)
      } catch {
        case e: Exception =>
          println(s"Error enrich event : $eventId", e.getMessage)
      }
    }
    eventResponse
  }


  /**
    * Obtiene todos los eventos del grupo que recibimos por parámetro y los indexa
    *
    * @param group
    */
  def indexGroupEvents(group: Group, version: Int, close: Boolean = true) = {
    try {
      val jsonGroupEvents = MeetupApi.getGroupPastEventsInfo(group.urlname)
      if (jsonGroupEvents.isDefined) {
        val events = Util.jsonToObject(jsonGroupEvents.get, classOf[Array[Event]]).filter(_.id != null)
        if (events.size > 0) {
          client.execute {
            bulk(
              events.map { event =>
                enrichEventDate(event)
                event.group = group
                index into ES_EVENTS_INDEX id event.id source (Util.objectToJson(event))
              }
            )
          }.await
        }
      }
      updateGroup(group.id, "events_extract" -> version)
    } catch {
      case e: Exception => println(s"Error getting group events", e.getMessage, e.printStackTrace())
    } finally {
      closeClient(close)
    }
  }

  /**
    * Indexa en elasticsearch la respuesta recibido por parámetro
    *
    * @param rsvp
    * @return
    */
  def indexRsvp(rsvp: Rsvp) = {
    client.execute {
      index into ES_RSVPS_INDEX id rsvp.rsvp_id source Util.objectToJson(rsvp)
    }.await
  }

  /**
    * Indexa en elasticsearch el evento recibido por parámetro
    *
    * @param event
    * @return
    */
  def indexEvent(event: Event) = {
    client.execute {
      index into ES_EVENTS_INDEX id event.id source Util.objectToJson(event)
    }.await
  }

  /**
    * Indexa en elasticsearch el grupo recibidopor parámetro
    *
    * @param group
    * @return
    */
  def indexGroup(group: Group) = {
    client.execute {
      index into ES_GROUPS_INDEX id group.id source Util.objectToJson(group)
    }.await
  }

  /**
    * Busca en elasticsearch la respuesta con el id recibido por parámetro
    *
    * @param id
    */
  def getRsvp(id: Int): Option[Rsvp] = {
    val res = client.execute {
      get id id from ES_RSVPS_INDEX
    }.await
    if (res.isExists) Some(mapToObject(res.source, classOf[Rsvp])) else None
  }

  /**
    * Busca en elasticsearch el evento con el id recibido por parámetro
    *
    * @param id
    * @return
    */
  def getGroup(id: Int): Option[Group] = {
    val res = client.execute {
      get id id from ES_GROUPS_INDEX
    }.await
    if (res.isExists) Some(mapToObject(res.source, classOf[Group])) else None
  }

  /**
    * Busca en elasticsearch el evento con el id recibido por parámetro
    *
    * @param id
    * @return
    */
  def getEvent(id: String): Option[Event] = {
    val res = client.execute {
      get id id from ES_EVENTS_INDEX
    }.await
    if (res.isExists) Some(mapToObject(res.source, classOf[Event])) else None
  }

  /**
    * Actualiza los campos recibidos por parámetro del groupo con el id indicado
    *
    * @param id     Id del Grupo
    * @param fields Campos a actualizar
    * @return
    */
  def updateGroup(id: Int, fields: (String, Any)*) = {
    client.execute {
      update(id).in(ES_GROUPS_INDEX).doc(fields)
    }.await
  }

  /**
    * Actualiza los campos recibidos por parámetro del evento con el id indicado
    *
    * @param id     Id del Evento
    * @param fields Campos a actualizar
    * @return
    */
  def updateEvent(id: String, fields: (String, Any)*) = {
    client.execute {
      update(id).in(ES_EVENTS_INDEX).doc(fields)
    }.await
  }

  /**
    * Cierra la conexión con elasticsearch
    */
  def closeClient(close: Boolean = true) = {
    client.close()
  }

}
