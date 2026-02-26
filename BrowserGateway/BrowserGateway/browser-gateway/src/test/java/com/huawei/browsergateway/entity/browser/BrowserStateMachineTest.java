package com.huawei.browsergateway.entity.browser;

import com.huawei.browsergateway.entity.request.InitBrowserRequest;
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
 * BrowserStateMachine单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrowserStateMachine决策表测试")
class BrowserStateMachineTest {

    private static final String TEST_USER_ID = "test_user_id";

    /**
     * 决策表：状态转换验证测试
     * 
     * 条件：
     * C1: 当前状态
     * C2: 目标状态
     * 
     * 动作：
     * A1: 是否可以转换
     */
    @ParameterizedTest(name = "状态转换验证决策表 - 规则{0}: {1} -> {2}, 预期={3}")
    @MethodSource("stateTransitionValidationDecisionTable")
    @DisplayName("状态转换验证决策表测试")
    void testCanTransitionDecisionTable(
            String ruleId,
            UserChrome.BrowserStatus current,
            UserChrome.BrowserStatus next,
            boolean expectedResult) {
        
        boolean result = BrowserStateMachine.canTransition(current, next);
        assertEquals(expectedResult, result, 
                String.format("状态转换验证失败: %s -> %s", current, next));
    }

    /**
     * 决策表数据源：状态转换验证
     */
    static Stream<Arguments> stateTransitionValidationDecisionTable() {
        return Stream.of(
            // R1: null -> READY -> false
            Arguments.of("R1", null, UserChrome.BrowserStatus.READY, false),
            
            // R2: INITIALIZING -> null -> false
            Arguments.of("R2", UserChrome.BrowserStatus.INITIALIZING, null, false),
            
            // R3: 相同状态 -> true (幂等)
            Arguments.of("R3", UserChrome.BrowserStatus.READY, UserChrome.BrowserStatus.READY, true),
            
            // R4: INITIALIZING -> CREATING -> true (合法转换)
            Arguments.of("R4", UserChrome.BrowserStatus.INITIALIZING, UserChrome.BrowserStatus.CREATING, true),
            
            // R5: INITIALIZING -> READY -> false (非法转换)
            Arguments.of("R5", UserChrome.BrowserStatus.INITIALIZING, UserChrome.BrowserStatus.READY, false),
            
            // R6: CREATING -> READY -> true (合法转换)
            Arguments.of("R6", UserChrome.BrowserStatus.CREATING, UserChrome.BrowserStatus.READY, true),
            
            // R7: READY -> CONNECTING -> true (合法转换)
            Arguments.of("R7", UserChrome.BrowserStatus.READY, UserChrome.BrowserStatus.CONNECTING, true),
            
            // R8: READY -> CLOSING -> true (合法转换)
            Arguments.of("R8", UserChrome.BrowserStatus.READY, UserChrome.BrowserStatus.CLOSING, true),
            
            // R9: CONNECTING -> CONNECTED -> true (合法转换)
            Arguments.of("R9", UserChrome.BrowserStatus.CONNECTING, UserChrome.BrowserStatus.CONNECTED, true),
            
            // R10: CONNECTED -> RUNNING -> true (合法转换)
            Arguments.of("R10", UserChrome.BrowserStatus.CONNECTED, UserChrome.BrowserStatus.RUNNING, true),
            
            // R11: RUNNING -> CLOSING -> true (合法转换)
            Arguments.of("R11", UserChrome.BrowserStatus.RUNNING, UserChrome.BrowserStatus.CLOSING, true),
            
            // R12: CLOSING -> CLOSED -> true (合法转换)
            Arguments.of("R12", UserChrome.BrowserStatus.CLOSING, UserChrome.BrowserStatus.CLOSED, true),
            
            // R13: CLOSED -> READY -> true (支持重启)
            Arguments.of("R13", UserChrome.BrowserStatus.CLOSED, UserChrome.BrowserStatus.READY, true),
            
            // R14: OPEN_ERROR -> CLOSING -> true (错误状态可以关闭)
            Arguments.of("R14", UserChrome.BrowserStatus.OPEN_ERROR, UserChrome.BrowserStatus.CLOSING, true),
            
            // R15: NETWORK_ERROR -> READY -> true (错误状态可以重试)
            Arguments.of("R15", UserChrome.BrowserStatus.NETWORK_ERROR, UserChrome.BrowserStatus.READY, true),
            
            // R16: MEMORY_ERROR -> CLOSED -> true (内存错误只能关闭)
            Arguments.of("R16", UserChrome.BrowserStatus.MEMORY_ERROR, UserChrome.BrowserStatus.CLOSED, true),
            
            // R17: MEMORY_ERROR -> READY -> false (内存错误不能重试)
            Arguments.of("R17", UserChrome.BrowserStatus.MEMORY_ERROR, UserChrome.BrowserStatus.READY, false),
            
            // R18: READY -> RUNNING -> false (非法转换，需要先CONNECTING)
            Arguments.of("R18", UserChrome.BrowserStatus.READY, UserChrome.BrowserStatus.RUNNING, false)
        );
    }

