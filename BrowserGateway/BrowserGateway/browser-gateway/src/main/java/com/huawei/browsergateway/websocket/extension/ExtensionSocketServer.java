package com.huawei.browsergateway.websocket.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.sdk.MuenDriver;
import com.huawei.browsergateway.service.IChromeSet;
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
 * WebSocket控制流端点（Muen代理）
 * URL: ws://{server}:{muen-port}/control/websocket/{imeiAndImsi}
 * 作为Muen SDK与浏览器扩展之间的代理WebSocket服务器
 */
@ServerEndpoint(value = "/control/websocket/{imeiAndImsi}")
@Component
public class ExtensionSocketServer {
    private static final Logger log = LoggerFactory.getLogger(ExtensionSocketServer.class);
    
    // 会话 -> 用户ID映射
    private static final Map<Session, String> sessionUsers = new ConcurrentHashMap<>();
    
    private static IChromeSet chromeSet;
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public void setChromeSet(IChromeSet chromeSet) {
        ExtensionSocketServer.chromeSet = chromeSet;
    }
    
    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("imeiAndImsi") String imeiAndImsi) {
        try {
            log.info("控制流WebSocket连接建立: imeiAndImsi={}, sessionId={}", imeiAndImsi, session.getId());
            
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
            
            // 检查浏览器实例是否存在
            if (chromeSet != null) {
                UserChrome userChrome = chromeSet.get(userId);
                if (userChrome == null) {
                    log.warn("浏览器实例不存在: userId={}", userId);
                } else {
                    log.info("控制流WebSocket连接成功: userId={}", userId);
                }
            }
            
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
     * 接收文本消息（JSON格式的控制命令）
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = sessionUsers.get(session);
        if (userId == null) {
            log.warn("收到消息但用户未登录: sessionId={}", session.getId());
            return;
        }
        
        try {
            log.debug("收到控制流消息: userId={}, message={}", userId, message);
            
            // 解析消息
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            String type = (String) messageMap.get("type");
            Object payload = messageMap.get("payload");
            
            // 获取浏览器实例和MuenDriver
            if (chromeSet != null) {
                UserChrome userChrome = chromeSet.get(userId);
                if (userChrome != null && userChrome.getMuenDriver() != null) {
                    MuenDriver muenDriver = userChrome.getMuenDriver();
                    
                    // 将消息转发给MuenDriver处理
                    // 根据Muen-SDK文档，应该调用receiveMessageFromWebscoket方法
                    try {
                        muenDriver.receiveMessageFromWebscoket(message);
                        log.debug("转发控制命令到MuenDriver: userId={}, type={}, messageLength={}", 
                                userId, type, message.length());
                    } catch (Exception e) {
                        log.error("调用MuenDriver处理消息失败: userId={}, type={}", userId, type, e);
                    }
                    
                    // 更新心跳
                    chromeSet.updateHeartbeats(userId, System.nanoTime());
                } else {
                    log.warn("浏览器实例或MuenDriver不存在: userId={}", userId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理控制流消息失败: userId={}", userId, e);
        }
    }
    
    /**
     * 接收二进制消息
     */
    @OnMessage
    public void onBinaryMessage(Session session, byte[] data) {
        String userId = sessionUsers.get(session);
        if (userId == null) {
            log.warn("收到二进制消息但用户未登录: sessionId={}", session.getId());
            return;
        }
        
        try {
            log.debug("收到控制流二进制消息: userId={}, size={}", userId, data.length);
            
            // 获取浏览器实例和MuenDriver
            if (chromeSet != null) {
                UserChrome userChrome = chromeSet.get(userId);
                if (userChrome != null && userChrome.getMuenDriver() != null) {
                    MuenDriver muenDriver = userChrome.getMuenDriver();
                    
                    // 根据Muen-SDK文档：二进制消息需要通过HWContext传递
                    // 创建HWContext并绑定ChromeDriver
                    com.huawei.browsergateway.sdk.HWContext hwContext = 
                            new com.huawei.browsergateway.sdk.HWContext();
                    hwContext.setChromeDriver(userChrome.getChromeDriver());
                    
                    // 调用MuenDriver的Handle方法处理二进制消息
                    muenDriver.handle(hwContext, data);
                    
                    // 更新心跳
                    chromeSet.updateHeartbeats(userId, System.nanoTime());
                } else {
                    log.warn("浏览器实例或MuenDriver不存在: userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("处理二进制消息失败: userId={}", userId, e);
        }
    }
    
    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session) {
        String userId = sessionUsers.remove(session);
        if (userId != null) {
            log.info("控制流WebSocket连接关闭: userId={}, sessionId={}", userId, session.getId());
        }
    }
    
    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String userId = sessionUsers.get(session);
        log.error("控制流WebSocket错误: userId={}, sessionId={}", userId, session.getId(), error);
        
        if (userId != null) {
            sessionUsers.remove(session);
        }
    }
    
    /**
     * 向客户端发送消息
     * 根据Moon-SDK文档：检查WebSocket会话并发送文本消息
     */
    public static void sendMessage(String userId, Object message) {
        if (userId == null || message == null) {
            log.warn("发送消息失败: userId或message为null");
            return;
        }
        
        // 查找用户对应的Session
        Session targetSession = null;
        for (Map.Entry<Session, String> entry : sessionUsers.entrySet()) {
            if (userId.equals(entry.getValue())) {
                targetSession = entry.getKey();
                break;
            }
        }
        
        if (targetSession == null) {
            log.warn("未找到用户的WebSocket会话: userId={}", userId);
            return;
        }
        
        if (!targetSession.isOpen()) {
            log.warn("WebSocket会话已关闭: userId={}, sessionId={}", userId, targetSession.getId());
            sessionUsers.remove(targetSession);
            return;
        }
        
        try {
            String messageStr = message instanceof String ? (String) message : message.toString();
            targetSession.getBasicRemote().sendText(messageStr);
            log.debug("WebSocket消息发送成功: userId={}, message={}", userId, messageStr);
        } catch (Exception e) {
            log.error("发送WebSocket消息异常: userId={}", userId, e);
        }
    }
}
