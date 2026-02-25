package com.huawei.browsergateway.exception.handler;

import com.huawei.browsergateway.common.response.BaseResponse;
import com.huawei.browsergateway.exception.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {}", e.getErrorMessage());
        return ResponseEntity.status(HttpStatus.OK)
            .body(BaseResponse.fail(e.getErrorCode(), e.getErrorMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.fail(500, "Internal server error"));
    }
}
