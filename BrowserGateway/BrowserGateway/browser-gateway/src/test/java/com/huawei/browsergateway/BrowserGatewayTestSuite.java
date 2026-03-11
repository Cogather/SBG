package com.huawei.browsergateway;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 浏览器网关测试套件
 * 运行所有模块的测试用例
 */
@Suite
@SuiteDisplayName("Browser Gateway Test Suite")
@SelectPackages({
    "com.huawei.browsergateway.service",
    "com.huawei.browsergateway.adapter",
    "com.huawei.browsergateway.sdk",
    "com.huawei.browsergateway.util"
})
public class BrowserGatewayTestSuite {
    // 测试套件入口
}
