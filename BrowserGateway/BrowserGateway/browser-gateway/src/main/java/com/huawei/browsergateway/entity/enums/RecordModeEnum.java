package com.huawei.browsergateway.entity.enums;

import lombok.Getter;

@Getter
public enum RecordModeEnum {
    FFMPEG(0, "ffmpeg"),
    WEBCODECS(1, "webcodecs"),
    ;
    private final int mode;
    private final String name;

    RecordModeEnum(int mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public static String getRecordNameByMode(int mode) {
        for (RecordModeEnum recordModeEnum : RecordModeEnum.values()) {
            if (recordModeEnum.getMode() == mode) {
                return recordModeEnum.getName();
            }
        }
        throw new IllegalArgumentException("invalid record mode:" + mode);
    }
}
