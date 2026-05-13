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

/**
 * 告警服务实现，负责告警的发送、清除及历史告警的启动时恢复处理
 */
@Service
public class AlarmServiceImpl implements IAlarm {

    private static final Logger log = LogManager.getLogger(AlarmServiceImpl.class);

    /** 告警冷却时间（毫秒），10 分钟内不重复发送同一告警 */
    private static final int ONE_MINUTE = 10 * 60 * 1000;

    private final AlarmAdapter alarmAdapter;
    private final SystemUtilAdapter systemUtilAdapter;

    /** 已发送告警的记录，key 为告警 ID，value 为发送时间戳 */
    public static ConcurrentHashMap<String, Long> alarmMap = new ConcurrentHashMap<>();

    /** 防止 Spring 上下文刷新事件重复触发初始化 */
    private boolean isInitialized = false;

    @Autowired
    public AlarmServiceImpl(AlarmAdapter alarmAdapter, SystemUtilAdapter systemUtilAdapter) {
        this.alarmAdapter = alarmAdapter;
        this.systemUtilAdapter = systemUtilAdapter;
    }

    @Override
    public void sendAlarm(AlarmEvent alarmEvent) {
        // TODO: 实现发送告警逻辑
    }

    @Override
    public void clearAlarm(AlarmEvent alarmEvent) {
        // TODO: 实现清除告警逻辑
    }

    /**
     * Spring 上下文就绪后执行一次，清除所有历史遗留告警
     */
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

    /**
     * 查询并清除所有历史遗留告警，防止重启后告警状态不一致
     */
    public void handleHistoryAlarm() {
        // TODO: 实现历史告警处理逻辑，查询并清除所有历史遗留告警
    }

    /** 构建告警上报所需的参数 Map */
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
}
