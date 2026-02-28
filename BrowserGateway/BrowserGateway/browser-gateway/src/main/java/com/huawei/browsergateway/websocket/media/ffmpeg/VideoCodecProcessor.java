package com.huawei.browsergateway.websocket.media.ffmpeg;

import com.huawei.browsergateway.websocket.media.MediaParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

public class VideoCodecProcessor implements CodecProcessor {
    private static final Logger log = LogManager.getLogger(VideoCodecProcessor.class);

    private AVCodecContext decodeCtx;
    private AVCodecContext encodeCtx;
    private AVFormatContext outFormatCtx;
    private AVFrame decodeFrame;
    private AVPacket encodePacket;
    private BytePointer outBuffer;
    private AVIOContext outIOCtx;

    private int streamIndex;
    private long lastDTS;

    @Override
    public boolean init(MediaParam mediaParam, AVFormatContext inFormatCtx
            , Write_packet_Pointer_BytePointer_int writePacket, Pointer userIdPtr) {
        try {
            this.initDecCtx(inFormatCtx);
            this.initEncCtx(writePacket, mediaParam, userIdPtr);
            this.lastDTS = 0L;
            return true;
        } catch (Exception e) {
            this.close();
            log.error("Failed to init video codec context", e);
            return false;
        }
    }

    @Override
    public int getStreamIndex() {
        return this.streamIndex;
    }

    @Override
    public void streamCodec(AVPacket pkt) {
        AVCodecContext decCtx = this.decodeCtx;
        AVFrame frame = this.decodeFrame;

        int ret = avcodec.avcodec_send_packet(decCtx, pkt);
        if (ret < 0) {
            throw new RuntimeException("Error sending a packet for decoding");
        }

        while (true) {
            ret = avcodec.avcodec_receive_frame(decCtx, frame);
            if (ret == avutil.AVERROR_EOF || ret == avutil.AVERROR_EAGAIN() ) {
                break;
            } else if (ret < 0) {
                log.error("Error during decoding");
                throw new RuntimeException("Error during decoding");
            }

            // Set frame properties
            frame.pts(frame.best_effort_timestamp());
            frame.time_base(decCtx.pkt_timebase());
            frame.pict_type(avutil.AV_PICTURE_TYPE_NONE);

            // Encode the frame
            this.encodeVideoFrame(frame);
        }
    }

    @Override
    public void close() {
        try {
            this.flush();
        } catch (Exception e) {
            log.error("Failed to close video codec context", e);
        }
        if (this.outIOCtx != null) {
            avformat.avio_context_free(this.outIOCtx);
            this.outIOCtx = null;
        }

        if (this.decodeCtx != null) {
            avcodec.avcodec_free_context(this.decodeCtx);
            this.decodeCtx = null;
        }

        if (this.encodeCtx != null) {
            avcodec.avcodec_free_context(this.encodeCtx);
            this.encodeCtx = null;
        }

        if (this.outFormatCtx != null) {
            avformat.avformat_free_context(this.outFormatCtx);
            this.outFormatCtx = null;
        }

        if (this.decodeFrame != null) {
            avutil.av_frame_free(this.decodeFrame);
            this.decodeFrame = null;
        }

        if (this.encodePacket != null) {
            avcodec.av_packet_free(this.encodePacket);
            this.encodePacket = null;
        }

        if (this.outBuffer != null) {
            avutil.av_free(this.outBuffer);
            this.outBuffer = null;
        }
    }

    public void flush() {
        this.encodeVideoFrame(null);
        avformat.av_write_trailer(this.outFormatCtx);
    }

    private void initDecCtx(AVFormatContext inFormartCtx) {
        this.streamIndex = avformat.av_find_best_stream(inFormartCtx, avutil.AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodec) null, 0);
        if (streamIndex == avutil.AVERROR_DECODER_NOT_FOUND || streamIndex == avutil.AVERROR_STREAM_NOT_FOUND) {
            log.error("Cannot find video stream");
            throw new RuntimeException("Cannot find video stream");
        }

        AVStream stream = inFormartCtx.streams(streamIndex);
        AVCodec decoder = avcodec.avcodec_find_decoder(stream.codecpar().codec_id());
        if (decoder == null) {
            log.error("Failed to find decoder for stream {}", streamIndex);
            throw new RuntimeException("Failed to find decoder for stream " + streamIndex);
        }
        AVCodecContext codecCtx = avcodec.avcodec_alloc_context3(decoder);
        if (codecCtx == null) {
            log.error("Cannot allocate decoder context");
            throw new RuntimeException("Cannot allocate decoder context");
        }
        int copyResult = avcodec.avcodec_parameters_to_context(codecCtx, stream.codecpar());
        if (copyResult < 0) {
            log.error("Failed to copy decoder parameters to input decoder context");
            throw new RuntimeException("Failed to copy decoder parameters to input decoder context");
        }

        codecCtx.pkt_timebase(stream.time_base());
        codecCtx.framerate(avformat.av_guess_frame_rate(inFormartCtx, stream, null));
        avcodec.avcodec_open2(codecCtx, decoder, (AVDictionary) null);

