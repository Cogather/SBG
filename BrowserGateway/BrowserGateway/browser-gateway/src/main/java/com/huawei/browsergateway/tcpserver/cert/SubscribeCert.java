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
 * 负责订阅证书更新通知，并在证书更新时触发服务器重启
 */
@Component
public class SubscribeCert {
    private static final Logger log = LogManager.getLogger(SubscribeCert.class);

    private static final String APP_NAME = "browsergw";
    private static final String CERT_BASE_PATH = "/opt/csp/browsergw";

    private static final String CA_CERT_SCENE_NAME = "sbg_server_ca_certificate";
    private static final String CA_CERT_DESC_CN = "云浏览器服务端CA证书";
    private static final String CA_CERT_DESC_EN = "SBG server CA certificate";

    private static final String DEVICE_CERT_SCENE_NAME = "sbg_server_device_certificate";
    private static final String DEVICE_CERT_DESC_CN = "云浏览器服务端设备证书";
    private static final String DEVICE_CERT_DESC_EN = "SBG server Device Certificate";

    @Autowired
    private ExCertInfo certUpdateHandler;

    @Autowired
    private CertificateAdapter certificateAdapter;

    /**
     * 初始化证书订阅
     */
    @PostConstruct
    public void SubscribeCertInfo() {
        log.info("Initializing certificate subscription");

        if (!initializeCertificateAdapter()) {
            return;
        }

        List<CertScene> certScenes = buildCertScenes();
        CertUpdateCallback callback = createCertUpdateCallback();

        subscribeCertificates(certScenes, callback);
    }

    /**
     * 初始化证书适配器
     */
    private boolean initializeCertificateAdapter() {
        if (!certificateAdapter.initialize()) {
            log.error("Failed to initialize certificate SDK");
            return false;
        }
        return true;
    }

    /**
     * 构建证书场景列表
     */
    private List<CertScene> buildCertScenes() {
        List<CertScene> certScenes = new ArrayList<>();
        certScenes.add(createCaCertScene());
        certScenes.add(createDeviceCertScene());
        return certScenes;
    }

    /**
     * 创建CA证书场景
     */
    private CertScene createCaCertScene() {
        CertScene caScene = new CertScene();
        caScene.setSceneName(CA_CERT_SCENE_NAME);
        caScene.setSceneDescCN(CA_CERT_DESC_CN);
        caScene.setSceneDescEN(CA_CERT_DESC_EN);
        caScene.setSceneType(CertScene.SceneType.CA);
        caScene.setFeature(0);
        return caScene;
    }

    /**
     * 创建设备证书场景
     */
    private CertScene createDeviceCertScene() {
        CertScene deviceScene = new CertScene();
        deviceScene.setSceneName(DEVICE_CERT_SCENE_NAME);
        deviceScene.setSceneDescCN(DEVICE_CERT_DESC_CN);
        deviceScene.setSceneDescEN(DEVICE_CERT_DESC_EN);
        deviceScene.setSceneType(CertScene.SceneType.DEVICE);
        deviceScene.setFeature(0);
        return deviceScene;
    }

    /**
     * 创建证书更新回调
     */
    private CertUpdateCallback createCertUpdateCallback() {
        return new CertUpdateCallback() {
            @Override
            public void onCertificateUpdate(List<CertEntity> certEntities, CertNotifyType certNotifyType) {
                certUpdateHandler.exCertHandler(certEntities, certNotifyType);
            }
        };
    }

    /**
     * 订阅证书更新
     */
    private void subscribeCertificates(List<CertScene> certScenes, CertUpdateCallback callback) {
        boolean success = certificateAdapter.subscribeCertificates(
                APP_NAME,
                certScenes,
                CERT_BASE_PATH,
                callback
        );

        if (!success) {
            log.error("Failed to subscribe certificate scenes");
            return;
        }

        log.info("Successfully subscribed to certificate scenes");
    }
}
