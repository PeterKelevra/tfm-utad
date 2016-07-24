package org.utad.tfm.ppena.ml.prediction

import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.GradientBoostedTrees
import org.apache.spark.mllib.tree.configuration.BoostingStrategy
import org.utad.tfm.ppena.core.Util
import org.utad.tfm.ppena.ml.prediction.PredictionUtil._

/**
  *
  */
object GradientBoostedRegressionPredictor extends App {

  val conf = Util.initConf
  conf.setAppName(getClass.getSimpleName)

  val sc = new SparkContext(conf)

  // Read the CSV file
  val recordsRDD = sc.textFile("data/events_2015-01-01_2016-01-01.csv")
    .map(parseCsvRecord).filter(_.yes_rsvp_count > 10).cache()

  val categorizedMaps = buildCategorizedMaps(recordsRDD)

  //Generamos los datos para el entreamiento y test
  val mldata = makeMlData(recordsRDD, categorizedMaps)

  // Train a GradientBoostedTrees model.
  // The defaultParams for Regression use SquaredError by default.
  var boostingStrategy = BoostingStrategy.defaultParams("Regression")
  boostingStrategy.numIterations = 100
  //boostingStrategy.treeStrategy.maxDepth = 3
  boostingStrategy.treeStrategy.maxBins = categorizedMaps.cities.size
  boostingStrategy.treeStrategy.categoricalFeaturesInfo = buildCategoricalFeauturesInfo(categorizedMaps)

  val model = GradientBoostedTrees.train(mldata.trainingData, boostingStrategy)

  // Evaluate model on test instances and compute test error
  val labelsAndPredictions = mldata.testData.map { point =>
    val prediction = model.predict(point.features)
    (point.label.toInt, prediction.toInt)
  }

  saveResults(labelsAndPredictions, s"boosting");

  //println("Learned regression tree model:\n" + model.toDebugString)

  // Save and load model
  model.save(sc, "target/myBoostingModelGt10")
  //val sameModel = RandomForestModel.load(sc, "target/tmp/myDecisionTreeRegressionModel")


}
