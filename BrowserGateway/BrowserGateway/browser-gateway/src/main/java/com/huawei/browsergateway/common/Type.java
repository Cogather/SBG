package com.huawei.browsergateway.common;

/**
 * TCP 消息类型常量，对应 TLV 协议中 {@link ID#TYPE}（id=1）字段的取值。
 *
 * @see com.huawei.browsergateway.util.encode.Ack
 * @see com.huawei.browsergateway.util.encode.LoginResponse
 */
public class Type {

    private Type() {
        // 工具类，禁止实例化
    }

    // ---- 消息类型 ----

    /** 登录 */
    public static final int LOGIN = 1;

    /** 心跳 */
    public static final int HEARTBEATS = 2;

    /** 控制指令 */
    public static final int CONTROL = 4;

    /** 音频流 */
    public static final int AUDIO = 5;

    /** 视频流 */
    public static final int VIDEO = 6;

    /** 通用应答 */
    public static final int ACK = 7;

    /** 媒体流回传 */
    public static final int RETURN_MEDIA = 9;

    /** 控制流回传 */
    public static final int RETURN_CONTROL = 12;

    /** 消息 */
    public static final int MESSAGE = 13;

    /** 文件上传 */
    public static final int UPLOAD_FILE = 16;

    /** 网络类型 */
    public static final int NETWORK_TYPE = 48;

    /**
     * 生成 TCP 连接绑定键，格式为 {@code imei_imsi}
     */
    public static String tcpBindKey(String imei, String imsi) {
        return String.format("%s_%s", imei, imsi);
    }
}
