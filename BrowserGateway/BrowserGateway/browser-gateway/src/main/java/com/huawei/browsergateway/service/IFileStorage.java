package com.huawei.browsergateway.service;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * 文件存储服务接口，统一管理用户数据和文件操作
 */
public interface IFileStorage {
    
    /**
     * 上传文件到存储
     * 
     * @param localPath 本地文件路径
     * @param remotePath 远程存储路径
     * @return String 文件的访问URL，失败返回null
     */
    String uploadFile(String localPath, String remotePath);
    
    /**
     * 下载文件到本地
     * 
     * @param localPath 本地保存路径
     * @param remotePath 远程文件路径
     * @return File 本地文件对象，失败返回null
     */
    File downloadFile(String localPath, String remotePath);
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     * @return boolean 删除是否成功
     */
    boolean deleteFile(String path);
    
    /**
     * 检查文件是否存在
     * 
     * @param path 文件路径
     * @return boolean 文件是否存在
     */
    boolean exist(String path);
    
    /**
     * 获取文件大小
     * 
     * @param path 文件路径
     * @return long 文件大小(字节)，文件不存在返回-1
     */
    long getFileSize(String path);
    
    /**
     * 获取文件访问URL
     * 
     * @param path 文件路径
     * @param expireSeconds 过期时间(秒)
     * @return String 文件访问URL，失败返回null
     */
    String getFileUrl(String path, int expireSeconds);
    
    /**
     * 用户数据下载
     * 
     * @param userDataPath 用户数据目录
     * @param userId 用户ID
     * @param serverAddr 服务器地址
     * @return String 本地用户数据路径
     */
    String downloadUserData(String userDataPath, String userId, String serverAddr);
    
    /**
     * 用户数据上传
     * 
     * @param userDataPath 用户数据目录
     * @param userId 用户ID
     * @param serverAddr 服务器地址
     * @return boolean 上传是否成功
     */
    boolean uploadUserData(String userDataPath, String userId, String serverAddr);
    
    /**
     * 批量文件上传
     * 
     * @param fileMap 文件路径映射 {本地路径: 远程路径}
     * @return Map<String, String> 上传结果 {文件路径: 访问URL}
     */
    Map<String, String> batchUpload(Map<String, String> fileMap);
    
    /**
     * 批量文件删除
     * 
     * @param paths 文件路径集合
     * @return Map<String, Boolean> 删除结果 {文件路径: 是否成功}
     */
    Map<String, Boolean> batchDelete(Set<String> paths);
}
