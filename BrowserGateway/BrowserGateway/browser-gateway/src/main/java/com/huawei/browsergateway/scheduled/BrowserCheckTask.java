package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.browser.UserChrome.BrowserStatus;
import com.huawei.browsergateway.driver.ChromeDriverProxy;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 浏览器检查任务
 * 定期检查浏览器实例状态，清理异常实例
 * 
 * 功能说明：
 * 1. 检查每个浏览器实例的运行状态
 * 2. 检查实例心跳时间
 * 3. 检查ChromeDriver是否可用
 * 4. 处理异常实例并记录日志
 * 5. 上报检查结果
 */
@Component
public class BrowserCheckTask {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserCheckTask.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private IAlarm alarmService;
    
    // 浏览器检查周期（默认30分钟）
    @Value("${browsergw.scheduled.check-browser-period:1800000}")
    private long checkBrowserPeriod;
    
    // 心跳超时阈值（纳秒），默认5分钟
    @Value("${browsergw.chrome.heartbeat-timeout:300000000000}")
    private long heartbeatTimeout;
    
    /**
     * 执行浏览器实例检查
     * 默认每30分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.check-browser-period:1800000}")
    public void checkBrowserInstances() {
        log.info("开始执行浏览器检查任务");
        
        try {
            Set<String> allUsers = chromeSet.getAllUser();
            
            if (allUsers.isEmpty()) {
                log.debug("没有活跃的浏览器实例");
                return;
            }
            
            int totalCount = allUsers.size();
            int abnormalCount = 0;
            int cleanedCount = 0;
            
            long currentTime = System.nanoTime();
            
            // 遍历所有浏览器实例
            for (String userId : allUsers) {
                try {
                    UserChrome userChrome = chromeSet.get(userId);
                    if (userChrome == null) {
                        log.warn("浏览器实例不存在: userId={}", userId);
                        continue;
                    }
                    
                    // 检查实例状态
                    BrowserCheckResult result = checkInstance(userChrome, currentTime);
                    
                    if (!result.isHealthy()) {
                        abnormalCount++;
                        log.warn("发现异常浏览器实例: userId={}, reason={}, status={}", 
                            userId, result.getReason(), userChrome.getStatus());
                        
                        // 根据异常类型处理
                        if (shouldCleanInstance(result)) {
                            log.info("清理异常浏览器实例: userId={}, reason={}", userId, result.getReason());
                            chromeSet.delete(userId);
                            cleanedCount++;
                            
                            // 发送告警
                            sendAbnormalInstanceAlarm(userId, result);
                        } else {
                            // 仅发送告警，不清理
                            sendAbnormalInstanceAlarm(userId, result);
                        }
                    } else {
                        log.debug("浏览器实例正常: userId={}, status={}", userId, userChrome.getStatus());
                    }
                    
                } catch (Exception e) {
                    log.error("检查浏览器实例异常: userId={}", userId, e);
                    abnormalCount++;
                }
            }
            
            // 记录检查结果
            log.info("浏览器检查任务完成: 总数={}, 异常数={}, 清理数={}", 
                totalCount, abnormalCount, cleanedCount);
            
        } catch (Exception e) {
            log.error("浏览器检查任务执行异常", e);
            
            // 检查任务失败时发送告警
            AlarmEvent alarmEvent = new AlarmEvent();
            alarmEvent.setAlarmCodeEnum(ErrorCodeEnum.HEALTH_REPORT_FAILURE);
            alarmEvent.setEventMessage("浏览器检查任务执行失败: " + e.getMessage());
            alarmService.sendAlarm(alarmEvent);
        }
    }
    
    /**
     * 检查单个浏览器实例
     * 
     * @param userChrome 浏览器实例
     * @param currentTime 当前时间（纳秒）
     * @return 检查结果
     */
    private BrowserCheckResult checkInstance(UserChrome userChrome, long currentTime) {
        BrowserCheckResult result = new BrowserCheckResult();
        result.setUserId(userChrome.getUserId());
        
        // 1. 检查运行状态
        BrowserStatus status = userChrome.getStatus();
        if (status == null) {
            result.setHealthy(false);
            result.setReason("状态为空");
            return result;
        }
        
        // 检查是否为异常状态
        if (isAbnormalStatus(status)) {
            result.setHealthy(false);
            result.setReason("状态异常: " + status.getDescription());
            result.setAbnormalStatus(status);
            return result;
        }
        
        // 2. 检查心跳时间
        long lastHeartbeat = userChrome.getLastHeartbeat();
        long elapsed = currentTime - lastHeartbeat;
        
        if (elapsed > heartbeatTimeout) {
            result.setHealthy(false);
            result.setReason("心跳超时: elapsed=" + (elapsed / 1_000_000_000) + "s, timeout=" + (heartbeatTimeout / 1_000_000_000) + "s");
            result.setHeartbeatTimeout(true);
            return result;
        }
        
        // 3. 检查ChromeDriver是否可用
        Object chromeDriverObj = userChrome.getChromeDriver();
        if (chromeDriverObj == null) {
            result.setHealthy(false);
            result.setReason("ChromeDriver为空");
            return result;
        }
        
        // 如果chromeDriver是ChromeDriverProxy类型，检查其可用性
        if (chromeDriverObj instanceof ChromeDriverProxy) {
            ChromeDriverProxy chromeDriver = (ChromeDriverProxy) chromeDriverObj;
            if (!chromeDriver.isAvailable()) {
                result.setHealthy(false);
                result.setReason("ChromeDriver不可用");
                return result;
            }
        }
        
        // 所有检查通过
        result.setHealthy(true);
        return result;
    }
    
