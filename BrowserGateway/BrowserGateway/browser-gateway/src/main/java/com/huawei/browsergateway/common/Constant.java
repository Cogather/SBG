package com.huawei.browsergateway.common;

public class Constant {
    // 插件加载状态
    public static String COMPLETE = "Completed";
    public static String FAILED = "Failed";
    public static String NOTSTART = "NotStart";

    // tcp流的类型 : 媒体流、控制流
    public static String TCP_MEDIA = "TcpMedia";
    public static String TCP_CONTROL = "TcpControl";

    public static final String CONTROL_SERVICE_TYPE = "devicetcp";
    public static final String MEDIA_SERVICE_TYPE = "datadeal";

    public static final int TCP_DECODER_MAX_SIZE = 3145728;

}