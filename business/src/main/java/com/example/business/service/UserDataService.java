package com.example.business.service;

import com.example.business.pojo.*;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface UserDataService {
    PageInfo<UserData> getUserList(int start, int size, Map<String, String> map);

    String getToken(int uid);

    PageInfo<BriefMovieWithTime> recentRating(int start, int size, int uid);

    UserData getUserInfo(int uid);

    List<Count> genresCount(int uid);

    List<Count> tagCount(int uid);

    List<Actor> actorCount(int uid);

    int canRate(int uid,int mid) throws Exception;

    int rateMovie(int uid, int mid, int score);

    BriefMovie shakeMovie(int uid);

    List<UserData> getSimUser(int uid);

    List<BriefMovie> getSameMovie(int uid, int simUid);

    List<BriefMovie> getUnSameMovie(int uid, int simUid);

    int sameMovieCount(int uid, int simUid);

    int movieCount(int uid);
}
