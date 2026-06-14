package com.viaoa.sync;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientCallbackInterface;

class OASyncClientTest {

    @Test
    void constructorAndGetClientInfoStoreServerConnectionSettings() {
        TestSyncClient client = new TestSyncClient("sync.example.test", 25001);

        ClientInfo ci = client.getClientInfo();

        assertEquals("sync.example.test", ci.getServerHostName());
        assertEquals(25001, ci.getServerHostPort());
        assertNotNull(ci.getCreated());
        assertSame(ci, client.getClientInfo());
        assertFalse(client.isStarted());
    }

    @Test
    void getDetailReturnsNullForNullMasterOrProperty() {
        TestSyncClient client = new TestSyncClient("localhost", 1);

        assertNull(client.getDetail(null, "name"));
        assertNull(client.getDetail(new com.test.pos.model.oa.Store(), null));
    }

    @Test
    void remoteClientCallbackEchoesPingAndCanBeReused() {
        TestSyncClient client = new TestSyncClient("localhost", 1);

        RemoteClientCallbackInterface callback = client.getRemoteClientCallback();

        assertSame(callback, client.getRemoteClientCallback());
        assertEquals("client recvd hello", callback.ping("hello"));
        assertNotNull(callback.performThreadDump("dump"));
        assertDoesNotThrow(() -> callback.stop("title", "message"));
    }

    @Test
    void stopBeforeStartIsSafe() throws Exception {
        TestSyncClient client = new TestSyncClient("localhost", 1);

        client.stop();

        assertFalse(client.isStarted());
    }

    private static class TestSyncClient extends OASyncClient {
        TestSyncClient(String host, int port) {
            super(host, port);
        }

        @Override
        protected void createRemoteDataSource() {
        }

        @Override
        protected void closeRemoteDataSource() {
        }
    }
}
