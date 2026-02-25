package com.huawei.browsergateway.tcpserver.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLV协议编码器
 * 将TlvMessage对象编码为字节流
 */
public class TlvEncoder extends MessageToByteEncoder<TlvMessage> {
    private static final Logger log = LoggerFactory.getLogger(TlvEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, TlvMessage msg, ByteBuf out) throws Exception {
        if (msg == null) {
            return;
        }
        
        byte[] bytes = msg.toBytes();
        out.writeBytes(bytes);
        
        log.debug("编码TLV消息: type={}, length={}", msg.getType(), msg.getLength());
    }
}
