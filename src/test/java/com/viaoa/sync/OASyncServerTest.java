package com.viaoa.sync;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.model.ServerInfo;
import com.viaoa.sync.remote.RemoteSyncImpl;

class OASyncServerTest {

    @Test
    void constructorInitializesServerWithoutStartingNetworkServices() {
        OASyncServer server = new OASyncServer(0);

        ClientInfo ci = server.getClientInfo();

        assertEquals(0, ci.getConnectionId());
        assertNotNull(ci.getCreated());
        assertSame(ci, server.getClientInfo());
    }

    @Test
    void getRemoteSyncImplIsLazyAndStable() {
        OASyncServer server = new OASyncServer(0);

        RemoteSyncImpl rs = server.getRemoteSyncImpl();

        assertNotNull(rs);
        assertSame(rs, server.getRemoteSyncImpl());
        assertNull(server.getRemoteSyncInterface());
    }

    @Test
    void getServerInfoIsLazyAndStable() {
        OASyncServer server = new OASyncServer(0);

        ServerInfo info = server.getServerInfo();

        assertNotNull(info.getCreated());
        assertSame(info, server.getServerInfo());
    }

    @Test
    void getInvalidConnectionMessageReturnsDefaultMessage() {
        OASyncServer server = new OASyncServer(0);

        assertEquals("default", server.getInvalidConnectionMessage("default"));
        assertNull(server.getInvalidConnectionMessage(null));
    }

    @Test
    void performDgcBeforeRemoteMultiplexerCreationIsSafe() {
        OASyncServer server = new OASyncServer(0);

        assertDoesNotThrow(server::performDGC);
    }

    @Test
    void getServerFileIsLazyAndStable() {
        OASyncServer server = new OASyncServer(0);

        assertSame(server.getServerFile(), server.getServerFile());
    }
}
