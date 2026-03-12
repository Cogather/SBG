package com.huawei.browsergateway.tcpserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowRateTrackerTest {

    private FlowRateTracker flowRateTracker;

    @BeforeEach
    void setUp() {
        flowRateTracker = new FlowRateTracker();
    }

    @Test
    void testAddDataSize() {
        flowRateTracker.add("session1", "control", 100);
        flowRateTracker.add("session1", "control", 200);

        long result = flowRateTracker.flowRateStat("session1", "control");

        assertEquals(300, result);
    }

    @Test
    void testAddDataSizeForDifferentSessions() {
        flowRateTracker.add("session1", "control", 100);
        flowRateTracker.add("session2", "control", 200);

        long result1 = flowRateTracker.flowRateStat("session1", "control");
        long result2 = flowRateTracker.flowRateStat("session2", "control");

        assertEquals(100, result1);
        assertEquals(200, result2);
    }

    @Test
    void testAddDataSizeForDifferentServiceTypes() {
        flowRateTracker.add("session1", "control", 100);
        flowRateTracker.add("session1", "media", 200);

        long controlResult = flowRateTracker.flowRateStat("session1", "control");
        long mediaResult = flowRateTracker.flowRateStat("session1", "media");

        assertEquals(100, controlResult);
        assertEquals(200, mediaResult);
    }

    @Test
    void testFlowRateStatRemovesEntry() {
        flowRateTracker.add("session1", "control", 100);

        long firstResult = flowRateTracker.flowRateStat("session1", "control");
        long secondResult = flowRateTracker.flowRateStat("session1", "control");

        assertEquals(100, firstResult);
        assertEquals(0, secondResult);
    }

    @Test
    void testFlowRateStatForNonExistentKey() {
        long result = flowRateTracker.flowRateStat("nonExistent", "control");

        assertEquals(0, result);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                flowRateTracker.add("session1", "control", 1);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                flowRateTracker.add("session1", "control", 1);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        long result = flowRateTracker.flowRateStat("session1", "control");

        assertEquals(2000, result);
    }
}
