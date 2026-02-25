package com.huawei.browsergateway.entity.remote;

import lombok.Data;

/**
 * 用户绑定信息
 */
@Data
public class UserBind {
    private String sessionId;
    private String browserInstance;
    private String controlEndpoint;
    private String mediaEndpoint;
    private String controlTlsEndpoint;
    private String mediaTlsEndpoint;
    private String innerMediaEndpoint;
    private String innerBrowserEndpoint;
    private long createTime;
    private long updateTime;
    
    /**
     * 获取有效的控制端点
     */
    public String getEffectiveControlEndpoint() {
        return controlTlsEndpoint != null && !controlTlsEndpoint.isEmpty() 
                ? controlTlsEndpoint : controlEndpoint;
    }
    
    /**
     * 获取有效的媒体端点
     */
    public String getEffectiveMediaEndpoint() {
        return mediaTlsEndpoint != null && !mediaTlsEndpoint.isEmpty() 
                ? mediaTlsEndpoint : mediaEndpoint;
    }
    
    /**
     * 更新时间戳
     */
    public void updateTime() {
        this.updateTime = System.currentTimeMillis();
    }
    
    /**
     * 判断会话是否过期
     */
    public boolean isExpired(long timeoutMillis) {
        return (System.currentTimeMillis() - updateTime) > timeoutMillis;
    }
}
