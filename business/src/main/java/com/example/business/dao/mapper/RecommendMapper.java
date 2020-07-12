package com.example.business.dao.mapper;

import com.example.business.pojo.BriefMovie;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecommendMapper {
    @Select("select m.mid,m.name,m.score,m.pic from movie m " +
            "join movie_recs c on m.mid=c.similar_movie " +
            "where c.mid=#{mid} order by c.similar_rate desc limit #{size}")
    List<BriefMovie> getAlsoSawMovies(int mid, int size);

    @Select("select m.mid,m.name,m.score,m.pic from movie m " +
            "join content_recs c on m.mid=c.similar_movie " +
            "where c.mid=#{mid} order by c.similar_rate desc limit #{size}")
    List<BriefMovie> getContentSimMovie(int mid, int size);

    @Select("select m.mid,m.name,m.score,m.pic from movie m " +
            "join user_recs c on m.mid=c.mid " +
            "where c.uid=#{uid} order by c.ratting desc limit #{size}")
    List<BriefMovie> getGussLikeMovies(int uid, int size);

    @Select("select m.mid,m.name,m.score,m.pic from movie m " +
            "join stream_recs c on m.mid=c.mid " +
            "where c.uid=#{uid} order by c.recs desc limit #{size}")
    List<BriefMovie> getStreamMovies(int uid, int size);

    @Select("select m.mid,m.name,m.score,m.pic from movie m " +
            "join stream_recs c on m.mid=c.mid " +
            "where c.uid=#{uid} and see=0 order by c.recs desc limit #{size}")
    List<BriefMovie> streamRecommend(int uid, int size);

    @Update("update stream_recs set see=1 where uid=#{uid}")
    int updateStatus(int uid);
}
