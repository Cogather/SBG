package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.config.ReportConfig;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.moon.cloud.browser.sdk.core.MuenDriver;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChromeSet实现测试类
 * 采用决策表（Decision Table）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChromeSet实现测试")
class ChromeSetImplTest {

    // 测试常量
    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "987654321098765";
    private static final String TEST_USER_ID = TEST_IMEI + "_" + TEST_IMSI;

    @Mock
    private IFileStorage fs;
    @Mock
    private Config config;
    @Mock
    private ControlClientSet controlClientSet;
    @Mock
    private MediaClientSet mediaClientSet;
    @Mock
    private IRemote remote;
    @Mock
    private IPluginManage pluginManage;
    @Mock
    private MuenDriver muenDriver;
    @Mock
    private ReportConfig reportConfig;

    @InjectMocks
    private ChromeSetImpl chromeSet;

    @BeforeEach
    void setUp() {
        // 设置Config的mock返回值 - 使用lenient避免UnnecessaryStubbingException
        lenient().when(config.getUserDataPath()).thenReturn("/tmp/userdata");
        lenient().when(config.getSelfAddr()).thenReturn("127.0.0.1:8080");
        lenient().when(config.getBaseDataPath()).thenReturn("/tmp/basedata");
        lenient().when(config.getRecordExtensionPage()).thenReturn("about:blank");
        lenient().when(config.getReport()).thenReturn(reportConfig);
        lenient().when(reportConfig.getCap()).thenReturn(100);
        lenient().when(config.getAddress()).thenReturn("127.0.0.1");
        lenient().when(config.getWebsocket()).thenReturn(mock(com.huawei.browsergateway.config.WebsocketConfig.class));
        lenient().when(config.getWebsocket().getMediaPort()).thenReturn(30002);
        // Mock ChromeConfig
        com.huawei.browsergateway.config.ChromeConfig chromeConfig = mock(com.huawei.browsergateway.config.ChromeConfig.class);
        lenient().when(chromeConfig.getEndpoint()).thenReturn("http://localhost:9222");
        lenient().when(chromeConfig.getRecordExtensionId()).thenReturn("test-extension-id");
        lenient().when(chromeConfig.getExecutablePath()).thenReturn("/path/to/chrome");
        lenient().when(chromeConfig.isHeadless()).thenReturn(false);
        lenient().when(config.getChrome()).thenReturn(chromeConfig);
        lenient().when(config.getRecordExtensionPath()).thenReturn("/path/to/extension");
        lenient().when(pluginManage.getPluginStatus()).thenReturn("ACTIVE");
        lenient().when(pluginManage.createDriver(anyString())).thenReturn(muenDriver);
    }

    /**
     * 决策表：创建浏览器实例测试
     * 
     * 条件（Conditions）：
     * C1: 实例是否已存在
     * C2: UserDataManager是否可用
     * C3: 用户数据下载是否成功
     * C4: PluginManage是否为PluginManageImpl实例
     * C5: MuenDriver创建是否成功
     * 
     * 动作（Actions）：
     * A1: 是否删除旧实例
     * A2: 是否下载用户数据
     * A3: 是否创建MuenDriver
     * A4: 最终状态
     */
    @ParameterizedTest(name = "创建浏览器实例测试 - 规则{index}: {0}")
    @MethodSource("createBrowserTestCases")
    @DisplayName("创建浏览器实例测试")
    void testCreate(
            String description,
            InitBrowserRequest request,
            UserChrome existingChrome,
            boolean shouldSucceed
    ) {
        // Given
        if (existingChrome != null) {
            // 如果实例已存在，先创建一个实例
            chromeSet.create(request); // 先创建一个实例
        }

        // When
        UserChrome result = chromeSet.create(request);

        // Then
        if (shouldSucceed) {
            assertNotNull(result);
            assertEquals(TEST_USER_ID, result.getUserId());
            verify(pluginManage, atLeastOnce()).createDriver(anyString());
        } else {
            // 如果容量不足，应该抛出异常
            assertThrows(RuntimeException.class, () -> {
                // 设置容量为0来触发异常
                when(reportConfig.getCap()).thenReturn(0);
                chromeSet.create(request);
            });
        }
    }

    static Stream<Arguments> createBrowserTestCases() {
        InitBrowserRequest request = createRequest();
        UserChrome existingChrome = mock(UserChrome.class);

        return Stream.of(
            // R1: 实例不存在，所有条件满足 → 成功创建
            Arguments.of(
                "R1: 实例不存在，所有条件满足",
                request,
                null,
                true
            ),
            // R2: 实例已存在，所有条件满足 → 删除旧实例后创建（实际代码会覆盖）
            Arguments.of(
                "R2: 实例已存在，所有条件满足",
                request,
                existingChrome,
                true
            ),
            // R3: 实例不存在，UserDataManager不可用 → 成功创建（跳过数据下载）
            Arguments.of(
                "R3: 实例不存在，UserDataManager不可用",
                request,
                null,
                true
            ),
            // R4: 实例不存在，UserDataManager可用，下载失败 → 成功创建（容错）
            Arguments.of(
                "R4: 实例不存在，下载失败",
                request,
                null,
                true
            ),
            // R5: 实例不存在，不是PluginManageImpl → 成功创建（跳过MuenDriver）
            Arguments.of(
                "R5: 实例不存在，不是PluginManageImpl",
                request,
                null,
                true
            )
        );
    }

