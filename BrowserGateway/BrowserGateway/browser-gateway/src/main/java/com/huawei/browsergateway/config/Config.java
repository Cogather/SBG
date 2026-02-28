package com.huawei.browsergateway.config;

import cn.hutool.core.io.FileUtil;
import com.huawei.browsergateway.entity.enums.RecordModeEnum;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Data
@Configuration
public class Config {
    public static final String USER_DATA_PATH = "userdata";
    public static final String BASE_DATA_PATH = "basedata";
    public static final String LOCAL_STORAGE_PATH = "storage";
    public static final String TMP_PATH = "tmp";
    public static final String EXTENSION_PATH = "extension";
    public static final String KEY = "keys";
    public static final String TOUCH = "touch";
    public static final String RECORD = "record";
    public static final String  JAR = "jar";

    @Value("${browsergw.workspace}")
    private String workspace;

    @Value("${browsergw.context-limit:40}")
    private int contextLimit;

    @Value("${server.address}")
    private String address;
    @Value("${browsergw.context-limit:40}")
    private Integer port;

    @Resource
    private ReportConfig report;
    @Resource
    private WebsocketConfig websocket;
    @Resource
    private TcpConfig tcp;
    @Resource
    private ChromeConfig chrome;

    public String getLocalStoragePath() {
        return FileUtil.file(workspace, LOCAL_STORAGE_PATH).getAbsolutePath();
    }

    public String getUserDataPath() {
        return FileUtil.file(workspace, USER_DATA_PATH).getAbsolutePath();
    }

    public String getBaseDataPath() {
        return FileUtil.file(workspace, BASE_DATA_PATH).getAbsolutePath();
    }

    public String getTmpPath() {
        return FileUtil.file(workspace, TMP_PATH).getAbsolutePath();
    }

    public String getExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH).getAbsolutePath();
    }

    public String getKeyExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, KEY).getAbsolutePath();
    }

    public String getJarDirPath() {
        return FileUtil.file(workspace, JAR).getAbsolutePath();
    }

    public String getTouchExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, TOUCH).getAbsolutePath();
    }

    public String getRecordExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, RECORD).getAbsolutePath();
    }

    public String getCodecMode() {
        return RecordModeEnum.getRecordNameByMode(chrome.getRecordMode());
    }

    public String getRecordExtensionPage() {
        return String.format("chrome-extension://%s/offscreen.html", chrome.getRecordExtensionId());
    }

    public String getSelfAddr() {
        return this.address + ":" + this.port;
    }

    public String getInnerMediaEndpoint() {
        return this.address + ":" + this.websocket.getMediaPort();
    }
}
