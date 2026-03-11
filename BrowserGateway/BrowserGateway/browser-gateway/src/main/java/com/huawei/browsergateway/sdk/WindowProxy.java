package com.huawei.browsergateway.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;

import javax.annotation.Nonnull;
import java.util.Set;


/**
 * 窗口管理代理类
 * 实现WebDriver的Options和Window接口，提供浏览器窗口和选项的管理功能
 */
public class WindowProxy implements WebDriver.Options, WebDriver.Window {

    private static final Logger log = LogManager.getLogger(WindowProxy.class);

    /** 浏览器驱动实例 */
    private final BrowserDriver driver;

    /**
     * 构造函数
     *
     * @param driver 浏览器驱动实例
     */
    public WindowProxy(BrowserDriver driver) {
        this.driver = driver;
    }

    @Override
    @Nonnull
    public WebDriver.Window window() {
        return this;
    }

    @Override
    public void setSize(@Nonnull Dimension targetSize) {
        log.info("webdriver proxy set size: {}", targetSize);
    }

    /*************************************************selenium 原生*****************************/

    @Override
    public void addCookie(Cookie cookie) {
        // 空实现
    }

    @Override
    public void deleteCookieNamed(String name) {
        // 空实现
    }

    @Override
    public void deleteCookie(Cookie cookie) {
        // 空实现
    }

    @Override
    public void deleteAllCookies() {
        // 空实现
    }

    @Override
    public Set<Cookie> getCookies() {
        return null;
    }

    @Override
    public @Nullable Cookie getCookieNamed(String name) {
        return null;
    }

    @Override
    public WebDriver.Timeouts timeouts() {
        return null;
    }

    @Override
    public Logs logs() {
        return null;
    }

    @Override
    public Dimension getSize() {
        return null;
    }

    @Override
    public Point getPosition() {
        return null;
    }

    @Override
    public void setPosition(Point targetPosition) {
        // 空实现
    }

    @Override
    public void maximize() {
        // 空实现
    }

    @Override
    public void minimize() {
        // 空实现
    }

    @Override
    public void fullscreen() {
        // 空实现
    }
}
