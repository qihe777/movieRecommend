package com.example.business.config.interceptor;

import com.example.business.utils.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
public class GlobalExceptionHandler {


    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
        ErrorResult.ErrorResultBuilder builder = ErrorResult.builder().status(false);
        logger.error("出现错误：", e);
        builder.errorMsg(e.getMessage());
        /*if (e instanceof ConstraintViolationException){
            logger.error("参数不匹配：",e);

        }*/
        return builder.build();
    }
}
