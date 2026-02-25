package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;
import com.huawei.browsergateway.adapter.interfaces.CertificateAdapter;
import com.huawei.csp.certsdk.certapiImpl.CertMgrApi;
import com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl;
import com.huawei.csp.certsdk.certapiImpl.ExCertMgrApi;
import com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl;
import com.huawei.csp.certsdk.enums.SceneType;
import com.huawei.csp.certsdk.handler.IExCertHandler;
import com.huawei.csp.certsdk.pojo.ExCertEntity;
import com.huawei.csp.certsdk.pojo.ExCertInfo;
import com.huawei.csp.certsdk.pojo.SubscribeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 证书适配器 - CSP SDK实现
 * 适用场景：内网环境，从CSP证书中心订阅
 */
@Component
public class CspCertificateAdapter implements CertificateAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspCertificateAdapter.class);
    
    private volatile String caContent = "";
    private volatile String deviceContent = "";
    private volatile String privateKey = "";
    
    private final List<CertUpdateCallback> callbacks = new CopyOnWriteArrayList<>();
    
    @Override
    public boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
        String certPath, CertUpdateCallback callback) {
        try {
            // 初始化证书SDK
            CertMgrApi certMgrApi = CertMgrApiImpl.getCertMgrApi();
            certMgrApi.certSDKInit();
            
            // 构建订阅实体列表
            ArrayList<SubscribeEntity> certList = new ArrayList<>();
            for (CertScene scene : certScenes) {
                SubscribeEntity entity = new SubscribeEntity();
                entity.setSceneName(scene.getSceneName());
                entity.setSceneDescCN(scene.getSceneDescCN());
                entity.setSceneDescEN(scene.getSceneDescEN());
                
                // 转换场景类型
                if (scene.getSceneType() == CertScene.SceneType.CA) {
                    entity.setSceneType(SceneType.CA);
                } else if (scene.getSceneType() == CertScene.SceneType.DEVICE) {
                    entity.setSceneType(SceneType.DEVICE);
                }
                
                entity.setFeature(scene.getFeature());
                certList.add(entity);
            }
            
            // 注册回调
            if (callback != null) {
                callbacks.add(callback);
            }
            
            // 实现证书变更处理器
            IExCertHandler handler = new IExCertHandler() {
                @Override
                public void handle(ExCertInfo certInfo) {
                    logger.info("Certificate updated, sceneName: {}, key: {}", 
                        certInfo.getKey(), certInfo.getKey());
                    
                    // 更新证书内容
                    if (certInfo.getCaContent() != null && !certInfo.getCaContent().isEmpty()) {
                        caContent = certInfo.getCaContent();
                        logger.info("CA certificate content updated");
                    }
                    
                    if (certInfo.getExCertEntity() != null) {
                        ExCertEntity exCertEntity = certInfo.getExCertEntity();
                        if (exCertEntity.getDeviceContent() != null) {
                            deviceContent = exCertEntity.getDeviceContent();
                        }
                        if (exCertEntity.getPrivateKeyContent() != null) {
                            privateKey = exCertEntity.getPrivateKeyContent();
                        }
                        logger.info("Device certificate content updated");
                    }
                    
                    // 触发所有注册的回调
                    for (CertUpdateCallback cb : callbacks) {
                        try {
                            cb.onCertificateUpdate(caContent, deviceContent);
                        } catch (Exception e) {
                            logger.error("Error in certificate update callback", e);
                        }
                    }
                }
            };
            
            // 订阅证书
            ExCertMgrApi exCertMgrApi = ExCertMgrApiImpl.getExCertMgrApi();
            boolean success = exCertMgrApi.subscribeExCert(serviceName, certList, handler, certPath);
            
            if (success) {
                logger.info("Certificate subscription successful for service: {}", serviceName);
            } else {
                logger.error("Certificate subscription failed for service: {}", serviceName);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Failed to subscribe certificates", e);
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
            // 初始化CSP证书SDK
            CertMgrApi certMgrApi = CertMgrApiImpl.getCertMgrApi();
            certMgrApi.certSDKInit();
            logger.info("Certificate SDK initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize certificate SDK", e);
            return false;
        }
    }
}
