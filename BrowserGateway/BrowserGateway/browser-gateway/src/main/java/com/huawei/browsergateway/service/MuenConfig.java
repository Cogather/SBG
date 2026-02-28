package com.huawei.browsergateway.service;

import com.huawei.browsergateway.config.ChromeConfig;
import com.huawei.browsergateway.entity.browser.RouteAppConfig;
import com.huawei.browsergateway.entity.browser.UrlConfig;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import lombok.Data;

import java.util.List;

@Data
public class MuenConfig {
    private List<ChromeConfig> chromeConfigList;
    private List<RouteAppConfig> routeAppConfigList;
    private List<UrlConfig> urlConfigList;

    public MuenConfig(List<ChromeConfig> chromeConfigList, List<RouteAppConfig> routeAppConfigList, List<UrlConfig> urlConfigList) {
        this.chromeConfigList = chromeConfigList;
        this.routeAppConfigList = routeAppConfigList;
        this.urlConfigList = urlConfigList;
    }
}
