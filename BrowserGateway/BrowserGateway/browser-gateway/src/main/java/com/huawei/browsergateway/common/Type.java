package com.huawei.browsergateway.common;

/**
 * TCP 消息类型常量类
 * <p>
 * 定义 TCP 控制通道和媒体通道中传输的消息类型标识，
 * 对应 TLV 协议中 {@link ID#TYPE}（id=1）字段的值。
 * </p>
 *
 * @see com.huawei.browsergateway.util.encode.Ack
 * @see com.huawei.browsergateway.util.encode.LoginResponse
 */
public class Type {

    private Type() {
        // 工具类，禁止实例化
    }

    // ======================== 连接管理类型 ========================

    /** 登录消息 */
    public static final int LOGIN = 1;

    /** 心跳消息 */
    public static final int HEARTBEATS = 2;

    // ======================== 控制与事件类型 ========================

    /** 控制消息 */
    public static final int CONTROL = 4;

    /** 通用应答消息 */
    public static final int ACK = 7;

    /** 通用消息 */
    public static final int MESSAGE = 13;

    // ======================== 媒体流类型 ========================

    /** 音频流消息 */
    public static final int AUDIO = 5;

    /** 视频流消息 */
    public static final int VIDEO = 6;

    // ======================== 服务端响应类型 ========================

    /** 返回媒体通道地址 */
    public static final int RETURN_MEDIA = 9;

    /** 返回控制通道地址 */
    public static final int RETURN_CONTROL = 12;

    // ======================== 文件与网络类型 ========================

    /** 文件上传消息 */
    public static final int UPLOAD_FILE = 16;

    /** 网络类型消息 */
    public static final int NETWORK_TYPE = 48;

    // ======================== 工具方法 ========================

    /**
     * 生成 TCP 会话绑定的唯一 Key
     * <p>
     * 通过 IMEI 和 IMSI 组合生成，用于在控制通道和媒体通道中
     * 唯一标识一个客户端连接。
     * </p>
     *
     * @param imei 设备 IMEI
     * @param imsi 用户 IMSI
     * @return 格式为 "{imei}_{imsi}" 的绑定 Key
     */
    public static String tcpBindKey(String imei, String imsi) {
        return String.format("%s_%s", imei, imsi);
    }
}
