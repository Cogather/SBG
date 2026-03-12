package com.huawei.browsergateway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for TimeUtil */
class TimeUtilTest {

    @Test
    void testGetCurrentDate_formatIsCorrect() {
        String result = TimeUtil.getCurrentDate();
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Expected format yyyy-MM-dd HH:mm:ss, got: " + result);
    }

    @Test
    void testGetCurrentDate_consecutiveCallsAreMonotonic() {
        String t1 = TimeUtil.getCurrentDate();
        String t2 = TimeUtil.getCurrentDate();
        assertNotNull(t1);
        assertNotNull(t2);
        assertTrue(t2.compareTo(t1) >= 0);
    }
}
