package com.huawei.browsergateway.adapter.interfaces;

/**
 * 框架适配器接口
 * 职责：管理CSE框架和OM SDK的初始化与生命周期
 */
public interface FrameworkAdapter {
    
    /**
     * 启动框架
     * @return 启动是否成功
     */
    boolean start();
    
    /**
     * 停止框架
     * @return 停止是否成功
     */
    boolean stop();
    
    /**
     * 初始化OM SDK
     * @return 初始化是否成功
     */
    boolean initializeOmSdK();
    
    /**
     * 检查框架是否已启动
     * @return 框架是否已启动
     */
    boolean isStarted();
}
