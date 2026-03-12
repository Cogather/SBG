package com.huawei.browsergateway.service;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * 文件存储服务接口，统一管理用户数据和文件的远程存取
 */
public interface IFileStorage {

    /**
     * 上传本地文件到远程存储
     *
     * @param localPath  本地文件路径
     * @param remotePath 远程存储路径（格式：bucket/path/name）
     */
    void uploadFile(String localPath, String remotePath);

    /**
     * 从远程存储下载文件到本地
     *
     * @param localPath  本地保存路径
     * @param remotePath 远程文件路径
     */
    void downloadFile(String localPath, String remotePath);

    /**
     * 删除远程存储中的文件
     *
     * @param path 远程文件路径
     */
    void deleteFile(String path);

    /**
     * 检查远程文件是否存在
     *
     * @param path 远程文件路径
     * @return true 表示文件存在
     */
    boolean exist(String path);
}
