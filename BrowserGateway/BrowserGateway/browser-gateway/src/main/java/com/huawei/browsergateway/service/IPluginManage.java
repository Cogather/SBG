package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;

/**
 * 插件管理接口
 */
public interface IPluginManage {
    
    /**
     * 加载扩展
     */
    boolean loadExtension(LoadExtensionRequest request);
    
    /**
     * 获取插件信息
     */
    PluginActive getPluginInfo();
}
