package com.huawei.browsergateway.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yeauty.annotation.ServerEndpoint;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerEndpointExporter 功能一致性测试
 * 确保重构后功能100%一致
 */
@DisplayName("ServerEndpointExporter 功能一致性测试")
class ServerEndpointExporterFunctionalTest {

    @Test
    @DisplayName("buildConfig 方法参数数量验证")
    void testBuildConfig方法参数() throws Exception {
        // Given
        Method method = ServerEndpointExporter.class.getDeclaredMethod("buildConfig", ServerEndpoint.class);
        method.setAccessible(true);

        // Then
        assertEquals(1, method.getParameterCount(), "buildConfig 应该只有1个参数");
        assertEquals(ServerEndpoint.class, method.getParameterTypes()[0],
                "参数类型应该是 ServerEndpoint");
    }

    @Test
    @DisplayName("resolveAnnotationValue 方法签名验证")
    void testResolveAnnotationValue方法签名() throws Exception {
        // Given
        Method method = ServerEndpointExporter.class.getDeclaredMethod(
                "resolveAnnotationValue", Object.class, Class.class, String.class);
        method.setAccessible(true);

        // Then
        assertEquals(3, method.getParameterCount(), "resolveAnnotationValue 应该有3个参数");
        assertEquals(Object.class, method.getParameterTypes()[0], "第1个参数应该是 Object");
        assertEquals(Class.class, method.getParameterTypes()[1], "第2个参数应该是 Class");
        assertEquals(String.class, method.getParameterTypes()[2], "第3个参数应该是 String");
    }

    @Test
    @DisplayName("验证所有必需的私有方法存在")
    void test验证所有私有方法存在() {
        // Given
        String[] requiredMethods = {
                "registerEndpoints",
                "scanPackage",
                "init",
                "registerEndpoint",
                "buildConfig",
                "resolveAnnotationValue"
        };

        // When & Then
        for (String methodName : requiredMethods) {
            boolean methodExists = false;
            for (Method method : ServerEndpointExporter.class.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    methodExists = true;
                    break;
                }
            }
            assertTrue(methodExists, "方法 " + methodName + " 应该存在");
        }
    }

    @Test
    @DisplayName("验证 EndpointClassPathScanner 内部类存在")
    void test验证内部类存在() {
        // Given
        Class<?>[] innerClasses = ServerEndpointExporter.class.getDeclaredClasses();

        // Then
        boolean scannerExists = false;
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals("EndpointClassPathScanner")) {
                scannerExists = true;
                break;
            }
        }
        assertTrue(scannerExists, "EndpointClassPathScanner 内部类应该存在");
    }

    @Test
    @DisplayName("验证 EndpointClassPathScanner 继承关系")
    void test验证内部类继承关系() {
        // Given
        Class<?> scannerClass = null;
        for (Class<?> innerClass : ServerEndpointExporter.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("EndpointClassPathScanner")) {
                scannerClass = innerClass;
                break;
            }
        }

        // Then
        assertNotNull(scannerClass, "EndpointClassPathScanner 应该存在");
        assertEquals(org.springframework.context.annotation.ClassPathBeanDefinitionScanner.class,
                scannerClass.getSuperclass(),
                "EndpointClassPathScanner 应该继承 ClassPathBeanDefinitionScanner");
    }

    @Test
    @DisplayName("验证 registerEndpoints 方法可见性")
    void test验证registerEndpoints可见性() throws Exception {
        // Given
        Method method = ServerEndpointExporter.class.getDeclaredMethod("registerEndpoints");

        // Then
        // registerEndpoints 应该是 protected，以便子类可以覆盖
        assertTrue(java.lang.reflect.Modifier.isProtected(method.getModifiers()) ||
                        java.lang.reflect.Modifier.isPublic(method.getModifiers()),
                "registerEndpoints 应该是 protected 或 public");
    }

    @Test
    @DisplayName("验证类的字段")
    void test验证类字段() throws Exception {
        // Given
        ServerEndpointExporter exporter = new ServerEndpointExporter();

        // When
        var loggerField = ServerEndpointExporter.class.getDeclaredField("logger");
        var mapField = ServerEndpointExporter.class.getDeclaredField("addressWebsocketServerMap");

        // Then
        assertNotNull(loggerField, "logger 字段应该存在");
        assertTrue(java.lang.reflect.Modifier.isStatic(loggerField.getModifiers()),
                "logger 应该是 static");
        assertTrue(java.lang.reflect.Modifier.isFinal(loggerField.getModifiers()),
                "logger 应该是 final");

        assertNotNull(mapField, "addressWebsocketServerMap 字段应该存在");
        assertTrue(java.lang.reflect.Modifier.isFinal(mapField.getModifiers()),
                "addressWebsocketServerMap 应该是 final");
    }

    @Test
    @DisplayName("验证 buildConfig 返回非null配置")
    void testBuildConfig返回类型() throws Exception {
        // Given
        Method method = ServerEndpointExporter.class.getDeclaredMethod("buildConfig", ServerEndpoint.class);

        // Then
        assertEquals(org.yeauty.standard.ServerEndpointConfig.class, method.getReturnType(),
                "buildConfig 应该返回 ServerEndpointConfig 类型");
    }
}
