package com.huawei.browsergateway.entity.browser;

import lombok.Data;

/**
 * Chrome浏览器配置
 */
@Data
public class ChromeConfig {
    /** 厂商 */
    private String manufacturer;
    /** 机型 */
    private String model;
    /** 国家/地区 */
    private String country;
    /** 应用帧率 */
    private int appFrameRate;
    /** 视频帧率 */
    private int videoFrameRate;
    /** 应用码率 */
    private int appBitRate;
    /** 视频码率 */
    private int videoBitRate;
    /** 音频采样率 */
    private int sampleRate;
    /** 声道数 */
    private int channels;
    /** 机器类型 */
    private int machineType;
    /** 编解码标识 */
    private String ffCode;
    /** 分辨率 */
    private String resolution;
    /** 录制模式 */
    private int recordMode;
}
