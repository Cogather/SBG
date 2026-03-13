package com.huawei.browsergateway.websocket;

import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.yeauty.annotation.ServerEndpoint;
import org.yeauty.exception.DeploymentException;
import org.yeauty.pojo.PojoEndpointServer;
import org.yeauty.pojo.PojoMethodMapping;
import org.yeauty.standard.ServerEndpointConfig;
import org.yeauty.standard.WebsocketServer;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * WebSocket 服务端点导出器
 * 负责扫描、注册和初始化所有 @ServerEndpoint 注解的 WebSocket 端点类
 */
@Component
public class ServerEndpointExporter implements SmartInitializingSingleton {

    private static final Logger logger = LogManager.getLogger(ServerEndpointExporter.class);
    private static final String BASE_PACKAGE = "com.huawei.browsergateway";

    private final Map<InetSocketAddress, WebsocketServer> addressWebsocketServerMap = new HashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        registerEndpoints();
    }

    /**
     * 端点类路径扫描器
     */
    public static class EndpointClassPathScanner extends ClassPathBeanDefinitionScanner {

        public EndpointClassPathScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
            super(registry, useDefaultFilters);
        }

        @Override
        public Set<BeanDefinitionHolder> doScan(String... basePackages) {
            addIncludeFilter(new AnnotationTypeFilter(ServerEndpoint.class));
            return super.doScan(basePackages);
        }
    }

    /**
     * 注册所有 WebSocket 端点
     */
    protected void registerEndpoints() {
        ApplicationContext context = BeanUtils.getContext();
        scanPackage(context);

        String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
        Set<Class<?>> endpointClasses = collectEndpointClasses(context, endpointBeanNames);

        registerAllEndpoints(endpointClasses);
        init();
    }

    /**
     * 收集端点类
     */
    private Set<Class<?>> collectEndpointClasses(ApplicationContext context, String[] beanNames) {
        Set<Class<?>> endpointClasses = new LinkedHashSet<>();
        for (String beanName : beanNames) {
            endpointClasses.add(context.getType(beanName));
        }
        return endpointClasses;
    }

    /**
     * 注册所有端点类
     */
    private void registerAllEndpoints(Set<Class<?>> endpointClasses) {
        for (Class<?> endpointClass : endpointClasses) {
            Class<?> targetClass = ClassUtils.isCglibProxyClass(endpointClass)
                    ? endpointClass.getSuperclass()
                    : endpointClass;
            registerEndpoint(targetClass);
        }
    }

    /**
     * 扫描指定包路径下的端点类
     */
    private void scanPackage(ApplicationContext context) {
        EndpointClassPathScanner scanner = new EndpointClassPathScanner(
                (BeanDefinitionRegistry) context.getAutowireCapableBeanFactory(), false);
        scanner.setResourceLoader(BeanUtils.getContext());
        scanner.doScan(BASE_PACKAGE);
    }

    /**
     * 初始化并启动所有 WebSocket 服务器
     */
    private void init() {
        for (Map.Entry<InetSocketAddress, WebsocketServer> entry : addressWebsocketServerMap.entrySet()) {
            initWebsocketServer(entry);
        }
    }

    /**
     * 初始化单个 WebSocket 服务器
     */
    private void initWebsocketServer(Map.Entry<InetSocketAddress, WebsocketServer> entry) {
        WebsocketServer websocketServer = entry.getValue();
        try {
            websocketServer.init();
            logServerStartup(websocketServer);
        } catch (InterruptedException e) {
            logger.error(String.format("websocket [%s] init fail", entry.getKey()), e);
        } catch (SSLException e) {
            logger.error(String.format("websocket [%s] ssl create fail", entry.getKey()), e);
        }
    }

    /**
     * 记录服务器启动日志
     */
    private void logServerStartup(WebsocketServer websocketServer) {
        PojoEndpointServer pojoEndpointServer = websocketServer.getPojoEndpointServer();
        StringJoiner pathJoiner = new StringJoiner(",");
        pojoEndpointServer.getPathMatcherSet().forEach(
                pathMatcher -> pathJoiner.add("'" + pathMatcher.getPattern() + "'"));

        logger.info(String.format("\u001b[34mNetty WebSocket started on port: %s with context path(s): %s .\u001b[0m",
                pojoEndpointServer.getPort(), pathJoiner.toString()));
    }

    /**
     * 注册单个 WebSocket 端点
     */
    private void registerEndpoint(Class<?> endpointClass) {
        ServerEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(endpointClass, ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalStateException("missingAnnotation ServerEndpoint");
        }

        ServerEndpointConfig config = buildConfig(annotation);
        PojoMethodMapping methodMapping = createMethodMapping(endpointClass);
        registerOrAddEndpoint(annotation, config, methodMapping);
    }

    /**
     * 创建方法映射
     */
    private PojoMethodMapping createMethodMapping(Class<?> endpointClass) {
        ApplicationContext context = BeanUtils.getContext();
        try {
            return new PojoMethodMapping(endpointClass, context,
                    (AbstractBeanFactory) context.getAutowireCapableBeanFactory());
        } catch (DeploymentException e) {
            throw new IllegalStateException("Failed to register ServerEndpointConfig for class: " + endpointClass, e);
        }
    }

    /**
     * 注册或添加端点到服务器
     */
    private void registerOrAddEndpoint(ServerEndpoint annotation, ServerEndpointConfig config,
                                       PojoMethodMapping methodMapping) {
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        String path = resolveAnnotationValue(annotation.value(), String.class, "path");
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("WebSocket endpoint path cannot be null or empty");
        }

        WebsocketServer server = addressWebsocketServerMap.get(address);
        if (server == null) {
            createNewServer(address, config, methodMapping, path);
        } else {
            server.getPojoEndpointServer().addPathPojoMethodMapping(path, methodMapping);
        }
    }

    /**
     * 创建新的 WebSocket 服务器
     */
    private void createNewServer(InetSocketAddress address, ServerEndpointConfig config,
                                 PojoMethodMapping methodMapping, String path) {
        PojoEndpointServer pojoEndpointServer = new PojoEndpointServer(methodMapping, config, path);
        WebsocketServer websocketServer = new WebsocketServer(pojoEndpointServer, config);
        addressWebsocketServerMap.put(address, websocketServer);
    }

    /**
     * 从注解构建服务端点配置
     */
    private ServerEndpointConfig buildConfig(ServerEndpoint annotation) {
        // 基础配置（必需字段）
        String host = resolveStringRequired(annotation.host(), "host", "0.0.0.0");
        int port = resolveInt(annotation.port(), "port");

        // 线程配置
        int bossThreads = resolveInt(annotation.bossLoopGroupThreads(), "bossLoopGroupThreads");
        int workerThreads = resolveInt(annotation.workerLoopGroupThreads(), "workerLoopGroupThreads");

        // 通用选项
        boolean useCompression = resolveBoolean(annotation.useCompressionHandler(), "useCompressionHandler");
        int connectTimeout = resolveInt(annotation.optionConnectTimeoutMillis(), "optionConnectTimeoutMillis");
        int soBacklog = resolveInt(annotation.optionSoBacklog(), "optionSoBacklog");

        // 子选项 - 写相关
        int writeSpinCount = resolveInt(annotation.childOptionWriteSpinCount(), "childOptionWriteSpinCount");
        int writeBufferHigh = resolveInt(annotation.childOptionWriteBufferHighWaterMark(), "childOptionWriteBufferHighWaterMark");
        int writeBufferLow = resolveInt(annotation.childOptionWriteBufferLowWaterMark(), "childOptionWriteBufferLowWaterMark");

        // 子选项 - Socket相关
        int soRcvbuf = resolveInt(annotation.childOptionSoRcvbuf(), "childOptionSoRcvbuf");
        int soSndbuf = resolveInt(annotation.childOptionSoSndbuf(), "childOptionSoSndbuf");
        boolean tcpNodelay = resolveBoolean(annotation.childOptionTcpNodelay(), "childOptionTcpNodelay");
        boolean soKeepalive = resolveBoolean(annotation.childOptionSoKeepalive(), "childOptionSoKeepalive");
        int soLinger = resolveInt(annotation.childOptionSoLinger(), "childOptionSoLinger");
        boolean allowHalfClosure = resolveBoolean(annotation.childOptionAllowHalfClosure(), "childOptionAllowHalfClosure");

        // 空闲超时配置
        int readerIdleSeconds = resolveInt(annotation.readerIdleTimeSeconds(), "readerIdleTimeSeconds");
        int writerIdleSeconds = resolveInt(annotation.writerIdleTimeSeconds(), "writerIdleTimeSeconds");
        int allIdleSeconds = resolveInt(annotation.allIdleTimeSeconds(), "allIdleTimeSeconds");

        // WebSocket配置
        int maxFrameLength = resolveInt(annotation.maxFramePayloadLength(), "maxFramePayloadLength");
        boolean useEventExecutor = resolveBoolean(annotation.useEventExecutorGroup(), "useEventExecutorGroup");
        int eventExecutorThreads = resolveInt(annotation.eventExecutorGroupThreads(), "eventExecutorGroupThreads");

        // SSL配置（可选）
        SslConfig sslConfig = resolveSslConfig(annotation);

        // CORS配置（可选）
        CorsConfig corsConfig = resolveCorsConfig(annotation);

        return new ServerEndpointConfig(
                host, port, bossThreads, workerThreads, useCompression, connectTimeout, soBacklog,
                writeSpinCount, writeBufferHigh, writeBufferLow, soRcvbuf, soSndbuf, tcpNodelay,
                soKeepalive, soLinger, allowHalfClosure, readerIdleSeconds, writerIdleSeconds,
                allIdleSeconds, maxFrameLength, useEventExecutor, eventExecutorThreads,
                sslConfig.keyPassword, sslConfig.keyStore, sslConfig.keyStorePassword, sslConfig.keyStoreType,
                sslConfig.trustStore, sslConfig.trustStorePassword, sslConfig.trustStoreType,
                corsConfig.origins, corsConfig.allowCredentials);
    }

    /**
     * SSL 配置内部类
     */
    private static class SslConfig {
        String keyPassword;
        String keyStore;
        String keyStorePassword;
        String keyStoreType;
        String trustStore;
        String trustStorePassword;
        String trustStoreType;
    }

    /**
     * CORS 配置内部类
     */
    private static class CorsConfig {
        String[] origins;
        Boolean allowCredentials;
    }

    /**
     * 解析 SSL 配置
     */
    private SslConfig resolveSslConfig(ServerEndpoint annotation) {
        SslConfig config = new SslConfig();
        config.keyPassword = resolveString(annotation.sslKeyPassword(), "sslKeyPassword");
        config.keyStore = resolveString(annotation.sslKeyStore(), "sslKeyStore");
        config.keyStorePassword = resolveString(annotation.sslKeyStorePassword(), "sslKeyStorePassword");
        config.keyStoreType = resolveString(annotation.sslKeyStoreType(), "sslKeyStoreType");
        config.trustStore = resolveString(annotation.sslTrustStore(), "sslTrustStore");
        config.trustStorePassword = resolveString(annotation.sslTrustStorePassword(), "sslTrustStorePassword");
        config.trustStoreType = resolveString(annotation.sslTrustStoreType(), "sslTrustStoreType");
        return config;
    }

    /**
     * 解析 CORS 配置
     */
    private CorsConfig resolveCorsConfig(ServerEndpoint annotation) {
        CorsConfig config = new CorsConfig();
        config.origins = resolveCorsOrigins(annotation.corsOrigins());
        config.allowCredentials = resolveBoolean(annotation.corsAllowCredentials(), "corsAllowCredentials");
        return config;
    }

    /**
     * 解析 CORS 源配置
     */
    private String[] resolveCorsOrigins(String[] origins) {
        if (origins.length == 0) {
            return origins;
        }
        String[] resolved = new String[origins.length];
        for (int i = 0; i < origins.length; i++) {
            resolved[i] = resolveString(origins[i], "corsOrigins");
        }
        return resolved;
    }

    /**
     * 解析字符串类型的注解值
     */
    private String resolveString(Object value, String paramName) {
        return resolveAnnotationValue(value, String.class, paramName);
    }

    /**
     * 解析必需的字符串类型注解值，如果为null则使用默认值
     */
    private String resolveStringRequired(Object value, String paramName, String defaultValue) {
        String result = resolveAnnotationValue(value, String.class, paramName);
        return result != null ? result : defaultValue;
    }

    /**
     * 解析整数类型的注解值
     */
    private int resolveInt(Object value, String paramName) {
        Integer result = resolveAnnotationValue(value, Integer.class, paramName);
        return result != null ? result : 0;
    }

    /**
     * 解析布尔类型的注解值
     */
    private boolean resolveBoolean(Object value, String paramName) {
        Boolean result = resolveAnnotationValue(value, Boolean.class, paramName);
        return result != null ? result : false;
    }

    private <T> T resolveAnnotationValue(Object value, Class<T> requiredType, String paramName) {
        if (value == null) {
            return null;
        } else {
            AbstractBeanFactory beanFactory = (AbstractBeanFactory) BeanUtils.getContext().getAutowireCapableBeanFactory();
            TypeConverter typeConverter = beanFactory.getTypeConverter();
            if (value instanceof String) {
                String strVal = beanFactory.resolveEmbeddedValue((String) value);
                BeanExpressionResolver beanExpressionResolver = beanFactory.getBeanExpressionResolver();
                if (beanExpressionResolver != null) {
                    value = beanExpressionResolver.evaluate(strVal, new BeanExpressionContext(beanFactory, (Scope) null));
                } else {
                    value = strVal;
                }
            }

            try {
                return (T) typeConverter.convertIfNecessary(value, requiredType);
            } catch (TypeMismatchException var7) {
                throw new IllegalArgumentException("Failed to convert value of parameter '" + paramName + "' to required type '" + requiredType.getName() + "'");
            }
        }
    }
}