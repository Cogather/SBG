package com.huawei.browsergateway.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SocketKeyConst 测试类
 * 测试 Socket 常量定义
 */
@DisplayName("SocketKeyConst 测试")
class SocketKeyConstTest {

    @Test
    @DisplayName("USER_ID_KEY 常量值验证")
    void testUserIdKey常量值() {
        // Then
        assertEquals("userId", SocketKeyConst.USER_ID_KEY, "USER_ID_KEY 常量值应该为 'userId'");
    }

    @Test
    @DisplayName("USER_ID_KEY 常量不为null")
    void testUserIdKey不为null() {
        // Then
        assertNotNull(SocketKeyConst.USER_ID_KEY, "USER_ID_KEY 常量不应该为null");
    }

    @Test
    @DisplayName("USER_ID_KEY 常量不为空字符串")
    void testUserIdKey不为空字符串() {
        // Then
        assertFalse(SocketKeyConst.USER_ID_KEY.isEmpty(), "USER_ID_KEY 常量不应该为空字符串");
    }
}
