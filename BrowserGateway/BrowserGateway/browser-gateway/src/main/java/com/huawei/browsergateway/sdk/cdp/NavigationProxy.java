package com.huawei.browsergateway.sdk.cdp;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * 导航管理代理
 */
public class NavigationProxy implements WebDriver.Navigation {
    
    private static final Logger log = LoggerFactory.getLogger(NavigationProxy.class);
    
    private final BrowserDriver browserDriver;
    
    public NavigationProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }
    
    @Override
    public void back() {
        log.debug("导航后退");
        if (browserDriver != null) {
            browserDriver.executeScript("window.history.back();");
        }
    }
    
    @Override
    public void forward() {
        log.debug("导航前进");
        if (browserDriver != null) {
            browserDriver.executeScript("window.history.forward();");
        }
    }
    
    @Override
    public void to(String url) {
        log.debug("导航到URL: {}", url);
        if (browserDriver != null) {
            browserDriver.gotoUrl(url);
        }
    }
    
    @Override
    public void to(URL url) {
        log.debug("导航到URL: {}", url);
        if (browserDriver != null && url != null) {
            browserDriver.gotoUrl(url.toString());
        }
    }
    
    @Override
    public void refresh() {
        log.debug("刷新页面");
        if (browserDriver != null) {
            browserDriver.executeScript("window.location.reload();");
        }
    }
}
