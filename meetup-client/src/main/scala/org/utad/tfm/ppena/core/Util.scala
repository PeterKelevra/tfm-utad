package org.utad.tfm.ppena.core

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.redis.RedisClient
import org.apache.spark.SparkConf
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.utad.tfm.ppena.core.Constants._

/**
  */
object Util {

  def initConf = {
    val conf = new SparkConf()
    val SPARK_PREFIX = "spark."

    //Propiedades de configuración para elasticsearch
    conf.set(ES_NODES, conf.get(s"$SPARK_PREFIX$ES_NODES", MASTER_HOST))
    conf.set(ES_PORT, conf.get(s"$SPARK_PREFIX$ES_PORT", ES_HTTP_PORT))
    conf.set(ES_BATCH_WRITE_RETRY_COUNT, conf.get(s"$SPARK_PREFIX$ES_BATCH_WRITE_RETRY_COUNT", "-1"))
    conf.set(ES_BATCH_WRITE_RETRY_WAIT, conf.get(s"$SPARK_PREFIX$ES_BATCH_WRITE_RETRY_WAIT", "100s"))
    conf.set(ES_HTTP_TIMEOUT, conf.get(s"$SPARK_PREFIX$ES_HTTP_TIMEOUT", "2m"))


    println(">>>> INPUT CONFIG <<<<<")
    conf.getAll.foreach(println)
    conf
  }

  /**
    * Obtenemos parámetros de configuración para acceso a elasticsearch
    *
    * @param conf
    * @return
    */
  def getElasticSearchClusterConfig(conf: SparkConf) = {
    new ElasticSearchClusterConfig(conf.get("spark.es.cluster.name", ES_CLUSTER_NAME),
      conf.get("spark.es.cluster.uri", s"elasticsearch://$MASTER_HOST:$ES_TRANSPORT_PORT"))
  }

  def redisClient(host: String, port: Int): RedisClient = {
    new RedisClient(host, port)
  }


  def objectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.setSerializationInclusion(Include.NON_NULL)
    mapper
  }


  def mapToObject[T](map: collection.Map[String, AnyRef], clasz: Class[T]): T = {
    objectMapper.readValue(objectMapper.writeValueAsString(map), clasz)
  }

  def mapToObject[T](map: java.util.Map[String, Object], clasz: Class[T]): T = {
    objectMapper.readValue(objectMapper.writeValueAsString(map), clasz)
  }

  def jsonToObject[T](json: String, clasz: Class[T]): T = {
    objectMapper.readValue(json, clasz)
  }

  def jsonToArrayObjects[T](json: String, clasz: T): Array[T] = {
    objectMapper.readValue(json, classOf[Array[T]])
  }

  def objectToJson(obj: Any) = {
    objectMapper.writeValueAsString(obj)
  }

}
