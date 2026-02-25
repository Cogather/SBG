package com.huawei.browsergateway.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCodeEnum {
    
    SUCCESS(200, "成功"),
    
    // 4xx 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 5xx 服务器错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // 业务错误码 1000+
    BROWSER_CREATE_FAILED(1001, "浏览器创建失败"),
    EXTENSION_LOAD_FAILED(1002, "扩展加载失败"),
    SESSION_NOT_FOUND(1003, "会话不存在"),
    USER_DATA_DELETE_FAILED(1004, "用户数据删除失败"),
    PLUGIN_NOT_FOUND(1005, "插件不存在"),
    
    // 文件存储错误码 2000+
    FILE_UPLOAD_FAILED(2001, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(2002, "文件下载失败"),
    FILE_DELETE_FAILED(2003, "文件删除失败"),
    FILE_NOT_FOUND(2004, "文件不存在"),
    FILE_SIZE_EXCEEDED(2005, "文件大小超出限制"),
    STORAGE_CONFIG_INVALID(2006, "存储配置无效"),
    STORAGE_ADAPTER_NOT_FOUND(2007, "存储适配器未找到"),
    USER_DATA_UPLOAD_FAILED(2008, "用户数据上传失败"),
    USER_DATA_DOWNLOAD_FAILED(2009, "用户数据下载失败"),
    USER_DATA_COMPRESS_FAILED(2010, "用户数据压缩失败"),
    USER_DATA_DECOMPRESS_FAILED(2011, "用户数据解压失败"),
    
    // 告警错误码 3000+
    CPU_USAGE_HIGH(3001, "CPU使用率过高"),
    MEMORY_USAGE_HIGH(3002, "内存使用率过高"),
    NETWORK_USAGE_HIGH(3003, "网络使用率过高"),
    CAPACITY_OVERFLOW(3004, "服务容量溢出"),
    HEALTH_REPORT_FAILURE(3005, "健康上报失败"),
    TCP_CONNECTION_TIMEOUT(3006, "TCP连接超时"),
    BROWSER_INSTANCE_TIMEOUT(3007, "浏览器实例超时");
    
    private final Integer code;
    private final String message;
    
    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
