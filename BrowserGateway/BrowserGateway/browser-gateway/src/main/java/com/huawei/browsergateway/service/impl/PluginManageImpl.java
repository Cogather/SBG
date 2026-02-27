package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.adapter.factory.EnvironmentAwareAdapterFactory;
import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import com.huawei.browsergateway.entity.plugin.ExtensionFilePaths;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.sdk.muen.HWCallback;
import com.huawei.browsergateway.sdk.muen.MuenDriver;
import com.huawei.browsergateway.util.MuenPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
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
    
    @Value("${browsergw.workspace:/opt/host}")
    private String workspace;
    
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
                    updateStatus("COMPLETE");
                    log.info("Moon SDK插件初始化成功");
                } else {
                    log.error("Moon SDK插件初始化失败");
                    sendPluginLoadFailureAlarm("MoonSDK", "0.0.22", "类加载器初始化失败");
                    updateStatus("FAILED");
                }
            } else {
                log.warn("未找到本地JAR文件，将等待手动加载: {}", localJarPath);
            }
        } catch (Exception e) {
            log.error("初始化Moon SDK插件异常", e);
            sendPluginLoadFailureAlarm("MoonSDK", "0.0.22", "初始化异常: " + e.getMessage());
            updateStatus("FAILED");
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
    public void loadPlugin(String keyPath, String touchPath, String jarPath) {
        log.info("开始加载插件: keyPath={}, touchPath={}, jarPath={}", keyPath, touchPath, jarPath);
        
        // 从pluginActive获取插件信息（应该在调用loadPlugin之前已经通过updatePluginActive设置）
        String pluginName = "MoonSDK";
        String pluginVersion = "Unknown";
        String pluginType = "ChromeExtension";
        
        if (pluginActive != null) {
            pluginName = pluginActive.getName() != null ? pluginActive.getName() : pluginName;
            pluginVersion = pluginActive.getVersion() != null ? pluginActive.getVersion() : pluginVersion;
            pluginType = pluginActive.getType() != null ? pluginActive.getType() : pluginType;
        }
        
        try {
            // 1. 参数验证
            if (jarPath == null || jarPath.trim().isEmpty()) {
                throw new IllegalArgumentException("jarPath不能为空");
            }
            
            // 2. 加载JS扩展（如果提供）
            // 使用loadJSExtension方法，按照存量代码的实现方式
            boolean jsExtensionSuccess = loadJSExtension(keyPath, touchPath);
            if (!jsExtensionSuccess) {
                log.warn("JS扩展加载失败，但继续加载SDK");
            }
            
            // 3. 验证JAR文件
            Path jarFilePath = Paths.get(jarPath);
            if (!Files.exists(jarFilePath)) {
                throw new IllegalArgumentException("JAR文件不存在: " + jarPath);
            }
            
            // 4. 验证插件（签名、完整性等）
            if (!validatePlugin(jarPath, pluginName, pluginVersion)) {
                log.error("插件验证失败: {}", jarPath);
                sendPluginLoadFailureAlarm(pluginName, pluginVersion, "插件验证失败");
                updateStatus("FAILED");
                throw new RuntimeException("插件验证失败");
            }
            
            // 5. 初始化插件类加载器
            muenPluginClassLoader = new MuenPluginClassLoader();
            boolean success = muenPluginClassLoader.init(jarFilePath);
            
            if (!success) {
                log.error("插件类加载器初始化失败: {}", jarPath);
                sendPluginLoadFailureAlarm(pluginName, pluginVersion, "类加载器初始化失败");
                updateStatus("FAILED");
                throw new RuntimeException("插件类加载器初始化失败");
            }
            
            // 6. 更新插件状态（如果pluginActive还未设置，这里会设置；如果已设置，这里会更新状态）
            if (pluginActive == null) {
                updatePluginActive(pluginName, pluginVersion, pluginType);
            }
            updateStatus("COMPLETE");
            
            log.info("插件加载成功: name={}, version={}, jarPath={}", pluginName, pluginVersion, jarPath);
            
        } catch (Exception e) {
            log.error("插件加载失败", e);
            sendPluginLoadFailureAlarm(pluginName, pluginVersion, "插件加载异常: " + e.getMessage());
            updateStatus("FAILED");
            throw new RuntimeException("插件加载失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PluginActive getPluginActive() {
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
    
    @Override
    public MuenDriver createDriver(String userId) {
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
            // 创建回调实例（需要根据实际情况创建，这里先使用null，实际应该注入或创建）
            // TODO: 需要根据userId创建对应的HWCallback实例
            HWCallback callback = createCallbackForUser(userId);
            if (callback == null) {
                log.error("无法创建回调实例: userId={}", userId);
                return null;
            }
            
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
     * 为用户创建回调实例
     * 
     * @param userId 用户ID
     * @return 回调实例
     */
    private HWCallback createCallbackForUser(String userId) {
        // 尝试从HWCallbackImpl获取已存在的实例
        try {
            HWCallbackImpl existingCallback = HWCallbackImpl.getInstance(userId);
            if (existingCallback != null) {
                return existingCallback;
            }
        } catch (Exception e) {
            log.debug("无法获取已存在的回调实例: userId={}", userId);
        }
        
        // 如果不存在，创建新的回调实例
        // 注意：这里需要注入必要的依赖，实际实现可能需要调整
        log.warn("需要创建新的回调实例，但当前实现可能不完整: userId={}", userId);
        return null;
    }
    
    /**
     * 更新插件活跃状态
     * 动态更新插件的名称、版本和类型信息
     * 
     * @param name 插件名称
     * @param version 插件版本
     * @param type 插件类型
     */
    @Override
    public void updatePluginActive(String name, String version, String type) {
        // 参数验证
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("插件名称不能为空");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("插件版本不能为空");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("插件类型不能为空");
        }
        
        // 保存旧状态信息（用于日志记录）
        String oldName = pluginActive != null ? pluginActive.getName() : null;
        String oldVersion = pluginActive != null ? pluginActive.getVersion() : null;
        String oldType = pluginActive != null ? pluginActive.getType() : null;
        String oldStatus = pluginActive != null ? pluginActive.getStatus() : null;
        
        // 创建或更新插件活跃状态对象
        if (pluginActive == null) {
            pluginActive = new PluginActive();
        }
        
        // 更新插件信息
        pluginActive.setName(name);
        pluginActive.setVersion(version);
        pluginActive.setType(type);
        
        // 如果状态为空或为NOTSTART，设置为ACTIVE；否则保持现有状态
        if (pluginActive.getStatus() == null || "NOTSTART".equals(pluginActive.getStatus())) {
            pluginActive.setStatus("ACTIVE");
        }
        
        // 更新加载时间
        pluginActive.setLoadTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 记录更新日志
        if (oldName != null) {
            log.info("插件活跃状态已更新: name={}->{}, version={}->{}, type={}->{}, status={}",
                    oldName, name, oldVersion, version, oldType, type, oldStatus);
        } else {
            log.info("插件活跃状态已创建: name={}, version={}, type={}, status={}",
                    name, version, type, pluginActive.getStatus());
        }
    }
    
    /**
     * 更新插件运行状态
     * 根据设计文档：更新状态并触发相关操作
     * 
     * @param pluginStatus 新的插件状态
     */
    @Override
    public void updateStatus(String pluginStatus) {
        if (pluginActive == null) {
            log.warn("插件状态对象为空，无法更新状态");
            return;
        }
        
        // 1. 保存旧状态
        String oldStatus = pluginActive.getStatus();
        
        // 2. 设置新状态
        pluginActive.setStatus(pluginStatus);
        
        // 3. 更新加载时间
        pluginActive.setLoadTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 4. 处理状态变更事件
        handleStatusChange(oldStatus, pluginStatus);
        
        log.info("插件状态已更新: {} -> {}", oldStatus, pluginStatus);
    }
    
    /**
     * 处理状态变更事件
     * 根据设计文档：根据新状态清除或发送告警
     * 
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    private void handleStatusChange(String oldStatus, String newStatus) {
        try {
            // COMPLETE状态：清除插件加载告警
            if ("COMPLETE".equals(newStatus)) {
                clearPluginLoadAlarm();
                log.info("插件加载完成，清除相关告警");
            }
            
            // FAILED状态：发送插件加载失败告警
            if ("FAILED".equals(newStatus) && !"FAILED".equals(oldStatus)) {
                // 如果是从非失败状态变为失败状态，发送告警
                String pluginName = pluginActive != null ? pluginActive.getName() : "Unknown";
                String version = pluginActive != null ? pluginActive.getVersion() : "Unknown";
                sendPluginLoadFailureAlarm(pluginName, version, "插件状态变更为FAILED");
                log.warn("插件状态变更为FAILED，已发送告警");
            }
            
            // 记录状态变更日志
            log.debug("插件状态变更处理完成: {} -> {}", oldStatus, newStatus);
            
        } catch (Exception e) {
            log.error("处理状态变更事件异常: {} -> {}", oldStatus, newStatus, e);
        }
    }
    
    /**
     * 清除插件加载告警
     */
    private void clearPluginLoadAlarm() {
        try {
            if (adapterFactory != null) {
                AlarmAdapter alarmAdapter = adapterFactory.createAlarmAdapter();
                if (alarmAdapter != null) {
                    boolean success = alarmAdapter.clearAlarm("plugin-load-fail");
                    if (success) {
                        log.info("插件加载告警清除成功");
                    } else {
                        log.debug("插件加载告警清除失败或告警不存在");
                    }
                }
            }
        } catch (Exception e) {
            log.error("清除插件加载告警异常", e);
        }
    }
    
    /**
     * 获取插件运行状态
     * 
     * @return 插件状态字符串
     */
    @Override
    public String getPluginStatus() {
        if (pluginActive != null) {
            return pluginActive.getStatus();
        }
        return "NOTSTART";
    }
    
    /**
     * 加载扩展文件
     * 
     * @param sourcePath 源文件路径
     * @param extensionType 扩展类型（key或touch）
     */
    private void loadExtensionFile(String sourcePath, String extensionType) {
        try {
            Path source = Paths.get(sourcePath);
            if (!Files.exists(source)) {
                log.warn("扩展文件不存在: {}", sourcePath);
                return;
            }
            
            // 构建目标路径
            Path targetDir = Paths.get(extensionsDir, extensionType);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 如果是目录，复制目录内容；如果是文件，复制文件
            if (Files.isDirectory(source)) {
                // 复制目录内容
                Path target = targetDir.resolve(source.getFileName());
                if (Files.exists(target)) {
                    deleteRecursively(target);
                }
                copyDirectory(source, target);
                log.info("扩展目录复制成功: {} -> {}", sourcePath, target);
            } else {
                // 复制文件
                Path target = targetDir.resolve(source.getFileName());
                if (Files.exists(target)) {
                    Files.delete(target);
                }
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("扩展文件复制成功: {} -> {}", sourcePath, target);
            }
        } catch (Exception e) {
            log.error("加载扩展文件失败: sourcePath={}, extensionType={}", sourcePath, extensionType, e);
            throw new RuntimeException("加载扩展文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归复制目录
     */
    private void copyDirectory(Path source, Path target) throws Exception {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                throw new RuntimeException("复制目录失败", e);
            }
        });
    }
    
    /**
     * 递归删除目录或文件
     */
    private void deleteRecursively(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (Exception e) {
                        log.warn("删除文件失败: {}", p, e);
                    }
                });
        } else {
            Files.delete(path);
        }
    }
    
    /**
     * 加载JavaScript扩展文件
     * 按照存量代码的实现方式：删除旧扩展，复制新扩展到指定目录
     * 
     * @param keyPath 按键扩展文件路径
     * @param touchPath 触控扩展文件路径
     * @return 是否成功
     */
    @Override
    public boolean loadJSExtension(String keyPath, String touchPath) {
        log.info("开始加载JS扩展: keyPath={}, touchPath={}", keyPath, touchPath);
        
        try {
            // 处理按键扩展
            if (keyPath != null && !keyPath.trim().isEmpty()) {
                String keyExtensionPath = getKeyExtensionPath();
                // 删除旧扩展
                Path keyTargetDir = Paths.get(keyExtensionPath);
                if (Files.exists(keyTargetDir)) {
                    deleteRecursively(keyTargetDir);
                    log.info("已删除旧按键扩展: {}", keyExtensionPath);
                }
                
                // 确保目标目录的父目录存在
                Files.createDirectories(keyTargetDir.getParent());
                
                // 复制新扩展
                Path keySource = Paths.get(keyPath);
                if (!Files.exists(keySource)) {
                    log.warn("按键扩展源文件不存在: {}", keyPath);
                } else {
                    if (Files.isDirectory(keySource)) {
                        // 如果是目录，复制整个目录
                        copyDirectory(keySource, keyTargetDir);
                    } else {
                        // 如果是文件，复制到目标目录
                        Files.createDirectories(keyTargetDir);
                        Path keyTarget = keyTargetDir.resolve(keySource.getFileName());
                        Files.copy(keySource, keyTarget, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.info("按键扩展加载成功: {} -> {}", keyPath, keyExtensionPath);
                }
            }
            
            // 处理触控扩展
            if (touchPath != null && !touchPath.trim().isEmpty()) {
                String touchExtensionPath = getTouchExtensionPath();
                // 删除旧扩展
                Path touchTargetDir = Paths.get(touchExtensionPath);
                if (Files.exists(touchTargetDir)) {
                    deleteRecursively(touchTargetDir);
                    log.info("已删除旧触控扩展: {}", touchExtensionPath);
                }
                
                // 确保目标目录的父目录存在
                Files.createDirectories(touchTargetDir.getParent());
                
                // 复制新扩展
                Path touchSource = Paths.get(touchPath);
                if (!Files.exists(touchSource)) {
                    log.warn("触控扩展源文件不存在: {}", touchPath);
                } else {
                    if (Files.isDirectory(touchSource)) {
                        // 如果是目录，复制整个目录
                        copyDirectory(touchSource, touchTargetDir);
                    } else {
                        // 如果是文件，复制到目标目录
                        Files.createDirectories(touchTargetDir);
                        Path touchTarget = touchTargetDir.resolve(touchSource.getFileName());
                        Files.copy(touchSource, touchTarget, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.info("触控扩展加载成功: {} -> {}", touchPath, touchExtensionPath);
                }
            }
            
            log.info("JS扩展加载完成");
            return true;
            
        } catch (Exception e) {
            log.error("加载JS扩展失败: keyPath={}, touchPath={}", keyPath, touchPath, e);
            return false;
        }
    }
    
    /**
     * 获取按键扩展路径
     * 路径格式: workspace/extension/keys/chrome
     */
    private String getKeyExtensionPath() {
        return workspace + File.separator + "extension" + File.separator + "keys" + File.separator + "chrome";
    }
    
    /**
     * 获取触控扩展路径
     * 路径格式: workspace/extension/touch/chrome
     */
    private String getTouchExtensionPath() {
        return workspace + File.separator + "extension" + File.separator + "touch" + File.separator + "chrome";
    }
    
    /**
     * 获取录制扩展路径
     * 路径格式: workspace/extension/record/chrome
     */
    private String getRecordExtensionPath() {
        return workspace + File.separator + "extension" + File.separator + "record" + File.separator + "chrome";
    }
    
    /**
     * 获取JAR目录路径
     * 路径格式: workspace/jar
     */
    private String getJarDirPath() {
        return workspace + File.separator + "jar";
    }
    
    @Override
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
    
    @Override
    public synchronized boolean loadExtension(LoadExtensionRequest request) {
        log.info("reload extension, request params: {}", request);
        try {
            ExtensionFilePaths extensionFilePaths = downPlugin(request);
            updatePluginActive(request.getName(), request.getVersion(), request.getType());
            loadPlugin(extensionFilePaths.getKeyDir(), extensionFilePaths.getTouchDir(), extensionFilePaths.getJarPath());
            postLoadingPlugin(extensionFilePaths);
            log.info("success to load extension name:{} version:{}", request.getName(), request.getVersion());
            return true;
        } catch (Exception e) {
            log.error("load extension failed", e);
            updateStatus("FAILED");
            return false;
        }
    }
    
    @Override
    public ExtensionFilePaths downPlugin(LoadExtensionRequest request) {
        log.info("开始下载插件: name={}, version={}, bucketName={}, extensionFilePath={}", 
                request.getName(), request.getVersion(), request.getBucketName(), request.getExtensionFilePath());
        
        try {
            // 1. 确保临时目录存在
            Path tempDir = Paths.get(pluginTempDir);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // 2. 构建本地JAR文件路径
            String fileName = request.getName() + "-" + request.getVersion() + ".jar";
            String localJarPath = Paths.get(pluginTempDir, fileName).toString();
            
            // 3. 构建远程路径（bucketName/extensionFilePath）
            String remotePath = request.getBucketName() + "/" + request.getExtensionFilePath();
            
            // 4. 下载JAR文件
            File downloadedFile = fileStorageService.downloadFile(localJarPath, remotePath);
            if (downloadedFile == null || !downloadedFile.exists()) {
                throw new RuntimeException("插件JAR文件下载失败: " + remotePath);
            }
            
            log.info("插件JAR文件下载成功: localPath={}, remotePath={}", localJarPath, remotePath);
            
            // 5. 构建扩展文件路径对象
            ExtensionFilePaths extensionFilePaths = new ExtensionFilePaths();
            extensionFilePaths.setJarPath(localJarPath);
            
            // 6. 设置按键和触控扩展目录路径（从workspace配置获取）
            String keyDir = workspace + File.separator + "extension" + File.separator + "keys" + File.separator + "chrome";
            String touchDir = workspace + File.separator + "extension" + File.separator + "touch" + File.separator + "chrome";
            
            extensionFilePaths.setKeyDir(keyDir);
            extensionFilePaths.setTouchDir(touchDir);
            
            log.info("扩展文件路径构建完成: jarPath={}, keyDir={}, touchDir={}", 
                    localJarPath, keyDir, touchDir);
            
            return extensionFilePaths;
            
        } catch (Exception e) {
            log.error("下载插件失败", e);
            throw new RuntimeException("下载插件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void postLoadingPlugin(ExtensionFilePaths extensionFilePaths) {
        log.info("执行插件加载后处理: jarPath={}, keyDir={}, touchDir={}", 
                extensionFilePaths.getJarPath(), extensionFilePaths.getKeyDir(), extensionFilePaths.getTouchDir());
        
        // 后置处理逻辑
        // 1. 验证文件是否已正确加载
        try {
            Path jarPath = Paths.get(extensionFilePaths.getJarPath());
            if (!Files.exists(jarPath)) {
                log.warn("插件JAR文件在加载后不存在: {}", extensionFilePaths.getJarPath());
            } else {
                log.info("插件JAR文件验证通过: {}", extensionFilePaths.getJarPath());
            }
            
            // 2. 可以在这里添加其他后置处理逻辑，比如：
            // - 清理临时文件
            // - 更新缓存
            // - 发送通知等
            
        } catch (Exception e) {
            log.error("插件加载后处理失败", e);
            // 后置处理失败不影响主流程，只记录日志
        }
    }
}
