package com.huawei.browsergateway.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警枚举
 */
@AllArgsConstructor
@Getter
@ToString
public enum AlarmEnum {
    /** 创建插件失败 */
    ALARM_300030("300030", "Failed to create plugin"),
    /** 创建用户界面失败 */
    ALARM_300031("300031", "Failed to create user interface"),
    /** Pod不健康 */
    ALARM_300032("300032", "Pod is not healthy"),
    /** 创建浏览器失败 */
    ALARM_300033("300033", "Failed to create browser");

    /** 告警ID */
    private final String alarmId;
    /** 告警名称 */
    private final String alarmName;

    /**
     * 根据告警ID获取告警枚举
     *
     * @param alarmId 告警ID
     * @return 对应的告警枚举
     * @throws IllegalArgumentException 告警ID不存在时抛出
     */
    public static AlarmEnum getAlarmNameById(String alarmId) {
        for (AlarmEnum alarmCodeEnum : AlarmEnum.values()) {
            if (alarmCodeEnum.getAlarmId().equals(alarmId)) {
                return alarmCodeEnum;
            }
        }
        throw new IllegalArgumentException("invalid mode:" + alarmId);
    }

    /**
     * 获取所有告警ID，以 & 连接
     *
     * @return 所有告警ID拼接字符串
     */
    public static String getAllCodes() {
        List<AlarmEnum> alarmCodeEnums = Arrays.asList(values());
        return alarmCodeEnums.stream()
                .map(AlarmEnum::getAlarmId)
                .collect(Collectors.joining("&"));
    }
}
