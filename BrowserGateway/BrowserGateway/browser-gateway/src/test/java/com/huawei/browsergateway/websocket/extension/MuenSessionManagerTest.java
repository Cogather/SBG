package com.huawei.browsergateway.websocket.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yeauty.pojo.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MuenSessionManager 测试类
 * 测试 Muen 会话管理器功能
 */
@DisplayName("MuenSessionManager 测试")
class MuenSessionManagerTest {

    private MuenSessionManager muenSessionManager;

    @Mock
    private Session mockSession1;

    @Mock
    private Session mockSession2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        muenSessionManager = new MuenSessionManager();
    }

    @Test
    @DisplayName("继承功能 - 添加会话")
    void test继承功能_添加会话() {
        // Given
        String key = "user123";

        // When
        muenSessionManager.addSession(key, mockSession1);

        // Then
        Session retrievedSession = muenSessionManager.getSession(key);
        assertNotNull(retrievedSession, "添加的会话应该能被获取");
        assertEquals(mockSession1, retrievedSession, "获取的会话应该与添加的会话相同");
    }

    @Test
    @DisplayName("继承功能 - 替换已存在的会话")
    void test继承功能_替换已存在的会话() {
        // Given
        String key = "user123";
        muenSessionManager.addSession(key, mockSession1);

        // When
        muenSessionManager.addSession(key, mockSession2);

        // Then
        Session retrievedSession = muenSessionManager.getSession(key);
        assertEquals(mockSession2, retrievedSession, "新会话应该替换旧会话");
        verify(mockSession1, times(1)).close();
    }

    @Test
    @DisplayName("继承功能 - 获取会话")
    void test继承功能_获取会话() {
        // Given
        String key = "user123";
        muenSessionManager.addSession(key, mockSession1);

        // When
        Session retrievedSession = muenSessionManager.getSession(key);

        // Then
        assertNotNull(retrievedSession, "应该能获取到已添加的会话");
        assertEquals(mockSession1, retrievedSession, "获取的会话应该正确");
    }

    @Test
    @DisplayName("继承功能 - 删除会话")
    void test继承功能_删除会话() {
        // Given
        String key = "user123";
        muenSessionManager.addSession(key, mockSession1);

        // When
        muenSessionManager.del(key);

        // Then
        Session retrievedSession = muenSessionManager.getSession(key);
        assertNull(retrievedSession, "删除后应该无法获取到会话");
        verify(mockSession1, times(1)).close();
    }

    @Test
    @DisplayName("继承功能 - 获取所有会话键")
    void test继承功能_获取所有会话键() {
        // Given
        muenSessionManager.addSession("user1", mockSession1);
        muenSessionManager.addSession("user2", mockSession2);

        // When
        var allKeys = muenSessionManager.allSessions();

        // Then
        assertNotNull(allKeys, "会话键集合不应为null");
        assertEquals(2, allKeys.size(), "应该有2个会话");
        assertTrue(allKeys.contains("user1"), "应该包含user1");
        assertTrue(allKeys.contains("user2"), "应该包含user2");
    }
}
