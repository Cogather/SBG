package com.huawei.browsergateway.websocket.media;

import com.huawei.browsergateway.common.Type;
import com.huawei.browsergateway.util.encode.TlvTag;
import lombok.Data;

@Data
public class VideoResponse{

    @TlvTag(type = "int32", id = 1)
    protected Integer type;

    @TlvTag(type = "int64", id = 14)
    protected Long seq;


    @TlvTag(type = "bytes", id = 16)
    private byte[] videoData;

    @TlvTag(type = "string", id = 22)
    protected String sessionID;

    // 媒体相关
    @TlvTag(type = "int32", id = 23)
    private Integer frameType;

    public VideoResponse(long seq, byte[] videoData, String sessionID, int frameType) {
        this.type = Type.VIDEO;
        this.seq = seq;
        this.videoData = videoData;
        this.sessionID = sessionID;
        this.frameType = frameType;
    }
}