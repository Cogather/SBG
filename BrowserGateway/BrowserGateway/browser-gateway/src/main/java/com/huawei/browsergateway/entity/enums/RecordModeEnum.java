package com.huawei.browsergateway.entity.enums;

import lombok.Getter;

/**
 * 录制模式枚举
 */
@Getter
public enum RecordModeEnum {
    /** FFmpeg录制模式 */
    FFMPEG(0, "ffmpeg"),
    /** WebCodecs录制模式 */
    WEBCODECS(1, "webcodecs"),
    ;

    /** 模式值 */
    private final int mode;
    /** 模式名称 */
    private final String name;

    RecordModeEnum(int mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    /**
     * 根据模式值获取模式名称
     *
     * @param mode 模式值
     * @return 模式名称
     * @throws IllegalArgumentException 模式值不存在时抛出
     */
    public static String getRecordNameByMode(int mode) {
        for (RecordModeEnum recordModeEnum : RecordModeEnum.values()) {
            if (recordModeEnum.getMode() == mode) {
                return recordModeEnum.getName();
            }
        }
        throw new IllegalArgumentException("invalid record mode:" + mode);
    }
}
