package com.example.business.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.business.pojo.UserWx;
import com.example.business.service.UserWxService;
import com.example.business.utils.HttpClientUtil;
import com.example.business.utils.RetResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.Security;

@RestController
@RequestMapping("/wx")
public class WxUserController {

    private Logger logger = LoggerFactory.getLogger(WxUserController.class);

    @Autowired
    UserWxService userWxService;

    @PostMapping("/login")
    public Object wxUserLogin(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "rawData", required = false) String rawData,
                             @RequestParam(value = "signature", required = false) String signature,
                             @RequestParam(value = "encrypteData", required = false) String encrypteData,
                             @RequestParam(value = "iv", required = false) String iv) throws Exception {
        // 用户非敏感信息：rawData
        // 签名：signature
        // 1.接收小程序发送的code
        // 2.开发者服务器 登录凭证校验接口 appi + appsecret + code
        JSONObject SessionKeyOpenId = userWxService.getSessionKeyOrOpenId(code);
        // 3.接收微信接口服务 获取返回的参数
        String openid = SessionKeyOpenId.getString("openid");
        String sessionKey = SessionKeyOpenId.getString("session_key");

        // 4.校验签名 小程序发送的签名signature与服务器端生成的签名signature2 = sha1(rawData + sessionKey)
        String signature2 = DigestUtils.sha1Hex(rawData + sessionKey);
        if (!signature.equals(signature2)) {
            throw new Exception("签名校验失败");
        }
        //5.判断用户是否是新用户，是的话，将用户信息存到数据库；不是的话，更新最新登录时间
        String token=userWxService.processUserData(openid,sessionKey,rawData);
        //encrypteData比rowData多了appid和openid
        //JSONObject userInfo = getUserInfo(encrypteData, sessionKey, iv);
        //6. 把新的skey返回给小程序
        return RetResult.builder().status(true).data(token).build();
    }
}
