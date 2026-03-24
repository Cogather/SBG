package com.huawei.mobile.encode;

import io.netty.buffer.ByteBuf;

public class TlbData extends TlvData<ByteBuf> {
    public TlbData() {
    }

    public void release(int key) {
        ByteBuf byteBuf = this.get(key);
        if (byteBuf != null && byteBuf.release()) {
            this.remove(key);
        }

    }

    public void releaseAll() {
        for (ByteBuf byteBuf : this.values()) {
            byteBuf.release();
        }

        this.clear();
    }
}