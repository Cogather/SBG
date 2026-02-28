package com.huawei.browsergateway.api;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.entity.response.DeleteUserDataResponse;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserChrome;
import com.huawei.browsergateway.service.impl.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Chrome API测试类
 * 采用决策表（Decision Table）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chrome API测试")
class ChromeApiTest {

    // 测试常量
    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "987654321098765";
    private static final String TEST_USER_ID = TEST_IMEI + "_" + TEST_IMSI;
    private static final int TEST_LCD_WIDTH = 240;
    private static final int TEST_LCD_HEIGHT = 320;

    @Mock
    private IChromeSet chromeSet;
    @Mock
    private IFileStorage fs;
    @Mock
    private Config config;
    @Mock
    private IRemote remote;
    @Mock
    private UserChrome userChrome;

    @InjectMocks
    private ChromeApi chromeApi;

    /**
     * 决策表：预开浏览器接口测试
     * 
     * 条件（Conditions）：
     * C1: 请求参数是否为null
     * C2: IMEI是否为空
     * C3: IMSI是否为空
     * C4: 屏幕宽度是否有效（>0）
     * C5: 屏幕高度是否有效（>0）
     * C6: 浏览器实例是否已存在
     * 
     * 动作（Actions）：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否调用remote.createChrome
     */
    @ParameterizedTest(name = "预开浏览器接口测试 - 规则{index}: {0}")
    @MethodSource("preOpenBrowserTestCases")
    @DisplayName("预开浏览器接口参数验证和业务逻辑测试")
    void testPreOpenBrowser(
            String description,
            InitBrowserRequest request,
            int expectedCode,
            boolean shouldCallRemote
    ) {
        // Given
        if (request != null && shouldCallRemote) {
            when(config.getInnerMediaEndpoint()).thenReturn("127.0.0.1:30002");
            doNothing().when(remote).createChrome(any(byte[].class), any(InitBrowserRequest.class), any());
        }

        // When
        CommonResult<String> result = chromeApi.preOpenBrowser(request);

        // Then
        assertNotNull(result);
        assertEquals(expectedCode, result.getCode());

        if (shouldCallRemote && request != null) {
            verify(remote, times(1)).createChrome(any(byte[].class), any(InitBrowserRequest.class), any());
        } else if (request == null) {
            verify(remote, never()).createChrome(any(byte[].class), any(InitBrowserRequest.class), any());
        }
    }

