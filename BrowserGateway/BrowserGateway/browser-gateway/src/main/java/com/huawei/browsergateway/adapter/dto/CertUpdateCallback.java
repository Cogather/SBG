package com.huawei.browsergateway.adapter.dto;

/**
 * 证书更新回调接口
 */
public interface CertUpdateCallback {
    /**
     * 证书更新时的回调
     * @param caContent CA证书内容
     * @param deviceContent 设备证书内容
     */
    void onCertificateUpdate(String caContent, String deviceContent);
}