        this.decodeCtx = codecCtx;
        this.decodeFrame = avutil.av_frame_alloc();
        avformat.av_dump_format(inFormartCtx, 0, (BytePointer) null, 0);
    }

    private void initEncCtx(Write_packet_Pointer_BytePointer_int writePacket, MediaParam mediaParam, Pointer userIdPtr) {
        // 分配输出格式上下文
        this.outFormatCtx = new AVFormatContext(null);
        avformat.avformat_alloc_output_context2(this.outFormatCtx, null, FfmpegConstants.VIDEO_ENCODE_FORMAT, "");

        // 分配缓冲区
        Pointer cachePointer = avutil.av_malloc(FfmpegConstants.BUFFER_SIZE);
        if (cachePointer == null) {
            log.error("Failed to allocate buffer");
            throw new RuntimeException("Failed to allocate buffer");
        }
        this.outBuffer = new BytePointer(cachePointer);

        // 分配AVIO上下文
        this.outIOCtx = avformat.avio_alloc_context(this.outBuffer, FfmpegConstants.BUFFER_SIZE, 1
                , userIdPtr, null, writePacket, null);
        if (outIOCtx == null) {
            log.error("Failed to allocate AVIO context");
            throw new RuntimeException("Failed to allocate AVIO context");
        }
        this.outFormatCtx.pb(this.outIOCtx);
        this.outFormatCtx.flags(outFormatCtx.flags() | FfmpegConstants.AV_FMT_FLAG_FLUSH_PACKETS);

        AVStream stream = avformat.avformat_new_stream(this.outFormatCtx, null);

        // 查找编码器
        AVCodec codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_H264);
        AVCodecContext encodeCtx = avcodec.avcodec_alloc_context3(codec);

        setEncodeContext(encodeCtx, mediaParam);
        int result = avcodec.avcodec_open2(encodeCtx, codec, (AVDictionary) null);
        if (result < 0) {
            log.error("Cannot open {} encoder for video stream", codec.name());
            throw new RuntimeException("Cannot open " + codec.name() + " encoder for video stream");
        }

        avcodec.avcodec_parameters_from_context(stream.codecpar(), encodeCtx);
        this.encodeCtx = encodeCtx;
        this.encodePacket = avcodec.av_packet_alloc();
        avformat.avformat_write_header(this.outFormatCtx, (PointerPointer) null);
    }

    private void setEncodeContext(AVCodecContext encodeCtx, MediaParam mediaParam) {
        encodeCtx.bit_rate(this.decodeCtx.height() * this.decodeCtx.width() * mediaParam.getBitRate() / 100);
        encodeCtx.height(this.decodeCtx.height());
        encodeCtx.width(this.decodeCtx.width());
        encodeCtx.sample_aspect_ratio(this.decodeCtx.sample_aspect_ratio());

        AVRational videoFrameRate = new AVRational();
        videoFrameRate.num(mediaParam.getFrameRate());
        videoFrameRate.den(1);
        encodeCtx.time_base(videoFrameRate);

        encodeCtx.pix_fmt(0);
        encodeCtx.pkt_timebase(this.decodeCtx.pkt_timebase());
        encodeCtx.gop_size(mediaParam.getGopSize());
        encodeCtx.max_b_frames(0);
        encodeCtx.profile(FfmpegConstants.AV_PROFILE_H264_BASELINE);
        avutil.av_opt_set(encodeCtx.priv_data(), "preset", "slow", 0);
    }

    private void encodeVideoFrame(AVFrame frame) {
        AVCodecContext encCtx = this.encodeCtx;
        AVPacket encPkt = this.encodePacket;
        int ret;

        if (frame != null && frame.pts() != avutil.AV_NOPTS_VALUE) {
            frame.pts(avutil.av_rescale_q(frame.pts(), frame.time_base(), encCtx.time_base()));
        }

        ret = avcodec.avcodec_send_frame(encCtx, frame);
        if (ret < 0) {
            log.error("Error during sending a frame for encoding, ret:{}", ret);
            throw new RuntimeException("Error during sending a frame for encoding");
        }

        while (true) {
            ret = avcodec.avcodec_receive_packet(encCtx, encPkt);
            if (ret == avutil.AVERROR_EOF || ret == avutil.AVERROR_EAGAIN()) {
                break;
            } else if (ret < 0) {
                log.error("Error during receiving a packet from the encoder, err code:{}", ret);
                throw new RuntimeException("Error during receiving a packet from the encoder");
            }

            encPkt.stream_index(0);
            if (this.lastDTS >= encPkt.dts()) {
                this.lastDTS++;
                encPkt.dts(this.lastDTS);
                encPkt.pts(this.lastDTS);
            }
            this.lastDTS = encPkt.dts();
            avcodec.av_packet_rescale_ts(encPkt, encCtx.time_base(), encCtx.pkt_timebase());
            avformat.av_write_frame(this.outFormatCtx, encPkt);
            avcodec.av_packet_unref(encPkt);
        }
    }
}