package com.huawei.browsergateway.util.encode;

import lombok.Data;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Data
public class TlvField {
    private int type;
    private int len;
    private byte[] data;

    public TlvField(){}

    public TlvField(int type, int len, byte[] data) {
        this.type = type;
        this.len = len;
        this.data = data;
    }

    public void setType(int type){this.type = type;}
    public void setLen(int len){this.len = len;}
    public void setData(byte[] data){this.data = data;}

    /**
     * 从 data 字节数组中解析 int 值（大端序）
     * @return int 值
     */
    public int getInt(){
        if (data == null || data.length < 4) {
            throw new IllegalArgumentException("data is null or length < 4, cannot get int value");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }
}
