package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.moon.cloud.browser.sdk.core.MuenDriver;

/**
 * 插件管理接口，负责 SDK 插件的加载、状态维护及驱动实例创建
 */
public interface IPluginManage {

    /**
     * 获取当前激活的插件信息
     */
    PluginActive getPluginActive();

    /**
     * 更新激活插件的元数据（名称、版本、类型）
     */
    void updatePluginActive(String name, String version, String type);

    /**
     * 加载插件：包括 SDK JAR 和 JS 扩展文件
     *
     * @param keyPath   keys 扩展目录路径
     * @param touchPath touch 扩展目录路径
     * @param jarPaht   SDK JAR 文件路径
     */
    void loadPlugin(String keyPath, String touchPath, String jarPaht);

    /**
     * 更新插件加载状态，并触发对应告警
     */
    void updateStatus(String pluginStatus);

    /**
     * 获取当前插件加载状态
     */
    String getPluginStatus();

    /**
     * 为指定用户创建 MuenDriver 实例
     *
     * @param userId 用户 ID
     * @return MuenDriver 实例，插件未加载时返回 null
     */
    MuenDriver createDriver(String userId);
}
