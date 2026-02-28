package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警适配器 - CSP SDK实现
 */
@Component("cspAlarmAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspAlarmAdapter implements AlarmAdapter {
    
    private static final Logger logger = LogManager.getLogger(CspAlarmAdapter.class);
    
    private static final String GET_ALARM_INTERFACE = "cse://FMService/fmOperation/v1/alarms/get_alarms";
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000; // 10分钟
    
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    private final SystemUtilAdapter systemUtilAdapter;
    
    public CspAlarmAdapter(SystemUtilAdapter systemUtilAdapter) {
        this.systemUtilAdapter = systemUtilAdapter;
    }
    
    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 告警去重检查
        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            logger.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }
        
        try {
            // 使用反射调用CSP SDK
            Class<?> alarmModelClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel");
            Object alarmType = getAlarmTypeEnum(alarmModelClass, type);
            
            Class<?> alarmClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmanager.Alarm");
            Object alarm = alarmClass.getConstructor(String.class, alarmType.getClass())
                    .newInstance(alarmId, alarmType);
            
            // 设置默认参数
            Method appendParameter = alarmClass.getMethod("appendParameter", String.class, String.class);
            appendParameter.invoke(alarm, "source", systemUtilAdapter.getEnvString("SERVICENAME", "browser-gateway"));
            appendParameter.invoke(alarm, "kind", "service");
            appendParameter.invoke(alarm, "name", systemUtilAdapter.getEnvString("PODNAME", "unknown"));
            appendParameter.invoke(alarm, "namespace", systemUtilAdapter.getEnvString("NAMESPACE", "default"));
            appendParameter.invoke(alarm, "EventSource", "BrowserGateway Service");
            appendParameter.invoke(alarm, "OriginalEventTime", String.valueOf(System.currentTimeMillis()));
            
            // 设置自定义参数
            if (parameters != null) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    appendParameter.invoke(alarm, entry.getKey(), entry.getValue());
                }
            }
            
            // 发送告警
            Class<?> alarmSendManagerClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager");
            Method getInstance = alarmSendManagerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);
            Method sendAlarm = alarmSendManagerClass.getMethod("sendAlarm", alarmClass);
            Boolean success = (Boolean) sendAlarm.invoke(manager, alarm);
            
            if (success) {
                lastAlarmTime.put(alarmId, System.currentTimeMillis());
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
            Class<?> alarmModelClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel");
            Object clearType = getAlarmTypeEnum(alarmModelClass, AlarmType.CLEAR);
            
            Class<?> alarmClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmanager.Alarm");
            Object alarm = alarmClass.getConstructor(String.class, clearType.getClass())
                    .newInstance(alarmId, clearType);
            
            Class<?> alarmSendManagerClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager");
            Method getInstance = alarmSendManagerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);
            Method sendAlarm = alarmSendManagerClass.getMethod("sendAlarm", alarmClass);
            Boolean success = (Boolean) sendAlarm.invoke(manager, alarm);
            
            if (success) {
                lastAlarmTime.remove(alarmId);
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
            boolean success = sendAlarmWithRetry(alarm, maxRetry);
            if (success) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds) {
        try {
            String appId = systemUtilAdapter.getEnvString("appId", "0");
            String jsonParam = String.format("{\"cmd\":\"GET_ACTIVE_ALARMS\",\"language\":\"en-us\",\"data\":{\"appId\":\"%s\",\"alarmIds\":\"%s\"}}",
                    appId, String.join(",", alarmIds));
            
            // 使用反射创建RestTemplate
            Class<?> restTemplateBuilderClass = Class.forName("com.huawei.csp.jsf.api.CspRestTemplateBuilder");
            Method create = restTemplateBuilderClass.getMethod("create");
            RestTemplate restTemplate = (RestTemplate) create.invoke(null);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonParam);
            ResponseEntity<String> response = restTemplate.exchange(GET_ALARM_INTERFACE, HttpMethod.POST, requestEntity, String.class);
            
            if (response.getStatusCodeValue() == HttpStatus.OK.value()) {
                // 解析响应并转换为AlarmInfo列表
                // 这里需要根据实际响应格式进行解析
                return new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Failed to query historical alarms", e);
            return new ArrayList<>();
        }
    }
    
    private boolean sendAlarmWithRetry(AlarmRequest alarm, int maxRetry) {
        int maxRetryCount = alarm.getMaxRetry() != null ? alarm.getMaxRetry() : maxRetry;
        int retryCount = 0;
        long retryDelay = 5000L; // 5秒
        
        while (retryCount < maxRetryCount) {
            try {
                if (sendAlarm(alarm.getAlarmId(), alarm.getType(), alarm.getParameters())) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("Failed to send alarm {} on attempt {}/{}", 
                    alarm.getAlarmId(), retryCount + 1, maxRetryCount, e);
            }
            
            retryCount++;
            if (retryCount < maxRetryCount) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }
    
    private Object getAlarmTypeEnum(Class<?> alarmModelClass, AlarmType type) throws Exception {
        Class<?> enumClass = Class.forName("com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel$EuGenClearType");
        if (type == AlarmType.GENERATE) {
            return Enum.valueOf((Class<Enum>) enumClass, "GENERATE");
        } else {
            return Enum.valueOf((Class<Enum>) enumClass, "CLEAR");
        }
    }
}
