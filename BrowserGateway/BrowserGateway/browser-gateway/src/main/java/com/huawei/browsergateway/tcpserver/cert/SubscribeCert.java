package com.huawei.browsergateway.tcpserver.cert;

import com.huawei.browsergateway.tcpserver.control.ControlTcpServer;
import com.huawei.browsergateway.tcpserver.media.MediaTcpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 证书订阅组件
 * 负责订阅证书更新，并在证书更新时触发TCP服务器重启
 */
@Component
public class SubscribeCert {
    private static final Logger log = LogManager.getLogger(SubscribeCert.class);

    @Autowired
    private ExCertInfo exCertHandler;

    @PostConstruct
    public void SubscribeCertInfo() {
        log.info("start subscribe sbg certificate scene");
        try {
            // 使用反射访问 CSP SDK 类
            Class<?> certMgrApiImplClass = Class.forName("com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl");
            Method getCertMgrApi = certMgrApiImplClass.getMethod("getCertMgrApi");
            Object certMgrApi = getCertMgrApi.invoke(null);
            Method certSDKInit = certMgrApi.getClass().getMethod("certSDKInit");
            certSDKInit.invoke(certMgrApi);
            
            Class<?> subscribeEntityClass = Class.forName("com.huawei.csp.certsdk.pojo.SubscribeEntity");
            Class<?> sceneTypeClass = Class.forName("com.huawei.csp.certsdk.enums.SceneType");
            
            ArrayList<Object> certList = new ArrayList<>();
            
            // 创建 CA 证书实体
            Object caEntity = subscribeEntityClass.newInstance();
            Method setSceneName = subscribeEntityClass.getMethod("setSceneName", String.class);
            Method setSceneDescCN = subscribeEntityClass.getMethod("setSceneDescCN", String.class);
            Method setSceneDescEN = subscribeEntityClass.getMethod("setSceneDescEN", String.class);
            Method setSceneType = subscribeEntityClass.getMethod("setSceneType", sceneTypeClass);
            Method setFeature = subscribeEntityClass.getMethod("setFeature", int.class);
            
            setSceneName.invoke(caEntity, "sbg_server_ca_certificate");
            setSceneDescCN.invoke(caEntity, "云浏览器服务端CA证书");
            setSceneDescEN.invoke(caEntity, "SBG server CA certificate");
            Object caSceneType = Enum.valueOf((Class<Enum>) sceneTypeClass, "CA");
            setSceneType.invoke(caEntity, caSceneType);
            setFeature.invoke(caEntity, 0);
            certList.add(caEntity);
            
            // 创建设备证书实体
            Object certEntity = subscribeEntityClass.newInstance();
            setSceneName.invoke(certEntity, "sbg_server_device_certificate");
            setSceneDescCN.invoke(certEntity, "云浏览器服务端设备证书");
            setSceneDescEN.invoke(certEntity, "SBG server Device Certificate");
            Object deviceSceneType = Enum.valueOf((Class<Enum>) sceneTypeClass, "DEVICE");
            setSceneType.invoke(certEntity, deviceSceneType);
            setFeature.invoke(certEntity, 0);
            certList.add(certEntity);
            
            // 订阅证书
            Class<?> exCertMgrApiImplClass = Class.forName("com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl");
            Method getExCertMgrApi = exCertMgrApiImplClass.getMethod("getExCertMgrApi");
            Object exCertMgr = getExCertMgrApi.invoke(null);
            Method subscribeExCert = exCertMgr.getClass().getMethod("subscribeExCert", 
                    String.class, List.class, 
                    Class.forName("com.huawei.csp.certsdk.handler.IExCertHandler"), 
                    String.class);
            Boolean isSubscriptionSuccessful = (Boolean) subscribeExCert.invoke(exCertMgr, 
                    "browsergw", certList, exCertHandler, "/opt/csp/browsergw");
            
            if (isSubscriptionSuccessful == null || !isSubscriptionSuccessful) {
                log.error("subscribe sbg certificate scene failed");
                return;
            }
            log.info("subscribe sbg certificate scene successful");
        } catch (Exception e) {
            log.error("Failed to subscribe certificates", e);
        }
    }
}
