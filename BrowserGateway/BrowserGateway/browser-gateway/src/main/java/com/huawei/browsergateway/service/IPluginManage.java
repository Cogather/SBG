package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.moon.cloud.browser.sdk.core.MuenDriver;

public interface IPluginManage {

    PluginActive getPluginActive();

    void updatePluginActive(String name, String version, String type);

    void loadPlugin(String keyPath, String touchPath, String jarPaht);

    void updateStatus(String pluginStatus);

    String getPluginStatus();

    MuenDriver createDriver(String userId);
}
