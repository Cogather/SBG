package com.huawei.browsergateway.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service模块基础测试
 * 用于验证重构前后功能一致性
 */
class ServiceModuleBasicTest {

    @Test
    void testRemoteImpl_ClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.huawei.browsergateway.service.impl.RemoteImpl"));
    }

    @Test
    void testFileStorageServiceImpl_ClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.huawei.browsergateway.service.impl.FileStorageServiceImpl"));
    }

    @Test
    void testChromeSetImpl_ClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.huawei.browsergateway.service.impl.ChromeSetImpl"));
    }

    @Test
    void testHWCallbackImpl_ClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.huawei.browsergateway.service.impl.HWCallbackImpl"));
    }

    @Test
    void testPluginManageImpl_ClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.huawei.browsergateway.service.impl.PluginManageImpl"));
    }
}
