package com.huawei.browsergateway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for DateTimeUtil */
class DateTimeUtilTest {

    @Test
    void testMillisToDate_normalTimestamp() {
        long ts = 1704067200000L;
        String result = DateTimeUtil.millisToDate(ts);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Expected format yyyy-MM-dd HH:mm:ss, got: " + result);
    }

    @Test
    void testMillisToDate_zeroTimestamp() {
        String result = DateTimeUtil.millisToDate(0L);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testMillisToDate_negativeTimestamp() {
        String result = DateTimeUtil.millisToDate(-1000L);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testMillisToDate_threadSafety() throws InterruptedException {
        long ts = 1704067200000L;
        Thread[] threads = new Thread[10];
        String[] results = new String[10];
        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[idx] = new Thread(() -> results[idx] = DateTimeUtil.millisToDate(ts));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        for (String r : results) {
            assertNotNull(r);
            assertEquals(results[0], r, "Concurrent results should be consistent");
        }
    }
}
