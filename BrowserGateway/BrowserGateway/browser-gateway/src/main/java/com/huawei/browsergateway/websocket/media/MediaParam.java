package com.huawei.browsergateway.websocket.media;

import lombok.Data;

/**
 * 媒体流参数
 */
@Data
public class MediaParam {
    /**
     * 视频宽度，默认: 1920
     */
    private int width = 1920;
    
    /**
     * 视频高度，默认: 1080
     */
    private int height = 1080;
    
    /**
     * 帧率 FPS，默认: 30
     */
    private int frameRate = 30;
    
    /**
     * 采样率 Hz，默认: 44100
     */
    private int sampleRate = 44100;
    
    /**
     * 音频通道数，默认: 2
     */
    private int channels = 2;
    
    /**
     * 音频比特率 kbps，默认: 128
     */
    private int bitRate = 128;
    
    /**
     * GOP 大小，默认: 29
     */
    private int gopSize = 29;
    
    /**
     * 丢帧倍数，0-1.0，默认: 0.0
     */
    private double dropFrameMulti = 0.0;
    
    /**
     * 视频编码格式: H264/H265，默认: H264
     */
    private String codecType = "H264";
    
    /**
     * 处理器类型: webcodecs/ffmpeg，默认: ffmpeg
     */
    private String processorType = "ffmpeg";
}
