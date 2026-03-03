package com.huawei.browsergateway.websocket.media.webcodecs;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.websocket.media.AudioResponse;
import com.huawei.browsergateway.websocket.media.MediaParam;
import com.huawei.browsergateway.websocket.media.MediaStreamProcessor;
import com.huawei.browsergateway.websocket.media.VideoResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;


public class WebCodecsStreamProcessor implements MediaStreamProcessor {
    private static final Logger log = LogManager.getLogger(WebCodecsStreamProcessor.class);
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
        log.info("[WebCodecsStreamProcessor] 处理媒体流数据: userId={}, 数据大小={}字节", userId, data.length);
        
        // 检查数据格式：客户端发送格式为 [4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
        // 但当前代码期望格式为 [1字节: type][1字节: frameType][N字节: data]
        // 需要适配新的格式
        if (data.length >= 12) {
            // 解析新格式：[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
            int frameType = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            int timestamp = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
            int dataLength = ((data[8] & 0xFF) << 24) | ((data[9] & 0xFF) << 16) | ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);
            
            log.info("[WebCodecsStreamProcessor] 解析视频帧: userId={}, 帧类型={}, 时间戳={}, 数据长度={}字节", 
                     userId, frameType, timestamp, dataLength);
            
            // 提取实际视频数据（跳过12字节头部）
            byte[] videoData = ArrayUtil.sub(data, 12, data.length);
            
            // 转换为期望的帧类型格式（1=I帧，2=P帧）
            byte convertedFrameType = (byte) frameType;
            
            long seq = ++videoSeq;
            VideoResponse response = new VideoResponse(seq, videoData, userId, convertedFrameType);
            Client client = clients.get(userId);
            if (client != null) {
                log.info("[WebCodecsStreamProcessor] 发送视频响应: userId={}, seq={}, 帧类型={}, 数据大小={}字节", 
                         userId, seq, convertedFrameType, videoData.length);
                this.videoThread.submit(() -> client.send(response));
            } else {
                log.warn("[WebCodecsStreamProcessor] 客户端不存在: userId={}", userId);
            }
            return;
        }
        
        // 兼容旧格式：[1字节: type][1字节: frameType][N字节: data]
        byte type = data[0];
        if (type == VIDEO_TYPE) {
            byte frameType = data[1];
            byte[] videoData = ArrayUtil.sub(data, 2, data.length);
            long seq = ++videoSeq;
            
            log.info("[WebCodecsStreamProcessor] 处理视频帧(旧格式): userId={}, seq={}, 帧类型={}, 数据大小={}字节", 
                     userId, seq, frameType, videoData.length);

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
            log.info("[WebCodecsStreamProcessor] 处理音频帧: userId={}, seq={}, 数据大小={}字节", 
                     userId, seq, audioData.length);
            AudioResponse response = new AudioResponse(seq, audioData, userId);
            Client client = clients.get(userId);
            if (client != null) {
                this.audioThread.submit(() -> client.send(response));
            }
        } else {
            log.warn("[WebCodecsStreamProcessor] 未知的媒体类型: userId={}, type={}, 数据大小={}字节", 
                     userId, type, data.length);
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