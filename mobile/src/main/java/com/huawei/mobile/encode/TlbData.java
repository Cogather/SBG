package com.huawei.mobile.encode;

import io.netty.buffer.ByteBuf;

public class TlbData extends TlvData<Object> {
    public TlbData() {
    }

    public TlbData(int initialCapacity) {
        super();
    }

    @Override
    public Object put(Integer key, Object value) {
        if (value instanceof ByteBuf) {
            addByteBufRef((ByteBuf) value);
        }
        return super.put(key, value);
    }

    @Override
    public void clear() {
        super.values().forEach(v -> {
            if (v instanceof ByteBuf) {
                ((ByteBuf) v).release();
            }
        });
        super.clear();
    }

    private void addByteBufRef(ByteBuf byteBuf) {
        ByteBuf retainedBuf = byteBuf.retainedSlice();
        retainedBuf.retain();
    }
}
