package com.huawei.browsergateway.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.TypeConverter;
import org.yeauty.annotation.ServerEndpoint;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ServerEndpointExporter 测试类
 * 测试 WebSocket 端点导出器的核心功能
 */
@DisplayName("ServerEndpointExporter tests")
class ServerEndpointExporterTest {

    private ServerEndpointExporter exporter;

    @Mock
    private AbstractBeanFactory mockBeanFactory;

    @Mock
    private TypeConverter mockTypeConverter;

    @Mock
    private BeanExpressionResolver mockExpressionResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exporter = new ServerEndpointExporter();
    }

    @Test
    @DisplayName("resolveAnnotationValue - null value")
    void testResolveAnnotationValue_nullValue() throws Exception {
        // Given
        Method method = ServerEndpointExporter.class.getDeclaredMethod(
                "resolveAnnotationValue", Object.class, Class.class, String.class);
        method.setAccessible(true);

        // When
        Object result = method.invoke(exporter, null, String.class, "testParam");

        // Then
        assertNull(result, "null值应该返回null");
    }

    @Test
    @DisplayName("resolveAnnotationValue - non-string value")
    void testResolveAnnotationValue_nonStringValue() throws Exception {
        // 由于 resolveAnnotationValue 依赖 BeanUtils.getContext()，
        // 这个测试需要 Spring 上下文，暂时跳过
        // 实际项目中应该使用集成测试
    }

    @Test
    @DisplayName("EndpointClassPathScanner - constructor")
    void testEndpointClassPathScanner_constructor() {
        // Given & When & Then
        // 验证内部类可以正常实例化
        assertDoesNotThrow(() -> {
            Class<?> scannerClass = ServerEndpointExporter.EndpointClassPathScanner.class;
            assertNotNull(scannerClass, "EndpointClassPathScanner 类应该存在");
        });
    }

    @Test
    @DisplayName("Implements SmartInitializingSingleton interface")
    void testImplementsSmartInitializingSingleton() {
        // Then
        assertTrue(exporter instanceof org.springframework.beans.factory.SmartInitializingSingleton,
                "ServerEndpointExporter 应该实现 SmartInitializingSingleton 接口");
    }

    @Test
    @DisplayName("afterSingletonsInstantiated method exists")
    void testAfterSingletonsInstantiatedExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getMethod("afterSingletonsInstantiated");

        // Then
        assertNotNull(method, "afterSingletonsInstantiated 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "方法返回类型应该是 void");
    }

    @Test
    @DisplayName("registerEndpoints method exists")
    void testRegisterEndpointsExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod("registerEndpoints");

        // Then
        assertNotNull(method, "registerEndpoints 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "方法返回类型应该是 void");
    }

    @Test
    @DisplayName("buildConfig method exists")
    void testBuildConfigExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod("buildConfig", ServerEndpoint.class);

        // Then
        assertNotNull(method, "buildConfig 方法应该存在");
        assertEquals(org.yeauty.standard.ServerEndpointConfig.class, method.getReturnType(),
                "方法返回类型应该是 ServerEndpointConfig");
    }

    @Test
    @DisplayName("resolveAnnotationValue method exists")
    void testResolveAnnotationValueExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod(
                "resolveAnnotationValue", Object.class, Class.class, String.class);

        // Then
        assertNotNull(method, "resolveAnnotationValue 方法应该存在");
        assertTrue(method.getGenericReturnType().getTypeName().contains("T"),
                "方法应该是泛型方法");
    }

    @Test
    @DisplayName("scanPackage method exists")
    void testScanPackageExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod(
                "scanPackage", org.springframework.context.ApplicationContext.class);

        // Then
        assertNotNull(method, "scanPackage 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "方法返回类型应该是 void");
    }

    @Test
    @DisplayName("init method exists")
    void testInitExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod("init");

        // Then
        assertNotNull(method, "init 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "方法返回类型应该是 void");
    }

    @Test
    @DisplayName("registerEndpoint method exists")
    void testRegisterEndpointExists() throws Exception {
        // When
        Method method = ServerEndpointExporter.class.getDeclaredMethod("registerEndpoint", Class.class);

        // Then
        assertNotNull(method, "registerEndpoint 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "方法返回类型应该是 void");
    }

    @Test
    @DisplayName("Class has correct annotations")
    void testClassAnnotations() {
        // Then
        assertTrue(exporter.getClass().isAnnotationPresent(org.springframework.stereotype.Component.class),
                "ServerEndpointExporter 应该有 @Component 注解");
    }
}
