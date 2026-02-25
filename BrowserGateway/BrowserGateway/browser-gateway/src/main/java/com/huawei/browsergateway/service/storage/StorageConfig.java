package com.huawei.browsergateway.service.storage;

import lombok.Data;

/**
 * 存储配置
 */
@Data
public class StorageConfig {
    
    /** 存储类型 */
    private StorageType storageType;
    
    /** 访问密钥 */
    private String accessKey;
    
    /** 密钥 */
    private String secretKey;
    
    /** 区域/地区 */
    private String region;
    
    /** 存储桶名称 */
    private String bucketName;
    
    /** 服务端点 */
    private String endpoint;
    
    /** 基础路径 */
    private String basePath;
    
    /** 临时文件路径 */
    private String tempPath;
    
    /** 最大文件大小(MB) */
    private int maxFileSize;
    
    /** 是否启用缓存 */
    private boolean enableCache;
    
    /** 缓存大小(MB) */
    private int cacheSize;
    
    /** 缓存过期时间(秒) */
    private long cacheExpire;
    
    /** 最大上传线程数 */
    private int maxUploadThreads;
    
    /** 最大下载线程数 */
    private int maxDownloadThreads;
    
    /** 连接超时(秒) */
    private int connectTimeout;
    
    /** 读取超时(秒) */
    private int readTimeout;
    
    /** 重试次数 */
    private int retryCount;
    
    /**
     * 验证配置有效性
     */
    public void validate() {
        if (storageType == null) {
            throw new IllegalArgumentException("存储类型不能为空");
        }
        
        if (storageType == StorageType.LOCAL) {
            validateLocalConfig();
        } else {
            validateCloudConfig();
        }
        
        validateCommonConfig();
    }
    
    /**
     * 验证本地存储配置
     */
    private void validateLocalConfig() {
        if (basePath == null || basePath.trim().isEmpty()) {
            throw new IllegalArgumentException("本地存储基础路径不能为空");
        }
    }
    
    /**
     * 验证云存储配置
     */
    private void validateCloudConfig() {
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalArgumentException("访问密钥不能为空");
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("区域不能为空");
        }
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("存储桶名称不能为空");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("服务端点不能为空");
        }
    }
    
    /**
     * 验证通用配置
     */
    private void validateCommonConfig() {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("最大文件大小必须大于0");
        }
        if (enableCache && cacheSize <= 0) {
            throw new IllegalArgumentException("启用缓存时，缓存大小必须大于0");
        }
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("连接超时时间必须大于0");
        }
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("读取超时时间必须大于0");
        }
    }
}
