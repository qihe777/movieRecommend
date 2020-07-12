package com.example.business.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.business.pojo.*;
import com.example.business.service.MovieService;
import com.example.business.service.RecommendService;
import com.example.business.utils.Constant;
import com.example.business.utils.RetResult;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//用于处理电影相关的功能
@RestController
@Validated
public class MovieRestApi {

    @Autowired
    private MovieService movieService;

    @Autowired
    private RecommendService recommendService;

    private Logger logger = LoggerFactory.getLogger(MovieRestApi.class);

    //电影的详细页面
    @GetMapping("/movie/{id}")
    public Object getMovieInfo(@PathVariable Integer id) {
        logger.info("正在查询电影详细信息:{}",id);
        return RetResult.<Movie>builder()
                .status(true)
                .data(movieService.getMovieInfo(id))
                .build();
    }
    //电影的详细页面
    @GetMapping("/movie_extra/{id}")
    public Object getMovieExtraInfo(@PathVariable Integer id) {
        JSONObject jsonObject = new JSONObject();
       /* //获取电影comment_parse
        jsonObject.put("comment_parse", movieService.getCommentParse(id));*/
        //获取电影演员
        jsonObject.put("actors", movieService.getActors(id));
        //获取电影tag
        jsonObject.put("tags", movieService.getTages(id));
        //获取电影分类
        jsonObject.put("genres", movieService.getGenres(id));
        //相似电影，actor和tag和genre计算余弦相似度
        jsonObject.put("content_sim",recommendService.getContentSimMovie(id,Constant.DEFAULT_SIZE));
        //看了此电影的人还看了，基于als的物品相似度计算
        jsonObject.put("also_saw",recommendService.getAlsoSawMovies(id, Constant.DEFAULT_SIZE));

        return RetResult.<JSONObject>builder()
                .status(true)
                .data(jsonObject)
                .build();
    }

    @GetMapping("/category")
    public Object category(@Min(value = 0, message = "start太小了吧") @Max(value = 7000, message = "start太大了吧") @RequestParam(value = "start", required = false, defaultValue = "1") Integer start,
                           @Min(value = 1, message = "num太小了吧") @Max(value = 100, message = "num太大了吧") @RequestParam(value = "pagenum", required = false, defaultValue = "6") Integer pagenum,
                           @Min(value = 0, message = "sort奇怪的参数") @Max(value = 2, message = "sort奇怪的参数") @RequestParam(value = "sort", required = false, defaultValue = "0") Integer sort,
                           @Min(value = 1800, message = "year太小了吧") @Max(value = 2022, message = "year太大了吧") @RequestParam(value = "beginyear", required = false) Integer beginyear,
                           @Min(value = 1800, message = "year太小了吧") @Max(value = 2022, message = "year太大了吧") @RequestParam(value = "endyear", required = false, defaultValue = "2021") Integer endyear,
                           @Size(min = 1, max = 10, message = "genre长度得按要求走") @RequestParam(value = "genres", required = false) String genres,
                           @Size(min = 1, max = 10, message = "place长度得按要求走") @RequestParam(value = "place", required = false) String place,
                           @Size(min = 1, max = 10, message = "tag长度得按要求走") @RequestParam(value = "tag", required = false) String tag) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("start", start);
        myMap.put("pagenum", pagenum);
        switch (sort) {
            case 2:
                myMap.put("sort", "m.score desc");
                break;
            case 1:
                myMap.put("sort", "m.year desc");
                break;
            default:
                myMap.put("sort", "m.votes desc");
        }
        myMap.put("endyear", endyear);
        if (genres != null)
            myMap.put("genres", genres);
        if (beginyear != null)
            myMap.put("beginyear", beginyear);
        if (place != null)
            myMap.put("place", place);
        if (tag != null)
            myMap.put("tag", tag);

        return RetResult.<PageInfo<BriefMovie>>builder()
                .data(movieService.category(myMap))
                .status(true)
                .build();
    }

    //电影评论分页
    @GetMapping("/comment")
    public Object getMovieInfo(@RequestParam(value = "mid", required = true) Integer mid,
                               @Min(value = 0, message = "start太小了吧") @Max(value = 50, message = "start太大了吧") @RequestParam(value = "start", required = false, defaultValue = "1") Integer start,
                               @Min(value = 1, message = "num太小了吧") @Max(value = 20, message = "size太大了吧") @RequestParam(value = "size", required = false, defaultValue = "6") Integer size) {


        return RetResult.<PageInfo<Comment>>builder()
                .status(true)
                .data(movieService.getComment(mid,start,size))
                .build();
    }


    //模糊搜索功能
    @GetMapping("/search")
    public Object searchMovie(@RequestParam(value = "type", required = false, defaultValue = "0") Integer type,
                              @RequestParam(value = "text") String text) throws Exception {
        
        return RetResult.<List<BriefMovieWithActor>>builder()
                .status(true)
                .data(recommendService.getFuzzyMovies(text,type))
                .build();
    }

    //actor信息
    @GetMapping("/actor/{aid}")
    public Object actorInfo(@PathVariable Integer aid) {

        return RetResult.<BriefActor>builder()
                .status(true)
                .data(movieService.getActor(aid))
                .build();
    }


    //actor出演电影分页
    @GetMapping("/actor_movie")
    public Object getActorMovie(@RequestParam(value = "aid") Integer aid,
                               @Min(value = 0, message = "start太小了吧") @Max(value = 50, message = "start太大了吧") @RequestParam(value = "start", required = false, defaultValue = "1") Integer start,
                               @Min(value = 1, message = "num太小了吧") @Max(value = 20, message = "size太大了吧") @RequestParam(value = "size", required = false, defaultValue = "6") Integer size) {


        return RetResult.<PageInfo<BriefMovieWithActor>>builder()
                .status(true)
                .data(movieService.getActorMovies(aid,start,size))
                .build();
    }

    @GetMapping("/actor/hot")
    public Object hotActor() {

        return RetResult.<List<Actor>>builder()
                .status(true)
                .data(movieService.getHotActor())
                .build();
    }
}
