package com.huawei.browsergateway.websocket.media.ffmpeg;

import cn.hutool.core.thread.ThreadUtil;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.websocket.media.AudioResponse;
import com.huawei.browsergateway.websocket.media.MediaParam;
import com.huawei.browsergateway.websocket.media.MediaStreamProcessor;
import com.huawei.browsergateway.websocket.media.VideoResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

public class FfmpegStreamProcessor implements MediaStreamProcessor {
    private static final Logger log = LogManager.getLogger(FfmpegStreamProcessor.class);
    private final Read_packet_Pointer_BytePointer_int readPacket;
    private final Write_packet_Pointer_BytePointer_int writeVideoPacket;
    private final Write_packet_Pointer_BytePointer_int writeAudioPacket;
    private final ExecutorService executor = ThreadUtil.newSingleExecutor();
    private final MediaClientSet clientSet;
    private final ByteBuffer cacheBuffer;
    private final Object lock = new Object();
    private final String userId;
    private final CharPointer userIdPtr;

    private FfmpegCodecService ffmpegCodecService;
    private MediaParam mediaParam;
    private long videoSeq = 0L;
    private long audioSeq = 0L;
    private boolean isEOF = false;

    public FfmpegStreamProcessor(MediaClientSet clientSet, String userId) {
        this.clientSet = clientSet;
        this.userId = userId;
        this.userIdPtr = new CharPointer(userId);
        this.cacheBuffer = ByteBuffer.allocateDirect(FfmpegConstants.CACHE_SIZE);
        this.readPacket = new Read_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaque, BytePointer buf, int bufSize) {
                return readHook(buf, bufSize);
            }
        };

        this.writeVideoPacket = new Write_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaque, BytePointer buf, int bufSize) {
                return writeVideoHook(buf, bufSize);
            }
        };

        this.writeAudioPacket = new Write_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaque, BytePointer buf, int bufSize) {
                return writeAudioHook(buf, bufSize);
            }
        };
    }

    @Override
    public void init(MediaParam initParam) {
        this.mediaParam = initParam;
        this.ffmpegCodecService = new FfmpegCodecService(initParam);
        long startTime = System.currentTimeMillis();
        executor.submit(() -> {
            while (this.cacheBuffer.position() <= 0) {
                if (System.currentTimeMillis() - startTime > 120000) {
                    this.isEOF = true;
                    log.info("Not connected for a long time, user:{}", userId);
                    return;
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for cacheBuffer data", e);
                    return;
                }
            }

            log.info("wait for connection success, user:{}", userId);
            boolean initResult = this.ffmpegCodecService.init(this.userIdPtr, this.readPacket, this.writeVideoPacket
                    , this.writeAudioPacket);
            if (!initResult) {
                log.error("init ffmpeg failed, param:{}", this.mediaParam);
                return;
            }
            this.ffmpegCodecService.start();
        });
    }

    @Override
    public void processMediaStream(byte[] data) {
        if (this.isEOF) {
            return;
        }
        int offset = 0;
        int length = data.length;
        synchronized (this.lock) {
            if (this.cacheBuffer.remaining() >= length) {
                this.cacheBuffer.put(data, offset, length);
                return;
            }
            log.error("insufficient cache space, user:{}, remaining:{}, data length:{}", this.userId
                    , this.cacheBuffer.remaining(), length);
        }
    }

    @Override
    public void close() {
        this.isEOF = true;
        if (!this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }

    private int readHook(BytePointer buf, int bufSize) {
        try {
            if (this.isEOF) {
                return avutil.AVERROR_EOF;
            }
            while (this.cacheBuffer.position() <= 0) {
                log.trace("read packet wait...... current position:{}", this.cacheBuffer.position());
                Thread.sleep(50L);
//                return 0;
            }
            int len = Math.min(bufSize, this.cacheBuffer.position());
            byte[] data = new byte[len];
            synchronized (this.lock) {
                this.cacheBuffer.flip();
                this.cacheBuffer.get(data);
                this.cacheBuffer.compact();
            }
            buf.put(data, 0, len);
            return len;
        } catch (Exception e) {
            return -1;
        }
    }

    private int writeVideoHook(BytePointer buf, int bufSize) {
        try {
            if (this.isEOF) {
                return avutil.AVERROR_EOF;
            }

            byte[] data = new byte[bufSize];
            buf.get(data, 0, bufSize);
            int isKeyFrame = 0;

            if (this.videoSeq % ((long) this.mediaParam.getGopSize()) == 0) {
                isKeyFrame = 1;
            }

            if (this.videoSeq < this.mediaParam.getDropFrameMulti() * this.mediaParam.getFrameRate()) {
                ++this.videoSeq;
                return bufSize;
            }

            VideoResponse videoResponse = new VideoResponse(++this.videoSeq, data, this.userId, isKeyFrame);
            Client client = clientSet.get(this.userId);
            if (client != null) {
                client.send(videoResponse);
            }
            return bufSize;
        } catch (Exception e) {
            return -1;
        }
    }

    private int writeAudioHook(BytePointer buf, int bufSize) {
        try {
            if (this.isEOF) {
                return avutil.AVERROR_EOF;
            }
            byte[] data = new byte[bufSize];
            buf.get(data, 0, bufSize);
            AudioResponse audioResponse = new AudioResponse(++this.audioSeq, data, this.userId);
            Client client = clientSet.get(this.userId);
            if (client != null) {
                client.send(audioResponse);
            }
            return bufSize;
        } catch (Exception e) {
            return -1;
        }
    }
}