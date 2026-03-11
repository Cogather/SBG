package com.huawei.browsergateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebsocketConfig 测试类
 * 测试 WebSocket 配置功能
 */
@DisplayName("WebsocketConfig 测试")
class WebsocketConfigTest {

    private WebsocketConfig websocketConfig;

    @BeforeEach
    void setUp() {
        websocketConfig = new WebsocketConfig();
    }

    @Test
    @DisplayName("设置和获取 mediaPort")
    void testMediaPort() {
        // Given
        Integer expectedPort = 8080;

        // When
        websocketConfig.setMediaPort(expectedPort);

        // Then
        assertEquals(expectedPort, websocketConfig.getMediaPort(), "mediaPort 应该正确设置和获取");
    }

    @Test
    @DisplayName("设置和获取 muenPort")
    void testMuenPort() {
        // Given
        Integer expectedPort = 8081;

        // When
        websocketConfig.setMuenPort(expectedPort);

        // Then
        assertEquals(expectedPort, websocketConfig.getMuenPort(), "muenPort 应该正确设置和获取");
    }

    @Test
    @DisplayName("设置和获取 boss 线程数")
    void testBoss() {
        // Given
        Integer expectedBoss = 2;

        // When
        websocketConfig.setBoss(expectedBoss);

        // Then
        assertEquals(expectedBoss, websocketConfig.getBoss(), "boss 线程数应该正确设置和获取");
    }

    @Test
    @DisplayName("设置和获取 worker 线程数")
    void testWorker() {
        // Given
        Integer expectedWorker = 4;

        // When
        websocketConfig.setWorker(expectedWorker);

        // Then
        assertEquals(expectedWorker, websocketConfig.getWorker(), "worker 线程数应该正确设置和获取");
    }

    @Test
    @DisplayName("设置和获取 heartbeatTtl")
    void testHeartbeatTtl() {
        // Given
        Long expectedTtl = 60000L;

        // When
        websocketConfig.setHeartbeatTtl(expectedTtl);

        // Then
        assertEquals(expectedTtl, websocketConfig.getHeartbeatTtl(), "heartbeatTtl 应该正确设置和获取");
    }

    @Test
    @DisplayName("默认值测试 - 所有字段初始为null")
    void test默认值() {
        // Then
        assertNull(websocketConfig.getMediaPort(), "mediaPort 初始值应该为null");
        assertNull(websocketConfig.getMuenPort(), "muenPort 初始值应该为null");
        assertNull(websocketConfig.getBoss(), "boss 初始值应该为null");
        assertNull(websocketConfig.getWorker(), "worker 初始值应该为null");
        assertNull(websocketConfig.getHeartbeatTtl(), "heartbeatTtl 初始值应该为null");
    }

    @Test
    @DisplayName("设置null值")
    void test设置null值() {
        // Given
        websocketConfig.setMediaPort(8080);
        websocketConfig.setMuenPort(8081);

        // When
        websocketConfig.setMediaPort(null);
        websocketConfig.setMuenPort(null);

        // Then
        assertNull(websocketConfig.getMediaPort(), "应该能设置null值");
        assertNull(websocketConfig.getMuenPort(), "应该能设置null值");
    }
}
