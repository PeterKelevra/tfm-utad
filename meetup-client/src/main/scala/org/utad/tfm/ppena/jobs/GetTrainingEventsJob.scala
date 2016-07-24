package org.utad.tfm.ppena.jobs

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.elasticsearch.spark._
import org.joda.time.LocalDate
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util._

/**
  * @deprecated
  * Clase utilizada para obtener los eventos a utilizar para los análisis con mllib
  *
  * Esta clase simplemente fue creada para poder exportar datos de elasticsearch a ficheros csv, para poder hacer pruebas
  * sin necesitar un elasticsearch
  */
object GetTrainingEventsJob {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    conf.set(ES_READ_FIELD_EXCLUDE, "group.group_topics")
    val sparkContext = new SparkContext(conf)

    val sqlContext = new SQLContext(sparkContext)

    exportFinishedEvents

    //getNumCountries
    //getNumCities

    /**
      * Obtiene los datos de los eventos finalizados
      */
    def exportFinishedEvents = {
      val fromDate = new LocalDate(2015, 1, 1).toString(DATE_FORMAT)
      val toDate = new LocalDate(2016, 1, 1).toString(DATE_FORMAT)


      val options = Map(ES_QUERY -> s"""{"query": {"range" : {"time" : {"gte": "$fromDate","lt": "$toDate","format": "$DATE_FORMAT"}}}}""")

      sqlContext.read.format("es").options(options).load(ES_EVENTS_INDEX)

        //.where(s"time >= '${fromDate}' and time < '${toDate}'")

        //        .select("yes_rsvp_count", "week_day", "local_time", "local_hour", "visibility", "waitlist_count", "group.id", "group.urlname", "group.city",
        //          "group.localized_country_name", "group.members", "group.who", "group.timezone", "group.category.shortname")

        .selectExpr("yes_rsvp_count", "month(time)", "week_day", "local_hour", "group.city", "group.country", "group.members", "group.category.id")
        .coalesce(1)
        .write
        .format("com.databricks.spark.csv")
        .option("header", "false")
        //.option("quoteMode", "NON_NUMERIC")
        //.option("delimiter", " ")
        .save(s"events_${fromDate}_${toDate}.csv")
    }

    def getNumCountries = {
      val num = sqlContext.read.format("es").load(ES_EVENTS_INDEX)
        .select("group.country").distinct().count()
      println(s"Countries $num")
    }

    def getNumCities = {
      val num = sqlContext.read.format("es").load(ES_EVENTS_INDEX)
        .select("group.city").distinct().count()
      println(s"Cities $num")
    }

    def retrieveEvents = {

      sqlContext.read.format("es").load(ES_RSVPS_INDEX)
        //.where("group.group_country = 'es'")
        .select("event.event_id", "group.group_urlname").distinct()
        .foreach(row => {
          println(row)
        })

      //    sqlContext.sql(s"""CREATE TEMPORARY TABLE responses USING org.elasticsearch.spark.sql OPTIONS (resource '$ES_RSVPS_INDEX')""")
      //    sqlContext.sql("select distinct event.event_id, group.group_urlname from responses where group.group_country = 'es'")
      //      .foreach(row => {
      //        println(row)
      //      })

    }
  }

}
