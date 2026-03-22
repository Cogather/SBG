package com.huawei.mobile;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.dto.Message;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.List;

@Component
@ServerEndpoint(
        path = "/app/websocket/{imeiAndImsi}",
        port = "40002",
        bossLoopGroupThreads = "8",
        workerLoopGroupThreads = "64",
        optionSoBacklog = "1024",
        maxFramePayloadLength = "655360"
)
@EnableAsync
public class WebsocketServer {
    private static final Log log = LogFactory.get();

    @OnOpen
    public void onOpen(Session session, @PathVariable String imeiAndImsi,
                       @RequestParam MultiValueMap<String, String> reqMap) {
        log.info("websocket open, {}", imeiAndImsi);
        BrowserContext context = new BrowserContext();
        List<String> split = StrUtil.split(imeiAndImsi, '_');
        context.getRequest().setImei(split.get(0));
        context.getRequest().setImsi(split.get(1));
        List<String> gidsAddrList = reqMap.get("gids_addr");
        if (gidsAddrList != null && !gidsAddrList.isEmpty()) {
            context.setGidsAddr(gidsAddrList.get(0));
        }
        context.setSession(session);
        session.setAttribute("context", context);
    }

    @OnMessage
    public void onMessage(Session session, String message) throws InterruptedException, IOException {
        BrowserContext context = session.getAttribute("context");
        Message msg = JSONUtil.toBean(message, Message.class);

        switch (msg.getType()) {
            case "login":
                List<String> cs = StrUtil.split(msg.getCs(), 'x');
                context.getRequest().setWidth(cs.get(0));
                context.getRequest().setHeight(cs.get(1));
                context.getRequest().setDeviceType(String.valueOf(msg.getDv()));
                context.getRequest().setAppType(String.valueOf(msg.getAt()));
                if (msg.getGa() != null && !msg.getGa().isEmpty()) {
                    context.setGidsAddr(msg.getGa());
                }
                context.deviceLogin();
                break;
            case "logout":
                context.close();
                break;
            case "direction":
                context.handleDirection(msg.getCt(), msg.getCv());
                break;
            case "upload":
                context.confirmInputHandle(msg.getUt(), msg.getContent());
                break;
            case "send_error":
                context.sendError();
                break;
            case "send_time":
                context.sendUseTime();
                break;
            case "upload_file":
                context.handleUploadFile(msg.getFa());
                break;
            default:
                log.error("un support type :{}", msg.getType());
                break;
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        BrowserContext context = session.getAttribute("context");
        context.close();
        session.close();
        log.info("close websocket");
    }

    @OnError
    public void onError(Session session, Throwable error) throws IOException {
        BrowserContext context = session.getAttribute("context");
        context.close();
        session.close();
        log.error("websocket error", error);
    }
}
