package com.huawei.browsergateway.service.storage;

/**
 * 存储类型枚举
 */
public enum StorageType {
    /** 本地文件系统 */
    LOCAL,
    /** Amazon S3兼容存储 */
    S3,
    /** MinIO对象存储 */
    MINIO,
    /** 阿里云OSS */
    ALIYUN_OSS,
    /** 华为云OBS */
    HUAWEI_OBS
}
