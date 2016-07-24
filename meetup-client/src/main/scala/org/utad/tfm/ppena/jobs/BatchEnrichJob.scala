package org.utad.tfm.ppena.jobs

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.HitAs
import com.sksamuel.elastic4s.RichSearchHit
import org.apache.spark.SparkContext
import org.elasticsearch.spark._
import org.utad.tfm.ppena.core.Constants._
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util._
import org.utad.tfm.ppena.elastic.ElasticSearchManager

/**
  * Proceso encargado de enriquecer las respuestas existentes en elasticsearch
  */
object BatchEnrichJob {

  def main(args: Array[String]) {

    val conf = initConf
    conf.setAppName(getClass.getSimpleName)

    val esConfig = getElasticSearchClusterConfig(conf)

    enrichByRdd
    enrichByElastic4s

    def enrichByRdd = {
      val sc = new SparkContext(conf)
      val clusterConfig = sc.broadcast(esConfig)

      while (true) {
        println("Enrich phase")
        //"?q=enriched:false"
        sc.esJsonRDD(ES_RSVPS_INDEX, "?q=_missing_:week_day").foreach(k => {
          val esClient = new ElasticSearchManager(clusterConfig.value)
          try {
            val rsvp = jsonToObject(k._2, classOf[Rsvp])
            esClient.enrichRsvps(rsvp)
          } catch {
            case e: Exception => println("Enrich Error", e.getMessage)
          } finally {
            esClient.closeClient()
          }
        })
      }
    }

    def enrichByElastic4s = {

      implicit object RsvpHitAs extends HitAs[Rsvp] {
        override def as(hit: RichSearchHit): Rsvp = mapToObject(hit.sourceAsMap, classOf[Rsvp])
      }

      var moreItems = false
      val esClient = new ElasticSearchManager(esConfig)
      do {
        val resp = esClient.client.execute {
          search in ES_RSVPS_INDEX query "_missing_:week_day" size (100)
        }.await
        resp.as[Rsvp].par.foreach(rsvp => new ElasticSearchManager(esConfig).enrichRsvps(rsvp))
        moreItems = !resp.isEmpty
      } while (moreItems)
      esClient.closeClient()
    }

  }

}
