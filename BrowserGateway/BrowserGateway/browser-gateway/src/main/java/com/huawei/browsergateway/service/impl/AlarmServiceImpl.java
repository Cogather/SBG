package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.service.IAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警服务实现类
 * 负责告警的发送、清除、去重和历史查询
 */
@Service
public class AlarmServiceImpl implements IAlarm {
    
    private static final Logger log = LoggerFactory.getLogger(AlarmServiceImpl.class);
    
    // 告警去重时间窗口：10分钟
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000;
    
    // 告警重试次数
    private static final int MAX_RETRY_COUNT = 2;
    
    // 记录最近一次告警时间，用于去重
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    
    @Autowired
    private AlarmAdapter alarmAdapter;
    
    @Autowired
    private SystemUtilAdapter systemUtilAdapter;
    
    @Override
    public boolean sendAlarm(AlarmEvent alarmEvent) {
        if (alarmEvent == null || alarmEvent.getAlarmCodeEnum() == null) {
            log.warn("告警事件为空或告警码为空，跳过发送");
            return false;
        }
        
        String alarmId = String.valueOf(alarmEvent.getAlarmCodeEnum().getCode());
        log.info("开始发送告警: alarmId={}, message={}", alarmId, alarmEvent.getEventMessage());
        
        // 告警去重检查
        long currentTime = System.currentTimeMillis();
        long lastTime = lastAlarmTime.getOrDefault(alarmId, 0L);
        if (currentTime - lastTime < ALARM_DEDUPE_INTERVAL) {
            log.info("告警 {} 在10分钟内已发送过，跳过本次发送", alarmId);
            return false;
        }
        
        // 构建告警参数
        Map<String, String> parameters = buildAlarmParameters(alarmEvent);
        
        // 重试发送告警
        boolean success = retrySendAlarm(alarmId, AlarmAdapter.AlarmType.GENERATE, parameters);
        
        if (success) {
            lastAlarmTime.put(alarmId, currentTime);
            log.info("告警发送成功: alarmId={}", alarmId);
        } else {
            log.error("告警发送失败: alarmId={}", alarmId);
        }
        
        return success;
    }
    
