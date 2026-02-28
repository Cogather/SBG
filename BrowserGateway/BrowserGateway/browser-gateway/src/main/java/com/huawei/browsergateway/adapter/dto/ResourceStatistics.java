package com.huawei.browsergateway.adapter.dto;

/**
 * 资源统计信息
 */
public class ResourceStatistics {
    private boolean success;
    private float ratio;
    private long timestamp;
    private long available;
    private long capacity;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public float getRatio() {
        return ratio;
    }
    
    public void setRatio(float ratio) {
        this.ratio = ratio;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getAvailable() {
        return available;
    }
    
    public void setAvailable(long available) {
        this.available = available;
    }
    
    public long getCapacity() {
        return capacity;
    }
    
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
