package com.huawei.browsergateway.entity.browser;

import com.huawei.browsergateway.entity.enums.BrowserStatus;
import com.huawei.browsergateway.service.impl.UserChrome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 浏览器状态机测试类
 * 采用决策表（Decision Table）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("浏览器状态机测试")
class BrowserStateMachineTest {

    /**
     * 决策表：状态转换验证测试
     * 
     * 条件（Conditions）：
     * C1: 当前状态
     * C2: 目标状态
     * 
     * 动作（Actions）：
     * A1: 是否可以转换
     */
    @ParameterizedTest(name = "状态转换验证测试 - 规则{index}: {0}")
    @MethodSource("stateTransitionValidationTestCases")
    @DisplayName("状态转换验证测试")
    void testCanTransition(
            String description,
            BrowserStatus currentStatus,
            BrowserStatus targetStatus,
            boolean expectedResult
    ) {
        // When
        boolean result = BrowserStateMachine.canTransition(currentStatus, targetStatus);

        // Then
        assertEquals(expectedResult, result, description);
    }

    static Stream<Arguments> stateTransitionValidationTestCases() {
        return Stream.of(
            // R1: null状态 → false
            Arguments.of(
                "R1: 当前状态为null",
                null,
                BrowserStatus.NORMAL,
                false
            ),
            // R2: 目标状态为null → false
            Arguments.of(
                "R2: 目标状态为null",
                BrowserStatus.NORMAL,
                null,
                false
            ),
            // R3: 相同状态 → true（幂等）
            Arguments.of(
                "R3: 相同状态转换（幂等）",
                BrowserStatus.NORMAL,
                BrowserStatus.NORMAL,
                true
            ),
            // R4: NORMAL → REOPEN → true
            Arguments.of(
                "R4: NORMAL → REOPEN",
                BrowserStatus.NORMAL,
                BrowserStatus.REOPEN,
                true
            ),
            // R5: NORMAL → OPEN_ERROR → true
            Arguments.of(
                "R5: NORMAL → OPEN_ERROR",
                BrowserStatus.NORMAL,
                BrowserStatus.OPEN_ERROR,
                true
            ),
            // R6: NORMAL → PAGE_CONTROL_ERROR → true
            Arguments.of(
                "R6: NORMAL → PAGE_CONTROL_ERROR",
                BrowserStatus.NORMAL,
                BrowserStatus.PAGE_CONTROL_ERROR,
                true
            ),
            // R7: REOPEN → NORMAL → true
            Arguments.of(
                "R7: REOPEN → NORMAL",
                BrowserStatus.REOPEN,
                BrowserStatus.NORMAL,
                true
            ),
            // R8: REOPEN → OPEN_ERROR → true
            Arguments.of(
                "R8: REOPEN → OPEN_ERROR",
                BrowserStatus.REOPEN,
                BrowserStatus.OPEN_ERROR,
                true
            ),
            // R9: OPEN_ERROR → REOPEN → true
            Arguments.of(
                "R9: OPEN_ERROR → REOPEN",
                BrowserStatus.OPEN_ERROR,
                BrowserStatus.REOPEN,
                true
            ),
            // R10: OPEN_ERROR → NORMAL → true
            Arguments.of(
                "R10: OPEN_ERROR → NORMAL",
                BrowserStatus.OPEN_ERROR,
                BrowserStatus.NORMAL,
                true
            ),
            // R11: PAGE_CONTROL_ERROR → NORMAL → true
            Arguments.of(
                "R11: PAGE_CONTROL_ERROR → NORMAL",
                BrowserStatus.PAGE_CONTROL_ERROR,
                BrowserStatus.NORMAL,
                true
            ),
            // R12: PAGE_CONTROL_ERROR → REOPEN → true
            Arguments.of(
                "R12: PAGE_CONTROL_ERROR → REOPEN",
                BrowserStatus.PAGE_CONTROL_ERROR,
                BrowserStatus.REOPEN,
                true
            ),
            // R13: 非法转换：NORMAL → PAGE_CONTROL_ERROR（已允许，但测试其他非法转换）
            Arguments.of(
                "R13: REOPEN → PAGE_CONTROL_ERROR（非法）",
                BrowserStatus.REOPEN,
                BrowserStatus.PAGE_CONTROL_ERROR,
                false
            ),
            // R14: 非法转换：OPEN_ERROR → PAGE_CONTROL_ERROR
            Arguments.of(
                "R14: OPEN_ERROR → PAGE_CONTROL_ERROR（非法）",
                BrowserStatus.OPEN_ERROR,
                BrowserStatus.PAGE_CONTROL_ERROR,
                false
            ),
            // R15: 非法转换：PAGE_CONTROL_ERROR → OPEN_ERROR
            Arguments.of(
                "R15: PAGE_CONTROL_ERROR → OPEN_ERROR（非法）",
                BrowserStatus.PAGE_CONTROL_ERROR,
                BrowserStatus.OPEN_ERROR,
                false
            )
        );
    }

