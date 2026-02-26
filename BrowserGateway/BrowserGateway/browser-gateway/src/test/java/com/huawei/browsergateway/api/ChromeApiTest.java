package com.huawei.browsergateway.api;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.common.response.BaseResponse;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChromeApi单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChromeApi决策表测试")
class ChromeApiTest {

    @Mock
    private IRemote remoteService;

    @Mock
    private IChromeSet chromeSet;

    @Mock
    private UserDataManager userDataManager;

    @InjectMocks
    private ChromeApi chromeApi;

    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "123456789012345";
    private static final String TEST_USER_ID = "test_user_id";

    @BeforeEach
    void setUp() {
        // 设置workspace属性
        ReflectionTestUtils.setField(chromeApi, "workspace", "/opt/host");
    }

    /**
     * 决策表：预开浏览器接口测试
     * 
     * 条件：
     * C1: 请求参数是否为null
     * C2: IMEI是否为空
     * C3: IMSI是否为空
     * C4: 屏幕宽度是否有效（>0）
     * C5: 屏幕高度是否有效（>0）
     * C6: 浏览器实例是否已存在
     * 
     * 动作：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否调用remoteService.createChrome
     */
    @ParameterizedTest(name = "预开浏览器决策表 - 规则{0}: request={1}, imei={2}, imsi={3}, width={4}, height={5}, instanceExists={6}")
    @MethodSource("preOpenBrowserDecisionTable")
    @DisplayName("预开浏览器决策表测试")
    void testPreOpenBrowserDecisionTable(
            String ruleId,
            InitBrowserRequest request,
            String expectedCode,
            String expectedMessage,
            boolean shouldCallCreateChrome,
            boolean shouldThrowException) {
        
        // 设置Mock行为（只有在不会抛出异常的情况下才设置）
        if (request != null && !shouldThrowException) {
            // 检查IMEI和IMSI是否至少有一个不为空
            boolean hasValidId = (request.getImei() != null && !request.getImei().trim().isEmpty()) ||
                                 (request.getImsi() != null && !request.getImsi().trim().isEmpty());
            if (hasValidId) {
                String userId = com.huawei.browsergateway.common.utils.UserIdUtil.generateUserId(
                        request.getImei() != null ? request.getImei() : "",
                        request.getImsi() != null ? request.getImsi() : "");
                if (shouldCallCreateChrome) {
                    when(chromeSet.get(anyString())).thenReturn(null);
                } else {
                    UserChrome existingChrome = new UserChrome(userId, request);
                    when(chromeSet.get(anyString())).thenReturn(existingChrome);
                }
            }
        }

        // 执行测试
        BaseResponse<String> response = chromeApi.preOpenBrowser(request);
        assertEquals(expectedCode, String.valueOf(response.getCode()));
        // 对于成功响应，如果expectedMessage不是"成功"，则检查data字段
        if ("200".equals(expectedCode) && !"成功".equals(expectedMessage)) {
            assertNotNull(response.getData());
            assertTrue(response.getData().contains(expectedMessage));
        } else {
            assertTrue(response.getMessage().contains(expectedMessage));
        }
        
        if (shouldThrowException) {
            // 参数验证失败时，不应该调用remoteService
            verify(remoteService, never()).createChrome(any(), any(), any());
        } else {
            if (shouldCallCreateChrome) {
                verify(remoteService, times(1)).createChrome(any(byte[].class), eq(request), isNull());
            } else {
                verify(remoteService, never()).createChrome(any(), any(), any());
            }
        }
    }

