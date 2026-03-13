package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.huawei.browsergateway.util.encode.Tlv.MAGIC;

/** TLV 协议解码器，从字节流中解析出 {@link Tlv} 对象 */
public class TlvDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(TlvDecoder.class);

    /** 头部最小长度：magic(2) + count(4) + dataLen(4) */
    private static final int HEADER_SIZE = 10;

    private final int maxLen;
    private final FlowRateTracker flowRateTracker;
    private final String serviceType;

    public TlvDecoder(int maxLen, FlowRateTracker flowRateTracker, String serviceType) {
        this.maxLen = maxLen;
        this.flowRateTracker = flowRateTracker;
        this.serviceType = serviceType;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) return;

        in.markReaderIndex();
        int totalBytes = 0;
        try {
            short magic = in.readShort();
            totalBytes += 2;
            if (magic != MAGIC) {
                // 魔数不匹配，跳过一个字节避免死循环
                in.resetReaderIndex();
                in.readByte();
                return;
            }

            int count = in.readInt();
            totalBytes += 4;
            int dataLen = in.readInt();
            totalBytes += 4;

            if (in.readableBytes() < dataLen) {
                in.resetReaderIndex();
                return;
            }

            Tlv tlv = new Tlv();
            tlv.setMagic(MAGIC);
            tlv.setCount(count);
            tlv.setLen(dataLen);

            for (int i = 0; i < count; i++) {
                if (in.readableBytes() < 8) {
                    in.resetReaderIndex();
                    return;
                }
                int t = in.readInt();
                totalBytes += 4;
                int len = in.readInt();
                totalBytes += 4;

                if (len < 0) throw new IllegalArgumentException("invalid len: " + len);
                if (len > maxLen) throw new IllegalArgumentException("len " + len + " exceeds maxLen " + maxLen);

                if (in.readableBytes() < len) {
                    in.resetReaderIndex();
                    return;
                }

                ByteBuf slice = in.readSlice(len).retain();
                try {
                    byte[] data = new byte[slice.readableBytes()];
                    slice.getBytes(slice.readerIndex(), data);
                    tlv.getFields().add(new TlvField(t, len, data));
                    totalBytes += len;
                } finally {
                    slice.release();
                }
            }

            String sessionId = Client.fromCtx(ctx).getStr(Client.VAL_SESSION_ID);
            flowRateTracker.add(sessionId, serviceType, totalBytes);
            out.add(tlv);
        } catch (Exception e) {
            log.warn("TLV decode failed, resetting reader index", e);
            in.resetReaderIndex();
        }
    }
}
