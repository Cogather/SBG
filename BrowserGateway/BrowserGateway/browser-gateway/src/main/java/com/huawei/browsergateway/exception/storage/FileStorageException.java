package com.huawei.browsergateway.exception.storage;

/**
 * 文件存储相关异常
 */
public class FileStorageException extends RuntimeException {
    
    public FileStorageException(String message) {
        super(message);
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 文件上传异常
     */
    public static class UploadException extends FileStorageException {
        public UploadException(String localPath, String remotePath, String reason) {
            super(String.format("文件上传失败: localPath=%s, remotePath=%s, reason=%s", 
                localPath, remotePath, reason));
        }
        
        public UploadException(String localPath, String remotePath, Throwable cause) {
            super(String.format("文件上传失败: localPath=%s, remotePath=%s", 
                localPath, remotePath), cause);
        }
    }
    
    /**
     * 文件下载异常
     */
    public static class DownloadException extends FileStorageException {
        public DownloadException(String localPath, String remotePath, String reason) {
            super(String.format("文件下载失败: localPath=%s, remotePath=%s, reason=%s", 
                localPath, remotePath, reason));
        }
        
        public DownloadException(String localPath, String remotePath, Throwable cause) {
            super(String.format("文件下载失败: localPath=%s, remotePath=%s", 
                localPath, remotePath), cause);
        }
    }
    
    /**
     * 文件删除异常
     */
    public static class DeleteException extends FileStorageException {
        public DeleteException(String path, String reason) {
            super(String.format("文件删除失败: path=%s, reason=%s", path, reason));
        }
        
        public DeleteException(String path, Throwable cause) {
            super(String.format("文件删除失败: path=%s", path), cause);
        }
    }
    
    /**
     * 用户数据异常
     */
    public static class UserDataException extends FileStorageException {
        public UserDataException(String userId, String operation, String reason) {
            super(String.format("用户数据处理失败: userId=%s, operation=%s, reason=%s", 
                userId, operation, reason));
        }
        
        public UserDataException(String userId, String operation, Throwable cause) {
            super(String.format("用户数据处理失败: userId=%s, operation=%s", 
                userId, operation), cause);
        }
    }
    
    /**
     * 权限异常
     */
    public static class AccessException extends FileStorageException {
        public AccessException(String userId, String path, String operation) {
            super(String.format("文件访问权限不足: userId=%s, path=%s, operation=%s", 
                userId, path, operation));
        }
    }
}
