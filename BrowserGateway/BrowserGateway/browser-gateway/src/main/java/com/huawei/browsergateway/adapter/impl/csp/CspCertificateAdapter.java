package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;
import com.huawei.browsergateway.adapter.interfaces.CertificateAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 证书适配器 - CSP SDK实现
 */
@Component("cspCertificateAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspCertificateAdapter implements CertificateAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspCertificateAdapter.class);
    
    private String caContent = "";
    private String deviceContent = "";
    private String privateKey = "";
    private final List<CertUpdateCallback> callbacks = new CopyOnWriteArrayList<>();
    
    @Override
    public boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
            String certPath, CertUpdateCallback callback) {
        try {
            // 初始化证书SDK
            Class<?> certMgrApiImplClass = Class.forName("com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl");
            Method getCertMgrApi = certMgrApiImplClass.getMethod("getCertMgrApi");
            Object certMgrApi = getCertMgrApi.invoke(null);
            Method certSDKInit = certMgrApi.getClass().getMethod("certSDKInit");
            certSDKInit.invoke(certMgrApi);
            
            // 订阅证书
            Class<?> exCertMgrApiImplClass = Class.forName("com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl");
            Method getExCertMgrApi = exCertMgrApiImplClass.getMethod("getExCertMgrApi");
            Object exCertMgrApi = getExCertMgrApi.invoke(null);
            
            // 转换CertScene到SubscribeEntity
            List<Object> subscribeEntities = convertToSubscribeEntities(certScenes);
            
            // 创建证书处理器
            Object handler = createCertHandler(callback);
            
            Method subscribeExCert = exCertMgrApi.getClass().getMethod("subscribeExCert", 
                    String.class, List.class, 
                    Class.forName("com.huawei.csp.certsdk.handler.IExCertHandler"), 
                    String.class);
            Boolean result = (Boolean) subscribeExCert.invoke(exCertMgrApi, serviceName, subscribeEntities, handler, certPath);
            
            if (result && callback != null) {
                callbacks.add(callback);
            }
            
            return result != null && result;
        } catch (Exception e) {
            logger.error("Failed to subscribe certificates via CSP SDK", e);
            return false;
        }
    }
    
    @Override
    public String getCaCertificate() {
        return caContent;
    }
    
    @Override
    public String getDeviceCertificate() {
        return deviceContent;
    }
    
    @Override
    public String getPrivateKey() {
        return privateKey;
    }
    
    @Override
    public boolean isCertificateReady() {
        return caContent != null && !caContent.isEmpty() && 
               deviceContent != null && !deviceContent.isEmpty();
    }
    
    @Override
    public boolean initialize() {
        try {
            Class<?> certMgrApiImplClass = Class.forName("com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl");
            Method getCertMgrApi = certMgrApiImplClass.getMethod("getCertMgrApi");
            Object certMgrApi = getCertMgrApi.invoke(null);
            Method certSDKInit = certMgrApi.getClass().getMethod("certSDKInit");
            certSDKInit.invoke(certMgrApi);
            logger.info("Certificate SDK initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize certificate SDK", e);
            return false;
        }
    }
    
    private List<Object> convertToSubscribeEntities(List<CertScene> certScenes) throws Exception {
        List<Object> entities = new ArrayList<>();
        Class<?> subscribeEntityClass = Class.forName("com.huawei.csp.certsdk.pojo.SubscribeEntity");
        Class<?> sceneTypeClass = Class.forName("com.huawei.csp.certsdk.enums.SceneType");
        
        for (CertScene scene : certScenes) {
            Object entity = subscribeEntityClass.getDeclaredConstructor().newInstance();
            Method setSceneName = subscribeEntityClass.getMethod("setSceneName", String.class);
            Method setSceneDescCN = subscribeEntityClass.getMethod("setSceneDescCN", String.class);
            Method setSceneDescEN = subscribeEntityClass.getMethod("setSceneDescEN", String.class);
            Method setSceneType = subscribeEntityClass.getMethod("setSceneType", sceneTypeClass);
            Method setFeature = subscribeEntityClass.getMethod("setFeature", int.class);
            
            setSceneName.invoke(entity, scene.getSceneName());
            setSceneDescCN.invoke(entity, scene.getSceneDescCN());
            setSceneDescEN.invoke(entity, scene.getSceneDescEN());
            
            // 转换SceneType
            Object sceneType = Enum.valueOf((Class<Enum>) sceneTypeClass, scene.getSceneType().name());
            setSceneType.invoke(entity, sceneType);
            setFeature.invoke(entity, scene.getFeature());
            
            entities.add(entity);
        }
        return entities;
    }
    
    private Object createCertHandler(CertUpdateCallback callback) {
        // 创建匿名内部类实现IExCertHandler
        try {
            Class<?> handlerInterface = Class.forName("com.huawei.csp.certsdk.handler.IExCertHandler");
            // 使用动态代理创建处理器
            return java.lang.reflect.Proxy.newProxyInstance(
                    handlerInterface.getClassLoader(),
                    new Class[]{handlerInterface},
                    (proxy, method, args) -> {
                        if ("handle".equals(method.getName()) && args.length == 1) {
                            Object certInfo = args[0];
                            // 提取证书信息
                            Method getCaContent = certInfo.getClass().getMethod("getCaContent");
                            Method getExCertEntity = certInfo.getClass().getMethod("getExCertEntity");
                            
                            caContent = (String) getCaContent.invoke(certInfo);
                            Object exCertEntity = getExCertEntity.invoke(certInfo);
                            
                            if (exCertEntity != null) {
                                Method getDeviceContent = exCertEntity.getClass().getMethod("getDeviceContent");
                                Method getPrivateKeyContent = exCertEntity.getClass().getMethod("getPrivateKeyContent");
                                
                                deviceContent = (String) getDeviceContent.invoke(exCertEntity);
                                privateKey = (String) getPrivateKeyContent.invoke(exCertEntity);
                            }
                            
                            // 触发回调
                            if (callback != null) {
                                callback.onCertificateUpdate(caContent, deviceContent, privateKey);
                            }
                            return null;
                        }
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Failed to create certificate handler", e);
            return null;
        }
    }
}
