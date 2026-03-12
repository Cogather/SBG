package com.huawei.browsergateway.common;

/**
 * TLV 协议字段标识（Tag ID）常量，与 {@link com.huawei.browsergateway.util.encode.TlvTag}
 * 注解的 {@code id} 属性一一对应。
 *
 * @see com.huawei.browsergateway.util.encode.TlvTag
 * @see com.huawei.browsergateway.util.encode.TlvCodec
 * @see com.huawei.browsergateway.util.encode.Tlv
 */
public class ID {

    private ID() {
        // 工具类，禁止实例化
    }

    // ---- TLV 字段标识 ----

    /** 消息类型 */
    public static final int TYPE = 1;
    /** 厂商 */
    public static final int FACTORY = 2;
    /** 机型 */
    public static final int DEV_TYPE = 3;
    /** IMSI 值 */
    public static final int IMSI = 4;
    /** IMEI 值 */
    public static final int IMEI = 5;
    /** 设备屏幕宽度 */
    public static final int LCD_WIDTH = 6;
    /** 设备屏幕高度 */
    public static final int LCD_HEIGHT = 7;
    /** 音频类型 */
    public static final int AUD_TYPE = 8;
    /** 服务端通用应答值 */
    public static final int ACK_TYPE = 9;
    /** 返回状态码 */
    public static final int CODE = 10;
    /** 事件消息 */
    public static final int EVENT = 11;
    /** 控制类型 */
    public static final int CTRL_TYPE = 12;
    /** 控制值 */
    public static final int CTRL_VAL = 13;
    /** 时间序列 */
    public static final int SEQ = 14;
    /** 音频流数据 */
    public static final int AUDIO_DATA = 15;
    /** 视频流数据 */
    public static final int VIDEO_DATA = 16;
    /** 音频采样率 */
    public static final int AUD_SMPRATE = 17;
    /** 音频通道数 */
    public static final int AUD_CHANNEL = 18;
    /** 应用类型 */
    public static final int APP_TYPE = 19;
    /** 流媒体地址 */
    public static final int TCP_ADDR = 20;
    /** 用户 HTTP 登录 token */
    public static final int TOKEN = 21;
    /** 浏览器 sessionId */
    public static final int SESSION_ID = 22;
    /** 帧类型 */
    public static final int FRAME_TYPE = 23;
    /** 服务端控制响应元素 */
    public static final int CTRL_RSP_ELM = 24;
    /** 服务端控制响应信息反馈 */
    public static final int CTRL_RSP_INFO = 25;
    /** 传输内容 */
    public static final int CONTENT = 26;
    /** 设备端信息上传类型 */
    public static final int UPLOAD_TYPE = 27;
    /** 应用 appId */
    public static final int APP_ID = 28;
    /** 平台类型 */
    public static final int PLAT_TYPE = 29;
    /** 扩展机型 */
    public static final int EXT_TYPE = 30;
    /** 视频地址 */
    public static final int VIDEO_ADDR = 31;
    /** 视频类型 */
    public static final int VIDEO_MODEL = 32;
    /** 播放器状态 */
    public static final int PLAYER_STATUS = 33;
    /** 文件传输类型 */
    public static final int UPLOAD_FILE_TYPE = 34;
    /** 文件传输反馈 */
    public static final int UPLOAD_FILE_RESULT = 35;
    /** 文件地址 */
    public static final int FILE_ADDR = 36;
    /** 播放模式 */
    public static final int PLAY_MODE = 37;
    /** JPG 流数据 */
    public static final int JPG_DATA = 38;
    /** 位置信息 */
    public static final int LOCATION_DATA = 39;
    /** 代理地址 */
    public static final int SOCKS5_ADDR = 40;
    /** 代理通道 */
    public static final int SOCKS5_TUNNEL = 41;
    /** 能力值 */
    public static final int ABILITY = 42;
    /** 回调事件类型 */
    public static final int COMMAND = 43;
    /** 状态 */
    public static final int STATUS = 44;
    /** 客户端语言 */
    public static final int CLIENT_LANGUAGE = 45;
    /** 设备类型细分 */
    public static final int DEVICE_TYPE = 46;
    /** 输入框类型 */
    public static final int WRITE_TYPE = 47;
    /** 网络类型 */
    public static final int NETWORK_TYPE = 48;
    /** 页面地址类型 */
    public static final int URL_TYPE = 49;
}
