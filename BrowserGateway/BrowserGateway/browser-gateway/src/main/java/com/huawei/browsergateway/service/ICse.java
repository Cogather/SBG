package com.huawei.browsergateway.service;

/**
 * CSE 服务发现接口，提供上报端点地址
 */
public interface ICse {

    /**
     * 从 CSE 注册中心随机获取一个可用的上报端点（host:port 格式）
     */
    String getReportEndpoint();
}
