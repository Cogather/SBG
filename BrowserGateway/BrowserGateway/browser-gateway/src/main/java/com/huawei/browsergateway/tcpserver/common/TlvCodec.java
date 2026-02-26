package com.huawei.browsergateway.tcpserver.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * TLV (Type-Length-Value) 协议编解码器
 * 负责Message对象与TLV格式之间的转换
 * 
 * 注意：
 * - 当前实现使用JSON格式序列化消息体（与存量代码可能不同）
 * - 存量代码可能使用二进制TLV格式
 * - 可通过配置项 browsergw.tlv.format 选择格式（json|binary）
 * - 默认使用JSON格式，如需与存量代码兼容，请切换到binary格式
 */
public class TlvCodec {
    
    private static final Logger log = LoggerFactory.getLogger(TlvCodec.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * TLV消息体格式类型
     */
    public enum TlvFormat {
        /** JSON格式（当前默认） */
        JSON,
        /** 二进制格式（与存量代码兼容） */
        BINARY
    }
    
    /**
     * 当前使用的格式（可通过配置修改）
     * 默认使用JSON格式，如需与存量代码兼容，请设置为BINARY
     */
    private static TlvFormat currentFormat = TlvFormat.JSON;
    
    /**
     * 设置TLV格式
     * 
     * @param format 格式类型
     */
    public static void setFormat(TlvFormat format) {
        if (format != null) {
            currentFormat = format;
            log.info("TLV格式已设置为: {}", format);
        }
    }
    
    /**
     * 获取当前TLV格式
     * 
     * @return 格式类型
     */
    public static TlvFormat getFormat() {
        return currentFormat;
    }
    
    /**
     * 将Message对象编组为TlvMessage
     * 
     * @param message 消息对象
     * @return TLV消息对象
     */
    public static TlvMessage marshal(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("消息对象不能为null");
        }
        
        try {
            // 编组消息体为JSON字节数组
            byte[] value = marshalMessageBody(message);
            
            // 创建TlvMessage对象
            TlvMessage tlvMessage = new TlvMessage(message.getType(), value);
            tlvMessage.setChecksumEnabled(true); // 默认启用校验和
            
            log.debug("编组TLV消息: type={}, length={}", message.getType(), value.length);
            return tlvMessage;
            
        } catch (Exception e) {
            log.error("编组TLV消息失败", e);
            throw new RuntimeException("编组TLV消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将TlvMessage解码为Message对象
     * 
     * @param tlvMessage TLV消息对象
     * @return 消息对象
     */
    public static Message unmarshal(TlvMessage tlvMessage) {
        if (tlvMessage == null) {
            throw new IllegalArgumentException("TLV消息对象不能为null");
        }
        
        try {
            Message message = new Message();
            message.setType(tlvMessage.getType());
            
            // 解析消息体
            if (tlvMessage.getValue() != null && tlvMessage.getValue().length > 0) {
                Map<String, Object> fields = parseMessageBody(tlvMessage.getValue());
                
                // 设置标准字段
                if (fields.containsKey("imei")) {
                    message.setImei(String.valueOf(fields.get("imei")));
                }
                if (fields.containsKey("imsi")) {
                    message.setImsi(String.valueOf(fields.get("imsi")));
                }
                if (fields.containsKey("lcdWidth")) {
                    message.setLcdWidth(getIntegerValue(fields.get("lcdWidth")));
                }
                if (fields.containsKey("lcdHeight")) {
                    message.setLcdHeight(getIntegerValue(fields.get("lcdHeight")));
                }
                if (fields.containsKey("appType")) {
                    message.setAppType(String.valueOf(fields.get("appType")));
                }
                if (fields.containsKey("audType")) {
                    message.setAudType(String.valueOf(fields.get("audType")));
                }
                if (fields.containsKey("token")) {
                    message.setToken(String.valueOf(fields.get("token")));
                }
                if (fields.containsKey("sessionID")) {
                    message.setSessionID(String.valueOf(fields.get("sessionID")));
                }
                
                // 设置其他字段
                message.setFields(fields);
            }
            
            log.debug("解码TLV消息: type={}, length={}", tlvMessage.getType(), tlvMessage.getLength());
            return message;
            
        } catch (Exception e) {
            log.error("解码TLV消息失败", e);
            throw new RuntimeException("解码TLV消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将Message对象编组为字节数组（使用TlvMessage格式）
     * 
     * @param message 消息对象
     * @return 字节数组
     */
    public static byte[] marshalToBytes(Message message) {
        TlvMessage tlvMessage = marshal(message);
        return tlvMessage.toBytes();
    }
    
    /**
     * 将Message对象编组为字节数组（支持字节序）
     * 
     * @param message 消息对象
     * @param byteOrder 字节序（大端序或小端序）
     * @return 字节数组
     */
    public static byte[] marshalWithByteOrder(Message message, ByteOrder byteOrder) {
        if (message == null) {
            throw new IllegalArgumentException("消息对象不能为null");
        }
        
        try {
            // 编组消息体
            byte[] value = marshalMessageBody(message);
            
            // 创建ByteBuffer，使用指定的字节序
            ByteBuffer buffer = ByteBuffer.allocate(6 + value.length);
            buffer.order(byteOrder);
            
            // 写入Type（2字节）
            buffer.putShort(message.getType());
            
            // 写入Length（4字节）
            buffer.putInt(value.length);
            
            // 写入Value
            buffer.put(value);
            
            log.debug("编组TLV消息（字节序={}）: type={}, length={}", byteOrder, message.getType(), value.length);
            return buffer.array();
            
        } catch (Exception e) {
            log.error("编组TLV消息失败（字节序={}）", byteOrder, e);
            throw new RuntimeException("编组TLV消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 编组消息体
     * 根据配置的格式（JSON或二进制）序列化消息体
     * 
     * @param message 消息对象
     * @return 消息体字节数组
     */
    private static byte[] marshalMessageBody(Message message) throws IOException {
        if (currentFormat == TlvFormat.BINARY) {
            return marshalMessageBodyBinary(message);
        } else {
            return marshalMessageBodyJson(message);
        }
    }
    
    /**
     * 编组消息体（JSON格式）
     * 将Message对象的字段序列化为JSON格式的字节数组
     * 
     * @param message 消息对象
     * @return 消息体字节数组
     */
    private static byte[] marshalMessageBodyJson(Message message) throws IOException {
        // 构建JSON对象
        Map<String, Object> jsonMap = new java.util.HashMap<>();
        
        // 添加标准字段
        if (message.getImei() != null) {
            jsonMap.put("imei", message.getImei());
        }
        if (message.getImsi() != null) {
            jsonMap.put("imsi", message.getImsi());
        }
        if (message.getLcdWidth() != null) {
            jsonMap.put("lcdWidth", message.getLcdWidth());
        }
        if (message.getLcdHeight() != null) {
            jsonMap.put("lcdHeight", message.getLcdHeight());
        }
        if (message.getAppType() != null) {
            jsonMap.put("appType", message.getAppType());
        }
        if (message.getAudType() != null) {
            jsonMap.put("audType", message.getAudType());
        }
        if (message.getToken() != null) {
            jsonMap.put("token", message.getToken());
        }
        if (message.getSessionID() != null) {
            jsonMap.put("sessionID", message.getSessionID());
        }
        
        // 添加其他字段
        if (message.getFields() != null) {
            for (Map.Entry<String, Object> entry : message.getFields().entrySet()) {
                // 避免覆盖标准字段
                if (!jsonMap.containsKey(entry.getKey())) {
                    jsonMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // 序列化为JSON字节数组
        return objectMapper.writeValueAsBytes(jsonMap);
    }
    
    /**
     * 编组消息体（二进制格式）
     * 将Message对象的字段序列化为二进制TLV格式
     * 注意：此方法为二进制格式的占位实现，实际使用时需要根据存量代码的格式规范实现
     * 
     * @param message 消息对象
     * @return 消息体字节数组
     */
    private static byte[] marshalMessageBodyBinary(Message message) throws IOException {
        // TODO: 实现二进制TLV格式编组
        // 需要根据存量代码的格式规范实现：
        // 1. String类型：UTF-8编码 + 长度前缀（4字节）
        // 2. Integer类型：4字节整数
        // 3. Long类型：8字节整数
        // 4. Byte array：直接写入
        
        log.warn("二进制TLV格式编组尚未实现，当前回退到JSON格式");
        // 暂时回退到JSON格式
        return marshalMessageBodyJson(message);
    }
    
    /**
     * 解析消息体
     * 根据配置的格式（JSON或二进制）解析消息体
     * 
     * @param value 消息体字节数组
     * @return 字段映射
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMessageBody(byte[] value) throws IOException {
        if (value == null || value.length == 0) {
            return new java.util.HashMap<>();
        }
        
        if (currentFormat == TlvFormat.BINARY) {
            return parseMessageBodyBinary(value);
        } else {
            return parseMessageBodyJson(value);
        }
    }
    
    /**
     * 解析消息体（JSON格式）
     * 将字节数组解析为字段映射
     * 
     * @param value 消息体字节数组
     * @return 字段映射
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMessageBodyJson(byte[] value) throws IOException {
        // 将字节数组解析为JSON对象
        return objectMapper.readValue(value, Map.class);
    }
    
    /**
     * 解析消息体（二进制格式）
     * 将二进制TLV格式的字节数组解析为字段映射
     * 注意：此方法为二进制格式的占位实现，实际使用时需要根据存量代码的格式规范实现
     * 
     * @param value 消息体字节数组
     * @return 字段映射
     */
    private static Map<String, Object> parseMessageBodyBinary(byte[] value) throws IOException {
        // TODO: 实现二进制TLV格式解析
        // 需要根据存量代码的格式规范实现：
        // 1. 读取字段类型
        // 2. 读取字段长度
        // 3. 读取字段值
        // 4. 根据类型解析值（String、Integer、Long等）
        
        log.warn("二进制TLV格式解析尚未实现，当前回退到JSON格式");
        // 暂时回退到JSON格式
        return parseMessageBodyJson(value);
    }
    
    /**
     * 获取整数值
     * 支持Integer、Long、String等类型转换
     */
    private static Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法将字符串转换为整数: {}", value);
                return null;
            }
        }
        
        return null;
    }
}
