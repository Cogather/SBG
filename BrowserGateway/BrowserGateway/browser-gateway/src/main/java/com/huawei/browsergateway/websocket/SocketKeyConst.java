package com.huawei.browsergateway.websocket;

/**
 * WebSocket 会话属性键常量定义
 */
public final class SocketKeyConst {

    /** 用户ID属性键，用于在 Session 中存储和获取用户ID */
    public static final String USER_ID_KEY = "userId";

    /** 私有构造函数，防止实例化 */
    private SocketKeyConst() {
        throw new AssertionError("常量类不应被实例化");
    }
}

