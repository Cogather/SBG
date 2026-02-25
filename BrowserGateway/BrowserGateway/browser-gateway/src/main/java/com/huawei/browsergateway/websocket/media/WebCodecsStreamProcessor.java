package com.huawei.browsergateway.websocket.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.nio.ByteBuffer;

/**
 * WebCodecs模式媒体流处理器
 * 使用浏览器原生WebCodecs API处理媒体流
 */
public class WebCodecsStreamProcessor implements MediaStreamProcessor {
    private static final Logger log = LoggerFactory.getLogger(WebCodecsStreamProcessor.class);
    
    private String userId;
    private Session session;
    private MediaParam param;
    
    @Override
    public void init(String userId, Session session, MediaParam param) {
        this.userId = userId;
        this.session = session;
        this.param = param;
        log.info("初始化WebCodecs处理器: userId={}, width={}, height={}, frameRate={}", 
                userId, param.getWidth(), param.getHeight(), param.getFrameRate());
    }
    
    @Override
    public void processMediaStream(byte[] data) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket会话已关闭，无法处理媒体流: userId={}", userId);
            return;
        }
        
        try {
            // WebCodecs模式：直接转发原始媒体流数据
            // 数据格式: [4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
            ByteBuffer buffer = ByteBuffer.wrap(data);
            session.getBasicRemote().sendBinary(buffer);
            
            log.debug("转发WebCodecs媒体流: userId={}, size={}", userId, data.length);
            
        } catch (Exception e) {
            log.error("处理WebCodecs媒体流失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void close() {
        log.info("关闭WebCodecs处理器: userId={}", userId);
        this.session = null;
        this.param = null;
    }
}
