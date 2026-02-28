package com.huawei.browsergateway.sdk;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.idealized.target.model.TargetID;
import org.openqa.selenium.remote.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.WebSocket;
import java.util.Map;

/**
 * DevTools代理
 * 提供Chrome DevTools Protocol命令执行能力
 */
public class DevToolsProxy extends DevTools {

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

    public static class ConnectionProxy extends Connection {

        public ConnectionProxy(HttpClient client, String url) {
            super(client, url);
        }
    }

    private static final Log log = LogFactory.get();

    private BrowserDriver driver;

    public DevToolsProxy(BrowserDriver driver) {
        super((dt) -> null, new ConnectionProxy(new HttpClientProxy(), ""));
        this.driver = driver;
    }

    @Override
    public <X> X send(Command<X> command) {
        switch (command.getMethod()) {
            case "Target.createTarget":
                String url = command.getParams().get("url").toString();
                return (X) new TargetID(driver.newPage(url));
            default:
                driver.executeCdp(command.getMethod(), command.getParams());
                return (X) new Object();
        }
    }

    @Override
    public void createSession(String windowHandle) {
        log.info("create session ignore");
    }
}
