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
    
    // 校验和长度（4字节，可选）
    private static final int CHECKSUM_LENGTH = 4;
    
    // 最大消息长度（防止内存溢出）
    private static final int MAX_MESSAGE_LENGTH = 10 * 1024 * 1024; // 10MB
    
    // 是否启用校验和验证
    private final boolean checksumEnabled;
    
    public TlvDecoder() {
        this(true); // 默认启用校验和
    }
    
    public TlvDecoder(boolean checksumEnabled) {
        this.checksumEnabled = checksumEnabled;
    }
    
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
        
        // 计算需要读取的总字节数（包括校验和）
        int checksumSize = checksumEnabled ? CHECKSUM_LENGTH : 0;
        int totalBytesNeeded = length + checksumSize;
        
        // 检查是否有足够的字节读取Value和校验和
        if (in.readableBytes() < totalBytesNeeded) {
            // 数据不完整，重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }
        
        // 读取Value
        byte[] value = new byte[length];
        in.readBytes(value);
        
        // 读取校验和（如果启用）
        int checksum = 0;
        if (checksumEnabled) {
            checksum = in.readInt(); // 读取4字节校验和（大端序）
        }
        
        // 创建TlvMessage对象
        TlvMessage message = new TlvMessage(type, value);
        message.setChecksum(checksum);
        message.setChecksumEnabled(checksumEnabled);
        
        // 验证校验和
        if (checksumEnabled && !message.verifyChecksum()) {
            log.error("TLV消息校验和验证失败: type={}, length={}, 关闭连接", type, length);
            ctx.close();
            return;
        }
        
        out.add(message);
        log.debug("解码TLV消息: type={}, length={}, checksum={}", type, length, checksum);
    }
}
