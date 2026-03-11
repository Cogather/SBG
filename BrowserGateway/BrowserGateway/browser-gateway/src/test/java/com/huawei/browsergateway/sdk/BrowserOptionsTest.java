package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BrowserOptions 测试
 * 测试浏览器配置选项的基本功能
 */
class BrowserOptionsTest {

    private BrowserOptions options;

    @BeforeEach
    void setUp() {
        options = new BrowserOptions();
    }

    @Test
    void testSetAndGetEndpoint() {
        // Given
        String endpoint = "http://localhost:9222";

        // When
        options.setEndpoint(endpoint);

        // Then
        assertEquals(endpoint, options.getEndpoint(), "端点地址应该匹配");
    }

    @Test
    void testSetAndGetBrowserType() {
        // Given
        Type.BrowserType browserType = Type.BrowserType.KEYS;

        // When
        options.setBrowserType(browserType);

        // Then
        assertEquals(browserType, options.getBrowserType(), "浏览器类型应该匹配");
    }

    @Test
    void testSetAndGetHeadless() {
        // When
        options.setHeadless(true);

        // Then
        assertTrue(options.isHeadless(), "无头模式应该为true");
    }

    @Test
    void testSetAndGetUrl() {
        // Given
        String url = "https://www.example.com";

        // When
        options.setUrl(url);

        // Then
        assertEquals(url, options.getUrl(), "URL应该匹配");
    }

    @Test
    void testSetAndGetExtensionPaths() {
        // Given
        List<String> paths = Arrays.asList("/path/to/ext1", "/path/to/ext2");

        // When
        options.setExtensionPaths(paths);

        // Then
        assertEquals(paths, options.getExtensionPaths(), "扩展路径列表应该匹配");
        assertEquals(2, options.getExtensionPaths().size(), "扩展路径数量应为2");
    }

    @Test
    void testSetAndGetLanguage() {
        // Given
        String language = "zh-CN";

        // When
        options.setLanguage(language);

        // Then
        assertEquals(language, options.getLanguage(), "语言设置应该匹配");
    }

    @Test
    void testDefaultValues() {
        // Then
        assertFalse(options.isHeadless(), "默认应该不是无头模式");
        assertEquals(0, options.getLimit(), "默认限制应为0");
    }
}
