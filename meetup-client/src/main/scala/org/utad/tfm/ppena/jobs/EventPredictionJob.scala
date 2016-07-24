package org.utad.tfm.ppena.jobs

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.elasticsearch.spark._
import org.joda.time.LocalDate
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Event
import org.utad.tfm.ppena.core.Util._

import scala.io.Source

/**
  * Se encarga de realizar el proceso de predición de confirmaciones que recibiran los eventos.
  * Entre las fechas recibidas por parámetro.
  * En caso de no indicar fechas, consultará todos los eventos del día actual y el anterior
  */
object EventPredictionJob {

  def main(args: Array[String]) {


    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    //Cargamos los mapas de categorización de paises, ciudades y día de la semana
    val weekDaysMap = retrieveMap("weekDays")
    val countriesMap = retrieveMap("countries")
    val citiesMap = retrieveMap("cities")

    val sc = new SparkContext(conf)

    //Cargamos el modelo de predicción
    val model = RandomForestModel.load(sc, "random-forest-model")

    //Recuperamos los eventos de las fechas indicadas
    val (fromDate, toDate) = if (args.length > 1) (args(0), args(1))
    else (new LocalDate().minusDays(1).toString(DATE_FORMAT), new LocalDate().toString(DATE_FORMAT))

    println(s"Prediction from Events between: $fromDate - $toDate")

    val events = sc.esJsonRDD(ES_EVENTS_INDEX,s"""{"query": {"range" : {"time" : {"gte": "$fromDate","lte": "$toDate","format": "$DATE_FORMAT"}}}}""")

    //Query mas estricta filtrando los que ya están predecidos, el problema es si queremos volver a predecir...
    //s"""{"query": {"bool":{"must": [{"range" : {"time" : {"gte": "$fromDate","lte": "$toDate","format": "$DATE_FORMAT"}}}],"must_not": [{"exists": {"field": "prediction_yes"}}]}}}"""


    events.map(r => {
      val event = jsonToObject(r._2, classOf[Event])
      val weekDay = weekDaysMap.getOrElse(event.week_day, 0)
      val city = citiesMap.getOrElse(event.group.city, 0)
      val country = countriesMap.getOrElse(event.group.country, 0)
      val category = if (event.group.category != null) event.group.category.id else 0
      val vector = Vectors.dense(new LocalDate(event.time).getMonthOfYear, weekDay, event.local_hour.toInt, city,
        country, event.group.members, category)

      event.prediction_yes = model.predict(vector).toInt

      objectToJson(event)

    }).saveJsonToEs(ES_EVENTS_INDEX, Map(ES_MAPPING_ID -> "id"))


  }

  /**
    * Lee el contenido del fichero recibido por parámetro y lo guarda en un mapa
    *
    * @param fileName
    */
  def retrieveMap(fileName: String): Map[String, Int] = {
    Source.fromFile(s"maps/$fileName").getLines.map {
      l =>
        val values = l.split(',')
        (values(0), values(1).toInt)
    }.toMap
  }

}
