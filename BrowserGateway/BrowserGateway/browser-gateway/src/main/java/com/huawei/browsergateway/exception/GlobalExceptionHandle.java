package com.huawei.browsergateway.exception;

import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandle {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandle.class);
    @ExceptionHandler(value = {Exception.class})
    public CommonResult<String> handleException(HttpServletRequest request, Exception ex) {
        log.error("system error:{}", ex.getMessage(), ex);
        CommonResult<String> result = CommonResult.error(ResultCode.FAIL);
        result.setData(ex.getMessage());
        return result;
    }
}