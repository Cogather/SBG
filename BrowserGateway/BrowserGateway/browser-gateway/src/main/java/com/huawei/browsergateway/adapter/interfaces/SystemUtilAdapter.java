package com.huawei.browsergateway.adapter.interfaces;

/**
 * 系统工具适配器接口
 * 职责：环境变量读取、本地配置管理
 */
public interface SystemUtilAdapter {
    
    /**
     * 从环境变量获取字符串值
     * @param key 环境变量键
     * @param defaultValue 默认值
     * @return 环境变量值
     */
    String getEnvString(String key, String defaultValue);
    
    /**
     * 从环境变量获取整数值
     * @param key 环境变量键
     * @param defaultValue 默认值
     * @return 环境变量值
     */
    int getEnvInteger(String key, int defaultValue);
    
    /**
     * 设置环境变量（用于测试）
     * @param key 键
     * @param value 值
     */
    void setEnv(String key, String value);
}
