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

/**
 * TCP客户端连接封装类
 * 管理单个客户端连接的状态信息和通信操作
 */
public class Client {
    // 客户端属性键常量
    public static final String VAL_APP_TYPE = "appType";
    public static final String VAL_SESSION_ID = "sessionID";
    public static final String VAL_UPDATE_TIME = "updateTime";
    public static final String VAL_HEARTBEAT_TIME = "heartTime";
    public static final String VAL_START_TIME = "startTime";
    public static final String VAL_TCP_UNIQUE_ID = "tcpUniqueId";
    public static final String VAL_NETWORK_TYPE = "networkType";
    public static final String HAS_BEEN_FALLBACK = "hasBeenFallback";

    private static final String CONTEXT_ATTRIBUTE_KEY = "ctx";

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Channel channel;

    @Getter
    private String clientIpAddress;

    /**
     * 从ChannelHandlerContext获取或创建Client实例
     *
     * @param ctx Netty通道处理上下文
     * @return Client实例
     */
    public static Client fromCtx(ChannelHandlerContext ctx) {
        AttributeKey<Client> contextKey = AttributeKey.valueOf(CONTEXT_ATTRIBUTE_KEY);
        Attribute<Client> clientAttribute = ctx.attr(contextKey);
        Client client = clientAttribute.get();

        if (client == null) {
            client = new Client(ctx.channel());
            clientAttribute.set(client);
        }

        client.set(VAL_HEARTBEAT_TIME, System.nanoTime());
        return client;
    }

    /**
     * 构造函数
     *
     * @param channel Netty通道
     */
    public Client(Channel channel) {
        this.channel = channel;
        this.clientIpAddress = extractClientIpAddress(channel);
    }

    /**
     * 从Channel中提取客户端IP地址
     */
    private String extractClientIpAddress(Channel channel) {
        if (channel.remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            return address.getAddress().getHostAddress();
        }
        return null;
    }

    /**
     * 设置客户端属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取字符串类型属性
     *
     * @param key 属性键
     * @return 属性值，不存在时返回null
     */
    public String getStr(String key) {
        Object value = attributes.get(key);
        return value != null ? (String) value : null;
    }

    /**
     * 获取整数类型属性
     *
     * @param key 属性键
     * @return 属性值，不存在时返回null
     */
    public Integer getInt(String key) {
        Object value = attributes.get(key);
        return value != null ? (Integer) value : null;
    }

    /**
     * 获取时间戳类型属性
     *
     * @param key 属性键
     * @return 属性值，不存在时返回0
     */
    public long getTime(String key) {
        Object value = attributes.get(key);
        return value != null ? (long) value : 0L;
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        channel.close();
    }

    /**
     * 发送ACK响应
     *
     * @param id 消息ID
     * @param code 响应码
     */
    public void ack(int id, int code) {
        send(new Ack(id, code));
    }

    /**
     * 发送消息对象
     *
     * @param message 待发送的消息对象
     */
    public void send(Object message) {
        if (channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }
}
