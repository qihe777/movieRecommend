package com.qihe.offline

import breeze.numerics.sqrt
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object AlsTrain {

  case class MovieRating(uid: Int, mid: Int, score: Double)

  val RATING_TABLE = "rating"

  def main(args: Array[String]): Unit = {

    //配置
    val config = Map {
      "spark.core" -> "local";
      "mysql.uri" -> "jdbc:mysql://localhost:3306/recommender"
    }
    val prop = new java.util.Properties()
    prop.put("driver", "com.mysql.cj.jdbc.Driver")
    prop.put("user", "root")
    prop.put("password", "zhangyinqi")

    //初始化spark环境
    val sparkConf = new SparkConf().setAppName("AlsTrain").setMaster(config("spark.core"))
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._

    //准备数据
    val ratingRDD = spark
      .read
      .jdbc(config("mysql.uri"), RATING_TABLE, prop)
      .as[MovieRating]
      .rdd
      .map(item => Rating(item.uid, item.uid, item.score))
      .cache()

    adjust(ratingRDD)

  }

  def adjust(ratingRDD: RDD[Rating]): Unit = {
    //拆分训练集和测试集,常用的方法为10折交叉验证
    val trainData: RDD[Rating] = null
    val verifyData: RDD[Rating] = null
    val testData = verifyData.map(item => (item.user, item.product))

    //测试不同的参数，找到评估函数最大的值
    val result = for (rank <- Array(30, 40, 50, 60, 70);
                      lambda <- Array(1, 0.1, 0.01, 0.001))
      yield {
        val model = ALS.train(verifyData, rank, 5, lambda)
        val predictData = model.predict(testData)
        val rmse = test(predictData, verifyData)
        (rank, lambda, rmse)
      }
    println(result.minBy(_._3))
  }

  def test(predictData: RDD[Rating], verifyData: RDD[Rating]): Double = {
    val predict = predictData.map(item => ((item.user, item.product), item.rating))
    val verify = verifyData.map(item => ((item.user, item.product), item.rating))

    sqrt(
      verify
        .join(predict)
        .map {
          case ((uid, mid), (real, pre)) =>
            val err = real - pre
            err * err
        }.mean()
    )
  }
}
