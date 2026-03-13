package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.By;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChromiumDriverProxyTest {

    @Mock
    private BrowserDriver browserDriver;

    private ChromiumDriverProxy proxy;

    private Type.Context ctx;

    @BeforeEach
    void setUp() throws Exception {
        ctx = new Type.Context();
        ctx.setId("ctx-1");
        ctx.setCurrent("page-1");
        Type.Page page = new Type.Page();
        page.setId("page-1");
        page.setUrl("https://example.com");
        ctx.setPages(List.of(page));

        sun.misc.Unsafe unsafe = getUnsafe();
        proxy = (ChromiumDriverProxy) unsafe.allocateInstance(ChromiumDriverProxy.class);

        setField(ChromiumDriverProxy.class, proxy, "driver", browserDriver);

        DevToolsProxy devTools = (DevToolsProxy) unsafe.allocateInstance(DevToolsProxy.class);
        var df = DevToolsProxy.class.getDeclaredField("driver");
        df.setAccessible(true);
        df.set(devTools, browserDriver);
        setField(ChromiumDriverProxy.class, proxy, "devTools", devTools);

        WindowProxy windowProxy = new WindowProxy(browserDriver);
        setField(ChromiumDriverProxy.class, proxy, "webDriver", windowProxy);

        when(browserDriver.getContext()).thenReturn(ctx);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    private static void setField(Class<?> clazz, Object target, String name, Object value) throws Exception {
        var f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // --- getWindowHandle ---

    @Test
    void testGetWindowHandle_returnsCurrentPage() {
        assertEquals("page-1", proxy.getWindowHandle());
    }

    // --- get ---

    @Test
    void testGet_delegatesGotoUrl() {
        proxy.get("https://target.com");
        verify(browserDriver).gotoUrl("https://target.com");
    }

    // --- quit ---

    @Test
    void testQuit_callsClose() {
        proxy.quit();
        verify(browserDriver).close();
    }

    // --- close (current page) ---

    @Test
    void testClose_callsCloseCurrentPage() {
        proxy.close();
        verify(browserDriver).closeCurrentPage();
    }

    // --- getCurrentUrl ---

    @Test
    void testGetCurrentUrl_delegatesToDriver() {
        when(browserDriver.getCurrentUrl()).thenReturn("https://example.com");
        assertEquals("https://example.com", proxy.getCurrentUrl());
    }

    // --- executeCdpCommand ---

    @Test
    void testExecuteCdpCommand_delegatesToDriver() {
        Map<String, Object> params = Map.of("k", "v");
        Map<String, Object> response = Map.of("result", "ok");
        when(browserDriver.executeCdp("Page.navigate", params)).thenReturn(response);

        assertEquals(response, proxy.executeCdpCommand("Page.navigate", params));
    }

    // --- executeScript special cases ---

    @Test
    void testExecuteScript_windowHistoryGo_resetsNavigation() {
        proxy.executeScript("window.history.go(-1)");
        verify(browserDriver).gotoUrl("about:blank");
        verify(browserDriver).executeCdp("Page.resetNavigationHistory", Map.of());
    }

    @Test
    void testExecuteScript_windowHistoryLength_returns2() {
        Object result = proxy.executeScript("return window.history.length");
        assertEquals(2L, result);
        verify(browserDriver, never()).executeScript(any());
    }

    @Test
    void testExecuteScript_regularScript_delegatesToDriver() {
        when(browserDriver.executeScript("return 1")).thenReturn(1L);
        Object result = proxy.executeScript("return 1");
        assertEquals(1L, result);
    }

    // --- findElement by tag name ---

    @Test
    void testFindElement_byTagName_delegatesToDriver() {
        when(browserDriver.findElementByTagName("body")).thenReturn(mock(org.openqa.selenium.WebElement.class));
        assertNotNull(proxy.findElement(By.tagName("body")));
    }

    @Test
    void testFindElement_unsupportedLocator_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> proxy.findElement(By.id("some-id")));
    }

    // --- navigate ---

    @Test
    void testNavigate_returnsNavigationProxy() {
        assertInstanceOf(NavigationProxy.class, proxy.navigate());
    }

    // --- manage ---

    @Test
    void testManage_returnsWindowProxy() {
        assertInstanceOf(WindowProxy.class, proxy.manage());
    }

    // --- saveUserdata ---

    @Test
    void testSaveUserdata_delegatesToDriver() {
        proxy.saveUserdata();
        verify(browserDriver).saveUserdata();
    }

    // --- getProxyContextId ---

    @Test
    void testGetProxyContextId_returnsContextId() {
        assertEquals("ctx-1", proxy.getProxyContextId());
    }
}
