package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static com.huawei.browsergateway.util.encode.Tlv.MAGIC;

public class TlvDecoder extends ByteToMessageDecoder {
    private static final int MIN_SIZE = 10;
    private static final short HEADER_TAG = 28021;
    private final int maxLen;

    private final FlowRateTracker flowRateTracker;

    private final String serviceType;

    public TlvDecoder(int maxLen, FlowRateTracker flowRateTracker, String serviceType) {
        this.maxLen = maxLen;
        this.flowRateTracker = flowRateTracker;
        this.serviceType = serviceType;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 确保至少有头部基础长度（magic(2) + count(4) + dataLen(4) = 10字节）
        if (in.readableBytes() < MIN_SIZE) {
            return; // 数据不足，等待更多数据
        }

        in.markReaderIndex(); // 标记当前位置，方便后续重置
        Tlv tlv = new Tlv();
        int totalBytes = 0;
        try {
            short magic = in.readShort();
            totalBytes += 2;
            if (magic != HEADER_TAG) {
                in.resetReaderIndex(); // 重置后跳过错误数据（或根据需求处理）
                in.readByte(); // 跳过一个字节，避免死循环
                return;
            }

            int count = in.readInt();
            totalBytes += 4;
            int dataLen = in.readInt();
            totalBytes += 4;

            // 检查整体数据是否足够（头部已读10字节，剩余数据需 >= dataLen）
            if (in.readableBytes() < dataLen) {
                in.resetReaderIndex(); // 数据不足，重置等待
                return;
            }

            tlv.setMagic(MAGIC);
            tlv.setCount(count);
            tlv.setLen(dataLen);

            for (int i = 0; i < count; i++) {
                // 检查是否有足够字节读取当前TLV的type和len（各4字节，共8字节）
                if (in.readableBytes() < 8) {
                    in.resetReaderIndex(); // 重置等待
                    return;
                }

                int t = in.readInt();
                totalBytes += 4;
                int len = in.readInt();
                totalBytes += 4;

                // 检查len是否合法
                if (len < 0) {
                    throw new IllegalArgumentException("invalid len: " + len + " (negative)");
                }
                if (len > this.maxLen) {
                    throw new IllegalArgumentException("len " + len + " exceeds maxLen " + this.maxLen);
                }

                // 关键修复：检查当前剩余字节是否足够读取value
                if (in.readableBytes() < len) {
                    in.resetReaderIndex(); // 数据不足，重置等待
                    return;
                }

                // 读取value
                ByteBuf v = in.readSlice(len).retain();
                try {
                    byte[] data = new byte[v.readableBytes()];
                    v.getBytes(v.readerIndex(), data);
                    tlv.getFields().add(new TlvField(t, len, data));
                    totalBytes += len;
                } finally {
                    v.release();
                }
            }
            Client client = Client.fromCtx(ctx);
            String sessionId = client.getStr(Client.VAL_SESSION_ID);
            flowRateTracker.add(sessionId, serviceType, totalBytes);
            out.add(tlv); // 解析成功，添加到输出
        } catch (Exception e) {
            in.resetReaderIndex(); // 出错时重置，避免数据混乱
        }
    }
}
