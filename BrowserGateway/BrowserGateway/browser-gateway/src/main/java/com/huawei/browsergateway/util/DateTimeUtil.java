package com.huawei.browsergateway.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {
    static SimpleDateFormat dateMillisLevel = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 将时间戳（毫秒）转换为 yyyy-MM-dd HH:mm:ss.SSS 格式的时间字符串
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串
     * @throws IllegalArgumentException 如果时间戳无效
     */
    public static String millisToDate(long timestamp) throws IllegalArgumentException {
        try {
            // 将时间戳转换为 Date 对象
            Date date = new Date(timestamp);
            // 将 Date 对象格式化为字符串
            return dateMillisLevel.format(date);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，抛出异常
            throw new IllegalArgumentException("无法解析时间戳: " + timestamp, e);
        }
    }
}
