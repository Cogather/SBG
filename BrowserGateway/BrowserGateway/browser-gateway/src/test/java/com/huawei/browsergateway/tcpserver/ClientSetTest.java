package com.huawei.browsergateway.tcpserver;

import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSetTest {

    private ClientSet clientSet;

    @Mock
    private Channel mockChannel1;

    @Mock
    private Channel mockChannel2;

    private InetSocketAddress testAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientSet = new ClientSet();
        testAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);

        when(mockChannel1.remoteAddress()).thenReturn(testAddress);
        when(mockChannel2.remoteAddress()).thenReturn(testAddress);
    }

    @Test
    void testSetClient() {
        Client client = new Client(mockChannel1);
        clientSet.set("session1", client);

        assertEquals(client, clientSet.get("session1"));
    }

    @Test
    void testSetClientReplacesOldClient() {
        Client oldClient = new Client(mockChannel1);
        Client newClient = new Client(mockChannel2);

        clientSet.set("session1", oldClient);
        clientSet.set("session1", newClient);

        assertEquals(newClient, clientSet.get("session1"));
        verify(mockChannel1, times(1)).close();
    }

    @Test
    void testGetNonExistentClient() {
        assertNull(clientSet.get("nonExistent"));
    }

    @Test
    void testDelByKey() {
        Client client = new Client(mockChannel1);
        clientSet.set("session1", client);

        clientSet.del("session1");

        assertNull(clientSet.get("session1"));
        verify(mockChannel1, times(1)).close();
    }

    @Test
    void testDelNonExistentKey() {
        assertDoesNotThrow(() -> clientSet.del("nonExistent"));
    }

    @Test
    void testDelByClient() {
        Client client = new Client(mockChannel1);
        client.set(Client.VAL_SESSION_ID, "session1");
        clientSet.set("session1", client);

        clientSet.del(client);

        assertNull(clientSet.get("session1"));
        verify(mockChannel1, times(1)).close();
    }

    @Test
    void testDelByClientWithoutSessionId() {
        Client client = new Client(mockChannel1);

        assertDoesNotThrow(() -> clientSet.del(client));
        verify(mockChannel1, never()).close();
    }

    @Test
    void testAllClient() {
        Client client1 = new Client(mockChannel1);
        Client client2 = new Client(mockChannel2);

        clientSet.set("session1", client1);
        clientSet.set("session2", client2);

        Set<String> allClients = clientSet.allClient();

        assertEquals(2, allClients.size());
        assertTrue(allClients.contains("session1"));
        assertTrue(allClients.contains("session2"));
    }

    @Test
    void testAllClientReturnsEmptySetWhenNoClients() {
        Set<String> allClients = clientSet.allClient();

        assertNotNull(allClients);
        assertTrue(allClients.isEmpty());
    }
}
