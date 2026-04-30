package com.huawei.browsergateway.adapter.http;

import com.huawei.browsergateway.adapter.dto.MeasureItem;
import com.huawei.browsergateway.adapter.dto.SnmpPerfRequest;
import com.huawei.browsergateway.config.SnmpConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnmpHttpClientTest {

    @Mock
    private SnmpConfig snmpConfig;

    @Mock
    private RestTemplate restTemplate;

    private SnmpHttpClient snmpHttpClient;

    @BeforeEach
    void setUp() {
        lenient().when(snmpConfig.getServerIPs()).thenReturn(new String[]{"192.168.16.4"});
        lenient().when(snmpConfig.getServerPort()).thenReturn(162);
        lenient().when(snmpConfig.getRetryTimes()).thenReturn(3);
        lenient().when(snmpConfig.getRetryInterval()).thenReturn(100);

        snmpHttpClient = new SnmpHttpClient();
        ReflectionTestUtils.setField(snmpHttpClient, "snmpConfig", snmpConfig);
        ReflectionTestUtils.setField(snmpHttpClient, "restTemplate", restTemplate);
    }

    @Test
    void testSendPerf_Success() {
        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(response);

        SnmpPerfRequest request = new SnmpPerfRequest();
        request.setMeasureList(Arrays.asList(
            new MeasureItem("TEST", "1", "bgw-test", 0, "set")
        ));

        snmpHttpClient.sendPerf(request);

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testSendPerf_AllServersFailed() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        SnmpPerfRequest request = new SnmpPerfRequest();
        request.setMeasureList(Arrays.asList(
            new MeasureItem("TEST", "1", "bgw-test", 0, "set")
        ));

        snmpHttpClient.sendPerf(request);

        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testSendPerf_EmptyServerIPs() {
        when(snmpConfig.getServerIPs()).thenReturn(new String[]{"", null, ""});

        SnmpPerfRequest request = new SnmpPerfRequest();
        request.setMeasureList(Arrays.asList(
            new MeasureItem("TEST", "1", "bgw-test", 0, "set")
        ));

        snmpHttpClient.sendPerf(request);

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }
}