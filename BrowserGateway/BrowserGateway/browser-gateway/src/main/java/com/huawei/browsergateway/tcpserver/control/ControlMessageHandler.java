package com.huawei.browsergateway.tcpserver.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.common.MessageType;
import com.huawei.browsergateway.tcpserver.common.TlvMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * TCP控制流消息处理器
 * 处理登录、心跳、业务消息等
 */
@Component
public class ControlMessageHandler extends SimpleChannelInboundHandler<TlvMessage> {
    private static final Logger log = LoggerFactory.getLogger(ControlMessageHandler.class);
    
    @Autowired
    private IRemote remoteService;
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private ControlClientSet clientSet;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
        short type = msg.getType();
        byte[] value = msg.getValue();
        
        log.debug("收到控制流消息: type={}, length={}", MessageType.getTypeName(type), msg.getLength());
        
        switch (type) {
            case MessageType.LOGIN:
                handleLogin(ctx, value);
                break;
            case MessageType.HEARTBEATS:
                handleHeartbeat(ctx);
                break;
            case MessageType.KEY_EVENT:
            case MessageType.TOUCH_EVENT:
            case MessageType.MOUSE_EVENT:
            case MessageType.DRAG_EVENT:
            case MessageType.TEXT_INPUT:
            case MessageType.CLIPBOARD:
                handleBusinessMessage(ctx, type, value);
                break;
            case MessageType.LOGOUT:
                handleLogout(ctx);
                break;
            default:
                log.warn("未知的消息类型: type={}", type);
                sendErrorResponse(ctx, "未知的消息类型: " + type);
        }
    }
    
    /**
     * 处理登录消息
     */
    private void handleLogin(ChannelHandlerContext ctx, byte[] value) {
        try {
            String json = new String(value, StandardCharsets.UTF_8);
            log.info("收到登录请求: {}", json);
            
            // 解析登录参数
            InitBrowserRequest request = objectMapper.readValue(json, InitBrowserRequest.class);
            
            // 验证必要参数
            if (request.getImei() == null || request.getImsi() == null) {
                log.error("登录请求缺少必要参数: imei或imsi为空");
                sendErrorResponse(ctx, "登录失败: 缺少必要参数");
                return;
            }
            
            // 生成用户ID
            String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());
            
            // 添加到客户端集合
            clientSet.addClient(userId, ctx.channel());
            
            // 创建浏览器实例
            Consumer<Object> consumer = result -> {
                if (result != null) {
                    log.info("浏览器实例创建成功: userId={}", userId);
                    sendSuccessResponse(ctx, "登录成功");
                } else {
                    log.error("浏览器实例创建失败: userId={}", userId);
                    sendErrorResponse(ctx, "浏览器实例创建失败");
                }
            };
            
            remoteService.createChrome(value, request, consumer);
            
        } catch (Exception e) {
            log.error("处理登录消息失败", e);
            sendErrorResponse(ctx, "登录失败: " + e.getMessage());
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
            log.debug("收到心跳: userId={}", userId);
        }
    }
    
    /**
     * 处理业务消息（按键、触摸等）
     */
    private void handleBusinessMessage(ChannelHandlerContext ctx, short type, byte[] value) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId == null) {
            log.warn("收到业务消息但用户未登录: type={}", MessageType.getTypeName(type));
            sendErrorResponse(ctx, "用户未登录");
            return;
        }
        
        try {
            // 更新心跳
            clientSet.updateHeartbeat(userId);
            chromeSet.updateHeartbeats(userId, System.nanoTime());
            
            // 转发给远程服务处理
            remoteService.handleEvent(value, userId);
            
            log.debug("处理业务消息: userId={}, type={}", userId, MessageType.getTypeName(type));
            
        } catch (Exception e) {
            log.error("处理业务消息失败: userId={}, type={}", userId, MessageType.getTypeName(type), e);
            sendErrorResponse(ctx, "处理消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理登出消息
     */
    private void handleLogout(ChannelHandlerContext ctx) {
        String userId = clientSet.getUserId(ctx.channel());
        if (userId != null) {
            log.info("用户登出: userId={}", userId);
            clientSet.removeClient(ctx.channel());
            chromeSet.delete(userId);
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
            TlvMessage response = new TlvMessage((short) 0x8001, value); // 0x8001表示响应消息
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
            TlvMessage response = new TlvMessage((short) 0x8002, value); // 0x8002表示错误响应
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("控制流客户端连接: channel={}", ctx.channel().id());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("控制流客户端断开: channel={}", ctx.channel().id());
        clientSet.removeClient(ctx.channel());
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("控制流消息处理异常: channel={}", ctx.channel().id(), cause);
        ctx.close();
    }
}
