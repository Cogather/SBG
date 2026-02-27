package com.huawei.browsergateway.entity.plugin;

import lombok.Data;

/**
 * 扩展文件路径封装类
 * 用于封装插件加载过程中涉及的各种文件路径
 */
@Data
public class ExtensionFilePaths {
    /**
     * 按键扩展目录路径
     */
    private String keyDir;
    
    /**
     * 触控扩展目录路径
     */
    private String touchDir;
    
    /**
     * JAR文件路径
     */
    private String jarPath;
}
