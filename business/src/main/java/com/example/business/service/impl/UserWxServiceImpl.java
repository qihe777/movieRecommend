package com.example.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.business.controller.MovieRestApi;
import com.example.business.dao.mapper.UserWxMapper;
import com.example.business.pojo.UserWx;
import com.example.business.service.UserWxService;
import com.example.business.utils.HttpClientUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.*;

@Service
public class UserWxServiceImpl implements UserWxService {

    private Logger logger = LoggerFactory.getLogger(UserWxServiceImpl.class);
    @Autowired
    UserWxMapper userWxMapper;



    //5.判断用户是否是新用户，是的话，将用户信息存到数据库；不是的话，更新最新登录时间
    @Override
    public String processUserData(String oppenid,String sessionKey, String userData) {
        // uuid生成唯一key，用于维护微信小程序用户与服务端的会话
        String token = UUID.randomUUID().toString();
        int exist = userWxMapper.ifExist(oppenid);
        if(exist==1){
            // 已存在，更新用户登录时间
            //user.setLastVisitTime(new Date());
            // 重新设置会话token
            int result=userWxMapper.updateToken(oppenid,token);
            logger.info("更新微信用户成功，返回结果：{}",result);
            return token;
        }

        JSONObject rawDataJson = JSON.parseObject(userData);
        UserWx user=UserWx.builder()
                .open_id(oppenid)
                .session_key(sessionKey)
                .token(token)
                .avatar_url(rawDataJson.getString("avatarUrl"))
                .gender(rawDataJson.getInteger("gender"))
                .city(rawDataJson.getString("city"))
                .country(rawDataJson.getString("country"))
                .nick_name(rawDataJson.getString("nickName"))
                .province(rawDataJson.getString("province"))
                .build();

        userWxMapper.insertUser(user);
        return token;
    }

    @Override
    public JSONObject getSessionKeyOrOpenId(String code) {
        String requestUrl = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, String> requestUrlParam = new HashMap<>();
        // https://mp.weixin.qq.com/wxopen/devprofile?action=get_profile&token=164113089&lang=zh_CN
        //小程序appId
        requestUrlParam.put("appid", "小程序appId");
        //小程序secret
        requestUrlParam.put("secret", "小程序secret");
        //小程序端返回的code
        requestUrlParam.put("js_code", code);
        //默认参数
        requestUrlParam.put("grant_type", "authorization_code");
        //发送post请求读取调用微信接口获取openid用户唯一标识
        return JSON.parseObject(HttpClientUtil.doPost(requestUrl, requestUrlParam,logger));
    }

    @Override
    public JSONObject getUserInfo(String encryptedData, String sessionKey, String iv) {
        // 被加密的数据
        byte[] dataByte = Base64.getDecoder().decode(encryptedData);
        // 加密秘钥
        byte[] keyByte = Base64.getDecoder().decode(sessionKey);
        // 偏移量
        byte[] ivByte = Base64.getDecoder().decode(iv);
        try {
            // 如果密钥不足16位，那么就补足.  这个if 中的内容很重要
            int base = 16;
            if (keyByte.length % base != 0) {
                int groups = keyByte.length / base + (keyByte.length % base != 0 ? 1 : 0);
                byte[] temp = new byte[groups * base];
                Arrays.fill(temp, (byte) 0);
                System.arraycopy(keyByte, 0, temp, 0, keyByte.length);
                keyByte = temp;
            }
            // 初始化
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            parameters.init(new IvParameterSpec(ivByte));
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);// 初始化
            byte[] resultByte = cipher.doFinal(dataByte);
            if (null != resultByte && resultByte.length > 0) {
                String result = new String(resultByte, StandardCharsets.UTF_8);
                return JSON.parseObject(result);
            }
        } catch (Exception e) {
            logger.error("出现错误：",e);
        }
        return null;
    }
}
