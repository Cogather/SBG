package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.constant.Constants;
import com.huawei.browsergateway.common.utils.ZstdUtil;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.util.UserdataSlimmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用户数据管理类
 * 负责用户数据的完整生命周期管理：上传、下载、删除
 * 集成脱敏和压缩功能
 */
@Component
public class UserDataManager {
    
    private static final Logger log = LoggerFactory.getLogger(UserDataManager.class);
    
    @Autowired
    private IFileStorage fileStorageService;
    
    @Value("${browsergw.workspace:/opt/host}")
    private String workspace;
    
    @Value("${browsergw.userdata.temp-path:/tmp/browsergateway/userdata}")
    private String tempPath;
    
    /**
     * 用户数据JSON文件后缀
     */
    private static final String USER_DATA_JSON_SUFFIX = ".json";
    
    /**
     * 用户数据压缩文件后缀
     */
    private static final String USER_DATA_COMPRESSED_SUFFIX = ".json.zst";
    
    /**
     * 上传用户数据
     * 流程：脱敏 -> 压缩 -> 上传 -> 清理临时文件
     * 
     * @param userId 用户ID
     * @param userDataPath 用户数据目录路径
     * @param serverAddr 服务器地址（可选）
     * @return 是否上传成功
     */
    public boolean uploadUserData(String userId, String userDataPath, String serverAddr) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (userDataPath == null || userDataPath.trim().isEmpty()) {
            throw new IllegalArgumentException("用户数据路径不能为空");
        }
        
        log.info("开始上传用户数据: userId={}, path={}", userId, userDataPath);
        
        Path userDataDir = Paths.get(userDataPath);
        if (!Files.exists(userDataDir) || !Files.isDirectory(userDataDir)) {
            log.error("用户数据目录不存在: {}", userDataPath);
            throw new IllegalArgumentException("用户数据目录不存在: " + userDataPath);
        }
        
