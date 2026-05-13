package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.adapter.ServiceManagementAdapter;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.*;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.util.UserIdUtil;

import com.moon.cloud.browser.sdk.core.MuenDriver;

import cn.hutool.json.JSONUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 浏览器会话集合管理实现，维护用户 ID 到 UserChrome 实例的映射，
 * 并负责向 CSE 上报实例使用情况
 *
 * @since 2026-04-16
 */
@Service
public class ChromeSetImpl implements IChromeSet {

    private static final Logger log = LogManager.getLogger(ChromeSetImpl.class);

    private static final String PROPERTY_KEY = "status";
    private static final String REPORT_CHAIN_KEY = "chainEndpoints";

    /** 用户浏览器实例映射表，线程安全 */
    private static final ConcurrentMap<String, UserChrome> userChromeMap = new ConcurrentHashMap<>();

    @Autowired
    private IFileStorage fs;
    @Autowired
    private Config config;
    @Autowired
    private ControlClientSet controlClientSet;
    @Autowired
    private MediaClientSet mediaClientSet;
    @Autowired
    @Lazy
    private IRemote remote;
    @Autowired
    private IPluginManage pluginManage;
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;

    @Autowired
    private TpusedMediaAccumulator tpusedMediaAccumulator;

    @Override
    public UserChrome create(InitBrowserRequest request) {
        // TODO: 实现浏览器实例创建逻辑
    }

    @Override
    public UserChrome get(String userId) {
        return userChromeMap.get(userId);
    }

    @Override
    public void delete(String userId) {
        // TODO: 实现删除用户浏览器实例逻辑
    }

    @Override
    public void deleteForRestart(String userId) {
        // TODO: 实现重启场景删除逻辑
    }

    @Override
    public Set<String> getAllUser() {
        return userChromeMap.keySet();
    }

    @Override
    public synchronized void reportUsed() {
        String id = config.getSelfAddr();
        String mediaInnerEndpoint = config.getAddress() + ":" + config.getWebsocket().getMediaPort();
        ServiceReport report = new ServiceReport(id, config.getReport(),
                mediaInnerEndpoint, pluginManage.getPluginStatus());
        report.setUsed(userChromeMap.size());
        int tpusedSnapshot = tpusedMediaAccumulator.getCurrentMbps();
        report.setTpUsed(tpusedSnapshot);

        Map<String, String> reportMap = new HashMap<>();
        reportMap.put(PROPERTY_KEY, JSONUtil.toJsonStr(report));
        if (!serviceManagementAdapter.reportInstanceProperties(reportMap)) {
            log.error("failed to update properties to cse");
        }

        tpusedMediaAccumulator.reset();
    }

    @Override
    public boolean reportChainEndpoints() {
        Map<String, String> reportMap = new HashMap<>();
        reportMap.put(REPORT_CHAIN_KEY, config.getReport().getChainEndpoints());
        if (!serviceManagementAdapter.reportInstanceProperties(reportMap)) {
            log.error("failed to report {} to cse", REPORT_CHAIN_KEY);
            return false;
        }
        return true;
    }

    @Override
    public void updateHeartbeats(String userId, long heartbeats) {
        UserChrome userChrome = getUserChromeOrWarn(userId);
        if (userChrome != null) {
            userChrome.setHeartbeats(heartbeats);
        }
    }

    @Override
    public long getHeartbeats(String userId) {
        UserChrome userChrome = getUserChromeOrWarn(userId);
        return userChrome != null ? userChrome.getHeartbeats() : 0;
    }

    /**
     * 关闭并删除所有用户浏览器实例
     */
    @Override
    public void deleteAll() {
        log.info("close all chrome instance start.");
        for (String key : new HashMap<>(userChromeMap).keySet()) {
            delete(key);
        }
        userChromeMap.clear();
        reportUsed();
        log.info("close all chrome instance success.");
    }

    /**
     * 内部删除逻辑：reopen=true 时跳过断开连接步骤（重启场景）
     */
    private void deleteInternal(String userId, boolean reopen) {
        // TODO: 实现内部删除逻辑，包括检查用户是否存在、关闭连接、关闭实例、移除映射
    }

    /** 获取用户实例，不存在时打印 warn 日志 */
    private UserChrome getUserChromeOrWarn(String userId) {
        UserChrome userChrome = userChromeMap.get(userId);
        if (userChrome == null) {
            log.warn("user: {} not exist.", userId);
        }
        return userChrome;
    }
}