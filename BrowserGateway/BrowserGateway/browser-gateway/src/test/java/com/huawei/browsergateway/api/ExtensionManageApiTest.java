package com.huawei.browsergateway.api;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.common.response.BaseResponse;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.entity.response.PluginInfoResponse;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IExtensionManage;
import com.huawei.browsergateway.service.IPluginManage;

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

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExtensionManageApi单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionManageApi决策表测试")
class ExtensionManageApiTest {

    @Mock
    private IPluginManage pluginManage;

    @Mock
    private IChromeSet chromeSet;

    @Mock
    private IExtensionManage extensionManageService;

    @InjectMocks
    private ExtensionManageApi extensionManageApi;

    @BeforeEach
    void setUp() {
        // ExtensionManageApi 不需要 pluginTempDir 字段
        // pluginTempDir 字段只存在于 PluginManageImpl 中，通过 mock 的 pluginManage 服务使用
    }

    private static final String TEST_PLUGIN_NAME = "test-plugin";
    private static final String TEST_VERSION = "1.0.0";
    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_FILE_PATH = "plugins/test-plugin-1.0.0.jar";

    /**
     * 决策表：加载扩展接口测试
     * 
     * 条件：
     * C1: 请求参数是否为null
     * C2: name是否为空
     * C3: version是否为空
     * C4: bucketName是否为空
     * C5: extensionFilePath是否为空
     * C6: extensionFilePath是否以.jar结尾
     * C7: 插件加载是否成功
     * 
     * 动作：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否调用chromeSet.deleteAll
     * A4: 是否调用pluginManage.loadExtension
     */
    @ParameterizedTest(name = "加载扩展决策表 - 规则{0}")
    @MethodSource("loadExtensionDecisionTable")
    @DisplayName("加载扩展决策表测试")
    void testLoadExtensionDecisionTable(
            String ruleId,
            LoadExtensionRequest request,
            boolean loadSuccess,
            String expectedCode,
            String expectedMessage,
            boolean shouldCallDeleteAll,
            boolean shouldCallLoadExtension,
            boolean shouldThrowException) {
        
        // 设置Mock行为
        if (shouldCallLoadExtension && !shouldThrowException) {
            // mock extensionManageService.loadExtension 方法
            when(extensionManageService.loadExtension(any(LoadExtensionRequest.class))).thenReturn(loadSuccess);
        }

        // 执行测试
        BaseResponse<LoadExtensionResponse> response = extensionManageApi.loadExtension(request);
        assertEquals(expectedCode, String.valueOf(response.getCode()));
        if (expectedMessage != null) {
            assertTrue(response.getMessage().contains(expectedMessage));
        }
        
        if (shouldThrowException) {
            // 参数验证失败时，不应该调用chromeSet.deleteAll和extensionManageService.loadExtension
            verify(chromeSet, never()).deleteAll();
            verify(extensionManageService, never()).loadExtension(any(LoadExtensionRequest.class));
        } else {
            if (shouldCallDeleteAll) {
                verify(chromeSet, times(1)).deleteAll();
            }
            
            if (shouldCallLoadExtension) {
                verify(extensionManageService, times(1)).loadExtension(any(LoadExtensionRequest.class));
            }
        }
    }

    /**
     * 决策表数据源：加载扩展
     */
    static Stream<Arguments> loadExtensionDecisionTable() {
        return Stream.of(
            // R1: 请求参数为null -> 返回错误
            Arguments.of("R1", null, false, "400", "请求参数不能为空", false, false, true),
            
            // R2: name为空 -> 返回错误
            Arguments.of("R2", createLoadRequest("", TEST_VERSION, TEST_BUCKET_NAME, TEST_FILE_PATH),
                    false, "400", "插件名称(name)不能为空", false, false, true),
            
            // R3: version为空 -> 返回错误
            Arguments.of("R3", createLoadRequest(TEST_PLUGIN_NAME, "", TEST_BUCKET_NAME, TEST_FILE_PATH),
                    false, "400", "插件版本(version)不能为空", false, false, true),
            
            // R4: bucketName为空 -> 返回错误
            Arguments.of("R4", createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, "", TEST_FILE_PATH),
                    false, "400", "存储桶名称(bucketName)不能为空", false, false, true),
            
            // R5: extensionFilePath为空 -> 返回错误
            Arguments.of("R5", createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, ""),
                    false, "400", "扩展文件路径(extensionFilePath)不能为空", false, false, true),
            
