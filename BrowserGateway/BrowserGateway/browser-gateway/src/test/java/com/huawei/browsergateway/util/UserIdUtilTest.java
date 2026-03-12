package com.huawei.browsergateway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for UserIdUtil */
class UserIdUtilTest {

    @Test
    void testGenerateUserId_normalParams() {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi("123456789012345", "460001234567890");
        assertNotNull(userId);
        assertEquals("123456789012345_460001234567890", userId);
    }

    @Test
    void testGenerateUserId_nullImei() {
        assertEquals("_460001234567890", UserIdUtil.generateUserIdByImeiAndImsi(null, "460001234567890"));
    }

    @Test
    void testGenerateUserId_nullImsi() {
        assertEquals("123456789012345_", UserIdUtil.generateUserIdByImeiAndImsi("123456789012345", null));
    }

    @Test
    void testGenerateUserId_bothNull() {
        assertEquals("_", UserIdUtil.generateUserIdByImeiAndImsi(null, null));
    }

    @Test
    void testGenerateUserId_emptyStrings() {
        assertEquals("_", UserIdUtil.generateUserIdByImeiAndImsi("", ""));
    }
}
