package com.huawei.browsergateway.sdk;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.idealized.target.model.TargetID;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.WebSocket;

import java.io.UncheckedIOException;
import java.util.Map;

/**
 * DevTools代理类
 * 提供Chrome DevTools Protocol命令执行能力
 * 继承自Selenium的DevTools类，实现自定义的命令处理逻辑
 */
public class DevToolsProxy extends DevTools {

    private static final Log log = LogFactory.get();

    /** 浏览器驱动实例 */
    private final BrowserDriver driver;

    /**
     * 构造函数
     *
     * @param driver 浏览器驱动实例
     */
    public DevToolsProxy(BrowserDriver driver) {
        super((dt) -> null, new ConnectionProxy(new HttpClientProxy(), ""));
        this.driver = driver;
    }

    /**
     * 发送CDP命令
     *
     * @param command CDP命令对象
     * @param <X>     返回类型
     * @return 命令执行结果
     */
    @Override
    public <X> X send(Command<X> command) {
        String method = command.getMethod();
        switch (method) {
            case "Target.createTarget":
                String url = command.getParams().get("url").toString();
                @SuppressWarnings("unchecked")
                X result = (X) new TargetID(driver.newPage(url));
                return result;
            default:
                driver.executeCdp(method, command.getParams());
                @SuppressWarnings("unchecked")
                X defaultResult = (X) new Object();
                return defaultResult;
        }
    }

    /**
     * 创建会话
     * 当前实现中忽略此操作
     *
     * @param windowHandle 窗口句柄
     */
    @Override
    public void createSession(String windowHandle) {
        log.info("create session ignore");
    }

    /**
     * HTTP客户端代理类
     * 实现HttpClient接口，用于DevTools连接
     */
    public static class HttpClientProxy implements HttpClient {

        /**
         * 打开WebSocket连接
         *
         * @param request  HTTP请求
         * @param listener WebSocket监听器
         * @return WebSocket连接（当前返回null）
         */
        @Override
        public WebSocket openSocket(HttpRequest request, WebSocket.Listener listener) {
            return null;
        }

        /**
         * 执行HTTP请求
         *
         * @param request HTTP请求
         * @return HTTP响应（当前返回null）
         * @throws UncheckedIOException IO异常
         */
        @Override
        public HttpResponse execute(HttpRequest request) throws UncheckedIOException {
            return null;
        }
    }

    /**
     * 连接代理类
     * 继承自Selenium的Connection类
     */
    public static class ConnectionProxy extends Connection {

        /**
         * 构造函数
         *
         * @param client HTTP客户端
         * @param url    连接URL
         */
        public ConnectionProxy(HttpClient client, String url) {
            super(client, url);
        }
    }
}
