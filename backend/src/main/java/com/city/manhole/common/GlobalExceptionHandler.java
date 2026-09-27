package com.city.manhole.common;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> biz(BizException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ApiResponse<Void> badRequest(Exception e) {
        return ApiResponse.error(400, "请求参数有误: " + e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> tooLarge(MaxUploadSizeExceededException e) {
        return ApiResponse.error(400, "上传文件超过大小限制");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> other(Exception e) {
        e.printStackTrace();
        return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}
