package com.huawei.browsergateway.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SocketKeyConst 测试类
 * 测试 Socket 常量定义
 */
@DisplayName("SocketKeyConst tests")
class SocketKeyConstTest {

    @Test
    @DisplayName("USER_ID_KEY constant value")
    void testUserIdKeyValue() {
        // Then
        assertEquals("userId", SocketKeyConst.USER_ID_KEY, "USER_ID_KEY 常量值应该为 'userId'");
    }

    @Test
    @DisplayName("USER_ID_KEY is not null")
    void testUserIdKeyNotNull() {
        // Then
        assertNotNull(SocketKeyConst.USER_ID_KEY, "USER_ID_KEY 常量不应该为null");
    }

    @Test
    @DisplayName("USER_ID_KEY is not empty string")
    void testUserIdKeyNotEmpty() {
        // Then
        assertFalse(SocketKeyConst.USER_ID_KEY.isEmpty(), "USER_ID_KEY 常量不应该为空字符串");
    }
}
