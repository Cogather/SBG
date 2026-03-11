/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2019. All rights reserved.
 */

package com.huawei.browsergateway.util;

import com.huawei.browsergateway.adapter.SystemUtilAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 安装部署相关工具类
 * 使用适配器模式,支持内网和外网环境
 */
public class DeployUtil {
    private static final Logger log = LoggerFactory.getLogger(DeployUtil.class);
    private static final String APP_ID_KEY = "APPID";
    private static final String APP_NAME_KEY = "APPNAME";
    private static final String DEFAULT_APP_ID = "0";
    private static final String DEFAULT_APP_NAME = "csp";

    private final SystemUtilAdapter systemUtilAdapter;

    /**
     * 构造函数,用于Spring依赖注入
     * @param systemUtilAdapter 系统工具适配器
     */
    public DeployUtil(SystemUtilAdapter systemUtilAdapter) {
        this.systemUtilAdapter = systemUtilAdapter;
    }

    /**
     * 默认构造函数,用于非Spring环境
     * 会根据环境自动选择适配器
     */
    public DeployUtil() {
        this.systemUtilAdapter = com.huawei.browsergateway.adapter.config.AdapterConfig
                .getAdapterFactory(com.huawei.browsergateway.adapter.config.AdapterConfig.getAdapterEnvironment())
                .createSystemUtilAdapter();
    }

    /**
     * 获取当前服务的APPID
     * @return 应用ID
     */
    public String getCurrentAppID() {
        String appId = systemUtilAdapter.getEnvString(APP_ID_KEY, null);
        if (StringUtils.isBlank(appId)) {
            appId = DEFAULT_APP_ID;
            log.error("getCurrentServiceIP return {}", DEFAULT_APP_ID);
        }
        return appId;
    }

    /**
     * 获取当前服务的APPNAME
     * @return 应用名称
     */
    public String getCurrentAppName() {
        String appName = systemUtilAdapter.getEnvString(APP_NAME_KEY, null);
        if (StringUtils.isBlank(appName)) {
            appName = DEFAULT_APP_NAME;
            log.error("getCurrentAppName return {}", DEFAULT_APP_NAME);
        }
        return appName;
    }

}