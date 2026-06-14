package com.viaoa.comm.multiplexer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.viaoa.comm.multiplexer.io.VirtualServerSocket;

class OAMultiplexerServerTest {

    @Test
    void constructorWithHostAndPortStoresValues() {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 9876);

        assertEquals("127.0.0.1", server.getHost());
        assertEquals(9876, server.getPort());
        assertFalse(server.isStarted());
    }

    @Test
    void constructorWithPortUsesNonBlankLocalHost() {
        OAMultiplexerServer server = new OAMultiplexerServer(9876);

        assertNotNull(server.getHost());
        assertFalse(server.getHost().isBlank());
        assertEquals(9876, server.getPort());
    }

    @Test
    void throttleLimitCanBeConfiguredBeforeStart() throws Exception {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        server.setThrottleLimit(5);

        assertEquals(5, server.getThrottleLimit());
    }

    @Test
    void stopServerSocketBeforeStartIsNoOp() {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        assertDoesNotThrow(server::stopServerSocket);
        assertFalse(server.isStarted());
    }

    @Test
    void createServerSocketReturnsNamedVirtualServerSocket() throws Exception {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        VirtualServerSocket socket = server.createServerSocket("service");

        assertNotNull(socket);
        assertEquals("service", socket.getName());
        socket.close();
    }

    @Test
    void createServerSocketReturnsSameInstanceForSameName() throws Exception {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        VirtualServerSocket socket1 = server.createServerSocket("service");
        VirtualServerSocket socket2 = server.createServerSocket("service");

        assertSame(socket1, socket2);
        socket1.close();
    }

    @Test
    void createServerSocketReturnsNullForBlankNames() throws Exception {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        assertNull(server.createServerSocket(null));
        assertNull(server.createServerSocket(""));
    }

    @Test
    void invalidConnectionMessageRoundTrips() {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        server.setInvalidConnectionMessage("invalid");

        assertEquals("invalid", server.getInvalidConnectionMessage());
    }

    @Test
    void defaultStatsAreZeroBeforeStart() {
        OAMultiplexerServer server = new OAMultiplexerServer("127.0.0.1", 0);

        assertEquals(0, server.getReadCount());
        assertEquals(0, server.getReadSize());
        assertEquals(0, server.getWriteCount());
        assertEquals(0, server.getWriteSize());
        assertEquals(0, server.getCreatedConnectionCount());
        assertEquals(0, server.getLiveConnectionCount());
    }

    @Test
    void protectedCallbacksCanBeOverridden() throws IOException {
        TestServer server = new TestServer("127.0.0.1", 0);

        server.exposeDisconnect(42);

        assertEquals(42, server.disconnectedId);
    }

    private static class TestServer extends OAMultiplexerServer {
        int disconnectedId;

        TestServer(String host, int port) {
            super(host, port);
        }

        void exposeDisconnect(int connectionId) {
            onClientDisconnect(connectionId);
        }

        @Override
        protected void onClientDisconnect(int connectionId) {
            disconnectedId = connectionId;
        }
    }
}
