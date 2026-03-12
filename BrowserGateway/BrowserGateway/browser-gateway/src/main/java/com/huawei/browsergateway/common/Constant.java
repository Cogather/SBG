package com.huawei.browsergateway.common;

/**
 * 全局通用常量，按业务领域分组定义
 */
public class Constant {

    private Constant() {
        // 工具类，禁止实例化
    }

    // ---- 插件加载状态 ----

    /** 插件加载成功 */
    public static final String COMPLETE = "Completed";

    /** 插件加载失败 */
    public static final String FAILED = "Failed";

    /** 插件尚未启动 */
    public static final String NOTSTART = "NotStart";

    // ---- TCP 流类型 ----

    /** 媒体流 */
    public static final String TCP_MEDIA = "TcpMedia";

    /** 控制流 */
    public static final String TCP_CONTROL = "TcpControl";

    // ---- TCP 服务类型标识 ----

    /** 控制流服务类型 */
    public static final String CONTROL_SERVICE_TYPE = "devicetcp";

    /** 媒体流服务类型 */
    public static final String MEDIA_SERVICE_TYPE = "datadeal";

    // ---- TCP 解码器限制 ----

    /** TCP 解码器单帧最大字节数（3 MB） */
    public static final int TCP_DECODER_MAX_SIZE = 3 * 1024 * 1024;
}
