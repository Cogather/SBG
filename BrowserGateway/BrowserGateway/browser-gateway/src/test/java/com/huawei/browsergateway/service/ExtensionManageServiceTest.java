package com.huawei.browsergateway.service;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.ChromeConfig;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtensionManageServiceTest {

    @Mock
    private IFileStorage fileStorageService;

    @Mock
    private IChromeSet chromeSet;

    @Mock
    private Config config;

    @Mock
    private ChromeConfig chromeConfig;

    @Mock
    private IPluginManage pluginManage;

    @Mock
    private IAlarm alarm;

    @TempDir
    Path tempDir;

    private ExtensionManageService extensionManageService;
    private HttpServer browserServer;

    @BeforeEach
    void setUp() {
        extensionManageService = new ExtensionManageService();
        ReflectionTestUtils.setField(extensionManageService, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(extensionManageService, "chromeSet", chromeSet);
        ReflectionTestUtils.setField(extensionManageService, "config", config);
        ReflectionTestUtils.setField(extensionManageService, "pluginManage", pluginManage);
        ReflectionTestUtils.setField(extensionManageService, "alarm", alarm);

        when(config.getTmpPath()).thenReturn(tempDir.resolve("tmp").toString());
        when(config.getChrome()).thenReturn(chromeConfig);
    }

    @AfterEach
    void tearDown() {
        if (browserServer != null) {
            browserServer.stop(0);
        }
    }

    @Test
    void loadExtensionShouldReturnTrueAndReportUsedWhenPluginLoadsSuccessfully() throws Exception {
        Path extensionZip = createExtensionArchive("demo-plugin");
        browserServer = startBrowserStub(List.of("browser-1", "browser-2"));
        when(chromeConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + browserServer.getAddress().getPort());
        doAnswer(invocation -> {
            String localPath = invocation.getArgument(0, String.class);
            Files.copy(extensionZip, Paths.get(localPath), StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(fileStorageService).downloadFile(anyString(), anyString());

        LoadExtensionRequest request = buildRequest();

        boolean result = extensionManageService.loadExtension(request);

        assertTrue(result);
        verify(pluginManage).updatePluginActive("demo-plugin", "1.0.0", "ChromeExtend");
        verify(pluginManage).loadPlugin(
                argThat(path -> path != null && path.endsWith("keys")),
                argThat(path -> path != null && path.endsWith("touch")),
                isNull()
        );
        verify(chromeSet).deleteAll();
        verify(chromeSet).reportUsed();
    }

    @Test
    void loadExtensionShouldReturnFalseAndSetFailedStatusWhenLoadThrowsException() throws Exception {
        Path extensionZip = createExtensionArchive("broken-plugin");
        doAnswer(invocation -> {
            String localPath = invocation.getArgument(0, String.class);
            Files.copy(extensionZip, Paths.get(localPath), StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(fileStorageService).downloadFile(anyString(), anyString());
        doThrow(new RuntimeException("load failed"))
                .when(pluginManage)
                .loadPlugin(anyString(), anyString(), isNull());

        LoadExtensionRequest request = buildRequest();
        request.setExtensionFilePath("broken-plugin.zip");

        boolean result = extensionManageService.loadExtension(request);

        assertFalse(result);
        verify(pluginManage).updatePluginActive("demo-plugin", "1.0.0", "ChromeExtend");
        verify(pluginManage).updateStatus(Constant.FAILED);
        verify(chromeSet, never()).deleteAll();
        verify(chromeSet).reportUsed();
    }

    @Test
    void getPluginInfoShouldDelegateToPluginManage() {
        PluginActive pluginActive = new PluginActive();
        pluginActive.setName("demo-plugin");
        when(pluginManage.getPluginActive()).thenReturn(pluginActive);

        PluginActive result = extensionManageService.getPluginInfo();

        assertSame(pluginActive, result);
        verify(pluginManage).getPluginActive();
    }

    private LoadExtensionRequest buildRequest() {
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setBucketName("test-bucket");
        request.setExtensionFilePath("demo-plugin.zip");
        request.setName("demo-plugin");
        request.setVersion("1.0.0");
        request.setType("ChromeExtend");
        return request;
    }

    private Path createExtensionArchive(String pluginFilePrefix) throws IOException {
        Path fixtureDir = tempDir.resolve("fixtures");
        Files.createDirectories(fixtureDir);

        Path compressedPlugin = fixtureDir.resolve(pluginFilePrefix + ".tar.gz");
        writeTarGzWithDirectories(compressedPlugin, List.of("jar/", "keys/", "touch/"));

        Path zipFile = fixtureDir.resolve(pluginFilePrefix + ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("package.json"));
            zipOutputStream.write("{\"name\":\"demo-plugin\"}".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry(pluginFilePrefix + ".tar.gz"));
            zipOutputStream.write(Files.readAllBytes(compressedPlugin));
            zipOutputStream.closeEntry();
        }
        return zipFile;
    }

    private HttpServer startBrowserStub(List<String> browserIds) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/browsers", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = browserIds.stream()
                        .map(id -> "{\"id\":\"" + id + "\"}")
                        .reduce((left, right) -> left + "," + right)
                        .map(body -> "[" + body + "]")
                        .orElse("[]");
                writeJsonResponse(exchange, 200, response);
                return;
            }
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        });
        server.createContext("/api/browsers/", exchange -> {
            if ("DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private void writeJsonResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    private void writeTarGzWithDirectories(Path tarGzFile, List<String> directoryNames) throws IOException {
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(tarGzFile))) {
            for (String directoryName : directoryNames) {
                writeTarDirectoryEntry(gzipOutputStream, directoryName);
            }
            gzipOutputStream.write(new byte[1024]);
        }
    }

    private void writeTarDirectoryEntry(OutputStream outputStream, String directoryName) throws IOException {
        byte[] header = new byte[512];
        writeString(header, 0, 100, directoryName);
        writeOctal(header, 100, 8, 0755);
        writeOctal(header, 108, 8, 0);
        writeOctal(header, 116, 8, 0);
        writeOctal(header, 124, 12, 0);
        writeOctal(header, 136, 12, Instant.now().getEpochSecond());
        Arrays.fill(header, 148, 156, (byte) ' ');
        header[156] = '5';
        writeString(header, 257, 6, "ustar");
        writeString(header, 263, 2, "00");
        writeChecksum(header);
        outputStream.write(header);
    }

    private void writeChecksum(byte[] header) {
        long checksum = 0;
        for (byte value : header) {
            checksum += value & 0xFF;
        }
        String octal = String.format("%06o", checksum);
        byte[] bytes = octal.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, 148, bytes.length);
        header[154] = 0;
        header[155] = (byte) ' ';
    }

    private void writeOctal(byte[] header, int offset, int length, long value) {
        String octal = String.format("%0" + (length - 1) + "o", value);
        byte[] bytes = octal.getBytes(StandardCharsets.US_ASCII);
        int start = offset + length - 1 - bytes.length;
        System.arraycopy(bytes, 0, header, start, bytes.length);
        header[offset + length - 1] = 0;
    }

    private void writeString(byte[] header, int offset, int length, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, header, offset, Math.min(bytes.length, length));
    }
}
