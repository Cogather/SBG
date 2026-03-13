package com.huawei.browsergateway.service;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.impl.ChromeSetImpl;
import com.huawei.browsergateway.service.impl.UserChrome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ChromeSet 服务测试
 * 测试浏览器会话管理的核心功能
 */
class ChromeSetImplTest {

    @InjectMocks
    private ChromeSetImpl chromeSet;

    @Mock
    private Config config;

    @Mock
    private IFileStorage fileStorage;

    @Mock
    private IRemote remote;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllUser_emptyInitially() {
        // When
        Set<String> users = chromeSet.getAllUser();

        // Then
        assertNotNull(users, "用户集合不应为null");
    }

    @Test
    void testGetHeartbeats_returnsZeroForNonExistentUser() {
        // Given
        String userId = "non-existent-user";

        // When
        long result = chromeSet.getHeartbeats(userId);

        // Then
        assertEquals(0, result, "不存在的用户心跳应为0");
    }

    @Test
    void testUpdateHeartbeats_ignoresNonExistentUser() {
        // Given
        String userId = "non-existent-user";
        long heartbeats = System.currentTimeMillis();

        // When - 更新不存在的用户不应抛出异常
        assertDoesNotThrow(() -> {
            chromeSet.updateHeartbeats(userId, heartbeats);
        }, "更新不存在的用户不应抛出异常");

        // Then - 获取心跳应返回0
        long result = chromeSet.getHeartbeats(userId);
        assertEquals(0, result, "不存在的用户心跳应为0");
    }

    @Test
    void testGet_returnsNullForNonExistentUser() {
        // Given
        String userId = "non-existent-user";

        // When
        UserChrome result = chromeSet.get(userId);

        // Then
        assertNull(result, "不存在的用户应返回null");
    }
}
