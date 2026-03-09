package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.Arrays;

/**
 * 证书实体类
 * 与CSP的ExCertEntity定义完全保持一致
 * 用于封装证书相关信息
 */
@Data
public class CertEntity {
    private String sceneName;
    private String certName;
    private CertType certType;
    private String caFileName;
    private String caContent;
    private int caContentLen;
    private String deviceFileName;
    private String deviceContent;
    private int deviceContentLen;
    private String deviceSN;
    private String crlFileName;
    private String crlContent;
    private int crlContentLen;
    private String privateKeyFileName;
    private String privateKeyContent;
    private int privateKeyContentLen;
    private byte[] privateKeyPassword;

    public byte[] getPrivateKeyPassword() {
        return this.privateKeyPassword == null ? new byte[0] : Arrays.copyOf(this.privateKeyPassword, this.privateKeyPassword.length);
    }

    public void setPrivateKeyPassword(byte[] privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword == null ? null : Arrays.copyOf(privateKeyPassword, privateKeyPassword.length);
    }
}
