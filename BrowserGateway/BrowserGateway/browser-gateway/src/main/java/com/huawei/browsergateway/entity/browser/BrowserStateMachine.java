package com.huawei.browsergateway.entity.browser;

import com.huawei.browsergateway.entity.enums.BrowserStatus;
import com.huawei.browsergateway.service.impl.UserChrome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * 浏览器状态机工具类
 * 管理浏览器状态转换规则和验证
 */
public class BrowserStateMachine {
    private static final Logger log = LogManager.getLogger(BrowserStateMachine.class);

    // 状态转换规则映射：当前状态 -> 允许的目标状态集合
    private static final Map<BrowserStatus, Set<BrowserStatus>> TRANSITION_RULES = new HashMap<>();

    static {
        // 初始化状态转换规则
        // NORMAL状态可以转换到REOPEN、OPEN_ERROR、PAGE_CONTROL_ERROR
        TRANSITION_RULES.put(BrowserStatus.NORMAL, Set.of(
            BrowserStatus.REOPEN,
            BrowserStatus.OPEN_ERROR,
            BrowserStatus.PAGE_CONTROL_ERROR
        ));

        // REOPEN状态可以转换到NORMAL、OPEN_ERROR
        TRANSITION_RULES.put(BrowserStatus.REOPEN, Set.of(
            BrowserStatus.NORMAL,
            BrowserStatus.OPEN_ERROR
        ));

        // OPEN_ERROR状态可以转换到REOPEN、NORMAL
        TRANSITION_RULES.put(BrowserStatus.OPEN_ERROR, Set.of(
            BrowserStatus.REOPEN,
            BrowserStatus.NORMAL
        ));

        // PAGE_CONTROL_ERROR状态可以转换到NORMAL、REOPEN
        TRANSITION_RULES.put(BrowserStatus.PAGE_CONTROL_ERROR, Set.of(
            BrowserStatus.NORMAL,
            BrowserStatus.REOPEN
        ));
    }

    /**
     * 验证状态转换是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus   目标状态
     * @return 是否可以转换
     */
    public static boolean canTransition(BrowserStatus currentStatus, BrowserStatus targetStatus) {
        // null状态检查
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        // 相同状态转换（幂等）
        if (currentStatus == targetStatus) {
            return true;
        }

        // 检查转换规则
        Set<BrowserStatus> allowedTransitions = TRANSITION_RULES.get(currentStatus);
        if (allowedTransitions == null) {
            return false;
        }

        return allowedTransitions.contains(targetStatus);
    }

    /**
     * 执行状态转换
     *
     * @param userChrome   用户浏览器实例
     * @param targetStatus 目标状态
     * @param reason       转换原因
     * @throws IllegalStateException 如果转换不合法
     */
    public static void transition(UserChrome userChrome, BrowserStatus targetStatus, String reason) {
        if (userChrome == null) {
            throw new IllegalArgumentException("userChrome cannot be null");
        }

        BrowserStatus currentStatus = userChrome.getStatus();
        if (currentStatus == null) {
            // 状态为null，自动设置为NORMAL
            currentStatus = BrowserStatus.NORMAL;
            userChrome.setStatus(currentStatus);
            log.info("userChrome status was null, auto-set to NORMAL");
        }

        // 验证转换是否合法
        if (!canTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                String.format("Invalid state transition from %s to %s. Reason: %s", 
                    currentStatus, targetStatus, reason)
            );
        }

        // 执行转换
        userChrome.setStatus(targetStatus);
        log.info("State transition: {} -> {} (reason: {})", currentStatus, targetStatus, reason);
    }

    /**
     * 获取允许的状态转换列表
     *
     * @param currentStatus 当前状态
     * @return 允许的目标状态集合
     */
    public static Set<BrowserStatus> getAllowedTransitions(BrowserStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }

        Set<BrowserStatus> allowed = TRANSITION_RULES.getOrDefault(currentStatus, Collections.emptySet());
        // 添加自身（幂等转换）
        Set<BrowserStatus> result = new HashSet<>(allowed);
        result.add(currentStatus);
        return result;
    }
}
