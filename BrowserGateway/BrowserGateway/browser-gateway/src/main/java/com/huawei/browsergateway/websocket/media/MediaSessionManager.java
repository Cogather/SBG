package com.huawei.browsergateway.websocket.media;

import com.huawei.browsergateway.websocket.SessionSet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体流 WebSocket 会话管理器
 *
 * 扩展 SessionSet，同时管理 WebSocket 会话和媒体流处理器
 */
@Component
public class MediaSessionManager extends SessionSet {

    /** 媒体流处理器存储容器 */
    private final Map<String, MediaStreamProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * 添加媒体流处理器，如果键已存在则关闭旧处理器并替换
     *
     * @param key       用户标识键
     * @param processor 媒体流处理器
     */
    public void addProcessor(String key, MediaStreamProcessor processor) {
        MediaStreamProcessor oldProcessor = processorMap.get(key);
        if (oldProcessor != null) {
            oldProcessor.close();
        }
        processorMap.put(key, processor);
    }

    /**
     * 根据键获取媒体流处理器
     *
     * @param key 用户标识键
     * @return 媒体流处理器，不存在则返回 null
     */
    public MediaStreamProcessor getProcessor(String key) {
        return processorMap.get(key);
    }

    /**
     * 删除媒体流处理器并关闭
     *
     * @param key 用户标识键
     */
    public void delProcessor(String key) {
        MediaStreamProcessor remove = processorMap.remove(key);
        if (remove != null) {
            remove.close();
        }
    }

    /**
     * 删除会话和处理器，确保资源完整清理
     *
     * @param key 用户标识键
     */
    @Override
    public void del(String key) {
        this.delProcessor(key);
        super.del(key);
    }
}
