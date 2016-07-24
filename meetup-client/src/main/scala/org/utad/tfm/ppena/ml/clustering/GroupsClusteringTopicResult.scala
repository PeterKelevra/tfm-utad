package org.utad.tfm.ppena.ml.clustering

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions.explode
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.elasticsearch.spark._
import org.joda.time.LocalDate
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Util._

import scala.collection.mutable

/**
  * Clase utilizada para exportar los topics obtenidos en el cluster
  */
object GroupsClusteringTopicResult {

  def main(args: Array[String]) {

    val conf = initConf

    conf.setAppName(getClass.getSimpleName)

    conf.set(ES_READ_FIELD_AS_ARRAY_INCLUDE, "topics")

    val sparkContext = new SparkContext(conf)

    val sql = new SQLContext(sparkContext)

    import sql.implicits._
    
    val df = sql.read.format("es").load(ES_CLUSTER_GROUPS_INDEX)
    val groupTopics = df.select("topics", "clusterId")
    groupTopics.printSchema()

    val topicsDic = groupTopics.withColumn("topics", explode($"topics")).select("topics", "clusterId").coalesce(1)
      .write.format("com.databricks.spark.csv").option("header", "true").save("cluster-topics")

  }
}
