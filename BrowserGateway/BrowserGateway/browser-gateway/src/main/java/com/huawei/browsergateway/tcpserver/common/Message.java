package com.huawei.browsergateway.tcpserver.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * TLV消息对象
 * 用于TLV协议的编解码
 */
@Data
public class Message {
    /**
     * 消息类型
     */
    private short type;
    
    /**
     * IMEI
     */
    private String imei;
    
    /**
     * IMSI
     */
    private String imsi;
    
    /**
     * 屏幕宽度
     */
    private Integer lcdWidth;
    
    /**
     * 屏幕高度
     */
    private Integer lcdHeight;
    
    /**
     * 应用类型
     */
    private String appType;
    
    /**
     * 认证类型（预开浏览器时为空字符串）
     */
    private String audType;
    
    /**
     * 登录凭证（预开浏览器时为空字符串）
     */
    private String token;
    
    /**
     * 会话ID
     */
    private String sessionID;
    
    /**
     * 其他字段（用于扩展）
     */
    private Map<String, Object> fields;
    
    public Message() {
        this.fields = new HashMap<>();
    }
    
    /**
     * 设置字段值
     */
    public void setField(String key, Object value) {
        if (fields == null) {
            fields = new HashMap<>();
        }
        fields.put(key, value);
    }
    
    /**
     * 获取字段值
     */
    public Object getField(String key) {
        return fields != null ? fields.get(key) : null;
    }
}
