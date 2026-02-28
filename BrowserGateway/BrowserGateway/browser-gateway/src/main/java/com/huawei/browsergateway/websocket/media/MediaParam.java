package com.huawei.browsergateway.websocket.media;

import lombok.Data;

@Data
public class MediaParam {
    private Long bitRate;
    private Integer channels;
    private Integer sampleRate;
    private Integer frameRate;
    private Double dropFrameMulti;
    private Integer gopSize;
}
