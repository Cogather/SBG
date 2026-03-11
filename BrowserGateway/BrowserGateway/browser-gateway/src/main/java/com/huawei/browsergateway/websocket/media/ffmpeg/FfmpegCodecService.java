package com.huawei.browsergateway.websocket.media.ffmpeg;

import com.huawei.browsergateway.websocket.media.MediaParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

/**
 * FFmpeg 编解码服务
 * 负责音视频流的解复用、解码、编码和复用
 */
public class FfmpegCodecService {

    private static final Logger log = LogManager.getLogger(FfmpegCodecService.class);

    private final MediaParam mediaParam;
    private final CodecProcessor videoCodecCtx;
    private final CodecProcessor audioCodecCtx;

    private AVFormatContext inFormatCtx;
    private AVIOContext inIOCtx;
    private AVPacket packet;
    private BytePointer inBuffer;

    public FfmpegCodecService(MediaParam mediaParam) {
        this.mediaParam = mediaParam;
        this.videoCodecCtx = new VideoCodecProcessor();
        this.audioCodecCtx = new AudioCodecProcessor();
    }

    /**
     * 初始化编解码服务
     */
    public boolean init(Pointer opaque, Read_packet_Pointer_BytePointer_int readPacket,
                        Write_packet_Pointer_BytePointer_int videoWritePacket,
                        Write_packet_Pointer_BytePointer_int audioWritePacket) {
        try {
            initInputContext(opaque, readPacket);

            if (!initCodecProcessor(videoCodecCtx, videoWritePacket, opaque, "video")) {
                return false;
            }

            if (!initCodecProcessor(audioCodecCtx, audioWritePacket, opaque, "audio")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("ffmpeg init failed", e);
            close();
            return false;
        }
    }

    /**
     * 初始化编解码处理器
     */
    private boolean initCodecProcessor(CodecProcessor processor,
                                       Write_packet_Pointer_BytePointer_int writePacket,
                                       Pointer opaque, String type) {
        boolean success = processor.init(mediaParam, inFormatCtx, writePacket, opaque);
        if (!success) {
            log.error("{} codec init failed", type);
        }
        return success;
    }

    /**
     * 开始处理媒体流
     */
    public void start() {
        log.info("start to handle ffmpeg stream.");

        while (processNextFrame()) {
            // 持续处理帧直到结束或出错
        }
    }

    /**
     * 处理下一帧
     *
     * @return true 继续处理，false 停止处理
     */
    private boolean processNextFrame() {
        int ret = avformat.av_read_frame(inFormatCtx, packet);

        if (ret == avutil.AVERROR_EOF()) {
            log.info("av_read_frame AVERROR_EOF");
            close();
            return false;
        }

        if (ret < 0) {
            log.error("av_read_frame failed, ret: {}", ret);
            close();
            return false;
        }

        processPacket();
        avcodec.av_packet_unref(packet);
        return true;
    }

    /**
     * 处理数据包
     */
    private void processPacket() {
        int streamIndex = packet.stream_index();
        CodecProcessor processor = getProcessorForStream(streamIndex);

        if (processor != null) {
            try {
                processor.streamCodec(packet);
            } catch (RuntimeException e) {
                String type = (streamIndex == videoCodecCtx.getStreamIndex()) ? "video" : "audio";
                log.error("streamCodec failed, type:{}", type, e);
                close();
                throw e;
            }
        }
    }

    /**
     * 根据流索引获取对应的处理器
     */
    private CodecProcessor getProcessorForStream(int streamIndex) {
        if (streamIndex == videoCodecCtx.getStreamIndex()) {
            return videoCodecCtx;
        } else if (streamIndex == audioCodecCtx.getStreamIndex()) {
            return audioCodecCtx;
        }
        return null;
    }

    /**
     * 关闭并释放所有资源
     */
    public void close() {
        videoCodecCtx.close();
        audioCodecCtx.close();

        releaseResource(inBuffer, "inBuffer", avutil::av_free);
        releaseResource(inFormatCtx, "inFormatCtx", avformat::avformat_close_input);
        releaseResource(inIOCtx, "inIOCtx", avformat::avio_context_free);
        releaseResource(packet, "packet", avcodec::av_packet_free);
    }

    /**
     * 释放单个资源
     */
    private <T> void releaseResource(T resource, String name, ResourceReleaser<T> releaser) {
        if (resource != null) {
            releaser.release(resource);
        }
    }

    /**
     * 资源释放器函数式接口
     */
    @FunctionalInterface
    private interface ResourceReleaser<T> {
        void release(T resource);
    }

    /**
     * 初始化输入上下文
     */
    private void initInputContext(Pointer opaque, Read_packet_Pointer_BytePointer_int readPacket) {
        packet = allocatePacket();
        inBuffer = allocateBuffer();
        inIOCtx = createIOContext(inBuffer, opaque, readPacket);
        inFormatCtx = createFormatContext(inIOCtx);
    }

    /**
     * 分配数据包
     */
    private AVPacket allocatePacket() {
        AVPacket pkt = avcodec.av_packet_alloc();
        if (pkt == null) {
            throw new RuntimeException("av_packet_alloc failed");
        }
        return pkt;
    }

    /**
     * 分配缓冲区
     */
    private BytePointer allocateBuffer() {
        return new BytePointer(avutil.av_malloc(FfmpegConstants.BUFFER_SIZE));
    }

    /**
     * 创建 IO 上下文
     */
    private AVIOContext createIOContext(BytePointer buffer, Pointer opaque,
                                        Read_packet_Pointer_BytePointer_int readPacket) {
        AVIOContext ctx = avformat.avio_alloc_context(buffer, FfmpegConstants.BUFFER_SIZE,
                0, opaque, readPacket, null, null);
        if (ctx == null) {
            throw new RuntimeException("avio_alloc_context failed");
        }
        return ctx;
    }

    /**
     * 创建格式上下文
     */
    private AVFormatContext createFormatContext(AVIOContext ioCtx) {
        AVFormatContext ctx = avformat.avformat_alloc_context();
        if (ctx == null) {
            throw new RuntimeException("avformat_alloc_context failed");
        }

        AVInputFormat format = avformat.av_find_input_format(FfmpegConstants.CONTAIN_FORMAT);
        if (format == null) {
            throw new RuntimeException("av_find_input_format failed");
        }

        ctx.iformat(format);
        ctx.pb(ioCtx);
        ctx.flags(ctx.flags() | FfmpegConstants.AV_FMT_FLAG_FLUSH_PACKETS);

        int ret = avformat.avformat_open_input(ctx, (BytePointer) null, format, null);
        if (ret < 0) {
            log.error("avformat_open_input failed, ret: {}", ret);
            throw new RuntimeException("avformat_open_input failed");
        }

        ctx.max_analyze_duration(FfmpegConstants.MICROSECONDS_PER_SECOND);
        return ctx;
    }
}
