package com.lee.graphic_reasoning_server.handler;

import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> biz(BizException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> all(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统异常：" + e.getMessage());
    }
}
