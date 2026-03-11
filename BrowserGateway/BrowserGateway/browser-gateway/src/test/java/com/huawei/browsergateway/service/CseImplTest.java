package com.huawei.browsergateway.service;

import com.huawei.browsergateway.service.impl.CseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSE 服务测试
 * 测试服务注册中心相关功能
 */
class CseImplTest {

    private CseImpl cseImpl;

    @BeforeEach
    void setUp() {
        cseImpl = new CseImpl();
    }

    @Test
    void testGetReportEndpoint_外网环境应返回空字符串() {
        // When
        String endpoint = cseImpl.getReportEndpoint();

        // Then
        assertNotNull(endpoint, "端点不应为null");
        assertEquals("", endpoint, "外网环境应返回空字符串");
    }

    @Test
    void testGetReportEndpoint_多次调用应保持一致() {
        // When
        String endpoint1 = cseImpl.getReportEndpoint();
        String endpoint2 = cseImpl.getReportEndpoint();

        // Then
        assertEquals(endpoint1, endpoint2, "多次调用应返回相同结果");
    }
}
