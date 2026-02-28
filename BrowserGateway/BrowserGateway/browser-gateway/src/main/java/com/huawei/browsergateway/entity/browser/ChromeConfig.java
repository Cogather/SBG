package com.huawei.browsergateway.entity.browser;

import lombok.Data;

@Data
public class ChromeConfig {
    private String manufacturer;
    private String model;
    private String country;
    private int appFrameRate;
    private int videoFrameRate;
    private int appBitRate;
    private int videoBitRate;
    private int sampleRate;
    private int channels;
    private int machineType;
    private String ffCode;
    private String resolution;
    private int recordMode;
}
