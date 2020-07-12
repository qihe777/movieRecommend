package com.example.business.service;


import com.example.business.pojo.BriefMovie;
import com.example.business.pojo.BriefMovieWithActor;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface RecommendService {
    List<BriefMovie> getAlsoSawMovies(int mid, int size);

    List<BriefMovie> getContentSimMovie(int mid, int size);

    List<BriefMovie> getGussLikeMovies(int uid, int size);

    List<BriefMovie> getStreamMovies(int uid, int size);

    List<BriefMovieWithActor> getFuzzyMovies(String text,int type) throws Exception;

    List<BriefMovie> streamRecommend(int uid, int size) throws Exception;
}
