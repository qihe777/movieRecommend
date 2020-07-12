package com.example.business.config.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class MessageInterceptor implements HandlerInterceptor {
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        for(Map.Entry<String,Object> entry:modelAndView.getModel().entrySet()){
            System.out.println(entry.getKey()+":"+entry.getValue());
        }
    }
}
