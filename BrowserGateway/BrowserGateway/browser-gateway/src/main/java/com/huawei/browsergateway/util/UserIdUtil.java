package com.huawei.browsergateway.util;

public class UserIdUtil {
    public static String generateUserIdByImeiAndImsi(String imei, String imsi){
        String imeiStr = (imei == null) ? "" : imei;
        String imsiStr = (imsi == null) ? "" : imsi;
        return imeiStr + "_" + imsiStr;
    }
}
