package com.huawei.browsergateway.service;

import cn.hutool.core.collection.CollectionUtil;
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

import com.moon.cloud.browser.sdk.core.HWCallback;
import com.moon.cloud.browser.sdk.core.MuenDriver;

public class MuenPluginClassLoader {
    private static final Logger log = LogManager.getLogger(MuenPluginClassLoader.class);
    private URLClassLoader classLoader;
    private final String MUENGROUPPREFIX = "com.moon";
    private Class<?> driverImplClass;

    public boolean init(Path jarPath) {
        log.info("load jar from {}", jarPath);
        try {
            classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, Thread.currentThread().getContextClassLoader());
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

    public MuenDriver createDriverInstance(HWCallback hwCallback)  {
        if(driverImplClass == null || hwCallback == null) {
            return null;
        }
        MuenDriver muenDriver = null;
        try {
            muenDriver = (MuenDriver) driverImplClass.getConstructor(HWCallback.class).newInstance(hwCallback);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("failed to instance MuenDriver", e);
        }
        return muenDriver;
    }

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
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                result.add(className);
            }
        } catch (IOException e) {
            log.error("failed to list all class names", e);
            return CollectionUtil.empty(String.class);
        }
        result = result.stream()
                .filter(className -> className.startsWith(MUENGROUPPREFIX))  // 只保留muen类
                .filter(className -> !className.contains("$"))               // 过滤子类
                .collect(Collectors.toList());
        return result;
    }

    private static boolean isInstantiable(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum() && !clazz.isAnnotation();
    }

    private Class<?> findDriverImpl(List<String> classNames) {
        if (CollectionUtil.isEmpty(classNames)) {
            return null;
        }
        for (var clazz : classNames) {
            try {
                Class<?> loadedClass = classLoader.loadClass(clazz);
                if (isInstantiable(loadedClass) && MuenDriver.class.isAssignableFrom(loadedClass)) {
                    return loadedClass;
                }
            } catch (ClassNotFoundException e) {
                log.error("cannot load class of {} ", clazz, e);
                break;
            }

        }
        return null;
    }

    public void close() {
        try {
            classLoader.close();
        } catch (IOException e) {
            log.error("close classLoader failed.", e);
        }
    }
}

