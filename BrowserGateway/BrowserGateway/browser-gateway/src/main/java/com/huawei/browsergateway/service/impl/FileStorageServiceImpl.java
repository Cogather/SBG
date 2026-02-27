package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.Constants;
import com.huawei.browsergateway.common.utils.ZstdUtil;
import com.huawei.browsergateway.exception.storage.FileStorageException;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.storage.FileInfo;
import com.huawei.browsergateway.service.storage.StorageAdapter;
import com.huawei.browsergateway.service.storage.StorageConfig;
import com.huawei.browsergateway.service.storage.StorageStrategySelector;
import com.huawei.browsergateway.service.storage.StorageType;
import com.huawei.browsergateway.service.storage.impl.LocalFileSystemAdapter;
import com.huawei.browsergateway.service.storage.impl.S3CompatibleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件存储服务实现类
 */
@Service
public class FileStorageServiceImpl implements IFileStorage {
    
    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    
    private StorageStrategySelector strategySelector;
    private StorageConfig defaultConfig;
    private ExecutorService executorService;
    
    @Value("${file-storage.type:LOCAL}")
    private String storageType;
    
    @Value("${file-storage.base-path:" + Constants.STORAGE_BASE_PATH + "}")
    private String basePath;
    
    @Value("${file-storage.temp-path:" + Constants.STORAGE_TEMP_PATH + "}")
    private String tempPath;
    
    @Value("${file-storage.s3.access-key:}")
    private String s3AccessKey;
    
    @Value("${file-storage.s3.secret-key:}")
    private String s3SecretKey;
    
    @Value("${file-storage.s3.region:}")
    private String s3Region;
    
    @Value("${file-storage.s3.bucket-name:}")
    private String s3BucketName;
    
    @Value("${file-storage.s3.endpoint:}")
    private String s3Endpoint;
    
    @PostConstruct
    public void init() {
        log.info("初始化文件存储服务...");
        
        // 创建策略选择器
        strategySelector = new StorageStrategySelector();
        
        // 注册本地文件系统适配器
        LocalFileSystemAdapter localAdapter = new LocalFileSystemAdapter();
        strategySelector.registerAdapter(StorageType.LOCAL, localAdapter);
        
        // 如果配置了S3，注册S3适配器
        if (isS3Configured()) {
            S3CompatibleAdapter s3Adapter = new S3CompatibleAdapter();
            strategySelector.registerAdapter(StorageType.S3, s3Adapter);
        }
        
        // 创建默认配置
        defaultConfig = createDefaultConfig();
        
        // 初始化默认适配器
        try {
            StorageAdapter defaultAdapter = strategySelector.selectAdapter(defaultConfig);
            log.info("文件存储服务初始化成功: type={}", defaultConfig.getStorageType());
        } catch (Exception e) {
            log.error("文件存储服务初始化失败", e);
            throw new RuntimeException("文件存储服务初始化失败", e);
        }
        
        // 创建线程池用于异步操作
        executorService = Executors.newFixedThreadPool(10);
        
        log.info("文件存储服务初始化完成");
    }
    
    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    @Override
    public String uploadFile(String localPath, String remotePath) {
        try {
            validatePath(localPath, "本地路径");
            validatePath(remotePath, "远程路径");
            
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                throw new FileStorageException.UploadException(localPath, remotePath, "本地文件不存在");
            }
            
            // 检查文件大小
            long fileSize = localFile.length();
            if (fileSize > Constants.MAX_FILE_SIZE) {
                throw new FileStorageException("文件大小超出限制: " + fileSize);
            }
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(remotePath);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 执行上传
            return adapter.uploadFile(localPath, remotePath);
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.UploadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public File downloadFile(String localPath, String remotePath) {
        try {
            validatePath(localPath, "本地路径");
            validatePath(remotePath, "远程路径");
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(remotePath);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 执行下载
            return adapter.downloadFile(localPath, remotePath);
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.DownloadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public boolean deleteFile(String path) {
        try {
            validatePath(path, "文件路径");
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(path);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 执行删除
            return adapter.deleteFile(path);
            
        } catch (FileStorageException e) {
            log.error("删除文件失败: {}", path, e);
            return false;
        } catch (Exception e) {
            log.error("删除文件失败: {}", path, e);
            return false;
        }
    }
    
    @Override
    public boolean exist(String path) {
        try {
            validatePath(path, "文件路径");
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(path);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 检查存在性
            return adapter.exist(path);
            
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", path, e);
            return false;
        }
    }
    
    @Override
    public long getFileSize(String path) {
        try {
            validatePath(path, "文件路径");
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(path);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 获取文件信息
            FileInfo fileInfo = adapter.getFileInfo(path);
            return fileInfo != null ? fileInfo.getSize() : -1;
            
        } catch (Exception e) {
            log.error("获取文件大小失败: {}", path, e);
            return -1;
        }
    }
    
    @Override
    public String getFileUrl(String path, int expireSeconds) {
        try {
            validatePath(path, "文件路径");
            
            // 选择适配器
            StorageAdapter adapter = strategySelector.selectAdapterByPath(path);
            if (adapter == null) {
                adapter = strategySelector.selectAdapter(defaultConfig);
            }
            
            // 获取访问URL
            return adapter.getFileUrl(path, expireSeconds);
            
        } catch (Exception e) {
            log.error("获取文件访问URL失败: {}", path, e);
            return null;
        }
    }
    
    @Override
    public String downloadUserData(String userDataPath, String userId, String serverAddr) {
        try {
            validatePath(userDataPath, "用户数据路径");
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            
            // 构建远程路径
            String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + Constants.USER_DATA_ARCHIVE_SUFFIX;
            
            // 创建临时目录
            Path tempDir = Paths.get(tempPath, "download", userId);
            Files.createDirectories(tempDir);
            
            // 下载压缩包
            String archivePath = tempDir.resolve(userId + Constants.USER_DATA_ARCHIVE_SUFFIX).toString();
            File archiveFile = downloadFile(archivePath, remotePath);
            
            if (archiveFile == null || !archiveFile.exists()) {
                throw new FileStorageException.UserDataException(userId, "download", "下载压缩包失败");
            }
            
            // 解压到目标目录
            String targetDir = userDataPath;
            ZstdUtil.decompress(archivePath, targetDir);
            
            // 清理临时文件
            archiveFile.delete();
            
            log.info("用户数据下载成功: userId={}, path={}", userId, targetDir);
            return targetDir;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.UserDataException(userId, "download", e);
        }
    }
    
    @Override
    public boolean uploadUserData(String userDataPath, String userId, String serverAddr) {
        try {
            validatePath(userDataPath, "用户数据路径");
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            
            Path userDataDir = Paths.get(userDataPath);
            if (!Files.exists(userDataDir) || !Files.isDirectory(userDataDir)) {
                throw new FileStorageException.UserDataException(userId, "upload", "用户数据目录不存在");
            }
            
            // 创建临时目录
            Path tempDir = Paths.get(tempPath, "upload", userId);
            Files.createDirectories(tempDir);
            
            // 压缩用户数据
            String archivePath = tempDir.resolve(userId + Constants.USER_DATA_ARCHIVE_SUFFIX).toString();
            ZstdUtil.compress(userDataPath, archivePath);
            
            // 上传压缩包
            String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + Constants.USER_DATA_ARCHIVE_SUFFIX;
            String uploadedPath = uploadFile(archivePath, remotePath);
            
            if (uploadedPath == null) {
                throw new FileStorageException.UserDataException(userId, "upload", "上传压缩包失败");
            }
            
            // 清理临时文件
            new File(archivePath).delete();
            
            log.info("用户数据上传成功: userId={}, path={}", userId, remotePath);
            return true;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.UserDataException(userId, "upload", e);
        }
    }
    
    @Override
    public Map<String, String> batchUpload(Map<String, String> fileMap) {
        Map<String, String> results = new HashMap<>();
        
        if (fileMap == null || fileMap.isEmpty()) {
            return results;
        }
        
        // 使用CompletableFuture并行上传
        CompletableFuture<?>[] futures = fileMap.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                try {
                    String localPath = entry.getKey();
                    String remotePath = entry.getValue();
                    String uploadedPath = uploadFile(localPath, remotePath);
                    synchronized (results) {
                        results.put(localPath, uploadedPath);
                    }
                } catch (Exception e) {
                    log.error("批量上传文件失败: {}", entry.getKey(), e);
                    synchronized (results) {
                        results.put(entry.getKey(), null);
                    }
                }
            }, executorService))
            .toArray(CompletableFuture[]::new);
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();
        
        return results;
    }
    
