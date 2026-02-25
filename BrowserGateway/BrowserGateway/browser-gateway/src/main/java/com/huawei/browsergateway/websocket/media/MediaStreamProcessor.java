package com.huawei.browsergateway.websocket.media;

import javax.websocket.Session;

/**
 * 媒体流处理器接口
 */
public interface MediaStreamProcessor {
    
    /**
     * 初始化处理器
     * 
     * @param userId 用户ID
     * @param session WebSocket会话
     * @param param 媒体参数
     */
    void init(String userId, Session session, MediaParam param);
    
    /**
     * 处理媒体流数据
     * 
     * @param data 媒体流数据
     */
    void processMediaStream(byte[] data);
    
    /**
     * 关闭处理器
     */
    void close();
}
