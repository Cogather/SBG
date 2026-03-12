package com.huawei.browsergateway.tcpserver.media;

import cn.hutool.core.util.ObjectUtil;
import com.huawei.browsergateway.common.Code;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.common.Type;
import com.huawei.browsergateway.entity.event.DataDealEvent;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.event.EventTypeEnum;
import com.huawei.browsergateway.entity.event.FlowStatEvent;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserBind;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import com.huawei.browsergateway.util.encode.Message;
import com.huawei.browsergateway.util.encode.Tlv;
import com.huawei.browsergateway.util.encode.TlvCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 媒体流TCP服务器处理器
 * 处理客户端媒体流的登录、心跳和数据传输
 */
public class MediaTcpServerHandle extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(MediaTcpServerHandle.class);

    private final MediaClientSet clientSet;
    private final IRemote remote;
    private final FlowRateTracker flowRateTracker;
    private final ConcurrentMap<String, Message> loginInfoMap = new ConcurrentHashMap<>();

    public MediaTcpServerHandle(MediaClientSet clientSet, IRemote remote, FlowRateTracker flowRateTracker) {
        this.clientSet = clientSet;
        this.remote = remote;
        this.flowRateTracker = flowRateTracker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Media client connected: {}", ctx.channel().remoteAddress());
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
        Message message = parseMessage(tlv);
        String sessionKey = Type.tcpBindKey(message.getImei(), message.getImsi());

        validateUserBind(sessionKey, message);
        initializeClientSession(client, message, sessionKey);

        clientSet.set(sessionKey, client);
        loginInfoMap.put(sessionKey, message);

        client.ack(message.getType(), Code.OK);
        uploadDataDealLoginEvent(message);
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
     * 验证用户绑定信息
     */
    private void validateUserBind(String sessionKey, Message message) {
        UserBind userBind = remote.getUserBind(sessionKey);
        if (userBind == null) {
            log.warn("User bind info not found: {}", sessionKey);
            throw new RuntimeException("User bind info not found");
        }

        if (ObjectUtil.notEqual(message.getToken(), userBind.getToken())) {
            log.warn("Invalid token for user: {}", sessionKey);
            throw new RuntimeException("User token is invalid");
        }
    }

    /**
     * 初始化客户端会话信息
     */
    private void initializeClientSession(Client client, Message message, String sessionKey) {
        long currentTime = System.currentTimeMillis();

        client.set(Client.VAL_APP_TYPE, message.getAppType());
        client.set(Client.VAL_SESSION_ID, sessionKey);
        client.set(Client.VAL_UPDATE_TIME, currentTime);
        client.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        client.set(Client.VAL_START_TIME, currentTime);
        client.set(Client.VAL_NETWORK_TYPE, message.getNetworkType());
    }

    /**
     * 处理默认消息
     */
    private void processDefault(Client client, Tlv tlv) throws Exception {
        Message message = parseMessage(tlv);
        client.ack(message.getType(), Code.OK);
    }

    /**
     * 处理心跳请求
     */
    private void processHeartbeats(Client client, Tlv tlv) throws Exception {
        Message message = parseMessage(tlv);

        client.set(Client.VAL_UPDATE_TIME, System.currentTimeMillis());
        client.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        client.ack(message.getType(), Code.OK);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Media channel exception", cause);
        closeConnection(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Media client disconnected: {}", ctx.channel().remoteAddress());
        closeConnection(ctx);
    }

    /**
     * 关闭连接并清理资源
     */
    private void closeConnection(ChannelHandlerContext ctx) {
        Client client = Client.fromCtx(ctx);
        String sessionId = client.getStr(Client.VAL_SESSION_ID);

        clientSet.del(client);
        uploadMediaFlowStatEvent(sessionId);
    }

    /**
     * 上报数据处理登录事件
     */
    private void uploadDataDealLoginEvent(Message message) {
        Date now = new Date();
        DataDealEvent event = new DataDealEvent(message);
        event.setLoginTime(now);

        EventInfo<DataDealEvent> uploadEvent = EventInfo.create(
                event,
                EventTypeEnum.BROWSER_USER_DATA_DEAL_LOGIN,
                now
        );
        remote.reportEvent(uploadEvent);
    }

    /**
     * 上报媒体流流量统计事件
     */
    private void uploadMediaFlowStatEvent(String sessionId) {
        long flowRate = flowRateTracker.flowRateStat(sessionId, Constant.MEDIA_SERVICE_TYPE);
        Message message = loginInfoMap.get(sessionId);

        if (message == null) {
            log.warn("Login info not found for flow stat event, sessionId={}", sessionId);
            return;
        }

        FlowStatEvent event = new FlowStatEvent(message);
        Date now = new Date();
        event.setServiceType(Constant.MEDIA_SERVICE_TYPE);
        event.setExitTime(now);
        event.setDataSize(flowRate);

        EventInfo<FlowStatEvent> uploadEvent = EventInfo.create(
                event,
                EventTypeEnum.APP_FLOW_RATE_STAT,
                now
        );
        loginInfoMap.remove(sessionId);
        remote.reportEvent(uploadEvent);
    }
}
