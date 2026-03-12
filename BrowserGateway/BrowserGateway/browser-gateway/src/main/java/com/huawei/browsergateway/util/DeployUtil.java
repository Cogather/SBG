package com.huawei.browsergateway.util;

import com.huawei.browsergateway.adapter.SystemUtilAdapter;
import com.huawei.browsergateway.adapter.config.AdapterConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 部署环境工具类，通过适配器模式支持内网/外网两种环境
 */
public class DeployUtil {

    private static final Logger log = LoggerFactory.getLogger(DeployUtil.class);

    private static final String APP_ID_KEY = "APPID";
    private static final String APP_NAME_KEY = "APPNAME";
    private static final String DEFAULT_APP_ID = "0";
    private static final String DEFAULT_APP_NAME = "csp";

    private final SystemUtilAdapter systemUtilAdapter;

    /** Spring 依赖注入构造函数 */
    public DeployUtil(SystemUtilAdapter systemUtilAdapter) {
        this.systemUtilAdapter = systemUtilAdapter;
    }

    /** 非 Spring 环境构造函数，自动选择适配器 */
    public DeployUtil() {
        this.systemUtilAdapter = AdapterConfig
                .getAdapterFactory(AdapterConfig.getAdapterEnvironment())
                .createSystemUtilAdapter();
    }

    /**
     * 获取当前服务的 APPID
     *
     * @return 应用 ID，环境变量未配置时返回默认值 {@code "0"}
     */
    public String getCurrentAppID() {
        String appId = systemUtilAdapter.getEnvString(APP_ID_KEY, null);
        if (StringUtils.isBlank(appId)) {
            appId = DEFAULT_APP_ID;
            log.error("getCurrentAppID return default value: {}", DEFAULT_APP_ID);
        }
        return appId;
    }

    /**
     * 获取当前服务的 APPNAME
     *
     * @return 应用名称，环境变量未配置时返回默认值 {@code "csp"}
     */
    public String getCurrentAppName() {
        String appName = systemUtilAdapter.getEnvString(APP_NAME_KEY, null);
        if (StringUtils.isBlank(appName)) {
            appName = DEFAULT_APP_NAME;
            log.error("getCurrentAppName return default value: {}", DEFAULT_APP_NAME);
        }
        return appName;
    }
}
