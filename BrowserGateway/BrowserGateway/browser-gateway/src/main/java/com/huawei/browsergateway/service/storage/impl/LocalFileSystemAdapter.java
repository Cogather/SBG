package com.huawei.browsergateway.service.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.huawei.browsergateway.exception.storage.FileStorageException;
import com.huawei.browsergateway.service.storage.FileInfo;
import com.huawei.browsergateway.service.storage.StorageAdapter;
import com.huawei.browsergateway.service.storage.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件系统适配器
 */
public class LocalFileSystemAdapter implements StorageAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemAdapter.class);
    
    private String basePath;
    
    @Override
    public void init(StorageConfig config) throws FileStorageException {
        this.basePath = config.getBasePath();
        if (basePath == null || basePath.trim().isEmpty()) {
            throw new FileStorageException("本地存储基础路径不能为空");
        }
        
        // 创建基础目录
        try {
            File baseDir = new File(basePath);
            if (!baseDir.exists()) {
                boolean created = baseDir.mkdirs();
                if (!created) {
                    throw new FileStorageException("创建基础目录失败: " + basePath);
                }
                log.info("创建本地存储基础目录: {}", basePath);
            }
        } catch (Exception e) {
            throw new FileStorageException("初始化本地存储适配器失败", e);
        }
    }
    
    @Override
    public String uploadFile(String localPath, String remotePath) throws FileStorageException {
        try {
            // 构造完整目标路径
            String fullRemotePath = buildFullPath(remotePath);
            File targetFile = new File(fullRemotePath);
            
            // 创建目标目录
            File targetDir = targetFile.getParentFile();
            if (targetDir != null && !targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    throw new FileStorageException("创建目标目录失败: " + targetDir.getAbsolutePath());
                }
            }
            
            // 验证源文件存在
            File sourceFile = new File(localPath);
            if (!sourceFile.exists()) {
                throw new FileStorageException.UploadException(localPath, remotePath, "源文件不存在");
            }
            
            // 计算源文件MD5哈希
            String sourceHash = DigestUtil.md5Hex(sourceFile);
            
            // 执行文件复制
            FileUtil.copy(sourceFile, targetFile, true);
            
            // 验证复制后的文件
            if (!targetFile.exists()) {
                throw new FileStorageException.UploadException(localPath, remotePath, "文件复制后不存在");
            }
            
            // 验证文件哈希（可选，确保文件完整性）
            String targetHash = DigestUtil.md5Hex(targetFile);
            if (!sourceHash.equals(targetHash)) {
                throw new FileStorageException.UploadException(localPath, remotePath, "文件哈希不匹配");
            }
            
            log.debug("文件上传成功: {} -> {}", localPath, fullRemotePath);
            return remotePath;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.UploadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public File downloadFile(String localPath, String remotePath) throws FileStorageException {
        try {
            // 构造源文件路径
            String fullRemotePath = buildFullPath(remotePath);
            File sourceFile = new File(fullRemotePath);
            
            // 验证源文件存在
            if (!sourceFile.exists()) {
                throw new FileStorageException.DownloadException(localPath, remotePath, "源文件不存在");
            }
            
            // 创建目标目录
            File targetFile = new File(localPath);
            File targetDir = targetFile.getParentFile();
            if (targetDir != null && !targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    throw new FileStorageException("创建目标目录失败: " + targetDir.getAbsolutePath());
                }
            }
            
            // 执行文件复制
            FileUtil.copy(sourceFile, targetFile, true);
            
            // 验证复制后的文件
            if (!targetFile.exists()) {
                throw new FileStorageException.DownloadException(localPath, remotePath, "文件复制后不存在");
            }
            
            log.debug("文件下载成功: {} -> {}", fullRemotePath, localPath);
            return targetFile;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.DownloadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public boolean deleteFile(String path) throws FileStorageException {
        try {
            String fullPath = buildFullPath(path);
            File file = new File(fullPath);
            
            if (!file.exists()) {
                log.warn("文件不存在，无法删除: {}", fullPath);
                return false;
            }
            
            boolean deleted = file.delete();
            if (deleted) {
                log.debug("文件删除成功: {}", fullPath);
            } else {
                log.warn("文件删除失败: {}", fullPath);
            }
            
            return deleted;
            
        } catch (Exception e) {
            throw new FileStorageException.DeleteException(path, e);
        }
    }
    
    @Override
    public boolean exist(String path) throws FileStorageException {
        try {
            String fullPath = buildFullPath(path);
            File file = new File(fullPath);
            return file.exists();
        } catch (Exception e) {
            throw new FileStorageException("检查文件存在性失败: " + path, e);
        }
    }
    
    @Override
    public FileInfo getFileInfo(String path) throws FileStorageException {
        try {
            String fullPath = buildFullPath(path);
            File file = new File(fullPath);
            
            if (!file.exists()) {
                return null;
            }
            
            return FileInfo.create(path, file);
            
        } catch (Exception e) {
            throw new FileStorageException("获取文件信息失败: " + path, e);
        }
    }
    
    @Override
    public String getFileUrl(String path, int expireSeconds) throws FileStorageException {
        // 本地文件系统直接返回路径
        return path;
    }
    
    /**
     * 构建完整路径
     */
    private String buildFullPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        
        // 规范化路径，防止路径遍历攻击
        Path normalizedPath = Paths.get(basePath, path).normalize();
        Path basePathObj = Paths.get(basePath).normalize();
        
        if (!normalizedPath.startsWith(basePathObj)) {
            throw new IllegalArgumentException("路径超出基础目录范围: " + path);
        }
        
        return normalizedPath.toString();
    }
}
