package com.huawei.browsergateway.common;

/**
 * 系统常量定义
 */
public class Constants {
    
    /** 默认字符集 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /** 默认心跳TTL（秒） */
    public static final int DEFAULT_HEARTBEAT_TTL = 60;
    
    /** 用户数据前缀 */
    public static final String DATA_PREFIX = "userdata-";
    
    /** 默认工作目录 */
    public static final String DEFAULT_WORKSPACE = "/opt/host";
    
    /** 默认临时目录 */
    public static final String DEFAULT_TMP_PATH = "/tmp/browsergateway";
    
    /** 告警去重间隔（毫秒） */
    public static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000;
    
    // ========== 文件存储相关常量 ==========
    
    /** 文件存储基础路径 */
    public static final String STORAGE_BASE_PATH = "/opt/browsergateway/storage";
    
    /** 文件存储临时路径 */
    public static final String STORAGE_TEMP_PATH = "/tmp/browsergateway/storage-temp";
    
    /** 用户数据存储路径 */
    public static final String STORAGE_USER_PATH = "/opt/browsergateway/storage/user";
    
    /** 缓存存储路径 */
    public static final String STORAGE_CACHE_PATH = "/opt/browsergateway/storage/cache";
    
    /** 备份存储路径 */
    public static final String STORAGE_BACKUP_PATH = "/opt/browsergateway/storage/backup";
    
    /** 最大文件大小（1GB） */
    public static final long MAX_FILE_SIZE = 1073741824L;
    
    /** 最大单个文件大小（50MB） */
    public static final long MAX_SINGLE_FILE_SIZE = 52428800L;
    
    /** 最大批量文件大小（100MB） */
    public static final long MAX_BATCH_FILE_SIZE = 104857600L;
    
    /** 默认分片大小（5MB） */
    public static final int DEFAULT_CHUNK_SIZE = 5242880;
    
    /** 默认最大重试次数 */
    public static final int DEFAULT_MAX_RETRY = 3;
    
    /** 默认超时时间（秒） */
    public static final int DEFAULT_TIMEOUT = 300;
    
    /** 用户数据压缩包后缀 */
    public static final String USER_DATA_ARCHIVE_SUFFIX = ".tar.zst";
    
    /** 用户数据路径前缀 */
    public static final String USER_DATA_PATH_PREFIX = "user/";
    
    /** 缓存路径前缀 */
    public static final String CACHE_PATH_PREFIX = "cache/";

    // tcp流的类型: 媒体流、控制流
    public static String TCP_MEDIA = "TcpMedia";
    public static String TCP_CONTROL = "TcpControl";
}
