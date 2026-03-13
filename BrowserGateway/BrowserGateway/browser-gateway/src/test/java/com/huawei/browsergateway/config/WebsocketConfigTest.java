package com.huawei.browsergateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebsocketConfig 测试类
 * 测试 WebSocket 配置功能
 */
@DisplayName("WebsocketConfig tests")
class WebsocketConfigTest {

    private WebsocketConfig websocketConfig;

    @BeforeEach
    void setUp() {
        websocketConfig = new WebsocketConfig();
    }

    @Test
    @DisplayName("Set and get mediaPort")
    void testMediaPort() {
        // Given
        Integer expectedPort = 8080;

        // When
        websocketConfig.setMediaPort(expectedPort);

        // Then
        assertEquals(expectedPort, websocketConfig.getMediaPort(), "mediaPort 应该正确设置和获取");
    }

    @Test
    @DisplayName("Set and get muenPort")
    void testMuenPort() {
        // Given
        Integer expectedPort = 8081;

        // When
        websocketConfig.setMuenPort(expectedPort);

        // Then
        assertEquals(expectedPort, websocketConfig.getMuenPort(), "muenPort 应该正确设置和获取");
    }

    @Test
    @DisplayName("Set and get boss threads")
    void testBoss() {
        // Given
        Integer expectedBoss = 2;

        // When
        websocketConfig.setBoss(expectedBoss);

        // Then
        assertEquals(expectedBoss, websocketConfig.getBoss(), "boss 线程数应该正确设置和获取");
    }

    @Test
    @DisplayName("Set and get worker threads")
    void testWorker() {
        // Given
        Integer expectedWorker = 4;

        // When
        websocketConfig.setWorker(expectedWorker);

        // Then
        assertEquals(expectedWorker, websocketConfig.getWorker(), "worker 线程数应该正确设置和获取");
    }

    @Test
    @DisplayName("Set and get heartbeatTtl")
    void testHeartbeatTtl() {
        // Given
        Long expectedTtl = 60000L;

        // When
        websocketConfig.setHeartbeatTtl(expectedTtl);

        // Then
        assertEquals(expectedTtl, websocketConfig.getHeartbeatTtl(), "heartbeatTtl 应该正确设置和获取");
    }

    @Test
    @DisplayName("Default values - all fields initially null")
    void testDefaultValues() {
        // Then
        assertNull(websocketConfig.getMediaPort(), "mediaPort 初始值应该为null");
        assertNull(websocketConfig.getMuenPort(), "muenPort 初始值应该为null");
        assertNull(websocketConfig.getBoss(), "boss 初始值应该为null");
        assertNull(websocketConfig.getWorker(), "worker 初始值应该为null");
        assertNull(websocketConfig.getHeartbeatTtl(), "heartbeatTtl 初始值应该为null");
    }

    @Test
    @DisplayName("Set null values")
    void testSetNullValues() {
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
