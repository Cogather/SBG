package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BrowserDriver tests — uses reflection to inject mocks, bypassing the HTTP-calling constructor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrowserDriverTest {

    @Mock
    private DriverClient mockClient;
    @Mock
    private DriverClient.Context mockContextCli;
    @Mock
    private DriverClient.Page mockPage;

    private BrowserDriver driver;
    private Type.Context ctx;

    @BeforeEach
    void setUp() throws Exception {
        ctx = new Type.Context();
        ctx.setId("ctx-1");
        ctx.setBrowserId("browser-1");
        ctx.setCurrent("page-1");
        Type.Page page = new Type.Page();
        page.setId("page-1");
        page.setUrl("https://example.com");
        ctx.setPages(List.of(page));

        // Bypass constructor via reflection
        driver = (BrowserDriver) sun.misc.Unsafe.class
                .getDeclaredMethod("allocateInstance", Class.class)
                .invoke(getUnsafe(), BrowserDriver.class);

        setField(driver, "client", mockClient);
        setField(driver, "contextCli", mockContextCli);
        setField(driver, "context", ctx);

        when(mockContextCli.page(any())).thenReturn(mockPage);
        when(mockClient.context(any())).thenReturn(mockContextCli);
    }

    private static Object getUnsafe() throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return f.get(null);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = BrowserDriver.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // --- close ---

    @Test
    void testClose_deletesContext() {
        driver.close();
        verify(mockClient).context("browser-1");
        verify(mockContextCli).delete("ctx-1");
    }

    // --- saveUserdata ---

    @Test
    void testSaveUserdata_callsSaveUserdata() {
        driver.saveUserdata();
        verify(mockContextCli).saveUserdata("ctx-1");
    }

    // --- newPage ---

    @Test
    void testNewPage_returnsCurrentPage() {
        Type.Context updated = new Type.Context();
        updated.setId("ctx-1");
        updated.setCurrent("page-2");
        updated.setPages(List.of());
        when(mockPage.create("https://new.com")).thenReturn(updated);

        String result = driver.newPage("https://new.com");
        assertEquals("page-2", result);
    }

    // --- gotoUrl ---

    @Test
    void testGotoUrl_updatesContext() {
        Type.Context updated = new Type.Context();
        updated.setId("ctx-1");
        updated.setCurrent("page-1");
        updated.setPages(List.of());
        when(mockPage.gotoUrl("https://target.com")).thenReturn(updated);

        driver.gotoUrl("https://target.com");
        verify(mockPage).gotoUrl("https://target.com");
    }

    // --- executeScript result types ---

    @Test
    void testExecuteScript_stringResult() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("string");
        jsResult.setValue("hello");
        when(mockPage.execute("return 'hello'")).thenReturn(jsResult);

        Object result = driver.executeScript("return 'hello'");
        assertEquals("hello", result);
    }

    @Test
    void testExecuteScript_intResult() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("int");
        jsResult.setValue("42");
        when(mockPage.execute("return 42")).thenReturn(jsResult);

        Object result = driver.executeScript("return 42");
        assertEquals(42L, result);
    }

    @Test
    void testExecuteScript_noneResult() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("none");
        when(mockPage.execute("void 0")).thenReturn(jsResult);

        assertNull(driver.executeScript("void 0"));
    }

    @Test
    void testExecuteScript_dictResult_noElementKeys() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("dict");
        jsResult.setValue("{\"key\":\"val\"}");
        when(mockPage.execute("return {}")).thenReturn(jsResult);

        Object result = driver.executeScript("return {}");
        assertInstanceOf(Map.class, result);
    }

    @Test
    void testExecuteScript_unknownResultType_throws() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("unknown");
        when(mockPage.execute("x")).thenReturn(jsResult);

        assertThrows(IllegalArgumentException.class, () -> driver.executeScript("x"));
    }

    // --- executeElement ---

    @Test
    void testExecuteElement_delegatesToPage() {
        Request.Action action = new Request.Action("e1", "click", null);
        driver.executeElement(action);
        verify(mockPage).executeElement(action);
    }

    // --- executeCdp ---

    @Test
    void testExecuteCdp_delegatesToPage() {
        Map<String, Object> params = Map.of("key", "val");
        Map<String, Object> response = Map.of("result", "ok");
        when(mockPage.executeCdp("Page.navigate", params)).thenReturn(response);

        Map<String, Object> result = driver.executeCdp("Page.navigate", params);
        assertEquals(response, result);
    }

    // --- closeCurrentPage ---

    @Test
    void testCloseCurrentPage_deletesCurrentPage() {
        Type.Context updated = new Type.Context();
        updated.setId("ctx-1");
        updated.setCurrent("page-1");
        updated.setPages(List.of());
        when(mockPage.delete("page-1")).thenReturn(updated);

        driver.closeCurrentPage();
        verify(mockPage).delete("page-1");
    }

    // --- getCurrentUrl ---

    @Test
    void testGetCurrentUrl_returnsUrl() {
        Type.Context refreshed = new Type.Context();
        refreshed.setId("ctx-1");
        refreshed.setCurrent("page-1");
        Type.Page page = new Type.Page();
        page.setId("page-1");
        page.setUrl("https://current.com");
        refreshed.setPages(List.of(page));
        when(mockContextCli.get("ctx-1")).thenReturn(refreshed);

        assertEquals("https://current.com", driver.getCurrentUrl());
    }

    // --- back / forward ---

    @Test
    void testBack_callsGoBack() {
        driver.back();
        verify(mockPage).goBack();
    }

    @Test
    void testForward_callsGoForward() {
        driver.forward();
        verify(mockPage).goForward();
    }

    // --- findElementByTagName ---

    @Test
    void testFindElementByTagName_returnsWebElement() {
        Request.JSResult jsResult = new Request.JSResult();
        jsResult.setResultType("element");
        jsResult.setValue("{\"id\":\"e1\",\"preview\":\"<div>\"}");
        when(mockPage.findElement("div")).thenReturn(jsResult);

        assertNotNull(driver.findElementByTagName("div"));
    }

    // --- getSize ---

    @Test
    void testGetSize_returnsSize() {
        Type.Size size = new Type.Size();
        size.setWidth(50.0);
        size.setHeight(80.0);
        when(mockPage.getElementSize("e1")).thenReturn(size);

        Type.Size result = driver.getSize("e1");
        assertEquals(50.0, result.getWidth());
        assertEquals(80.0, result.getHeight());
    }
}
