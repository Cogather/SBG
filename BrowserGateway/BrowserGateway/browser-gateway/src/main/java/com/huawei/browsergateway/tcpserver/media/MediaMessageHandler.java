package com.huawei.browsergateway.tcpserver.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.tcpserver.common.MessageType;
import com.huawei.browsergateway.tcpserver.common.TlvMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * TCP媒体流消息处理器
 * 处理登录、心跳、媒体流数据等
 */
@Component
public class MediaMessageHandler extends SimpleChannelInboundHandler<TlvMessage> {
    private static final Logger log = LoggerFactory.getLogger(MediaMessageHandler.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private MediaClientSet clientSet;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
        short type = msg.getType();
        byte[] value = msg.getValue();
        
        log.debug("收到媒体流消息: type={}, length={}", MessageType.getTypeName(type), msg.getLength());
        
        switch (type) {
            case MessageType.LOGIN:
                handleLogin(ctx, value);
                break;
            case MessageType.HEARTBEATS:
                handleHeartbeat(ctx);
                break;
            case MessageType.VIDEO_FRAME:
            case MessageType.AUDIO_FRAME:
                handleMediaFrame(ctx, type, value);
                break;
            case MessageType.MEDIA_CONFIG:
                handleMediaConfig(ctx, value);
                break;
            case MessageType.LOGOUT:
                handleLogout(ctx);
                break;
            default:
                log.warn("未知的媒体流消息类型: type={}", type);
        }
    }
    
    /**
     * 处理登录消息
     */
    private void handleLogin(ChannelHandlerContext ctx, byte[] value) {
        try {
            String json = new String(value, StandardCharsets.UTF_8);
            log.info("收到媒体流登录请求: {}", json);
            
            // 解析登录参数
            InitBrowserRequest request = objectMapper.readValue(json, InitBrowserRequest.class);
            
            // 生成用户ID
            String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());
            
            // 添加到客户端集合
            clientSet.addClient(userId, ctx.channel());
            
            // 更新心跳
            chromeSet.updateHeartbeats(userId, System.nanoTime());
            
            // 通知MuenDriver媒体TCP连接建立
            try {
                com.huawei.browsergateway.entity.browser.UserChrome userChrome = chromeSet.get(userId);
                if (userChrome != null && userChrome.getMuenDriver() != null) {
                    userChrome.getMuenDriver().onMediaTcpConnected();
                    log.debug("已通知MuenDriver媒体TCP连接建立: userId={}", userId);
                } else {
                    log.debug("UserChrome或MuenDriver不存在，跳过媒体TCP连接回调: userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("通知MuenDriver媒体TCP连接建立失败: userId={}", userId, e);
                // 不阻断登录流程，继续执行
            }
            
            sendSuccessResponse(ctx, "媒体流登录成功");
            
        } catch (Exception e) {
            log.error("处理媒体流登录消息失败", e);
            sendErrorResponse(ctx, "媒体流登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId != null) {
            clientSet.updateHeartbeat(userId);
            chromeSet.updateHeartbeats(userId, System.nanoTime());
            log.debug("收到媒体流心跳: userId={}", userId);
        }
    }
    
    /**
     * 处理媒体帧数据
     */
    private void handleMediaFrame(ChannelHandlerContext ctx, short type, byte[] value) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId == null) {
            log.warn("收到媒体帧但用户未登录: type={}", MessageType.getTypeName(type));
            return;
        }
        
        try {
            // 更新心跳
            clientSet.updateHeartbeat(userId);
            chromeSet.updateHeartbeats(userId, System.nanoTime());
            
            // 这里可以将媒体帧数据转发到WebSocket或其他处理逻辑
            // 暂时只记录日志
            log.debug("收到媒体帧: userId={}, type={}, size={}", 
                    userId, MessageType.getTypeName(type), value.length);
            
        } catch (Exception e) {
            log.error("处理媒体帧失败: userId={}, type={}", userId, MessageType.getTypeName(type), e);
        }
    }
    
    /**
     * 处理媒体配置消息
     */
    private void handleMediaConfig(ChannelHandlerContext ctx, byte[] value) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId == null) {
            log.warn("收到媒体配置但用户未登录");
            return;
        }
        
        try {
            String json = new String(value, StandardCharsets.UTF_8);
            log.info("收到媒体配置: userId={}, config={}", userId, json);
            
            // 这里可以更新媒体配置
            // 暂时只记录日志
            
        } catch (Exception e) {
            log.error("处理媒体配置失败: userId={}", userId, e);
        }
    }
    
    /**
     * 处理登出消息
     */
    private void handleLogout(ChannelHandlerContext ctx) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId != null) {
            log.info("媒体流用户登出: userId={}", userId);
            clientSet.removeClient(ctx.channel());
        }
        ctx.close();
    }
    
    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(ChannelHandlerContext ctx, String message) {
        try {
            String json = "{\"code\":0,\"message\":\"" + message + "\"}";
            byte[] value = json.getBytes(StandardCharsets.UTF_8);
            TlvMessage response = new TlvMessage((short) 0x8001, value);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("发送成功响应失败", e);
        }
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String error) {
        try {
            String json = "{\"code\":-1,\"message\":\"" + error + "\"}";
            byte[] value = json.getBytes(StandardCharsets.UTF_8);
            TlvMessage response = new TlvMessage((short) 0x8002, value);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("媒体流客户端连接: channel={}", ctx.channel().id());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("媒体流客户端断开: channel={}", ctx.channel().id());
        clientSet.removeClient(ctx.channel());
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("媒体流消息处理异常: channel={}", ctx.channel().id(), cause);
        ctx.close();
    }
}
