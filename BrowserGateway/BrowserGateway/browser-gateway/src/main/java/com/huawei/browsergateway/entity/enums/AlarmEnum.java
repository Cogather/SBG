package com.huawei.browsergateway.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@ToString
public enum AlarmEnum {
    ALARM_300030("300030", "Failed to create plugin"),
    ALARM_300031("300031", "Failed to create user interface"),
    ALARM_300032("300032", "Pod is not healthy"),
    ALARM_300033("300033", "Failed to create browser");

    private final String alarmId;
    private final String alarmName;


    public static AlarmEnum getAlarmNameById(String alarmId) {
        for (AlarmEnum alarmCodeEnum : AlarmEnum.values()) {
            if (alarmCodeEnum.getAlarmId().equals(alarmId)) {
                return alarmCodeEnum;
            }
        }
        throw new IllegalArgumentException("invalid mode:" + alarmId);
    }


    public static String getAllCodes() {
        List<AlarmEnum> alarmCodeEnums = Arrays.asList(values());
        return alarmCodeEnums.stream()
                .map(AlarmEnum::getAlarmId)
                .collect(Collectors.joining("&"));
    }

}