    /**
     * 判断是否为异常状态
     * 
     * @param status 浏览器状态
     * @return 是否为异常状态
     */
    private boolean isAbnormalStatus(BrowserStatus status) {
        // 异常状态包括：OPEN_ERROR, CONNECTION_ERROR, PAGE_CONTROL_ERROR, NETWORK_ERROR, MEMORY_ERROR
        return status == BrowserStatus.OPEN_ERROR ||
               status == BrowserStatus.CONNECTION_ERROR ||
               status == BrowserStatus.PAGE_CONTROL_ERROR ||
               status == BrowserStatus.NETWORK_ERROR ||
               status == BrowserStatus.MEMORY_ERROR;
    }
    
    /**
     * 判断是否应该清理实例
     * 
     * @param result 检查结果
     * @return 是否应该清理
     */
    private boolean shouldCleanInstance(BrowserCheckResult result) {
        // 心跳超时或严重异常状态时清理
        if (result.isHeartbeatTimeout()) {
            return true;
        }
        
        // 某些异常状态需要清理
        if (result.getAbnormalStatus() != null) {
            BrowserStatus status = result.getAbnormalStatus();
            // 严重错误状态需要清理
            return status == BrowserStatus.MEMORY_ERROR ||
                   status == BrowserStatus.OPEN_ERROR ||
                   (status == BrowserStatus.CONNECTION_ERROR && result.isHeartbeatTimeout());
        }
        
        return false;
    }
    
    /**
     * 发送异常实例告警
     * 
     * @param userId 用户ID
     * @param result 检查结果
     */
    private void sendAbnormalInstanceAlarm(String userId, BrowserCheckResult result) {
        try {
            AlarmEvent alarmEvent = new AlarmEvent();
            alarmEvent.setAlarmCodeEnum(ErrorCodeEnum.BROWSER_INSTANCE_TIMEOUT);
            alarmEvent.setEventMessage(String.format("浏览器实例异常: userId=%s, reason=%s", userId, result.getReason()));
            alarmService.sendAlarm(alarmEvent);
        } catch (Exception e) {
            log.error("发送异常实例告警失败: userId={}", userId, e);
        }
    }
    
    /**
     * 浏览器检查结果
     */
    private static class BrowserCheckResult {
        private String userId;
        private boolean healthy;
        private String reason;
        private boolean heartbeatTimeout;
        private BrowserStatus abnormalStatus;
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public boolean isHeartbeatTimeout() {
            return heartbeatTimeout;
        }
        
        public void setHeartbeatTimeout(boolean heartbeatTimeout) {
            this.heartbeatTimeout = heartbeatTimeout;
        }
        
        public BrowserStatus getAbnormalStatus() {
            return abnormalStatus;
        }
        
        public void setAbnormalStatus(BrowserStatus abnormalStatus) {
            this.abnormalStatus = abnormalStatus;
        }
    }
}
