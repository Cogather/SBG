package com.huawei.browsergateway.tcpserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientTest {

    @Mock
    private Channel mockChannel;

    @Mock
    private ChannelHandlerContext mockCtx;

    @Mock
    private Attribute<Client> mockAttribute;

    private InetSocketAddress testAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);
    }

    @Test
    void testClientCreation() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);

        assertNotNull(client);
        assertEquals("127.0.0.1", client.getClientIpAddress());
    }

    @Test
    void testSetAndGetStr() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);
        client.set("testKey", "testValue");

        assertEquals("testValue", client.getStr("testKey"));
    }

    @Test
    void testGetStrReturnsNullForNonExistentKey() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);

        assertNull(client.getStr("nonExistentKey"));
    }

    @Test
    void testSetAndGetInt() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);
        client.set("intKey", 42);

        assertEquals(42, client.getInt("intKey"));
    }

    @Test
    void testGetIntReturnsNullForNonExistentKey() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);

        assertNull(client.getInt("nonExistentKey"));
    }

    @Test
    void testGetTime() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);
        long currentTime = System.nanoTime();
        client.set("timeKey", currentTime);

        assertEquals(currentTime, client.getTime("timeKey"));
    }

    @Test
    void testGetTimeReturnsZeroForNonExistentKey() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);

        assertEquals(0L, client.getTime("nonExistentKey"));
    }

    @Test
    void testClose() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = new Client(mockChannel);
        client.close();

        verify(mockChannel, times(1)).close();
    }

    @Test
    void testSendWhenChannelIsActive() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);
        when(mockChannel.isActive()).thenReturn(true);

        Client client = new Client(mockChannel);
        Object testObject = new Object();
        client.send(testObject);

        verify(mockChannel, times(1)).writeAndFlush(testObject);
    }

    @Test
    void testSendWhenChannelIsNotActive() {
        when(mockChannel.remoteAddress()).thenReturn(testAddress);
        when(mockChannel.isActive()).thenReturn(false);

        Client client = new Client(mockChannel);
        Object testObject = new Object();
        client.send(testObject);

        verify(mockChannel, never()).writeAndFlush(any());
    }

    @Test
    void testFromCtxCreatesNewClient() {
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockCtx.attr(any(AttributeKey.class))).thenReturn(mockAttribute);
        when(mockAttribute.get()).thenReturn(null);
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = Client.fromCtx(mockCtx);

        assertNotNull(client);
        verify(mockAttribute, times(1)).set(any(Client.class));
    }

    @Test
    void testFromCtxReturnsExistingClient() {
        Client existingClient = new Client(mockChannel);
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockCtx.attr(any(AttributeKey.class))).thenReturn(mockAttribute);
        when(mockAttribute.get()).thenReturn(existingClient);
        when(mockChannel.remoteAddress()).thenReturn(testAddress);

        Client client = Client.fromCtx(mockCtx);

        assertSame(existingClient, client);
        verify(mockAttribute, never()).set(any(Client.class));
    }
}
