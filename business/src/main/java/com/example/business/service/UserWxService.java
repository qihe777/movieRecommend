package com.example.business.service;

import com.alibaba.fastjson.JSONObject;

public interface UserWxService {
    String processUserData(String oppenid,String sessionKey, String userData);
    JSONObject getSessionKeyOrOpenId(String code);
    JSONObject getUserInfo(String encryptedData, String sessionKey, String iv);
}
