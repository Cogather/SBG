package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 审计日志信息
 */
@Data
public class AuditLogInfo {
    /**
     * 操作名称
     */
    private String operation;

    /**
     * 日志级别。可以是如下值之一：
     * WARNING：提示
     * MINOR：一般
     * RISK：危险
     */
    private String level;

    /**
     * 操作用户
     */
    private String userName;

    /**
     * 时间戳
     */
    private String dateTime;

    /**
     * 操作来源
     */
    private String appName;

    /**
     * 发起操作的客户端IP地址，可从HTTP Header中获取
     */
    private String terminal;

    /**
     * 操作对象
     */
    private String serviceName;

    /**
     * 操作结果。可以是如下值之一：
     * SUCCESSFUL：成功
     * FAILURE：失败
     * PARTIAL_SUCCESS：部分成功
     */
    private String result;

    /**
     * 详细信息，最多支持800个字符
     */
    private String detail;

    /**
     * 详细信息(中文)，最多支持800个字符
     */
    private String detailZh;

    /**
     * 操作类型（仅操作日志需要）
     */
    private String operateType;

    /**
     * 审计日志类型
     */
    private String auditType;
}
