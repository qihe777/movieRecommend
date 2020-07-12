package com.qihe.offline

import java.sql.{DriverManager, Statement}

import scala.collection.mutable
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SaveMode, SparkSession}

object UserDraw {

  case class MovieItem(uid: String, item: String)
  case class UserInfoRecs(uid: Int, mid: Int, score: Double)
  val MYSQL_INFO_RECS="info_recs"
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

    Class.forName("com.mysql.cj.jdbc.Driver")
    val connection = DriverManager.getConnection("jdbc:mysql://mysql:3306/movie_recommend", "qihe", "ZHANGyinqi123...")
    val statement = connection.createStatement()

    //首先读取数据
    val tagMap = getUserMap(spark,"tag","select d.uid,t.name item from data_ratings d join tag_union t on t.mid=d.mid")

    val actorMap = getUserMap(spark,"actor","select d.uid,a.aid item from data_ratings d join actors_union a on a.mid=d.mid")
    val genreMap = getUserMap(spark,"genre","select d.uid,g.name item from data_ratings d join genres_union g on g.mid=d.mid")
    val movieScoreMap = getScore(statement)
    val movieRDD=spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> s"(select uid,mid item from user_recs) as myuser_recs",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[MovieItem]
      .rdd
      .map(tmp=>(tmp.uid,tmp.item))
      .groupByKey()
      .map {
        case (mid, tests) =>
          (mid, tests.toList)
      }

    movieRDD.collect().foreach {
      case (uid, mList) =>
        mList.map {
          mid =>
            var myscore = 0.0
            //获取此mid对应的score，taglist，actorlist，genrelist
            val score = movieScoreMap(mid)
            if (score > 0.8) {
              myscore = 0.2
            } else if (score > 0.6) {
              myscore = 0.1
            }
            //println(myscore)
            val tagList = getList(statement, s"select name from tag_union where mid=$mid")
            myscore += computeCos(tagMap(uid), tagList)
            //println(myscore)
            val actorList = getList(statement, s"select aid name from actors_union where mid=$mid")
            myscore += computeCos(actorMap(uid), actorList)
            //println(myscore)
            val genreList = getList(statement, s"select name from genres_union where mid=$mid")
            myscore += computeCos(genreMap(uid), genreList)
            //println(myscore)
            (mid, myscore)
        }
          .sortWith(_._2 > _._2)
          .take(10)
          .foreach {
            case (str, d) =>
              try{
                val sql=s"insert into $MYSQL_INFO_RECS values ($uid,  ${str} , ${d})"
                println(sql)
                statement.executeUpdate(sql)
              }catch{
                case e:Exception=>e.printStackTrace()
              }
          }
    }

  }

  def getUserMap(spark: SparkSession, name: String, sql: String): Map[String,Map[String,Int]] = {
    import spark.implicits._
    spark
      .read
      .format("jdbc")
      .options(Map("url" -> "jdbc:mysql://mysql:3306/movie_recommend",
        "driver" -> "com.mysql.cj.jdbc.Driver",
        "dbtable" -> s"($sql) as $name",
        "user" -> "qihe", "password" -> "ZHANGyinqi123..."))
      .load()
      .as[MovieItem]
      .rdd
      .map(tmp=>((tmp.uid,tmp.item),1))
      .reduceByKey(_+_)
      .map(key=>(key._1._1,(key._1._2,key._2)))
      .groupByKey()
      .map {
        case (mid, tests) =>
          (mid, tests.toMap)
      }
      .collect()
      .toMap

  }
  //缓存mid对应的score
  def getScore(statement:Statement): Map[String,Int] ={
    val result=mutable.HashMap[String,Int]()
    val rs1 = statement.executeQuery("select mid,score from movie where mid in (select distinct(mid) from user_recs)")
    while (rs1.next) {
      result.put(rs1.getString("mid"),rs1.getInt("score"))
    }
    result.toMap
  }

  def getList(statement:Statement,sql:String): List[String]={
    val result=mutable.ListBuffer[String]()
    val rs1 = statement.executeQuery(s"$sql")
    while (rs1.next) {
      result.+=(rs1.getString("name"))
    }
    result.toList
  }

  def computeCos(usrMap:Map[String,Int],movieList:List[String]): Double ={
    val userList=usrMap.keySet.toList
    val sameList=userList.intersect(movieList)
    val fenzi=sameList.foldLeft(0)((a,b)=>{a+usrMap(b)})
    val fenmu=Math.sqrt(movieList.length*userList.foldLeft(0)((a,b)=>{usrMap(b)*usrMap(b)+a}))
    fenzi/fenmu
  }

}