        try {
            // 查找用户数据JSON文件
            File userDataJsonFile = findUserDataJsonFile(userDataDir.toFile());
            if (userDataJsonFile == null) {
                log.warn("未找到用户数据JSON文件，尝试处理整个目录: {}", userDataPath);
                // 如果找不到JSON文件，对整个目录进行脱敏处理
                UserdataSlimmer.slimDirectory(userDataDir.toFile());
            } else {
                // 对JSON文件进行脱敏处理
                log.info("对用户数据进行脱敏处理: {}", userDataJsonFile.getAbsolutePath());
                UserdataSlimmer.slimInplace(userDataJsonFile);
                
                // 验证脱敏是否成功
                if (!UserdataSlimmer.isSlimmed(userDataJsonFile)) {
                    log.warn("用户数据脱敏验证失败，但继续上传: {}", userDataJsonFile.getAbsolutePath());
                }
            }
            
            // 创建临时目录
            Path tempDir = Paths.get(tempPath, "upload", userId);
            Files.createDirectories(tempDir);
            
            // 压缩用户数据
            String compressedPath = tempDir.resolve(userId + USER_DATA_COMPRESSED_SUFFIX).toString();
            
            if (userDataJsonFile != null) {
                // 压缩JSON文件
                ZstdUtil.compressJson(userDataJsonFile.getAbsolutePath(), compressedPath);
            } else {
                // 压缩整个目录
                // 先打包为tar，然后压缩（这里简化处理，直接压缩目录）
                ZstdUtil.compress(userDataPath, compressedPath);
            }
            
            // 构建远程路径
            String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + USER_DATA_COMPRESSED_SUFFIX;
            
            // 删除远程旧文件（如果存在）
            if (fileStorageService.exist(remotePath)) {
                log.info("删除远程旧文件: {}", remotePath);
                fileStorageService.deleteFile(remotePath);
            }
            
            // 上传压缩文件
            String uploadedPath = fileStorageService.uploadFile(compressedPath, remotePath);
            if (uploadedPath == null) {
                throw new IOException("上传用户数据失败");
            }
            
            // 清理临时文件
            File compressedFile = new File(compressedPath);
            if (compressedFile.exists()) {
                compressedFile.delete();
            }
            
            log.info("用户数据上传成功: userId={}, remotePath={}", userId, remotePath);
            return true;
            
        } catch (Exception e) {
            log.error("上传用户数据失败: userId={}, path={}", userId, userDataPath, e);
            throw new RuntimeException("上传用户数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载用户数据
     * 流程：下载压缩文件 -> 解压 -> 返回本地路径
     * 
     * @param userId 用户ID
     * @param targetPath 目标目录路径
     * @param serverAddr 服务器地址（可选）
     * @return 本地用户数据路径
     */
    public String downloadUserData(String userId, String targetPath, String serverAddr) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        log.info("开始下载用户数据: userId={}, targetPath={}", userId, targetPath);
        
        try {
            // 构建远程路径
            String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + USER_DATA_COMPRESSED_SUFFIX;
            
            // 检查远程文件是否存在
            if (!fileStorageService.exist(remotePath)) {
                log.warn("远程用户数据不存在: userId={}, remotePath={}", userId, remotePath);
                return null;
            }
            
            // 创建目标目录
            Path targetDir = Paths.get(targetPath);
            Files.createDirectories(targetDir);
            
            // 创建临时目录
            Path tempDir = Paths.get(tempPath, "download", userId);
            Files.createDirectories(tempDir);
            
            // 下载压缩文件
            String compressedPath = tempDir.resolve(userId + USER_DATA_COMPRESSED_SUFFIX).toString();
            File compressedFile = fileStorageService.downloadFile(compressedPath, remotePath);
            
            if (compressedFile == null || !compressedFile.exists()) {
                throw new IOException("下载用户数据压缩文件失败");
            }
            
            // 解压JSON文件
            String jsonFilePath = targetDir.resolve("userdata" + USER_DATA_JSON_SUFFIX).toString();
            ZstdUtil.decompressJson(compressedPath, jsonFilePath);
            
            // 清理临时文件
            compressedFile.delete();
            
            log.info("用户数据下载成功: userId={}, localPath={}", userId, jsonFilePath);
            return jsonFilePath;
            
        } catch (Exception e) {
            log.error("下载用户数据失败: userId={}, targetPath={}", userId, targetPath, e);
            throw new RuntimeException("下载用户数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除用户数据
     * 流程：删除远程文件 -> 删除本地文件
     * 
     * @param userId 用户ID
     * @param localPath 本地用户数据路径（可选）
     * @return 是否删除成功
     */
    public boolean deleteUserData(String userId, String localPath) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        log.info("开始删除用户数据: userId={}, localPath={}", userId, localPath);
        
        boolean success = true;
        
        try {
            // 删除远程文件
            String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + USER_DATA_COMPRESSED_SUFFIX;
            if (fileStorageService.exist(remotePath)) {
                boolean deleted = fileStorageService.deleteFile(remotePath);
                if (deleted) {
                    log.info("远程用户数据删除成功: {}", remotePath);
                } else {
                    log.warn("远程用户数据删除失败: {}", remotePath);
                    success = false;
                }
            } else {
                log.info("远程用户数据不存在，跳过删除: {}", remotePath);
            }
            
            // 删除本地文件
            if (localPath != null && !localPath.trim().isEmpty()) {
                Path localDataPath = Paths.get(localPath);
                if (Files.exists(localDataPath)) {
                    if (Files.isDirectory(localDataPath)) {
                        // 删除目录
                        deleteDirectory(localDataPath.toFile());
                        log.info("本地用户数据目录删除成功: {}", localPath);
                    } else {
                        // 删除文件
                        Files.delete(localDataPath);
                        log.info("本地用户数据文件删除成功: {}", localPath);
                    }
                } else {
                    log.info("本地用户数据不存在，跳过删除: {}", localPath);
                }
            }
            
            // 清理临时文件
            Path tempDir = Paths.get(tempPath, userId);
            if (Files.exists(tempDir)) {
                deleteDirectory(tempDir.toFile());
            }
            
            log.info("用户数据删除完成: userId={}, success={}", userId, success);
            return success;
            
        } catch (Exception e) {
            log.error("删除用户数据失败: userId={}, localPath={}", userId, localPath, e);
            return false;
        }
    }
    
    /**
     * 检查用户数据是否存在
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean existsUserData(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        String remotePath = Constants.USER_DATA_PATH_PREFIX + userId + USER_DATA_COMPRESSED_SUFFIX;
        return fileStorageService.exist(remotePath);
    }
    
    /**
     * 查找用户数据JSON文件
     * 
     * @param dir 目录
     * @return JSON文件，如果不存在返回null
     */
    private File findUserDataJsonFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(USER_DATA_JSON_SUFFIX));
        
        if (files != null && files.length > 0) {
            // 优先查找userdata.json
            for (File file : files) {
                if ("userdata.json".equalsIgnoreCase(file.getName())) {
                    return file;
                }
            }
            // 如果没有userdata.json，返回第一个JSON文件
            return files[0];
        }
        
        return null;
    }
    
    /**
     * 递归删除目录
     * 
     * @param dir 目录
     */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        
        dir.delete();
    }
}