    /**
     * 决策表：状态转换执行测试
     * 
     * 条件（Conditions）：
     * C1: 当前状态
     * C2: 目标状态
     * C3: 转换是否合法
     * 
     * 动作（Actions）：
     * A1: 是否抛出异常
     * A2: 状态是否更新
     */
    @ParameterizedTest(name = "状态转换执行测试 - 规则{index}: {0}")
    @MethodSource("stateTransitionExecutionTestCases")
    @DisplayName("状态转换执行测试")
    void testTransition(
            String description,
            BrowserStatus currentStatus,
            BrowserStatus targetStatus,
            boolean isLegal,
            boolean shouldThrowException
    ) {
        // Given
        UserChrome userChrome = mock(UserChrome.class);
        when(userChrome.getStatus()).thenReturn(currentStatus);

        if (shouldThrowException) {
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                BrowserStateMachine.transition(userChrome, targetStatus, "test reason");
            });
            // 状态不应该改变
            verify(userChrome, never()).setStatus(any());
        } else {
            // When
            BrowserStateMachine.transition(userChrome, targetStatus, "test reason");

            // Then
            verify(userChrome, times(1)).setStatus(targetStatus);
        }
    }

    static Stream<Arguments> stateTransitionExecutionTestCases() {
        return Stream.of(
            // R1: 合法转换：NORMAL → REOPEN → 成功，状态更新
            Arguments.of(
                "R1: 合法转换 NORMAL → REOPEN",
                BrowserStatus.NORMAL,
                BrowserStatus.REOPEN,
                true,
                false
            ),
            // R2: 合法转换：REOPEN → NORMAL → 成功，状态更新
            Arguments.of(
                "R2: 合法转换 REOPEN → NORMAL",
                BrowserStatus.REOPEN,
                BrowserStatus.NORMAL,
                true,
                false
            ),
            // R3: 合法转换：NORMAL → OPEN_ERROR → 成功，状态更新
            Arguments.of(
                "R3: 合法转换 NORMAL → OPEN_ERROR",
                BrowserStatus.NORMAL,
                BrowserStatus.OPEN_ERROR,
                true,
                false
            ),
            // R4: 合法转换：OPEN_ERROR → NORMAL → 成功，状态更新
            Arguments.of(
                "R4: 合法转换 OPEN_ERROR → NORMAL",
                BrowserStatus.OPEN_ERROR,
                BrowserStatus.NORMAL,
                true,
                false
            ),
            // R5: 非法转换：REOPEN → PAGE_CONTROL_ERROR → 抛出异常，状态不变
            Arguments.of(
                "R5: 非法转换 REOPEN → PAGE_CONTROL_ERROR",
                BrowserStatus.REOPEN,
                BrowserStatus.PAGE_CONTROL_ERROR,
                false,
                true
            ),
            // R6: 非法转换：OPEN_ERROR → PAGE_CONTROL_ERROR → 抛出异常，状态不变
            Arguments.of(
                "R6: 非法转换 OPEN_ERROR → PAGE_CONTROL_ERROR",
                BrowserStatus.OPEN_ERROR,
                BrowserStatus.PAGE_CONTROL_ERROR,
                false,
                true
            ),
            // R7: 相同状态转换（幂等） → 成功，状态不变（但会调用setStatus）
            Arguments.of(
                "R7: 相同状态转换（幂等）",
                BrowserStatus.NORMAL,
                BrowserStatus.NORMAL,
                true,
                false
            )
        );
    }

    /**
     * 异常场景测试：userChrome为null
     */
    @Test
    @DisplayName("异常场景：userChrome为null")
    void testTransitionWithNullUserChrome() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            BrowserStateMachine.transition(null, BrowserStatus.NORMAL, "test reason");
        });
    }

    /**
     * 异常场景测试：状态为null的异常场景（自动设置为NORMAL）
     */
    @Test
    @DisplayName("异常场景：状态为null，自动设置为NORMAL")
    void testTransitionWithNullStatus() {
        // Given
        UserChrome userChrome = mock(UserChrome.class);
        when(userChrome.getStatus()).thenReturn(null);

        // When
        BrowserStateMachine.transition(userChrome, BrowserStatus.NORMAL, "test reason");

        // Then
        // 当状态为null时，会先设置为NORMAL（第92行），然后再设置为目标状态NORMAL（第105行），所以调用2次
        verify(userChrome, times(2)).setStatus(BrowserStatus.NORMAL);
    }

    /**
     * 状态转换历史记录测试
     */
    @Test
    @DisplayName("状态转换历史记录测试")
    void testStateTransitionHistory() {
        // Given
        UserChrome userChrome = mock(UserChrome.class);
        when(userChrome.getStatus()).thenReturn(BrowserStatus.NORMAL);

        // When - 执行一系列合法转换
        BrowserStateMachine.transition(userChrome, BrowserStatus.REOPEN, "reason1");
        when(userChrome.getStatus()).thenReturn(BrowserStatus.REOPEN);
        
        BrowserStateMachine.transition(userChrome, BrowserStatus.NORMAL, "reason2");
        when(userChrome.getStatus()).thenReturn(BrowserStatus.NORMAL);
        
        BrowserStateMachine.transition(userChrome, BrowserStatus.PAGE_CONTROL_ERROR, "reason3");

        // Then
        verify(userChrome, times(3)).setStatus(any());
    }

    /**
     * 完整状态转换路径测试
     */
    @Test
    @DisplayName("完整状态转换路径测试")
    void testCompleteStateTransitionPath() {
        // Given
        UserChrome userChrome = mock(UserChrome.class);
        when(userChrome.getStatus()).thenReturn(BrowserStatus.NORMAL);

        // When - 执行完整的状态转换路径
        // NORMAL -> REOPEN -> NORMAL -> OPEN_ERROR -> NORMAL
        BrowserStateMachine.transition(userChrome, BrowserStatus.REOPEN, "转换到REOPEN");
        when(userChrome.getStatus()).thenReturn(BrowserStatus.REOPEN);
        
        BrowserStateMachine.transition(userChrome, BrowserStatus.NORMAL, "恢复到NORMAL");
        when(userChrome.getStatus()).thenReturn(BrowserStatus.NORMAL);
        
        BrowserStateMachine.transition(userChrome, BrowserStatus.OPEN_ERROR, "发生OPEN_ERROR");
        when(userChrome.getStatus()).thenReturn(BrowserStatus.OPEN_ERROR);
        
        BrowserStateMachine.transition(userChrome, BrowserStatus.NORMAL, "恢复到NORMAL");

        // Then
        verify(userChrome, times(4)).setStatus(any());
    }

    /**
     * 获取允许的状态转换列表测试
     */
    @Test
    @DisplayName("获取允许的状态转换列表测试")
    void testGetAllowedTransitions() {
        // When
        Set<BrowserStatus> normalTransitions = BrowserStateMachine.getAllowedTransitions(BrowserStatus.NORMAL);
        Set<BrowserStatus> reopenTransitions = BrowserStateMachine.getAllowedTransitions(BrowserStatus.REOPEN);
        Set<BrowserStatus> nullTransitions = BrowserStateMachine.getAllowedTransitions(null);

        // Then
        assertNotNull(normalTransitions);
        assertTrue(normalTransitions.contains(BrowserStatus.NORMAL)); // 包含自身（幂等）
        assertTrue(normalTransitions.contains(BrowserStatus.REOPEN));
        assertTrue(normalTransitions.contains(BrowserStatus.OPEN_ERROR));
        assertTrue(normalTransitions.contains(BrowserStatus.PAGE_CONTROL_ERROR));

        assertNotNull(reopenTransitions);
        assertTrue(reopenTransitions.contains(BrowserStatus.REOPEN)); // 包含自身
        assertTrue(reopenTransitions.contains(BrowserStatus.NORMAL));
        assertTrue(reopenTransitions.contains(BrowserStatus.OPEN_ERROR));

        assertTrue(nullTransitions.isEmpty());
    }
}
