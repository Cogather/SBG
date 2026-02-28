package com.huawei.browsergateway.websocket.media.ffmpeg;

public class FfmpegConstants {
    public static final int BUFFER_SIZE = 65536;
    public static final int CACHE_SIZE = BUFFER_SIZE * 10;
    public static final String CONTAIN_FORMAT = "webm";
    public static final String VIDEO_ENCODE_FORMAT = "h264";
    public static final String AUDIO_ENCODE_FORMAT = "mp3";
    public static final int AV_FMT_FLAG_FLUSH_PACKETS = 0x80;

    public static final int AV_PROFILE_H264_BASELINE = 66;

}