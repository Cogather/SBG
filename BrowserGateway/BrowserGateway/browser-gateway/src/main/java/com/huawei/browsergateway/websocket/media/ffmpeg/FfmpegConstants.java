package com.huawei.browsergateway.websocket.media.ffmpeg;

/**
 * FFmpeg 编解码常量定义
 */
public final class FfmpegConstants {

    /** 缓冲区大小：64KB */
    public static final int BUFFER_SIZE = 65536;

    /** 缓存大小：640KB (10倍缓冲区) */
    public static final int CACHE_SIZE = BUFFER_SIZE * 10;

    /** 容器格式 */
    public static final String CONTAIN_FORMAT = "webm";

    /** 视频编码格式 */
    public static final String VIDEO_ENCODE_FORMAT = "h264";

    /** 音频编码格式 */
    public static final String AUDIO_ENCODE_FORMAT = "mp3";

    /** AV格式标志：刷新数据包 */
    public static final int AV_FMT_FLAG_FLUSH_PACKETS = 0x80;

    /** H264 Baseline Profile */
    public static final int AV_PROFILE_H264_BASELINE = 66;

    /** 音频通道布局：立体声 */
    public static final int CHANNEL_LAYOUT_STEREO = 3;

    /** 音频通道布局：单声道 */
    public static final int CHANNEL_LAYOUT_MONO = 4;

    /** 微秒每秒 */
    public static final long MICROSECONDS_PER_SECOND = 1000000L;

    /** 私有构造函数，防止实例化 */
    private FfmpegConstants() {
        throw new AssertionError("常量类不应被实例化");
    }
}
