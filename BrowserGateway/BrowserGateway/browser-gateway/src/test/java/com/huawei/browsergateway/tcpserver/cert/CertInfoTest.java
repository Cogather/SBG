package com.huawei.browsergateway.tcpserver.cert;

import com.huawei.browsergateway.adapter.dto.CertEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CertInfoTest {

    @BeforeEach
    void setUp() {
        CertInfo.SetCaContent("");
        CertInfo instance = CertInfo.getInstance();
        assertNotNull(instance);
    }

    @Test
    void testGetInstance() {
        CertInfo instance1 = CertInfo.getInstance();
        CertInfo instance2 = CertInfo.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testSetCaContent() {
        String testCa = "-----BEGIN CERTIFICATE-----\ntest ca content\n-----END CERTIFICATE-----";
        CertInfo.SetCaContent(testCa);

        CertInfo instance = CertInfo.getInstance();
        InputStream caStream = instance.Ca();

        assertNotNull(caStream);
        String content = readInputStream(caStream);
        assertEquals(testCa, content);
    }

    @Test
    void testSetCaContentWithNull() {
        CertInfo.SetCaContent("initial");
        CertInfo.SetCaContent(null);

        CertInfo instance = CertInfo.getInstance();
        InputStream caStream = instance.Ca();

        String content = readInputStream(caStream);
        assertEquals("initial", content);
    }

    @Test
    void testSetDeviceContent() {
        CertEntity certEntity = new CertEntity();
        certEntity.setDeviceContent("-----BEGIN CERTIFICATE-----\ndevice cert\n-----END CERTIFICATE-----");
        certEntity.setPrivateKeyContent("-----BEGIN RSA PRIVATE KEY-----\nkey content\n-----END RSA PRIVATE KEY-----");
        certEntity.setPrivateKeyPassword("".getBytes());

        CertInfo.SetDeviceContent(certEntity);

        CertInfo instance = CertInfo.getInstance();
        InputStream deviceStream = instance.Device();

        assertNotNull(deviceStream);
        String content = readInputStream(deviceStream);
        assertTrue(content.contains("device cert"));
    }

    @Test
    void testSetDeviceContentWithNull() {
        CertEntity initialCert = new CertEntity();
        initialCert.setDeviceContent("initial");
        initialCert.setPrivateKeyContent("-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----");
        initialCert.setPrivateKeyPassword("".getBytes());

        CertInfo.SetDeviceContent(initialCert);
        CertInfo.SetDeviceContent(null);

        CertInfo instance = CertInfo.getInstance();
        InputStream deviceStream = instance.Device();

        String content = readInputStream(deviceStream);
        assertEquals("initial", content);
    }

    @Test
    void testIsCertReadyWhenNotReady() {
        CertInfo.SetCaContent("");

        CertInfo instance = CertInfo.getInstance();

        assertFalse(instance.isCertReady());
    }

    @Test
    void testIsCertReadyWhenReady() {
        CertInfo.SetCaContent("ca content");

        CertEntity certEntity = new CertEntity();
        certEntity.setDeviceContent("device content");
        certEntity.setPrivateKeyContent("key content");
        certEntity.setPrivateKeyPassword("".getBytes());
        CertInfo.SetDeviceContent(certEntity);

        CertInfo instance = CertInfo.getInstance();

        assertTrue(instance.isCertReady());
    }

    @Test
    void testCaInputStream() {
        String testContent = "test ca content";
        CertInfo.SetCaContent(testContent);

        CertInfo instance = CertInfo.getInstance();
        InputStream stream = instance.Ca();

        assertNotNull(stream);
        String content = readInputStream(stream);
        assertEquals(testContent, content);
    }

    @Test
    void testDeviceInputStream() {
        CertEntity certEntity = new CertEntity();
        certEntity.setDeviceContent("test device content");
        certEntity.setPrivateKeyContent("key");
        certEntity.setPrivateKeyPassword("".getBytes());
        CertInfo.SetDeviceContent(certEntity);

        CertInfo instance = CertInfo.getInstance();
        InputStream stream = instance.Device();

        assertNotNull(stream);
        String content = readInputStream(stream);
        assertEquals("test device content", content);
    }

    private String readInputStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            fail("Failed to read input stream: " + e.getMessage());
            return null;
        }
    }
}
