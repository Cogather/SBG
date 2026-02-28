package com.huawei.browsergateway.common;

public class Type {
    public static final int LOGIN = 1;
    public static final int HEARTBEATS = 2;
    public static final int CONTROL = 4;
    public static final int AUDIO = 5;
    public static final int VIDEO = 6;
    public static final int ACK = 7;
    public static final int RETURN_MEDIA = 9;
    public static final int RETURN_CONTROL = 12;
    public static final int MESSAGE = 13;
    public static final int UPLOAD_FILE = 16;
    public static final int NETWORK_TYPE = 48;

    public static String tcpBindKey(String imei, String imsi) {
        return String.format("%s_%s", imei, imsi);
    }
}