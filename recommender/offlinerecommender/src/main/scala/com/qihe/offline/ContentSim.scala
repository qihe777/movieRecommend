package com.qihe.offline

import com.qihe.offline.OfflineRecommender.{MovieRating, USER_MAXRECOMMENDATION, USER_RECS_TABLE, UserRecs}
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StructField, StructType}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.control.Breaks

object ContentSim {

  case class Actor(mid: BigInt, aid: BigInt)

  case class Genre(mid: BigInt, name: String)

  case class Tag(mid: BigInt, name: String)

  case class ContentRecs(mid: Int, similar_movie: Int, similar_rate: Double)

  def main(args: Array[String]): Unit = {
    val CONTENT_RECS_TABLE = "content_recs"

    val prop = new java.util.Properties()
    prop.put("driver", "com.mysql.cj.jdbc.Driver")
    prop.put("user", "qihe")
    prop.put("password", "ZHANGyinqi123...")

    //构建环境
    val conf = new SparkConf().setAppName("Sparksql")
      .setMaster("local[*]")

    val spark = SparkSession.builder().config(conf).getOrCreate()
    import spark.implicits._
    val actorRDD = spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> "(select mid,aid from actors_union) as actors",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[Actor]
      .rdd
      .map(actor => (actor.mid.toInt, actor.aid.toInt))
      .groupByKey()
      .map {
        case (mid, aid) =>
          (mid, aid.toList.sortWith(_ > _))
      }

    val actorCOS = actorRDD.cartesian(actorRDD) //笛卡尔积
      .filter {
        case (a, b) => a._1 != b._1
      }.map {
      case (a, b) =>
        val c = a._2.intersect(b._2)
        if (c.isEmpty)
          ((a._1, b._1), 0.0)
        else
          ((a._1, b._1), c.length / Math.sqrt(a._2.length * b._2.length))
    }.filter(_._2 > 0)
      //希望有一个电影演员相同则排名靠前,一个电影演员相似度大概为0.2,*3=0.6+0.5,则排名比较靠前
      .map {
        case (a, b) =>
          (a, 0.45 * b)
      }

    //种类genre
    val genreRDD = spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> "(select mid,name from genres_union) as genres",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[Genre]
      .rdd
      .map(genre => (genre.mid.toInt, genre.name))
      .groupByKey()
      .map {
        case (mid, tests) =>
          (mid, tests.toList)
      }

    val genreCOS = genreRDD.cartesian(genreRDD) //笛卡尔积
      .filter {
        case (a, b) => a._1 != b._1
      }.map {
      case (a, b) =>
        val c = a._2.intersect(b._2)
        if (c.isEmpty)
          ((a._1, b._1), 0.0)
        else
          ((a._1, b._1), c.length / Math.sqrt(a._2.length * b._2.length))
    }.filter(_._2 >= 0.5)
      .map {
        case (a, b) =>
          (a, 0.22 * b)
      }

    //标签tag
    val tagRDD = spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> "(select mid,name from tag_union) as tags",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[Tag]
      .rdd
      .map(tag => (tag.mid.toInt, tag.name))
      .groupByKey()
      .map {
        case (mid, tests) =>
          (mid, tests.toList)
      }

    val tagCOS = tagRDD.cartesian(tagRDD) //笛卡尔积
      .filter {
        case (a, b) => a._1 != b._1
      }.map {
      case (a, b) =>
        val c = a._2.intersect(b._2)
        if (c.isEmpty)
          ((a._1, b._1), 0.0)
        else
          ((a._1, b._1), c.length / Math.sqrt(a._2.length * b._2.length))
    }.filter(_._2 > 0)
      //希望有2个tag相同则排名靠前,2个电影演员相似度大概为0.4,*1.5=0.6+0.5,则排名比较靠前
      .map {
        case (a, b) =>
          (a, 0.33 * b)
      }

    val cosDF = actorCOS
      .union(genreCOS)
      .union(tagCOS)
      .groupByKey()
      .map {
        case (a, b) =>
          val c = b.toList
          if (c.length == 3)
            (a._1, (a._2, c.head + c(1) + c(2)))
          else if (c.length == 2)
            (a._1, (a._2, c.head + c.last))
          else
            (a._1, (a._2, c.head))
      }
      .groupByKey()
      .map {
        case (a, b) =>
          b.toList.sortWith(_._2 > _._2).take(20)
            .map(x => ContentRecs(a, x._1, x._2))
      }
      .collect()
      .toList
      .foldLeft(List.empty[ContentRecs])(_ ::: _)
      .toDF()

    cosDF
      .write
      .mode(SaveMode.Overwrite)
      .jdbc("jdbc:mysql://mysql:3306/movie_recommend", CONTENT_RECS_TABLE, prop)
  }
}
