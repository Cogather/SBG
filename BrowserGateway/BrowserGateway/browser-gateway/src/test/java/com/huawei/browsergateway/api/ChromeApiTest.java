package com.huawei.browsergateway.api;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.entity.response.DeleteUserDataResponse;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserChrome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChromeApi 测试类
 * 测试浏览器管理相关的API接口
 */
class ChromeApiTest {

    @InjectMocks
    private ChromeApi chromeApi;

    @Mock
    private IChromeSet chromeSet;

    @Mock
    private IFileStorage fs;

    @Mock
    private Config config;

    @Mock
    private IRemote remote;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试删除用户数据 - 用户有浏览器实例
     */
    @Test
    void testDeleteUserDataWithExistingBrowserInstance() {
        // Given
        DeleteUserDataRequest request = new DeleteUserDataRequest();
        request.setImei("123456789012345");
        request.setImsi("460001234567890");

        UserChrome userChrome = mock(UserChrome.class);
        when(chromeSet.get(anyString())).thenReturn(userChrome);
        when(config.getUserDataPath()).thenReturn("/path/to/userdata");
        when(config.getSelfAddr()).thenReturn("localhost:8080");

        // When
        CommonResult<DeleteUserDataResponse> result = chromeApi.deleteUserData(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "成功时应返回200");
        assertNotNull(result.getData(), "成功时应返回数据");
        assertEquals("123456789012345", result.getData().getImei(), "IMEI应匹配");
        assertEquals("460001234567890", result.getData().getImsi(), "IMSI应匹配");

        verify(chromeSet, times(1)).get(anyString());
        verify(chromeSet, times(1)).delete(anyString());
    }

    /**
     * 测试删除用户数据 - 用户无浏览器实例
     */
    @Test
    void testDeleteUserDataWithoutBrowserInstance() {
        // Given
        DeleteUserDataRequest request = new DeleteUserDataRequest();
        request.setImei("123456789012345");
        request.setImsi("460001234567890");

        when(chromeSet.get(anyString())).thenReturn(null);
        when(config.getUserDataPath()).thenReturn("/path/to/userdata");
        when(config.getSelfAddr()).thenReturn("localhost:8080");

        // When
        CommonResult<DeleteUserDataResponse> result = chromeApi.deleteUserData(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "成功时应返回200");
        assertNotNull(result.getData(), "成功时应返回数据");

        verify(chromeSet, times(1)).get(anyString());
        verify(chromeSet, never()).delete(anyString());
    }

    /**
     * 测试预打开浏览器 - 成功场景
     */
    @Test
    void testPreOpenBrowserSuccess() throws Exception {
        // Given
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei("123456789012345");
        request.setImsi("460001234567890");

        when(config.getInnerMediaEndpoint()).thenReturn("http://media-endpoint");
        doNothing().when(remote).createChrome(any(byte[].class), any(InitBrowserRequest.class), isNull());

        // When
        CommonResult<String> result = chromeApi.preOpenBrowser(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals(200, result.getCode(), "成功时应返回200");
        assertEquals("success", result.getData(), "成功消息应匹配");

        verify(config, times(1)).getInnerMediaEndpoint();
        verify(remote, times(1)).createChrome(any(byte[].class), any(InitBrowserRequest.class), isNull());
    }

    /**
     * 测试预打开浏览器 - 异常场景
     */
    @Test
    void testPreOpenBrowserWithException() throws Exception {
        // Given
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei("123456789012345");
        request.setImsi("460001234567890");

        when(config.getInnerMediaEndpoint()).thenReturn("http://media-endpoint");
        doThrow(new RuntimeException("Remote service error"))
                .when(remote).createChrome(any(byte[].class), any(InitBrowserRequest.class), isNull());

        // When
        CommonResult<String> result = chromeApi.preOpenBrowser(request);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertNotEquals(200, result.getCode(), "失败时不应返回200");

        verify(remote, times(1)).createChrome(any(byte[].class), any(InitBrowserRequest.class), isNull());
    }
}
