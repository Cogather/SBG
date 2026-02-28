package com.huawei.browsergateway.websocket.media;

import com.huawei.browsergateway.common.Type;
import com.huawei.browsergateway.util.encode.TlvTag;
import lombok.Data;

@Data
public class AudioResponse {
    @TlvTag(type = "int32", id = 1)
    protected Integer type;

    @TlvTag(type = "int64", id = 14)
    protected Long seq;

    @TlvTag(type = "bytes", id = 15)
    private byte[] audioData; // 音频流数据

    @TlvTag(type = "string", id = 22)
    private String sessionID;

    public AudioResponse(long seq, byte[] audioData, String sessionID) {
        this.type = Type.AUDIO;
        this.seq = seq;
        this.audioData = audioData;
        this.sessionID = sessionID;
    }
}