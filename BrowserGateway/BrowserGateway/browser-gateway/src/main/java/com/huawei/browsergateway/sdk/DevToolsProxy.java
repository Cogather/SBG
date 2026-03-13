package com.huawei.browsergateway.sdk;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v132.target.model.TargetID;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.WebSocket;

import java.io.UncheckedIOException;

/**
 * DevTools 代理实现，用于执行 CDP 命令。
 * 在 Selenium DevTools API 与 CDP 服务之间提供桥接。
 */
public class DevToolsProxy extends DevTools {

    private static final Log log = LogFactory.get();
    private static final String CREATE_TARGET_METHOD = "Target.createTarget";
    private static final String URL_PARAM = "url";

    /**
     * DevTools 连接的 HTTP 客户端代理。
     * 提供桩实现以兼容 Selenium。
     */
    public static class HttpClientProxy implements HttpClient {

        @Override
        public WebSocket openSocket(HttpRequest request, WebSocket.Listener listener) {
            return null;
        }

        @Override
        public HttpResponse execute(HttpRequest req) throws UncheckedIOException {
            return null;
        }
    }

    /**
     * DevTools 连接代理。
     * 提供桩实现以兼容 Selenium。
     */
    public static class ConnectionProxy extends Connection {

        public ConnectionProxy(HttpClient client, String url) {
            super(client, url);
        }
    }

    private final BrowserDriver driver;

    /**
     * 构造 DevToolsProxy 实例。
     *
     * @param driver 浏览器驱动实例
     */
    public DevToolsProxy(BrowserDriver driver) {
        super((dt) -> null, new ConnectionProxy(new HttpClientProxy(), ""));
        this.driver = driver;
    }

    /**
     * 发送 CDP 命令并返回结果。
     * 对 Target.createTarget 命令做特殊处理。
     *
     * @param command 要执行的 CDP 命令
     * @param <X>     返回类型
     * @return 命令执行结果
     */
    @Override
    public <X> X send(Command<X> command) {
        String method = command.getMethod();

        if (CREATE_TARGET_METHOD.equals(method)) {
            return handleCreateTarget(command);
        }

        driver.executeCdp(method, command.getParams());
        return (X) new Object();
    }

    /**
     * 通过创建新页面处理 Target.createTarget 命令。
     *
     * @param command 创建目标命令
     * @param <X>     返回类型
     * @return 新页面的 TargetID
     */
    private <X> X handleCreateTarget(Command<X> command) {
        String url = command.getParams().get(URL_PARAM).toString();
        String pageId = driver.newPage(url);
        return (X) new TargetID(pageId);
    }

    /**
     * 创建 DevTools 会话。
     * 在代理实现中该操作被忽略。
     *
     * @param windowHandle 窗口句柄
     */
    @Override
    public void createSession(String windowHandle) {
        log.info("DevTools 会话创建已忽略，窗口: {}", windowHandle);
    }
}