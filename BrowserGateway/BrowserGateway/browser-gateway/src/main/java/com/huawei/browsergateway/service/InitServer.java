package com.huawei.browsergateway.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.util.HttpUtil;
import org.apache.http.client.methods.HttpPost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务启动初始化组件，在后台线程中轮询远端插件信息并完成首次插件加载，
 * 直到加载成功或插件状态已为 COMPLETE 为止
 */
@Component
public class InitServer {

    private static final Logger log = LogManager.getLogger(InitServer.class);

    private final ExtensionManageService extensionManageService;
    private final IPluginManage pluginManage;
    private final ICse cse;

    public InitServer(ExtensionManageService extensionManageService, IPluginManage pluginManage, ICse cse) {
        this.extensionManageService = extensionManageService;
        this.pluginManage = pluginManage;
        this.cse = cse;
        startInitializationThread();
    }

    /** 启动后台初始化线程，避免阻塞 Spring 上下文启动 */
    private void startInitializationThread() {
        new Thread(() -> {
            try {
                initializeExtensions();
            } catch (Exception e) {
                log.error("Initialization thread failed", e);
            }
        }).start();
    }

    /**
     * 循环尝试加载插件，每次失败后等待 5 秒重试，
     * 直到插件状态变为 COMPLETE 或加载成功
     */
    private void initializeExtensions() throws InterruptedException {
        while (!pluginManage.getPluginStatus().equals(Constant.COMPLETE)) {
            try {
                LoadExtensionRequest request = getLoadExtensionRequest();
                if (StrUtil.isEmpty(request.getBucketName()) || StrUtil.isEmpty(request.getExtensionFilePath())) {
                    Thread.sleep(5000);
                    continue;
                }
                if (extensionManageService.loadExtension(request)) {
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to load plugin on starting", e);
            }
            Thread.sleep(5000);
        }
    }

    /** 从远端查询当前激活的插件信息，构建加载请求 */
    private LoadExtensionRequest getLoadExtensionRequest() {
        String url = String.format("http://%s/plugin/v1/current", cse.getReportEndpoint());
        List<PluginActive> pluginActives = HttpUtil.request(url, HttpPost.METHOD_NAME, null, new TypeReference<>() {});
        if (CollectionUtil.isEmpty(pluginActives)) {
            return new LoadExtensionRequest();
        }
        PluginActive pluginActive = pluginActives.get(0);
        LoadExtensionRequest request = new LoadExtensionRequest();
        request.setName(pluginActive.getName());
        request.setVersion(pluginActive.getVersion());
        request.setExtensionFilePath(pluginActive.getPackageName());
        request.setBucketName(pluginActive.getBucketName());
        log.info("get current plugin success, name: {}, version: {}", pluginActive.getName(), pluginActive.getVersion());
        return request;
    }
}
