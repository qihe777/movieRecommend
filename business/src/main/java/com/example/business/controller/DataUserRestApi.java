package com.example.business.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.business.pojo.BriefMovie;
import com.example.business.pojo.BriefMovieWithTime;
import com.example.business.pojo.UserData;
import com.example.business.service.RecommendService;
import com.example.business.service.UserDataService;
import com.example.business.utils.Constant;
import com.example.business.utils.RetResult;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//用于用户相关的rest接口
@RestController
@RequestMapping("/data")
public class DataUserRestApi {
    private Logger logger = LoggerFactory.getLogger(DataUserRestApi.class);
    @Autowired
    private RecommendService recommendService;

    @Autowired
    private UserDataService userDataService;


    @GetMapping("/userlist")
    public Object getUserList(@RequestParam(value = "start", required = false, defaultValue = "1") Integer start,
                              @RequestParam(value = "size", required = false, defaultValue = "6") Integer size,
                              @RequestParam(value = "gender", required = false) String gender,
                              @RequestParam(value = "age", required = false) String age,
                              @RequestParam(value = "occupation", required = false) String occupation) {
        Map<String, String> myMap = new HashMap<>();
        if (gender != null)
            myMap.put("gender", gender);
        if (age != null)
            myMap.put("age", age);
        if (occupation != null)
            myMap.put("occupation", occupation);

        return RetResult.<PageInfo<UserData>>builder()
                .data(userDataService.getUserList(start, size, myMap))
                .status(true)
                .build();
    }

    //返回token以及用户个人信息
    @PostMapping("/login")
    public Object dataUserLogin(@RequestParam(value = "uid", required = true) Integer uid) {
        JSONObject jsonObject = new JSONObject();
        //获取用户信息
        jsonObject.put("user_info", userDataService.getUserInfo(uid));
        //获取用户token
        jsonObject.put("token", userDataService.getToken(uid));
        return RetResult.<JSONObject>builder()
                .data(jsonObject)
                .status(true)
                .build();
    }

    //用户其他附加信息
    @GetMapping("/user_extra/{uid}")
    public Object getUserExtraInfo(@PathVariable Integer uid) {
        JSONObject jsonObject = new JSONObject();
        //1.获取才你喜欢离线推荐
        jsonObject.put("gusslike", recommendService.getGussLikeMovies(uid, Constant.DEFAULT_SIZE));
        //2.获取实时推荐
        jsonObject.put("stream", recommendService.getStreamMovies(uid, Constant.DEFAULT_SIZE));
        //3.评分里电影类型统计，前10个
        jsonObject.put("genres", userDataService.genresCount(uid));
        //4.评分里电影tag统计，前10个
        jsonObject.put("tags", userDataService.tagCount(uid));
        //5.评分里电影明星统计，前6个
        jsonObject.put("actors", userDataService.actorCount(uid));

        return RetResult.<JSONObject>builder()
                .data(jsonObject)
                .status(true)
                .build();
    }

    //用户最近观看的电影
    @GetMapping("/recent_rating")
    public Object recentRating(@RequestParam(value = "start", required = false, defaultValue = "1") Integer start,
                               @RequestParam(value = "size", required = false, defaultValue = "6") Integer size,
                               @RequestParam(value = "uid") Integer uid) {

        return RetResult.<PageInfo<BriefMovieWithTime>>builder()
                .data(userDataService.recentRating(start, size, uid))
                .status(true)
                .build();
    }

    /**
     * 获取实时推荐信息的接口
     *
     * @return
     */
    @GetMapping("/stream/{uid}")
    public Object getStreamMovies(@PathVariable Integer uid) throws Exception {

        return RetResult.<List<BriefMovie>>builder()
                .data(recommendService.streamRecommend(uid, Constant.DEFAULT_SIZE))
                .status(true)
                .build();
    }

    @GetMapping("/can_rate/{uid}/{mid}")
    public Object canRate(@PathVariable Integer uid,@PathVariable Integer mid) throws Exception {

        return RetResult.<Integer>builder()
                .data(userDataService.canRate(uid, mid))
                .status(true)
                .build();
    }

    //给此电影打分
    @PostMapping("/rate")
    public Object rateMovie(@RequestParam(value = "uid") Integer uid,
                            @RequestParam(value = "mid") Integer mid,
                            @RequestParam(value = "score") Integer score) throws Exception {
        int result = userDataService.rateMovie(uid, mid, score);
        if (result == 0) {
            throw new Exception("插入评分错误");
        }
        //输出埋点日志,字符串拼接需要优化
        logger.info("USER_RATING_LOG_PREFIX:{}|{}|{}|{}", uid, mid, score, System.currentTimeMillis());
        return RetResult.<String>builder()
                .status(true)
                .data("插入成功")
                .build();
    }

    @GetMapping("/shake/{uid}")
    public Object getShake(@PathVariable Integer uid) {
        return RetResult.<BriefMovie>builder()
                .data(userDataService.shakeMovie(uid))
                .status(true)
                .build();
    }
    @GetMapping("/sim/{uid}")
    public Object simUser(@PathVariable Integer uid) {
        return RetResult.<List<UserData>>builder()
                .data(userDataService.getSimUser(uid))
                .status(true)
                .build();
    }
    @GetMapping("/sim_extra/{uid}/{simUid}")
    public Object simExtra(@PathVariable Integer uid,@PathVariable Integer simUid) {
        JSONObject jsonObject = new JSONObject();
        //1.获取自己评价的电影总数
        jsonObject.put("myMovieCount", userDataService.movieCount(uid));
        //2.获取对面电影评价总数
        jsonObject.put("simMovieCount", userDataService.movieCount(simUid));
        //3.两个人共同评价电影总数
        jsonObject.put("sameMovieCount", userDataService.sameMovieCount(uid,simUid));
        //4.两个人共同评价电影前10个
        jsonObject.put("sameMovie", userDataService.getSameMovie(uid,simUid));
        //5.当前用户未评价的电影
        jsonObject.put("unSameMovie", userDataService.getUnSameMovie(uid,simUid));
        return RetResult.<JSONObject>builder()
                .data(jsonObject)
                .status(true)
                .build();
    }
}
