package com.huawei.browsergateway.util;

/** 用户 ID 生成工具类 */
public final class UserIdUtil {

    private UserIdUtil() {}

    /**
     * 根据 IMEI 和 IMSI 生成用户 ID，格式为 {@code imei_imsi}
     *
     * @param imei 设备 IMEI，null 时视为空字符串
     * @param imsi 设备 IMSI，null 时视为空字符串
     * @return 格式为 {@code imei_imsi} 的用户 ID
     */
    public static String generateUserIdByImeiAndImsi(String imei, String imsi) {
        return (imei != null ? imei : "") + "_" + (imsi != null ? imsi : "");
    }
}
