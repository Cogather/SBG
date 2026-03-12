package com.huawei.browsergateway.tcpserver.cert;

import com.huawei.browsergateway.adapter.dto.CertEntity;
import com.huawei.browsergateway.adapter.dto.CertNotifyType;
import com.huawei.browsergateway.tcpserver.control.ControlTcpServer;
import com.huawei.browsergateway.tcpserver.media.MediaTcpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 证书更新处理器
 * 处理证书更新事件并重启TCP服务器以应用新证书
 * 注意：此组件仅在CSP模式下可用
 */
@Component
public class ExCertInfo {
    private static final Logger log = LogManager.getLogger(ExCertInfo.class);
    private static final String CA_CERT_SCENE = "sbg_server_ca_certificate";
    private static final String DEVICE_CERT_SCENE = "sbg_server_device_certificate";

    @Autowired
    private MediaTcpServer mediaServer;

    @Autowired
    private ControlTcpServer controlServer;

    /**
     * 处理证书更新事件
     *
     * @param certEntities 证书实体列表
     * @param certNotifyType 证书通知类型
     */
    public void exCertHandler(List<CertEntity> certEntities, CertNotifyType certNotifyType) {
        log.info("Processing certificate update, count: {}", certEntities.size());

        updateCertificates(certEntities);
        restartServers();

        log.info("Certificate update completed");
    }

    /**
     * 更新证书内容
     */
    private void updateCertificates(List<CertEntity> certEntities) {
        certEntities.forEach(cert -> {
            String sceneName = cert.getSceneName();
            switch (sceneName) {
                case CA_CERT_SCENE:
                    CertInfo.SetCaContent(cert.getCaContent());
                    log.info("Updated CA certificate");
                    break;
                case DEVICE_CERT_SCENE:
                    CertInfo.SetDeviceContent(cert);
                    log.info("Updated device certificate");
                    break;
                default:
                    log.warn("Unknown certificate scene: {}", sceneName);
                    break;
            }
        });
    }

    /**
     * 重启TCP服务器以应用新证书
     */
    private void restartServers() {
        log.info("Restarting TCP servers to apply new certificates");

        mediaServer.stopServer();
        controlServer.stopServer();

        mediaServer.startServer();
        controlServer.startServer();

        log.info("TCP servers restarted successfully");
    }
}
