package com.huawei.browsergateway.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 应用配置：从 jar 同目录或开发目录加载 application.yaml，注册为 Spring 占位符配置源。
 */
@Configuration
public class ApplicationConfig {

    private static final Logger log = LogManager.getLogger(ApplicationConfig.class);

    // ---- 配置路径 ----

    /** 开发环境相对 jar 的 yaml 路径（Windows） */
    private static final String DEV_CONFIG_RELATIVE = ".." + File.separator + "src" + File.separator
            + "main" + File.separator + "resources";

    /** 生产环境相对 jar 的配置目录名 */
    private static final String PROD_CONFIG_DIR = "conf";

    /** 配置文件名称 */
    private static final String APPLICATION_YAML = "application.yaml";

    /**
     * 根据运行环境解析 application.yaml 的绝对路径：Windows 使用开发路径，其他使用 conf 目录。
     */
    private static String resolveApplicationYamlPath(String jarParentPath) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        Path base = Paths.get(jarParentPath);
        if (osName.contains("win")) {
            return base.resolve(DEV_CONFIG_RELATIVE).resolve(APPLICATION_YAML).toString();
        }
        return base.resolve(PROD_CONFIG_DIR).resolve(APPLICATION_YAML).toString();
    }

    /**
     * 注册占位符配置器，从 jar 所在目录（或开发目录）加载 application.yaml。
     *
     * @return 配置器实例
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        String jarPath = new File(ApplicationConfig.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParent();
        String applicationYamlPath = resolveApplicationYamlPath(jarPath);

        log.info("application.yaml path: {}", applicationYamlPath);

        Resource fileResource = new FileSystemResource(applicationYamlPath);
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(fileResource);
        Properties properties = yaml.getObject();

        log.info("application.yaml content: {}", properties);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(fileResource);
        configurer.setProperties(properties);
        return configurer;
    }
}
