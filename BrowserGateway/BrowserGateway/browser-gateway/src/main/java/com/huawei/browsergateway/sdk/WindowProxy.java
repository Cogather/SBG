package com.huawei.browsergateway.sdk;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * 窗口管理代理
 */
public class WindowProxy implements WebDriver.Options,WebDriver.Window {
    
    private static final Logger log = LoggerFactory.getLogger(WindowProxy.class);
    
    private final BrowserDriver driver;

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

    }

    @Override
    public void deleteCookieNamed(String name) {

    }

    @Override
    public void deleteCookie(Cookie cookie) {

    }

    @Override
    public void deleteAllCookies() {

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

    }

    @Override
    public void maximize() {

    }

    @Override
    public void minimize() {

    }

    @Override
    public void fullscreen() {

    }
}