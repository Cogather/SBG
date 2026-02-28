package com.huawei.browsergateway.service.impl;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;

import com.alibaba.fastjson.JSONObject;
import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.alarm.AlarmResponseParam;
import com.huawei.browsergateway.entity.alarm.DataParam;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.util.DeployUtil;
import com.huawei.browsergateway.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlarmServiceImpl implements IAlarm {
    private static final Logger log = LogManager.getLogger(AlarmServiceImpl.class);

    private static final Integer ONE_MINUTE = 10 * 60 * 1000;
    public static ConcurrentHashMap<String, Long> alarmMap = new ConcurrentHashMap<>();

    @Autowired
    private AlarmAdapter alarmAdapter;
    
    @Autowired
    private SystemUtilAdapter systemUtilAdapter;

    @Override
    public boolean sendAlarm(AlarmEvent alarmEvent) {
        log.info("enter send alarm");
        if (System.currentTimeMillis() - alarmMap.getOrDefault(alarmEvent.getAlarmCodeEnum().getAlarmId(), 0L) < ONE_MINUTE) {
            log.info("An alarm was already reported within 10 minute; skipping this operation.");
            return false;
        }
        
        Map<String, String> parameters = new HashMap<>();
        parameters.put("EventMessage", alarmEvent.getEventMessage());
        parameters.put("EventSource", "BrowserGW Service");
        parameters.put("OriginalEventTime", TimeUtil.getCurrentDate());
        
        boolean result = alarmAdapter.sendAlarm(
            alarmEvent.getAlarmCodeEnum().getAlarmId(), 
            AlarmAdapter.AlarmType.GENERATE, 
            parameters
        );
        
        if (result) {
            alarmMap.put(alarmEvent.getAlarmCodeEnum().getAlarmId(), System.currentTimeMillis());
            log.info("send alarm successfully.");
        } else {
            log.info("Failed to send alarm.");
        }
        return result;
    }

    @Override
    public boolean clearAlarm(String alarmId) {
        if (!alarmMap.containsKey(alarmId)) {
            return false;
        }
        
        boolean result = alarmAdapter.clearAlarm(alarmId);
        if (result) {
            alarmMap.remove(alarmId);
            log.info("send recover alarm successfully.");
        } else {
            log.info("Failed to send recover alarm.");
        }
        return result;
    }

    private boolean isInitialized = false;

    @EventListener(ContextRefreshedEvent.class)
    public void runAfterStartup(ContextRefreshedEvent event) {
        if (isInitialized) {
            log.info("Initialization attempt skipped as it was already completed.");
            return;
        }
        log.info("start runAfterStartup.");
        handleHistoryAlarm();
        log.info("end runAfterStartup.");
        isInitialized = true;
    }

    public void handleHistoryAlarm() {
        try {
            String allCodes = AlarmEnum.getAllCodes();
            List<String> alarmIds = Arrays.asList(allCodes.split("&"));
            List<AlarmInfo> alarms = alarmAdapter.queryHistoricalAlarms(alarmIds);
            if (alarms == null || alarms.isEmpty()) {
                log.info("get alarm fail or no active alarms, skip it");
                return;
            }
            for (AlarmInfo alarmInfo : alarms) {
                log.info("send history alarm: {}", JSONUtil.toJsonStr(alarmInfo));
                // 清除告警
                Map<String, String> parameters = new HashMap<>();
                parameters.put("EventMessage", alarmInfo.getMessage());
                parameters.put("EventSource", "BrowserGW Service");
                parameters.put("OriginalEventTime", String.valueOf(alarmInfo.getTimestamp()));
                
                boolean result = alarmAdapter.clearAlarm(alarmInfo.getAlarmId());
                if (result) {
                    log.info("send recover alarm successfully.");
                } else {
                    log.info("Failed to send recover alarm.");
                }
            }
        } catch (Exception e) {
            log.info("ignore all exception", e);
        }
    }
}
