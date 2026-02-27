package com.huawei.browsergateway.websocket.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket媒体流端点
 * URL: ws://{server}:{media-port}/browser/websocket/{imeiAndImsi}
 */
@ServerEndpoint("/browser/websocket/{imeiAndImsi}")
@Component
public class MediaStreamSocketServer {
    private static final Logger log = LoggerFactory.getLogger(MediaStreamSocketServer.class);
    
    // 用户ID -> 处理器映射
    private static final Map<String, MediaStreamProcessor> processors = new ConcurrentHashMap<>();
    
    // 会话 -> 用户ID映射
    private static final Map<Session, String> sessionUsers = new ConcurrentHashMap<>();
    
    private static MediaClientSet mediaClientSet;
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public void setMediaClientSet(MediaClientSet mediaClientSet) {
        MediaStreamSocketServer.mediaClientSet = mediaClientSet;
    }
    
    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("imeiAndImsi") String imeiAndImsi) {
        try {
            log.info("媒体流WebSocket连接建立: imeiAndImsi={}, sessionId={}", imeiAndImsi, session.getId());
            
            // 解析用户ID（imeiAndImsi格式: imei_imsi）
            String[] parts = imeiAndImsi.split("_");
            if (parts.length != 2) {
                log.error("无效的用户标识格式: {}", imeiAndImsi);
                session.close();
                return;
            }
            
            String imei = parts[0];
            String imsi = parts[1];
            String userId = UserIdUtil.generateUserId(imei, imsi);
            
            sessionUsers.put(session, userId);
            
            // 等待客户端发送初始化参数
            log.info("等待客户端发送初始化参数: userId={}", userId);
            
        } catch (Exception e) {
            log.error("处理WebSocket连接失败: imeiAndImsi={}", imeiAndImsi, e);
            try {
                session.close();
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }
    
    /**
     * 接收文本消息（初始化参数）
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = sessionUsers.get(session);
        if (userId == null) {
            log.warn("收到消息但用户未登录: sessionId={}", session.getId());
            return;
        }
        
        try {
            log.info("收到初始化参数: userId={}, message={}", userId, message);
            
            // 解析初始化参数
            MediaParam param = objectMapper.readValue(message, MediaParam.class);
            
            // 创建媒体流处理器
            MediaStreamProcessor processor;
            if ("webcodecs".equalsIgnoreCase(param.getProcessorType())) {
                processor = new WebCodecsStreamProcessor();
            } else {
                processor = new FfmpegStreamProcessor();
            }
            
            // 初始化处理器
            processor.init(userId, session, param);
            processors.put(userId, processor);
            
            // 更新心跳
            if (mediaClientSet != null) {
                mediaClientSet.updateHeartbeat(userId);
            }
            
            log.info("媒体流处理器初始化成功: userId={}, processorType={}", 
                    userId, param.getProcessorType());
            
        } catch (Exception e) {
            log.error("处理初始化参数失败: userId={}", userId, e);
            try {
                session.close();
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }
    
    /**
     * 接收二进制消息（媒体流数据）
     */
    @OnMessage
    public void onBinaryMessage(Session session, byte[] data) {
        String userId = sessionUsers.get(session);
        if (userId == null) {
            log.warn("收到二进制消息但用户未登录: sessionId={}", session.getId());
            return;
        }
        
        MediaStreamProcessor processor = processors.get(userId);
        if (processor == null) {
            log.warn("收到媒体流数据但处理器未初始化: userId={}", userId);
            return;
        }
        
        try {
            // 处理媒体流数据
            processor.processMediaStream(data);
            
            // 更新心跳
            if (mediaClientSet != null) {
                mediaClientSet.updateHeartbeat(userId);
            }
            
        } catch (Exception e) {
            log.error("处理媒体流数据失败: userId={}", userId, e);
        }
    }
    
    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session) {
        String userId = sessionUsers.remove(session);
        if (userId != null) {
            log.info("媒体流WebSocket连接关闭: userId={}, sessionId={}", userId, session.getId());
            
            // 关闭处理器
            MediaStreamProcessor processor = processors.remove(userId);
            if (processor != null) {
                processor.close();
            }
        }
    }
    
    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String userId = sessionUsers.get(session);
        log.error("媒体流WebSocket错误: userId={}, sessionId={}", userId, session.getId(), error);
        
        if (userId != null) {
            // 清理资源
            sessionUsers.remove(session);
            MediaStreamProcessor processor = processors.remove(userId);
            if (processor != null) {
                processor.close();
            }
        }
    }
}
