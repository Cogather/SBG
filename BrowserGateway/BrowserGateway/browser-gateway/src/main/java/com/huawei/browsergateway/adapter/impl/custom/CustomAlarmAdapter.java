package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警适配器 - 自定义实现
 * 适用场景：外网环境，将告警写入本地日志文件或发送到监控系统
 */
@Component
public class CustomAlarmAdapter implements AlarmAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAlarmAdapter.class);
    
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000;
    
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    
    @Value("${adapter.custom.alarm.log-path:/tmp/browsergw_alarms.log}")
    private String logFilePath;
    
    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 去重检查
        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            logger.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }
        
        try {
            // 记录告警到日志文件
            String alarmLog = formatAlarmLog(alarmId, type, parameters);
            writeAlarmLog(alarmLog);
            
            // 同时记录到应用日志
            logger.warn("ALARM TRIGGERED: {}", alarmLog);
            
            // 更新最后发送时间
            lastAlarmTime.put(alarmId, System.currentTimeMillis());
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to send alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public boolean clearAlarm(String alarmId) {
        if (!lastAlarmTime.containsKey(alarmId)) {
            return false;
        }
        
        try {
            String clearLog = formatClearLog(alarmId);
            writeAlarmLog(clearLog);
            logger.info("Alarm cleared: {}", alarmId);
            
            lastAlarmTime.remove(alarmId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to clear alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry) {
        int successCount = 0;
        for (AlarmRequest alarm : alarms) {
            boolean success = sendAlarm(alarm.getAlarmId(), alarm.getType(), alarm.getParameters());
            if (success) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds) {
        // 外网环境不支持历史告警查询，返回空列表
        return new ArrayList<>();
    }
    
    private String formatAlarmLog(String alarmId, AlarmType type, Map<String, String> parameters) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(formatter)).append("] ");
        sb.append("ALARM_ID=").append(alarmId).append(" ");
        sb.append("TYPE=").append(type).append(" ");
        
        if (parameters != null) {
            parameters.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
        }
        
        return sb.toString();
    }
    
    private String formatClearLog(String alarmId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] ALARM_CLEARED: ALARM_ID=%s", 
            LocalDateTime.now().format(formatter), alarmId);
    }
    
    private void writeAlarmLog(String logMessage) throws IOException {
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(logMessage + "\n");
        }
    }
}
