package com.huawei.browsergateway.websocket;

import com.huawei.browsergateway.BrowserGatewayApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

@Component
public class ServerEndpointExporter implements SmartInitializingSingleton {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        this.registerEndpoints();
    }

    public static class EndpointClassPathScanner extends ClassPathBeanDefinitionScanner {
        public EndpointClassPathScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
            super(registry, useDefaultFilters);
        }

        public Set<BeanDefinitionHolder> doScan(String... basePackages) {
            this.addIncludeFilter(new AnnotationTypeFilter(ServerEndpoint.class));
            return super.doScan(basePackages);
        }
    }


    private static final Logger logger = LogManager.getLogger(ServerEndpointExporter.class);


    private final Map<InetSocketAddress, WebsocketServer> addressWebsocketServerMap = new HashMap();


    protected void registerEndpoints() {
        ApplicationContext context = this.applicationContext;
        this.scanPackage(context);
        String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
        Set<Class<?>> endpointClasses = new LinkedHashSet();

        for (String beanName : endpointBeanNames) {
            endpointClasses.add(context.getType(beanName));
        }

        for (Class<?> endpointClass : endpointClasses) {
            if (ClassUtils.isCglibProxyClass(endpointClass)) {
                this.registerEndpoint(endpointClass.getSuperclass());
            } else {
                this.registerEndpoint(endpointClass);
            }
        }

        this.init();
    }

    private void scanPackage(ApplicationContext context) {
        String[] basePackages = new String[]{"com.huawei.browsergateway"};

        EndpointClassPathScanner scanHandle = new EndpointClassPathScanner((BeanDefinitionRegistry) context.getAutowireCapableBeanFactory(), false);
        scanHandle.setResourceLoader(context);
        for (String basePackage : basePackages) {
            scanHandle.doScan(basePackage);
        }

    }

    private void init() {
        for (Map.Entry<InetSocketAddress, WebsocketServer> entry : this.addressWebsocketServerMap.entrySet()) {
            WebsocketServer websocketServer = (WebsocketServer) entry.getValue();

            try {
                websocketServer.init();
                PojoEndpointServer pojoEndpointServer = websocketServer.getPojoEndpointServer();
                StringJoiner stringJoiner = new StringJoiner(",");
                pojoEndpointServer.getPathMatcherSet().forEach((pathMatcher) -> stringJoiner.add("'" + pathMatcher.getPattern() + "'"));
                this.logger.info(String.format("\u001b[34mNetty WebSocket started on port: %s with context path(s): %s .\u001b[0m", pojoEndpointServer.getPort(), stringJoiner.toString()));
            } catch (InterruptedException e) {
                this.logger.error(String.format("websocket [%s] init fail", entry.getKey()), e);
            } catch (SSLException e) {
                this.logger.error(String.format("websocket [%s] ssl create fail", entry.getKey()), e);
            }
        }

    }

    private void registerEndpoint(Class<?> endpointClass) {
        ServerEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(endpointClass, ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalStateException("missingAnnotation ServerEndpoint");
        } else {
            ServerEndpointConfig serverEndpointConfig = this.buildConfig(annotation);
            ApplicationContext context = this.applicationContext;
            PojoMethodMapping pojoMethodMapping = null;

            try {
                pojoMethodMapping = new PojoMethodMapping(endpointClass, context, (AbstractBeanFactory) context.getAutowireCapableBeanFactory());
            } catch (DeploymentException e) {
                throw new IllegalStateException("Failed to register ServerEndpointConfig: " + String.valueOf(serverEndpointConfig), e);
            }

            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverEndpointConfig.getHost(), serverEndpointConfig.getPort());
            String path = (String) this.resolveAnnotationValue(annotation.value(), String.class, "path");
            WebsocketServer websocketServer = (WebsocketServer) this.addressWebsocketServerMap.get(inetSocketAddress);
            if (websocketServer == null) {
                PojoEndpointServer pojoEndpointServer = new PojoEndpointServer(pojoMethodMapping, serverEndpointConfig, path);
                websocketServer = new WebsocketServer(pojoEndpointServer, serverEndpointConfig);
                this.addressWebsocketServerMap.put(inetSocketAddress, websocketServer);
            } else {
                websocketServer.getPojoEndpointServer().addPathPojoMethodMapping(path, pojoMethodMapping);
            }

        }
    }

    private ServerEndpointConfig buildConfig(ServerEndpoint annotation) {
        String host = (String) this.resolveAnnotationValue(annotation.host(), String.class, "host");
        int port = (Integer) this.resolveAnnotationValue(annotation.port(), Integer.class, "port");
        String path = (String) this.resolveAnnotationValue(annotation.value(), String.class, "value");
        int bossLoopGroupThreads = (Integer) this.resolveAnnotationValue(annotation.bossLoopGroupThreads(), Integer.class, "bossLoopGroupThreads");
        int workerLoopGroupThreads = (Integer) this.resolveAnnotationValue(annotation.workerLoopGroupThreads(), Integer.class, "workerLoopGroupThreads");
        boolean useCompressionHandler = (Boolean) this.resolveAnnotationValue(annotation.useCompressionHandler(), Boolean.class, "useCompressionHandler");
        int optionConnectTimeoutMillis = (Integer) this.resolveAnnotationValue(annotation.optionConnectTimeoutMillis(), Integer.class, "optionConnectTimeoutMillis");
        int optionSoBacklog = (Integer) this.resolveAnnotationValue(annotation.optionSoBacklog(), Integer.class, "optionSoBacklog");
        int childOptionWriteSpinCount = (Integer) this.resolveAnnotationValue(annotation.childOptionWriteSpinCount(), Integer.class, "childOptionWriteSpinCount");
        int childOptionWriteBufferHighWaterMark = (Integer) this.resolveAnnotationValue(annotation.childOptionWriteBufferHighWaterMark(), Integer.class, "childOptionWriteBufferHighWaterMark");
        int childOptionWriteBufferLowWaterMark = (Integer) this.resolveAnnotationValue(annotation.childOptionWriteBufferLowWaterMark(), Integer.class, "childOptionWriteBufferLowWaterMark");
        int childOptionSoRcvbuf = (Integer) this.resolveAnnotationValue(annotation.childOptionSoRcvbuf(), Integer.class, "childOptionSoRcvbuf");
        int childOptionSoSndbuf = (Integer) this.resolveAnnotationValue(annotation.childOptionSoSndbuf(), Integer.class, "childOptionSoSndbuf");
        boolean childOptionTcpNodelay = (Boolean) this.resolveAnnotationValue(annotation.childOptionTcpNodelay(), Boolean.class, "childOptionTcpNodelay");
        boolean childOptionSoKeepalive = (Boolean) this.resolveAnnotationValue(annotation.childOptionSoKeepalive(), Boolean.class, "childOptionSoKeepalive");
        int childOptionSoLinger = (Integer) this.resolveAnnotationValue(annotation.childOptionSoLinger(), Integer.class, "childOptionSoLinger");
        boolean childOptionAllowHalfClosure = (Boolean) this.resolveAnnotationValue(annotation.childOptionAllowHalfClosure(), Boolean.class, "childOptionAllowHalfClosure");
        int readerIdleTimeSeconds = (Integer) this.resolveAnnotationValue(annotation.readerIdleTimeSeconds(), Integer.class, "readerIdleTimeSeconds");
        int writerIdleTimeSeconds = (Integer) this.resolveAnnotationValue(annotation.writerIdleTimeSeconds(), Integer.class, "writerIdleTimeSeconds");
        int allIdleTimeSeconds = (Integer) this.resolveAnnotationValue(annotation.allIdleTimeSeconds(), Integer.class, "allIdleTimeSeconds");
        int maxFramePayloadLength = (Integer) this.resolveAnnotationValue(annotation.maxFramePayloadLength(), Integer.class, "maxFramePayloadLength");
        boolean useEventExecutorGroup = (Boolean) this.resolveAnnotationValue(annotation.useEventExecutorGroup(), Boolean.class, "useEventExecutorGroup");
        int eventExecutorGroupThreads = (Integer) this.resolveAnnotationValue(annotation.eventExecutorGroupThreads(), Integer.class, "eventExecutorGroupThreads");
        String sslKeyPassword = (String) this.resolveAnnotationValue(annotation.sslKeyPassword(), String.class, "sslKeyPassword");
        String sslKeyStore = (String) this.resolveAnnotationValue(annotation.sslKeyStore(), String.class, "sslKeyStore");
        String sslKeyStorePassword = (String) this.resolveAnnotationValue(annotation.sslKeyStorePassword(), String.class, "sslKeyStorePassword");
        String sslKeyStoreType = (String) this.resolveAnnotationValue(annotation.sslKeyStoreType(), String.class, "sslKeyStoreType");
        String sslTrustStore = (String) this.resolveAnnotationValue(annotation.sslTrustStore(), String.class, "sslTrustStore");
        String sslTrustStorePassword = (String) this.resolveAnnotationValue(annotation.sslTrustStorePassword(), String.class, "sslTrustStorePassword");
        String sslTrustStoreType = (String) this.resolveAnnotationValue(annotation.sslTrustStoreType(), String.class, "sslTrustStoreType");
        String[] corsOrigins = annotation.corsOrigins();
        if (corsOrigins.length != 0) {
            for (int i = 0; i < corsOrigins.length; ++i) {
                corsOrigins[i] = (String) this.resolveAnnotationValue(corsOrigins[i], String.class, "corsOrigins");
            }
        }

        Boolean corsAllowCredentials = (Boolean) this.resolveAnnotationValue(annotation.corsAllowCredentials(), Boolean.class, "corsAllowCredentials");
        ServerEndpointConfig serverEndpointConfig = new ServerEndpointConfig(host, port, bossLoopGroupThreads, workerLoopGroupThreads, useCompressionHandler, optionConnectTimeoutMillis, optionSoBacklog, childOptionWriteSpinCount, childOptionWriteBufferHighWaterMark, childOptionWriteBufferLowWaterMark, childOptionSoRcvbuf, childOptionSoSndbuf, childOptionTcpNodelay, childOptionSoKeepalive, childOptionSoLinger, childOptionAllowHalfClosure, readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, maxFramePayloadLength, useEventExecutorGroup, eventExecutorGroupThreads, sslKeyPassword, sslKeyStore, sslKeyStorePassword, sslKeyStoreType, sslTrustStore, sslTrustStorePassword, sslTrustStoreType, corsOrigins, corsAllowCredentials);
        return serverEndpointConfig;
    }

    private <T> T resolveAnnotationValue(Object value, Class<T> requiredType, String paramName) {
        if (value == null) {
            return null;
        } else {
            AbstractBeanFactory beanFactory = (AbstractBeanFactory) this.applicationContext.getAutowireCapableBeanFactory();
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