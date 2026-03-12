package com.huawei.browsergateway.util.encode;

import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** TLV 单个字段，包含 Type、Length、Data 三元组 */
@Data
public class TlvField {

    private int type;
    private int len;
    private byte[] data;

    public TlvField() {}

    public TlvField(int type, int len, byte[] data) {
        this.type = type;
        this.len = len;
        this.data = data;
    }

    /**
     * 将 data 字节数组解析为大端序 int 值
     *
     * @return 解析后的 int 值
     */
    public int getInt() {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
