package com.example.business.service.impl;

import com.example.business.config.MyKeyGenerator;
import com.example.business.dao.mapper.MovieMapper;
import com.example.business.pojo.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.example.business.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieMapper movieMapper;

    @Override
    @Cacheable(value = "movie", keyGenerator = "myKeyGenerator")
    public PageInfo<BriefMovie> category(Map<String, Object> myMap) {
        //设置分页参数
        PageHelper.startPage(((Integer) myMap.get("start")), (Integer) myMap.get("pagenum"));
        //使用对象包装返回结果
        return new PageInfo<>(movieMapper.findShowMovie(myMap));
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public Movie getMovieInfo(int mid) {
        return movieMapper.getMovieByid(mid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public List<Actor> getActors(int mid) {
        return movieMapper.getActors(mid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public List<String> getTages(int mid) {
        return movieMapper.getTages(mid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public List<String> getGenres(int mid) {
        return movieMapper.getGenres(mid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public String getCommentParse(int mid) {
        return movieMapper.getCommentParse(mid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid+'-'+#start+'-'+#size")
    public PageInfo<Comment> getComment(int mid, int start, int size) {
        //设置分页参数
        PageHelper.startPage(start, size);
        //使用对象包装返回结果
        return new PageInfo<>(movieMapper.getComment(mid));
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#aid")
    public BriefActor getActor(int aid) {
        return movieMapper.getActor(aid);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#aid+'-'+#start+'-'+#size")
    public PageInfo<BriefMovieWithActor> getActorMovies(int aid, int start, int size) {
        PageHelper.startPage(start, size);
        return new PageInfo<>(movieMapper.getActorMovies(aid));
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName")
    public List<Actor> getHotActor() {
        return movieMapper.getHotActor();
    }

}
