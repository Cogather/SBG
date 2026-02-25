package com.huawei.browsergateway.adapter.impl.csp;

import com.alibaba.fastjson.JSONObject;
import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import com.huawei.csp.jsf.api.CspRestTemplateBuilder;
import com.huawei.csp.om.alarmsdk.alarmmanager.Alarm;
import com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager;
import com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel;
import com.huawei.csp.om.alarmsdk.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警适配器 - CSP SDK实现
 */
@Component
public class CspAlarmAdapter implements AlarmAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspAlarmAdapter.class);
    
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000; // 10分钟
    private static final String GETALARMINTERFACE = "cse://FMService/fmOperation/v1/alarms/get_alarms";
    
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    
    /**
     * 转换告警类型
     */
    private AlarmModel.EuGenClearType convertAlarmType(AlarmType type) {
        return type == AlarmType.GENERATE 
            ? AlarmModel.EuGenClearType.GENERATE 
            : AlarmModel.EuGenClearType.CLEAR;
    }
    
    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 告警去重检查
        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            logger.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }
        
        try {
            // 使用CSP SDK的AlarmSendManager发送告警
            AlarmModel.EuGenClearType alarmType = convertAlarmType(type);
            Alarm alarm = new Alarm(alarmId, alarmType);
            
            // 添加系统参数
            alarm.appendParameter("source", SystemUtil.getStringFromEnv("SERVICENAME"));
            alarm.appendParameter("kind", "service");
            alarm.appendParameter("name", SystemUtil.getStringFromEnv("PODNAME"));
            alarm.appendParameter("namespace", SystemUtil.getStringFromEnv("NAMESPACE"));
            
            // 添加用户自定义参数
            if (parameters != null) {
                parameters.forEach((key, value) -> alarm.appendParameter(key, value));
            }
            
            // 发送告警
            boolean success = AlarmSendManager.getInstance().sendAlarm(alarm);
            
            if (success) {
                lastAlarmTime.put(alarmId, System.currentTimeMillis());
                logger.info("Alarm sent successfully: {}", alarmId);
            } else {
                logger.warn("Failed to send alarm: {}", alarmId);
            }
            
            return success;
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
            // 使用CSP SDK清除告警
            Alarm alarm = new Alarm(alarmId, AlarmModel.EuGenClearType.CLEAR);
            
            // 添加系统参数
            alarm.appendParameter("source", SystemUtil.getStringFromEnv("SERVICENAME"));
            alarm.appendParameter("kind", "service");
            alarm.appendParameter("name", SystemUtil.getStringFromEnv("PODNAME"));
            alarm.appendParameter("namespace", SystemUtil.getStringFromEnv("NAMESPACE"));
            
            boolean success = AlarmSendManager.getInstance().sendAlarm(alarm);
            
            if (success) {
                lastAlarmTime.remove(alarmId);
                logger.info("Alarm cleared successfully: {}", alarmId);
            } else {
                logger.warn("Failed to clear alarm: {}", alarmId);
            }
            
            return success;
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
        try {
            // 使用CSP SDK查询历史告警
            if (alarmIds == null || alarmIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 构建查询参数
            String alarmIdsStr = String.join(",", alarmIds);
            String appId = SystemUtil.getStringFromEnv("APPID");
            if (appId == null || appId.isEmpty()) {
                appId = "0";
            }
            
            String jsonParam = String.format(
                "{\"cmd\":\"GET_ACTIVE_ALARMS\",\"language\":\"en-us\",\"data\":{\"appId\":\"%s\",\"alarmIds\":\"%s\"}}",
                appId, alarmIdsStr
            );
            
            // 使用CSP RestTemplate创建HTTP客户端
            RestTemplate restTemplate = CspRestTemplateBuilder.create();
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonParam);
            ResponseEntity<String> response = restTemplate.exchange(
                GETALARMINTERFACE, HttpMethod.POST, requestEntity, String.class
            );
            
            int statusCode = response.getStatusCodeValue();
            if (HttpStatus.OK.value() != statusCode) {
                logger.error("Query historical alarms failed, status: {}, response: {}", statusCode, response.getBody());
                return new ArrayList<>();
            }
            
            // 解析响应
            String message = response.getBody();
            logger.info("Query historical alarms response: {}", message);
            
            // 解析告警数据（根据实际响应格式解析）
            List<AlarmInfo> alarmInfoList = new ArrayList<>();
            // TODO: 根据实际响应格式解析AlarmInfo列表
            // 这里需要根据实际的响应格式进行解析
            
            return alarmInfoList;
        } catch (Exception e) {
            logger.error("Failed to query historical alarms", e);
            return new ArrayList<>();
        }
    }
}
