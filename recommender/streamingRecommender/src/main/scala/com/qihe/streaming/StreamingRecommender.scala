package com.qihe.streaming

import java.sql.{Connection, DriverManager}
import java.util

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Random


//电影相似度推荐
case class MovieRecs(mid: Int, similar_movie: Int, similar_rate: Double)

object StreamingRecommender {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MOVIES_NUM = 20
  val MYSQL_STREAM_RECS_TABLE = "stream_recs"
  val MYSQL_RATRING_TABLE = "data_ratings"
  val MYSQL_CONTENT_RECS_TABLE = "content_recs"


  //入口方法
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.core" -> "local[*]",
      "kafka.topic" -> "recommender",
      "mysql.url" -> "jdbc:mysql://mysql:3306/movie_recommend?autoReconnect=true",
      "mysql.user" -> "qihe",
      "mysql.pwd" -> "ZHANGyinqi123...",
      "mysql.uri" -> "com.mysql.cj.jdbc.Driver"
    )

    //创建kafka链接
    val kafkaPara = Map(
      "bootstrap.servers" -> "localhost:9092", //kafka地址
      "key.deserializer" -> classOf[StringDeserializer], //key-value反序列化类
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender", //消费者组
      "auto.offset.reset" -> "latest" //消费的
    )

    //将kafka和spark链接
    //创建sparkconf配置
    val sparkConf = new SparkConf().setAppName("streaming").setMaster(config("spark.core"))

    //创建spark的对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._
    spark.sparkContext.setLogLevel("WARN")
    val sc = spark.sparkContext

    val ssc = new StreamingContext(sc, Seconds(2)) //streaming context

    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc, LocationStrategies.PreferConsistent
      , ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaPara)) //消费者监听队列，

    //产生评分流，流中传出的内容：uid|mid|score|timestamp
    val ratingStream = kafkaStream.map {
      msg =>
        println(s"收到消息啦:${msg.value()}")
        val atr = msg.value().split(":")(1).split("\\|")
        (atr(0).toInt, atr(1).toInt, atr(2).toDouble, atr(3))
    }

    Class.forName(config("mysql.uri"))
    val connection = DriverManager.getConnection(config("mysql.url"), config("mysql.user"), config("mysql.pwd"))

    ratingStream.foreachRDD { rdd =>
      rdd.collect().foreach{ case (uid, mid, score, timestamp) =>
        println(s">>>>>>>>正在处理用户（$uid）>>>>>>>>")
        //获取电影最相似的k个电影s，（去除uid评过分的）
        val mostSimMovies = getTopSimMovies(connection, MAX_SIM_MOVIES_NUM, mid, uid)

        //获取当前最近的k次电影评分
        val userRecentRating = getUserRecentRating(connection, MAX_USER_RATINGS_NUM, uid)

        //在数据库中查询s中的电影和最近评分的k个电影的相似度
        val simMovieMap = getSimMovieMap(connection, userRecentRating)


        //计算待选电影的推荐优先级
        val streamRecs = computeMovieScores(userRecentRating, mostSimMovies, simMovieMap)
        //将数据保存到mysql:
        //先删除，再保存:uid,mid,recs
        val statement = connection.createStatement()
        val result = mutable.Set[(Int, Double)]()
        val rs = statement.executeQuery(s"select uid,mid,recs from  $MYSQL_STREAM_RECS_TABLE where uid =$uid")
        while (rs.next) {
          result.add((rs.getInt("mid"), rs.getDouble("recs")))
        }
        val tmpList=result.toList
        statement.executeUpdate(s"delete from $MYSQL_STREAM_RECS_TABLE where uid =$uid")
        //获取的新推荐和数据库保存的电影进行排序，通过filter去重
        val newresult = streamRecs.toList.filter(item => !tmpList.map(_._1).contains(item._1)) ::: tmpList
        val datas = newresult.sortWith(_._2 > _._2).take(10)
        for (data <- datas) {
          val sql=s"insert into $MYSQL_STREAM_RECS_TABLE values ($uid,  ${data._1} , ${data._2},0)"
          statement.addBatch(sql)
        }
        statement.executeBatch()
        println(s"<<<<<<<<解析用户($uid)完成<<<<")
      }
    }
    println("启动streaming")
    //启动Streaming程序
    ssc.start()
    ssc.awaitTermination()
    println("正在等待消息")
  }

  /**
   * 从mysql中获取num个最近的评分
   *
   * @param connectrion 数据库链接
   * @param num         次数
   * @param uid         用户id
   * @return
   */
  def getUserRecentRating(connectrion: Connection, num: Int, uid: Int): Array[(Int, Int)] = {
    //查找出用户看过的电影
    val statement = connectrion.createStatement()
    val rs = statement.executeQuery(s"select mid,ratting from $MYSQL_RATRING_TABLE where uid=$uid order by time desc limit $num")
    val result = mutable.Set[(Int, Int)]()
    while (rs.next) {
      result.add((rs.getInt("mid"), rs.getInt("ratting")))
    }
    result.toArray
  }

  /**
   * 从mysql中获取当前电影相似的电影
   *
   * @param connection mysql jdbc链接
   * @param num        相似的k个电影
   * @param mid        电影id
   * @param uid        用户id
   * @return
   */
  def getTopSimMovies(connection: Connection, num: Int, mid: Int, uid: Int): Array[Int] = {

    val statement = connection.createStatement()
    val rs = statement.executeQuery(s"select similar_movie from $MYSQL_CONTENT_RECS_TABLE where mid=$mid and similar_movie not in(select mid from $MYSQL_RATRING_TABLE where uid=$uid)")
    val result = mutable.Set[Int]()
    while (rs.next) {
      result.add(rs.getInt("similar_movie"))
    }
    result.toArray
  }

  /**
   * 从mysql中获取当前电影相似的电影
   *
   * @param connection       mysql jdbc链接
   * @param userRecentRating 相似的k个电影
   * @return
   */
  def getSimMovieMap(connection: Connection, userRecentRating: Array[(Int, Int)]): Map[Int, Map[Int, Double]] = {
    val statement = connection.createStatement()
    var ids = ""
    userRecentRating.map(_._1).foreach(x => ids=ids.concat(s"$x,"))
    val idstr = ids.substring(0, ids.length - 1)
    val rs = statement.executeQuery(s"select mid,similar_movie,similar_rate from $MYSQL_CONTENT_RECS_TABLE where mid in($idstr)")
    val result = mutable.Set[(Int, Int, Double)]()
    while (rs.next) {
      result.add((rs.getInt("mid"), rs.getInt("similar_movie"), rs.getDouble("similar_rate")))
    }
    result
      .map(x => (x._1, (x._2, x._3)))
      .groupBy(_._1)
      .map {
        case (key, value) =>
          val tmp = value.map {
            case (x, y) =>
              (y._1, y._2)
          }
            .toMap
          (key, tmp)
      }
  }

  /**
   * 计算待选电影的推荐优先级
   *
   * @param userRecentRatings 用户最近k次评分
   * @param topSimMovies      当前电影最相似的m个电影
   * @param simMovieMap       电影的相似度map
   * @return
   */
  def computeMovieScores(userRecentRatings: Array[(Int, Int)], topSimMovies: Array[Int], simMovieMap: Map[Int, Map[Int, Double]]): Array[(Int, Double)] = {
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
      println("推荐结果为空，进行推荐相似电影")
      Random.shuffle(topSimMovies.map(x => (x, 0.0)).toList).take(10).toArray
    } else {
      println("得到事实推荐结果")
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
