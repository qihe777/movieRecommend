package com.example.business.dao.mapper;

import com.example.business.dao.MovieCategoryProvider;
import com.example.business.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MovieMapper {
    @SelectProvider(type = MovieCategoryProvider.class, method = "joinSql")
    List<BriefMovie> findShowMovie(Map<String, Object> map);

    @Select("select mid,pic,name,year,regions,duration,imdb_id,score,votes," +
            "storyline,showname,five,four,three,two,one " +
            "from movie where mid=#{id}")
    Movie getMovieByid(int id);

    @Select("select comment,rating,vote_count,create_time,user_id,user_name,user_avatar " +
            "from comment where mid=#{id}")
    List<Comment> getComment(int id);

    @Select("select u.aid,u.role,a.name,a.pic from actors_union u " +
            "join actor a on a.id=u.aid " +
            "where u.mid=#{mid} " +
            "order by u.id limit 6")
    List<Actor> getActors(int mid);

    @Select("select name from tag_union where mid=#{mid} limit 6")
    List<String> getTages(int mid);

    @Select("select name from genres_union where mid=#{mid} limit 6")
    List<String> getGenres(int mid);

    @Select("select parse from comment_parse where mid=#{mid} limit 1")
    String getCommentParse(int mid);

    @Select("select id,name,pic from actor where id=#{aid}")
    BriefActor getActor(int aid);

    @Select("select m.mid,m.name,m.score,m.pic,a.role actor " +
            "from actors_union a " +
            "join movie m on m.mid=a.mid " +
            "where a.aid=#{aid} " +
            "order by m.votes desc")
    List<BriefMovieWithActor> getActorMovies(int aid);

    @Select("select a.id aid,tmp.count role,a.name,a.pic " +
            "from actor a join " +
            "(select aid,count(*) count from actors_union " +
            "group by aid order by count desc limit 20) tmp " +
            "on tmp.aid=a.id")
    List<Actor> getHotActor();
}
