package com.huawei.browsergateway.sdk;

import org.openqa.selenium.WebDriver;

import java.net.URL;

/**
 * 导航管理代理类
 * 实现WebDriver的Navigation接口，提供浏览器导航功能
 * 包括前进、后退、刷新、跳转等操作
 */
public class NavigationProxy implements WebDriver.Navigation {

    /** 浏览器驱动实例 */
    private final BrowserDriver browserDriver;

    /**
     * 构造函数
     *
     * @param browserDriver 浏览器驱动实例
     */
    public NavigationProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }

    @Override
    public void back() {
        browserDriver.back();
    }

    @Override
    public void forward() {
        browserDriver.forward();
    }

    @Override
    public void to(String url) {
    }

    @Override
    public void to(URL url) {
    }

    @Override
    public void refresh() {
    }
}
