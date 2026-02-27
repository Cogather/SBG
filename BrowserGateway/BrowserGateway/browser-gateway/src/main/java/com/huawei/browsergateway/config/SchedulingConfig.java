package com.huawei.browsergateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 * 对应存量代码中的SchedulingConfig类
 * 启用Spring的定时任务功能
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 定时任务已通过@EnableScheduling启用
}
