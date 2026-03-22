package com.huawei.mobile.encode;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.Map;

public class TlvEncode {
    private static final Log log = LogFactory.get();
    private ByteBuf byteBuf;

    public ByteBuf getByteBuf() {
        return this.byteBuf;
    }

    public TlvEncode(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void writeByte(int key, byte b) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(1);
        this.byteBuf.writeByte(b);
    }

    public void writeInt(int key, int i) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(4);
        this.byteBuf.writeInt(i);
    }

    public void writeLong(int key, long l) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(8);
        this.byteBuf.writeLong(l);
    }

    public void writeBytes(int key, byte[] bytes) {
        this.byteBuf.writeInt(key);
        if (null != bytes) {
            this.byteBuf.writeInt(bytes.length);
            this.byteBuf.writeBytes(bytes);
        } else {
            this.byteBuf.writeInt(0);
        }
    }

    public void writeByteBuf(int key, ByteBuf buf) {
        this.byteBuf.writeInt(key);
        if (null != buf) {
            buf.readerIndex(0);
            int len = buf.readableBytes();
            this.byteBuf.writeInt(len);
            this.byteBuf.writeBytes(buf);
        } else {
            this.byteBuf.writeInt(0);
        }
    }

    public void writeString(int key, String str) {
        this.byteBuf.writeInt(key);
        if (null != str) {
            byte[] bytes = str.getBytes(Charset.forName("utf-8"));
            int len = bytes.length;
            this.byteBuf.writeInt(len);
            this.byteBuf.writeBytes(bytes);
        }
    }

    public void writeMap(TlvData<Object> data) {
        for (Map.Entry<Integer, Object> integerObjectEntry : data.entrySet()) {
            Map.Entry<Integer, Object> entry = (Map.Entry) integerObjectEntry;
            Integer key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof String) {
                this.writeString(key, (String) val);
            } else if (val instanceof byte[]) {
                this.writeBytes(key, (byte[]) val);
            } else if (val instanceof Byte) {
                this.writeByte(key, (Byte) val);
            } else if (val instanceof Integer) {
                this.writeInt(key, (Integer) val);
            } else if (val instanceof Long) {
                this.writeLong(key, (Long) val);
            } else {
                if (!(val instanceof ByteBuf)) {
                    log.error("[TlvEncode writeMap]  data：{}", JSONUtil.toJsonStr(data, JSONConfig.create().setIgnoreNullValue(false)));
                    throw new UnsupportedOperationException();
                }
                ByteBuf byteBuf = (ByteBuf) val;
                byteBuf.readerIndex(0);
                this.writeByteBuf(key, byteBuf);
            }
        }
    }
}