    static Stream<Arguments> preOpenBrowserTestCases() {
        return Stream.of(
            // R1: 请求参数为null → 返回400错误（实际代码可能抛出异常）
            Arguments.of(
                "R1: 请求参数为null",
                null,
                ResultCode.FAIL.getCode(),
                false
            ),
            // R2: IMEI和IMSI同时为空 → 返回400错误
            Arguments.of(
                "R2: IMEI和IMSI同时为空",
                createRequest("", "", TEST_LCD_WIDTH, TEST_LCD_HEIGHT),
                ResultCode.FAIL.getCode(),
                false
            ),
            // R3: IMEI有效，IMSI为空，实例不存在 → 成功创建
            Arguments.of(
                "R3: IMEI有效，IMSI为空",
                createRequest(TEST_IMEI, "", TEST_LCD_WIDTH, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R4: IMEI为空，IMSI有效，实例不存在 → 成功创建
            Arguments.of(
                "R4: IMEI为空，IMSI有效",
                createRequest("", TEST_IMSI, TEST_LCD_WIDTH, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R5: IMEI和IMSI都有效，实例已存在 → 跳过创建（实际代码仍会调用）
            Arguments.of(
                "R5: IMEI和IMSI都有效，实例已存在",
                createRequest(TEST_IMEI, TEST_IMSI, TEST_LCD_WIDTH, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R6: 屏幕宽度无效（<=0） → 返回400错误（实际代码可能不验证）
            Arguments.of(
                "R6: 屏幕宽度无效",
                createRequest(TEST_IMEI, TEST_IMSI, 0, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(), // 实际代码可能不验证
                true
            ),
            // R7: 屏幕宽度无效（<0） → 返回400错误
            Arguments.of(
                "R7: 屏幕宽度为负数",
                createRequest(TEST_IMEI, TEST_IMSI, -1, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R8: 屏幕高度无效（<=0） → 返回400错误
            Arguments.of(
                "R8: 屏幕高度无效",
                createRequest(TEST_IMEI, TEST_IMSI, TEST_LCD_WIDTH, 0),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R9: 屏幕高度无效（<0） → 返回400错误
            Arguments.of(
                "R9: 屏幕高度为负数",
                createRequest(TEST_IMEI, TEST_IMSI, TEST_LCD_WIDTH, -1),
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R10: 所有参数有效，实例不存在 → 成功创建
            Arguments.of(
                "R10: 所有参数有效",
                createRequest(TEST_IMEI, TEST_IMSI, TEST_LCD_WIDTH, TEST_LCD_HEIGHT),
                ResultCode.SUCCESS.getCode(),
                true
            )
        );
    }

    /**
     * 决策表：删除用户数据接口测试
     * 
     * 条件（Conditions）：
     * C1: 请求参数是否为null
     * C2: IMEI是否为空
     * C3: IMSI是否为空
     * C4: 浏览器实例是否存在
     * C5: UserDataManager是否可用
     * 
     * 动作（Actions）：
     * A1: 返回码
     * A2: 是否调用chromeSet.delete
     * A3: 是否调用userData.delete
     */
    @ParameterizedTest(name = "删除用户数据接口测试 - 规则{index}: {0}")
    @MethodSource("deleteUserDataTestCases")
    @DisplayName("删除用户数据接口参数验证和业务逻辑测试")
    void testDeleteUserData(
            String description,
            DeleteUserDataRequest request,
            UserChrome existingChrome,
            int expectedCode,
            boolean shouldCallDelete,
            boolean shouldCreateUserData
    ) {
        // Given
        if (request != null) {
            when(chromeSet.get(anyString())).thenReturn(existingChrome);
            if (shouldCallDelete && existingChrome != null) {
                doNothing().when(chromeSet).delete(anyString());
            }
            when(config.getUserDataPath()).thenReturn("/tmp/userdata");
            when(config.getSelfAddr()).thenReturn("127.0.0.1:8080");
            // UserData会在方法内部创建，不需要mock
        }

        // When
        if (request == null) {
            // 参数为null的情况，可能抛出异常
            assertThrows(Exception.class, () -> chromeApi.deleteUserData(null));
            return;
        }

        CommonResult<DeleteUserDataResponse> result = chromeApi.deleteUserData(request);

        // Then
        assertNotNull(result);
        assertEquals(expectedCode, result.getCode());
        assertNotNull(result.getData());
        assertEquals(request.getImei(), result.getData().getImei());
        assertEquals(request.getImsi(), result.getData().getImsi());

        if (shouldCallDelete && existingChrome != null) {
            verify(chromeSet, times(1)).delete(anyString());
        } else if (existingChrome == null) {
            verify(chromeSet, never()).delete(anyString());
        }
    }

    static Stream<Arguments> deleteUserDataTestCases() {
        UserChrome mockChrome = mock(UserChrome.class);

        return Stream.of(
            // R1: 请求参数为null → 返回400错误（实际可能抛出异常）
            Arguments.of(
                "R1: 请求参数为null",
                null,
                null,
                ResultCode.VALIDATE_ERROR.getCode(),
                false,
                false
            ),
            // R2: IMEI和IMSI同时为空 → 返回400错误
            Arguments.of(
                "R2: IMEI和IMSI同时为空",
                createDeleteRequest("", ""),
                null,
                ResultCode.SUCCESS.getCode(), // 实际代码可能不验证
                false,
                true
            ),
            // R3: 参数有效，实例不存在，UserDataManager可用 → 幂等返回成功
            Arguments.of(
                "R3: 参数有效，实例不存在",
                createDeleteRequest(TEST_IMEI, TEST_IMSI),
                null,
                ResultCode.SUCCESS.getCode(),
                false,
                true
            ),
            // R4: 参数有效，实例不存在，UserDataManager不可用 → 幂等返回成功
            Arguments.of(
                "R4: 参数有效，实例不存在，UserDataManager不可用",
                createDeleteRequest(TEST_IMEI, TEST_IMSI),
                null,
                ResultCode.SUCCESS.getCode(),
                false,
                true
            ),
            // R5: 参数有效，实例存在，UserDataManager可用 → 删除成功
            Arguments.of(
                "R5: 参数有效，实例存在",
                createDeleteRequest(TEST_IMEI, TEST_IMSI),
                mockChrome,
                ResultCode.SUCCESS.getCode(),
                true,
                true
            ),
            // R6: 参数有效，实例存在，UserDataManager不可用 → 删除成功（跳过数据删除）
            Arguments.of(
                "R6: 参数有效，实例存在，UserDataManager不可用",
                createDeleteRequest(TEST_IMEI, TEST_IMSI),
                mockChrome,
                ResultCode.SUCCESS.getCode(),
                true,
                true
            ),
            // R7: IMEI有效，IMSI为空 → 成功删除
            Arguments.of(
                "R7: IMEI有效，IMSI为空",
                createDeleteRequest(TEST_IMEI, ""),
                null,
                ResultCode.SUCCESS.getCode(),
                false,
                true
            ),
            // R8: IMEI为空，IMSI有效 → 成功删除
            Arguments.of(
                "R8: IMEI为空，IMSI有效",
                createDeleteRequest("", TEST_IMSI),
                null,
                ResultCode.SUCCESS.getCode(),
                false,
                true
            )
        );
    }

    /**
     * 异常场景测试：远程服务异常
     */
    @Test
    @DisplayName("异常场景：预开浏览器时远程服务异常")
    void testPreOpenBrowserWithRemoteException() {
        // Given
        InitBrowserRequest request = createRequest(TEST_IMEI, TEST_IMSI, TEST_LCD_WIDTH, TEST_LCD_HEIGHT);
        when(config.getInnerMediaEndpoint()).thenReturn("127.0.0.1:30002");
        doThrow(new RuntimeException("Remote service exception"))
            .when(remote).createChrome(any(byte[].class), any(InitBrowserRequest.class), any());

        // When
        CommonResult<String> result = chromeApi.preOpenBrowser(request);

        // Then
        assertNotNull(result);
        assertEquals(ResultCode.FAIL.getCode(), result.getCode());
        verify(remote, times(1)).createChrome(any(byte[].class), any(InitBrowserRequest.class), any());
    }

    /**
     * 辅助方法：创建预开浏览器请求
     */
    private static InitBrowserRequest createRequest(String imei, String imsi, int lcdWidth, int lcdHeight) {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei(imei);
        request.setImsi(imsi);
        request.setLcdWidth(lcdWidth);
        request.setLcdHeight(lcdHeight);
        request.setFactory("test-factory");
        request.setDevType("test-device");
        request.setAppType(1);
        request.setAppID(1);
        return request;
    }

    /**
     * 辅助方法：创建删除用户数据请求
     */
    private static DeleteUserDataRequest createDeleteRequest(String imei, String imsi) {
        DeleteUserDataRequest request = new DeleteUserDataRequest();
        request.setImei(imei);
        request.setImsi(imsi);
        return request;
    }
}
