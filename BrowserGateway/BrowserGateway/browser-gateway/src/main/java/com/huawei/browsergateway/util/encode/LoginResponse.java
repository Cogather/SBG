package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.Code;
import com.huawei.browsergateway.common.Type;
import lombok.Data;

@Data
public class LoginResponse {
    // 基础字段
    @TlvTag(type = "int32", id = 1)
    private int type;      // 消息类型：1.登录 2.心跳等


    @TlvTag(type = "int32", id = 10)
    private int code;       // 返回状态码

    // 网络地址信息
    @TlvTag(type = "string", id = 20)
    private String tcpAddr;   // 流媒体地址

    @TlvTag(type = "string", id = 54)
    private String tlsAddr;

    public LoginResponse(String tcpAddr, String tlsAddr) {
        this.type = Type.RETURN_MEDIA;
        this.code = Code.OK;
        this.tcpAddr = tcpAddr;
        this.tlsAddr = tlsAddr;
    }
}