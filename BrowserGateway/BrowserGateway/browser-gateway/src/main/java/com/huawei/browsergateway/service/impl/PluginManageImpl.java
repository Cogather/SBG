package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.sdk.HWCallback;
import com.huawei.browsergateway.sdk.MuenDriver;
import com.huawei.browsergateway.util.MuenPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 插件管理服务实现类
 */
@Service
public class PluginManageImpl implements IPluginManage {
    
    private static final Logger log = LoggerFactory.getLogger(PluginManageImpl.class);
    
    @Autowired
    private IFileStorage fileStorageService;
    
    @Value("${browsergw.plugin.temp-dir:/tmp/browsergateway/plugins}")
    private String pluginTempDir;
    
    @Value("${browsergw.plugin.extensions-dir:/opt/browsergateway/extensions}")
    private String extensionsDir;
    
    @Value("${browsergw.plugin.local-jar-path:src/main/resources/lib/original-browser-module-sdk-api-0.0.22.jar}")
    private String localJarPath;
    
    // 插件类加载器
    private MuenPluginClassLoader muenPluginClassLoader;
    
    // 插件状态信息
    private volatile PluginActive pluginActive;
    
    // 驱动实例缓存（按userId缓存）
    private final ConcurrentMap<String, MuenDriver> driverCache = new ConcurrentHashMap<>();
    
