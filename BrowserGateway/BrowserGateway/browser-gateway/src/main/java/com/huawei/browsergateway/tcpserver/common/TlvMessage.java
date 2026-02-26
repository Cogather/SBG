package com.huawei.browsergateway.tcpserver.common;

import lombok.Data;
import java.util.zip.CRC32;

/**
 * TLV消息封装类
 * TLV格式: Type(2字节) + Length(4字节) + Value(N字节) + Checksum(4字节，可选)
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
    
    /**
     * 校验和（4字节，可选）
     */
    private int checksum;
    
    /**
     * 是否启用校验和
     */
    private boolean checksumEnabled = true;
    
    public TlvMessage() {
    }
    
    public TlvMessage(short type, byte[] value) {
        this.type = type;
        this.value = value;
        this.length = value != null ? value.length : 0;
    }
    
    /**
     * 获取完整的TLV消息字节数组
     * 如果启用校验和，格式为: Type(2字节) + Length(4字节) + Value(N字节) + Checksum(4字节)
     * 如果未启用校验和，格式为: Type(2字节) + Length(4字节) + Value(N字节)
     */
    public byte[] toBytes() {
        if (value == null) {
            value = new byte[0];
        }
        length = value.length;
        
        // 计算校验和
        if (checksumEnabled) {
            checksum = calculateChecksum();
        }
        
        int headerSize = 6; // Type(2) + Length(4)
        int checksumSize = checksumEnabled ? 4 : 0;
        byte[] result = new byte[headerSize + length + checksumSize];
        
        int offset = 0;
        
        // 写入Type（2字节，大端序）
        result[offset++] = (byte) ((type >> 8) & 0xFF);
        result[offset++] = (byte) (type & 0xFF);
        
        // 写入Length（4字节，大端序）
        result[offset++] = (byte) ((length >> 24) & 0xFF);
        result[offset++] = (byte) ((length >> 16) & 0xFF);
        result[offset++] = (byte) ((length >> 8) & 0xFF);
        result[offset++] = (byte) (length & 0xFF);
        
        // 写入Value
        System.arraycopy(value, 0, result, offset, length);
        offset += length;
        
        // 写入Checksum（如果启用）
        if (checksumEnabled) {
            result[offset++] = (byte) ((checksum >> 24) & 0xFF);
            result[offset++] = (byte) ((checksum >> 16) & 0xFF);
            result[offset++] = (byte) ((checksum >> 8) & 0xFF);
            result[offset++] = (byte) (checksum & 0xFF);
        }
        
        return result;
    }
    
    /**
     * 计算TLV消息的校验和
     * 使用CRC32算法计算Type + Length + Value的校验和
     * 
     * @return 校验和值
     */
    public int calculateChecksum() {
        CRC32 crc32 = new CRC32();
        
        // 计算Type的CRC32
        crc32.update((byte) ((type >> 8) & 0xFF));
        crc32.update((byte) (type & 0xFF));
        
        // 计算Length的CRC32
        crc32.update((byte) ((length >> 24) & 0xFF));
        crc32.update((byte) ((length >> 16) & 0xFF));
        crc32.update((byte) ((length >> 8) & 0xFF));
        crc32.update((byte) (length & 0xFF));
        
        // 计算Value的CRC32
        if (value != null && value.length > 0) {
            crc32.update(value);
        }
        
        return (int) crc32.getValue();
    }
    
    /**
     * 验证校验和
     * 
     * @return 校验和是否有效
     */
    public boolean verifyChecksum() {
        if (!checksumEnabled) {
            return true; // 未启用校验和，认为验证通过
        }
        
        int calculatedChecksum = calculateChecksum();
        return calculatedChecksum == checksum;
    }
}
