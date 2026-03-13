package com.huawei.browsergateway.sdk;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.io.FileUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CDP服务请求数据模型
 * 包含浏览器、上下文、页面操作相关的请求对象定义
 */
public class Request {

    /**
     * 创建浏览器请求对象
     * 用于向CDP服务发送创建浏览器的请求
     */
    @Data
    public static class CreateBrowser {
        /** 浏览器可执行文件路径 */
        @Alias("executable_path")
        private String executablePath;

        /** 浏览器基础数据目录 */
        @Alias("base_data")
        private String baseData;

        /** 扩展插件路径列表 */
        @Alias("extension_paths")
        private List<String> extensionPaths;

        /** 扩展插件ID列表 */
        @Alias("extension_ids")
        private List<String> extensionIds;

        /** 允许列表中的扩展ID */
        @Alias("allowlisted_extension_id")
        private String allowlistedExtensionId;

        /** 浏览器类型 */
        @Alias("browser_type")
        private Type.BrowserType browserType;

        /** 是否无头模式 */
        private boolean headless;

        /** 语言设置 */
        private String language;

        /**
         * 从浏览器选项创建请求对象
         *
         * @param options 浏览器配置选项
         * @param id      浏览器实例ID
         * @return 创建浏览器请求对象
         */
        public static CreateBrowser from(BrowserOptions options, String id) {
            CreateBrowser req = new CreateBrowser();
            req.setBrowserType(options.getBrowserType());
            req.setExecutablePath(options.getExecutablePath());
            req.setExtensionIds(options.getExtensionIds());
            req.setExtensionPaths(options.getExtensionPaths());
            req.setAllowlistedExtensionId(options.getAllowlistedExtensionId());
            req.setBaseData(FileUtil.file(options.getBaseDataDir(), id).getAbsolutePath());
            req.setHeadless(options.isHeadless());
            req.setLanguage(options.getLanguage());
            return req;
        }
    }

    /**
     * 视口配置对象
     * 定义浏览器窗口的宽度和高度
     */
    @Data
    public static class ViewPort {
        /** 视口宽度 */
        private Integer width;

        /** 视口高度 */
        private Integer height;

        /**
         * 构造函数
         *
         * @param width  视口宽度
         * @param height 视口高度
         */
        public ViewPort(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * 创建上下文请求对象
     * 用于创建浏览器上下文（会话）
     */
    @Data
    public static class CreateContext {
        /** 初始URL */
        private String url;

        /** 视口配置 */
        private ViewPort viewport;

        /** 用户数据目录 */
        private String userdata;

        /** 录制数据 */
        private String data;

        /** 语言设置 */
        private String language;

        /**
         * 从浏览器选项创建上下文请求对象
         *
         * @param options 浏览器配置选项
         * @return 创建上下文请求对象
         */
        public static CreateContext from(BrowserOptions options) {
            CreateContext req = new CreateContext();
            req.setUrl(options.getUrl());
            req.setUserdata(options.getUserdata());
            req.setViewport(options.getViewpoint());
            req.setData(options.getRecordData());
            req.setLanguage(options.getLanguage());
            return req;
        }
    }

    /**
     * JavaScript执行结果对象
     * 包含执行结果的类型、值和元素键列表
     */
    @Data
    public static class JSResult {
        /** 结果类型（element/string/int/none/dict） */
        @Alias("result_type")
        private String resultType;

        /** 结果值 */
        @Alias("value")
        private String value;

        /** 元素键列表 */
        @Alias("element_keys")
        private List<String> elementKeys = new ArrayList<>();
    }

    /**
     * 页面元素对象
     * 包含元素ID和预览信息
     */
    @Data
    public static class Element {
        /** 元素ID */
        private String id;

        /** 元素预览信息 */
        private String preview;
    }

    /**
     * 元素操作请求对象
     * 用于执行页面元素的各种操作
     */
    @Data
    public static class Action {
        /** 元素ID */
        @Alias("element_id")
        private String elementId;

        /** 操作类型 */
        private String action;

        /** 操作值 */
        private String value;

        /**
         * 构造函数
         *
         * @param elementId 元素ID
         * @param action   操作类型
         * @param value    操作值
         */
        public Action(String elementId, String action, String value) {
            this.elementId = elementId;
            this.action = action;
            this.value = value;
        }
    }
}
