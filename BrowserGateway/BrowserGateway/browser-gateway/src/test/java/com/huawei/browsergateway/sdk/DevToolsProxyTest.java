package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.v132.target.model.TargetID;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevToolsProxyTest {

    @Mock
    private BrowserDriver browserDriver;

    private DevToolsProxy devToolsProxy;

    @BeforeEach
    void setUp() throws Exception {
        devToolsProxy = (DevToolsProxy) getUnsafe().allocateInstance(DevToolsProxy.class);
        var f = DevToolsProxy.class.getDeclaredField("driver");
        f.setAccessible(true);
        f.set(devToolsProxy, browserDriver);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    @Test
    void testSend_createTarget_returnsTargetId() {
        when(browserDriver.newPage("https://example.com")).thenReturn("page-new");

        Command<TargetID> cmd = mock(Command.class);
        when(cmd.getMethod()).thenReturn("Target.createTarget");
        when(cmd.getParams()).thenReturn(Map.of("url", "https://example.com"));

        TargetID result = devToolsProxy.send(cmd);
        assertNotNull(result);
        assertEquals("page-new", result.toString());
    }

    @Test
    void testSend_otherCommand_callsExecuteCdp() {
        Map<String, Object> params = Map.of("key", "val");
        when(browserDriver.executeCdp("Page.navigate", params)).thenReturn(Map.of());

        Command<Object> cmd = mock(Command.class);
        when(cmd.getMethod()).thenReturn("Page.navigate");
        when(cmd.getParams()).thenReturn(params);

        Object result = devToolsProxy.send(cmd);
        assertNotNull(result);
        verify(browserDriver).executeCdp("Page.navigate", params);
    }

    @Test
    void testCreateSession_doesNotThrow() {
        assertDoesNotThrow(() -> devToolsProxy.createSession("window-1"));
    }
}
