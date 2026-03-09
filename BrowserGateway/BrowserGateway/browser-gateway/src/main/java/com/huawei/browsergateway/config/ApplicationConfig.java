package com.huawei.browsergateway.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * 应用配置类
 * 同时提供操作系统类型判断的工具方法，供其他模块按平台做差异化处理。
 */
@Configuration
public class ApplicationConfig {

    private static final Logger log = LogManager.getLogger(ApplicationConfig.class);

    /** 当前操作系统名称（小写） */
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    /**
     * 判断当前是否运行在 Windows 系统上
     *
     * @return 是 Windows 系统返回 true，否则返回 false
     */
    public static boolean isWindows() {
        return OS_NAME.contains("windows");
    }

    /**
     * 判断当前是否运行在 Linux 系统上
     *
     * @return 是 Linux 系统返回 true，否则返回 false
     */
    public static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    /**
     * 获取当前操作系统名称（小写）
     *
     * @return 操作系统名称
     */
    public static String getOsName() {
        return OS_NAME;
    }

    /**
     * 配置 PropertySourcesPlaceholderConfigurer
     * <p>
     * 根据运行环境动态加载配置文件，优先使用外部 conf 目录下的配置。
     * </p>
     *
     * @return PropertySourcesPlaceholderConfigurer 实例
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        log.info("Current OS: {} | isWindows: {}", OS_NAME, isWindows());

        String jarPath = new File(ApplicationConfig.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParent();

        String configPath;
        if (isWindows()) {
            // Windows 开发环境：加载源码目录下的配置文件
            configPath = Paths.get(jarPath, "..", "src", "main", "resources").resolve("application.yaml").toString();
        } else {
            // Linux 生产环境：加载 JAR 包同级 conf 目录下的配置文件
            configPath = Paths.get(jarPath, "conf").resolve("application.yaml").toString();
        }

        Resource fileResource = new FileSystemResource(configPath);
        Resource classpathResource = new ClassPathResource("application.yaml");
        boolean useExternal = fileResource.exists();
        Resource activeResource = useExternal ? fileResource : classpathResource;

        log.info("Loading config from: {}", useExternal ? configPath : "classpath:application.yaml");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(activeResource);
        Properties properties = yaml.getObject();

        log.info("Config loaded successfully, properties count: {}",
                properties != null ? properties.size() : 0);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(activeResource);
        if (properties != null) {
            configurer.setProperties(properties);
        }
        return configurer;
    }
}