    /**
     * 决策表：状态转换执行测试
     * 
     * 条件：
     * C1: 当前状态
     * C2: 目标状态
     * C3: 转换是否合法
     * 
     * 动作：
     * A1: 是否抛出异常
     * A2: 状态是否更新
     */
    @ParameterizedTest(name = "状态转换执行决策表 - 规则{0}: {1} -> {2}, 合法={3}")
    @MethodSource("stateTransitionExecutionDecisionTable")
    @DisplayName("状态转换执行决策表测试")
    void testTransitionDecisionTable(
            String ruleId,
            UserChrome.BrowserStatus current,
            UserChrome.BrowserStatus next,
            boolean isValidTransition,
            boolean shouldThrowException,
            UserChrome.BrowserStatus expectedStatus) {
        
        InitBrowserRequest request = createRequest();
        UserChrome userChrome = new UserChrome(TEST_USER_ID, request);
        userChrome.setStatus(current);
        
        if (shouldThrowException) {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                BrowserStateMachine.transition(userChrome, next, "测试转换");
            });
            assertTrue(exception.getMessage().contains("非法的状态转换"));
            // 状态不应该改变
            assertEquals(current, userChrome.getStatus());
        } else {
            assertDoesNotThrow(() -> {
                BrowserStateMachine.transition(userChrome, next, "测试转换");
            });
            assertEquals(expectedStatus, userChrome.getStatus());
        }
    }

    /**
     * 决策表数据源：状态转换执行
     */
    static Stream<Arguments> stateTransitionExecutionDecisionTable() {
        return Stream.of(
            // R1: 合法转换 INITIALIZING -> CREATING -> 成功
            Arguments.of("R1", UserChrome.BrowserStatus.INITIALIZING, 
                    UserChrome.BrowserStatus.CREATING, true, false, UserChrome.BrowserStatus.CREATING),
            
            // R2: 合法转换 CREATING -> READY -> 成功
            Arguments.of("R2", UserChrome.BrowserStatus.CREATING, 
                    UserChrome.BrowserStatus.READY, true, false, UserChrome.BrowserStatus.READY),
            
            // R3: 合法转换 READY -> CLOSING -> 成功
            Arguments.of("R3", UserChrome.BrowserStatus.READY, 
                    UserChrome.BrowserStatus.CLOSING, true, false, UserChrome.BrowserStatus.CLOSING),
            
            // R4: 合法转换 CLOSING -> CLOSED -> 成功
            Arguments.of("R4", UserChrome.BrowserStatus.CLOSING, 
                    UserChrome.BrowserStatus.CLOSED, true, false, UserChrome.BrowserStatus.CLOSED),
            
            // R5: 非法转换 INITIALIZING -> READY -> 抛出异常
            Arguments.of("R5", UserChrome.BrowserStatus.INITIALIZING, 
                    UserChrome.BrowserStatus.READY, false, true, UserChrome.BrowserStatus.INITIALIZING),
            
            // R6: 非法转换 READY -> RUNNING -> 抛出异常
            Arguments.of("R6", UserChrome.BrowserStatus.READY, 
                    UserChrome.BrowserStatus.RUNNING, false, true, UserChrome.BrowserStatus.READY),
            
            // R7: 相同状态转换（幂等） -> 成功，状态不变
            Arguments.of("R7", UserChrome.BrowserStatus.READY, 
                    UserChrome.BrowserStatus.READY, true, false, UserChrome.BrowserStatus.READY)
        );
    }

    /**
     * 测试userChrome为null的情况
     */
    @Test
    @DisplayName("状态转换异常场景：userChrome为null")
    void testTransitionWithNullUserChrome() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            BrowserStateMachine.transition(null, UserChrome.BrowserStatus.READY, "测试");
        });
        assertTrue(exception.getMessage().contains("浏览器实例不能为null"));
    }

    /**
     * 测试状态为null的情况（应该自动设置为INITIALIZING）
     */
    @Test
    @DisplayName("状态转换异常场景：状态为null")
    void testTransitionWithNullStatus() {
        InitBrowserRequest request = createRequest();
        UserChrome userChrome = new UserChrome(TEST_USER_ID, request);
        userChrome.setStatus(null);
        
        // 应该自动设置为INITIALIZING，然后转换到CREATING
        assertDoesNotThrow(() -> {
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CREATING, "测试");
        });
        assertEquals(UserChrome.BrowserStatus.CREATING, userChrome.getStatus());
    }

    /**
     * 测试状态转换历史记录
     */
    @Test
    @DisplayName("状态转换历史记录")
    void testStateTransitionHistory() {
        InitBrowserRequest request = createRequest();
        UserChrome userChrome = new UserChrome(TEST_USER_ID, request);
        userChrome.setStatus(UserChrome.BrowserStatus.INITIALIZING);
        
        // 执行多次状态转换
        BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CREATING, "步骤1");
        BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.READY, "步骤2");
        BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CONNECTING, "步骤3");
        
        // 验证最终状态
        assertEquals(UserChrome.BrowserStatus.CONNECTING, userChrome.getStatus());
    }

    /**
     * 测试所有合法状态转换路径
     */
    @Test
    @DisplayName("完整状态转换路径测试")
    void testCompleteStateTransitionPath() {
        InitBrowserRequest request = createRequest();
        UserChrome userChrome = new UserChrome(TEST_USER_ID, request);
        
        // 完整路径：INITIALIZING -> CREATING -> READY -> CONNECTING -> CONNECTED -> RUNNING -> CLOSING -> CLOSED
        assertDoesNotThrow(() -> {
            userChrome.setStatus(UserChrome.BrowserStatus.INITIALIZING);
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CREATING, "创建中");
            assertEquals(UserChrome.BrowserStatus.CREATING, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.READY, "就绪");
            assertEquals(UserChrome.BrowserStatus.READY, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CONNECTING, "连接中");
            assertEquals(UserChrome.BrowserStatus.CONNECTING, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CONNECTED, "已连接");
            assertEquals(UserChrome.BrowserStatus.CONNECTED, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.RUNNING, "运行中");
            assertEquals(UserChrome.BrowserStatus.RUNNING, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CLOSING, "关闭中");
            assertEquals(UserChrome.BrowserStatus.CLOSING, userChrome.getStatus());
            
            BrowserStateMachine.transition(userChrome, UserChrome.BrowserStatus.CLOSED, "已关闭");
            assertEquals(UserChrome.BrowserStatus.CLOSED, userChrome.getStatus());
        });
    }

    // ========== 辅助方法 ==========

    private static InitBrowserRequest createRequest() {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei("123456789012345");
        request.setImsi("123456789012345");
        request.setLcdWidth(1920);
        request.setLcdHeight(1080);
        request.setAppType("mobile");
        return request;
    }
}
