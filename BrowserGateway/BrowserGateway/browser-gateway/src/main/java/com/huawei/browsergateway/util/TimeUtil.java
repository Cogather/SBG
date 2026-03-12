package com.huawei.browsergateway.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** 当前时间格式化工具类（GMT 时区） */
public final class TimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId GMT = ZoneId.of("GMT");

    private TimeUtil() {}

    /**
     * 获取当前 GMT 时间字符串
     *
     * @return {@code yyyy-MM-dd HH:mm:ss} 格式的 GMT 时间
     */
    public static String getCurrentDate() {
        return ZonedDateTime.now(GMT).format(FORMATTER);
    }
}
