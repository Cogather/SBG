package com.huawei.browsergateway.exception;

import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandle {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandle.class);

    /**
     * 处理所有未捕获异常，返回统一错误响应
     *
     * @param request 当前HTTP请求
     * @param ex      捕获的异常
     * @return 包含错误信息的通用响应结果
     */
    @ExceptionHandler(value = {Exception.class})
    public CommonResult<String> handleException(HttpServletRequest request, Exception ex) {
        log.error("system error:{}", ex.getMessage(), ex);
        CommonResult<String> result = CommonResult.error(ResultCode.FAIL);
        result.setData(ex.getMessage());
        return result;
    }
}
