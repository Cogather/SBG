package com.huawei.browsergateway.sdk;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * 导航管理代理
 */
public class NavigationProxy implements WebDriver.Navigation {
    private final BrowserDriver browserDriver;
    public NavigationProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }
    @Override
    public void back() { browserDriver.back(); }

    @Override
    public void forward() {
        browserDriver.forward();
    }
    @Override
    public void to(String url) {
        browserDriver.gotoUrl(url);
    }
    @Override
    public void to(URL url){
        browserDriver.gotoUrl(url.toString());
    }
    @Override
    public void refresh(){
        browserDriver.gotoUrl(browserDriver.getCurrentUrl());
    }
}
