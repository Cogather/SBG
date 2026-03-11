package com.huawei.browsergateway.service.impl;

import cn.hutool.json.JSONUtil;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.AlarmAdapter;
import com.huawei.browsergateway.adapter.SystemUtilAdapter;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.service.IAlarm;
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

    private final AlarmAdapter alarmAdapter;
    private final SystemUtilAdapter systemUtilAdapter;

    public static ConcurrentHashMap<String, Long> alarmMap = new ConcurrentHashMap<>();

    @Autowired
    public AlarmServiceImpl(AlarmAdapter alarmAdapter, SystemUtilAdapter systemUtilAdapter) {
        this.alarmAdapter = alarmAdapter;
        this.systemUtilAdapter = systemUtilAdapter;
    };

    @Override
    public void sendAlarm(AlarmEvent alarmEvent) {
        log.info("enter send alarm");
        boolean result = alarmAdapter.sendAlarm(
                alarmEvent.getAlarmCodeEnum().getAlarmId(),
                AlarmAdapter.AlarmType.GENERATE,
                buildAlarmParameters(alarmEvent)
        );
        if (result) {
            log.info("send alarm successfully.");
        } else {
            log.info("Failed to send alarm.");
        }
    }

    @Override
    public void clearAlarm(AlarmEvent alarmEvent) {
        if (!alarmMap.containsKey(alarmEvent.getAlarmCodeEnum().getAlarmId())) {
            return;
        }

        boolean result = alarmAdapter.clearAlarm(alarmEvent.getAlarmCodeEnum().getAlarmId());
        if (result) {
            alarmMap.remove(alarmEvent.getAlarmCodeEnum().getAlarmId());
            log.info("send recover alarm successfully.");
        } else {
            log.info("Failed to send recover alarm.");
        }
    }

    /**
     * 构建告警参数
     * @param alarmEvent 告警事件
     * @return 告警参数Map
     */
    private Map<String, String> buildAlarmParameters(AlarmEvent alarmEvent) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("source", systemUtilAdapter.getEnvString("SERVICENAME", "browser-gateway"));
        parameters.put("kind", "service");
        parameters.put("name", systemUtilAdapter.getEnvString("PODNAME", "unknown"));
        parameters.put("namespace", systemUtilAdapter.getEnvString("NAMESPACE", "default"));
        parameters.put("EventMessage", alarmEvent.getEventMessage());
        parameters.put("EventSource", "BrowserGW Service");
        parameters.put("OriginalEventTime", TimeUtil.getCurrentDate());
        return parameters;
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
            String alarmIds = AlarmEnum.getAllCodes();
            List<String> alarmIdList = Arrays.asList(alarmIds.split("&"));
            List<AlarmInfo> alarms = alarmAdapter.queryHistoricalAlarms(alarmIdList);
            if (alarms == null || alarms.isEmpty()) {
                log.info("No historical alarms found.");
                return;
            }
            for (AlarmInfo alarmInfo : alarms) {
                log.info("Processing historical alarm: {}", alarmInfo.getAlarmId());
                // 取消告警
                boolean result = alarmAdapter.clearAlarm(alarmInfo.getAlarmId());
                if (result) {
                    log.info("Send recover alarm successfully for alarmId: {}", alarmInfo.getAlarmId());
                } else {
                    log.warn("Failed to send recover alarm for alarmId: {}", alarmInfo.getAlarmId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing historical alarms", e);
        }
    }
}