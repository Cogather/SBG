package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.Type;
import lombok.Data;

@Data
public class Ack {
    // 基础字段
    @TlvTag(type = "int32", id = 1)
    private int type;      // 消息类型：1.登录 2.心跳等

    // 应答相关
    @TlvTag(type = "int32", id = 9)
    private int ackType;    // 服务端通用应答值

    @TlvTag(type = "int32", id = 10)
    private int code;       // 返回状态码

    public Ack(int ackType, int code) {
        this.type = Type.ACK;
        this.ackType = ackType;
        this.code = code;
    }
}
