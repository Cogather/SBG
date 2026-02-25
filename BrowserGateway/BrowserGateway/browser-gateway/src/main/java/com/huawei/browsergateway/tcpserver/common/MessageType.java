package com.huawei.browsergateway.tcpserver.common;

/**
 * TLV消息类型定义
 * 根据API文档定义的消息类型
 */
public class MessageType {
    // 控制流消息类型
    public static final short LOGIN = 0x0001;           // 登录请求
    public static final short HEARTBEATS = 0x0002;     // 心跳包
    public static final short KEY_EVENT = 0x0003;      // 按键事件
    public static final short TOUCH_EVENT = 0x0004;    // 触摸事件
    public static final short MOUSE_EVENT = 0x0005;    // 鼠标事件
    public static final short DRAG_EVENT = 0x0006;     // 拖拽事件
    public static final short TEXT_INPUT = 0x0007;     // 文本输入事件
    public static final short CLIPBOARD = 0x0008;      // 剪贴板操作
    public static final short LOGOUT = 0x00FF;         // 登出/断开连接
    
    // 媒体流消息类型
    public static final short VIDEO_FRAME = 0x000A;    // 视频帧数据
    public static final short AUDIO_FRAME = 0x000B;    // 音频帧数据
    public static final short MEDIA_CONFIG = 0x000C;   // 媒体配置参数
    
    // 自定义消息类型（0xFF00+）
    public static final short CUSTOM_START = (short) 0xFF00;   // 自定义消息起始值
    
    /**
     * 判断是否为控制流消息类型
     */
    public static boolean isControlMessage(short type) {
        return type == LOGIN || type == HEARTBEATS || 
               type == KEY_EVENT || type == TOUCH_EVENT || 
               type == MOUSE_EVENT || type == DRAG_EVENT || 
               type == TEXT_INPUT || type == CLIPBOARD || 
               type == LOGOUT;
    }
    
    /**
     * 判断是否为媒体流消息类型
     */
    public static boolean isMediaMessage(short type) {
        return type == VIDEO_FRAME || type == AUDIO_FRAME || type == MEDIA_CONFIG;
    }
    
    /**
     * 判断是否为自定义消息类型
     */
    public static boolean isCustomMessage(short type) {
        return type >= CUSTOM_START;
    }
    
    /**
     * 获取消息类型名称
     */
    public static String getTypeName(short type) {
        switch (type) {
            case LOGIN: return "LOGIN";
            case HEARTBEATS: return "HEARTBEATS";
            case KEY_EVENT: return "KEY_EVENT";
            case TOUCH_EVENT: return "TOUCH_EVENT";
            case MOUSE_EVENT: return "MOUSE_EVENT";
            case DRAG_EVENT: return "DRAG_EVENT";
            case TEXT_INPUT: return "TEXT_INPUT";
            case CLIPBOARD: return "CLIPBOARD";
            case LOGOUT: return "LOGOUT";
            case VIDEO_FRAME: return "VIDEO_FRAME";
            case AUDIO_FRAME: return "AUDIO_FRAME";
            case MEDIA_CONFIG: return "MEDIA_CONFIG";
            default:
                if (isCustomMessage(type)) {
                    return "CUSTOM_" + Integer.toHexString(type);
                }
                return "UNKNOWN_" + Integer.toHexString(type);
        }
    }
}
