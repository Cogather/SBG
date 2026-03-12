package com.huawei.browsergateway.tcpserver.control;

import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.Client;
import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControlClientSetTest {

    private ControlClientSet controlClientSet;

    @Mock
    private IRemote mockRemote;

    @Mock
    private Channel mockChannel1;

    @Mock
    private Channel mockChannel2;

    private InetSocketAddress testAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controlClientSet = new ControlClientSet();
        ReflectionTestUtils.setField(controlClientSet, "remote", mockRemote);
        testAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);

        when(mockChannel1.remoteAddress()).thenReturn(testAddress);
        when(mockChannel2.remoteAddress()).thenReturn(testAddress);
    }

    @Test
    void testSetClientWithNoExistingClient() {
        Client client = new Client(mockChannel1);
        controlClientSet.set("session1", client);

        assertEquals(client, controlClientSet.get("session1"));
        verify(mockRemote, never()).fallback(anyString());
    }

    @Test
    void testSetClientReplacesOldClientAndCallsFallback() {
        Client oldClient = new Client(mockChannel1);
        Client newClient = new Client(mockChannel2);

        controlClientSet.set("session1", oldClient);
        controlClientSet.set("session1", newClient);

        assertEquals(newClient, controlClientSet.get("session1"));
        verify(mockChannel1, times(1)).close();
        verify(mockRemote, times(1)).fallback("session1");
        assertEquals("true", oldClient.getStr(Client.HAS_BEEN_FALLBACK));
    }

    @Test
    void testSetClientMarksOldClientAsFallback() {
        Client oldClient = new Client(mockChannel1);
        Client newClient = new Client(mockChannel2);

        controlClientSet.set("session1", oldClient);
        controlClientSet.set("session1", newClient);

        assertEquals("true", oldClient.getStr(Client.HAS_BEEN_FALLBACK));
    }
}
