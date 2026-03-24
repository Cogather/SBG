package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 测试配置类
 * 负责管理测试相关的配置信息，仅用于本地开发和测试环境
 */
@Data
@Configuration
public class TestConfig {

    /** 是否跳过用户绑定验证，从配置文件 browsergw.test.skip-user-bind 注入，默认为 false */
    @Value("${browsergw.test.skip-user-bind:false}")
    private boolean skipUserBind;

    /** 是否跳过浏览器创建，从配置文件 browsergw.test.skip-browser-creation 注入，默认为 false */
    @Value("${browsergw.test.skip-browser-creation:false}")
    private boolean skipBrowserCreation;
}
