package org.utad.tfm.ppena.ml.prediction

import java.io.File
import java.io.PrintWriter

import com.google.common.primitives.Ints
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.utad.tfm.ppena.core.Constants._

/**
  *
  */
object PredictionUtil {


  case class Record(yes_rsvp_count: Int, month: Int, week_day: String, hour: Int, city: String, country: String, members: Int, category_id: Int)

  case class CategorizedMaps(weekDays: Map[String, Int], countries: Map[String, Int], cities: Map[String, Int])

  case class MlData(trainingData: RDD[LabeledPoint], testData: RDD[LabeledPoint])


  /**
    * Obtiene los datos de entrada de los eventos de elasticsearch entre las fechas indicada
    *
    * @param sparkContext
    * @param from
    * @param to
    * @return
    */
  def readEventFromElastic(sparkContext: SparkContext, from: LocalDate, to: LocalDate, yesGt: Int) = {

    val sqlContext = new SQLContext(sparkContext)

    val options = Map(ES_QUERY -> s"""{"query": {"range" : {"time" : {"gte": "${from.toString(DATE_FORMAT)}","lte": "${to.toString(DATE_FORMAT)}","format": "$DATE_FORMAT"}}}}""")

    val query = sqlContext.read.format("es").options(options).load(ES_EVENTS_INDEX)
      .where(s"yes_rsvp_count > $yesGt and group.category.id > 0")
      .selectExpr("yes_rsvp_count", "month(time)", "week_day", "local_hour", "group.city", "group.country", "group.members", "group.category.id")
    query.map(row => {
      new Record(row.getLong(0).toInt, row.getInt(1), row.getString(2), row.getLong(3).toInt, row.getString(4), row.getString(5),
        row.getLong(6).toInt, row.getLong(7).toInt)
    })
  }

  /**
    * Categoriza los registros de tipo String a valores enteros
    */
  def buildCategorizedMaps(records: RDD[Record]) = {
    val weekDayMap = buildFieldMap(records.map(record => record.week_day))
    val countryMap = buildFieldMap(records.map(record => record.country))
    val cityMap = buildFieldMap(records.map(record => record.city))

    writeMapToFile(weekDayMap, "weekDays")
    writeMapToFile(countryMap, "countries")
    writeMapToFile(cityMap, "cities")

    new CategorizedMaps(weekDayMap, countryMap, cityMap)
  }

  def writeMapToFile(map: Map[String, Int], fileName: String) = {
    val writer = new PrintWriter(new File(s"target/$fileName"))
    val lineSeparator = System.getProperty("line.separator")
    map.foreach(entry => {
      writer.write(s"${entry._1},${entry._2}$lineSeparator")
    })
    writer.close()
  }

  /**
    * Recibe una linea csv por parámetro y la parsea devolviendo un objeto {@link Record}
    *
    * @param line
    * @return
    */
  def parseCsvRecord(line: String): Record = {
    val values = line.split(",")
    new Record(values(0).toInt, values(1).toInt, values(2), values(3).toInt, values(5), values(6), values(7).toInt, Ints.tryParse(values(8)))
  }

  /**
    * Función de "categorización". Construye un mapa a partir de los campos recibidos por paraámetro, asignando a cada valor un número entero
    *
    * @param fields
    * @return
    */
  def buildFieldMap(fields: RDD[String]) = {
    fields.distinct().collect().zipWithIndex.toMap
  }

  def buildCategoricalFeauturesInfo(categorizedMaps: CategorizedMaps) = {
    var categoricalFeaturesInfo = Map[Int, Int]()
    categoricalFeaturesInfo += (1 -> categorizedMaps.weekDays.size)
    categoricalFeaturesInfo += (3 -> categorizedMaps.cities.size)
    categoricalFeaturesInfo += (4 -> categorizedMaps.countries.size)

    println("weekDays", categorizedMaps.weekDays)
    println("countries", categorizedMaps.countries)
    println("cities", categorizedMaps.cities)
    categoricalFeaturesInfo
  }

  /**
    * Making LabeledPoint con las features para entrenar el modelo
    *
    * @param records
    * @param categorizedMaps
    * @return
    */
  def makeMlData(records: RDD[Record], categorizedMaps: CategorizedMaps) = {
    val mldata = records.map(r => LabeledPoint(r.yes_rsvp_count,
      Vectors.dense(r.month, categorizedMaps.weekDays.get(r.week_day).get, r.hour, categorizedMaps.cities.get(r.city).get,
        categorizedMaps.countries.get(r.country).get, r.members, r.category_id))
    )

    // Split the data into training and test sets (30% held out for testing)
    val splits = mldata.randomSplit(Array(0.7, 0.3))
    new MlData(splits(0), splits(1))
  }

  /**
    * Guarda los valores de la predición en target con el prefijo indicado y la hora actual
    *
    * @param labelsAndPredictions
    */
  def saveResults(labelsAndPredictions: RDD[(Int, Int)], prefix: String) = {

    val time = new LocalDateTime().toString("yyyyMMdd_HHmmss")
    labelsAndPredictions.coalesce(1).saveAsTextFile(s"target/$prefix-predictions-${time}")
    val testMSE = labelsAndPredictions.map { case (v, p) => math.pow(v - p, 2) }.mean()
    println("Test Mean Squared Error = " + testMSE)

  }

}
