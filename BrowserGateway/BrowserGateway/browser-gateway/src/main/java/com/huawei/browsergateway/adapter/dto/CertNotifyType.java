package com.huawei.browsergateway.adapter.dto;

/**
 * 证书通知类型枚举
 * 用于标识证书变更的通知类型
 */
public enum CertNotifyType {
    CERT_NOTIFY_TYPE_UPDATE(0),
    CERT_NOTIFY_TYPE_CONSISTENCY(1),
    CERT_NOTIFY_TYPE_QUERY(2),
    CERT_NOTIFY_TYPE_UNBIND(3);

    private final int state;

    CertNotifyType(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    public static CertNotifyType fromState(int state) {
        for (CertNotifyType type : values()) {
            if (type.state == state) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CertNotifyType state: " + state);
    }
}
