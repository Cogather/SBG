package com.huawei.browsergateway.util;

import com.huawei.browsergateway.adapter.SystemUtilAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

public class DeployUtil {
    private static final Logger log= LoggerFactory.getLogger(DeployUtil.class);
    private static final String APP_ID_KEY="appId";
    private static final String APP_NAME_KEY="APPNAME";
    
    private static SystemUtilAdapter systemUtilAdapter;

    public DeployUtil(SystemUtilAdapter systemUtilAdapter) {
        DeployUtil.systemUtilAdapter = systemUtilAdapter;
    }

    /**
     * 默认构造函数,用于非Spring环境
     * 会根据环境自动选择适配器
     */
    public DeployUtil() {
        systemUtilAdapter = com.huawei.browsergateway.adapter.config.AdapterConfig
                .getAdapterFactory(com.huawei.browsergateway.adapter.config.AdapterConfig.getAdapterEnvironment())
                .createSystemUtilAdapter();
    }
    public static String getCurrentAppID() {
        if (systemUtilAdapter == null) {
            log.warn("SystemUtilAdapter is not initialized, returning default appId");
            return "0";
        }
        String appId = systemUtilAdapter.getEnvString(APP_ID_KEY, "0");
        if (StringUtils.isBlank(appId)) {
            appId = "0";
            log.error("getCurrentServiceIP return 0");
        }

        return appId;
    }

    /**
     * 获取当前服务的APPNAME
     * @return 应用名称
     */
    public static String getCurrentAppName() {
        if (systemUtilAdapter == null) {
            log.warn("SystemUtilAdapter is not initialized, returning default appName");
            return "csp";
        }
        String appName = systemUtilAdapter.getEnvString(APP_NAME_KEY, "csp");
        if (StringUtils.isBlank(appName)) {
            appName = "csp";
            log.error("getCurrentAppName return csp");
        }

        return appName;
    }
}
