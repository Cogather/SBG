package com.huawei.browsergateway.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserIdUtil单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserIdUtil决策表测试")
class UserIdUtilTest {

    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "123456789012345";

    /**
     * 决策表：生成用户ID测试
     * 
     * 条件：
     * C1: IMEI是否为空
     * C2: IMSI是否为空
     * C3: IMEI是否包含空格
     * C4: IMSI是否包含空格
     * 
     * 动作：
     * A1: 是否抛出异常
     * A2: 生成的用户ID格式是否正确（32位MD5）
     * A3: 相同输入是否生成相同ID
     */
    @ParameterizedTest(name = "生成用户ID决策表 - 规则{0}: imei={1}, imsi={2}, shouldThrow={3}")
    @MethodSource("generateUserIdDecisionTable")
    @DisplayName("生成用户ID决策表测试")
    void testGenerateUserIdDecisionTable(
            String ruleId,
            String imei,
            String imsi,
            boolean shouldThrowException,
            boolean shouldBeValid) {
        
        if (shouldThrowException) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                UserIdUtil.generateUserId(imei, imsi);
            });
            assertTrue(exception.getMessage().contains("IMEI和IMSI不能同时为空"));
        } else {
            String userId = UserIdUtil.generateUserId(imei, imsi);
            assertNotNull(userId);
            
            if (shouldBeValid) {
                assertTrue(UserIdUtil.isValidUserId(userId), 
                        "生成的用户ID格式不正确: " + userId);
                assertEquals(32, userId.length(), "用户ID长度应该为32位");
            }
        }
    }

    /**
     * 决策表数据源：生成用户ID
     */
    static Stream<Arguments> generateUserIdDecisionTable() {
        return Stream.of(
            // R1: IMEI和IMSI都为空 -> 抛出异常
            Arguments.of("R1", "", "", true, false),
            
            // R2: IMEI和IMSI都为null -> 抛出异常
            Arguments.of("R2", null, null, true, false),
            
            // R3: IMEI为空，IMSI有效 -> 成功生成
            Arguments.of("R3", "", TEST_IMSI, false, true),
            
            // R4: IMEI为null，IMSI有效 -> 成功生成
            Arguments.of("R4", null, TEST_IMSI, false, true),
            
            // R5: IMEI有效，IMSI为空 -> 成功生成
            Arguments.of("R5", TEST_IMEI, "", false, true),
            
            // R6: IMEI有效，IMSI为null -> 成功生成
            Arguments.of("R6", TEST_IMEI, null, false, true),
            
            // R7: IMEI和IMSI都有效 -> 成功生成
            Arguments.of("R7", TEST_IMEI, TEST_IMSI, false, true),
            
            // R8: IMEI包含前导空格 -> 成功生成（自动trim）
            Arguments.of("R8", " " + TEST_IMEI, TEST_IMSI, false, true),
            
            // R9: IMEI包含尾随空格 -> 成功生成（自动trim）
            Arguments.of("R9", TEST_IMEI + " ", TEST_IMSI, false, true),
            
            // R10: IMSI包含空格 -> 成功生成（自动trim）
            Arguments.of("R10", TEST_IMEI, " " + TEST_IMSI + " ", false, true)
        );
    }

    /**
     * 决策表：验证用户ID格式测试
     * 
     * 条件：
     * C1: 用户ID是否为null
     * C2: 用户ID是否为空字符串
     * C3: 用户ID长度是否为32位
     * C4: 用户ID是否只包含十六进制字符
     * 
     * 动作：
     * A1: 验证结果
     */
    @ParameterizedTest(name = "验证用户ID格式决策表 - 规则{0}: userId={1}, expected={2}")
    @MethodSource("validateUserIdDecisionTable")
    @DisplayName("验证用户ID格式决策表测试")
    void testIsValidUserIdDecisionTable(
            String ruleId,
            String userId,
            boolean expectedResult) {
        
        boolean result = UserIdUtil.isValidUserId(userId);
        assertEquals(expectedResult, result, 
                String.format("用户ID验证失败: %s", userId));
    }

    /**
     * 决策表数据源：验证用户ID格式
     */
    static Stream<Arguments> validateUserIdDecisionTable() {
        // 生成一个有效的MD5哈希值作为测试数据
        String validUserId = UserIdUtil.generateUserId(TEST_IMEI, TEST_IMSI);
        
        return Stream.of(
            // R1: null -> false
            Arguments.of("R1", null, false),
            
            // R2: 空字符串 -> false
            Arguments.of("R2", "", false),
            
            // R3: 空白字符串 -> false
            Arguments.of("R3", "   ", false),
            
            // R4: 有效用户ID -> true
            Arguments.of("R4", validUserId, true),
            
            // R5: 长度不足32位 -> false
            Arguments.of("R5", "123456789012345678901234567890", false),
            
            // R6: 长度超过32位 -> false
            Arguments.of("R6", validUserId + "0", false),
            
            // R7: 包含非十六进制字符 -> false
            Arguments.of("R7", "12345678901234567890123456789g", false),
            
            // R8: 包含大写字母 -> false（MD5是小写）
            Arguments.of("R8", "123456789012345678901234567890A", false),
            
            // R9: 包含特殊字符 -> false
            Arguments.of("R9", "12345678901234567890123456789-", false),
            
            // R10: 全为数字但长度正确 -> true（如果格式正确）
            Arguments.of("R10", "12345678901234567890123456789012", true)
        );
    }

    /**
     * 测试相同输入生成相同ID（幂等性）
     */
    @Test
    @DisplayName("用户ID生成幂等性测试")
    void testGenerateUserIdIdempotency() {
        String userId1 = UserIdUtil.generateUserId(TEST_IMEI, TEST_IMSI);
        String userId2 = UserIdUtil.generateUserId(TEST_IMEI, TEST_IMSI);
        
        assertEquals(userId1, userId2, "相同输入应该生成相同的用户ID");
    }

    /**
     * 测试不同输入生成不同ID
     */
    @Test
    @DisplayName("不同输入生成不同ID")
    void testGenerateUserIdUniqueness() {
        String userId1 = UserIdUtil.generateUserId(TEST_IMEI, TEST_IMSI);
        String userId2 = UserIdUtil.generateUserId(TEST_IMEI, "different_imsi");
        String userId3 = UserIdUtil.generateUserId("different_imei", TEST_IMSI);
        
        assertNotEquals(userId1, userId2, "不同IMSI应该生成不同的用户ID");
        assertNotEquals(userId1, userId3, "不同IMEI应该生成不同的用户ID");
        assertNotEquals(userId2, userId3, "不同IMEI和IMSI应该生成不同的用户ID");
    }

    /**
     * 测试空格处理
     */
    @Test
    @DisplayName("用户ID生成空格处理")
    void testGenerateUserIdWithSpaces() {
        String userId1 = UserIdUtil.generateUserId(TEST_IMEI, TEST_IMSI);
        String userId2 = UserIdUtil.generateUserId(" " + TEST_IMEI + " ", " " + TEST_IMSI + " ");
        
        assertEquals(userId1, userId2, "包含空格的输入应该与trim后的输入生成相同的用户ID");
    }

    /**
     * 测试边界值：只有IMEI
     */
    @Test
    @DisplayName("边界值测试：只有IMEI")
    void testGenerateUserIdWithOnlyImei() {
        String userId = UserIdUtil.generateUserId(TEST_IMEI, "");
        assertNotNull(userId);
        assertTrue(UserIdUtil.isValidUserId(userId));
        assertEquals(32, userId.length());
    }

    /**
     * 测试边界值：只有IMSI
     */
    @Test
    @DisplayName("边界值测试：只有IMSI")
    void testGenerateUserIdWithOnlyImsi() {
        String userId = UserIdUtil.generateUserId("", TEST_IMSI);
        assertNotNull(userId);
        assertTrue(UserIdUtil.isValidUserId(userId));
        assertEquals(32, userId.length());
    }
}
