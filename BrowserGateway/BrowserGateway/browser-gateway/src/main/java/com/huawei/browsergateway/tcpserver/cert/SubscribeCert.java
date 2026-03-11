package com.huawei.browsergateway.tcpserver.cert;


import com.huawei.browsergateway.adapter.CertificateAdapter;
import com.huawei.browsergateway.adapter.dto.CertEntity;
import com.huawei.browsergateway.adapter.dto.CertNotifyType;
import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
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

    @Autowired
    private CertificateAdapter certificateAdapter;

    @PostConstruct
    public void SubscribeCertInfo() {
        log.info("start subscribe sbg certificate scene");

        // 初始化证书SDK
        if (!certificateAdapter.initialize()) {
            log.error("Failed to initialize certificate SDK");
            return;
        }

        // 构建证书场景列表
        List<CertScene> certScenes = new ArrayList<>();
        CertScene caScene = new CertScene();
        caScene.setSceneName("sbg_server_ca_certificate");
        caScene.setSceneDescCN("云浏览器服务端CA证书");
        caScene.setSceneDescEN("SBG server CA certificate");
        caScene.setSceneType(CertScene.SceneType.CA);
        caScene.setFeature(0);
        certScenes.add(caScene);

        CertScene deviceScene = new CertScene();
        deviceScene.setSceneName("sbg_server_device_certificate");
        deviceScene.setSceneDescCN("云浏览器服务端设备证书");
        deviceScene.setSceneDescEN("SBG server Device Certificate");
        deviceScene.setSceneType(CertScene.SceneType.DEVICE);
        deviceScene.setFeature(0);
        certScenes.add(deviceScene);

        // 创建证书更新回调
        CertUpdateCallback callback = new CertUpdateCallback() {
            @Override
            public void onCertificateUpdate(List<CertEntity> cerEntities, CertNotifyType certNotifyType) {
                exCertHandler.exCertHandler(cerEntities, certNotifyType);
            }
        };

        // 订阅证书
        boolean isSubscriptionSuccessful = certificateAdapter.subscribeCertificates(
                "browsergw", certScenes, "/opt/csp/browsergw", callback
        );

        if (!isSubscriptionSuccessful) {
            log.error("subscribe sbg certificate scene failed");
            return;
        }
        log.info("subscribe sbg certificate scene successful");
    }
}
