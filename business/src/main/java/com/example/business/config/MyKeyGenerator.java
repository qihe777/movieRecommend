package com.example.business.config;


import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class MyKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if(params.length==0){
            return SimpleKey.EMPTY;
        }
        Object param=params[0];
        if(param instanceof Map){
            StringBuilder builder=new StringBuilder();
            String sp="-";
            builder.append(method.getName()).append(sp);
            Map<String, Object> myMap=(Map<String, Object>) param;
            if(myMap.isEmpty()){
                return builder.toString();
            }
            for(Object value: myMap.values()){
                builder.append(value.toString()).append(sp);
            }
            return builder.toString();
        }
        return new SimpleKey(params);
    }
}
