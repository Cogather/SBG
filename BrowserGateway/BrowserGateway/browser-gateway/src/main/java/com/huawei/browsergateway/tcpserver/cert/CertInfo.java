package com.huawei.browsergateway.tcpserver.cert;

import com.huawei.browsergateway.adapter.dto.CertEntity;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;

/**
 * 证书信息管理类
 * 单例模式管理CA证书、设备证书和私钥
 */
public class CertInfo {
    private static volatile CertInfo instance;

    private static String caContent = "";
    private static String deviceContent = "";
    private static String keyContent = "";
    private static String keyPassword = "";

    /**
     * 设置CA证书内容
     *
     * @param content CA证书内容
     */
    public static synchronized void SetCaContent(String content) {
        if (content != null) {
            caContent = content;
        }
    }

    /**
     * 设置设备证书内容
     *
     * @param certEntity 证书实体对象
     */
    public static synchronized void SetDeviceContent(CertEntity certEntity) {
        if (certEntity == null) {
            return;
        }
        deviceContent = certEntity.getDeviceContent();
        keyContent = certEntity.getPrivateKeyContent();
        keyPassword = new String(certEntity.getPrivateKeyPassword());
    }

    /**
     * 获取CertInfo单例实例
     *
     * @return CertInfo实例
     */
    public static CertInfo getInstance() {
        if (instance == null) {
            synchronized (CertInfo.class) {
                if (instance == null) {
                    instance = new CertInfo();
                }
            }
        }
        return instance;
    }

    /**
     * 获取CA证书输入流
     *
     * @return CA证书输入流
     */
    public InputStream Ca() {
        return new ByteArrayInputStream(caContent.getBytes());
    }

    /**
     * 获取设备证书输入流
     *
     * @return 设备证书输入流
     */
    public InputStream Device() {
        return new ByteArrayInputStream(deviceContent.getBytes());
    }

    /**
     * 获取私钥输入流（PKCS#8格式）
     *
     * @return 私钥输入流
     * @throws IOException 转换失败时抛出
     */
    public InputStream Key() throws IOException {
        PrivateKey privateKey = loadEncryptedPrivateKey(keyContent, keyPassword.toCharArray());
        return convertToPkcs8Stream(privateKey);
    }

    /**
     * 检查证书是否就绪
     *
     * @return 证书就绪返回true，否则返回false
     */
    public boolean isCertReady() {
        return !caContent.isEmpty() && !keyContent.isEmpty();
    }

    /**
     * 加载加密的私钥
     *
     * @param pemContent PEM格式的私钥内容
     * @param password 私钥密码
     * @return 私钥对象
     * @throws IOException 加载失败时抛出
     */
    private static PrivateKey loadEncryptedPrivateKey(String pemContent, char[] password) throws IOException {
        Security.addProvider(new BouncyCastleProvider());

        try (Reader reader = new StringReader(pemContent);
             PEMParser parser = new PEMParser(reader)) {

            Object pemObject = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            PEMKeyPair keyPair = extractKeyPair(pemObject, password);
            return converter.getKeyPair(keyPair).getPrivate();
        }
    }

    /**
     * 从PEM对象中提取密钥对
     */
    private static PEMKeyPair extractKeyPair(Object pemObject, char[] password) throws IOException {
        if (pemObject instanceof PEMEncryptedKeyPair) {
            PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) pemObject;
            if (password == null || password.length == 0) {
                throw new IllegalArgumentException("Password required for encrypted private key");
            }
            PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().build(password);
            return encryptedKeyPair.decryptKeyPair(decryptorProvider);
        } else if (pemObject instanceof PEMKeyPair) {
            return (PEMKeyPair) pemObject;
        } else {
            throw new IllegalArgumentException("Unsupported PEM object type: " + pemObject);
        }
    }

    /**
     * 将私钥转换为PKCS#8格式的输入流
     *
     * @param privateKey 私钥对象
     * @return PKCS#8格式的输入流
     * @throws IOException 转换失败时抛出
     */
    private static InputStream convertToPkcs8Stream(PrivateKey privateKey) throws IOException {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        byte[] encodedKey = privateKeyInfo.getEncoded();
        PemObject pemObject = new PemObject("PRIVATE KEY", encodedKey);

        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(pemObject);
        }

        return new ByteArrayInputStream(stringWriter.toString().getBytes());
    }
}
