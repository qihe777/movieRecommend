package com.qihe.offline

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.jblas.DoubleMatrix


object OfflineRecommender {

  //als算法中有个类叫rating，防止冲突
  case class MovieRatingtable(uid: BigInt, mid: BigInt, ratting: Int)

  case class MovieRating(uid: Int, mid: Int, ratting: Double)

  //用户的推荐
  case class UserRecs(uid: Int, mid: Int, score: Double)

  //电影相似度推荐
  case class MovieRecs(mid: Int, similar_movie: Int, similar_rate: Double)

  val RATING_TABLE = "data_rating"
  val USER_RECS_TABLE = "user_recs"
  val MOVIE_RECS_TABLE = "movie_recs"
  //电影推荐最大数量
  val USER_MAXRECOMMENDATION = 20

  def main(args: Array[String]): Unit = {

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
        "dbtable" -> "(select uid,mid,ratting from data_rating) as rating",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[MovieRatingtable]
      .rdd
      .map(rating => (rating.uid.toInt, rating.mid.toInt, rating.ratting))
      .cache()


    //用户数据集，只需要用户id，直接从评分里面导出来
    val userRDD = ratingRDD.map(_._1).distinct()

    val movieRDD = ratingRDD.map(_._2).distinct()


    //创建训练数据集
    val trainData = ratingRDD.map(x => Rating(x._1, x._2, x._3))
    print("测试：创建训练及")
    //als训练参数
    val (rank, iterations, lambda) = (50, 5, 0.01)

    //训练als模型,没必要保存，每次模型都不同
    val model = ALS.train(trainData, rank, iterations, lambda)
    print("测试：完成模型训练")
    //计算用户推荐矩阵
    //先构造userMovies，RDD[(uid,mid)]
    val userMovies = userRDD.cartesian(movieRDD) //笛卡尔积
    print("测试：完成笛卡尔积")
    val preRatings = model.predict(userMovies)
    print("测试：完成推荐,正在存进数据库")
    val userRecsDF = preRatings
      .map(rating => (rating.user, (rating.product, rating.rating)))
      .groupByKey()
      .map { case (uid, recs) =>
        recs.toList
          .sortWith(_._2 > _._2)
          .take(20)
          .map(x => MovieRating(uid, x._1, x._2))
      }.collect()
      .toList
      .foldLeft(List.empty[MovieRating])(_ ::: _)
      .toDF()
    userRecsDF.printSchema()
    println(userRecsDF.head)

    /*    val schema = StructType(
          StructType(
            Seq(
              StructField("uid", IntegerType, nullable = false)
              , StructField("mid", IntegerType, nullable = false)
              , StructField("ratting", DoubleType, nullable = false))))
        val emptyDF = spark.createDataFrame(spark.sparkContext.emptyRDD[Row], schema)
        println(emptyDF.printSchema())
        //转化成
        val userRecsDF = preRatings
          .map(rating => (rating.user, (rating.product, rating.rating)))
          .groupByKey()
          .map { case (uid, recs) =>
            recs.toList
              .sortWith(_._2 > _._2)
              .take(20)
              .map(x => MovieRating(uid, x._1, x._2))
              .toSeq.toDF()
          }
          .fold(emptyDF)((x, y) => x.join(y))*/

    print("测试：推荐数据装进数据库")
    userRecsDF
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(config("mysql.uri"), USER_RECS_TABLE, prop)


    //计算电影相似读矩阵
    println("正在计算电影相似度")
    //获取电影的特征矩阵,mid和电影的特征矩阵
    val movieFeatures = model.productFeatures
      .map {
        case (mid, freatures) => (mid, new DoubleMatrix(freatures))
      }


    val movieRecs = movieFeatures.cartesian(movieFeatures)
      .filter {
        case (a, b) => a._1 != b._1
      }
      .map {
        case (a, b) =>
          val simScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, simScore))
      }
      .filter(_._2._2 > 0.6)
      .map {
        case (mid, items) => MovieRecs(mid, items._1, items._2)
      }
      .toDF()


    movieRecs
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(config("mysql.uri"), MOVIE_RECS_TABLE, prop)
    spark.close()
  }

  //计算两个电影之间的余弦相似读
  def consinSim(movie1: DoubleMatrix, movie2: DoubleMatrix): Double = {
    movie1.dot(movie2) / (movie1.norm2() * movie2.norm2())
  }

}
