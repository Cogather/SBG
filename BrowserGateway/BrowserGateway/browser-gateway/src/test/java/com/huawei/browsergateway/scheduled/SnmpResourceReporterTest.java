package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.dto.MeasureItem;
import com.huawei.browsergateway.adapter.dto.SnmpPerfRequest;
import com.huawei.browsergateway.adapter.http.SnmpHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnmpResourceReporterTest {

    @Mock
    private ResourceMonitorAdapter resourceMonitorAdapter;

    @Mock
    private SnmpHttpClient snmpHttpClient;

    private SnmpResourceReporter snmpResourceReporter;

    @BeforeEach
    void setUp() {
        snmpResourceReporter = new SnmpResourceReporter();
        ReflectionTestUtils.setField(snmpResourceReporter, "resourceMonitorAdapter", resourceMonitorAdapter);
        ReflectionTestUtils.setField(snmpResourceReporter, "snmpHttpClient", snmpHttpClient);
        ReflectionTestUtils.setField(snmpResourceReporter, "period", 300000L);
    }

    @Test
    void testReportResourceLoad_Success() {
        when(resourceMonitorAdapter.getCpuUsage()).thenReturn(35.5f);
        when(resourceMonitorAdapter.getMemoryUsage()).thenReturn(60.2f);

        snmpResourceReporter.reportResourceLoad();

        verify(resourceMonitorAdapter, times(1)).getCpuUsage();
        verify(resourceMonitorAdapter, times(1)).getMemoryUsage();
        verify(snmpHttpClient, times(1)).sendPerf(any(SnmpPerfRequest.class));
    }

    @Test
    void testReportResourceLoad_AdapterFailure() {
        when(resourceMonitorAdapter.getCpuUsage()).thenThrow(new RuntimeException("CPU monitoring failed"));

        snmpResourceReporter.reportResourceLoad();

        verify(resourceMonitorAdapter, times(1)).getCpuUsage();
        verify(resourceMonitorAdapter, never()).getMemoryUsage();
        verify(snmpHttpClient, never()).sendPerf(any(SnmpPerfRequest.class));
    }

    @Test
    void testReportResourceLoad_SnmpClientFailure() {
        when(resourceMonitorAdapter.getCpuUsage()).thenReturn(35.5f);
        when(resourceMonitorAdapter.getMemoryUsage()).thenReturn(60.2f);
        doThrow(new RuntimeException("SNMP send failed")).when(snmpHttpClient).sendPerf(any(SnmpPerfRequest.class));

        snmpResourceReporter.reportResourceLoad();

        verify(resourceMonitorAdapter, times(1)).getCpuUsage();
        verify(resourceMonitorAdapter, times(1)).getMemoryUsage();
        verify(snmpHttpClient, times(1)).sendPerf(any(SnmpPerfRequest.class));
    }

    @Test
    void testReportResourceLoad_ValidMeasureList() {
        when(resourceMonitorAdapter.getCpuUsage()).thenReturn(35.5f);
        when(resourceMonitorAdapter.getMemoryUsage()).thenReturn(60.2f);

        snmpResourceReporter.reportResourceLoad();

        verify(snmpHttpClient, times(1)).sendPerf(argThat(request -> {
            if (request.getMeasureList() == null || request.getMeasureList().size() != 2) {
                return false;
            }
            MeasureItem cpuItem = request.getMeasureList().get(0);
            MeasureItem memItem = request.getMeasureList().get(1);
            
            return "320301".equals(cpuItem.getMeasureUnit()) &&
                   "320302".equals(memItem.getMeasureUnit()) &&
                   "set".equals(cpuItem.getOptType()) &&
                   "set".equals(memItem.getOptType());
        }));
    }
}