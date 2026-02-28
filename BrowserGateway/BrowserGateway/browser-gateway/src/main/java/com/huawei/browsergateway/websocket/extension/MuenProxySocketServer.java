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

    @OnOpen
    public void onOpen(Session session, @PathVariable String imeiAndImsi
            , @RequestParam MultiValueMap<String, String> requestMap) {
        log.info("proxy communication between muen sdk and extension, user login:{}", imeiAndImsi);
        session.setAttribute(SocketKeyConst.USER_ID_KEY, imeiAndImsi);
        muenSessionManager.addSession(imeiAndImsi, session);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("proxy communication between muen sdk and extension, user logout:{}"
                , session.getAttribute("userId").toString());
        muenSessionManager.del(session.getAttribute("userId").toString());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("proxy communication between muen sdk and extension error, user:{}"
                , session.getAttribute("userId").toString(), error);
        muenSessionManager.del(session.getAttribute("userId").toString());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = session.getAttribute(SocketKeyConst.USER_ID_KEY).toString();
        chromeSet.get(userId).getMuenDriver().receiveMessageFromWebscoket(userId, message);
    }

    @OnBinary
    public void onBinary(Session session, byte[] data) {
    }
}