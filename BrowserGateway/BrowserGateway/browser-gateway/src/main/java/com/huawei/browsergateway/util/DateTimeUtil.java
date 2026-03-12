package com.huawei.browsergateway.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** 时间戳格式化工具类 */
public final class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private DateTimeUtil() {}

    /**
     * 将毫秒时间戳转换为 {@code yyyy-MM-dd HH:mm:ss} 格式字符串（UTC）
     *
     * @param timestamp 毫秒时间戳
     * @return 格式化后的时间字符串
     */
    public static String millisToDate(long timestamp) {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