    /**
     * 决策表数据源：预开浏览器
     */
    static Stream<Arguments> preOpenBrowserDecisionTable() {
        return Stream.of(
            // R1: 请求参数为null -> 返回错误
            Arguments.of("R1", null, "400", "请求参数不能为空", false, true),
            
            // R2: IMEI和IMSI同时为空 -> 返回错误
            Arguments.of("R2", createRequest("", "", 1920, 1080), "400", "IMEI和IMSI不能同时为空", false, true),
            
            // R3: IMEI有效，IMSI为空，实例不存在 -> 成功创建
            Arguments.of("R3", createRequest(TEST_IMEI, "", 1920, 1080), "200", "成功", true, false),
            
            // R4: IMEI为空，IMSI有效，实例不存在 -> 成功创建
            Arguments.of("R4", createRequest("", TEST_IMSI, 1920, 1080), "200", "成功", true, false),
            
            // R5: IMEI和IMSI都有效，实例已存在 -> 跳过创建
            Arguments.of("R5", createRequest(TEST_IMEI, TEST_IMSI, 1920, 1080), "200", "浏览器实例已存在", false, false),
            
            // R6: 屏幕宽度为0 -> 返回错误
            Arguments.of("R6", createRequest(TEST_IMEI, TEST_IMSI, 0, 1080), "400", "屏幕宽度必须大于0", false, true),
            
            // R7: 屏幕宽度为负数 -> 返回错误
            Arguments.of("R7", createRequest(TEST_IMEI, TEST_IMSI, -1, 1080), "400", "屏幕宽度必须大于0", false, true),
            
            // R8: 屏幕高度为0 -> 返回错误
            Arguments.of("R8", createRequest(TEST_IMEI, TEST_IMSI, 1920, 0), "400", "屏幕高度必须大于0", false, true),
            
            // R9: 屏幕高度为负数 -> 返回错误
            Arguments.of("R9", createRequest(TEST_IMEI, TEST_IMSI, 1920, -1), "400", "屏幕高度必须大于0", false, true),
            
            // R10: 所有参数有效，实例不存在 -> 成功创建
            Arguments.of("R10", createRequest(TEST_IMEI, TEST_IMSI, 1920, 1080), "200", "成功", true, false)
        );
    }

    /**
     * 决策表：删除用户数据接口测试
     * 
     * 条件：
     * C1: 请求参数是否为null
     * C2: IMEI是否为空
     * C3: IMSI是否为空
     * C4: 浏览器实例是否存在
     * C5: UserDataManager是否可用
     * 
     * 动作：
     * A1: 返回码
     * A2: 是否调用chromeSet.delete
     * A3: 是否调用userDataManager.deleteUserData
     */
    @ParameterizedTest(name = "删除用户数据决策表 - 规则{0}: request={1}, instanceExists={2}, userDataManagerAvailable={3}")
    @MethodSource("deleteUserDataDecisionTable")
    @DisplayName("删除用户数据决策表测试")
    void testDeleteUserDataDecisionTable(
            String ruleId,
            DeleteUserDataRequest request,
            boolean instanceExists,
            boolean userDataManagerAvailable,
            String expectedCode,
            boolean shouldCallDelete,
            boolean shouldCallDeleteUserData,
            boolean shouldThrowException) {
        
        // 设置Mock行为
        if (request != null && !shouldThrowException) {
            String userId = com.huawei.browsergateway.common.utils.UserIdUtil.generateUserId(
                    request.getImei(), request.getImsi());
            if (instanceExists) {
                UserChrome userChrome = new UserChrome(userId, 
                    createInitRequest(request.getImei(), request.getImsi()));
                when(chromeSet.get(userId)).thenReturn(userChrome);
            } else {
                when(chromeSet.get(userId)).thenReturn(null);
            }
        }

        // 设置UserDataManager可用性
        if (!userDataManagerAvailable) {
            ReflectionTestUtils.setField(chromeApi, "userDataManager", null);
        }

        // 执行测试
        BaseResponse<DeleteUserDataRequest> response = chromeApi.deleteUserData(request);
        assertEquals(expectedCode, String.valueOf(response.getCode()));
        
        if (shouldThrowException) {
            // 参数验证失败时，不应该调用chromeSet.delete
            verify(chromeSet, never()).delete(anyString());
        } else {
            if (shouldCallDelete) {
                verify(chromeSet, times(1)).delete(anyString());
            } else {
                verify(chromeSet, never()).delete(anyString());
            }
            
            if (shouldCallDeleteUserData && userDataManagerAvailable) {
                verify(userDataManager, times(1)).deleteUserData(anyString(), anyString());
            }
        }
    }

