package com.huawei.browsergateway.api;

import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.service.ExtensionManageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ExtensionManageApi 测试类
 * 测试插件管理相关的API接口
 */
class ExtensionManageApiTest {

    @InjectMocks
    private ExtensionManageApi extensionManageApi;

    @Mock
    private ExtensionManageService extensionManageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试加载插件成功的场景
     */
    @Test
    void testLoadExtensionSuccess() {
        // Given
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setBucketName("test-bucket");
        request.setExtensionFilePath("/path/to/extension");

        when(extensionManageService.loadExtension(request)).thenReturn(true);

        // When
        CommonResult<LoadExtensionResponse> result = extensionManageApi.loadExtension(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "成功时应返回200");
        assertNotNull(result.getData(), "成功时应返回数据");
        assertEquals("test-bucket", result.getData().getBucketName(), "bucket名称应匹配");
        assertEquals("/path/to/extension", result.getData().getExtensionFilePath(), "文件路径应匹配");

        verify(extensionManageService, times(1)).loadExtension(request);
    }

    /**
     * 测试加载插件失败的场景
     */
    @Test
    void testLoadExtensionFailure() {
        // Given
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setBucketName("test-bucket");
        request.setExtensionFilePath("/path/to/extension");

        when(extensionManageService.loadExtension(request)).thenReturn(false);

        // When
        CommonResult<LoadExtensionResponse> result = extensionManageApi.loadExtension(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertNotEquals(200, result.getCode(), "失败时不应返回200");
        assertEquals("reload extension failed", result.getMessage(), "失败消息应匹配");

        verify(extensionManageService, times(1)).loadExtension(request);
    }

    /**
     * 测试获取插件信息
     */
    @Test
    void testGetPluginInfoSuccess() {
        // Given
        PluginActive pluginActive = new PluginActive();
        when(extensionManageService.getPluginInfo()).thenReturn(pluginActive);

        // When
        CommonResult<PluginActive> result = extensionManageApi.getPluginInfo();

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "成功时应返回200");
        assertNotNull(result.getData(), "成功时应返回数据");
        assertEquals(pluginActive, result.getData(), "插件信息应匹配");

        verify(extensionManageService, times(1)).getPluginInfo();
    }

    /**
     * 测试获取插件信息返回null的场景
     */
    @Test
    void testGetPluginInfoReturnsNull() {
        // Given
        when(extensionManageService.getPluginInfo()).thenReturn(null);

        // When
        CommonResult<PluginActive> result = extensionManageApi.getPluginInfo();

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "即使数据为null也应返回200");
        assertNull(result.getData(), "数据应为null");

        verify(extensionManageService, times(1)).getPluginInfo();
    }
}
