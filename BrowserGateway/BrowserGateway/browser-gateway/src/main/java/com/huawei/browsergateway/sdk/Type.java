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

    public enum BrowserType {
        KEYS(1),
        TOUCH(2);

        private final int id;

        BrowserType(Integer id) {
            this.id = id;
        }

        public static BrowserType valueOf(int id) {
            for (BrowserType type : BrowserType.values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("BrowserType not found");
        }
    }

    @Data
    public static class Browser {
        private String id;
        @Alias("browser_type")
        private BrowserType browserType;
        private Integer used;

    }

    @Data
    public static class Context {
        private String id;
        private String current;
        @Alias("browser_id")
        private String browserId;
        private List<Page> pages;

        public Page getCurrentPage() {
            return pages.stream().filter(page -> Objects.equals(page.id,current)).findAny().orElse(null);
        }

        public String getCurrentUrl() {
            return Optional.ofNullable(getCurrentPage()).map(Page::getUrl).orElseThrow();
        }
    }

    @Data
    public static class Page {
        private String id;
        private String url;
        @Alias("browser_id")
        private String browserId;
        @Alias("context_id")
        private String contextId;
        @Alias("support_cdp_session")
        private Boolean supportCdpSession;
    }

    @Data
    public static class Size {
        private double width;
        private double height;
    }

    @Data
    public static class HealthCheckResult {
        private boolean success;
        @Alias("err_contexts")
        private List<String> errContexts;
    }
}
