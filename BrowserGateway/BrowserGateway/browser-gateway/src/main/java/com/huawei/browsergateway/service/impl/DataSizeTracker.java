package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.service.IRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据大小追踪器
 * 用于追踪和上报媒体流和控制流的数据大小
 * 对应存量代码中的DataSizeTracker类
 */
public class DataSizeTracker {
    
    private static final Logger log = LoggerFactory.getLogger(DataSizeTracker.class);
    
    private final IRemote remote;
    private final String type; // "media" 或 "control"
    private long totalSize = 0;
    
    public DataSizeTracker(IRemote remote, String type) {
        this.remote = remote;
        this.type = type;
    }
    
    /**
     * 记录数据大小
     * 
     * @param size 数据大小（字节）
     */
    public void track(long size) {
        if (size > 0) {
            totalSize += size;
            log.debug("追踪{}数据: size={}, total={}", type, size, totalSize);
        }
    }
    
    /**
     * 获取总数据大小
     * 
     * @return 总数据大小（字节）
     */
    public long getTotalSize() {
        return totalSize;
    }
    
    /**
     * 重置计数器
     */
    public void reset() {
        totalSize = 0;
    }
    
    /**
     * 获取类型
     * 
     * @return 类型（"media" 或 "control"）
     */
    public String getType() {
        return type;
    }
}
