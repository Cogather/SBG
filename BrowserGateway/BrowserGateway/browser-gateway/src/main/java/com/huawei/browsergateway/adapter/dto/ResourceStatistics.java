package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 资源统计信息
 */
@Data
public class ResourceStatistics {
    private boolean success;
    private float ratio;
    private long timestamp;
    private long available;
    private long capacity;
}
