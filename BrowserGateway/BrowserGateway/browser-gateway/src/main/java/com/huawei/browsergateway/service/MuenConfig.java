package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.browser.ChromeConfig;
import com.huawei.browsergateway.entity.browser.RouteAppConfig;
import com.huawei.browsergateway.entity.browser.UrlConfig;
import lombok.Data;

import java.util.List;

/**
 * SDK 插件配置数据，包含浏览器配置、路由应用配置和 URL 配置列表
 */
@Data
public class MuenConfig {

    private List<ChromeConfig> chromeConfigList;
    private List<RouteAppConfig> routeAppConfigList;
    private List<UrlConfig> urlConfigList;

    public MuenConfig(List<ChromeConfig> chromeConfigList,
                      List<RouteAppConfig> routeAppConfigList,
                      List<UrlConfig> urlConfigList) {
        this.chromeConfigList = chromeConfigList;
        this.routeAppConfigList = routeAppConfigList;
        this.urlConfigList = urlConfigList;
    }
}
