package com.huawei.browsergateway.service.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.Data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件信息
 */
@Data
public class FileInfo {
    
    /** 文件路径 */
    private String path;
    
    /** 文件名 */
    private String name;
    
    /** 文件扩展名 */
    private String extension;
    
    /** 文件大小(字节) */
    private long size;
    
    /** 最后修改时间戳 */
    private long lastModified;
    
    /** 文件哈希值 */
    private String hash;
    
    /** 访问URL */
    private String accessUrl;
    
    /** 自定义元数据 */
    private Map<String, Object> metadata;
    
    /**
     * 创建文件信息
     */
    public static FileInfo create(String path, File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPath(path);
        fileInfo.setName(file.getName());
        fileInfo.setSize(file.length());
        fileInfo.setLastModified(file.lastModified());
        fileInfo.setExtension(getExtension(file.getName()));
        fileInfo.setMetadata(new HashMap<>());
        fileInfo.setHash(calculateFileHash(file));
        
        return fileInfo;
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * 计算文件哈希
     */
    private static String calculateFileHash(File file) {
        try {
            return DigestUtil.md5Hex(file);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 判断是否是合法文件名
     */
    public boolean isValidFileName() {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // 检查文件名不包含非法字符：\ / : * ? " < > |
        String illegalChars = "\\/:*?\"<>|";
        for (char c : illegalChars.toCharArray()) {
            if (name.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 构建完整的访问URL
     */
    public String buildFullUrl(String baseUrl) {
        if (accessUrl == null || accessUrl.isEmpty()) {
            return null;
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            return accessUrl;
        }
        
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String url = accessUrl.startsWith("/") ? accessUrl.substring(1) : accessUrl;
        return base + url;
    }
}
