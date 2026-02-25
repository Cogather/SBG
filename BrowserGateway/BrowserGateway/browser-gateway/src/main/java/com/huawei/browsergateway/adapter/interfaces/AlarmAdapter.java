package com.huawei.browsergateway.adapter.interfaces;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;

import java.util.List;
import java.util.Map;

/**
 * 告警适配器接口
 * 职责：处理告警的发送、清除和历史查询
 */
public interface AlarmAdapter {
    
    /**
     * 发送告警
     * @param alarmId 告警ID
     * @param type 告警类型
     * @param parameters 告警参数
     * @return 发送是否成功
     */
    boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters);
    
    /**
     * 清除告警
     * @param alarmId 告警ID
     * @return 清除是否成功
     */
    boolean clearAlarm(String alarmId);
    
    /**
     * 批量发送告警（支持重试）
     * @param alarms 告警列表
     * @param maxRetry 最大重试次数
     * @return 成功发送的告警数量
     */
    int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry);
    
    /**
     * 查询历史告警
     * @param alarmIds 告警ID列表
     * @return 历史告警信息
     */
    List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds);
    
    /**
     * 告警类型枚举
     */
    enum AlarmType {
        GENERATE,
        CLEAR
    }
}
