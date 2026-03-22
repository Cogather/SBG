package com.huawei.mobile.encode;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class TlvEncoder extends MessageToByteEncoder<TlvData> {
    private static final Log log = LogFactory.get();

    public TlvEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TlvData msg, ByteBuf out) {
        ByteBuf buffer = ctx.alloc().buffer();
        try {
            TlvEncode tlvEncode = new TlvEncode(buffer);
            tlvEncode.writeMap(msg);
            int count = msg.size();
            int len = buffer.writerIndex();
            out.writeBytes("mu".getBytes(StandardCharsets.UTF_8));
            out.writeInt(count);
            out.writeInt(len);
            out.writeBytes(buffer);
        } finally {
            buffer.release();
        }
    }
}
