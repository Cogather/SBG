package com.huawei.browsergateway.tcpserver.media;

import com.huawei.browsergateway.tcpserver.Client;
import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaClientSetTest {

    private MediaClientSet mediaClientSet;

    @Mock
    private Channel mockChannel;

    private InetSocketAddress testAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mediaClientSet = new MediaClientSet();
        testAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);

        when(mockChannel.remoteAddress()).thenReturn(testAddress);
    }

    @Test
    void testMediaClientSetInheritsFromClientSet() {
        Client client = new Client(mockChannel);
        mediaClientSet.set("session1", client);

        assertEquals(client, mediaClientSet.get("session1"));
    }

    @Test
    void testMediaClientSetCanDeleteClient() {
        Client client = new Client(mockChannel);
        mediaClientSet.set("session1", client);

        mediaClientSet.del("session1");

        assertNull(mediaClientSet.get("session1"));
        verify(mockChannel, times(1)).close();
    }
}
