package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.config.ChromeConfig;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.browser.ChromeRecordConfig;
import com.huawei.browsergateway.entity.enums.BrowserStatus;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.sdk.ChromiumDriverProxy;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserChromeTest {

    @Mock
    private IFileStorage fs;

    @Mock
    private Config config;

    @Mock
    private ChromeConfig chromeConfig;

    @Mock
    private ControlClientSet controlClientSet;

    @Mock
    private MediaClientSet mediaClientSet;

    @Mock
    private IRemote remote;

    @Mock
    private MuenDriver muenDriver;

    @BeforeEach
    void setUp() {
        when(config.getChrome()).thenReturn(chromeConfig);
        when(config.getUserDataPath()).thenReturn("target/test-userdata");
        when(config.getSelfAddr()).thenReturn("127.0.0.1:8080");
        when(config.getBaseDataPath()).thenReturn("target/test-basedata");
        when(config.getRecordExtensionPath()).thenReturn("target/test-extension/record");
        when(config.getRecordExtensionPage()).thenReturn("chrome-extension://record-id/offscreen.html");
        when(chromeConfig.getEndpoint()).thenReturn("ws://127.0.0.1:9222");
        when(chromeConfig.getExecutablePath()).thenReturn("chrome.exe");
        when(chromeConfig.getRecordExtensionId()).thenReturn("record-id");
        when(chromeConfig.isHeadless()).thenReturn(false);
        when(fs.exist("userdata\\imei_imsi\\userdata.json.zst")).thenReturn(false);
    }

    @Test
    void constructorShouldInitializeCoreFields() {
        UserChrome userChrome = new UserChrome(buildRequest(), fs, config, muenDriver, controlClientSet, mediaClientSet, remote);

        assertEquals("imei_imsi", userChrome.getUserId());
        assertEquals(BrowserStatus.NORMAL, userChrome.getStatus());
        assertEquals("chrome-extension://record-id/offscreen.html", userChrome.getOptions().getUrl());
        assertEquals("en", userChrome.getOptions().getLanguage());
        assertNotNull(userChrome.getUserData());
        assertTrue(userChrome.getHeartbeats() > 0);
    }

    @Test
    void closeAppShouldDisconnectSaveAndUpload() {
        UserChrome userChrome = new UserChrome(buildRequest(), fs, config, muenDriver, controlClientSet, mediaClientSet, remote);
        ChromiumDriverProxy chromeDriver = mock(ChromiumDriverProxy.class);
        UserData userData = mock(UserData.class);
        ReflectionTestUtils.setField(userChrome, "chromeDriver", chromeDriver);
        ReflectionTestUtils.setField(userChrome, "userData", userData);

        userChrome.closeApp();

        verify(controlClientSet).del("imei_imsi");
        verify(mediaClientSet).del("imei_imsi");
        verify(muenDriver).onControlTcpDisconnected();
        verify(chromeDriver).saveUserdata();
        verify(userData).upload();
    }

    @Test
    void closeInstanceShouldQuitDriverAndUpload() {
        UserChrome userChrome = new UserChrome(buildRequest(), fs, config, muenDriver, controlClientSet, mediaClientSet, remote);
        ChromiumDriverProxy chromeDriver = mock(ChromiumDriverProxy.class);
        UserData userData = mock(UserData.class);
        ReflectionTestUtils.setField(userChrome, "chromeDriver", chromeDriver);
        ReflectionTestUtils.setField(userChrome, "userData", userData);

        userChrome.closeInstance();

        verify(chromeDriver).quit();
        verify(userData).upload();
    }

    @Test
    void closeConnectionShouldDeleteControlAndMediaClients() {
        UserChrome userChrome = new UserChrome(buildRequest(), fs, config, muenDriver, controlClientSet, mediaClientSet, remote);

        userChrome.closeConnection();

        verify(controlClientSet).del("imei_imsi");
        verify(mediaClientSet).del("imei_imsi");
    }

    @Test
    void setHeartbeatsShouldUpdateValue() {
        UserChrome userChrome = new UserChrome(buildRequest(), fs, config, muenDriver, controlClientSet, mediaClientSet, remote);

        userChrome.setHeartbeats(456L);

        assertEquals(456L, userChrome.getHeartbeats());
    }

    private InitBrowserRequest buildRequest() {
        InitBrowserRequest request = new InitBrowserRequest();
        request.setImei("imei");
        request.setImsi("imsi");
        request.setClientLanguage("en");
        return request;
    }

}
