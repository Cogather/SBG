package com.huawei.browsergateway.util;

import com.huawei.browsergateway.sdk.HWCallback;
import com.huawei.browsergateway.sdk.MuenDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Moon SDK插件类加载器
 * 负责动态加载插件JAR文件并创建驱动实例
 */
public class MuenPluginClassLoader {
    
    private static final Logger log = LoggerFactory.getLogger(MuenPluginClassLoader.class);
    
    private URLClassLoader classLoader;
    private Class<?> driverImplClass;
    private Path currentJarFile;
    
    /**
     * 初始化类加载器并加载插件
     * 
     * @param jarPath JAR文件路径
     * @return 是否初始化成功
     */
    public boolean init(Path jarPath) {
        try {
            // 1. 准备插件目录结构
            if (!Files.exists(jarPath) || !Files.isRegularFile(jarPath)) {
                log.error("JAR文件不存在或不是文件: {}", jarPath);
                return false;
            }
            
            // 2. 创建URLClassLoader实例
            URL jarUrl = jarPath.toUri().toURL();
            classLoader = new URLClassLoader(new URL[]{jarUrl}, 
                    Thread.currentThread().getContextClassLoader());
            
            // 3. 扫描JAR文件获取所有类名列表
            List<String> classNames = listAllClassName(jarPath);
            log.debug("扫描到{}个类", classNames.size());
            
            // 4. 查找符合条件的MuenDriver实现类
            driverImplClass = findDriverImpl(classNames);
            if (driverImplClass == null) {
                log.error("未找到MuenDriver实现类");
                cleanup();
                return false;
            }
            
            // 5. 缓存当前JAR文件路径
            currentJarFile = jarPath;
            
            log.info("插件类加载器初始化成功: jarPath={}, driverClass={}", 
                    jarPath, driverImplClass.getName());
            return true;
            
        } catch (Exception e) {
            log.error("插件类加载器初始化失败: jarPath={}", jarPath, e);
            cleanup();
            return false;
        }
    }
    
    /**
     * 创建驱动实例
     * 根据文档：通过反射实例化MuenDriver，传入HWCallback参数
     * 
     * @param hwCallback 回调接口
     * @return 驱动实例，失败返回null
     */
    public MuenDriver createDriverInstance(HWCallback hwCallback) {
        if (driverImplClass == null || hwCallback == null) {
            log.error("驱动实现类或回调接口为空");
            return null;
        }
        
        try {
            // 查找接受HWCallback参数的构造函数
            // 根据文档：MuenDriver构造函数强制注入HWCallback回调实例
            Constructor<?> constructor = driverImplClass.getConstructor(HWCallback.class);
            
            // 使用构造函数实例化驱动对象
            Object driverObj = constructor.newInstance(hwCallback);
            
            // 类型转换为MuenDriver
            if (driverObj instanceof MuenDriver) {
                log.info("MuenDriver实例创建成功: {}", driverImplClass.getName());
                return (MuenDriver) driverObj;
            } else {
                log.error("驱动对象不是MuenDriver类型: {}", driverObj.getClass().getName());
                return null;
            }
        } catch (NoSuchMethodException e) {
            log.error("未找到接受HWCallback参数的构造函数", e);
            return null;
        } catch (Exception e) {
            log.error("创建驱动实例失败", e);
            return null;
        }
    }
    
    /**
     * 查找JAR文件中所有类名称
     */
    private List<String> listAllClassName(Path jarPath) {
        List<String> classNames = new ArrayList<>();
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // 跳过目录和module-info.class
                if (entry.isDirectory() || entryName.equals("module-info.class")) {
                    continue;
                }
                
                // 只处理.class文件
                if (entryName.endsWith(".class")) {
                    // 将路径转换为类名格式（去掉.class后缀，替换/为.）
                    String className = entryName
                            .substring(0, entryName.length() - 6)
                            .replace('/', '.');
                    
                    // 过滤com.moon包下的类，排除内部类（包含$的类）
                    // 根据文档：只保留com.moon包下的类
                    if (className.startsWith("com.moon") && !className.contains("$")) {
                        classNames.add(className);
                        log.trace("找到com.moon包下的类: {}", className);
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取JAR文件失败: {}", jarPath, e);
        }
        
        return classNames;
    }
    
    /**
     * 查找驱动实现类
     */
    private Class<?> findDriverImpl(List<String> classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                
                // 检查是否为驱动候选
                if (isDriverCandidate(clazz)) {
                    log.info("找到驱动实现类: {}", className);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                log.warn("类加载失败: {}", className, e);
                // 继续查找下一个类
            } catch (Exception e) {
                log.warn("检查类失败: {}", className, e);
            }
        }
        
        return null;
    }
    
    /**
     * 判断类是否为驱动候选
     * 根据文档：检查类是否可实例化(非接口、抽象类等)，验证是否为MuenDriver子类
     */
    private boolean isDriverCandidate(Class<?> clazz) {
        // 检查是否为接口或抽象类
        if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        
        // 检查是否为枚举或注解
        if (clazz.isEnum() || clazz.isAnnotation()) {
            return false;
        }
        
        // 检查是否为MuenDriver的实现类
        // 优先尝试从SDK中加载MuenDriver接口进行判断
        try {
            // 尝试从SDK中加载MuenDriver接口
            Class<?> sdkMuenDriver = classLoader.loadClass("com.moon.cloud.browser.sdk.core.MuenDriver");
            if (sdkMuenDriver.isAssignableFrom(clazz)) {
                log.debug("找到SDK MuenDriver实现类: {}", clazz.getName());
                return true;
            }
        } catch (ClassNotFoundException e) {
            // 如果SDK中没有MuenDriver接口，尝试使用本地接口判断
            // 注意：由于类加载器不同，直接使用isAssignableFrom可能失败
            log.debug("SDK中未找到MuenDriver接口，尝试使用本地接口判断: {}", clazz.getName());
        }
        
        // 备用方案：检查类是否实现了MuenDriver接口的方法
        // 通过检查是否有接受HWCallback参数的构造函数来判断
        try {
            clazz.getConstructor(HWCallback.class);
            log.debug("找到符合MuenDriver特征的类（有HWCallback构造函数）: {}", clazz.getName());
            return true;
        } catch (NoSuchMethodException e) {
            // 没有符合的构造函数，不是驱动类
            return false;
        }
    }
    
    /**
     * 清理资源
     */
    public void close() {
        cleanup();
    }
    
    /**
     * 内部清理方法
     */
    private void cleanup() {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.error("关闭类加载器失败", e);
            }
            classLoader = null;
        }
        
        driverImplClass = null;
        currentJarFile = null;
    }
}
