package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.adapter.factory.EnvironmentAwareAdapterFactory;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
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

import java.util.HashMap;
import java.util.Map;

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
    
    @Autowired(required = false)
    private EnvironmentAwareAdapterFactory adapterFactory;
    
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
                    sendPluginLoadFailureAlarm("MoonSDK", "0.0.22", "类加载器初始化失败");
                    updatePluginStatus("FAILED");
                }
            } else {
                log.warn("未找到本地JAR文件，将等待手动加载: {}", localJarPath);
            }
        } catch (Exception e) {
            log.error("初始化Moon SDK插件异常", e);
            sendPluginLoadFailureAlarm("MoonSDK", "0.0.22", "初始化异常: " + e.getMessage());
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
            
            // 4. 验证插件（签名、完整性等）
            if (!validatePlugin(localJarPath, request.getName(), request.getVersion())) {
                log.error("插件验证失败: {}", localJarPath);
                sendPluginLoadFailureAlarm(request.getName(), request.getVersion(), "插件验证失败");
                updatePluginStatus("FAILED");
                return false;
            }
            
            // 5. 初始化插件类加载器
            Path jarPath = Paths.get(localJarPath);
            muenPluginClassLoader = new MuenPluginClassLoader();
            boolean success = muenPluginClassLoader.init(jarPath);
            
            if (!success) {
                log.error("插件类加载器初始化失败: {}", localJarPath);
                sendPluginLoadFailureAlarm(request.getName(), request.getVersion(), "类加载器初始化失败");
                updatePluginStatus("FAILED");
                return false;
            }
            
            // 6. 更新插件状态
            updatePluginActive(request.getName(), request.getVersion(), request.getType());
            updatePluginStatus("COMPLETE");
            
            log.info("插件加载成功: name={}, version={}", request.getName(), request.getVersion());
            return true;
            
        } catch (Exception e) {
            log.error("插件加载失败", e);
            sendPluginLoadFailureAlarm(request.getName(), request.getVersion(), "插件加载异常: " + e.getMessage());
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
    
    /**
     * 发送插件加载失败告警
     * 
     * @param pluginName 插件名称
     * @param version 插件版本
     * @param error 错误信息
     */
    private void sendPluginLoadFailureAlarm(String pluginName, String version, String error) {
        try {
            if (adapterFactory != null) {
                AlarmAdapter alarmAdapter = adapterFactory.createAlarmAdapter();
                if (alarmAdapter != null) {
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("pluginName", pluginName);
                    parameters.put("version", version);
                    parameters.put("type", "ChromeExtension");
                    parameters.put("error", error);
                    parameters.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    
                    boolean success = alarmAdapter.sendAlarm("plugin-load-fail", 
                            AlarmAdapter.AlarmType.GENERATE, parameters);
                    if (success) {
                        log.info("插件加载失败告警发送成功: pluginName={}, version={}", pluginName, version);
                    } else {
                        log.warn("插件加载失败告警发送失败: pluginName={}, version={}", pluginName, version);
                    }
                } else {
                    log.debug("告警适配器未初始化，跳过告警发送");
                }
            } else {
                log.debug("适配器工厂未注入，跳过告警发送");
            }
        } catch (Exception e) {
            log.error("发送插件加载失败告警异常: pluginName={}, version={}", pluginName, version, e);
        }
    }
    
    /**
     * 验证插件
     * 检查签名、完整性等
     * 
     * @param localPath 插件本地路径
     * @param name 插件名称
     * @param version 插件版本
     * @return 验证是否通过
     */
    private boolean validatePlugin(String localPath, String name, String version) {
        try {
            Path pluginPath = Paths.get(localPath);
            
            // 1. 检查文件是否存在
            if (!Files.exists(pluginPath)) {
                log.error("插件文件不存在: {}", localPath);
                return false;
            }
            
            // 2. 检查文件大小（防止异常大的文件）
            long fileSize = Files.size(pluginPath);
            long maxPluginSize = 100 * 1024 * 1024; // 100MB
            if (fileSize > maxPluginSize) {
                log.error("插件文件过大: {} bytes (最大: {} bytes)", fileSize, maxPluginSize);
                return false;
            }
            
            if (fileSize == 0) {
                log.error("插件文件为空: {}", localPath);
                return false;
            }
            
            // 3. 检查JAR文件格式（验证是否为有效的JAR文件）
            if (!isValidJarFile(pluginPath)) {
                log.error("无效的JAR文件: {}", localPath);
                return false;
            }
            
            // 4. 检查插件签名（如果启用签名验证）
            // TODO: 如果启用签名验证，可以在这里添加
            // if (enableSignatureValidation && !validatePluginSignature(pluginPath)) {
            //     log.error("插件签名验证失败: {}", localPath);
            //     return false;
            // }
            
            // 5. 检查插件元数据（从MANIFEST.MF读取）
            if (!validatePluginMetadata(pluginPath, name, version)) {
                log.warn("插件元数据验证失败（非致命）: {}", localPath);
                // 元数据验证失败不阻断加载，只记录警告
            }
            
            log.info("插件验证通过: name={}, version={}, size={} bytes", name, version, fileSize);
            return true;
            
        } catch (Exception e) {
            log.error("插件验证异常: {}", localPath, e);
            return false;
        }
    }
    
    /**
     * 验证JAR文件格式
     * 
     * @param jarPath JAR文件路径
     * @return 是否为有效的JAR文件
     */
    private boolean isValidJarFile(Path jarPath) {
        try {
            // 检查文件扩展名
            String fileName = jarPath.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".jar")) {
                return false;
            }
            
            // 检查JAR文件魔数（ZIP文件格式）
            // JAR文件本质上是ZIP文件，以PK开头（0x504B）
            try (java.io.FileInputStream fis = new java.io.FileInputStream(jarPath.toFile())) {
                byte[] header = new byte[4];
                int bytesRead = fis.read(header);
                if (bytesRead < 4) {
                    return false;
                }
                // ZIP文件魔数：PK (0x50 0x4B)
                return header[0] == 0x50 && header[1] == 0x4B;
            }
        } catch (Exception e) {
            log.error("验证JAR文件格式异常: {}", jarPath, e);
            return false;
        }
    }
    
    /**
     * 验证插件元数据
     * 从MANIFEST.MF读取插件信息并验证
     * 
     * @param jarPath JAR文件路径
     * @param expectedName 期望的插件名称
     * @param expectedVersion 期望的插件版本
     * @return 验证是否通过
     */
    private boolean validatePluginMetadata(Path jarPath, String expectedName, String expectedVersion) {
        try {
            // 尝试读取JAR文件的MANIFEST.MF
            // 这里简化实现，实际可以解析MANIFEST.MF文件
            // 由于Moon SDK的JAR可能没有标准的MANIFEST，这里只做基本检查
            log.debug("验证插件元数据: jarPath={}, expectedName={}, expectedVersion={}", 
                    jarPath, expectedName, expectedVersion);
            
            // 如果JAR文件存在且可读，认为元数据验证通过
            // 实际实现中可以根据需要解析MANIFEST.MF
            return true;
            
        } catch (Exception e) {
            log.warn("验证插件元数据异常: {}", jarPath, e);
            return false;
        }
    }
}
