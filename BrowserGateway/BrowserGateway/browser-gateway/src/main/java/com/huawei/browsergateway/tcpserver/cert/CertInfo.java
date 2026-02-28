package com.huawei.browsergateway.tcpserver.cert;
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

public class CertInfo {
    private static CertInfo instance;

    private static String caContent = "" ;
    private static String deviceContent = "" ;
    private static String keyContent = "";
    private static String keypwd = "" ;


    public static synchronized void SetCaContent(String c) {
        if (c == null) {
            return;
        }
        caContent = c;
    }
    public static synchronized void SetDeviceContent(String deviceContent, String keyContent, String keypwd) {
        if (deviceContent == null || keyContent == null) {
            return;
        }
        CertInfo.deviceContent = deviceContent;
        CertInfo.keyContent = keyContent;
        CertInfo.keypwd = keypwd != null ? keypwd : "";
    }
    
    // 保留原方法签名以兼容CSP模式（通过反射调用）
    public static synchronized void SetDeviceContent(Object cert) {
        // 在custom模式下，此方法不会被调用
        // 如果需要支持，可以通过反射获取字段值
    }

    public InputStream Ca() {
        return new ByteArrayInputStream(caContent.getBytes());
    }

    public InputStream Device() {
        return new ByteArrayInputStream(deviceContent.getBytes());
    }

    public InputStream Key() throws IOException {
        PrivateKey privateKey = loadEncryptedPrivateKey(keyContent, keypwd.toCharArray());
        return convertPkcs1ToPkcs8Stream(privateKey);
    }

    public static synchronized CertInfo getInstance() {
        if (instance == null) {
            synchronized (CertInfo.class) {
                if (instance == null) {
                    instance = new CertInfo();
                }
            }
        }
        return instance;
    }

    public boolean isCertReady() {
        return !caContent.isEmpty() && !keyContent.isEmpty();
    }

    private static PrivateKey loadEncryptedPrivateKey(String pemFile, char[] paasword) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        try (
                Reader reader = new StringReader(pemFile);
                PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            PEMKeyPair keyPair;
            if (obj instanceof PEMEncryptedKeyPair) {
                PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) obj;
                if (paasword == null || paasword.length == 0) {
                    throw new IllegalArgumentException("need paasword to encrypt the privateKey");
                }
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(paasword);
                keyPair = encryptedKeyPair.decryptKeyPair(decProv);
            } else if (obj instanceof PEMKeyPair) {
                keyPair = (PEMKeyPair) obj;
            } else {
                throw new IllegalArgumentException("Unsupported PEM object: " + obj);
            }
            return converter.getKeyPair(keyPair).getPrivate();
        }
    }

    private static InputStream convertPkcs1ToPkcs8Stream(PrivateKey privateKey) throws IOException {
        PrivateKeyInfo pkInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        byte[] der = pkInfo.getEncoded();
        PemObject pemObject = new PemObject("PRIVATE KEY", der); // PKCS#8 标签
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(pemObject);
        }
        return new ByteArrayInputStream(sw.toString().getBytes());
    }
}