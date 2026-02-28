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


public class MediaTcpServerHandle extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(MediaTcpServerHandle.class);
    private final MediaClientSet cs;

    private final IRemote remote;
    private final FlowRateTracker flowRateTracker;
    private final ConcurrentMap<String, Message> loginInfoMap = new ConcurrentHashMap<>();


    public MediaTcpServerHandle(MediaClientSet cs, IRemote remote, FlowRateTracker flowRateTracker) {
        this.cs = cs;
        this.remote = remote;
        this.flowRateTracker = flowRateTracker;
    }

    // 客户端连接建立时触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("media client connected: {}", ctx.channel().remoteAddress());
    }

    // 接收客户端发送的数据（字节数组）
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Tlv tlv = (Tlv) msg;
        int type = tlv.getType();
        Client cli = Client.fromCtx(ctx);
        switch (type) {
            case Type.LOGIN:
                processLogin(cli, tlv);
                break;
            case Type.HEARTBEATS:
                processHeartbeats(cli, tlv);
                break;
            default:
                processDefault(cli, tlv);
                break;
        }
    }

    private void processLogin(Client client, Tlv tlv) throws Exception {
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        String key = Type.tcpBindKey(message.getImei(), message.getImsi());
        UserBind ub = remote.getUserBind(key);
        if (ub == null) {
            log.warn("user bind info not found: {}", key);
            throw new RuntimeException("not found user bind info");
        }
        if (ObjectUtil.notEqual(message.getToken(), ub.getToken())) {
            log.warn("user token is invalid: {}", key);
            throw new RuntimeException("user token is invalid");
        }

        long now = System.currentTimeMillis();
        client.set(Client.VAL_APP_TYPE, message.getAppType());
        client.set(Client.VAL_SESSION_ID, key);
        client.set(Client.VAL_UPDATE_TIME, now);
        client.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        client.set(Client.VAL_START_TIME, now);
        client.set(Client.VAL_NETWORK_TYPE, message.getNetworkType());

        cs.set(key, client);
        loginInfoMap.put(key, message);

        client.ack(message.getType(), Code.OK);
        this.uploadDataDealLoginEvent(message);
    }

    private void processDefault(Client cli, Tlv tlv) throws Exception {
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        cli.ack(message.getType(), Code.OK);
    }

    private void processHeartbeats(Client cli, Tlv tlv) throws Exception {
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        cli.set(Client.VAL_UPDATE_TIME, System.currentTimeMillis());
        cli.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        cli.ack(message.getType(), Code.OK);
    }

    private void close(ChannelHandlerContext ctx) {
        Client cli = Client.fromCtx(ctx);
        cs.del(cli);
        this.uploadMediaFlowStatEvent(cli.getStr(Client.VAL_SESSION_ID));
    }

    private void uploadDataDealLoginEvent(Message message) {
        Date now = new Date();
        DataDealEvent event = new DataDealEvent(message);
        event.setLoginTime(now);
        EventInfo<DataDealEvent> uploadEvent = EventInfo.create(event, EventTypeEnum.BROWSER_USER_DATA_DEAL_LOGIN, now);
        remote.reportEvent(uploadEvent);
    }

    private void uploadMediaFlowStatEvent(String sessionId) {
        long flowRate = this.flowRateTracker.flowRateStat(sessionId, Constant.MEDIA_SERVICE_TYPE);
        Message message = this.loginInfoMap.get(sessionId);
        if (message == null) {
            log.warn("[media flow stat event]login info not found, sessionId={}", sessionId);
            return;
        }
        FlowStatEvent event = new FlowStatEvent(message);
        Date now = new Date();
        event.setServiceType(Constant.MEDIA_SERVICE_TYPE);
        event.setExitTime(now);
        event.setDataSize(flowRate);
        EventInfo<FlowStatEvent> uploadEvent = EventInfo.create(event, EventTypeEnum.APP_FLOW_RATE_STAT, now);
        this.loginInfoMap.remove(sessionId);
        remote.reportEvent(uploadEvent);
    }

    // 异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("media exception caught!", cause);
        close(ctx);
    }

    // 客户端断开连接时触发
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("media client disconnected: {}", ctx.channel().remoteAddress());
        close(ctx);
    }
}