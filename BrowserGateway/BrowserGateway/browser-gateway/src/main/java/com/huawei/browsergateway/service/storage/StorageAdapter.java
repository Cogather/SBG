package com.huawei.browsergateway.service.storage;

import com.huawei.browsergateway.exception.storage.FileStorageException;

import java.io.File;

/**
 * 存储适配器接口
 */
public interface StorageAdapter {
    
    /**
     * 初始化存储适配器
     * 
     * @param config 存储配置
     * @throws FileStorageException 初始化失败时抛出
     */
    void init(StorageConfig config) throws FileStorageException;
    
    /**
     * 上传文件
     * 
     * @param localPath 本地文件路径
     * @param remotePath 远程存储路径
     * @return 文件访问URL
     * @throws FileStorageException 上传失败时抛出
     */
    String uploadFile(String localPath, String remotePath) throws FileStorageException;
    
    /**
     * 下载文件到本地
     * 
     * @param localPath 本地保存路径
     * @param remotePath 远程文件路径
     * @return 本地文件对象
     * @throws FileStorageException 下载失败时抛出
     */
    File downloadFile(String localPath, String remotePath) throws FileStorageException;
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     * @return 删除是否成功
     * @throws FileStorageException 删除失败时抛出
     */
    boolean deleteFile(String path) throws FileStorageException;
    
    /**
     * 检查文件是否存在
     * 
     * @param path 文件路径
     * @return 文件是否存在
     * @throws FileStorageException 检查失败时抛出
     */
    boolean exist(String path) throws FileStorageException;
    
    /**
     * 获取文件信息
     * 
     * @param path 文件路径
     * @return 文件信息对象
     * @throws FileStorageException 获取失败时抛出
     */
    FileInfo getFileInfo(String path) throws FileStorageException;
    
    /**
     * 获取文件访问URL
     * 
     * @param path 文件路径
     * @param expireSeconds 过期时间(秒)
     * @return 文件访问URL
     * @throws FileStorageException 获取失败时抛出
     */
    String getFileUrl(String path, int expireSeconds) throws FileStorageException;
}
