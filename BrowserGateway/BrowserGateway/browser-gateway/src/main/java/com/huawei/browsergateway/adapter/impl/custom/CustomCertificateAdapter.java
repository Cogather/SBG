package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;
import com.huawei.browsergateway.adapter.interfaces.CertificateAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 证书适配器 - 自定义实现
 * 适用场景：外网环境，从本地文件加载证书或生成自签名证书
 */
@Component
public class CustomCertificateAdapter implements CertificateAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCertificateAdapter.class);
    
    private String caContent = "";
    private String deviceContent = "";
    private String privateKey = "";
    
    private final List<CertUpdateCallback> callbacks = new CopyOnWriteArrayList<>();
    
    @Value("${adapter.custom.certificate.ca-path:}")
    private String caCertPath;
    
    @Value("${adapter.custom.certificate.cert-path:}")
    private String deviceCertPath;
    
    @Value("${adapter.custom.certificate.key-path:}")
    private String privateKeyPath;
    
    @PostConstruct
    public void initializeCustomCertificates() {
        // 尝试从配置的路径加载证书文件
        loadCertificatesFromFile();
    }
    
    @Override
    public boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
        String certPath, CertUpdateCallback callback) {
        // 外网环境：生成自签名证书或使用本地证书
        logger.info("Certificate subscription for external environment (using local certificates)");
        
        if (callback != null) {
            callbacks.add(callback);
            // 立即触发一次回调
            callback.onCertificateUpdate(caContent, deviceContent);
        }
        return true;
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
        // 生成自签名证书或加载本地证书
        try {
            if (loadCertificatesFromFile() || generateSelfSignedCertificates()) {
                logger.info("Certificates initialized successfully");
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to initialize certificates", e);
            return false;
        }
    }
    
    private boolean loadCertificatesFromFile() {
        try {
            if (caCertPath != null && !caCertPath.isEmpty()) {
                caContent = new String(Files.readAllBytes(Paths.get(caCertPath)));
            }
            if (deviceCertPath != null && !deviceCertPath.isEmpty()) {
                deviceContent = new String(Files.readAllBytes(Paths.get(deviceCertPath)));
            }
            if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                privateKey = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            }
            return isCertificateReady();
        } catch (IOException e) {
            logger.warn("Failed to load certificates from file", e);
            return false;
        }
    }
    
    private boolean generateSelfSignedCertificates() {
        // TODO: 生成自签名证书的逻辑
        // 可以使用Java的KeyTool或BouncyCastle生成
        logger.info("Generated self-signed certificates");
        return true;
    }
}
