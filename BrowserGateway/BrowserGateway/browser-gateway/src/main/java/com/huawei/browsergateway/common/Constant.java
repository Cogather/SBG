package com.huawei.browsergateway.common;

/**
 * 全局通用常量定义类
 * <p>
 * 集中管理项目中使用的各类常量，按业务领域分组。
 * </p>
 */
public class Constant {

    private Constant() {
        // 工具类，禁止实例化
    }

    // ======================== 插件加载状态 ========================

    /** 插件加载完成 */
    public static final String COMPLETE = "Completed";

    /** 插件加载失败 */
    public static final String FAILED = "Failed";

    /** 插件未开始加载 */
    public static final String NOTSTART = "NotStart";

    // ======================== TCP 流类型 ========================

    /** TCP 媒体流 */
    public static final String TCP_MEDIA = "TcpMedia";

    /** TCP 控制流 */
    public static final String TCP_CONTROL = "TcpControl";

    // ======================== 服务类型标识 ========================

    /** 控制通道服务类型 */
    public static final String CONTROL_SERVICE_TYPE = "devicetcp";

    /** 媒体通道服务类型 */
    public static final String MEDIA_SERVICE_TYPE = "datadeal";

    // ======================== TCP 协议配置 ========================

    /** TLV 解码器最大帧大小（3MB = 3 * 1024 * 1024） */
    public static final int TCP_DECODER_MAX_SIZE = 3145728;
}
