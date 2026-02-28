package com.huawei.browsergateway.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.util.HttpUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

@Component
public class InitServer {
    private static final Logger log = LogManager.getLogger(InitServer.class);
    private final ExtensionManageService extensionManageService;
    private final ICse cse;

    private final IPluginManage pluginManage;

    public InitServer(ExtensionManageService extensionManageService, IPluginManage pluginManage, ICse cse) {
        this.extensionManageService = extensionManageService;
        this.pluginManage = pluginManage;
        this.cse = cse;
        startInitializationThread();
    }

    private void startInitializationThread() {
        new Thread(() -> {
            try {
                initializeExtensions();
            } catch (Exception e) {
                log.error("Initialization thread failed", e);
            }
        }).start();
    }

    private void initializeExtensions() throws InterruptedException {
        while (!pluginManage.getPluginStatus().equals(Constant.COMPLETE)) {
            try {
                LoadExtensionRequest loadExtensionRequest = getLoadExtensionRequest();
                if (StrUtil.isEmpty(loadExtensionRequest.getBucketName()) ||
                        StrUtil.isEmpty(loadExtensionRequest.getExtensionFilePath())) {
                    Thread.sleep(5000);
                    continue;
                }
                if (extensionManageService.loadExtension(loadExtensionRequest)) {
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to load plugin on starting", e);
            }
            Thread.sleep(5000); // 5秒间隔
        }
    }


    private LoadExtensionRequest getLoadExtensionRequest() {
        String endpoint = cse.getReportEndpoint();
        String url = String.format("http://%s/plugin/v1/current", endpoint);
        List<PluginActive> pluginActives = HttpUtil.request(url, HttpPost.METHOD_NAME, null,
                new TypeReference<>() {
                });
        if (CollectionUtil.isEmpty(pluginActives)) {
            return new LoadExtensionRequest();
        }
        var pluginActive = pluginActives.get(0);
        LoadExtensionRequest loadExtensionRequest = new LoadExtensionRequest();
        loadExtensionRequest.setName(pluginActive.getName());
        loadExtensionRequest.setVersion(pluginActive.getVersion());
        loadExtensionRequest.setExtensionFilePath(pluginActive.getPackageName());
        loadExtensionRequest.setBucketName(pluginActive.getBucketName());
        log.info("get current plugin success, name: {}, version: {}", pluginActive.getName(), pluginActive.getVersion());
        return loadExtensionRequest;
    }
}

