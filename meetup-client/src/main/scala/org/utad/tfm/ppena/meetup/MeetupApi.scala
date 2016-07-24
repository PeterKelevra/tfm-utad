package org.utad.tfm.ppena.meetup

import java.net.HttpURLConnection
import java.net.URL

import scala.util.Random

/**
  */
object MeetupApi {

  val URL = "https://api.meetup.com/"

  val KEYS = List("611c6d7814b6146210596a62465b", "1ce4343f6416701d67311a1e2f9", "263b4623672070651814464574363f48",
    "3797171563c4a1670225b4a4b2731e", "91f5ea3b4836d9197e6a787b5566",
    "473287245691b603b573d4f1c312", "623f1b9212530172420424540783912")

  val MEETUP_KEY = "?sig=true&key="

  val random = Random

  /**
    * Obtiene información de un grupo mediante el api de meetup.
    * Ejemplo petición https://api.meetup.com/SoCalALN?sig=true&key=611c6d7814b6146210596a62465b
    */
  def getGroupInfo(groupUrlName: String) = {
    var info: Option[String] = None
    try {
      val content = get(s"$URL$groupUrlName$MEETUP_KEY$getKey")
      info = Some(content)
    } catch {
      case e: Exception => println(s"Error getting group info: $groupUrlName", e.getMessage)
    }
    info
  }

  /**
    * Obtiene la información de un evento dado el url name del group y el id del evento
    * Ej: https://api.meetup.com/chicagofriendsofgermanculture/events/232229002?sig=true&key=611c6d7814b6146210596a62465b
    * Hay casos raros!?! p.e esta petición. Devuelve un id de evento distinto del que preguntamos?!?!
    * https://api.meetup.com/Soul-Inspiration-Group-Healers/events/232444351?sig=true&key=611c6d7814b6146210596a62465b
    *
    * @param groupUrlName
    * @param eventId
    */
  def getEventInfo(groupUrlName: String, eventId: String) = {
    var info: Option[String] = None
    try {
      val content = get(s"$URL$groupUrlName/events/$eventId$MEETUP_KEY$getKey")
      info = Some(content)
    } catch {
      case e: Exception => println(s"Error getting event info: $groupUrlName::$eventId", e.getMessage)
    }
    info
  }

  /**
    * Permite obtener todos los eventos de un grupo
    *
    * @param urlName
    * Ej: https://api.meetup.com/Clube-Poliglota-Rio-de-Janeiro/events?sig=true&key=611c6d7814b6146210596a62465b
    */
  def getGroupPastEventsInfo(urlName: String) = {
    var info: Option[String] = None
    try {
      val content = get(s"$URL$urlName/events$MEETUP_KEY$getKey&status=past")
      info = Some(content)
    } catch {
      case e: Exception => println(s"Error getting group events info: $urlName", e.getMessage)
    }
    info

  }

  def getKey() = {
    val keyIndex = random.nextInt(KEYS.size)
    KEYS(keyIndex)
  }

  @throws(classOf[java.io.IOException])
  @throws(classOf[java.net.SocketTimeoutException])
  def get(url: String, connectTimeout: Int = 5000, readTimeout: Int = 5000, requestMethod: String = "GET") = {
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = scala.io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }
}
