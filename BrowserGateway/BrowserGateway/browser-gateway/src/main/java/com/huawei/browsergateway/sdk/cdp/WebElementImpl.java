package com.huawei.browsergateway.sdk.cdp;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Coordinates;
import org.openqa.selenium.interactions.Locatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Web元素实现
 */
public class WebElementImpl implements WebElement, Locatable, TakesScreenshot {
    
    private static final Logger log = LoggerFactory.getLogger(WebElementImpl.class);
    
    private final String elementId;
    private final BrowserDriver browserDriver;
    
    public WebElementImpl(String elementId, BrowserDriver browserDriver) {
        this.elementId = elementId;
        this.browserDriver = browserDriver;
    }
    
    @Override
    public void click() {
        log.debug("点击元素: elementId={}", elementId);
        if (browserDriver != null) {
            String script = String.format(
                "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element) element.click();",
                elementId
            );
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public void submit() {
        log.debug("提交表单: elementId={}", elementId);
        if (browserDriver != null) {
            String script = String.format(
                "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element && element.form) element.form.submit();",
                elementId
            );
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public void sendKeys(CharSequence... keysToSend) {
        log.debug("输入文本: elementId={}", elementId);
        if (browserDriver != null && keysToSend != null) {
            StringBuilder text = new StringBuilder();
            for (CharSequence seq : keysToSend) {
                text.append(seq);
            }
            String script = String.format(
                "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element) { element.value = '%s'; element.dispatchEvent(new Event('input')); }",
                elementId, text.toString().replace("'", "\\'")
            );
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public void clear() {
        log.debug("清空元素: elementId={}", elementId);
        if (browserDriver != null) {
            String script = String.format(
                "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element) element.value = '';",
                elementId
            );
            browserDriver.executeScript(script);
        }
    }
    
    @Override
    public String getTagName() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? element.tagName : '';",
                    elementId
                );
                Object result = browserDriver.executeScript(script);
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取标签名失败", e);
            }
        }
        return "";
    }
    
    @Override
    public String getAttribute(String name) {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? element.getAttribute('%s') : null;",
                    elementId, name.replace("'", "\\'")
                );
                Object result = browserDriver.executeScript(script);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
                log.warn("获取属性失败: name={}", name, e);
            }
        }
        return null;
    }
    
    @Override
    public boolean isSelected() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? element.checked : false;",
                    elementId
                );
                Object result = browserDriver.executeScript(script);
                return result != null && Boolean.parseBoolean(result.toString());
            } catch (Exception e) {
                log.warn("检查选中状态失败", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? !element.disabled : false;",
                    elementId
                );
                Object result = browserDriver.executeScript(script);
                return result != null && Boolean.parseBoolean(result.toString());
            } catch (Exception e) {
                log.warn("检查启用状态失败", e);
            }
        }
        return false;
    }
    
    @Override
    public String getText() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? element.textContent || element.innerText : '';",
                    elementId
                );
                Object result = browserDriver.executeScript(script);
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取文本失败", e);
            }
        }
        return "";
    }
    
    @Override
    public List<WebElement> findElements(By by) {
        // 简化实现
        return List.of();
    }
    
    @Override
    public WebElement findElement(By by) {
        throw new WebDriverException("WebElement.findElement not supported");
    }
    
    @Override
    public boolean isDisplayed() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? element.offsetParent !== null : false;",
                    elementId
                );
                Object result = browserDriver.executeScript(script);
                return result != null && Boolean.parseBoolean(result.toString());
            } catch (Exception e) {
                log.warn("检查显示状态失败", e);
            }
        }
        return false;
    }
    
    @Override
    public Point getLocation() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element) { var rect = element.getBoundingClientRect(); return {x: rect.left, y: rect.top}; } return {x: 0, y: 0};",
                    elementId
                );
                // 简化实现
                return new Point(0, 0);
            } catch (Exception e) {
                log.warn("获取位置失败", e);
            }
        }
        return new Point(0, 0);
    }
    
    @Override
    public Dimension getSize() {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); if (element) { var rect = element.getBoundingClientRect(); return {width: rect.width, height: rect.height}; } return {width: 0, height: 0};",
                    elementId
                );
                // 简化实现
                return new Dimension(0, 0);
            } catch (Exception e) {
                log.warn("获取大小失败", e);
            }
        }
        return new Dimension(0, 0);
    }
    
    @Override
    public Rectangle getRect() {
        Point location = getLocation();
        Dimension size = getSize();
        return new Rectangle(location, size);
    }
    
    @Override
    public String getCssValue(String propertyName) {
        if (browserDriver != null) {
            try {
                String script = String.format(
                    "var element = document.querySelector('[data-element-id=\"%s\"]'); return element ? window.getComputedStyle(element).getPropertyValue('%s') : '';",
                    elementId, propertyName.replace("'", "\\'")
                );
                Object result = browserDriver.executeScript(script);
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取CSS值失败: propertyName={}", propertyName, e);
            }
        }
        return "";
    }
    
    // Locatable接口实现
    
    @Override
    public Coordinates getCoordinates() {
        return new Coordinates() {
            @Override
            public Point onScreen() {
                return getLocation();
            }
            
            @Override
            public Point inViewPort() {
                return getLocation();
            }
            
            @Override
            public Point onPage() {
                return getLocation();
            }
            
            @Override
            public Object getAuxiliary() {
                return elementId;
            }
        };
    }
    
    /**
     * 获取元素ID
     */
    public String getElementId() {
        return elementId;
    }
    
    // TakesScreenshot接口实现
    
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        log.debug("获取元素截图: elementId={}", elementId);
        // 简化实现，返回null或抛出异常
        // 实际实现需要通过CDP命令获取元素截图
        throw new WebDriverException("元素截图功能暂未实现");
    }
}
