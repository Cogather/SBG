package com.huawei.browsergateway.common.response;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import lombok.Data;

/**
 * 通用响应类
 */
@Data
public class BaseResponse<T> {
    
    private Integer code;
    private String message;
    private T data;
    
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setMessage(ErrorCodeEnum.SUCCESS.getMessage());
        response.setData(data);
        return response;
    }
    
    public static <T> BaseResponse<T> success() {
        return success(null);
    }
    
    public static <T> BaseResponse<T> fail(Integer code, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
    
    public static <T> BaseResponse<T> fail(String message) {
        return fail(ErrorCodeEnum.INTERNAL_ERROR.getCode(), message);
    }
    
    /**
     * 基于错误码枚举创建失败响应
     */
    public static <T> BaseResponse<T> fail(ErrorCodeEnum errorCodeEnum) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(errorCodeEnum.getCode());
        response.setMessage(errorCodeEnum.getMessage());
        return response;
    }
    
    /**
     * 基于错误码枚举创建失败响应（带自定义消息）
     */
    public static <T> BaseResponse<T> fail(ErrorCodeEnum errorCodeEnum, String customMessage) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(errorCodeEnum.getCode());
        response.setMessage(customMessage);
        return response;
    }
}
