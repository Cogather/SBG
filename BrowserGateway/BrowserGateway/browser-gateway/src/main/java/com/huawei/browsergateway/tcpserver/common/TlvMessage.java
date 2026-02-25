package com.huawei.browsergateway.tcpserver.common;

import lombok.Data;

/**
 * TLV消息封装类
 * TLV格式: Type(2字节) + Length(4字节) + Value(N字节)
 */
@Data
public class TlvMessage {
    /**
     * 消息类型（2字节）
     */
    private short type;
    
    /**
     * 消息内容长度（4字节）
     */
    private int length;
    
    /**
     * 消息内容（N字节）
     */
    private byte[] value;
    
    public TlvMessage() {
    }
    
    public TlvMessage(short type, byte[] value) {
        this.type = type;
        this.value = value;
        this.length = value != null ? value.length : 0;
    }
    
    /**
     * 获取完整的TLV消息字节数组
     */
    public byte[] toBytes() {
        if (value == null) {
            value = new byte[0];
        }
        length = value.length;
        
        byte[] result = new byte[6 + length];
        result[0] = (byte) ((type >> 8) & 0xFF);
        result[1] = (byte) (type & 0xFF);
        result[2] = (byte) ((length >> 24) & 0xFF);
        result[3] = (byte) ((length >> 16) & 0xFF);
        result[4] = (byte) ((length >> 8) & 0xFF);
        result[5] = (byte) (length & 0xFF);
        System.arraycopy(value, 0, result, 6, length);
        return result;
    }
}
