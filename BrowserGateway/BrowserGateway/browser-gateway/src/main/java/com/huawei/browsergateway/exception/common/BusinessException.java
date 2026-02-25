package com.huawei.browsergateway.exception.common;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer errorCode;
    private final String errorMessage;
    
    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = errorCodeEnum.getMessage();
    }
    
    public BusinessException(ErrorCodeEnum errorCodeEnum, String message) {
        super(message);
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = message;
    }
    
    public BusinessException(Integer errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