    @Override
    public Map<String, Boolean> batchDelete(Set<String> paths) {
        Map<String, Boolean> results = new HashMap<>();
        
        if (paths == null || paths.isEmpty()) {
            return results;
        }
        
        // 使用CompletableFuture并行删除
        CompletableFuture<?>[] futures = paths.stream()
            .map(path -> CompletableFuture.runAsync(() -> {
                boolean success = deleteFile(path);
                synchronized (results) {
                    results.put(path, success);
                }
            }, executorService))
            .toArray(CompletableFuture[]::new);
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();
        
        return results;
    }
    
    /**
     * 创建默认配置
     */
    private StorageConfig createDefaultConfig() {
        StorageConfig config = new StorageConfig();
        
        // 根据配置设置存储类型
        try {
            config.setStorageType(StorageType.valueOf(storageType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("无效的存储类型: {}, 使用默认LOCAL", storageType);
            config.setStorageType(StorageType.LOCAL);
        }
        
        config.setBasePath(basePath);
        config.setTempPath(tempPath);
        config.setMaxFileSize((int) (Constants.MAX_FILE_SIZE / 1024 / 1024)); // 转换为MB
        config.setEnableCache(false);
        config.setCacheSize(100);
        config.setCacheExpire(600);
        config.setMaxUploadThreads(10);
        config.setMaxDownloadThreads(10);
        config.setConnectTimeout(30);
        config.setReadTimeout(300);
        config.setRetryCount(Constants.DEFAULT_MAX_RETRY);
        
        // 如果是S3类型，设置S3相关配置
        if (config.getStorageType() == StorageType.S3) {
            config.setAccessKey(s3AccessKey);
            config.setSecretKey(s3SecretKey);
            config.setRegion(s3Region);
            config.setBucketName(s3BucketName);
            config.setEndpoint(s3Endpoint);
        }
        
        // 验证配置
        config.validate();
        
        return config;
    }
    
    /**
     * 检查S3是否已配置
     */
    private boolean isS3Configured() {
        return s3AccessKey != null && !s3AccessKey.trim().isEmpty() &&
               s3SecretKey != null && !s3SecretKey.trim().isEmpty() &&
               s3BucketName != null && !s3BucketName.trim().isEmpty();
    }
    
    /**
     * 验证路径
     */
    private void validatePath(String path, String pathName) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(pathName + "不能为空");
        }
    }
}
