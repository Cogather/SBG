package com.huawei.browsergateway.adapter.dto;

/**
 * 证书类型枚举
 * 用于标识证书的类型
 * 与CSP的证书类型定义保持一致
 */
public enum CertType {
    CERT_TYPE_CA(0),
    CERT_TYPE_DEVICE(1),
    CERT_TYPE_CRL(2),
    CERT_TYPE_CA_DEVICE(3),
    CERT_TYPE_CA_CRL(4),
    CERT_TYPE_CA_DEVICE_CRL(5),
    CERT_TYPE_EMPTY(6);

    private final int type;

    CertType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static CertType fromType(int type) {
        for (CertType certType : values()) {
            if (certType.type == type) {
                return certType;
            }
        }
        throw new IllegalArgumentException("Unknown CertType: " + type);
    }
}