    /**
     * 决策表数据源：删除用户数据
     */
    static Stream<Arguments> deleteUserDataDecisionTable() {
        return Stream.of(
            // R1: 请求参数为null -> 返回错误
            Arguments.of("R1", null, false, true, "400", false, false, true),
            
            // R2: IMEI和IMSI同时为空 -> 返回错误
            Arguments.of("R2", createDeleteRequest("", ""), false, true, "400", false, false, true),
            
            // R3: 参数有效，实例不存在，UserDataManager可用 -> 幂等返回成功
            Arguments.of("R3", createDeleteRequest(TEST_IMEI, TEST_IMSI), false, true, "200", false, false, false),
            
            // R4: 参数有效，实例不存在，UserDataManager不可用 -> 幂等返回成功
            Arguments.of("R4", createDeleteRequest(TEST_IMEI, TEST_IMSI), false, false, "200", false, false, false),
            
            // R5: 参数有效，实例存在，UserDataManager可用 -> 删除成功
            Arguments.of("R5", createDeleteRequest(TEST_IMEI, TEST_IMSI), true, true, "200", true, true, false),
            
            // R6: 参数有效，实例存在，UserDataManager不可用 -> 删除成功（跳过数据删除）
            Arguments.of("R6", createDeleteRequest(TEST_IMEI, TEST_IMSI), true, false, "200", true, false, false),
            
            // R7: IMEI有效，IMSI为空 -> 成功删除
            Arguments.of("R7", createDeleteRequest(TEST_IMEI, ""), true, true, "200", true, true, false),
            
            // R8: IMEI为空，IMSI有效 -> 成功删除
            Arguments.of("R8", createDeleteRequest("", TEST_IMSI), true, true, "200", true, true, false)
        );
    }

    /**
     * 测试异常场景：remoteService抛出异常
     */
    @Test
    @DisplayName("预开浏览器异常场景：远程服务异常")
    void testPreOpenBrowserRemoteServiceException() {
        InitBrowserRequest request = createRequest(TEST_IMEI, TEST_IMSI, 1920, 1080);
        when(chromeSet.get(anyString())).thenReturn(null);
        doThrow(new RuntimeException("远程服务异常")).when(remoteService).createChrome(any(), any(), any());
        
        BaseResponse<String> response = chromeApi.preOpenBrowser(request);
        
        assertEquals("1001", String.valueOf(response.getCode())); // BROWSER_CREATE_FAILED
        assertTrue(response.getMessage().contains("预开浏览器失败"));
    }

    /**
     * 测试异常场景：userDataManager删除失败
     */
    @Test
    @DisplayName("删除用户数据异常场景：数据删除失败")
    void testDeleteUserDataManagerException() {
        DeleteUserDataRequest request = createDeleteRequest(TEST_IMEI, TEST_IMSI);
        String userId = com.huawei.browsergateway.common.utils.UserIdUtil.generateUserId(
                request.getImei(), request.getImsi());
        UserChrome userChrome = new UserChrome(userId, createInitRequest(request.getImei(), request.getImsi()));
        when(chromeSet.get(userId)).thenReturn(userChrome);
        doThrow(new RuntimeException("删除失败")).when(userDataManager).deleteUserData(anyString(), anyString());
        
        // 即使删除失败，也应该返回成功（容错设计）
        BaseResponse<DeleteUserDataRequest> response = chromeApi.deleteUserData(request);
        
        assertEquals("200", String.valueOf(response.getCode()));
        verify(chromeSet, times(1)).delete(anyString());
    }

    // ========== 辅助方法 ==========

    private static InitBrowserRequest createRequest(String imei, String imsi, Integer width, Integer height) {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei(imei);
        request.setImsi(imsi);
        request.setLcdWidth(width);
        request.setLcdHeight(height);
        request.setAppType("mobile");
        return request;
    }

    private static InitBrowserRequest createInitRequest(String imei, String imsi) {
        return createRequest(imei, imsi, 1920, 1080);
    }

    private static DeleteUserDataRequest createDeleteRequest(String imei, String imsi) {
        DeleteUserDataRequest request = new DeleteUserDataRequest();
        request.setImei(imei);
        request.setImsi(imsi);
        return request;
    }
}
