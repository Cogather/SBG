package com.huawei.browsergateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
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
        String jarPath = new File(ApplicationConfig.class.getProtectionDomain().
                getCodeSource().getLocation().getPath()).getParent();
        String ap = Paths.get(jarPath, "conf").resolve("application.yaml").toString();
        Resource fileResource = new FileSystemResource(ap);
        Resource classpathResource = new ClassPathResource("application.yaml");
        Resource[] resources = fileResource.exists() ? new Resource[] { fileResource } : new Resource[] { classpathResource };

        System.out.println("------------------ application.yaml path is " + (fileResource.exists() ? ap : "classpath:application.yaml"));

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resources);
        Properties properties = yaml.getObject();

        System.out.println("------------------ application.yaml content is " + properties);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(fileResource.exists() ? fileResource : classpathResource);
        configurer.setProperties(properties);
        return configurer;
    }
}
