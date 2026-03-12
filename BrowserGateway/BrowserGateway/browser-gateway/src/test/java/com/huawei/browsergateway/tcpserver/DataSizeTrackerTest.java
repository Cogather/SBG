package com.huawei.browsergateway.tcpserver;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.service.IRemote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DataSizeTrackerTest {

    @Mock
    private IRemote mockRemote;

    private DataSizeTracker dataSizeTracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataSizeTracker = new DataSizeTracker(mockRemote, Constant.TCP_CONTROL);
        ReflectionTestUtils.setField(dataSizeTracker, "reportPeriodMillis", 1000L);
    }

    @Test
    void testAddDataSize() {
        dataSizeTracker.addDataSize("imei123", 1, "192.168.1.1", 100);
        dataSizeTracker.addDataSize("imei123", 1, "192.168.1.1", 200);

        dataSizeTracker.sendAllTrafficStatInfo();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockRemote, atLeastOnce()).sendTrafficControl(captor.capture());

        String sentData = captor.getValue();
        assertNotNull(sentData);
        assertTrue(sentData.contains("imei123"));
    }

    @Test
    void testAddDataSizeForDifferentUsers() {
        dataSizeTracker.addDataSize("imei1", 1, "192.168.1.1", 100);
        dataSizeTracker.addDataSize("imei2", 2, "192.168.1.2", 200);

        dataSizeTracker.sendAllTrafficStatInfo();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockRemote, atLeastOnce()).sendTrafficControl(captor.capture());
    }

    @Test
    void testSendAllTrafficStatInfoClearsMap() {
        dataSizeTracker.addDataSize("imei123", 1, "192.168.1.1", 100);

        dataSizeTracker.sendAllTrafficStatInfo();
        reset(mockRemote);
        dataSizeTracker.sendAllTrafficStatInfo();

        verify(mockRemote, never()).sendTrafficControl(anyString());
    }

    @Test
    void testSendAllTrafficStatInfoWithMediaType() {
        DataSizeTracker mediaTracker = new DataSizeTracker(mockRemote, Constant.TCP_MEDIA);
        ReflectionTestUtils.setField(mediaTracker, "reportPeriodMillis", 1000L);

        mediaTracker.addDataSize("imei123", 1, "192.168.1.1", 100);
        mediaTracker.sendAllTrafficStatInfo();

        verify(mockRemote, atLeastOnce()).sendTrafficMedia(anyString());
    }

    @Test
    void testSendAllTrafficStatInfoSkipsZeroDataSize() {
        dataSizeTracker.addDataSize("imei123", 1, "192.168.1.1", 100);
        dataSizeTracker.sendAllTrafficStatInfo();

        reset(mockRemote);
        dataSizeTracker.sendAllTrafficStatInfo();

        verify(mockRemote, never()).sendTrafficControl(anyString());
    }

    @Test
    void testBatchSending() {
        for (int i = 0; i < 1500; i++) {
            dataSizeTracker.addDataSize("imei" + i, 1, "192.168.1.1", 100);
        }

        dataSizeTracker.sendAllTrafficStatInfo();

        verify(mockRemote, atLeast(2)).sendTrafficControl(anyString());
    }
}
