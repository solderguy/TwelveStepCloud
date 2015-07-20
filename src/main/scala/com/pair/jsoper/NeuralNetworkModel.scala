package com.pair.jsoper

import java.io.File
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkFiles
import org.apache.spark.h2o.Frame
import org.apache.spark.h2o.H2OContext
import org.apache.spark.h2o.H2OFrame
import org.apache.spark.h2o.StringHolder
import org.apache.spark.h2o.IntHolder
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext

import hex.deeplearning.DeepLearning
import hex.deeplearning.DeepLearningModel
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import water.Key
import water.fvec.H2OFrame

/**
 * Neural Network Model for 12-Step OCR program
 * @author John Soper
 */
object NeuralNetworkModel {

  def configure(appName: String = "Sparkling Water Demo"): SparkConf = {
    val conf = new SparkConf()
      .setAppName(appName)
    conf.setIfMissing("spark.master", sys.env.getOrElse("spark.master", "local"))
    conf
  }

  /** Builds DeepLearning model. */
  def buildDLModel(train: Frame, valid: Frame, response_column: String = "target",
                   epochs: Int = 10, l1: Double = 0.001, l2: Double = 0.0,
                   hidden: Array[Int] = Array[Int](200, 200))(implicit h2oContext: H2OContext): DeepLearningModel = {
    import h2oContext._

    val dlParams = new DeepLearningParameters()
    dlParams._model_id = Key.make("dlModel.hex").asInstanceOf[water.Key[Frame]]
    dlParams._train = train
    dlParams._valid = valid
    dlParams._response_column = response_column
    dlParams._epochs = epochs
    dlParams._l1 = l1
    dlParams._hidden = hidden

    // Create a job
    val dl = new DeepLearning(dlParams)
    val dlModel = dl.trainModel.get

    // Compute metrics on both datasets
    dlModel.score(train).delete()
    dlModel.score(valid).delete()
    dlModel
  }

  def scoreData(dataRole: String, dataRDD: RDD[StringHolder], predictRDD: RDD[StringHolder]): Unit = {
    // Make sure that both RDDs have the same number of elements
    assert(dataRDD.count() == predictRDD.count)
    val numMispredictions = dataRDD.zip(predictRDD).filter(i => {
      val act = i._1
      val pred = i._2
      act.result != pred.result
    }).collect()

    println("Number of total " + dataRole + " predictions: " + predictRDD.count)
    println("Number of total " + dataRole + " mispredictions: " + numMispredictions.length)

    //    println(
    //      s"""
    //         |Number of mispredictions: ${numMispredictions.length}
    //         |
    //         |Mispredictions:
    //         |
    //         |actual X predicted
    //         |------------------
    //         |${numMispredictions.map(i => i._1.result.get + " X " + i._2.result.get).mkString("\n")}
    //       """.stripMargin)

  }

  def main(args: Array[String]) {
    // Create Spark and H2O Contexts
    val conf = configure("Sparkling Water Droplet")
    val sc = new SparkContext(conf)
    implicit val h2oContext = new H2OContext(sc).start()
    import h2oContext._

    // Register file to be available on all nodes
    sc.addFile(new File("data/letter-recognition_train.csv").getAbsolutePath)
    sc.addFile(new File("data/letter-recognition_test.csv").getAbsolutePath)
    sc.addFile(new File("data/two_line.csv").getAbsolutePath)

    // Load data and parse it via h2o parser
    val trainData = new H2OFrame(new File(SparkFiles.get("letter-recognition_train.csv")))
    val validData = new H2OFrame(new File(SparkFiles.get("letter-recognition_test.csv")))
    val oneLineData = new H2OFrame(new File(SparkFiles.get("two_line.csv")))

    val dlModel = buildDLModel(trainData.asInstanceOf[Frame],
      validData.asInstanceOf[Frame],
      "Let")

    // Make prediction on training data and compute number of mispredictions
    val trainRDD = asRDD[StringHolder](trainData('Let))
    val predictTrain = dlModel.score(trainData)('predict)
    val predictTrainRDD = asRDD[StringHolder](predictTrain)
    scoreData("training", trainRDD, predictTrainRDD)

    //    val validRDD = asRDD[StringHolder](validData('Let))
    //    val predictValid = dlModel.score(validData)('predict)
    //    val predictValidRDD = asRDD[StringHolder](predictValid)
    //    scoreData("validation", validRDD, predictValidRDD)

    val oneLineRDD = asRDD[StringHolder](oneLineData('Let))
    val predictOneLine = dlModel.score(oneLineData)('predict)
    val oneLinePredictRDD = asRDD[StringHolder](predictOneLine)
    scoreData("one line", oneLineRDD, oneLinePredictRDD)

    sc.stop()
    println("finished running")
  }
}
