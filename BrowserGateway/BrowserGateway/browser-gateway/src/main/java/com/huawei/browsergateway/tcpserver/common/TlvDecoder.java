package com.huawei.browsergateway.tcpserver.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * TLV协议解码器
 * 将字节流解码为TlvMessage对象
 */
public class TlvDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(TlvDecoder.class);
    
    // TLV消息头部长度：Type(2字节) + Length(4字节) = 6字节
    private static final int HEADER_LENGTH = 6;
    
    // 最大消息长度（防止内存溢出）
    private static final int MAX_MESSAGE_LENGTH = 10 * 1024 * 1024; // 10MB
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的字节读取头部
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }
        
        // 标记读取位置
        in.markReaderIndex();
        
        // 读取Type（2字节，大端序）
        short type = in.readShort();
        
        // 读取Length（4字节，大端序）
        int length = in.readInt();
        
        // 验证消息长度
        if (length < 0 || length > MAX_MESSAGE_LENGTH) {
            log.error("无效的消息长度: {}, 关闭连接", length);
            ctx.close();
            return;
        }
        
        // 检查是否有足够的字节读取Value
        if (in.readableBytes() < length) {
            // 数据不完整，重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }
        
        // 读取Value
        byte[] value = new byte[length];
        in.readBytes(value);
        
        // 创建TlvMessage对象
        TlvMessage message = new TlvMessage(type, value);
        out.add(message);
        
        log.debug("解码TLV消息: type={}, length={}", type, length);
    }
}