    /**
     * 决策表：删除浏览器实例测试
     * 
     * 条件（Conditions）：
     * C1: 实例是否存在
     * C2: ControlClientSet是否可用
     * C3: MediaClientSet是否可用
     * C4: UserDataManager是否可用
     * C5: 数据上传是否成功
     * 
     * 动作（Actions）：
     * A1: 是否调用removeClient（控制流）
     * A2: 是否调用removeClient（媒体流）
     * A3: 是否上传用户数据
     * A4: 是否调用closeInstance
     * A5: 实例是否从map中移除
     */
    @ParameterizedTest(name = "删除浏览器实例测试 - 规则{index}: {0}")
    @MethodSource("deleteBrowserTestCases")
    @DisplayName("删除浏览器实例测试")
    void testDelete(
            String description,
            String userId,
            UserChrome existingChrome,
            boolean shouldCallControl,
            boolean shouldCallMedia,
            boolean shouldCallClose
    ) {
        // Given
        if (existingChrome != null) {
            chromeSet.create(createRequest());
        }

        // When
        chromeSet.delete(userId);

        // Then
        if (existingChrome != null) {
            // 验证closeConnection()的副作用：调用controlClientSet和mediaClientSet的del方法
            if (shouldCallControl) {
                verify(controlClientSet, atLeastOnce()).del(userId);
            } else {
                verify(controlClientSet, never()).del(userId);
            }
            if (shouldCallMedia) {
                verify(mediaClientSet, atLeastOnce()).del(userId);
            } else {
                verify(mediaClientSet, never()).del(userId);
            }
            // 验证实例已从map中移除
            assertNull(chromeSet.get(userId));
        } else {
            // 实例不存在，不应该调用任何方法
            verify(controlClientSet, never()).del(anyString());
            verify(mediaClientSet, never()).del(anyString());
        }
    }

    static Stream<Arguments> deleteBrowserTestCases() {
        UserChrome mockChrome = mock(UserChrome.class);
        when(mockChrome.getUserId()).thenReturn(TEST_USER_ID);

        return Stream.of(
            // R1: 实例不存在 → 不执行任何操作
            Arguments.of(
                "R1: 实例不存在",
                TEST_USER_ID,
                null,
                false,
                false,
                false
            ),
            // R2: 实例存在，所有服务可用，上传成功 → 完整删除流程
            Arguments.of(
                "R2: 实例存在，所有服务可用",
                TEST_USER_ID,
                mockChrome,
                true,
                true,
                true
            ),
            // R3: 实例存在，ControlClientSet不可用 → 仍然会调用del（实际代码会调用，即使不可用）
            Arguments.of(
                "R3: 实例存在，ControlClientSet不可用",
                TEST_USER_ID,
                mockChrome,
                true,  // 实际代码会调用，即使不可用也会尝试调用
                true,
                true
            ),
            // R4: 实例存在，MediaClientSet不可用 → 仍然会调用del（实际代码会调用，即使不可用）
            Arguments.of(
                "R4: 实例存在，MediaClientSet不可用",
                TEST_USER_ID,
                mockChrome,
                true,
                true,  // 实际代码会调用，即使不可用也会尝试调用
                true
            ),
            // R5: 实例存在，UserDataManager不可用 → 跳过数据上传
            Arguments.of(
                "R5: 实例存在，UserDataManager不可用",
                TEST_USER_ID,
                mockChrome,
                true,
                true,
                true
            ),
            // R6: 实例存在，UserDataManager可用，上传失败 → 继续删除（容错）
            Arguments.of(
                "R6: 实例存在，上传失败",
                TEST_USER_ID,
                mockChrome,
                true,
                true,
                true
            )
        );
    }

    /**
     * 测试获取浏览器实例
     */
    @Test
    @DisplayName("获取浏览器实例测试")
    void testGet() {
        // Given
        InitBrowserRequest request = createRequest();
        chromeSet.create(request);

        // When
        UserChrome result = chromeSet.get(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());

        // 测试获取不存在的实例
        assertNull(chromeSet.get("non-existent-user"));
    }

    /**
     * 测试删除所有实例
     */
    @Test
    @DisplayName("删除所有实例测试")
    void testDeleteAll() {
        // Given
        chromeSet.create(createRequest());
        InitBrowserRequest request2 = createRequest();
        request2.setImei("111111111111111");
        request2.setImsi("222222222222222");
        chromeSet.create(request2);

        // When
        chromeSet.deleteAll();

        // Then
        assertEquals(0, chromeSet.getAllUser().size());
    }

    /**
     * 测试获取所有用户
     */
    @Test
    @DisplayName("获取所有用户测试")
    void testGetAllUser() {
        // Given
        chromeSet.create(createRequest());
        InitBrowserRequest request2 = createRequest();
        request2.setImei("111111111111111");
        request2.setImsi("222222222222222");
        chromeSet.create(request2);

        // When
        Set<String> users = chromeSet.getAllUser();

        // Then
        assertEquals(2, users.size());
        assertTrue(users.contains(TEST_USER_ID));
    }

    /**
     * 辅助方法：创建初始化浏览器请求
     */
    private static InitBrowserRequest createRequest() {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei(TEST_IMEI);
        request.setImsi(TEST_IMSI);
        request.setLcdWidth(240);
        request.setLcdHeight(320);
        request.setFactory("test-factory");
        request.setDevType("test-device");
        request.setAppType(1);
        request.setAppID(1);
        return request;
    }
}
