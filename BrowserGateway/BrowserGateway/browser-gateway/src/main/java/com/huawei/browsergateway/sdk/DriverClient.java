package com.huawei.browsergateway.sdk;

import cn.hutool.core.lang.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * 驱动客户端接口定义
 * 提供浏览器、上下文、页面的管理接口
 * 用于与CDP服务进行通信
 */
public interface DriverClient {

    /**
     * 浏览器管理接口
     * 提供浏览器的创建、查询、删除等操作
     */
    interface Browser {
        /**
         * 创建浏览器实例
         *
         * @param request 创建浏览器请求对象
         * @return 浏览器对象
         */
        Type.Browser create(Request.CreateBrowser request);

        /**
         * 根据ID获取浏览器实例
         *
         * @param id 浏览器ID
         * @return 浏览器对象
         */
        Type.Browser get(String id);

        /**
         * 获取所有浏览器实例列表
         *
         * @return 浏览器列表
         */
        List<Type.Browser> list();

        /**
         * 删除浏览器实例
         *
         * @param id 浏览器ID
         */
        void delete(String id);

        /**
         * 健康检查
         *
         * @return 健康检查结果
         */
        Type.HealthCheckResult healthCheck();
    }

    /**
     * 上下文管理接口
     * 提供浏览器上下文的创建、查询、删除等操作
     */
    interface Context {
        /**
         * 创建浏览器上下文
         *
         * @param request 创建上下文请求对象
         * @return 上下文对象
         */
        Type.Context create(Request.CreateContext request);

        /**
         * 根据ID获取上下文实例
         *
         * @param id 上下文ID
         * @return 上下文对象
         */
        Type.Context get(String id);

        /**
         * 获取所有上下文实例列表
         *
         * @return 上下文列表
         */
        List<Type.Context> list();

        /**
         * 删除上下文实例
         *
         * @param id 上下文ID
         */
        void delete(String id);

        /**
         * 保存用户数据
         *
         * @param contextId 上下文ID
         */
        void saveUserdata(String contextId);

        /**
         * 获取页面管理接口
         *
         * @param contextId 上下文ID
         * @return 页面管理接口
         */
        Page page(String contextId);
    }

    /**
     * 页面管理接口
     * 提供页面的创建、删除、导航、脚本执行等操作
     */
    interface Page {
        /**
         * 删除页面
         *
         * @param id 页面ID
         * @return 更新后的上下文对象
         */
        Type.Context delete(String id);

        /**
         * 创建新页面
         *
         * @param url 页面URL
         * @return 更新后的上下文对象
         */
        Type.Context create(String url);

        /**
         * 执行JavaScript表达式
         *
         * @param expression JavaScript表达式
         * @return JavaScript执行结果
         */
        Request.JSResult execute(String expression);

        /**
         * 执行CDP命令
         *
         * @param method CDP方法名
         * @param params CDP参数
         * @return CDP执行结果
         */
        Map<String, Object> executeCdp(String method, Map<String, Object> params);

        /**
         * 导航到指定URL
         *
         * @param url 目标URL
         * @return 更新后的上下文对象
         */
        Type.Context gotoUrl(String url);

        /**
         * 执行元素操作
         *
         * @param action 元素操作请求对象
         */
        void executeElement(Request.Action action);

        /**
         * 浏览器后退
         */
        void goBack();

        /**
         * 浏览器前进
         */
        void goForward();

        /**
         * 查找元素
         *
         * @param selector 元素选择器
         * @return JavaScript执行结果
         */
        Request.JSResult findElement(String selector);

        /**
         * 获取元素尺寸
         *
         * @param elementId 元素ID
         * @return 元素尺寸对象
         */
        Type.Size getElementSize(String elementId);
    }

    /**
     * 获取浏览器管理接口
     *
     * @return 浏览器管理接口
     */
    Browser browser();

    /**
     * 获取上下文管理接口
     *
     * @param browserId 浏览器ID
     * @return 上下文管理接口
     */
    Context context(String browserId);

    /**
     * 发送HTTP请求（带返回类型）
     *
     * @param url           请求URL
     * @param method        请求方法
     * @param body          请求体
     * @param typeReference 返回类型引用
     * @param <T>           返回类型
     * @return 响应对象
     */
    <T> T request(String url, String method, String body, TypeReference<T> typeReference);

    /**
     * 发送HTTP请求（无返回）
     *
     * @param url    请求URL
     * @param method 请求方法
     */
    void request(String url, String method);
}
