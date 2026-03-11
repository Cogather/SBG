package com.huawei.browsergateway.util;

import com.huawei.browsergateway.util.UserIdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户ID工具类测试
 * 测试用户ID生成和验证功能
 */
class UserIdUtilTest {

    @Test
    void testGenerateUserIdByImeiAndImsi_正常参数() {
        // Given
        String imei = "123456789012345";
        String imsi = "460001234567890";

        // When
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(imei, imsi);

        // Then
        assertNotNull(userId, "生成的用户ID不应为null");
        assertEquals("123456789012345_460001234567890", userId, "用户ID格式应正确");
    }

    @Test
    void testGenerateUserIdByImeiAndImsi_null参数处理() {
        // When
        String userId1 = UserIdUtil.generateUserIdByImeiAndImsi(null, "460001234567890");
        String userId2 = UserIdUtil.generateUserIdByImeiAndImsi("123456789012345", null);
        String userId3 = UserIdUtil.generateUserIdByImeiAndImsi(null, null);

        // Then
        assertEquals("_460001234567890", userId1, "imei为null时应使用空字符串");
        assertEquals("123456789012345_", userId2, "imsi为null时应使用空字符串");
        assertEquals("_", userId3, "两者都为null时应返回下划线");
    }

    @Test
    void testGenerateUserIdByImeiAndImsi_空字符串参数() {
        // When
        String userId = UserIdUtil.generateUserIdByImeiAndImsi("", "");

        // Then
        assertEquals("_", userId, "空字符串参数应返回下划线");
    }
}
