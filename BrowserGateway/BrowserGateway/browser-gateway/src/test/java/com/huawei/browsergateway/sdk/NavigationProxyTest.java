package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NavigationProxyTest {

    @Mock
    private BrowserDriver browserDriver;

    private NavigationProxy navigation;

    @BeforeEach
    void setUp() {
        navigation = new NavigationProxy(browserDriver);
    }

    @Test
    void testBack_delegatesToBrowserDriver() {
        navigation.back();
        verify(browserDriver).back();
    }

    @Test
    void testForward_delegatesToBrowserDriver() {
        navigation.forward();
        verify(browserDriver).forward();
    }

    @Test
    void testTo_string_doesNotThrow() {
        assertDoesNotThrow(() -> navigation.to("https://example.com"));
    }

    @Test
    void testRefresh_doesNotThrow() {
        assertDoesNotThrow(() -> navigation.refresh());
    }
}
