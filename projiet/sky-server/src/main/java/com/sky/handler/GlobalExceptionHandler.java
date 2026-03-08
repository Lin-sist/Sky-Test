package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    //补SQL唯一键冲突异常的处理
    @ExceptionHandler
    public Result exceptionHandler(org.springframework.dao.DuplicateKeyException ex) {
        log.error("异常信息：{}", ex.getMessage());
        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            String duplicateValue = message.split("'")[1];
            return Result.error(duplicateValue + MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
