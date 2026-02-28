package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Chrome浏览器配置类
 * 对应存量代码中的ChromeConfig类
 */
@Data
@Configuration
public class ChromeConfig {

    @Value("${browsergw.chrome.record-mode}")
    private Integer recordMode;

    @Value("${browsergw.chrome.headless:false}")
    private boolean headless = false;

    @Value("${browsergw.chrome.record-extension-id:}")
    private String recordExtensionId;

    @Value("${browsergw.chrome.endpoint}")
    private String endpoint;

    @Value("${browsergw.chrome.executable-path}")
    private String executablePath;

    @Value("${browsergw.chrome.ttl:360000000000}")
    private Long ttl;
}
