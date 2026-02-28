package com.huawei.browsergateway.sdk;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

@Data
public class BrowserOptions {
    private String endpoint;
    private Type.BrowserType browserType;
    private String baseDataDir;
    private String executablePath;
    private List<String> extensionPaths;
    private List<String> extensionIds;
    private String allowlistedExtensionId;
    private boolean headless;

    private String url;
    private Request.ViewPort viewpoint;
    private String userdata;
    @Alias("data")
    private String recordData;
    private String language;
    private int limit;
}