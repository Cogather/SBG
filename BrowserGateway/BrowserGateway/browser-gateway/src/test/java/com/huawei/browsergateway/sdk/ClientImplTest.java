package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientImplTest {

    @Test
    void testBuildUrl_concatenatesEndpointAndPath() {
        ClientImpl client = new ClientImpl("http://localhost:9222");
        assertEquals("http://localhost:9222/api/browsers", client.buildUrl("/api/browsers"));
    }

    @Test
    void testBuildUrl_emptyPath() {
        ClientImpl client = new ClientImpl("http://localhost:9222");
        assertEquals("http://localhost:9222", client.buildUrl(""));
    }

    @Test
    void testBuildUrl_withTrailingSlash() {
        ClientImpl client = new ClientImpl("http://localhost:9222/");
        assertEquals("http://localhost:9222//api/browsers", client.buildUrl("/api/browsers"));
    }

    @Test
    void testBrowser_returnsNonNull() {
        ClientImpl client = new ClientImpl("http://localhost:9222");
        assertNotNull(client.browser());
    }

    @Test
    void testContext_returnsNonNull() {
        ClientImpl client = new ClientImpl("http://localhost:9222");
        assertNotNull(client.context("browser-1"));
    }

    @Test
    void testBrowser_returnsSameInstance() {
        ClientImpl client = new ClientImpl("http://localhost:9222");
        assertSame(client.browser(), client.browser());
    }
}
