package com.huawei.browsergateway.entity.request;

import lombok.Data;

/**
 * 删除用户数据请求
 */
@Data
public class DeleteUserDataRequest {
    private String imei;
    private String imsi;
}
