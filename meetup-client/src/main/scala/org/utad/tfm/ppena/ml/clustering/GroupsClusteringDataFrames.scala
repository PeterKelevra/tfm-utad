package org.utad.tfm.ppena.ml.clustering

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions.explode
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.joda.time.LocalDate
import org.elasticsearch.spark._
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Util._

import scala.collection.mutable

/**
  * Clase utilizada para realizar el algoritmo de clustering kMeans sobre topics de grupos
  * haciendo uso de data frames.
  * El resultado de la "clasificación/clustring" de grupos es almacenado en un indice
  * de elasticsearch para su posterior consulta
  */
object GroupsClusteringDataFrames {

  def main(args: Array[String]) {

    val conf = initConf

    //Con esta configuración no obtiene los topics al hacer sparkContext.esRDD
    conf.set(ES_READ_FIELD_AS_ARRAY_INCLUDE, "group.group_topics")

    conf.setAppName(getClass.getSimpleName)

    val sparkContext = new SparkContext(conf)

    val (fromDate, toDate) = if (args.length > 1) (args(0), args(1))
    else (new LocalDate("2016-07-01").toString(DATE_FORMAT), new LocalDate("2016-07-02").toString(DATE_FORMAT))

    val sql = new SQLContext(sparkContext)

    import sql.implicits._

    val options = Map(ES_QUERY -> s"""{"query": {"range" : {"mtime" : {"gte": "$fromDate","lte": "$toDate","format": "$DATE_FORMAT"}}}}""")

    val df = sql.read.format("es").options(options).load(ES_RSVPS_INDEX)
    //df.printSchema()
    val groupTopics = df.select("group.group_id", "group.group_name", "group.group_topics.urlkey").distinct()
    //groupTopics.printSchema()

    //Generamos el diccionario de topics
    val topicsDic = groupTopics.withColumn("urlkey", explode($"urlkey")).select("urlkey").distinct().map(_.get(0)).zipWithIndex.collectAsMap()
    //topicsDic.take(10).foreach(println)
    println("Total topics", topicsDic.size)

    //Generamos un vector para cada grupo a partir de sus topics
    val groupVectors = groupTopics.map(r => {
      val groupId = r.getLong(0)
      val groupName = r.getString(1)
      val groupTopics = r.get(2).asInstanceOf[mutable.WrappedArray[String]]
      val index = groupTopics.map(topicsDic.get(_).get.toInt)
      ((groupId, groupName, groupTopics), Vectors.sparse(topicsDic.size, index.toArray, Array.fill(index.length)(1.0)))
    })


    val numClusters = 10
    val numIterations = 100
    val values = groupVectors.values.cache()
    val model = KMeans.train(values, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = model.computeCost(values)
    println("Within Set Sum of Squared Errors = " + WSSSE)
    val groupsCluster = model.predict(values)

    groupVectors.keys.zip(groupsCluster).map(record => {
      objectToJson(new GroupInfo(record._1._1, record._1._2, record._1._3, record._2._1, record._2._2))
    })
      .saveJsonToEs(ES_CLUSTER_GROUPS_INDEX, Map(ES_MAPPING_ID -> "id"))
    //.foreach(println)

  }

  case class GroupInfo(id: Long, name: String, topics: mutable.WrappedArray[String], clusterId: Int, distance: Double)

}
