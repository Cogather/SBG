package com.huawei.browsergateway.sdk.cdp;

import java.util.List;
import java.util.Map;

/**
 * 驱动客户端接口定义
 * 提供浏览器、上下文、页面的管理接口
 */
public interface DriverClient {
    
    /**
     * 浏览器管理接口
     */
    interface Browser {
        /**
         * 创建浏览器实例
         */
        Type.Browser create(Request.CreateBrowser req);
        
        /**
         * 获取浏览器实例
         */
        Type.Browser get(String id);
        
        /**
         * 列出所有浏览器实例
         */
        List<Type.Browser> list();
        
        /**
         * 删除浏览器实例
         */
        void delete(String id);
        
        /**
         * 健康检查
         */
        Type.HealthCheckResult healthCheck();
    }
    
    /**
     * 上下文管理接口
     */
    interface Context {
        /**
         * 创建上下文
         */
        Type.Context create(Request.CreateContext req);
        
        /**
         * 获取上下文
         */
        Type.Context get(String id);
        
        /**
         * 删除上下文
         */
        void delete(String id);
        
        /**
         * 保存用户数据
         */
        void saveUserdata(String contextId);
        
        /**
         * 获取页面管理接口
         */
        Page page(String contextId);
    }
    
    /**
     * 页面管理接口
     */
    interface Page {
        /**
         * 创建页面
         */
        Type.Page create(String url);
        
        /**
         * 执行JavaScript
         */
        Request.JSResult execute(String expression);
        
        /**
         * 执行CDP命令
         */
        Map<String, Object> executeCdp(String method, Map<String, Object> params);
        
        /**
         * 导航到URL
         */
        Type.Page gotoUrl(String url);
        
        /**
         * 查找元素
         */
        String findElement(String selector);
    }
    
    /**
     * 获取浏览器管理接口
     */
    Browser browser();
    
    /**
     * 获取上下文管理接口
     */
    Context context(String browserId);
}
