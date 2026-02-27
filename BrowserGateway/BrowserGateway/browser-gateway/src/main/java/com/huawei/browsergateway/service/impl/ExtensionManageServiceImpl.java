package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.entity.plugin.ExtensionFilePaths;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IExtensionManage;
import com.huawei.browsergateway.service.IPluginManage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 扩展管理服务实现类
 * 作为协调层，负责调用插件管理服务和浏览器实例管理服务
 */
@Service
public class ExtensionManageServiceImpl implements IExtensionManage {
    
    private static final Logger log = LoggerFactory.getLogger(ExtensionManageServiceImpl.class);
    
    @Autowired
    private IPluginManage pluginManage;
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Override
    public synchronized boolean loadExtension(LoadExtensionRequest request) {
        log.info("reload extension, request params: {}", request);
        try {
            // 调用插件管理服务的loadExtension方法（包含下载、加载、后置处理）
            boolean result = pluginManage.loadExtension(request);
            if (result) {
                log.info("success to load extension name:{} version:{}", request.getName(), request.getVersion());
            }
            return result;
        } catch (Exception e) {
            log.error("load extension failed", e);
            return false;
        } finally {
            // 上报浏览器实例使用情况
            chromeSet.reportUsed();
        }
    }
    
    @Override
    public ExtensionFilePaths downPlugin(LoadExtensionRequest request) {
        // 委托给插件管理服务
        return pluginManage.downPlugin(request);
    }
    
    @Override
    public void postLoadingPlugin(ExtensionFilePaths extensionFilePaths) {
        // 委托给插件管理服务
        pluginManage.postLoadingPlugin(extensionFilePaths);
    }
}
