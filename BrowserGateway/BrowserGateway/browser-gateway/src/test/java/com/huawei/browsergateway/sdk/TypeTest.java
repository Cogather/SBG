package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeTest {

    @Test
    void testBrowserType_valueOf_keys() {
        assertEquals(Type.BrowserType.KEYS, Type.BrowserType.valueOf(1));
    }

    @Test
    void testBrowserType_valueOf_touch() {
        assertEquals(Type.BrowserType.TOUCH, Type.BrowserType.valueOf(2));
    }

    @Test
    void testBrowserType_valueOf_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Type.BrowserType.valueOf(99));
    }

    @Test
    void testContext_getCurrentPage_found() {
        Type.Context context = new Type.Context();
        context.setCurrent("page1");

        Type.Page page1 = new Type.Page();
        page1.setId("page1");
        page1.setUrl("https://example.com");

        Type.Page page2 = new Type.Page();
        page2.setId("page2");
        page2.setUrl("https://other.com");

        context.setPages(Arrays.asList(page1, page2));

        assertEquals(page1, context.getCurrentPage());
    }

    @Test
    void testContext_getCurrentPage_notFound_returnsNull() {
        Type.Context context = new Type.Context();
        context.setCurrent("nonexistent");
        context.setPages(List.of());

        assertNull(context.getCurrentPage());
    }

    @Test
    void testContext_getCurrentUrl_success() {
        Type.Context context = new Type.Context();
        context.setCurrent("page1");

        Type.Page page = new Type.Page();
        page.setId("page1");
        page.setUrl("https://example.com");

        context.setPages(List.of(page));

        assertEquals("https://example.com", context.getCurrentUrl());
    }

    @Test
    void testContext_getCurrentUrl_noMatchingPage_throws() {
        Type.Context context = new Type.Context();
        context.setCurrent("nonexistent");
        context.setPages(List.of());

        assertThrows(Exception.class, context::getCurrentUrl);
    }

    @Test
    void testSize_gettersAndSetters() {
        Type.Size size = new Type.Size();
        size.setWidth(100.5);
        size.setHeight(200.0);

        assertEquals(100.5, size.getWidth());
        assertEquals(200.0, size.getHeight());
    }

    @Test
    void testHealthCheckResult_success() {
        Type.HealthCheckResult result = new Type.HealthCheckResult();
        result.setSuccess(true);
        result.setErrContexts(List.of());

        assertTrue(result.isSuccess());
        assertTrue(result.getErrContexts().isEmpty());
    }

    @Test
    void testHealthCheckResult_failure_withErrContexts() {
        Type.HealthCheckResult result = new Type.HealthCheckResult();
        result.setSuccess(false);
        result.setErrContexts(Arrays.asList("ctx1", "ctx2"));

        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrContexts().size());
    }
}
