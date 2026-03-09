package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.AlarmAdapter;
import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义告警适配器实现
 * 用于外网环境，不依赖CSP SDK
 */
public class CustomAlarmAdapter implements AlarmAdapter {
    private static final Logger log = LogManager.getLogger(CustomAlarmAdapter.class);
    private static final Integer ONE_MINUTE = 10 * 60 * 1000;
    private static final ConcurrentHashMap<String, Long> alarmMap = new ConcurrentHashMap<>();
    private final com.huawei.browsergateway.util.DeployUtil deployUtil;

    /**
     * 默认构造函数
     */
    public CustomAlarmAdapter() {
        this.deployUtil = new com.huawei.browsergateway.util.DeployUtil();
    }

    /**
     * 构造函数,用于依赖注入
     * @param deployUtil 部署工具类
     */
    public CustomAlarmAdapter(com.huawei.browsergateway.util.DeployUtil deployUtil) {
        this.deployUtil = deployUtil;
    }

    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 告警去重
        if (type == AlarmType.GENERATE &&
            System.currentTimeMillis() - alarmMap.getOrDefault(alarmId, 0L) < ONE_MINUTE) {
            log.info("An alarm was already reported within 10 minute; skipping this operation.");
            return false;
        }

        // 外网环境只记录日志，不发送告警
        log.info("Alarm (external environment) - ID: {}, Type: {}, Parameters: {}",
                alarmId, type, parameters);

        if (type == AlarmType.GENERATE) {
            alarmMap.put(alarmId, System.currentTimeMillis());
        } else if (type == AlarmType.CLEAR) {
            alarmMap.remove(alarmId);
        }

        return true;
    }

    @Override
    public boolean clearAlarm(String alarmId) {
        if (!alarmMap.containsKey(alarmId)) {
            return true;
        }
        return sendAlarm(alarmId, AlarmType.CLEAR, null);
    }

    @Override
    public int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry) {
        int successCount = 0;
        for (AlarmRequest request : alarms) {
            boolean success = sendAlarm(request.getAlarmId(), request.getType(), request.getParameters());
            if (success) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds) {
        // 外网环境返回空列表
        log.info("Query historical alarms skipped (external environment)");
        return new ArrayList<>();
    }
}