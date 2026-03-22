package com.huawei.mobile.dto;

import java.time.LocalDateTime;

public class DeviceLoginResponse {
    private String token = "1234";
    private LocalDateTime expiresTime;
    private String tcpAddr = "127.0.0.1:30001";
    private Long timeAxis;
    private Integer videoMode;
    private String shortAddr;
    private String nodeGateWayUrl;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(LocalDateTime expiresTime) {
        this.expiresTime = expiresTime;
    }

    public String getTcpAddr() {
        return tcpAddr;
    }

    public void setTcpAddr(String tcpAddr) {
        this.tcpAddr = tcpAddr;
    }

    public Long getTimeAxis() {
        return timeAxis;
    }

    public void setTimeAxis(Long timeAxis) {
        this.timeAxis = timeAxis;
    }

    public Integer getVideoMode() {
        return videoMode;
    }

    public void setVideoMode(Integer videoMode) {
        this.videoMode = videoMode;
    }

    public String getShortAddr() {
        return shortAddr;
    }

    public void setShortAddr(String shortAddr) {
        this.shortAddr = shortAddr;
    }

    public String getNodeGateWayUrl() {
        return nodeGateWayUrl;
    }

    public void setNodeGateWayUrl(String nodeGateWayUrl) {
        this.nodeGateWayUrl = nodeGateWayUrl;
    }
}
