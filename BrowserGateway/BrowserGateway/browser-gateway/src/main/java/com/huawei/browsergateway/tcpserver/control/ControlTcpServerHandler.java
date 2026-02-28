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

public class ControlTcpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(ControlTcpServerHandler.class);

    private final IRemote remote;
    private final ControlClientSet cs;
    private final IChromeSet chromeSet;

    private final FlowRateTracker flowRateTracker;

    private final ConcurrentMap<String, Message> loginInfoMap = new ConcurrentHashMap<>();



    public ControlTcpServerHandler(IRemote remote, ControlClientSet cs, IChromeSet chromeSet
            , FlowRateTracker flowRateTracker) {
        this.remote = remote;
        this.cs = cs;
        this.chromeSet = chromeSet;
        this.flowRateTracker = flowRateTracker;
    }

    // 客户端连接建立时触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("control client connected: {}", ctx.channel().remoteAddress());
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

    private void createBrowser(Client cli, Message message, UserBind ub, Tlv tlv) {
        try {
            String jsonString = JSONUtil.toJsonStr(message);
            InitBrowserRequest request = JSONUtil.toBean(jsonString, InitBrowserRequest.class);
            request.setInnerMediaEndpoint(ub.getInnerMediaEndpoint());
            byte[] bytes = tlv.marshal(ByteOrder.BIG_ENDIAN);
            remote.createChrome(bytes, request, (f)-> {
                cli.send(new LoginResponse(ub.getMediaEndpoint(), ub.getMediaTlsEndpoint()));
            });
        }catch (Exception e) {
            cli.ack(message.getType(), Code.FAILED);
            log.error("create chrome instance failed", e);
        }

    }

    private void processLogin(Client cli, Tlv tlv) throws Exception {
        // 解析成message
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        if (message.getLcdWidth() == 0 || message.getLcdHeight() == 0) {
            log.error("login failed, lcd width or height is invalid, width:{}, height:{}"
                    , message.getLcdWidth(), message.getLcdHeight());
            throw new RuntimeException("lcd width or height is invalid");
        }

        String key = Type.tcpBindKey(message.getImei(), message.getImsi());
        UserBind ub = remote.getUserBind(key);
        if (ub == null) {
            log.error("login failed, user not bind, user:{}", key);
            throw new RuntimeException("not found user bind info");
        }
        if (ObjectUtil.notEqual(message.getToken(), ub.getToken())) {
            log.error("login failed, user token is invalid, user:{}", key);
            throw new RuntimeException("user token is invalid");
        }
        long now = System.currentTimeMillis();
        String tcpUniqueId = UUID.randomUUID().toString(); // 生成唯一标识符
        cli.set(Client.VAL_APP_TYPE, message.getAppType());
        cli.set(Client.VAL_SESSION_ID, key);
        cli.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        cli.set(Client.VAL_UPDATE_TIME, now);
        cli.set(Client.VAL_START_TIME, now);
        cli.set(Client.VAL_TCP_UNIQUE_ID, tcpUniqueId);
        cli.set(Client.VAL_NETWORK_TYPE, message.getNetworkType());

        cs.set(key, cli);
        loginInfoMap.put(key, message);
        cli.ack(message.getType(), Code.OK);
        chromeSet.updateHeartbeats(key, System.nanoTime());
        UserBind newUserBind = remote.updateUserBind(key);
        ThreadUtil.execute(() -> createBrowser(cli, message, newUserBind, tlv));

        this.sessionLoginIn(key, message.getAppType(), tcpUniqueId);
    }

    private void processDefault(Client cli, Tlv tlv) throws Exception {
        long start = System.currentTimeMillis();
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        cli.ack(message.getType(), Code.OK);
        byte[] bytes = tlv.marshal(ByteOrder.BIG_ENDIAN);
        remote.handleEvent(bytes, cli.getStr(Client.VAL_SESSION_ID));
        log.info("process control event, cost:{}", System.currentTimeMillis() - start);
    }

    private void processHeartbeats(Client cli, Tlv tlv) throws Exception {
        Message message = new Message();
        TlvCodec.unmarshal(tlv, message);
        cli.set(Client.VAL_UPDATE_TIME, System.currentTimeMillis());
        cli.set(Client.VAL_HEARTBEAT_TIME, System.nanoTime());
        cli.ack(message.getType(), Code.OK);
        String key = cli.getStr(Client.VAL_SESSION_ID);
        chromeSet.updateHeartbeats(key, System.nanoTime());
        remote.expiredUserBind(key);
    }

    // 异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("control exception caught!", cause);
        this.close(ctx, true);
    }

    // 客户端断开连接时触发
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("control client disconnected: {}", ctx.channel().remoteAddress());
        this.close(ctx, false);
    }

    private void close(ChannelHandlerContext ctx, boolean closeByError) {
        Client cli = Client.fromCtx(ctx);
        closeSession(cli);
        String key = cli.getStr(Client.VAL_SESSION_ID);
        this.uploadControlFlowStatEvent(key);
        if (! closeByError && "true".equals(cli.getStr(Client.HAS_BEEN_FALLBACK))) {
            return;
        }
        if (closeByError) {
            remote.fallbackByError(key);
        } else {
            remote.fallback(key);
        }
    }

    private void uploadControlFlowStatEvent(String sessionId) {
        long flowRate = this.flowRateTracker.flowRateStat(sessionId, Constant.CONTROL_SERVICE_TYPE);
        Message message = this.loginInfoMap.get(sessionId);
        if (message == null) {
            log.warn("[control flow stat event]login info not found, sessionId={}", sessionId);
            return;
        }
        FlowStatEvent event = new FlowStatEvent(message);
        Date now = new Date();
        event.setServiceType(Constant.CONTROL_SERVICE_TYPE);
        event.setExitTime(now);
        event.setDataSize(flowRate);
        EventInfo<FlowStatEvent> uploadEvent = EventInfo.create(event, EventTypeEnum.APP_FLOW_RATE_STAT, now);
        this.loginInfoMap.remove(sessionId);
        remote.reportEvent(uploadEvent);
    }


    private void closeSession(Client cli) {
        String key = cli.getStr(Client.VAL_SESSION_ID);
        int appType = cli.getInt(Client.VAL_APP_TYPE);
        long startMillis = cli.getTime(Client.VAL_START_TIME);
        String tcpUniqueId = cli.getStr(Client.VAL_TCP_UNIQUE_ID);
        this.sessionLoginOut(key, appType, startMillis, tcpUniqueId);
    }

    public void sessionLoginIn(String imeiAndImsi, int appType, String tcpUniqueId) {
        long now = System.currentTimeMillis();
        String startStr = DateTimeUtil.millisToDate(now);
        Session session = new Session(imeiAndImsi, String.valueOf(appType), startStr, null, tcpUniqueId);
        log.info("Session login in : imeiAndImsi={}, appType={}, startedAt={}, tcpUniqueId={}",
                imeiAndImsi, appType, startStr, tcpUniqueId);
        remote.sendSession(JSONUtil.toJsonStr(session));
    }

    public void sessionLoginOut(String imeiAndImsi, int appType, long startMillis, String tcpUniqueId) {
        long now = System.currentTimeMillis();
        String endStr = DateTimeUtil.millisToDate(now);
        String startStr = DateTimeUtil.millisToDate(startMillis);
        Session session = new Session(imeiAndImsi, String.valueOf(appType), startStr, endStr, tcpUniqueId);
        log.info("Session login out : imeiAndImsi={}, appType={}, startedAt={}, finishedAt={}, tcpUniqueId={}",
                imeiAndImsi, appType, startStr, endStr, tcpUniqueId);
        remote.sendSession(JSONUtil.toJsonStr(session));
    }
}