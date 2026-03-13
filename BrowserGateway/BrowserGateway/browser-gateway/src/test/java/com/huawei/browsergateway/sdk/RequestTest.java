package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testCreateBrowser_from_mapsAllFields() {
        BrowserOptions options = new BrowserOptions();
        options.setBrowserType(Type.BrowserType.KEYS);
        options.setExecutablePath("/usr/bin/chrome");
        options.setExtensionIds(Arrays.asList("ext1", "ext2"));
        options.setExtensionPaths(List.of("/path/ext1"));
        options.setAllowlistedExtensionId("allowed-ext");
        options.setBaseDataDir("/tmp/data");
        options.setHeadless(true);
        options.setLanguage("zh-CN");

        Request.CreateBrowser req = Request.CreateBrowser.from(options, "test-id");

        assertEquals(Type.BrowserType.KEYS, req.getBrowserType());
        assertEquals("/usr/bin/chrome", req.getExecutablePath());
        assertEquals(Arrays.asList("ext1", "ext2"), req.getExtensionIds());
        assertEquals(List.of("/path/ext1"), req.getExtensionPaths());
        assertEquals("allowed-ext", req.getAllowlistedExtensionId());
        assertTrue(req.isHeadless());
        assertEquals("zh-CN", req.getLanguage());
        assertTrue(req.getBaseData().contains("test-id"));
    }

    @Test
    void testCreateBrowser_from_baseDataContainsBrowserId() {
        BrowserOptions options = new BrowserOptions();
        options.setBaseDataDir("/tmp/base");

        Request.CreateBrowser req = Request.CreateBrowser.from(options, "browser-uuid-123");

        assertTrue(req.getBaseData().contains("browser-uuid-123"));
    }

    @Test
    void testCreateContext_from_mapsAllFields() {
        BrowserOptions options = new BrowserOptions();
        options.setUrl("https://example.com");
        options.setUserdata("/tmp/userdata");
        options.setViewpoint(new Request.ViewPort(1920, 1080));
        options.setRecordData("record-data");
        options.setLanguage("en-US");

        Request.CreateContext req = Request.CreateContext.from(options);

        assertEquals("https://example.com", req.getUrl());
        assertEquals("/tmp/userdata", req.getUserdata());
        assertEquals(1920, req.getViewport().getWidth());
        assertEquals(1080, req.getViewport().getHeight());
        assertEquals("record-data", req.getData());
        assertEquals("en-US", req.getLanguage());
    }

    @Test
    void testViewPort_constructor() {
        Request.ViewPort vp = new Request.ViewPort(800, 600);
        assertEquals(800, vp.getWidth());
        assertEquals(600, vp.getHeight());
    }

    @Test
    void testAction_constructor() {
        Request.Action action = new Request.Action("elem-1", "click", "value");
        assertEquals("elem-1", action.getElementId());
        assertEquals("click", action.getAction());
        assertEquals("value", action.getValue());
    }

    @Test
    void testJSResult_defaultElementKeys_isEmpty() {
        Request.JSResult result = new Request.JSResult();
        assertNotNull(result.getElementKeys());
        assertTrue(result.getElementKeys().isEmpty());
    }

    @Test
    void testJSResult_settersAndGetters() {
        Request.JSResult result = new Request.JSResult();
        result.setResultType("string");
        result.setValue("hello");
        result.setElementKeys(List.of("key1"));

        assertEquals("string", result.getResultType());
        assertEquals("hello", result.getValue());
        assertEquals(List.of("key1"), result.getElementKeys());
    }
}
