package com.huawei.browsergateway.websocket.media;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.enums.RecordModeEnum;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.websocket.SocketKeyConst;
import com.huawei.browsergateway.websocket.media.ffmpeg.FfmpegStreamProcessor;
import com.huawei.browsergateway.websocket.media.webcodecs.WebCodecsStreamProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.util.Map;



@ServerEndpoint(
        path = "/browser/websocket/{imeiAndImsi}",
        host = "${server.address}",
        port = "${browsergw.websocket.media-port}",
        bossLoopGroupThreads = "${browsergw.websocket.boss}",
        workerLoopGroupThreads = "${browsergw.websocket.worker}",
        optionSoBacklog = "1024",
        maxFramePayloadLength = "655360"
)
@EnableAsync
@Component
public class MediaStreamSocketServer {
    private static final Logger log = LogManager.getLogger(MediaStreamSocketServer.class);

    @Autowired
    private MediaClientSet clients;

    @Autowired
    private MediaSessionManager mediaSessionManager;

    @Autowired
    private Config config;

    @Value("${browsergw.drop-frame-multi:0.0}")
    private Double dropFrameMulti;

    @OnOpen
    public void onOpen(Session session, @PathVariable String imeiAndImsi, @RequestParam MultiValueMap<String, String> requestMap) {
        log.info("a user accesses the WebSocket of media streams, userId:{}", imeiAndImsi);
        session.setAttribute(SocketKeyConst.USER_ID_KEY, imeiAndImsi);
        mediaSessionManager.addSession(imeiAndImsi, session);

        MediaStreamProcessor mediaStreamProcessor;
        if (config.getChrome().getRecordMode() == RecordModeEnum.WEBCODECS.getMode()) {
            mediaStreamProcessor = new WebCodecsStreamProcessor(clients, imeiAndImsi);
        } else {
            mediaStreamProcessor = new FfmpegStreamProcessor(clients, imeiAndImsi);
        }
        mediaSessionManager.addProcessor(imeiAndImsi, mediaStreamProcessor);

        Map<String, String> params = requestMap.toSingleValueMap();
        String jsonStr = JSONUtil.toJsonStr(params);
        MediaParam initParam = JSONUtil.toBean(jsonStr, MediaParam.class);
        initParam.setGopSize(initParam.getFrameRate() - 1);
        initParam.setDropFrameMulti(dropFrameMulti);
        try {
            mediaStreamProcessor.init(initParam);
        } catch (Exception e) {
            log.error("stream init error, userId:{}, initParam:{}", imeiAndImsi
                    , JSONUtil.toJsonStr(initParam), e);
            mediaSessionManager.del(imeiAndImsi);
        }
        log.info("WebSocket init success, userId:{}, initParam:{}", imeiAndImsi, JSONUtil.toJsonStr(initParam));
    }

    @OnClose
    public void onClose(Session session) {
        String userId = session.getAttribute(SocketKeyConst.USER_ID_KEY);
        log.info("WebSocket closed, userId:{}", userId);
        mediaSessionManager.del(userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String userId = session.getAttribute(SocketKeyConst.USER_ID_KEY);
        log.error("WebSocket error, userId:{}", userId, error);
        mediaSessionManager.del(userId);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
    }

    @OnBinary
    public void onBinary(Session session, byte[] data) {
        String userId = session.getAttribute(SocketKeyConst.USER_ID_KEY);
        MediaStreamProcessor mediaStreamProcessor = mediaSessionManager.getProcessor(userId);
        if (mediaStreamProcessor == null) {
            log.error("WebSocket binary data error, parser not exists, user:{}", userId);
            return;
        }
        mediaStreamProcessor.processMediaStream(data);
    }
}