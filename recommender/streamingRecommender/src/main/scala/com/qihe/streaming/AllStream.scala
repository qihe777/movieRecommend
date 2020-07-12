package com.qihe.streaming

import java.sql.DriverManager

import com.qihe.streaming.StreamingRecommender.computeMovieScores
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

object AllStream {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MOVIES_NUM = 20
  val MYSQL_STREAM_RECS_TABLE = "stream_recs"
  val MYSQL_RATRING_TABLE = "data_ratings"
  val MYSQL_CONTENT_RECS_TABLE = "content_recs"

  def main(args: Array[String]): Unit = {
    //初始化spark环境
    val config = Map(
      "spark.core" -> "local[*]",
      "mysql.url" -> "jdbc:mysql://mysql:3306/movie_recommend",
      "mysql.user" -> "qihe",
      "mysql.pwd" -> "ZHANGyinqi123...",
      "mysql.uri" -> "com.mysql.cj.jdbc.Driver"
    )
    val sparkConf = new SparkConf().setAppName("streaming").setMaster(config("spark.core"))

    //创建spark的对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._
    //链接数据库
    Class.forName(config("mysql.uri"))
    val connection = DriverManager.getConnection(config("mysql.url"), config("mysql.user"), config("mysql.pwd"))

    val statement = connection.createStatement()
    val movieSimMap = mutable.Map[Int, mutable.Map[Int, Double]]()
    //缓存全部的电影相似度表
    val rs1 = statement.executeQuery(s"select mid,similar_movie smid,similar_rate score from $MYSQL_CONTENT_RECS_TABLE")
    while (rs1.next) {
      val mid = rs1.getInt("mid")
      val smid = rs1.getInt("smid")
      val score = rs1.getDouble("score")

      if (movieSimMap.contains(mid)) {
        movieSimMap(mid).put(smid, score)
      } else {
        val tmpMap = mutable.Map[Int, Double]()
        tmpMap.put(smid, score)
        movieSimMap.put(mid, tmpMap)
      }

    }
    //获取全部的用户
    val rs2 = statement.executeQuery(s"select uid from $MYSQL_RATRING_TABLE group by uid")
    val allUser = mutable.Set[Int]()
    while (rs2.next) {
      allUser.add(rs2.getInt("uid"))
    }
    //便利全部用户的最近一次评分
    allUser.foreach {
      uid =>
        //1.获取当前最近的k次电影评分
        val rs3 = statement.executeQuery(s"select mid,ratting from $MYSQL_RATRING_TABLE " +
          s"where uid=$uid order by time desc limit 11")

        val rateMovies = ListBuffer[(Int, Int)]()
        while (rs3.next) {
          rateMovies.+=((rs3.getInt("mid"), rs3.getInt("ratting")))
        }
        val lastRateMid = rateMovies.remove(0)._1
        //2.获取此电影最相似的k个电影s，（去除uid评过分的）
        val lastRateSimMovie = movieSimMap(lastRateMid).filterKeys(mid => !rateMovies.map(_._1).contains(mid)).keys
        //3.在数据库中查询相似电影列表s中的电影和最近评分的k个电影的相似度
        //4.计算待选电影的推荐优先级
        val streamRecs = computeMovieScores(rateMovies.toArray, lastRateSimMovie.toArray, movieSimMap)
        //5.将数据保存到mysql:
        //先删除，再保存:uid,mid,recs
        val result = mutable.Set[(Int, Double)]()
        val rs = statement.executeQuery(s"select uid,mid,recs from  $MYSQL_STREAM_RECS_TABLE where uid =$uid")
        while (rs.next) {
          result.add((rs.getInt("mid"), rs.getDouble("recs")))
        }
        val tmpList = result.toList
        statement.executeUpdate(s"delete from $MYSQL_STREAM_RECS_TABLE where uid =$uid")
        //获取的新推荐和数据库保存的电影进行排序，通过filter去重
        val newresult = streamRecs.toList.filter(item => !tmpList.map(_._1).contains(item._1)) ::: tmpList
        val datas = newresult.sortWith(_._2 > _._2).take(10)
        for (data <- datas) {
          val sql = s"insert into $MYSQL_STREAM_RECS_TABLE values ($uid,  ${data._1} , ${data._2},0)"
          statement.addBatch(sql)
        }
        statement.executeBatch()
    }

  }

  //4.计算待选电影的推荐优先级
  def computeMovieScores(userRecentRatings: Array[(Int, Int)], topSimMovies: Array[Int], simMovieMap:  mutable.Map[Int, mutable.Map[Int, Double]]): Array[(Int, Double)] = {
    //用来保存每一个待选电影和最近评分的每一个电影的权重得分
    val score = mutable.ArrayBuffer[(Int, Double)]()

    //保存每个电影的增强因子数
    val increMap = mutable.HashMap[Int, Int]()
    //保存每个电影的减弱因子数
    val decreMap = mutable.HashMap[Int, Int]()

    //遍历与评分电影相似的,且未看过的电影，比如A
    //遍历用户最近评分的电影，比如[x,y,z]
    //计算sim(a,x),sim(a,y),sim(a,z)
    for (topSimMovie <- topSimMovies) {
      for (userRecentRating <- userRecentRatings) {
        //如果电影足够相似
        if (simMovieMap(userRecentRating._1).containsKey(topSimMovie)) {
          //则用A与x的相似度*用户对电影X的评分，放入缓存中
          score += ((topSimMovie, simMovieMap(userRecentRating._1)(topSimMovie) * userRecentRating._2))
          if (userRecentRating._2 > 3) {
            increMap(topSimMovie) = increMap.getOrDefault(topSimMovie, 0) + 1
          } else {
            decreMap(topSimMovie) = decreMap.getOrDefault(topSimMovie, 0) + 1
          }
        }
      }
    }

    //应为电影相似度矩阵只有很少的电影，很容易出现候选集合为空的情况。
    //为了防止尴尬，进行推荐此电影的最相似的电影
    if (score.isEmpty) {
      Random.shuffle(topSimMovies.map(x => (x, 0.0)).toList).take(10).toArray
    } else {
      //因为缓存中有许多相同的电影id
      score
        .groupBy(_._1)
        .map {
          case (mid, sims) =>
            (mid, sims.map(_._2).sum / sims.length + log(increMap.getOrDefault(mid, 1)) - log(decreMap.getOrDefault(mid, 1)))
        }.toArray
    }
  }

  //取2的对数
  def log(m: Int): Double = {
    math.log(m) / math.log(2)
  }

}
