package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.sdk.MuenDriver;

/**
 * 插件管理服务接口，负责Moon SDK插件的全生命周期管理
 */
public interface IPluginManage {
    
    /**
     * 加载插件
     * 
     * @param keyPath Key扩展文件路径
     * @param touchPath Touch扩展文件路径
     * @param jarPath SDK插件JAR文件路径
     */
    void loadPlugin(String keyPath, String touchPath, String jarPath);
    
    /**
     * 创建驱动实例
     * 
     * @param userId 用户ID
     * @return MuenDriver 驱动实例，失败返回null
     */
    MuenDriver createDriver(String userId);
    
    /**
     * 更新插件活跃状态
     * 
     * @param name 插件名称
     * @param version 插件版本
     * @param type 插件类型
     */
    void updatePluginActive(String name, String version, String type);
    
    /**
     * 获取插件状态信息
     * 
     * @return PluginActive 插件活跃状态信息
     */
    PluginActive getPluginActive();
    
    /**
     * 更新插件运行状态
     * 
     * @param pluginStatus 插件状态
     */
    void updateStatus(String pluginStatus);
    
    /**
     * 获取插件运行状态
     * 
     * @return 插件状态字符串
     */
    String getPluginStatus();
    
    /**
     * 关闭插件管理器
     * 释放所有资源
     */
    void shutdown();
}
