package com.huawei.browsergateway.websocket.media;

import com.huawei.browsergateway.websocket.SessionSet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MediaSessionManager extends SessionSet {
    private final Map<String, MediaStreamProcessor> processorMap = new ConcurrentHashMap<>();

    public void addProcessor(String key, MediaStreamProcessor processor) {
        MediaStreamProcessor oldProcessor = processorMap.get(key);
        if (oldProcessor != null) {
            oldProcessor.close();
        }
        processorMap.put(key, processor);
    }

    public MediaStreamProcessor getProcessor(String key) {
        return processorMap.get(key);
    }

    public void delProcessor(String key) {
        MediaStreamProcessor remove = processorMap.remove(key);
        if (remove != null) {
            remove.close();
        }
    }

    @Override
    public void del(String key) {
        this.delProcessor(key);
        super.del(key);
    }
}