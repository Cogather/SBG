package com.huawei.browsergateway.config;

import cn.hutool.core.io.FileUtil;
import com.huawei.browsergateway.entity.enums.RecordModeEnum;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * BrowserGateway 主配置类
 * 负责管理核心配置信息，包括工作空间路径、服务器配置和业务逻辑方法
 */
@Data
@Configuration
public class Config {
    /** 用户数据目录路径常量 */
    public static final String USER_DATA_PATH = "userdata";
    /** 基础数据目录路径常量 */
    public static final String BASE_DATA_PATH = "basedata";
    /** 本地存储目录路径常量 */
    public static final String LOCAL_STORAGE_PATH = "storage";
    /** 临时文件目录路径常量 */
    public static final String TMP_PATH = "tmp";
    /** 扩展程序目录路径常量 */
    public static final String EXTENSION_PATH = "extension";
    /** 密钥扩展程序子目录路径常量 */
    public static final String KEY = "keys";
    /** 触摸扩展程序子目录路径常量 */
    public static final String TOUCH = "touch";
    /** 录制扩展程序子目录路径常量 */
    public static final String RECORD = "record";
    /** JAR文件目录路径常量 */
    public static final String JAR = "jar";

    /** 工作空间根目录路径，从配置文件 browsergw.workspace 注入 */
    @Value("${browsergw.workspace}")
    private String workspace;

    /** 上下文限制数量，默认值为40，从配置文件 browsergw.context-limit 注入 */
    @Value("${browsergw.context-limit:40}")
    private int contextLimit;

    /** 服务器地址，从配置文件 server.address 注入 */
    @Value("${server.address}")
    private String address;
    /** 服务器端口，从配置文件 server.port 注入 */
    @Value("${server.port}")
    private Integer port;

    /** 服务上报配置 */
    @Resource
    private ReportConfig report;
    /** WebSocket服务配置 */
    @Resource
    private WebsocketConfig websocket;
    /** TCP服务配置 */
    @Resource
    private TcpConfig tcp;
    /** Chrome浏览器配置 */
    @Resource
    private ChromeConfig chrome;

    /**
     * 获取本地存储目录的绝对路径
     *
     * @return 本地存储目录的绝对路径
     */
    public String getLocalStoragePath() {
        return FileUtil.file(workspace, LOCAL_STORAGE_PATH).getAbsolutePath();
    }

    /**
     * 获取用户数据目录的绝对路径
     *
     * @return 用户数据目录的绝对路径
     */
    public String getUserDataPath() {
        return FileUtil.file(workspace, USER_DATA_PATH).getAbsolutePath();
    }

    /**
     * 获取基础数据目录的绝对路径
     *
     * @return 基础数据目录的绝对路径
     */
    public String getBaseDataPath() {
        return FileUtil.file(workspace, BASE_DATA_PATH).getAbsolutePath();
    }

    /**
     * 获取临时文件目录的绝对路径
     *
     * @return 临时文件目录的绝对路径
     */
    public String getTmpPath() {
        return FileUtil.file(workspace, TMP_PATH).getAbsolutePath();
    }

    /**
     * 获取扩展程序目录的绝对路径
     *
     * @return 扩展程序目录的绝对路径
     */
    public String getExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH).getAbsolutePath();
    }

    /**
     * 获取密钥扩展程序目录的绝对路径
     *
     * @return 密钥扩展程序目录的绝对路径
     */
    public String getKeyExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, KEY).getAbsolutePath();
    }

    /**
     * 获取JAR文件目录的绝对路径
     *
     * @return JAR文件目录的绝对路径
     */
    public String getJarDirPath() {
        return FileUtil.file(workspace, JAR).getAbsolutePath();
    }

    /**
     * 获取触摸扩展程序目录的绝对路径
     *
     * @return 触摸扩展程序目录的绝对路径
     */
    public String getTouchExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, TOUCH).getAbsolutePath();
    }

    /**
     * 获取录制扩展程序目录的绝对路径
     *
     * @return 录制扩展程序目录的绝对路径
     */
    public String getRecordExtensionPath() {
        return FileUtil.file(workspace, EXTENSION_PATH, RECORD).getAbsolutePath();
    }

    /**
     * 获取编解码模式名称
     * <p>
     * 根据Chrome配置中的录制模式，返回对应的录制模式名称
     *
     * @return 编解码模式名称
     */
    public String getCodecMode() {
        return RecordModeEnum.getRecordNameByMode(chrome.getRecordMode());
    }

    /**
     * 获取录制扩展程序的页面地址
     * <p>
     * 返回格式：chrome-extension://{extensionId}/offscreen.html
     *
     * @return 录制扩展程序的页面地址
     */
    public String getRecordExtensionPage() {
        return String.format("chrome-extension://%s/offscreen.html", chrome.getRecordExtensionId());
    }

    /**
     * 获取服务自身地址
     * <p>
     * 格式：{address}:{port}
     *
     * @return 服务自身地址
     */
    public String getSelfAddr() {
        return this.address + ":" + this.port;
    }

    /**
     * 获取内部媒体端点地址
     * <p>
     * 格式：{address}:{websocket.mediaPort}
     *
     * @return 内部媒体端点地址
     */
    public String getInnerMediaEndpoint() {
        return this.address + ":" + this.websocket.getMediaPort();
    }
}
