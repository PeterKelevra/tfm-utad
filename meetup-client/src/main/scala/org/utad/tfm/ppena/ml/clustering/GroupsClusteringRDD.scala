package org.utad.tfm.ppena.ml.clustering

import com.twitter.algebird.Operators._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.mllib.linalg.SparseVector
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.elasticsearch.spark._
import org.joda.time.LocalDate
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util._

/**
  * Clase utilizada para realizar el algoritmo de clustering kMeans sobre topics de grupos
  */
object GroupsClusteringRDD {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    conf.set(ES_READ_FIELD_EXCLUDE, "group.group_topics")

    val sparkContext = new SparkContext(conf)

    val (fromDate, toDate) = if (args.length > 1) (args(0), args(1))
    else (new LocalDate("2016-07-01").toString(DATE_FORMAT), new LocalDate("2016-07-02").toString(DATE_FORMAT))

    val rsvpsRDD = sparkContext.esJsonRDD(ES_RSVPS_INDEX,
      s"""{"query": {"range" : {"mtime" : {"gte": "$fromDate","lte": "$toDate","format": "$DATE_FORMAT"}}}}""")

    val rsvps = rsvpsRDD.map(rdd => {
      jsonToObject(rdd._2, classOf[Rsvp])
    })

    val topicsDic = rsvps.map(rsvp => {
      rsvp.group.group_topics.map(topic => topic.urlkey)
    }).reduce(_ + _).distinct.sorted.zipWithIndex.toMap

    println("Dictionary Size", topicsDic.size)
    val groups = rsvps.map(rsvp => {
      (rsvp.group.group_urlname, rsvp.group.group_topics.map(_.urlkey))
    }).reduceByKey((t1, t2) => t1)
      .map(r => {
        val index = r._2.map(topic => topicsDic.get(topic).get)
        (r._1, Vectors.sparse(topicsDic.size, index.toArray, Array.fill(index.length)(1.0)))
      })
    //groups.foreach(println)

    val numClusters = 5
    val numIterations = 100
    val values = groups.values.cache()
    val model = KMeans.train(values, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = model.computeCost(values)
    println("Within Set Sum of Squared Errors = " + WSSSE)
    val groupsCluster = model.predict(values)
    groups.keys.zip(groupsCluster).foreach(println)

  }

}
