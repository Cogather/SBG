package com.huawei.browsergateway.websocket.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yeauty.pojo.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MediaSessionManager 测试类
 * 测试媒体会话管理器功能
 */
@DisplayName("MediaSessionManager 测试")
class MediaSessionManagerTest {

    private MediaSessionManager mediaSessionManager;

    @Mock
    private Session mockSession;

    @Mock
    private MediaStreamProcessor mockProcessor1;

    @Mock
    private MediaStreamProcessor mockProcessor2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mediaSessionManager = new MediaSessionManager();
    }

    @Test
    @DisplayName("添加处理器 - 正常添加")
    void testAddProcessor_正常添加() {
        // Given
        String key = "user123";

        // When
        mediaSessionManager.addProcessor(key, mockProcessor1);

        // Then
        MediaStreamProcessor retrievedProcessor = mediaSessionManager.getProcessor(key);
        assertNotNull(retrievedProcessor, "添加的处理器应该能被获取");
        assertEquals(mockProcessor1, retrievedProcessor, "获取的处理器应该与添加的处理器相同");
        verify(mockProcessor1, never()).close();
    }

    @Test
    @DisplayName("添加处理器 - 替换已存在的处理器")
    void testAddProcessor_替换已存在的处理器() {
        // Given
        String key = "user123";
        mediaSessionManager.addProcessor(key, mockProcessor1);

        // When
        mediaSessionManager.addProcessor(key, mockProcessor2);

        // Then
        MediaStreamProcessor retrievedProcessor = mediaSessionManager.getProcessor(key);
        assertEquals(mockProcessor2, retrievedProcessor, "新处理器应该替换旧处理器");
        verify(mockProcessor1, times(1)).close();
        verify(mockProcessor2, never()).close();
    }

    @Test
    @DisplayName("获取处理器 - 存在的处理器")
    void testGetProcessor_存在的处理器() {
        // Given
        String key = "user123";
        mediaSessionManager.addProcessor(key, mockProcessor1);

        // When
        MediaStreamProcessor retrievedProcessor = mediaSessionManager.getProcessor(key);

        // Then
        assertNotNull(retrievedProcessor, "应该能获取到已添加的处理器");
        assertEquals(mockProcessor1, retrievedProcessor, "获取的处理器应该正确");
    }

    @Test
    @DisplayName("获取处理器 - 不存在的处理器")
    void testGetProcessor_不存在的处理器() {
        // When
        MediaStreamProcessor retrievedProcessor = mediaSessionManager.getProcessor("nonexistent");

        // Then
        assertNull(retrievedProcessor, "不存在的处理器应该返回null");
    }

    @Test
    @DisplayName("删除处理器 - 存在的处理器")
    void testDelProcessor_存在的处理器() {
        // Given
        String key = "user123";
        mediaSessionManager.addProcessor(key, mockProcessor1);

        // When
        mediaSessionManager.delProcessor(key);

        // Then
        MediaStreamProcessor retrievedProcessor = mediaSessionManager.getProcessor(key);
        assertNull(retrievedProcessor, "删除后应该无法获取到处理器");
        verify(mockProcessor1, times(1)).close();
    }

    @Test
    @DisplayName("删除处理器 - 不存在的处理器")
    void testDelProcessor_不存在的处理器() {
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> mediaSessionManager.delProcessor("nonexistent"),
            "删除不存在的处理器不应该抛出异常");
    }

    @Test
    @DisplayName("删除会话 - 同时删除处理器和会话")
    void testDel_同时删除处理器和会话() {
        // Given
        String key = "user123";
        mediaSessionManager.addSession(key, mockSession);
        mediaSessionManager.addProcessor(key, mockProcessor1);

        // When
        mediaSessionManager.del(key);

        // Then
        assertNull(mediaSessionManager.getSession(key), "会话应该被删除");
        assertNull(mediaSessionManager.getProcessor(key), "处理器应该被删除");
        verify(mockSession, times(1)).close();
        verify(mockProcessor1, times(1)).close();
    }

    @Test
    @DisplayName("继承功能 - 添加和获取会话")
    void test继承功能_添加和获取会话() {
        // Given
        String key = "user123";

        // When
        mediaSessionManager.addSession(key, mockSession);

        // Then
        Session retrievedSession = mediaSessionManager.getSession(key);
        assertNotNull(retrievedSession, "应该能获取到已添加的会话");
        assertEquals(mockSession, retrievedSession, "获取的会话应该正确");
    }

    @Test
    @DisplayName("继承功能 - 获取所有会话键")
    void test继承功能_获取所有会话键() {
        // Given
        mediaSessionManager.addSession("user1", mockSession);
        mediaSessionManager.addProcessor("user1", mockProcessor1);

        // When
        var allKeys = mediaSessionManager.allSessions();

        // Then
        assertNotNull(allKeys, "会话键集合不应为null");
        assertEquals(1, allKeys.size(), "应该有1个会话");
        assertTrue(allKeys.contains("user1"), "应该包含user1");
    }
}
