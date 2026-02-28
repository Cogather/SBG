package com.huawei.browsergateway.websocket.media.webcodecs;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.websocket.media.AudioResponse;
import com.huawei.browsergateway.websocket.media.MediaParam;
import com.huawei.browsergateway.websocket.media.MediaStreamProcessor;
import com.huawei.browsergateway.websocket.media.VideoResponse;

import java.util.concurrent.ExecutorService;


public class WebCodecsStreamProcessor implements MediaStreamProcessor {
    private final MediaClientSet clients;
    private final String userId;
    private static final int VIDEO_TYPE = 1;
    private static final int AUDIO_TYPE = 2;
    private final ExecutorService videoThread = ThreadUtil.newSingleExecutor();
    private final ExecutorService audioThread = ThreadUtil.newSingleExecutor();
    private long videoSeq = 0L;
    private long audioSeq = 0L;

    public WebCodecsStreamProcessor(MediaClientSet clients, String userId) {
        this.clients = clients;
        this.userId = userId;
    }

    @Override
    public void init(MediaParam initParam) {

    }

    @Override
    public void processMediaStream(byte[] data) {
        byte type = data[0];
        if (type == VIDEO_TYPE) {
            byte frameType = data[1];
            byte[] videoData = ArrayUtil.sub(data, 2, data.length);
            long seq = ++videoSeq;

            VideoResponse response = new VideoResponse(seq, videoData, userId, frameType);
            Client client = clients.get(userId);
            if (client != null) {
                this.videoThread.submit(() -> client.send(response));
            }
            return;
        }

        if (type == AUDIO_TYPE) {
            byte[] audioData = ArrayUtil.sub(data, 1, data.length);
            long seq = ++audioSeq;
            AudioResponse response = new AudioResponse(seq, audioData, userId);
            Client client = clients.get(userId);
            if (client != null) {
                this.audioThread.submit(() -> client.send(response));
            }
        }

    }

    @Override
    public void close() {
        if (!this.videoThread.isShutdown()) {
            this.videoThread.shutdown();
        }
        if (!this.audioThread.isShutdown()) {
            this.audioThread.shutdown();
        }
    }
}