    /**
     * 应用启动时自动加载本地JAR文件
     * 根据文档：JAR包已放置在src/main/resources/lib/original-browser-module-sdk-api-0.0.22.jar
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化Moon SDK插件...");
        
        try {
            // 尝试从本地资源目录加载JAR文件
            Path jarPath = loadLocalJarFile();
            if (jarPath != null && Files.exists(jarPath)) {
                log.info("找到本地JAR文件: {}", jarPath);
                
                // 初始化插件类加载器
                muenPluginClassLoader = new MuenPluginClassLoader();
                boolean success = muenPluginClassLoader.init(jarPath);
                
                if (success) {
                    updatePluginActive("MoonSDK", "0.0.22", "ChromeExtension");
                    updatePluginStatus("COMPLETE");
                    log.info("Moon SDK插件初始化成功");
                } else {
                    log.error("Moon SDK插件初始化失败");
                    updatePluginStatus("FAILED");
                }
            } else {
                log.warn("未找到本地JAR文件，将等待手动加载: {}", localJarPath);
            }
        } catch (Exception e) {
            log.error("初始化Moon SDK插件异常", e);
            updatePluginStatus("FAILED");
        }
    }
    
    /**
     * 加载本地JAR文件
     * 优先从资源目录加载，如果不存在则尝试从文件系统路径加载
     */
    private Path loadLocalJarFile() {
        try {
            // 1. 尝试从classpath资源加载
            String resourcePath = "lib/original-browser-module-sdk-api-0.0.22.jar";
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (resourceStream != null) {
                log.info("从classpath加载JAR文件: {}", resourcePath);
                
                // 将资源文件复制到临时目录
                Path tempDir = Paths.get(pluginTempDir);
                if (!Files.exists(tempDir)) {
                    Files.createDirectories(tempDir);
                }
                
                Path tempJarPath = tempDir.resolve("original-browser-module-sdk-api-0.0.22.jar");
                Files.copy(resourceStream, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
                resourceStream.close();
                
                log.info("JAR文件已复制到临时目录: {}", tempJarPath);
                return tempJarPath;
            }
            
            // 2. 尝试从配置的文件系统路径加载
            Path fileSystemPath = Paths.get(localJarPath);
            if (Files.exists(fileSystemPath)) {
                log.info("从文件系统加载JAR文件: {}", fileSystemPath);
                return fileSystemPath;
            }
            
            // 3. 尝试从工作目录加载
            Path workingDirPath = Paths.get(System.getProperty("user.dir"), localJarPath);
            if (Files.exists(workingDirPath)) {
                log.info("从工作目录加载JAR文件: {}", workingDirPath);
                return workingDirPath;
            }
            
            log.warn("未找到JAR文件，尝试路径: classpath:{}, filesystem:{}, workingdir:{}", 
                    resourcePath, fileSystemPath, workingDirPath);
            return null;
            
        } catch (Exception e) {
            log.error("加载本地JAR文件失败", e);
            return null;
        }
    }
    
    @Override
    public boolean loadExtension(LoadExtensionRequest request) {
        log.info("开始加载插件: name={}, version={}, type={}", 
                request.getName(), request.getVersion(), request.getType());
        
        try {
            // 1. 参数验证
            validateRequest(request);
            
            // 2. 关闭所有现有浏览器实例（如果需要）
            // 注意：这里需要依赖IChromeSet，但为了避免循环依赖，先注释
            // chromeSet.deleteAll();
            
            // 3. 下载插件JAR文件
            String localJarPath = downloadPluginJar(request);
            
            // 4. 初始化插件类加载器
            Path jarPath = Paths.get(localJarPath);
            muenPluginClassLoader = new MuenPluginClassLoader();
            boolean success = muenPluginClassLoader.init(jarPath);
            
            if (!success) {
                log.error("插件类加载器初始化失败: {}", localJarPath);
                updatePluginStatus("FAILED");
                return false;
            }
            
            // 5. 更新插件状态
            updatePluginActive(request.getName(), request.getVersion(), request.getType());
            updatePluginStatus("COMPLETE");
            
            log.info("插件加载成功: name={}, version={}", request.getName(), request.getVersion());
            return true;
            
        } catch (Exception e) {
            log.error("插件加载失败", e);
            updatePluginStatus("FAILED");
            return false;
        }
    }
    
    @Override
    public PluginActive getPluginInfo() {
        if (pluginActive == null) {
            // 返回默认状态
            PluginActive defaultActive = new PluginActive();
            defaultActive.setName("MoonSDK");
            defaultActive.setVersion("Unknown");
            defaultActive.setType("ChromeExtension");
            defaultActive.setStatus("NOTSTART");
            return defaultActive;
        }
        return pluginActive;
    }
    
    /**
     * 创建驱动实例
     * 
     * @param userId 用户ID
     * @param callback 回调接口
     * @return 驱动实例，失败返回null
     */
    public MuenDriver createDriver(String userId, HWCallback callback) {
        if (muenPluginClassLoader == null) {
            log.warn("插件未加载，无法创建驱动实例: userId={}", userId);
            return null;
        }
        
        // 检查缓存
        MuenDriver cachedDriver = driverCache.get(userId);
        if (cachedDriver != null) {
            log.debug("使用缓存的驱动实例: userId={}", userId);
            return cachedDriver;
        }
        
        try {
            // 创建驱动实例
            MuenDriver driver = muenPluginClassLoader.createDriverInstance(callback);
            if (driver != null) {
                driverCache.put(userId, driver);
                log.info("驱动实例创建成功: userId={}", userId);
            } else {
                log.error("驱动实例创建失败: userId={}", userId);
            }
            return driver;
        } catch (Exception e) {
            log.error("创建驱动实例异常: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 更新插件活跃状态
     */
    public void updatePluginActive(String name, String version, String type) {
        PluginActive active = new PluginActive();
        active.setName(name);
        active.setVersion(version);
        active.setType(type);
        active.setStatus("ACTIVE");
        active.setLoadTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        this.pluginActive = active;
    }
    
    /**
     * 更新插件运行状态
     */
    public void updatePluginStatus(String status) {
        if (pluginActive != null) {
            pluginActive.setStatus(status);
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(LoadExtensionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (request.getBucketName() == null || request.getBucketName().trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName不能为空");
        }
        if (request.getExtensionFilePath() == null || request.getExtensionFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("extensionFilePath不能为空");
        }
        if (!request.getExtensionFilePath().endsWith(".jar")) {
            throw new IllegalArgumentException("extensionFilePath必须以.jar结尾");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name不能为空");
        }
        if (request.getVersion() == null || request.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("version不能为空");
        }
    }
    
    /**
     * 下载插件JAR文件
     */
    private String downloadPluginJar(LoadExtensionRequest request) throws Exception {
        // 确保临时目录存在
        Path tempDir = Paths.get(pluginTempDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // 构建本地文件路径
        String fileName = request.getName() + "-" + request.getVersion() + ".jar";
        String localPath = Paths.get(pluginTempDir, fileName).toString();
        
        // 构建远程路径（bucketName/extensionFilePath）
        String remotePath = request.getBucketName() + "/" + request.getExtensionFilePath();
        
        // 下载文件
        File downloadedFile = fileStorageService.downloadFile(localPath, remotePath);
        if (downloadedFile == null || !downloadedFile.exists()) {
            throw new RuntimeException("插件JAR文件下载失败: " + remotePath);
        }
        
        log.info("插件JAR文件下载成功: localPath={}, remotePath={}", localPath, remotePath);
        return localPath;
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("插件管理器关闭，清理资源");
        
        // 清理驱动缓存
        driverCache.clear();
        
        // 关闭类加载器
        if (muenPluginClassLoader != null) {
            muenPluginClassLoader.close();
            muenPluginClassLoader = null;
        }
        
        // 重置状态
        pluginActive = null;
    }
}
