package com.huawei.browsergateway.adapter.interfaces;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;

import java.util.List;

/**
 * 证书适配器接口
 * 职责：管理证书的订阅、更新和获取
 */
public interface CertificateAdapter {
    
    /**
     * 订阅证书
     * @param serviceName 服务名称
     * @param certScenes 证书场景列表
     * @param certPath 证书存储路径
     * @param callback 证书更新回调
     * @return 订阅是否成功
     */
    boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
        String certPath, CertUpdateCallback callback);
    
    /**
     * 获取CA证书内容
     * @return CA证书内容
     */
    String getCaCertificate();
    
    /**
     * 获取设备证书内容
     * @return 设备证书内容
     */
    String getDeviceCertificate();
    
    /**
     * 获取私钥内容（已转换为PKCS#8格式）
     * @return 私钥内容
     */
    String getPrivateKey();
    
    /**
     * 检查证书是否就绪
     * @return 证书是否就绪
     */
    boolean isCertificateReady();
    
    /**
     * 初始化证书SDK
     * @return 初始化是否成功
     */
    boolean initialize();
}
