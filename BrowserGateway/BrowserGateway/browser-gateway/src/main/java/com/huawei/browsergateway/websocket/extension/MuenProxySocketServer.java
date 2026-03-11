package com.huawei.browsergateway.websocket.extension;

import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.websocket.SocketKeyConst;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

/**
 * Muen 代理 WebSocket 服务器
 *
 * 提供 Muen SDK 与浏览器扩展之间的 WebSocket 通信代理服务
 * 路径：/control/websocket/{imeiAndImsi}
 */
@ServerEndpoint(
        path = "/control/websocket/{imeiAndImsi}",
        host = "${server.address}",
        port = "${browsergw.websocket.muen-port}",
        bossLoopGroupThreads = "${browsergw.websocket.boss}",
        workerLoopGroupThreads = "${browsergw.websocket.worker}",
        optionSoBacklog = "1024",
        maxFramePayloadLength = "655360"
)
@EnableAsync
@Component
public class MuenProxySocketServer {

    private static final Logger log = LogManager.getLogger(MuenProxySocketServer.class);

    @Autowired
    private IChromeSet chromeSet;

    @Autowired
    private MuenSessionManager muenSessionManager;

    /**
     * WebSocket 连接建立回调
     */
    @OnOpen
    public void onOpen(Session session, @PathVariable String imeiAndImsi,
                       @RequestParam MultiValueMap<String, String> requestMap) {
        log.info("proxy communication between muen sdk and extension, user login:{}", imeiAndImsi);
        session.setAttribute(SocketKeyConst.USER_ID_KEY, imeiAndImsi);
        muenSessionManager.addSession(imeiAndImsi, session);
    }

    /**
     * WebSocket 连接关闭回调
     */
    @OnClose
    public void onClose(Session session) {
        String userId = (String) session.getAttribute(SocketKeyConst.USER_ID_KEY);
        log.info("proxy communication between muen sdk and extension, user logout:{}", userId);
        muenSessionManager.del(userId);
    }

    /**
     * WebSocket 错误回调
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String userId = (String) session.getAttribute(SocketKeyConst.USER_ID_KEY);
        log.error("proxy communication between muen sdk and extension error, user:{}", userId, error);
        muenSessionManager.del(userId);
    }

    /**
     * 接收文本消息回调，转发消息到浏览器扩展
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = (String) session.getAttribute(SocketKeyConst.USER_ID_KEY);
        chromeSet.get(userId).getMuenDriver().receiveMessageFromWebscoket(userId, message);
    }

    /**
     * 接收二进制消息回调（当前未使用）
     */
    @OnBinary
    public void onBinary(Session session, byte[] data) {
        // Muen 代理通道不处理二进制数据
    }
}
