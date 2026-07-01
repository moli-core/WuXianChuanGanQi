package com.wireless.handler;

import com.wireless.model.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return ApiResult.badRequest(msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<?> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
