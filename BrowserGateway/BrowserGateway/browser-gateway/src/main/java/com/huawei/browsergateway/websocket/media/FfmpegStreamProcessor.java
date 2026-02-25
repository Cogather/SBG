package com.huawei.browsergateway.websocket.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.nio.ByteBuffer;

/**
 * FFmpeg模式媒体流处理器
 * 使用FFmpeg进行编解码处理
 */
public class FfmpegStreamProcessor implements MediaStreamProcessor {
    private static final Logger log = LoggerFactory.getLogger(FfmpegStreamProcessor.class);
    
    private String userId;
    private Session session;
    private MediaParam param;
    
    @Override
    public void init(String userId, Session session, MediaParam param) {
        this.userId = userId;
        this.session = session;
        this.param = param;
        log.info("初始化FFmpeg处理器: userId={}, width={}, height={}, frameRate={}, codecType={}", 
                userId, param.getWidth(), param.getHeight(), param.getFrameRate(), param.getCodecType());
        
        // 这里可以初始化FFmpeg编解码器
        // 暂时只记录日志
    }
    
    @Override
    public void processMediaStream(byte[] data) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket会话已关闭，无法处理媒体流: userId={}", userId);
            return;
        }
        
        try {
            // FFmpeg模式：对媒体流进行编解码处理后再转发
            // 数据格式: [4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
            
            // 这里可以添加FFmpeg编解码逻辑
            // 暂时直接转发原始数据
            ByteBuffer buffer = ByteBuffer.wrap(data);
            session.getBasicRemote().sendBinary(buffer);
            
            log.debug("转发FFmpeg媒体流: userId={}, size={}", userId, data.length);
            
        } catch (Exception e) {
            log.error("处理FFmpeg媒体流失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void close() {
        log.info("关闭FFmpeg处理器: userId={}", userId);
        
        // 这里可以清理FFmpeg资源
        // 暂时只记录日志
        
        this.session = null;
        this.param = null;
    }
}
