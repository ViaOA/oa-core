package com.viaoa.remote.multiplexer;

import static org.junit.jupiter.api.Assertions.*;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.viaoa.comm.multiplexer.OAMultiplexerServer;
import com.viaoa.remote.info.BindInfo;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;

class OARemoteMultiplexerServerTest {

    @Test
    void constructorStoresMultiplexerServer() {
        OAMultiplexerServer mux = new OAMultiplexerServer("127.0.0.1", 0);
        OARemoteMultiplexerServer server = new OARemoteMultiplexerServer(mux);

        assertSame(mux, server.getMultiplexerServer());
    }

    @Test
    void createAndRemoveSessionByConnectionId() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));
        Socket socket = new Socket();

        server.createSession(socket, 9);
        OARemoteMultiplexerServer.Session session = server.getSession(9, false);
        assertNotNull(session);
        assertSame(socket, session.realSocket);

        server.removeSession(9);
        assertNull(server.getSession(9, false));
    }

    @Test
    void getSessionHonorsCreateFlag() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertNull(server.getSession(1, false));
        assertNotNull(server.getSession(1, true));
        assertSame(server.getSession(1, false), server.getSession(1, true));
    }

    @Test
    void createLookupRegistersAndRemoveLookupRemovesBindInfo() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));
        RemoteImpl impl = new RemoteImpl();

        server.createLookup("lookup", impl, Remote.class);
        BindInfo bind = server.exposeGetBindInfo("lookup");

        assertNotNull(bind);
        assertSame(impl, bind.getObject());
        assertFalse(bind.usesQueue);
        assertTrue(server.removeLookup("lookup"));
        assertNull(server.exposeGetBindInfo("lookup"));
        assertFalse(server.removeLookup("lookup"));
    }

    @Test
    void createLookupWithQueueCreatesCircularQueue() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        server.createLookup("lookup", new RemoteImpl(), Remote.class, "queue", 25);

        BindInfo bind = server.exposeGetBindInfo("lookup");
        assertTrue(bind.usesQueue);
        assertEquals("queue", bind.asyncQueueName);
        assertEquals(25, bind.asyncQueueSize);
        assertNotNull(server.getCircularQueue("queue"));
    }

    @Test
    void getBindInfoRejectsNullOrNonInterfaceInput() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertThrows(IllegalArgumentException.class, () -> server.exposeCreateBindInfo(null, null, Remote.class, null, 0));
        assertThrows(IllegalArgumentException.class, () -> server.exposeCreateBindInfo("name", null, RemoteImpl.class, null, 0));
    }

    @Test
    void getBindInfoByObjectUsesIdentity() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));
        RemoteImpl impl = new RemoteImpl();
        server.createLookup("lookup", impl, Remote.class);

        assertSame(server.exposeGetBindInfo("lookup"), server.exposeGetBindInfo(impl));
        assertNull(server.exposeGetBindInfo(new RemoteImpl()));
    }

    @Test
    void createBroadcastRejectsInvalidArguments() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertThrows(IllegalArgumentException.class, () -> server.createBroadcast(null, Remote.class, "q", 100));
        assertThrows(IllegalArgumentException.class, () -> server.createBroadcast("name", null, "q", 100));
        assertThrows(IllegalArgumentException.class, () -> server.createBroadcast("name", new Object(), Remote.class, "q", 100));
    }

    @Test
    void notifyAndWaitForMethodInvoked() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));
        RequestInfo info = new RequestInfo();

        server.exposeNotifyMethodInvoked(info);

        assertTrue(server.exposeWaitForMethodInvoked(info, 1));
        assertTrue(info.methodInvoked);
    }

    @Test
    void waitForMethodInvokedTimesOutWhenNotNotified() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertFalse(server.exposeWaitForMethodInvoked(new RequestInfo(), 1));
    }

    @Test
    void notifyAndWaitForProcessedByServer() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));
        RequestInfo info = new RequestInfo();
        info.bind = new BindInfo("name", new RemoteImpl(), Remote.class, null, false, "queue", 10);

        server.exposeNotifyProcessedByServer(info);
        server.exposeWaitForProcessedByServer(info);

        assertTrue(info.processedByServerQueue);
    }

    @Test
    void countersAndQueueHeadStartAtZero() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertEquals(0, server.getMethodCallCount());
        assertEquals(0, server.getReceivedMethodCount());
        assertEquals(0, server.getQueueHeadPos());
        assertNull(server.getCircularQueue("missing"));
    }

    @Test
    void shouldSendSyncMessageToClientDefaultsTrue() {
        TestRemoteServer server = new TestRemoteServer(new OAMultiplexerServer("127.0.0.1", 0));

        assertTrue(server.exposeShouldSendSyncMessageToClient(new RequestInfo(), new ConcurrentHashMap<>()));
    }

    @OARemoteInterface
    private interface Remote {
        void call();
    }

    private static class RemoteImpl implements Remote {
        @Override
        public void call() {
        }
    }

    private static class TestRemoteServer extends OARemoteMultiplexerServer {
        TestRemoteServer(OAMultiplexerServer server) {
            super(server);
        }

        BindInfo exposeGetBindInfo(String name) {
            return getBindInfo(name);
        }

        BindInfo exposeGetBindInfo(Object obj) {
            return getBindInfo(obj);
        }

        BindInfo exposeCreateBindInfo(String name, Object obj, Class<?> interfaceClass, String queueName, int queueSize) {
            return getBindInfo(name, obj, interfaceClass, queueName, queueSize);
        }

        void exposeNotifyMethodInvoked(RequestInfo ri) {
            notifyMethodInvoked(ri);
        }

        boolean exposeWaitForMethodInvoked(RequestInfo ri, int maxSeconds) {
            return waitForMethodInvoked(ri, maxSeconds);
        }

        void exposeNotifyProcessedByServer(RequestInfo ri) {
            notifyProcessedByServer(ri);
        }

        void exposeWaitForProcessedByServer(RequestInfo ri) {
            waitForProcessedByServer(ri);
        }

        boolean exposeShouldSendSyncMessageToClient(RequestInfo ri, ConcurrentHashMap<java.util.UUID, Boolean> hmGuid) {
            return shouldSendSyncMessageToClient(ri, hmGuid);
        }
    }
}
