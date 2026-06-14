package com.viaoa.comm.multiplexer;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;

class OAMultiplexerClientTest {

    @Test
    void constructorWithUriStoresHostAndPort() throws Exception {
        OAMultiplexerClient client = new OAMultiplexerClient(new URI("oa://localhost:1234"));

        assertEquals("localhost", client.getHost());
        assertEquals(1234, client.getPort());
        assertFalse(client.isConnected());
    }

    @Test
    void constructorWithHostAndPortStoresValues() {
        OAMultiplexerClient client = new OAMultiplexerClient("127.0.0.1", 5678);

        assertEquals("127.0.0.1", client.getHost());
        assertEquals(5678, client.getPort());
        assertNull(client.getSocket());
    }

    @Test
    void keepAliveCanBeConfiguredBeforeStart() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        client.setKeepAlive(10);

        assertEquals(10, client.getKeepAlive());
        assertFalse(client.isConnected());
    }

    @Test
    void runKeepAliveThreadIsNoOpWhenDisabled() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        assertDoesNotThrow(client::runKeepAliveThread);

        assertEquals(0, client.getKeepAlive());
    }

    @Test
    void pingServerIsNoOpBeforeStart() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        assertDoesNotThrow(client::pingServer);
    }

    @Test
    void throttleLimitCanBeConfiguredBeforeStart() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        client.setThrottleLimit(3);

        assertEquals(3, client.getThrottleLimit());
    }

    @Test
    void createSocketRequiresStartedClient() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        assertThrows(NullPointerException.class, () -> client.createSocket("service"));
        assertEquals(0, client.getCreatedSocketCount());
    }

    @Test
    void closeBeforeStartIsNoOp() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        assertDoesNotThrow(client::close);
        assertFalse(client.isConnected());
    }

    @Test
    void defaultStateBeforeStartHasNoConnectionOrStats() {
        OAMultiplexerClient client = new OAMultiplexerClient("localhost", 1);

        assertEquals(-1, client.getConnectionId());
        assertFalse(client.isConnected());
        assertEquals(0, client.getWriteCount());
        assertEquals(0, client.getWriteSize());
        assertEquals(0, client.getReadCount());
        assertEquals(0, client.getReadSize());
    }
}
