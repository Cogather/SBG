package com.huawei.browsergateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 应用配置类
 * 对应存量代码中的ApplicationConfig类
 * 负责根据操作系统动态加载配置文件
 */
@Configuration
public class ApplicationConfig {
    
    /**
     * 配置PropertySourcesPlaceholderConfigurer
     * 根据操作系统动态加载配置文件
     * 
     * @return PropertySourcesPlaceholderConfigurer实例
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String jarPath = new File(ApplicationConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getParent();
            
            String configPath;
            if (osName.contains("win")) {
                // Windows环境：从jar路径的上级目录的src/main/resources加载
                configPath = Paths.get(jarPath, "..", "src", "main", "resources")
                        .resolve("application.yaml").toString();
            } else {
                // Linux环境：从jar路径的conf目录加载
                configPath = Paths.get(jarPath, "conf").resolve("application.yaml").toString();
            }
            
            Resource fileResource = new FileSystemResource(configPath);
            
            // 如果配置文件不存在，使用默认配置（从classpath加载）
            if (!fileResource.exists()) {
                PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
                configurer.setIgnoreResourceNotFound(true);
                return configurer;
            }
            
            // 使用YamlPropertiesFactoryBean加载YAML配置并转换为Properties
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(fileResource);
            Properties properties = yaml.getObject();
            
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setLocation(fileResource);
            configurer.setProperties(properties);
            configurer.setIgnoreResourceNotFound(false);
            
            return configurer;
            
        } catch (Exception e) {
            // 如果加载失败，返回默认配置器（使用classpath配置）
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setIgnoreResourceNotFound(true);
            return configurer;
        }
    }
}
