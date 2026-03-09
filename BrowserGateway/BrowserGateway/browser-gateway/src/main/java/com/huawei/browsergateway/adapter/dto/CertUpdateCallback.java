package com.huawei.browsergateway.adapter.dto;

import java.util.List;

/**
 * 证书更新回调接口
 * 使用adapter层的DTO对象,实现与CSP接口的解耦
 */
public interface CertUpdateCallback {
    /**
     * 证书更新时的回调
     * @param certEntities 证书实体列表
     * @param certNotifyType 证书通知类型
     */
    void onCertificateUpdate(List<CertEntity> certEntities, CertNotifyType certNotifyType);
}
