package com.qihe.offline

import com.qihe.offline.OfflineRecommender.{MOVIE_RECS_TABLE, MovieRatingtable, MovieRecs}
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.jblas.DoubleMatrix

object UserSim {
  case class UserSimi(uid: Int, sim_uid: Int, score: Double)
  def main(args: Array[String]): Unit = {
    val USER_SIM_TABLE = "user_sim"

    val config = Map(
      "spark.core" -> "local[*]",
      "mysql.uri" -> "jdbc:mysql://mysql:3306/movie_recommend"
    )
    val prop = new java.util.Properties()
    prop.put("driver", "com.mysql.cj.jdbc.Driver")
    prop.put("user", "qihe")
    prop.put("password", "ZHANGyinqi123...")

    val sparkConf = new SparkConf().setAppName("offline").setMaster(config("spark.core"))
      .set("spark.executor.memory", "3G").set("spark.driver.memory", "3G")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    //首先读取数据
    val ratingRDD = spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> "(select uid,mid,ratting from data_ratings) as rating",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[MovieRatingtable]
      .rdd
      .map(rating => (rating.uid.toInt, rating.mid.toInt, rating.ratting))
      .cache()

    //创建训练数据集
    val trainData = ratingRDD.map(x => Rating(x._1, x._2, x._3))
    print("测试：创建训练及")
    //als训练参数
    val (rank, iterations, lambda) = (50, 5, 0.01)

    //训练als模型,没必要保存，每次模型都不同
    val model = ALS.train(trainData, rank, iterations, lambda)

    //计算电影相似读矩阵
    println("正在计算用户相似度")
    //获取用户的特征矩阵,mid和电影的特征矩阵
    val userFeatures = model.userFeatures
      .map {
        case (mid, freatures) => (mid, new DoubleMatrix(freatures))
      }


    val userSim = userFeatures.cartesian(userFeatures)
      .filter {
        case (a, b) => a._1 != b._1
      }
      .map {
        case (a, b) =>
          val simScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, simScore))
      }
      .groupByKey()
      .map { case (uid, recs) =>
        recs.toList
          .sortWith(_._2 > _._2)
          .take(10)
          .map(x => UserSimi(uid, x._1, x._2))
      }.collect()
      .toList
      .foldLeft(List.empty[UserSimi])(_ ::: _)
      .toDF()

    userSim
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(config("mysql.uri"), USER_SIM_TABLE, prop)
    spark.close()
  }

  //计算两个电影之间的余弦相似读
  def consinSim(movie1: DoubleMatrix, movie2: DoubleMatrix): Double = {
    movie1.dot(movie2) / (movie1.norm2() * movie2.norm2())
  }
}
