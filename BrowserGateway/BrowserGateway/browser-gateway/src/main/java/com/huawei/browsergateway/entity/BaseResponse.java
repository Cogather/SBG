package com.huawei.browsergateway.entity;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 基础响应
 */
@Data
public class BaseResponse {
    @Alias("code")
    int code;
    @Alias("msg")
    String message;
}
