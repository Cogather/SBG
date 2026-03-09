package com.huawei.browsergateway.sdk;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 浏览器配置选项
 * 包含浏览器启动和上下文创建所需的所有配置参数
 */
@Data
public class BrowserOptions {
    /** CDP服务端点地址 */
    private String endpoint;

    /** 浏览器类型 */
    private Type.BrowserType browserType;

    /** 基础数据目录 */
    private String baseDataDir;

    /** 浏览器可执行文件路径 */
    private String executablePath;

    /** 扩展插件路径列表 */
    private List<String> extensionPaths;

    /** 扩展插件ID列表 */
    private List<String> extensionIds;

    /** 允许列表中的扩展ID */
    private String allowlistedExtensionId;

    /** 是否无头模式 */
    private boolean headless;

    /** 初始URL */
    private String url;

    /** 视口配置 */
    private Request.ViewPort viewpoint;

    /** 用户数据目录 */
    private String userdata;

    /** 录制数据 */
    @Alias("data")
    private String recordData;

    /** 语言设置 */
    private String language;

    /** 浏览器使用限制数量 */
    private int limit;
}