    @Override
    public boolean clearAlarm(String alarmId) {
        if (alarmId == null || alarmId.isEmpty()) {
            log.warn("告警ID为空，跳过清除");
            return false;
        }
        
        log.info("开始清除告警: alarmId={}", alarmId);
        
        // 构建清除告警参数
        Map<String, String> parameters = new HashMap<>();
        parameters.put("source", systemUtilAdapter.getEnvString("SERVICENAME", "browser-gateway"));
        parameters.put("kind", "service");
        parameters.put("name", systemUtilAdapter.getEnvString("PODNAME", "unknown"));
        parameters.put("namespace", systemUtilAdapter.getEnvString("NAMESPACE", "default"));
        parameters.put("EventSource", "BrowserGW Service");
        parameters.put("OriginalEventTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 重试清除告警
        boolean success = retrySendAlarm(alarmId, AlarmAdapter.AlarmType.CLEAR, parameters);
        
        if (success) {
            lastAlarmTime.remove(alarmId);
            log.info("告警清除成功: alarmId={}", alarmId);
        } else {
            log.error("告警清除失败: alarmId={}", alarmId);
        }
        
        return success;
    }
    
    /**
     * 批量发送告警
     * 
     * @param alarmEvents 告警事件列表
     * @return 每个告警的发送结果 {告警ID: 是否成功}
     */
    public Map<String, Boolean> batchSendAlarms(List<AlarmEvent> alarmEvents) {
        Map<String, Boolean> results = new HashMap<>();
        
        if (alarmEvents == null || alarmEvents.isEmpty()) {
            log.warn("告警事件列表为空，跳过批量发送");
            return results;
        }
        
        log.info("开始批量发送告警，数量: {}", alarmEvents.size());
        
        // 构建告警请求列表
        List<AlarmRequest> alarmRequests = new ArrayList<>();
        for (AlarmEvent alarmEvent : alarmEvents) {
            if (alarmEvent == null || alarmEvent.getAlarmCodeEnum() == null) {
                continue;
            }
            
            AlarmRequest request = new AlarmRequest();
            request.setAlarmId(String.valueOf(alarmEvent.getAlarmCodeEnum().getCode()));
            request.setType(AlarmAdapter.AlarmType.GENERATE);
            request.setParameters(buildAlarmParameters(alarmEvent));
            request.setMaxRetry(MAX_RETRY_COUNT);
            alarmRequests.add(request);
        }
        
        // 批量发送
        int successCount = alarmAdapter.sendAlarmsBatch(alarmRequests, MAX_RETRY_COUNT);
        
        // 记录结果
        for (int i = 0; i < alarmEvents.size() && i < alarmRequests.size(); i++) {
            AlarmEvent alarmEvent = alarmEvents.get(i);
            AlarmRequest request = alarmRequests.get(i);
            boolean success = successCount > 0 && i < successCount;
            results.put(request.getAlarmId(), success);
            
            if (success) {
                lastAlarmTime.put(request.getAlarmId(), System.currentTimeMillis());
            }
        }
        
        log.info("批量发送告警完成，成功: {}/{}", successCount, alarmRequests.size());
        return results;
    }
    
    /**
     * 批量清除告警
     * 
     * @param alarmIds 告警ID列表
     * @return 每个告警的清除结果 {告警ID: 是否成功}
     */
    public Map<String, Boolean> batchClearAlarms(List<String> alarmIds) {
        Map<String, Boolean> results = new HashMap<>();
        
        if (alarmIds == null || alarmIds.isEmpty()) {
            log.warn("告警ID列表为空，跳过批量清除");
            return results;
        }
        
        log.info("开始批量清除告警，数量: {}", alarmIds.size());
        
        for (String alarmId : alarmIds) {
            boolean success = clearAlarm(alarmId);
            results.put(alarmId, success);
        }
        
        log.info("批量清除告警完成，成功: {}/{}", 
            results.values().stream().mapToInt(b -> b ? 1 : 0).sum(), alarmIds.size());
        return results;
    }
    
    /**
     * 获取告警历史记录
     * 
     * @param alarmIds 告警ID列表（逗号分隔）
     * @return 告警历史记录列表
     */
    public List<AlarmInfo> getAlarms(String alarmIds) {
        if (alarmIds == null || alarmIds.isEmpty()) {
            log.warn("告警ID为空，返回空列表");
            return new ArrayList<>();
        }
        
        log.info("查询告警历史: alarmIds={}", alarmIds);
        
        // 解析告警ID列表
        List<String> alarmIdList = new ArrayList<>();
        String[] ids = alarmIds.split(",");
        for (String id : ids) {
            String trimmedId = id.trim();
            if (!trimmedId.isEmpty()) {
                alarmIdList.add(trimmedId);
            }
        }
        
        if (alarmIdList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询历史告警
        List<AlarmInfo> alarmInfos = alarmAdapter.queryHistoricalAlarms(alarmIdList);
        
        log.info("查询告警历史完成，返回记录数: {}", alarmInfos.size());
        return alarmInfos;
    }
    
    /**
     * 检查告警是否存活
     * 
     * @param alarmId 告警ID
     * @return 是否存活
     */
    public boolean isAlarmAlive(String alarmId) {
        if (alarmId == null || alarmId.isEmpty()) {
            return false;
        }
        
        // 如果最近发送过告警，则认为告警存活
        long lastTime = lastAlarmTime.getOrDefault(alarmId, 0L);
        if (lastTime > 0) {
            long elapsed = System.currentTimeMillis() - lastTime;
            // 如果告警在去重窗口内，认为告警存活
            return elapsed < ALARM_DEDUPE_INTERVAL;
        }
        
        return false;
    }
    
    /**
     * 重试发送告警
     * 
     * @param alarmId 告警ID
     * @param type 告警类型
     * @param parameters 告警参数
     * @return 是否成功
     */
    private boolean retrySendAlarm(String alarmId, AlarmAdapter.AlarmType type, Map<String, String> parameters) {
        int retryCount = 0;
        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                boolean success = alarmAdapter.sendAlarm(alarmId, type, parameters);
                if (success) {
                    return true;
                }
                
                retryCount++;
                if (retryCount <= MAX_RETRY_COUNT) {
                    log.warn("告警发送失败，准备重试: alarmId={}, retryCount={}/{}", alarmId, retryCount, MAX_RETRY_COUNT);
                    // 等待5秒后重试
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", e);
                        return false;
                    }
                }
            } catch (Exception e) {
                retryCount++;
                log.error("告警发送异常: alarmId={}, retryCount={}/{}", alarmId, retryCount, MAX_RETRY_COUNT, e);
                if (retryCount <= MAX_RETRY_COUNT) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        
        log.error("告警发送失败，已达到最大重试次数: alarmId={}", alarmId);
        return false;
    }
    
    /**
     * 构建告警参数
     * 
     * @param alarmEvent 告警事件
     * @return 告警参数映射
     */
    private Map<String, String> buildAlarmParameters(AlarmEvent alarmEvent) {
        Map<String, String> parameters = new HashMap<>();
        
        // 系统信息
        parameters.put("source", systemUtilAdapter.getEnvString("SERVICENAME", "browser-gateway"));
        parameters.put("kind", "service");
        parameters.put("name", systemUtilAdapter.getEnvString("PODNAME", "unknown"));
        parameters.put("namespace", systemUtilAdapter.getEnvString("NAMESPACE", "default"));
        
        // 告警信息
        parameters.put("EventMessage", alarmEvent.getEventMessage() != null ? alarmEvent.getEventMessage() : "");
        parameters.put("EventSource", "BrowserGW Service");
        parameters.put("OriginalEventTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 告警码信息
        if (alarmEvent.getAlarmCodeEnum() != null) {
            parameters.put("alarmCode", String.valueOf(alarmEvent.getAlarmCodeEnum().getCode()));
            parameters.put("alarmMessage", alarmEvent.getAlarmCodeEnum().getMessage());
        }
        
        return parameters;
    }
}
