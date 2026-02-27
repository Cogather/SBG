package com.huawei.browsergateway.sdk.cdp;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 窗口管理代理
 */
public class WindowProxy implements WebDriver.Window {
    
    private static final Logger log = LoggerFactory.getLogger(WindowProxy.class);
    
    private final BrowserDriver browserDriver;
    
    public WindowProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }
    
    @Override
    public Point getPosition() {
        if (browserDriver != null) {
            try {
                Object x = browserDriver.executeScript("return window.screenX;");
                Object y = browserDriver.executeScript("return window.screenY;");
                return new Point(
                    x != null ? Integer.parseInt(x.toString()) : 0,
                    y != null ? Integer.parseInt(y.toString()) : 0
                );
            } catch (Exception e) {
                log.warn("获取窗口位置失败", e);
            }
        }
        return new Point(0, 0);
    }
    
    @Override
    public Dimension getSize() {
        if (browserDriver != null) {
            try {
                Object width = browserDriver.executeScript("return window.innerWidth;");
                Object height = browserDriver.executeScript("return window.innerHeight;");
                return new Dimension(
                    width != null ? Integer.parseInt(width.toString()) : 1920,
                    height != null ? Integer.parseInt(height.toString()) : 1080
                );
            } catch (Exception e) {
                log.warn("获取窗口大小失败", e);
            }
        }
        return new Dimension(1920, 1080);
    }
    
    @Override
    public void setPosition(Point targetPosition) {
        log.debug("设置窗口位置: {}", targetPosition);
        if (browserDriver != null) {
            String script = String.format("window.moveTo(%d, %d);", targetPosition.getX(), targetPosition.getY());
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public void setSize(Dimension targetSize) {
        log.debug("设置窗口大小: {}", targetSize);
        if (browserDriver != null) {
            String script = String.format("window.resizeTo(%d, %d);", targetSize.getWidth(), targetSize.getHeight());
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public void maximize() {
        log.debug("最大化窗口");
        if (browserDriver != null) {
            browserDriver.executeScript("window.moveTo(0, 0); window.resizeTo(screen.width, screen.height);");
        }
    }
    
    @Override
    public void minimize() {
        log.debug("最小化窗口");
        // 简化实现
    }
    
    @Override
    public void fullscreen() {
        log.debug("全屏窗口");
        if (browserDriver != null) {
            browserDriver.executeScript("document.documentElement.requestFullscreen();");
        }
    }
}
