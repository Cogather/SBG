package com.huawei.browsergateway.service.impl;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.*;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ChromeSetImpl implements IChromeSet {
    private static final Logger log = LogManager.getLogger(ChromeSetImpl.class);
    private static final ConcurrentMap<String, UserChrome> userChromeMap = new ConcurrentHashMap<>();

    private static final String PROPERTY_KEY = "status";
    private static final String REPORT_CHAIN_KEY = "chainEndpoints";

    @Autowired
    private IFileStorage fs;
    @Autowired
    private Config config;
    @Autowired
    private ControlClientSet controlClientSet;
    @Autowired
    private MediaClientSet mediaClientSet;
    @Autowired
    private IRemote remote;

    @Autowired
    private IPluginManage pluginManage;
    
    @Autowired
    private com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter serviceManagementAdapter;


    public synchronized void reportUsed() {
        String id = config.getSelfAddr();
        String mediaInnerEndpoint = config.getAddress() + ":" + config.getWebsocket().getMediaPort();
        ServiceReport report = new ServiceReport(id, config.getReport(), mediaInnerEndpoint, pluginManage.getPluginStatus());
        report.setUsed(userChromeMap.size());
        String jsonStr = JSONUtil.toJsonStr(report);
        Map<String, String> reportMap = new HashMap<>();
        reportMap.put(PROPERTY_KEY, jsonStr);
        if (serviceManagementAdapter != null && !serviceManagementAdapter.reportInstanceProperties(reportMap)) {
            log.error("failed to update properties to cse");
        }
    }

    public boolean reportChainEndpoints() {
        Map<String, String> reportMap = new HashMap<>();
        reportMap.put(REPORT_CHAIN_KEY, config.getReport().getChainEndpoints());
        if (serviceManagementAdapter != null && !serviceManagementAdapter.reportInstanceProperties(reportMap)) {
            log.error("failed to report {} to cse", REPORT_CHAIN_KEY);
            return false;
        }
        return true;
    }

    /**
     * If the current user does not have a browser instance, create one.
     *
     * @param request params
     * @return user and corresponding browser instance information
     */
    public UserChrome create(InitBrowserRequest request) {
        log.info("create user chrome, request: {}.", JSONUtil.toJsonStr(request));
        Integer cap = config.getReport().getCap();
        if (userChromeMap.size() >= cap) {
            log.error("cap is not enough, cap: {}, current user size: {}.", cap, userChromeMap.size());
            throw new RuntimeException("cap is not enough!");
        }
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(request.getImei(), request.getImsi());
        MuenDriver muenDriver = pluginManage.createDriver(userId);
        UserChrome chrome = new UserChrome(request, fs, config, muenDriver, controlClientSet, mediaClientSet, remote);
        userChromeMap.put(userId, chrome);
        reportUsed();
        return chrome;
    }


    /**
     * If the user instance already exists, return
     *
     * @param userId user id
     * @return user and corresponding browser instance information
     */
    public UserChrome get(String userId) {
        return userChromeMap.get(userId);
    }

    public void delete(String userId) {
        delete(userId, false);
    }

    public void deleteForRestart(String userId) {
        delete(userId, true);
    }

    /**
     * close user browser and upload user data
     * @param userId userId
     * @param reopen delete instance for restart
     */
    private void delete(String userId, boolean reopen) {
        long start = System.currentTimeMillis();
        UserChrome userChromeInfo = userChromeMap.get(userId);
        if (userChromeInfo == null) {
            log.warn("user: {} not exist.", userId);
            return;
        }

        // The connection does not need to be disconnected in the restart scenario.
        if (!reopen) {
            userChromeInfo.closeConnection();
        }
        userChromeInfo.closeInstance();
        userChromeMap.remove(userId);
        reportUsed();
        log.info("close browser instance and upload data success, userId: {}, cost:{}.", userId,
                System.currentTimeMillis() - start);
    }

    /**
     * get all users
     *
     * @return all userId
     */
    public Set<String> getAllUser() {
        return userChromeMap.keySet();
    }

    /**
     * close all user chrome instance
     */
    public void deleteAll() {
        log.info("close all chrome instance start.");
        for (String key : new HashMap<>(userChromeMap).keySet()) {
            delete(key);
        }
        userChromeMap.clear();
        reportUsed();
        log.info("close all chrome instance success.");
    }

    @Override
    public void updateHeartbeats(String userId, long heartbeats) {
        UserChrome userChromeInfo = userChromeMap.get(userId);
        if (userChromeInfo == null) {
            log.warn("user: {} not exist.", userId);
            return;
        }
        userChromeInfo.setHeartbeats(heartbeats);
    }

    @Override
    public long getHeartbeats(String userId) {
        UserChrome userChromeInfo = userChromeMap.get(userId);
        if (userChromeInfo == null) {
            log.warn("user: {} not exist.", userId);
            return 0;
        }
        return userChromeInfo.getHeartbeats();
    }

}