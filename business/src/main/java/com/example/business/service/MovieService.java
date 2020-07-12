package com.example.business.service;

import com.example.business.pojo.*;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface MovieService {
    PageInfo<BriefMovie> category(Map<String, Object> myMap);
    Movie getMovieInfo(int mid);
    List<Actor> getActors(int mid);
    List<String> getTages(int mid);
    List<String> getGenres(int mid);
    String getCommentParse(int mid);
    PageInfo<Comment> getComment(int mid,int start,int size);
    BriefActor getActor(int aid);
    PageInfo<BriefMovieWithActor> getActorMovies(int aid,int start,int size);
    List<Actor> getHotActor();
}
