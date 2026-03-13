package com.huawei.browsergateway.sdk;

import org.openqa.selenium.WebDriver;

import java.net.URL;

/**
 * 导航管理代理实现。
 * 实现 WebDriver Navigation 接口，提供浏览器导航操作。
 * 支持后退、前进、刷新及 URL 导航。
 */
public class NavigationProxy implements WebDriver.Navigation {

    private final BrowserDriver browserDriver;

    /**
     * 构造 NavigationProxy 实例。
     *
     * @param browserDriver 浏览器驱动实例
     */
    public NavigationProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }

    /**
     * 在浏览器历史中后退。
     */
    @Override
    public void back() {
        browserDriver.back();
    }

    /**
     * 在浏览器历史中前进。
     */
    @Override
    public void forward() {
        browserDriver.forward();
    }

    /**
     * 导航到指定 URL。
     * 当前未实现。
     *
     * @param url 目标 URL 字符串
     */
    @Override
    public void to(String url) {
        // 未实现
    }

    /**
     * 导航到指定 URL。
     * 当前未实现。
     *
     * @param url 目标 URL 对象
     */
    @Override
    public void to(URL url) {
        // 未实现
    }

    /**
     * 刷新当前页面。
     * 当前未实现。
     */
    @Override
    public void refresh() {
        // 未实现
    }
}
