package com.huawei.browsergateway.websocket.media;

public interface MediaStreamProcessor {
    void init(MediaParam initParam);
    void processMediaStream(byte[] data);

    void close();
}