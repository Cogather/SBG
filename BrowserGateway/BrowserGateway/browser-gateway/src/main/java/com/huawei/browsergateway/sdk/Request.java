package com.huawei.browsergateway.sdk;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.io.FileUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CDP服务请求数据模型
 */
public class Request {
    @Data
    public static class CreateBrowser {
        @Alias("executable_path")
        private String executablePath;
        @Alias("base_data")
        private String baseData;
        @Alias("extension_paths")
        private List<String> extensionPaths;
        @Alias("extension_ids")
        private List<String> extensionIds;
        @Alias("allowlisted_extension_id")
        private String allowlistedExtensionId;
        @Alias("browser_type")
        private Type.BrowserType browserType;
        private boolean headless;
        private String language;

        public static CreateBrowser from(BrowserOptions options, String id) {
            CreateBrowser ret = new CreateBrowser();
            ret.setBrowserType(options.getBrowserType());
            ret.setExecutablePath(options.getExecutablePath());
            ret.setExtensionIds(options.getExtensionIds());
            ret.setExtensionPaths(options.getExtensionPaths());
            ret.setAllowlistedExtensionId(options.getAllowlistedExtensionId());
            ret.setBaseData(FileUtil.file(options.getBaseDataDir(), id).getAbsolutePath());
            ret.setHeadless(options.isHeadless());
            ret.setLanguage(options.getLanguage());
            return ret;
        }
    }

    @Data
    public static class ViewPort {
        private Integer width;
        private Integer height;

        public ViewPort(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }
    }

    @Data
    public static class CreateContext {
        private String url;
        private ViewPort viewport;
        private String userdata;
        private String data;
        private String language;

        public static CreateContext from(BrowserOptions options) {
            CreateContext ret = new CreateContext();
            ret.setUrl(options.getUrl());
            ret.setUserdata(options.getUserdata());
            ret.setViewport(options.getViewpoint());
            ret.setData(options.getRecordData());
            ret.setLanguage(options.getLanguage());
            return ret;
        }
    }
    @Data
    public static class JSResult {
        @Alias("result_type")
        private String resultType;
        @Alias("value")
        private String value;
        @Alias("element_keys")
        private List<String> elementKeys = new ArrayList<>();
    }

    @Data
    public static class Element {
        private String id;
        private String preview;
    }

    @Data
    public static class Action {
        @Alias("element_id")
        private String elementId;
        private String action;
        private String value;

        public Action(String elementId, String action, String value) {
            this.elementId = elementId;
            this.action = action;
            this.value = value;
        }
    }
}