package org.utad.tfm.ppena.ml.prediction

import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.RandomForest
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.utad.tfm.ppena.core.Util
import org.utad.tfm.ppena.ml.prediction.PredictionUtil._

/**
  * Clase que implementa el algoritmo de RandomForest sobre un conjunto de eventos de entrada, con el objetivo de predecir
  * el número de respuestas de confirmación que tendrán futuros eventos
  */
object RandomForestPredictor extends App {

  val conf = Util.initConf
  conf.setAppName(getClass.getSimpleName)

  val sc = new SparkContext(conf)

  //Read data from elasticsearch
  val recordsRDD = readEventFromElastic(sc, new LocalDate("2015-01-01"), new LocalDate("2016-08-01"), 10)
  recordsRDD.take(20).foreach(println)

  val categorizedMaps = buildCategorizedMaps(recordsRDD)

  //Generamos los datos para el entrenamiento y test
  val mldata = makeMlData(recordsRDD, categorizedMaps)

  // Train a DecisionTree model.
  val categoricalFeaturesInfo = buildCategoricalFeauturesInfo(categorizedMaps)
  val impurity = "variance"
  val maxDepth = 5
  val maxBins = categorizedMaps.cities.size
  val numTrees = 20
  val featureSubsetStrategy = "auto" // Let the algorithm choose.

  val model = RandomForest.trainRegressor(mldata.trainingData, categoricalFeaturesInfo,
    numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)

  // Evaluate model on test instances and compute test error
  val labelsAndPredictions = mldata.testData.map { point =>
    val prediction = model.predict(point.features)
    (point.label.toInt, prediction.toInt)
  }

  saveResults(labelsAndPredictions, s"rf");

  println("Learned regression tree model:\n" + model.toDebugString)

  // Save and load model
  model.save(sc, s"target/myRfModelGt10_${new LocalDateTime().toString("yyyyMMdd_HHmm")}")

}
