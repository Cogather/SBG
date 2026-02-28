package com.huawei.browsergateway.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter TARGET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId TARGET_ZONE = ZoneId.of("GMT");

    public static String getCurrentDate() {
        ZonedDateTime currentTime = ZonedDateTime.now(TARGET_ZONE);
        return currentTime.format(TARGET_FORMATTER);
    }
}
