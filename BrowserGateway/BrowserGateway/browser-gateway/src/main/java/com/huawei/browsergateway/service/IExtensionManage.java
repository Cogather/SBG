package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.plugin.ExtensionFilePaths;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;

/**
 * 扩展管理服务接口
 * 作为协调层，负责协调插件管理和浏览器实例管理
 */
public interface IExtensionManage {
    
    /**
     * 加载扩展（协调层方法）
     * 调用插件管理服务加载扩展，并在完成后上报浏览器实例使用情况
     * 
     * @param request 加载扩展请求
     * @return 是否成功
     */
    boolean loadExtension(LoadExtensionRequest request);
    
    /**
     * 下载插件文件（委托给插件管理服务）
     * 
     * @param request 加载扩展请求
     * @return 扩展文件路径对象
     */
    ExtensionFilePaths downPlugin(LoadExtensionRequest request);
    
    /**
     * 插件加载后处理（委托给插件管理服务）
     * 
     * @param extensionFilePaths 扩展文件路径对象
     */
    void postLoadingPlugin(ExtensionFilePaths extensionFilePaths);
}
