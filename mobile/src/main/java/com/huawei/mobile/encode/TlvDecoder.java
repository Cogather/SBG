package com.huawei.mobile.encode;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class TlvDecoder extends ByteToMessageDecoder {
    private static final Log log = LogFactory.get();
    private static final int MIN_SIZE = 10;
    private static final short HEADER_TAG = 28021;
    private int maxLen;

    public TlvDecoder(int maxLen) {
        this.maxLen = maxLen;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 确保至少有头部基础长度（magic(2) + count(4) + dataLen(4) = 10字节）
        if (in.readableBytes() < 10) {
            return; // 数据不足，等待更多数据
        }

        in.markReaderIndex(); // 标记当前位置，方便后续重置
        try {
            short magic = in.readShort();
            if (magic != 28021) {
                log.error("magic header error, actual: {}", magic);
                in.resetReaderIndex(); // 重置后跳过错误数据（或根据需求处理）
                in.readByte(); // 跳过一个字节，避免死循环
                return;
            }

            int count = in.readInt();
            int dataLen = in.readInt();

            // 检查整体数据是否足够（头部已读10字节，剩余数据需 >= dataLen）
            if (in.readableBytes() < dataLen) {
//                log.debug("insufficient data for dataLen, need: {}, available: {}",
//                        dataLen, in.readableBytes());
                in.resetReaderIndex(); // 数据不足，重置等待
                return;
            }

            TlbData tlv = new TlbData();
            int totalRead = 0; // 记录已读取的TLV总长度（用于校验）

            for (int i = 0; i < count; i++) {
                // 检查是否有足够字节读取当前TLV的type和len（各4字节，共8字节）
                if (in.readableBytes() < 8) {
                    log.debug("insufficient data for TLV type&len, index: {}", i);
                    in.resetReaderIndex(); // 重置等待
                    return;
                }

                int t = in.readInt();
                int len = in.readInt();
                totalRead += 8; // 累加type+len的8字节

                // 检查len是否合法
                if (len < 0) {
                    throw new IllegalArgumentException("invalid len: " + len + " (negative)");
                }
                if (len > this.maxLen) {
                    throw new IllegalArgumentException("len " + len + " exceeds maxLen " + this.maxLen);
                }

                // 关键修复：检查当前剩余字节是否足够读取value
                if (in.readableBytes() < len) {
                    log.debug("insufficient data for TLV value, need: {}, available: {}, index: {}",
                            len, in.readableBytes(), i);
                    in.resetReaderIndex(); // 数据不足，重置等待
                    return;
                }

                // 读取value
                ByteBuf v = in.readSlice(len).retain();
                tlv.put(t, v);
                totalRead += len; // 累加value的len字节
            }

            // 校验总长度是否与dataLen一致（确保没有多余/缺失数据）
            if (totalRead != dataLen) {
                log.warn("TLV total length mismatch, expected: {}, actual: {}", dataLen, totalRead);
                // 可选：根据业务需求决定是否丢弃或处理
            }

//            log.info("receive data: {}", totalRead);

            out.add(tlv); // 解析成功，添加到输出
        } catch (Exception e) {
            log.error("TLV decode error", e);
            in.resetReaderIndex(); // 出错时重置，避免数据混乱
            // 可选：根据需求决定是否关闭连接或继续
        }
    }
}