            // R6: extensionFilePath不以.jar结尾 -> 返回错误
            Arguments.of("R6", createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, "test.zip"),
                    false, "400", "扩展文件路径必须以.jar结尾", false, false, true),
            
            // R7: 所有参数有效，加载成功 -> 成功
            Arguments.of("R7", createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_FILE_PATH),
                    true, "200", "成功", true, true, false),
            
            // R8: 所有参数有效，加载失败 -> 返回失败
            Arguments.of("R8", createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_FILE_PATH),
                    false, "1002", "reload extension failed", true, true, false) // EXTENSION_LOAD_FAILED
        );
    }

    /**
     * 决策表：获取插件信息接口测试
     * 
     * 条件：
     * C1: 插件是否已加载
     * 
     * 动作：
     * A1: 返回码
     * A2: 返回消息
     * A3: 是否返回插件信息
     */
    @ParameterizedTest(name = "获取插件信息决策表 - 规则{0}: pluginLoaded={1}")
    @MethodSource("getPluginInfoDecisionTable")
    @DisplayName("获取插件信息决策表测试")
    void testGetPluginInfoDecisionTable(
            String ruleId,
            boolean pluginLoaded,
            String expectedCode,
            String expectedMessage,
            boolean shouldReturnPluginInfo) {
        
        // 设置Mock行为
        if (pluginLoaded) {
            PluginActive pluginActive = createPluginActive();
            when(pluginManage.getPluginActive()).thenReturn(pluginActive);
        } else {
            when(pluginManage.getPluginActive()).thenReturn(null);
        }

        // 执行测试
        BaseResponse<PluginInfoResponse> response = extensionManageApi.getPluginInfo();
        
        assertEquals(expectedCode, String.valueOf(response.getCode()));
        if (expectedMessage != null) {
            assertTrue(response.getMessage().contains(expectedMessage));
        }
        
        if (shouldReturnPluginInfo) {
            assertNotNull(response.getData());
            assertEquals(TEST_PLUGIN_NAME, response.getData().getName());
            assertEquals(TEST_VERSION, response.getData().getVersion());
        } else {
            assertNull(response.getData());
        }
        
        verify(pluginManage, times(1)).getPluginActive();
    }

    /**
     * 决策表数据源：获取插件信息
     */
    static Stream<Arguments> getPluginInfoDecisionTable() {
        return Stream.of(
            // R1: 插件已加载 -> 返回插件信息
            Arguments.of("R1", true, "200", "成功", true),
            
            // R2: 插件未加载 -> 返回错误
            Arguments.of("R2", false, "1005", "插件未加载", false) // PLUGIN_NOT_FOUND
        );
    }

    /**
     * 测试异常场景：插件加载时抛出异常
     */
    @Test
    @DisplayName("加载扩展异常场景：插件管理服务异常")
    void testLoadExtensionPluginManageException() {
        LoadExtensionRequest request = createLoadRequest(TEST_PLUGIN_NAME, TEST_VERSION, TEST_BUCKET_NAME, TEST_FILE_PATH);
        // extensionManageService.loadExtension 抛出异常
        when(extensionManageService.loadExtension(any(LoadExtensionRequest.class)))
                .thenThrow(new RuntimeException("插件管理服务异常"));
        
        BaseResponse<LoadExtensionResponse> response = extensionManageApi.loadExtension(request);
        
        assertEquals("1002", String.valueOf(response.getCode())); // EXTENSION_LOAD_FAILED
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("加载扩展失败") || response.getMessage().contains("插件管理服务异常"));
        verify(chromeSet, times(1)).deleteAll();
        verify(extensionManageService, times(1)).loadExtension(any(LoadExtensionRequest.class));
    }

    /**
     * 测试异常场景：获取插件信息时抛出异常
     */
    @Test
    @DisplayName("获取插件信息异常场景：插件管理服务异常")
    void testGetPluginInfoException() {
        when(pluginManage.getPluginActive()).thenThrow(new RuntimeException("获取插件信息异常"));
        
        BaseResponse<PluginInfoResponse> response = extensionManageApi.getPluginInfo();
        
        assertEquals("1005", String.valueOf(response.getCode())); // PLUGIN_NOT_FOUND
        assertTrue(response.getMessage().contains("获取插件信息失败"));
    }

    // ========== 辅助方法 ==========

    private static LoadExtensionRequest createLoadRequest(String name, String version, 
                                                          String bucketName, String filePath) {
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setName(name);
        request.setVersion(version);
        request.setBucketName(bucketName);
        request.setExtensionFilePath(filePath);
        request.setType("jar");
        return request;
    }

    private static PluginActive createPluginActive() {
        PluginActive pluginActive = new PluginActive();
        pluginActive.setName(TEST_PLUGIN_NAME);
        pluginActive.setVersion(TEST_VERSION);
        pluginActive.setType("jar");
        pluginActive.setStatus("ACTIVE");
        pluginActive.setBucketName(TEST_BUCKET_NAME);
        pluginActive.setPackageName("com.example.plugin");
        pluginActive.setLoadTime(LocalDateTime.now().toString());
        return pluginActive;
    }
}
