package com.example.business.config.interceptor;

import com.example.business.config.annotation.MethodScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

public class TokenInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);

  /*  @Autowired
    LoginUsrService loginUsrService;

    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        String token = httpServletRequest.getHeader("token");// 从 http 请求头中取出 token
        // 如果不是映射到方法直接通过
        if (!(object instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) object;
        Method method = handlerMethod.getMethod();
        //检查是否有需要权限的注释，没有有则跳过认证
        if (method.isAnnotationPresent(MethodScope.class)) {
            MethodScope methodScope = method.getAnnotation(MethodScope.class);
            // 执行认证
            if (token == null) {
                throw new RuntimeException("1000");
            }
            // 获取 token 中的 user id，没有id则报回1002异常
            String userId;
            //先进行解码
            try {
                userId = JWT.decode(token).getAudience().get(0);
            } catch (JWTDecodeException j) {
                // 如果格式不正确返回1002错误
                throw new RuntimeException("1002");
            }
            LoginUsr loginUsr = loginUsrService.getLoginUsrById(userId);
            //用户不存在
            if (loginUsr == null) {
                throw new RuntimeException("1002");
            }
            // 验证 token
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(loginUsr.getSecret())).build();
            try {
                jwtVerifier.verify(token);
            } catch (TokenExpiredException e) {
                throw new RuntimeException("1001");
            } catch (JWTVerificationException e) {
                //如果token错误
                throw new RuntimeException("1002");
            }
            //如果权限比当前用户权限大
            if (methodScope.scope() > loginUsr.getScope()) {
                throw new RuntimeException("1003");
            }
            logger.info("token验证成功");
            return true;
        }
        logger.info("无需验证token");
        return true;
    }*/
}
