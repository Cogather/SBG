package com.huawei.browsergateway.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yeauty.pojo.Session;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SessionSet 测试类
 * 测试 WebSocket 会话集合管理功能
 */
@DisplayName("SessionSet 测试")
class SessionSetTest {

    private SessionSet sessionSet;

    @Mock
    private Session mockSession1;

    @Mock
    private Session mockSession2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sessionSet = new SessionSet();
    }

    @Test
    @DisplayName("添加会话 - 正常添加")
    void testAddSession_正常添加() {
        // Given
        String key = "user123";

        // When
        sessionSet.addSession(key, mockSession1);

        // Then
        Session retrievedSession = sessionSet.getSession(key);
        assertNotNull(retrievedSession, "添加的会话应该能被获取");
        assertEquals(mockSession1, retrievedSession, "获取的会话应该与添加的会话相同");
        verify(mockSession1, never()).close();
    }

    @Test
    @DisplayName("添加会话 - 替换已存在的会话")
    void testAddSession_替换已存在的会话() {
        // Given
        String key = "user123";
        sessionSet.addSession(key, mockSession1);

        // When
        sessionSet.addSession(key, mockSession2);

        // Then
        Session retrievedSession = sessionSet.getSession(key);
        assertEquals(mockSession2, retrievedSession, "新会话应该替换旧会话");
        verify(mockSession1, times(1)).close();
        verify(mockSession2, never()).close();
    }

    @Test
    @DisplayName("获取会话 - 存在的会话")
    void testGetSession_存在的会话() {
        // Given
        String key = "user123";
        sessionSet.addSession(key, mockSession1);

        // When
        Session retrievedSession = sessionSet.getSession(key);

        // Then
        assertNotNull(retrievedSession, "应该能获取到已添加的会话");
        assertEquals(mockSession1, retrievedSession, "获取的会话应该正确");
    }

    @Test
    @DisplayName("获取会话 - 不存在的会话")
    void testGetSession_不存在的会话() {
        // When
        Session retrievedSession = sessionSet.getSession("nonexistent");

        // Then
        assertNull(retrievedSession, "不存在的会话应该返回null");
    }

    @Test
    @DisplayName("删除会话 - 存在的会话")
    void testDel_存在的会话() {
        // Given
        String key = "user123";
        sessionSet.addSession(key, mockSession1);

        // When
        sessionSet.del(key);

        // Then
        Session retrievedSession = sessionSet.getSession(key);
        assertNull(retrievedSession, "删除后应该无法获取到会话");
        verify(mockSession1, times(1)).close();
    }

    @Test
    @DisplayName("删除会话 - 不存在的会话")
    void testDel_不存在的会话() {
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> sessionSet.del("nonexistent"),
            "删除不存在的会话不应该抛出异常");
    }

    @Test
    @DisplayName("获取所有会话键")
    void testAllSessions() {
        // Given
        sessionSet.addSession("user1", mockSession1);
        sessionSet.addSession("user2", mockSession2);

        // When
        Set<String> allKeys = sessionSet.allSessions();

        // Then
        assertNotNull(allKeys, "会话键集合不应为null");
        assertEquals(2, allKeys.size(), "应该有2个会话");
        assertTrue(allKeys.contains("user1"), "应该包含user1");
        assertTrue(allKeys.contains("user2"), "应该包含user2");
    }

    @Test
    @DisplayName("获取所有会话键 - 空集合")
    void testAllSessions_空集合() {
        // When
        Set<String> allKeys = sessionSet.allSessions();

        // Then
        assertNotNull(allKeys, "会话键集合不应为null");
        assertTrue(allKeys.isEmpty(), "空的SessionSet应该返回空集合");
    }

    @Test
    @DisplayName("并发添加会话 - 线程安全测试")
    void testAddSession_并发测试() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Session mockSession = mock(Session.class);
                sessionSet.addSession("user" + index, mockSession);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        Set<String> allKeys = sessionSet.allSessions();
        assertEquals(threadCount, allKeys.size(), "所有会话都应该被正确添加");
    }
}
