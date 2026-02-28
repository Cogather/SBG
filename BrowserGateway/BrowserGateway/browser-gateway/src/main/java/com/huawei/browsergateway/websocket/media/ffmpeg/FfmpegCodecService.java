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

public class FfmpegCodecService  {
    private static final long APR_USEC_PER_SEC = 1000000L;
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

    public boolean init(Pointer opaque, Read_packet_Pointer_BytePointer_int readPacket
            , Write_packet_Pointer_BytePointer_int videoWritePacket
            , Write_packet_Pointer_BytePointer_int audioWritePacket) {
        try {
            this.initInputContext(opaque, readPacket);
            boolean videoInit = videoCodecCtx.init(mediaParam, this.inFormatCtx, videoWritePacket, opaque);
            if (!videoInit) {
                log.error("videoCodecCtx init failed");
                return false;
            }
            boolean audioInit = audioCodecCtx.init(mediaParam, this.inFormatCtx, audioWritePacket, opaque);
            if (!audioInit) {
                log.error("audioCodecCtx init failed");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("ffmpeg init failed", e);
            this.close();
            return false;
        }
    }

    public void start()  {
        log.info("start to handle ffmpeg stream.");
        while (true) {
            int ret = avformat.av_read_frame(this.inFormatCtx, this.packet);
            if (ret == avutil.AVERROR_EOF()) {
                log.info("av_read_frame AVERROR_EOF");
                this.close();
                return;
            }
            if (ret < 0) {
                log.error("av_read_frame failed, ret: {}", ret);
                this.close();
                return;
            }

            int index = this.packet.stream_index();
            CodecProcessor codecCtx = null;
            if (index == this.videoCodecCtx.getStreamIndex()) {
                codecCtx = this.videoCodecCtx;
            } else if (index == this.audioCodecCtx.getStreamIndex()) {
                codecCtx = this.audioCodecCtx;
            }
            if (codecCtx != null) {
                try {
                    codecCtx.streamCodec(this.packet);
                } catch (RuntimeException e) {
                    String type = index == this.videoCodecCtx.getStreamIndex() ? "video" : "audio";
                    log.error("streamCodec failed, type:{}",type ,e);
                    this.close();
                    throw e;
                }
            }
            avcodec.av_packet_unref(this.packet);
        }


    }

    public void close() {
        this.videoCodecCtx.close();
        this.audioCodecCtx.close();

        if (this.inBuffer != null) {
            avutil.av_free(this.inBuffer);
            this.inBuffer = null;
        }

        if (this.inFormatCtx != null) {
            avformat.avformat_close_input(this.inFormatCtx);
            this.inFormatCtx = null;
        }

        if (this.inIOCtx != null) {
            avformat.avio_context_free(this.inIOCtx);
            this.inIOCtx = null;
        }

        if (this.packet != null) {
            avcodec.av_packet_free(this.packet);
            this.packet = null;
        }
    }

    private void initInputContext(Pointer opaque, Read_packet_Pointer_BytePointer_int readPacket) {
        this.packet = avcodec.av_packet_alloc();
        if (this.packet == null) {
            log.error("av_packet_alloc failed");
            throw new RuntimeException("av_packet_alloc failed");
        }

        this.inBuffer = new BytePointer(avutil.av_malloc(FfmpegConstants.BUFFER_SIZE));
        AVIOContext avioContext = avformat.avio_alloc_context(this.inBuffer, FfmpegConstants.BUFFER_SIZE, 0, opaque, readPacket
                , null, null);
        if (avioContext == null) {
            log.error("avio_alloc_context failed");
            throw new RuntimeException("avio_alloc_context failed");
        }
        this.inIOCtx = avioContext;

        AVFormatContext context = avformat.avformat_alloc_context();
        if (context == null) {
            log.error("avformat_alloc_context failed");
            throw new RuntimeException("avformat_alloc_context failed");
        }
        AVInputFormat format = avformat.av_find_input_format(FfmpegConstants.CONTAIN_FORMAT);
        if (format == null) {
            log.error("av_find_input_format failed");
            throw new RuntimeException("av_find_input_format failed");
        }
        context.iformat(format);
        context.pb(this.inIOCtx);
        context.flags(context.flags() | FfmpegConstants.AV_FMT_FLAG_FLUSH_PACKETS);
        int ret = avformat.avformat_open_input(context, (BytePointer) null, format, null);
        if (ret < 0) {
            log.error("avformat_open_input failed, ret: {}", ret);
            throw new RuntimeException("avformat_open_input failed");
        }
        context.max_analyze_duration(APR_USEC_PER_SEC);
        this.inFormatCtx = context;

    }
}