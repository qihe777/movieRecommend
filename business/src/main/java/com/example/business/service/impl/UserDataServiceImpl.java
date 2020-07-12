package com.example.business.service.impl;

import com.example.business.dao.mapper.UserDataMapper;
import com.example.business.pojo.*;
import com.example.business.service.UserDataService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserDataServiceImpl implements UserDataService {

    @Autowired
    UserDataMapper userDataMapper;

    @Override
    public PageInfo<UserData> getUserList(int start, int size, Map<String, String> map) {
        PageHelper.startPage(start, size);
        return new PageInfo<>(userDataMapper.getUserList(map));
    }

    @Override
    public String getToken(int uid) {
        String token = UUID.randomUUID().toString();
        userDataMapper.updateToken(uid, token);
        return token;
    }

    @Override
    public PageInfo<BriefMovieWithTime> recentRating(int start, int size, int uid) {
        PageHelper.startPage(start, size);
        return new PageInfo<>(userDataMapper.recentRating(uid));
    }

    @Override
    public UserData getUserInfo(int uid) {
        return userDataMapper.getUserInfo(uid);
    }

    @Override
    public List<Count> genresCount(int uid) {
        return userDataMapper.genresCount(uid);
    }

    @Override
    public List<Count> tagCount(int uid) {
        return userDataMapper.tagCount(uid);
    }

    @Override
    public List<Actor> actorCount(int uid) {
        return userDataMapper.actorCount(uid);
    }

    @Override
    public int canRate(int uid, int mid) throws Exception {
        List<Integer> list = userDataMapper.canRate(uid, mid);
        if(list.isEmpty())
            throw new Exception("评分表为空，可以评分");
        return list.get(0);
    }

    @Override
    public int rateMovie(int uid, int mid, int score) {
        return userDataMapper.rateMovie(uid, mid, score);
    }

    @Override
    public BriefMovie shakeMovie(int uid) {
        return userDataMapper.shakeMovie(uid);
    }

    @Override
    public List<UserData> getSimUser(int uid) {
        return userDataMapper.getSimUser(uid);
    }

    @Override
    public List<BriefMovie> getSameMovie(int uid, int simUid) {
        return userDataMapper.getSameMovie(uid, simUid);
    }

    @Override
    public List<BriefMovie> getUnSameMovie(int uid, int simUid) {
        return userDataMapper.getUnSameMovie(uid, simUid);
    }

    @Override
    public int sameMovieCount(int uid, int simUid) {
        return userDataMapper.sameMovieCount(uid, simUid);
    }

    @Override
    public int movieCount(int uid) {
        return userDataMapper.movieCount(uid);
    }
}
