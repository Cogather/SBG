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
import org.bytedeco.ffmpeg.avutil.AVAudioFifo;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

public class AudioCodecProcessor implements CodecProcessor {
    private static final Logger log = LogManager.getLogger(AudioCodecProcessor.class);

    private AVCodecContext decodeCtx;
    private AVCodecContext encodeCtx;
    private AVFormatContext outFormatCtx;
    private AVFrame decodeFrame;
    private AVPacket encodePacket;
    private BytePointer outBuffer;
    private AVIOContext outIOCtx;
    private AVAudioFifo audioFifo;
    private AVFrame audioFrame;

    private int streamIndex;
    private long pts;

    @Override
    public boolean init(MediaParam mediaParam, AVFormatContext inFormatCtx
            , Write_packet_Pointer_BytePointer_int writePacket, Pointer userIdPtr) {
        try {
            this.initDecCtx(inFormatCtx);
            this.initEncCtx(writePacket, mediaParam, userIdPtr);
            this.pts = 0L;
            this.initAudioFiFo();
            return true;
        } catch (Exception e) {
            this.close();
            log.error("Failed to init audio codec context", e);
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

            // Encode the frame
            this.encodeAudioFrame(frame);
        }
    }

    @Override
    public void close() {
        try {
            this.flush();
        } catch (Exception e) {
            log.error("Failed to flush audio codec context", e);
        }
        if (this.audioFifo != null) {
            avutil.av_audio_fifo_free(this.audioFifo);
            this.audioFifo = null;
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

        if (this.audioFrame != null) {
            avutil.av_frame_free(this.audioFrame);
            this.audioFrame = null;
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
        this.encodeAudioFrame(null);
        int ret = avcodec.avcodec_send_frame(this.encodeCtx, null);
        while (ret >= 0) {
            ret = avcodec.avcodec_receive_packet(this.encodeCtx, this.encodePacket);
            if (ret == org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN()) {
                break;
            }
            if (ret == avutil.AVERROR_EOF) {
                break;
            }
            this.encodePacket.stream_index(0);
            avcodec.av_packet_rescale_ts(this.encodePacket, this.encodeCtx.time_base(), this.encodeCtx.pkt_timebase());
            ret = avformat.av_write_frame(this.outFormatCtx, this.encodePacket);
            avcodec.av_packet_unref(this.encodePacket);
        }
        avformat.av_write_trailer(this.outFormatCtx);
    }

    private void initDecCtx(AVFormatContext inFormatCtx) {
        this.streamIndex = avformat.av_find_best_stream(inFormatCtx, avutil.AVMEDIA_TYPE_AUDIO, -1, -1, (AVCodec) null, 0);
        if (this.streamIndex == avutil.AVERROR_DECODER_NOT_FOUND || this.streamIndex == avutil.AVERROR_STREAM_NOT_FOUND) {
            throw new RuntimeException("Cannot find audio stream");
        }
        AVStream stream = inFormatCtx.streams(streamIndex);
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
        avcodec.avcodec_open2(codecCtx, decoder, (AVDictionary) null);

        this.decodeCtx = codecCtx;
        this.decodeFrame = avutil.av_frame_alloc();
        avformat.av_dump_format(inFormatCtx, 0, (BytePointer) null, 0);
    }
    private void initEncCtx(Write_packet_Pointer_BytePointer_int writePacket, MediaParam mediaParam, Pointer userIdPtr) {
        // 分配输出格式上下文
        this.outFormatCtx = new AVFormatContext(null);
        avformat.avformat_alloc_output_context2(this.outFormatCtx, null, FfmpegConstants.AUDIO_ENCODE_FORMAT
                , null);

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
        AVCodec codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_MP3);
        AVCodecContext encodeCtx = avcodec.avcodec_alloc_context3(codec);

        setEncodeContext(encodeCtx, mediaParam);
        int result = avcodec.avcodec_open2(encodeCtx, codec, (AVDictionary) null);
        if (result < 0) {
            log.error("Cannot open {} encoder for audio stream, err code: {}", codec.name(), result);
            throw new RuntimeException("Cannot open " + codec.name() + " encoder for audio stream");
        }

        avcodec.avcodec_parameters_from_context(stream.codecpar(), encodeCtx);

        this.encodeCtx = encodeCtx;
        this.encodePacket = avcodec.av_packet_alloc();
        avformat.avformat_write_header(this.outFormatCtx, (PointerPointer) null);
    }

    private void setEncodeContext(AVCodecContext encodeCtx, MediaParam mediaParam) {
        encodeCtx.pkt_timebase(this.decodeCtx.pkt_timebase());
        encodeCtx.sample_rate(this.decodeCtx.sample_rate());

        // todo:定义常量
        if (mediaParam.getChannels() == 2) {
            encodeCtx.channel_layout(3);
        } else {
            encodeCtx.channel_layout(4);
        }

        encodeCtx.channels(mediaParam.getChannels());
        encodeCtx.sample_fmt(this.decodeCtx.sample_fmt());
        encodeCtx.time_base(this.decodeCtx.time_base());
    }

    private void initAudioFiFo() {
        AVCodecContext enCtx = this.encodeCtx;
        this.audioFifo = avutil.av_audio_fifo_alloc(enCtx.sample_fmt(), enCtx.channels(), enCtx.frame_size());
        if (this.audioFifo == null) {
            throw new RuntimeException("Cannot allocate audio FIFO");
        }
        this.audioFrame = avutil.av_frame_alloc();
        if (this.audioFrame == null) {
            throw new RuntimeException("Cannot allocate audio frame");
        }
        this.audioFrame.nb_samples(enCtx.frame_size());
        this.audioFrame.sample_rate(enCtx.sample_rate());
        this.audioFrame.channel_layout(enCtx.channel_layout());
        this.audioFrame.channels(enCtx.channels());
        this.audioFrame.format(enCtx.sample_fmt());
        if (avutil.av_frame_get_buffer(this.audioFrame, 0) < 0) {
            throw new RuntimeException("Cannot allocate audio frame buffer");
        }
    }

    private void encodeAudioFrame(AVFrame frame) {
        if (frame != null) {
            if (frame.pts() != avutil.AV_NOPTS_VALUE) {
                frame.pts(avutil.av_rescale_q(frame.pts(), frame.time_base(), this.encodeCtx.time_base()));
            }

            int cacheSize = avutil.av_audio_fifo_size(this.audioFifo);
            avutil.av_audio_fifo_realloc(this.audioFifo, cacheSize + frame.nb_samples());
            avutil.av_audio_fifo_write(this.audioFifo, frame.data(), frame.nb_samples());
            avutil.av_frame_make_writable(this.audioFrame);
        }

        while (avutil.av_audio_fifo_size(this.audioFifo) > this.encodeCtx.frame_size()) {
            if (avutil.av_audio_fifo_read(this.audioFifo, this.audioFrame.data(), this.encodeCtx.frame_size()) < 0) {
                log.error("Error reading from audio FIFO");
                throw new RuntimeException("Error reading from audio FIFO");
            }
            this.audioFrame.pts(this.pts);
            this.pts += this.encodeCtx.frame_size();
            int ret = avcodec.avcodec_send_frame(this.encodeCtx, this.audioFrame);

            while (ret >= 0) {
                ret = avcodec.avcodec_receive_packet(this.encodeCtx, this.encodePacket);
                if (ret == avutil.AVERROR_EOF || ret == org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN()) {
                    break;
                } else if (ret < 0) {
                    log.error("Error during encoding, err code:{}", ret);
                    throw new RuntimeException("Error during encoding");
                }
                this.encodePacket.stream_index(0);
                avcodec.av_packet_rescale_ts(this.encodePacket, this.encodeCtx.time_base()
                        , this.encodeCtx.pkt_timebase());
                avformat.av_write_frame(this.outFormatCtx, this.encodePacket);
                avcodec.av_packet_unref(this.encodePacket);
            }
        }
    }

}
