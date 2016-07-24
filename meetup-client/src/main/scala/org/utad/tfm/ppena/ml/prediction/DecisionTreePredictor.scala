package org.utad.tfm.ppena.ml.prediction

import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.DecisionTree
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.utad.tfm.ppena.core.Util
import PredictionUtil._

/**
  *
  */
object DecisionTreePredictor extends App {

  val conf = Util.initConf
  conf.setAppName(getClass.getSimpleName)

  val sc = new SparkContext(conf)

  // Read the CSV file
  //val recordsRDD = sc.textFile("data/events-200k").map(parseCsvRecord).cache()

  //Read data from elasticsearch
  val recordsRDD = readEventFromElastic(sc, new LocalDate("2016-07-01"), new LocalDate("2016-07-31"), 10)

  val categorizedMaps = buildCategorizedMaps(recordsRDD)

  //Generamos los datos para el entreamiento y test
  val mldata = makeMlData(recordsRDD, categorizedMaps)

  // Train a DecisionTree model.
  val categoricalFeaturesInfo = buildCategoricalFeauturesInfo(categorizedMaps)
  val impurity = "variance"
  val maxDepth = 10
  val maxBins = categorizedMaps.cities.size


  val model = DecisionTree.trainRegressor(mldata.trainingData, categoricalFeaturesInfo, impurity, maxDepth, maxBins)

  // Evaluate model on test instances and compute test error
  val labelsAndPredictions = mldata.testData.map { point =>
    val prediction = model.predict(point.features)
    (point.label.toInt, prediction.toInt)
  }

  mldata.trainingData.take(10).foreach(println)

  saveResults(labelsAndPredictions, "dt")

  println("Learned regression tree model:\n" + model.toDebugString)

  // Save and load model
  model.save(sc, s"target/decisionTreeModelGt10_${new LocalDateTime().toString("yyyyMMdd_HHmm")}")
  //val sameModel = DecisionTreeModel.load(sc, "target/tmp/myDecisionTreeRegressionModel")


}
