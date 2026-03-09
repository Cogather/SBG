package com.huawei.browsergateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 * 通过@EnableScheduling注解启用Spring框架的定时任务调度功能
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 定时任务已通过@EnableScheduling启用
}
