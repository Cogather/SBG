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
 * CSP证书处理器 - 仅在CSP模式下可用
 * Custom模式下此功能由CustomCertificateAdapter处理
 */
@Component
public class ExCertInfo {
    private static final Logger log = LogManager.getLogger(ExCertInfo.class);

    @Autowired
    private MediaTcpServer mediaServer;

    @Autowired
    private ControlTcpServer controlServer;

    // 注意：此方法在custom模式下不会被调用，因为类本身被条件注解排除
    // 如果需要支持custom模式，需要创建Mock实现或使用反射
    public void exCertHandler(List<CertEntity> certEntities, CertNotifyType certNotifyType) {
        log.info("start processing the certificate list");
        certEntities.forEach(cert -> {
            String scene = cert.getSceneName();
            switch (scene) {
                case "sbg_server_ca_certificate":
                    CertInfo.SetCaContent(cert.getCaContent());
                    break;
                case "sbg_server_device_certificate":
                    CertInfo.SetDeviceContent(cert);
                    break;
                default:
                    break;
            }
        });
        log.info("start restart the server");
        mediaServer.stopServer();
        controlServer.stopServer();

        mediaServer.startServer();
        controlServer.startServer();
        log.info("server restarted end");
    }
}
