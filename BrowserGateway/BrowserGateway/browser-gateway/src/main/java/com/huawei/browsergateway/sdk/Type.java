package com.huawei.browsergateway.sdk;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CDP服务数据类型定义
 * 包含Browser、Context、Page等核心数据模型
 */
public class Type {

    /**
     * 浏览器类型枚举
     */
    public enum BrowserType {
        /** 键盘输入类型 */
        KEYS(1),
        /** 触摸输入类型 */
        TOUCH(2);

        /** 类型ID */
        private final int id;

        /**
         * 构造函数
         *
         * @param id 类型ID
         */
        BrowserType(Integer id) {
            this.id = id;
        }

        /**
         * 根据ID获取浏览器类型
         *
         * @param id 类型ID
         * @return 浏览器类型
         * @throws IllegalArgumentException 如果ID不存在
         */
        public static BrowserType valueOf(int id) {
            for (BrowserType type : BrowserType.values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("BrowserType not found: " + id);
        }
    }

    /**
     * 浏览器对象
     */
    @Data
    public static class Browser {
        /** 浏览器ID */
        private String id;

        /** 浏览器类型 */
        @Alias("browser_type")
        private BrowserType browserType;

        /** 已使用数量 */
        private Integer used;
    }

    /**
     * 上下文对象
     * 表示浏览器的一个会话上下文
     */
    @Data
    public static class Context {
        /** 上下文ID */
        private String id;

        /** 当前页面ID */
        private String current;

        /** 所属浏览器ID */
        @Alias("browser_id")
        private String browserId;

        /** 页面列表 */
        private List<Page> pages;

        /**
         * 获取当前页面对象
         *
         * @return 当前页面对象，如果不存在则返回null
         */
        public Page getCurrentPage() {
            return pages.stream()
                    .filter(page -> Objects.equals(page.id, current))
                    .findAny()
                    .orElse(null);
        }

        /**
         * 获取当前页面URL
         *
         * @return 当前页面URL
         * @throws RuntimeException 如果当前页面不存在
         */
        public String getCurrentUrl() {
            return Optional.ofNullable(getCurrentPage())
                    .map(Page::getUrl)
                    .orElseThrow(() -> new RuntimeException("Current page not found"));
        }
    }

    /**
     * 页面对象
     * 表示浏览器中的一个标签页
     */
    @Data
    public static class Page {
        /** 页面ID */
        private String id;

        /** 页面URL */
        private String url;

        /** 所属浏览器ID */
        @Alias("browser_id")
        private String browserId;

        /** 所属上下文ID */
        @Alias("context_id")
        private String contextId;

        /** 是否支持CDP会话 */
        @Alias("support_cdp_session")
        private Boolean supportCdpSession;
    }

    /**
     * 尺寸对象
     * 表示元素的宽度和高度
     */
    @Data
    public static class Size {
        /** 宽度 */
        private double width;

        /** 高度 */
        private double height;
    }

    /**
     * 健康检查结果对象
     */
    @Data
    public static class HealthCheckResult {
        /** 是否成功 */
        private boolean success;

        /** 错误上下文列表 */
        @Alias("err_contexts")
        private List<String> errContexts;
    }
}
