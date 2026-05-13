// Copyright (c) Huawei Technologies Co., Ltd. 2025-2026. All rights reserved.
package com.huawei.browsergateway.tcpserver.control;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.common.Code;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.common.Type;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.event.EventTypeEnum;
import com.huawei.browsergateway.entity.event.FlowStatEvent;
import com.huawei.browsergateway.entity.operate.Session;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserBind;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import com.huawei.browsergateway.util.DateTimeUtil;
import com.huawei.browsergateway.util.encode.LoginResponse;
import com.huawei.browsergateway.util.encode.Message;
import com.huawei.browsergateway.util.encode.Tlv;
import com.huawei.browsergateway.util.encode.TlvCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteOrder;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 控制流TCP服务器处理器
 * 处理客户端登录、心跳和控制事件
 */
public class ControlTcpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(ControlTcpServerHandler.class);
    private static final String FALLBACK_FLAG = "true";

    private final IRemote remote;
    private final ControlClientSet clientSet;
    private final IChromeSet chromeSet;
    private final FlowRateTracker flowRateTracker;
    private final ConcurrentMap<String, Message> loginInfoMap = new ConcurrentHashMap<>();

    public ControlTcpServerHandler(IRemote remote, ControlClientSet clientSet,
                                   IChromeSet chromeSet, FlowRateTracker flowRateTracker) {
        this.remote = remote;
        this.clientSet = clientSet;
        this.chromeSet = chromeSet;
        this.flowRateTracker = flowRateTracker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Control client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Tlv tlv = (Tlv) msg;
        int messageType = tlv.getType();
        Client client = Client.fromCtx(ctx);

        switch (messageType) {
            case Type.LOGIN:
                processLogin(client, tlv);
                break;
            case Type.HEARTBEATS:
                processHeartbeats(client, tlv);
                break;
            default:
                processDefault(client, tlv);
                break;
        }
    }

    /**
     * 处理登录请求
     */
    private void processLogin(Client client, Tlv tlv) throws Exception {
        // TODO: 实现登录请求处理逻辑，包括解析消息、验证、初始化会话、创建浏览器
    }

    /**
     * 解析TLV消息
     */
    private Message parseMessage(Tlv tlv) throws Exception {
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        return message;
    }

    /**
     * 验证登录请求参数
     */
    private void validateLoginRequest(Message message) {
        if (message.getLcdWidth() == 0 || message.getLcdHeight() == 0) {
            log.error("Invalid LCD dimensions: width={}, height={}",
                    message.getLcdWidth(), message.getLcdHeight());
            throw new RuntimeException("LCD width or height is invalid");
        }
    }

    /**
     * 验证用户绑定信息
     */
    private UserBind validateUserBind(String sessionKey, Message message) {
        UserBind userBind = remote.getUserBind(sessionKey);
        if (userBind == null) {
            log.error("User not bound: {}", sessionKey);
            throw new RuntimeException("User bind info not found");
        }

        if (ObjectUtil.notEqual(message.getToken(), userBind.getToken())) {
            log.error("Invalid token for user: {}", sessionKey);
            throw new RuntimeException("User token is invalid");
        }

        return userBind;
    }

    /**
     * 初始化客户端会话信息
     */
    private void initializeClientSession(Client client, Message message, String sessionKey) {
        long currentTime = System.currentTimeMillis();
        String tcpUniqueId = UUID.randomUUID().toString();

        client.set(Client.VAL_APP_TYPE, message.getAppType());
        client.set(Client.VAL_SESSION_ID, sessionKey);
        client.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        client.set(Client.VAL_UPDATE_TIME, currentTime);
        client.set(Client.VAL_START_TIME, currentTime);
        client.set(Client.VAL_TCP_UNIQUE_ID, tcpUniqueId);
        client.set(Client.VAL_NETWORK_TYPE, message.getNetworkType());
    }

    /**
     * 创建浏览器实例
     */
    private void createBrowser(Client client, Message message, UserBind userBind, Tlv tlv) {
        // TODO: 实现创建浏览器实例逻辑，解析请求、调用remote.createChrome
    }

    /**
     * 处理默认控制事件
     */
    private void processDefault(Client client, Tlv tlv) throws Exception {
        long startTime = System.currentTimeMillis();

        Message message = parseMessage(tlv);
        client.ack(message.getType(), Code.OK);

        byte[] tlvBytes = tlv.marshal(ByteOrder.BIG_ENDIAN);
        remote.handleEvent(tlvBytes, client.getStr(Client.VAL_SESSION_ID));

        log.info("Processed control event, cost: {}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 处理心跳请求
     */
    private void processHeartbeats(Client client, Tlv tlv) throws Exception {
        Message message = parseMessage(tlv);

        client.set(Client.VAL_UPDATE_TIME, System.currentTimeMillis());
        client.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        client.ack(message.getType(), Code.OK);

        String sessionKey = client.getStr(Client.VAL_SESSION_ID);
        chromeSet.updateHeartbeats(sessionKey, System.nanoTime());
        remote.expiredUserBind(sessionKey);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Control channel exception", cause);
        closeConnection(ctx, true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Control client disconnected: {}", ctx.channel().remoteAddress());
        closeConnection(ctx, false);
    }

    /**
     * 关闭连接并清理资源
     */
    private void closeConnection(ChannelHandlerContext ctx, boolean isError) {
        Client client = Client.fromCtx(ctx);
        String sessionKey = client.getStr(Client.VAL_SESSION_ID);

        uploadControlFlowStatEvent(sessionKey);
        recordSessionLogout(client);

        if (!isError && FALLBACK_FLAG.equals(client.getStr(Client.HAS_BEEN_FALLBACK))) {
            return;
        }

        if (isError) {
            remote.fallbackByError(sessionKey);
        } else {
            remote.fallback(sessionKey);
        }
    }

    /**
     * 上报控制流流量统计事件
     */
    private void uploadControlFlowStatEvent(String sessionId) {
        long flowRate = flowRateTracker.flowRateStat(sessionId, Constant.CONTROL_SERVICE_TYPE);
        Message message = loginInfoMap.get(sessionId);

        if (message == null) {
            log.warn("Login info not found for flow stat event, sessionId={}", sessionId);
            return;
        }

        FlowStatEvent event = new FlowStatEvent(message);
        Date now = new Date();
        event.setServiceType(Constant.CONTROL_SERVICE_TYPE);
        event.setExitTime(now);
        event.setDataSize(flowRate);

        EventInfo<FlowStatEvent> uploadEvent = EventInfo.create(event, EventTypeEnum.APP_FLOW_RATE_STAT, now);
        loginInfoMap.remove(sessionId);
        remote.reportEvent(uploadEvent);
    }

    /**
     * 记录会话登出
     */
    private void recordSessionLogout(Client client) {
        String sessionKey = client.getStr(Client.VAL_SESSION_ID);
        Integer appType = client.getInt(Client.VAL_APP_TYPE);
        long startMillis = client.getTime(Client.VAL_START_TIME);
        String tcpUniqueId = client.getStr(Client.VAL_TCP_UNIQUE_ID);

        sessionLoginOut(sessionKey, appType, startMillis, tcpUniqueId);
    }

    /**
     * 记录会话登入
     */
    private void recordSessionLogin(String imeiAndImsi, int appType, String tcpUniqueId) {
        long now = System.currentTimeMillis();
        String startTime = DateTimeUtil.millisToDate(now);

        Session session = new Session(imeiAndImsi, appType, startTime, null, tcpUniqueId);
        log.info("Session login: imeiAndImsi={}, appType={}, startedAt={}, tcpUniqueId={}",
                imeiAndImsi, appType, startTime, tcpUniqueId);

        remote.sendSession(JSONUtil.toJsonStr(session));
    }

    /**
     * 记录会话登出
     */
    private void sessionLoginOut(String imeiAndImsi, int appType, long startMillis, String tcpUniqueId) {
        long now = System.currentTimeMillis();
        String endTime = DateTimeUtil.millisToDate(now);
        String startTime = DateTimeUtil.millisToDate(startMillis);

        Session session = new Session(imeiAndImsi, appType, startTime, endTime, tcpUniqueId);
        log.info("Session logout: imeiAndImsi={}, appType={}, startedAt={}, finishedAt={}, tcpUniqueId={}",
                imeiAndImsi, appType, startTime, endTime, tcpUniqueId);

        remote.sendSession(JSONUtil.toJsonStr(session));
    }
}
