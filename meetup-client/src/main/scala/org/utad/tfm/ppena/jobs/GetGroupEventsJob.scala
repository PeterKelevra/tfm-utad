package org.utad.tfm.ppena.jobs


import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.HitAs
import com.sksamuel.elastic4s.RichSearchHit
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.elasticsearch.spark._
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Group
import org.utad.tfm.ppena.core.Util
import org.utad.tfm.ppena.elastic.ElasticSearchManager


/**
  * Proceso utilizado para obtener todos los eventos de cada uno de los grupos que existen en elasticsearch.
  * De esta manera podemos obtener un mayor número de eventos y con fechas antereiores a las del inicio de
  * procesado de streaming
  */
object GetGroupEventsJob {

  def main(args: Array[String]) {
    val conf = Util.initConf
    searchByElastic4s(conf, 2)
  }

  def searchByElastic4s(conf: SparkConf, extractVersion: Int) = {

    implicit object GroupHitAs extends HitAs[Group] {
      override def as(hit: RichSearchHit): Group = Util.mapToObject(hit.sourceAsMap, classOf[Group])
    }

    val config = Util.getElasticSearchClusterConfig(conf)
    val esClient = new ElasticSearchManager(config)
    var moreHits = true
    while (moreHits) {
      try {
        val resp = esClient.client.execute {
          //search in ES_GROUPS_INDEX query "_missing_:events_extracted AND _exists_:urlname"
          search in ES_GROUPS_INDEX query s"NOT events_extract:$extractVersion"
        }.await

        val groups: Seq[Group] = resp.as[Group]
        groups.par.foreach(group => {
          println(s">>> Getting group ${group.name} ${group.urlname}")
          new ElasticSearchManager(config).indexGroupEvents(group, extractVersion)
        })
        moreHits = !groups.isEmpty
      } catch {
        case e: Exception => println("Iteration error", e.getMessage, e.printStackTrace())
      }
    }
    esClient.closeClient()
  }

  def searchByRDD(conf: SparkConf, extractVersion: Int) = {

    conf.setAppName(getClass.getSimpleName)

    val sparkContext = new SparkContext(conf)

    val clusterConfig = sparkContext.broadcast(Util.getElasticSearchClusterConfig(conf))
    //val query ="""{"query":{"bool": {"must_not": {"exists": {"field": "events_extracted"}}}}}"""
    val query =
      s"""{"query": {"query_string": {"query": "NOT events_extract:$extractVersion"}}}"""
    sparkContext.esRDD(ES_GROUPS_INDEX, query).foreach(k => {
      val esClient = new ElasticSearchManager(clusterConfig.value)
      esClient.indexGroupEvents(Util.mapToObject(k._2, classOf[Group]), extractVersion)
    })
  }

}
