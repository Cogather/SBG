package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.entity.browser.BrowserStateMachine;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserDataManager;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
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

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChromeSetImpl单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChromeSetImpl决策表测试")
class ChromeSetImplTest {

    @Mock
    private IFileStorage fileStorageService;

    @Mock
    private IPluginManage pluginManage;

    @Mock
    private IRemote remoteService;

    @Mock
    private ControlClientSet controlClientSet;

    @Mock
    private MediaClientSet mediaClientSet;

    @Mock
    private UserDataManager userDataManager;

    @InjectMocks
    private ChromeSetImpl chromeSet;

    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "123456789012345";
    private static final String TEST_USER_ID = "test_user_id";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chromeSet, "serverAddress", "127.0.0.1");
        ReflectionTestUtils.setField(chromeSet, "workspace", "/opt/host");
    }

    /**
     * 决策表：创建浏览器实例测试
     * 
     * 条件：
     * C1: 实例是否已存在
     * C2: UserDataManager是否可用
     * C3: 用户数据下载是否成功
     * C4: PluginManage是否为PluginManageImpl实例
     * C5: MuenDriver创建是否成功
     * 
     * 动作：
     * A1: 是否删除旧实例
     * A2: 是否下载用户数据
     * A3: 是否创建MuenDriver
     * A4: 最终状态
     */
    @ParameterizedTest(name = "创建浏览器实例决策表 - 规则{0}")
    @MethodSource("createInstanceDecisionTable")
    @DisplayName("创建浏览器实例决策表测试")
    void testCreateInstanceDecisionTable(
            String ruleId,
            boolean instanceExists,
            boolean userDataManagerAvailable,
            boolean downloadSuccess,
            boolean isPluginManageImpl,
            boolean muenDriverCreated,
            boolean shouldDeleteOld,
            boolean shouldDownloadData,
            boolean shouldCreateDriver,
            UserChrome.BrowserStatus expectedStatus) {
        
        InitBrowserRequest request = createRequest();
        String userId = com.huawei.browsergateway.common.utils.UserIdUtil.generateUserId(
                request.getImei(), request.getImsi());

        // 设置Mock行为
        if (instanceExists) {
            UserChrome existingChrome = new UserChrome(userId, request);
            // 先创建一个实例，然后通过反射设置到map中
            java.util.concurrent.ConcurrentMap<String, UserChrome> userChromeMap = 
                    new java.util.concurrent.ConcurrentHashMap<>();
            userChromeMap.put(userId, existingChrome);
            ReflectionTestUtils.setField(chromeSet, "userChromeMap", userChromeMap);
        } else {
            ReflectionTestUtils.setField(chromeSet, "userChromeMap", 
                    new java.util.concurrent.ConcurrentHashMap<>());
        }

        if (userDataManagerAvailable) {
            ReflectionTestUtils.setField(chromeSet, "userDataManager", userDataManager);
            if (shouldDownloadData) {
                if (downloadSuccess) {
                    when(userDataManager.downloadUserData(anyString(), anyString(), anyString()))
                            .thenReturn("/opt/host/" + userId);
                } else {
                    when(userDataManager.downloadUserData(anyString(), anyString(), anyString()))
                            .thenThrow(new RuntimeException("下载失败"));
                }
            }
        } else {
            ReflectionTestUtils.setField(chromeSet, "userDataManager", null);
        }

        if (isPluginManageImpl) {
            PluginManageImpl pluginManageImpl = mock(PluginManageImpl.class);
            ReflectionTestUtils.setField(chromeSet, "pluginManage", pluginManageImpl);
            if (shouldCreateDriver && muenDriverCreated) {
                when(pluginManageImpl.createDriver(anyString(), any())).thenReturn(mock(com.huawei.browsergateway.sdk.MuenDriver.class));
            } else {
                when(pluginManageImpl.createDriver(anyString(), any())).thenReturn(null);
            }
        } else {
            ReflectionTestUtils.setField(chromeSet, "pluginManage", pluginManage);
        }

        // 执行测试
        try {
            UserChrome result = chromeSet.create(request);
            
            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(expectedStatus, result.getStatus());
            
            if (shouldDeleteOld && instanceExists) {
                // 验证旧实例被删除（通过检查新实例状态）
                assertNotEquals(UserChrome.BrowserStatus.INITIALIZING, result.getStatus());
            }
            
            if (shouldDownloadData && userDataManagerAvailable) {
                verify(userDataManager, atLeastOnce()).downloadUserData(anyString(), anyString(), anyString());
            }
            
        } catch (BusinessException e) {
            // 如果期望失败，验证异常
            if (expectedStatus == UserChrome.BrowserStatus.OPEN_ERROR) {
                assertNotNull(e);
            } else {
                fail("不应该抛出异常: " + e.getMessage());
            }
        }
    }

    /**
     * 决策表数据源：创建浏览器实例
     */
    static Stream<Arguments> createInstanceDecisionTable() {
        return Stream.of(
            // R1: 实例不存在，UserDataManager可用，下载成功，PluginManageImpl，MuenDriver创建成功 -> 成功创建
            Arguments.of("R1", false, true, true, true, true, false, true, true, UserChrome.BrowserStatus.READY),
            
            // R2: 实例已存在，UserDataManager可用，下载成功，PluginManageImpl，MuenDriver创建成功 -> 删除旧实例后创建
            Arguments.of("R2", true, true, true, true, true, true, true, true, UserChrome.BrowserStatus.READY),
            
            // R3: 实例不存在，UserDataManager不可用 -> 成功创建（跳过数据下载）
            Arguments.of("R3", false, false, false, true, true, false, false, true, UserChrome.BrowserStatus.READY),
            
            // R4: 实例不存在，UserDataManager可用，下载失败 -> 成功创建（容错）
            Arguments.of("R4", false, true, false, true, true, false, true, true, UserChrome.BrowserStatus.READY),
            
            // R5: 实例不存在，不是PluginManageImpl -> 成功创建（跳过MuenDriver）
            Arguments.of("R5", false, true, true, false, false, false, true, false, UserChrome.BrowserStatus.READY)
        );
    }

    /**
     * 决策表：删除浏览器实例测试
     * 
     * 条件：
     * C1: 实例是否存在
     * C2: ControlClientSet是否可用
     * C3: MediaClientSet是否可用
     * C4: UserDataManager是否可用
     * C5: 数据上传是否成功
     * 
     * 动作：
     * A1: 是否调用removeClient
     * A2: 是否上传用户数据
     * A3: 是否调用closeInstance
     * A4: 实例是否从map中移除
     */
    @ParameterizedTest(name = "删除浏览器实例决策表 - 规则{0}")
    @MethodSource("deleteInstanceDecisionTable")
    @DisplayName("删除浏览器实例决策表测试")
    void testDeleteInstanceDecisionTable(
            String ruleId,
            boolean instanceExists,
            boolean controlClientSetAvailable,
            boolean mediaClientSetAvailable,
            boolean userDataManagerAvailable,
            boolean uploadSuccess,
            boolean shouldRemoveControl,
            boolean shouldRemoveMedia,
            boolean shouldUploadData,
            boolean shouldCloseInstance) {
        
        String userId = TEST_USER_ID;
        InitBrowserRequest request = createRequest();
        
        // 设置Mock行为
        java.util.concurrent.ConcurrentMap<String, UserChrome> userChromeMap = 
                new java.util.concurrent.ConcurrentHashMap<>();
        ReflectionTestUtils.setField(chromeSet, "userChromeMap", userChromeMap);
        
        if (instanceExists) {
            UserChrome userChrome = new UserChrome(userId, request);
            userChrome.setStatus(UserChrome.BrowserStatus.READY);
            userChromeMap.put(userId, userChrome);
        }

        if (controlClientSetAvailable) {
            ReflectionTestUtils.setField(chromeSet, "controlClientSet", controlClientSet);
        } else {
            ReflectionTestUtils.setField(chromeSet, "controlClientSet", null);
        }

        if (mediaClientSetAvailable) {
            ReflectionTestUtils.setField(chromeSet, "mediaClientSet", mediaClientSet);
        } else {
            ReflectionTestUtils.setField(chromeSet, "mediaClientSet", null);
        }

        if (userDataManagerAvailable) {
            ReflectionTestUtils.setField(chromeSet, "userDataManager", userDataManager);
            if (shouldUploadData) {
                if (uploadSuccess) {
                    when(userDataManager.uploadUserData(anyString(), anyString(), anyString())).thenReturn(true);
                } else {
                    doThrow(new RuntimeException("上传失败")).when(userDataManager)
                            .uploadUserData(anyString(), anyString(), anyString());
                }
            }
        } else {
            ReflectionTestUtils.setField(chromeSet, "userDataManager", null);
        }

        // 执行测试
        chromeSet.delete(userId);

        // 验证结果
        if (instanceExists) {
            if (shouldRemoveControl && controlClientSetAvailable) {
                verify(controlClientSet, times(1)).removeClient(userId);
            }
            
            if (shouldRemoveMedia && mediaClientSetAvailable) {
                verify(mediaClientSet, times(1)).removeClient(userId);
            }
            
            if (shouldUploadData && userDataManagerAvailable) {
                verify(userDataManager, times(1)).uploadUserData(anyString(), anyString(), anyString());
            }
            
            // 验证实例已从map中移除
            assertNull(chromeSet.get(userId));
        } else {
            // 实例不存在时，不应该调用任何方法
            verify(controlClientSet, never()).removeClient(anyString());
            verify(mediaClientSet, never()).removeClient(anyString());
            if (userDataManagerAvailable) {
                verify(userDataManager, never()).uploadUserData(anyString(), anyString(), anyString());
            }
        }
    }

    /**
     * 决策表数据源：删除浏览器实例
     */
    static Stream<Arguments> deleteInstanceDecisionTable() {
        return Stream.of(
            // R1: 实例不存在 -> 不执行任何操作
            Arguments.of("R1", false, true, true, true, true, false, false, false, false),
            
            // R2: 实例存在，所有服务可用，上传成功 -> 完整删除流程
            Arguments.of("R2", true, true, true, true, true, true, true, true, true),
            
            // R3: 实例存在，ControlClientSet不可用 -> 跳过控制流清理
            Arguments.of("R3", true, false, true, true, true, false, true, true, true),
            
            // R4: 实例存在，MediaClientSet不可用 -> 跳过媒体流清理
            Arguments.of("R4", true, true, false, true, true, true, false, true, true),
            
            // R5: 实例存在，UserDataManager不可用 -> 跳过数据上传
            Arguments.of("R5", true, true, true, false, false, true, true, false, true),
            
            // R6: 实例存在，UserDataManager可用，上传失败 -> 继续删除（容错）
            Arguments.of("R6", true, true, true, true, false, true, true, true, true)
        );
    }

    /**
     * 测试获取浏览器实例
     */
    @Test
    @DisplayName("获取浏览器实例")
    void testGet() {
        String userId = TEST_USER_ID;
        InitBrowserRequest request = createRequest();
        UserChrome userChrome = new UserChrome(userId, request);
        
        java.util.concurrent.ConcurrentMap<String, UserChrome> userChromeMap = 
                new java.util.concurrent.ConcurrentHashMap<>();
        userChromeMap.put(userId, userChrome);
        ReflectionTestUtils.setField(chromeSet, "userChromeMap", userChromeMap);
        
        UserChrome result = chromeSet.get(userId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        
        // 测试不存在的实例
        assertNull(chromeSet.get("non_existent_user"));
    }

    /**
     * 测试删除所有实例
     */
    @Test
    @DisplayName("删除所有浏览器实例")
    void testDeleteAll() {
        InitBrowserRequest request = createRequest();
        String userId1 = "user1";
        String userId2 = "user2";
        
        java.util.concurrent.ConcurrentMap<String, UserChrome> userChromeMap = 
                new java.util.concurrent.ConcurrentHashMap<>();
        userChromeMap.put(userId1, new UserChrome(userId1, request));
        userChromeMap.put(userId2, new UserChrome(userId2, request));
        ReflectionTestUtils.setField(chromeSet, "userChromeMap", userChromeMap);
        
        ReflectionTestUtils.setField(chromeSet, "controlClientSet", controlClientSet);
        ReflectionTestUtils.setField(chromeSet, "mediaClientSet", mediaClientSet);
        ReflectionTestUtils.setField(chromeSet, "userDataManager", userDataManager);
        
        chromeSet.deleteAll();
        
        assertTrue(chromeSet.getAllUser().isEmpty());
        verify(controlClientSet, times(2)).removeClient(anyString());
        verify(mediaClientSet, times(2)).removeClient(anyString());
    }

    /**
     * 测试获取所有用户
     */
    @Test
    @DisplayName("获取所有用户")
    void testGetAllUser() {
        InitBrowserRequest request = createRequest();
        String userId1 = "user1";
        String userId2 = "user2";
        
        java.util.concurrent.ConcurrentMap<String, UserChrome> userChromeMap = 
                new java.util.concurrent.ConcurrentHashMap<>();
        userChromeMap.put(userId1, new UserChrome(userId1, request));
        userChromeMap.put(userId2, new UserChrome(userId2, request));
        ReflectionTestUtils.setField(chromeSet, "userChromeMap", userChromeMap);
        
        Set<String> allUsers = chromeSet.getAllUser();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(userId1));
        assertTrue(allUsers.contains(userId2));
    }

    // ========== 辅助方法 ==========

    private static InitBrowserRequest createRequest() {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei(TEST_IMEI);
        request.setImsi(TEST_IMSI);
        request.setLcdWidth(1920);
        request.setLcdHeight(1080);
        request.setAppType("mobile");
        return request;
    }
}
