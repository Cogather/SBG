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

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 10) {
            return;
        }

        in.markReaderIndex();
        byte[] magic = new byte[2];
        in.readBytes(magic);
        String magicStr = new String(magic, StandardCharsets.UTF_8);

        if (!"mu".equals(magicStr)) {
            in.resetReaderIndex();
            return;
        }

        int count = in.readInt();
        int length = in.readInt();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        ByteBuf data = in.readBytes(length);
        TlbData tlbData = new TlbData();
        byte[] bytes = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), bytes);
        data.release();

        int offset = 0;
        for (int i = 0; i < count; i++) {
            if (offset + 4 > bytes.length) {
                break;
            }

            int key = bytesToInt(bytes, offset);
            offset += 4;

            if (offset + 4 > bytes.length) {
                break;
            }

            int len = bytesToInt(bytes, offset);
            offset += 4;

            if (offset + len > bytes.length) {
                break;
            }

            byte[] valueBytes = new byte[len];
            System.arraycopy(bytes, offset, valueBytes, 0, len);
            offset += len;

            // Store audio/video payload as raw byte[] to avoid UTF-8 corruption
            if (key == ID.AUDIO_DATA || key == ID.VIDEO_DATA) {
                tlbData.put(key, valueBytes);
            } else {
                String value = new String(valueBytes, StandardCharsets.UTF_8);
                tlbData.put(key, value);
            }
        }

        out.add(tlbData);
    }

    private int bytesToInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) << 24
                | (bytes[offset + 1] & 0xff) << 16
                | (bytes[offset + 2] & 0xff) << 8
                | bytes[offset + 3] & 0xff;
    }
}
