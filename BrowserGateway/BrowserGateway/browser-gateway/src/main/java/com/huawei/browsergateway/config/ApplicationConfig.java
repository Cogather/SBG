package com.huawei.browsergateway.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
public class ApplicationConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        String osName = System.getProperty("os.name").toLowerCase();

        String jarPath = new File(ApplicationConfig.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParent();

        String ap;
        if (osName.contains("win")) {
            ap = Paths.get(jarPath, "..", "src", "main", "resources").resolve("application.yaml").toString();
        } else {
            ap = Paths.get(jarPath, "conf").resolve("application.yaml").toString();
        }

        System.out.println("------------------ application.yaml path is "+ ap);
        Resource fileResource = new FileSystemResource(ap);

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(ap));
        Properties properties = yaml.getObject();

        System.out.println("------------------ application.yaml content is "+ properties.toString());

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(fileResource);
        configurer.setProperties(properties);
        return configurer;
    }
}
