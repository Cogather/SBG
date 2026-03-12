package com.huawei.browsergateway.entity;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 基础响应
 */
@Data
public class BaseResponse {
    /** 响应码 */
    @Alias("code")
    int code;
    /** 响应消息 */
    @Alias("msg")
    String message;
}
