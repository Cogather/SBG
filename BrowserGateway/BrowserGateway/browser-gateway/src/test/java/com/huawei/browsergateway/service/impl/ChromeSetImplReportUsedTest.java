package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.adapter.ServiceManagementAdapter;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.config.ReportConfig;
import com.huawei.browsergateway.config.WebsocketConfig;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.TpusedMediaAccumulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ChromeSetImpl#reportUsed()} 与 {@code tpused}、累加器扣减的契约测试。
 */
@ExtendWith(MockitoExtension.class)
class ChromeSetImplReportUsedTest {

    @Spy
    private TpusedMediaAccumulator tpusedMediaAccumulator = new TpusedMediaAccumulator();

    @Mock
    private Config config;
    @Mock
    private ReportConfig reportConfig;
    @Mock
    private WebsocketConfig websocketConfig;
    @Mock
    private IPluginManage pluginManage;
    @Mock
    private ServiceManagementAdapter serviceManagementAdapter;

    @InjectMocks
    private ChromeSetImpl chromeSet;

    @BeforeEach
    void stubConfig() {
        when(config.getSelfAddr()).thenReturn("gw-1");
        when(config.getAddress()).thenReturn("10.0.0.1");
        when(config.getWebsocket()).thenReturn(websocketConfig);
        when(websocketConfig.getMediaPort()).thenReturn(9000);
        when(config.getReport()).thenReturn(reportConfig);
        when(reportConfig.getControlEndpoint()).thenReturn("ce");
        when(reportConfig.getMediaEndpoint()).thenReturn("me");
        when(reportConfig.getControlTlsEndpoint()).thenReturn("cet");
        when(reportConfig.getMediaTlsEndpoint()).thenReturn("met");
        when(reportConfig.getCap()).thenReturn(10);
        when(pluginManage.getPluginStatus()).thenReturn("ok");
    }

    @Test
    void reportUsed_onCseSuccess_subtractsReportedTpused() {
        tpusedMediaAccumulator.addPayloadBytes(50);
        when(serviceManagementAdapter.reportInstanceProperties(any())).thenReturn(true);

        chromeSet.reportUsed();

        assertEquals(0, tpusedMediaAccumulator.getCurrentBytes());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(serviceManagementAdapter).reportInstanceProperties(captor.capture());
        String statusJson = captor.getValue().get("status");
        assertTrue(statusJson.contains("\"tpused\":50") || statusJson.contains("\"tpused\": 50"),
                () -> "status JSON should contain tpused=50: " + statusJson);
    }

    @Test
    void reportUsed_onCseFailure_doesNotSubtractTpused() {
        tpusedMediaAccumulator.addPayloadBytes(80);
        when(serviceManagementAdapter.reportInstanceProperties(any())).thenReturn(false);

        chromeSet.reportUsed();

        assertEquals(80, tpusedMediaAccumulator.getCurrentBytes());
    }
}
