package com.huawei.browsergateway.tcpserver;

import com.huawei.browsergateway.util.encode.Ack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    public static final String VAL_APP_TYPE = "appType";
    public static final String VAL_SESSION_ID = "sessionID";
    public static final String VAL_UPDATE_TIME = "updateTime";
    public static final String VAL_HEARTBEAT_TIME = "heartTime";
    public static final String VAL_START_TIME = "startTime";
    public static final String VAL_TCP_UNIQUE_ID = "tcpUniqueId";
    public static final String VAL_NETWORK_TYPE = "networkType";

    public static final String HAS_BEEN_FALLBACK = "hasBeenFallback";

    public static Client fromCtx(ChannelHandlerContext ctx) {
        AttributeKey<Client> ctx1 = AttributeKey.valueOf("ctx");
        Attribute<Client> cliAttr = ctx.attr(ctx1);
        Client ret = cliAttr.get();
        if (ret == null) {
            ret = new Client(ctx.channel());
            cliAttr.set(ret);
        }
        ret.set(VAL_HEARTBEAT_TIME, System.nanoTime());
        return ret;
    }

    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private final Channel channel;
    @Getter
    private String clientIpAddress;

    public Client(Channel channel) {
        this.channel = channel;

        if (channel.remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            this.clientIpAddress = address.getAddress().getHostAddress();
        }
    }

    public void set(String key, Object obj) {
        map.put(key, obj);
    }

    public String getStr(String key) {
        Object ret = map.get(key);
        if (ret == null) {
            return null;
        }
        return (String) ret;
    }

    public Integer getInt(String key) {
        Object ret = map.get(key);
        if (ret == null) {
            return null;
        }
        return (Integer) ret;
    }

    public long getTime(String key) {
        Object ret = map.get(key);
        if (ret == null) {
            return 0L;
        }
        return (long) ret;
    }

    public void close() {
        channel.close();
    }

    public void ack(int id, int code) {
        Ack ack = new Ack(id, code);
        send(ack);
    }

    public void send(Object obj) {
        if (channel.isActive()) {
            channel.writeAndFlush(obj);
        }
    }

}