package com.example.business.dao.mapper;

import com.example.business.dao.MovieCategoryProvider;
import com.example.business.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserDataMapper {
    @SelectProvider(type = MovieCategoryProvider.class, method = "joinUserSql")
    List<UserData> getUserList(Map<String,String> map);

    @Update("update data_users set token=#{token} where uid=#{uid}")
    int updateToken(int uid, String token);

    @Select("select m.mid,m.name,m.pic,d.ratting,d.time from movie m " +
            "join data_ratings d on d.mid=m.mid where d.uid=#{uid} " +
            "order by d.time desc")
    List<BriefMovieWithTime> recentRating(int uid);

    @Select("select uid,gender,age,occupation from data_users where uid=#{uid}")
    UserData getUserInfo(int uid);

    @Select("select name,count(*) count from genres_union " +
            "where mid in " +
            "(select mid from data_ratings where uid=#{uid})" +
            "group by name order by count desc limit 10")
    List<Count> genresCount(int uid);

    @Select("select name,count(*) count from tag_union " +
            "where mid in " +
            "(select mid from data_ratings where uid=#{uid})" +
            "group by name order by count desc limit 10")
    List<Count> tagCount(int uid);

    @Select("select a.id aid,tmp.count role,a.name,a.pic from actor a " +
            "right join " +
            "(select u.aid,count(*) count from actors_union u where u.mid " +
            "in (select mid from data_ratings where uid=1) " +
            "group by u.aid order by count desc limit 6) " +
            "tmp on tmp.aid=a.id")
    List<Actor> actorCount(int uid);

    @Select("select ratting from data_ratings where uid=#{uid} and mid=#{mid}")
    List<Integer> canRate(int uid,int mid);

    @Insert("insert into data_ratings (uid,mid,ratting) value (#{uid},#{mid},#{score})")
    int rateMovie(int uid, int mid, int score);

    @Select("SELECT m.mid,m.name,m.score,m.pic FROM info_recs i join movie m on m.mid=i.mid where uid=#{uid} ORDER BY RAND() LIMIT 1")
    BriefMovie shakeMovie(int uid);

    @Select("select d.uid,d.gender,d.age,d.occupation " +
            "from user_sim u " +
            "join data_users d on d.uid=u.sim_uid " +
            "where u.uid=#{uid} order by u.score desc")
    List<UserData> getSimUser(int uid);

    @Select("select count(*) from data_ratings where uid=#{uid}")
    int movieCount(int uid);

    @Select("select count(*) from data_ratings where uid=#{simUid} " +
            "and mid in " +
            "(select mid from data_ratings where uid=#{uid})")
    int sameMovieCount(int uid,int simUid);

    @Select("select mid,name,score,pic from movie " +
            "where mid in" +
            "(select mid from data_ratings " +
            "where uid=#{simUid} and mid in " +
            "(select mid from data_ratings where uid=#{uid})) " +
            "order by votes desc limit 10")
    List<BriefMovie> getSameMovie(int uid,int simUid);

    @Select("select mid,name,score,pic from movie " +
            "where mid in" +
            "(select mid from data_ratings " +
            "where uid=#{simUid} and mid not in " +
            "(select mid from data_ratings where uid=#{uid})) " +
            "order by votes desc limit 10")
    List<BriefMovie> getUnSameMovie(int uid,int simUid);
}
