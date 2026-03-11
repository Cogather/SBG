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
     */
     void uploadFile(String localPath, String remotePath);
    
    /**
     * 下载文件到本地
     * 
     * @param localPath 本地保存路径
     * @param remotePath 远程文件路径
     */
    void downloadFile(String localPath, String remotePath);
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     */
    void deleteFile(String path);
    
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
}
