package com.huawei.browsergateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpusedMediaAccumulatorTest {

    private TpusedMediaAccumulator accumulator;

    @BeforeEach
    void setUp() {
        accumulator = new TpusedMediaAccumulator();
    }

    @Test
    void addPayloadBytes_positive_sums() {
        accumulator.addPayloadBytes(10);
        accumulator.addPayloadBytes(5);
        assertEquals(15, accumulator.getCurrentBytes());
    }

    @Test
    void addPayloadBytes_nonPositive_ignored() {
        accumulator.addPayloadBytes(100);
        accumulator.addPayloadBytes(0);
        accumulator.addPayloadBytes(-1);
        assertEquals(100, accumulator.getCurrentBytes());
    }

    @Test
    void subtractReported_removesSnapshot() {
        accumulator.addPayloadBytes(100);
        accumulator.subtractReported(40);
        assertEquals(60, accumulator.getCurrentBytes());
    }

    @Test
    void subtractReported_nonPositive_ignored() {
        accumulator.addPayloadBytes(10);
        accumulator.subtractReported(0);
        accumulator.subtractReported(-5);
        assertEquals(10, accumulator.getCurrentBytes());
    }
}
