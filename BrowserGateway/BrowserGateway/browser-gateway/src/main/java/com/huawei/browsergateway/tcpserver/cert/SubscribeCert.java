package com.huawei.browsergateway.tcpserver.cert;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;
import com.huawei.browsergateway.adapter.interfaces.CertificateAdapter;
import com.huawei.browsergateway.tcpserver.control.ControlTcpServer;
import com.huawei.browsergateway.tcpserver.media.MediaTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 证书订阅组件
 * 负责订阅证书更新，并在证书更新时触发TCP服务器重启
 */
@Component
public class SubscribeCert {
    
    private static final Logger log = LoggerFactory.getLogger(SubscribeCert.class);
    
    @Autowired
    private CertificateAdapter certificateAdapter;
    
    @Autowired
    private ControlTcpServer controlTcpServer;
    
    @Autowired
    private MediaTcpServer mediaTcpServer;
    
    @Value("${browsergw.cert.service-name:browsergw}")
    private String serviceName;
    
    @Value("${browsergw.cert.cert-path:/opt/csp/browsergw}")
    private String certPath;
    
    @Value("${browsergw.tcp.use-tls:true}")
    private boolean useTls;
    
    /**
     * 初始化证书订阅
     */
    @PostConstruct
    public void subscribeCertInfo() {
        log.info("开始初始化证书订阅，服务名称: {}, 证书路径: {}", serviceName, certPath);
        
        try {
            // 初始化证书适配器
            if (!certificateAdapter.initialize()) {
                log.error("证书适配器初始化失败");
                return;
            }
            
            // 构建证书场景列表
            List<CertScene> certScenes = Arrays.asList(
                createCertScene("sbg_server_ca_certificate", CertScene.SceneType.CA, 
                    "SBG服务器CA证书", "SBG Server CA Certificate"),
                createCertScene("sbg_server_device_certificate", CertScene.SceneType.DEVICE,
                    "SBG服务器设备证书", "SBG Server Device Certificate")
            );
            
            // 订阅证书更新
            boolean success = certificateAdapter.subscribeCertificates(
                serviceName, 
                certScenes, 
                certPath,
                createCertUpdateCallback()
            );
            
            if (success) {
                log.info("证书订阅成功");
                
                // 初始化CertInfo（从适配器获取当前证书）
                initializeCertInfo();
            } else {
                log.error("证书订阅失败");
            }
            
        } catch (Exception e) {
            log.error("证书订阅初始化异常", e);
        }
    }
    
    /**
     * 创建证书场景对象
     */
    private CertScene createCertScene(String sceneName, CertScene.SceneType sceneType,
                                      String descCN, String descEN) {
        CertScene scene = new CertScene();
        scene.setSceneName(sceneName);
        scene.setSceneType(sceneType);
        scene.setSceneDescCN(descCN);
        scene.setSceneDescEN(descEN);
        scene.setFeature(0);
        return scene;
    }
    
    /**
     * 创建证书更新回调
     */
    private CertUpdateCallback createCertUpdateCallback() {
        return new CertUpdateCallback() {
            @Override
            public void onCertificateUpdate(String caContent, String deviceContent) {
                log.info("收到证书更新通知");
                
                try {
                    // 更新CertInfo单例
                    CertInfo certInfo = CertInfo.getInstance();
                    String privateKey = certificateAdapter.getPrivateKey();
                    certInfo.updateAll(caContent, deviceContent, privateKey);
                    
                    log.info("证书信息已更新到CertInfo，CA证书长度: {}, 设备证书长度: {}, 私钥长度: {}",
                        caContent != null ? caContent.length() : 0,
                        deviceContent != null ? deviceContent.length() : 0,
                        privateKey != null ? privateKey.length() : 0);
                    
                    // 重启TCP服务器以应用新证书
                    restartTcpServers();
                    
                } catch (Exception e) {
                    log.error("处理证书更新回调时发生异常", e);
                }
            }
        };
    }
    
    /**
     * 初始化CertInfo（从适配器获取当前证书）
     */
    private void initializeCertInfo() {
        try {
            CertInfo certInfo = CertInfo.getInstance();
            String caContent = certificateAdapter.getCaCertificate();
            String deviceContent = certificateAdapter.getDeviceCertificate();
            String privateKey = certificateAdapter.getPrivateKey();
            
            certInfo.updateAll(caContent, deviceContent, privateKey);
            
            log.info("CertInfo初始化完成，证书就绪状态: {}", certInfo.isReady());
        } catch (Exception e) {
            log.error("初始化CertInfo失败", e);
        }
    }
    
    /**
     * 重启TCP服务器以应用新证书
     */
    private void restartTcpServers() {
        log.info("开始重启TCP服务器以应用新证书，使用TLS: {}", useTls);
        
        try {
            // 停止服务器
            log.info("停止控制流TCP服务器");
            controlTcpServer.stopServer();
            
            log.info("停止媒体流TCP服务器");
            mediaTcpServer.stopServer();
            
            // 等待一段时间确保资源释放
            Thread.sleep(1000);
            
            // 重新启动服务器
            log.info("重启控制流TCP服务器");
            controlTcpServer.startServer(useTls);
            
            log.info("重启媒体流TCP服务器");
            mediaTcpServer.startServer(useTls);
            
            log.info("TCP服务器重启完成");
            
        } catch (InterruptedException e) {
            log.error("重启TCP服务器时被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("重启TCP服务器失败", e);
            // 即使重启失败，也尝试启动服务器（可能使用旧证书）
            try {
                controlTcpServer.startServer(useTls);
                mediaTcpServer.startServer(useTls);
            } catch (Exception ex) {
                log.error("尝试启动TCP服务器失败", ex);
            }
        }
    }
}
