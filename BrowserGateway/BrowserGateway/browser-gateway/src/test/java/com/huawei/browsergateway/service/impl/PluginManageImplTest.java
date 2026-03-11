package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.config.WebsocketConfig;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.MuenPluginClassLoader;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.websocket.extension.MuenSessionManager;
import com.moon.cloud.browser.sdk.core.HWCallback;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PluginManageImplTest {

    @Mock
    private IFileStorage fs;

    @Mock
    private Config config;

    @Mock
    private ICse cse;

    @Mock
    private ControlClientSet controlClientSet;

    @Mock
    private MuenSessionManager muenSessionManager;

    @Mock
    private IAlarm alarm;

    @Mock
    private WebsocketConfig websocketConfig;

    @Mock
    private MuenPluginClassLoader muenPluginClassLoader;

    @Mock
    private MuenDriver muenDriver;

    private PluginManageImpl pluginManage;

    @BeforeEach
    void setUp() {
        pluginManage = new PluginManageImpl();
        ReflectionTestUtils.setField(pluginManage, "fs", fs);
        ReflectionTestUtils.setField(pluginManage, "config", config);
        ReflectionTestUtils.setField(pluginManage, "cse", cse);
        ReflectionTestUtils.setField(pluginManage, "controlClientSet", controlClientSet);
        ReflectionTestUtils.setField(pluginManage, "muenSessionManager", muenSessionManager);
        ReflectionTestUtils.setField(pluginManage, "alarm", alarm);
        ReflectionTestUtils.setField(pluginManage, "address", "127.0.0.1");

        when(config.getWebsocket()).thenReturn(websocketConfig);
        when(websocketConfig.getMediaPort()).thenReturn(30002);
        when(config.getTmpPath()).thenReturn("target/plugin-tmp");
        when(cse.getReportEndpoint()).thenReturn("report-endpoint");

        pluginManage.initPluginActive();
    }

    @Test
    void initPluginActiveShouldSetDefaultStatusAndType() {
        PluginActive pluginActive = pluginManage.getPluginActive();

        assertNotNull(pluginActive);
        assertEquals(Constant.NOTSTART, pluginActive.getStatus());
        assertEquals("ChromeExtend", pluginActive.getType());
    }

    @Test
    void updatePluginActiveShouldRefreshPluginMetadata() {
        pluginManage.updatePluginActive("demo-plugin", "1.0.0", "CustomType");

        PluginActive pluginActive = pluginManage.getPluginActive();
        assertEquals("demo-plugin", pluginActive.getName());
        assertEquals("1.0.0", pluginActive.getVersion());
        assertEquals("CustomType", pluginActive.getType());
    }

    @Test
    void loadPluginShouldMarkCompleteWhenSdkAndJsExtensionLoadSuccessfully() {
        PluginManageImpl pluginManageSpy = spy(pluginManage);
        doReturn(true).when(pluginManageSpy).loadSDK("plugin.jar");
        doReturn(true).when(pluginManageSpy).loadJSExtension("keys", "touch");

        pluginManageSpy.loadPlugin("keys", "touch", "plugin.jar");

        assertEquals(Constant.COMPLETE, pluginManageSpy.getPluginStatus());
        verify(alarm).clearAlarm(any(AlarmEvent.class));
        verify(alarm, never()).sendAlarm(any(AlarmEvent.class));
    }

    @Test
    void loadPluginShouldMarkFailedWhenSdkLoadFails() {
        PluginManageImpl pluginManageSpy = spy(pluginManage);
        doReturn(false).when(pluginManageSpy).loadSDK("broken.jar");

        pluginManageSpy.loadPlugin("keys", "touch", "broken.jar");

        assertEquals(Constant.FAILED, pluginManageSpy.getPluginStatus());
        verify(pluginManageSpy, never()).loadJSExtension(anyString(), anyString());

        ArgumentCaptor<AlarmEvent> alarmCaptor = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarm).sendAlarm(alarmCaptor.capture());
        assertEquals(AlarmEnum.ALARM_300030, alarmCaptor.getValue().getAlarmCodeEnum());
        assertTrue(alarmCaptor.getValue().getEventMessage().contains("Failed to create plugin"));
    }

    @Test
    void createDriverShouldBuildCallbackAndDelegateToPluginClassLoader() {
        ReflectionTestUtils.setField(pluginManage, "muenPluginClassLoader", muenPluginClassLoader);
        when(muenPluginClassLoader.createDriverInstance(any(HWCallback.class))).thenReturn(muenDriver);

        MuenDriver result = pluginManage.createDriver("user-1");

        assertSame(muenDriver, result);

        ArgumentCaptor<HWCallback> callbackCaptor = ArgumentCaptor.forClass(HWCallback.class);
        verify(muenPluginClassLoader).createDriverInstance(callbackCaptor.capture());
        HWCallbackImpl callback = (HWCallbackImpl) callbackCaptor.getValue();
        assertEquals("user-1", callback.getUserId());
        assertEquals("127.0.0.1:30002", callback.Address());
        assertEquals("report-endpoint", callback.getEndpoint());
        assertEquals("target/plugin-tmp", callback.getLocalTmp());
        assertSame(fs, callback.getFileStorage());
    }
}
