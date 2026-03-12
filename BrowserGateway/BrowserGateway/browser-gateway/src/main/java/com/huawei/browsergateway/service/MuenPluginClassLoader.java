package com.huawei.browsergateway.service;

import cn.hutool.core.collection.CollectionUtil;
import com.moon.cloud.browser.sdk.core.HWCallback;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 动态 JAR 类加载器，从插件包中扫描并实例化 MuenDriver 实现类
 */
public class MuenPluginClassLoader {

    private static final Logger log = LogManager.getLogger(MuenPluginClassLoader.class);

    /** 只扫描 com.moon 包下的类 */
    private static final String MUEN_GROUP_PREFIX = "com.moon";

    private URLClassLoader classLoader;
    private Class<?> driverImplClass;

    /**
     * 初始化类加载器并从 JAR 中查找 MuenDriver 实现类
     *
     * @param jarPath JAR 文件路径
     * @return 初始化是否成功
     */
    public boolean init(Path jarPath) {
        log.info("load jar from {}", jarPath);
        try {
            classLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    Thread.currentThread().getContextClassLoader());
        } catch (MalformedURLException e) {
            log.error("failed to new urlClassLoader", e);
            return false;
        }

        List<String> classNames = listAllClassName(jarPath);
        driverImplClass = findDriverImpl(classNames);
        if (driverImplClass == null) {
            log.warn("cannot find the driver implement class from {}", jarPath);
            return false;
        }
        return true;
    }

    /**
     * 创建 MuenDriver 实例，注入 HWCallback 回调
     *
     * @param hwCallback 回调实现
     * @return MuenDriver 实例，失败时返回 null
     */
    public MuenDriver createDriverInstance(HWCallback hwCallback) {
        if (driverImplClass == null || hwCallback == null) {
            return null;
        }
        try {
            return (MuenDriver) driverImplClass.getConstructor(HWCallback.class).newInstance(hwCallback);
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            log.error("failed to instance MuenDriver", e);
            return null;
        }
    }

    /** 关闭类加载器，释放 JAR 文件句柄 */
    public void close() {
        try {
            classLoader.close();
        } catch (IOException e) {
            log.error("close classLoader failed.", e);
        }
    }

    /** 列出 JAR 中所有符合条件的类名（muen 包、非内部类） */
    private List<String> listAllClassName(Path path) {
        List<String> result = new LinkedList<>();
        try (JarFile jarFile = new JarFile(path.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory()) continue;
                if (!name.endsWith(".class")) continue;
                if (name.equals("module-info.class")) continue;
                result.add(name.substring(0, name.length() - 6).replace('/', '.'));
            }
        } catch (IOException e) {
            log.error("failed to list all class names", e);
            return CollectionUtil.empty(String.class);
        }

        return result.stream()
                .filter(cn -> cn.startsWith(MUEN_GROUP_PREFIX))  // 只保留 muen 类
                .filter(cn -> !cn.contains("$"))                  // 过滤内部类
                .collect(Collectors.toList());
    }

    /** 判断类是否可实例化（非接口、非抽象、非枚举、非注解） */
    private static boolean isInstantiable(Class<?> clazz) {
        return !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())
                && !clazz.isEnum()
                && !clazz.isAnnotation();
    }

    /** 从类名列表中找到第一个 MuenDriver 的可实例化实现类 */
    private Class<?> findDriverImpl(List<String> classNames) {
        if (CollectionUtil.isEmpty(classNames)) {
            return null;
        }
        for (String clazz : classNames) {
            try {
                Class<?> loaded = classLoader.loadClass(clazz);
                if (isInstantiable(loaded) && MuenDriver.class.isAssignableFrom(loaded)) {
                    return loaded;
                }
            } catch (ClassNotFoundException e) {
                log.error("cannot load class of {} ", clazz, e);
                break;
            }
        }
        return null;
    }
}
