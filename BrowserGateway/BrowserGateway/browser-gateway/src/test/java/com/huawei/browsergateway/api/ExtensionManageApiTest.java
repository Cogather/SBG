package com.huawei.browsergateway.api;

import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.service.ExtensionManageService;
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
import static org.mockito.Mockito.*;

/**
 * 扩展管理API测试类
 * 采用决策表（Decision Table）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("扩展管理API测试")
class ExtensionManageApiTest {

    // 测试常量
    private static final String TEST_PLUGIN_NAME = "test-plugin";
    private static final String TEST_VERSION = "1.0.0";
    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_EXTENSION_FILE_PATH = "test-extension.jar";

    @Mock
    private ExtensionManageService extensionManageService;

    @InjectMocks
    private ExtensionManageApi extensionManageApi;

    /**
     * 决策表：加载扩展接口测试
     * 
     * 条件（Conditions）：
     * C1: 请求参数是否为null
     * C2: name是否为空
     * C3: version是否为空
     * C4: bucketName是否为空
     * C5: extensionFilePath是否为空
     * C6: extensionFilePath是否以.jar结尾
     * C7: 插件加载是否成功
     * 
     * 动作（Actions）：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否调用extensionManageService.loadExtension
     */
    @ParameterizedTest(name = "加载扩展接口测试 - 规则{index}: {0}")
    @MethodSource("loadExtensionTestCases")
    @DisplayName("加载扩展接口参数验证和业务逻辑测试")
    void testLoadExtension(
            String description,
            LoadExtensionRequest request,
            boolean loadSuccess,
            int expectedCode,
            String expectedMessage,
            boolean shouldCallService
    ) {
        // Given
        if (request != null && shouldCallService) {
            when(extensionManageService.loadExtension(request)).thenReturn(loadSuccess);
        }

        // When
        CommonResult<LoadExtensionResponse> result = extensionManageApi.loadExtension(request);

        // Then
        assertNotNull(result);
        assertEquals(expectedCode, result.getCode());
        if (expectedMessage != null) {
            assertTrue(result.getMessage().contains(expectedMessage) || 
                      result.getMessage().equals(expectedMessage));
        }

        if (shouldCallService && request != null) {
            verify(extensionManageService, times(1)).loadExtension(request);
        } else {
            verify(extensionManageService, never()).loadExtension(any());
        }
    }

    static Stream<Arguments> loadExtensionTestCases() {
        return Stream.of(
            // R1: 请求参数为null → 返回400错误
            Arguments.of(
                "R1: 请求参数为null",
                null,
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R2: name为空 → 返回400错误（实际代码中可能不会验证，这里按文档设计）
            Arguments.of(
                "R2: name为空",
                createLoadRequest("", TEST_VERSION, TEST_BUCKET_NAME, TEST_EXTENSION_FILE_PATH),
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R3: version为空 → 返回400错误
            Arguments.of(
                "R3: version为空",
                createLoadRequest(TEST_PLUGIN_NAME, "", TEST_BUCKET_NAME, TEST_EXTENSION_FILE_PATH),
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R4: bucketName为空 → 返回400错误
            Arguments.of(
                "R4: bucketName为空",
                createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, "", TEST_EXTENSION_FILE_PATH),
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R5: extensionFilePath为空 → 返回400错误
            Arguments.of(
                "R5: extensionFilePath为空",
                createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, ""),
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R6: extensionFilePath不以.jar结尾 → 返回400错误
            Arguments.of(
                "R6: extensionFilePath不以.jar结尾",
                createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, "test-extension.zip"),
                false,
                ResultCode.VALIDATE_ERROR.getCode(),
                null,
                false
            ),
            // R7: 所有参数有效，加载成功 → 返回200成功
            Arguments.of(
                "R7: 所有参数有效，加载成功",
                createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_EXTENSION_FILE_PATH),
                true,
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                true
            ),
            // R8: 所有参数有效，加载失败 → 返回1002错误（实际代码返回500）
            Arguments.of(
                "R8: 所有参数有效，加载失败",
                createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_EXTENSION_FILE_PATH),
                false,
                ResultCode.FAIL.getCode(),
                "reload extension failed",
                true
            )
        );
    }

    /**
     * 决策表：获取插件信息接口测试
     * 
     * 条件（Conditions）：
     * C1: 插件是否已加载
     * 
     * 动作（Actions）：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否返回插件信息
     */
    @ParameterizedTest(name = "获取插件信息接口测试 - 规则{index}: {0}")
    @MethodSource("getPluginInfoTestCases")
    @DisplayName("获取插件信息接口测试")
    void testGetPluginInfo(
            String description,
            PluginActive pluginActive,
            int expectedCode,
            boolean shouldReturnData
    ) {
        // Given
        when(extensionManageService.getPluginInfo()).thenReturn(pluginActive);

        // When
        CommonResult<PluginActive> result = extensionManageApi.getPluginInfo();

        // Then
        assertNotNull(result);
        assertEquals(expectedCode, result.getCode());
        if (shouldReturnData) {
            assertNotNull(result.getData());
            assertEquals(pluginActive, result.getData());
        } else {
            assertNull(result.getData());
        }
        verify(extensionManageService, times(1)).getPluginInfo();
    }

    static Stream<Arguments> getPluginInfoTestCases() {
        PluginActive activePlugin = new PluginActive();
        activePlugin.setName(TEST_PLUGIN_NAME);
        activePlugin.setVersion(TEST_VERSION);
        activePlugin.setStatus("ACTIVE");

        return Stream.of(
            // R1: 插件已加载 → 返回200和插件信息
            Arguments.of(
                "R1: 插件已加载",
                activePlugin,
                ResultCode.SUCCESS.getCode(),
                true
            ),
            // R2: 插件未加载 → 返回null（实际代码可能返回null，这里按文档设计）
            Arguments.of(
                "R2: 插件未加载",
                null,
                ResultCode.SUCCESS.getCode(),
                false
            )
        );
    }

    /**
     * 异常场景测试：插件管理服务异常
     */
    @Test
    @DisplayName("异常场景：插件管理服务异常")
    void testLoadExtensionWithServiceException() {
        // Given
        LoadExtensionRequest request = createLoadRequest(
            TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_EXTENSION_FILE_PATH
        );
        when(extensionManageService.loadExtension(request))
            .thenThrow(new RuntimeException("Service exception"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            extensionManageApi.loadExtension(request);
        });
        verify(extensionManageService, times(1)).loadExtension(request);
    }

    /**
     * 异常场景测试：获取插件信息异常
     */
    @Test
    @DisplayName("异常场景：获取插件信息异常")
    void testGetPluginInfoWithException() {
        // Given
        when(extensionManageService.getPluginInfo())
            .thenThrow(new RuntimeException("Service exception"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            extensionManageApi.getPluginInfo();
        });
        verify(extensionManageService, times(1)).getPluginInfo();
    }

    /**
     * 辅助方法：创建加载扩展请求
     */
    private static LoadExtensionRequest createLoadRequest(
            String name, String version, String bucketName, String extensionFilePath) {
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setName(name);
        request.setVersion(version);
        request.setBucketName(bucketName);
        request.setExtensionFilePath(extensionFilePath);
        request.setType("ChromeExtend");
        return request;
    }
}
