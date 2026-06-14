package com.viaoa.remote.multiplexer;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.comm.multiplexer.io.VirtualSocket;
import com.viaoa.remote.info.BindInfo;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;

class OARemoteMultiplexerClientTest {
    private TestRemoteClient remoteClient;

    @AfterEach
    void afterEach() {
        if (remoteClient != null) remoteClient.close();
    }

    @Test
    void constructorRejectsNullMultiplexerClient() {
        assertThrows(IllegalArgumentException.class, () -> new OARemoteMultiplexerClient(null));
    }

    @Test
    void constructorStoresMultiplexerClient() {
        TestMultiplexerClient mux = new TestMultiplexerClient();
        remoteClient = new TestRemoteClient(mux);

        assertSame(mux, remoteClient.getMultiplexerClient());
    }

    @Test
    void closeIsIdempotent() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertDoesNotThrow(remoteClient::close);
        assertDoesNotThrow(remoteClient::close);
    }

    @Test
    void registerBroadcastRejectsNullLookupName() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertThrows(IllegalArgumentException.class, () -> remoteClient.registerBroadcast(null, new RemoteImpl()));
    }

    @Test
    void lookupBroadcastRejectsNullCallback() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertThrows(IllegalArgumentException.class, () -> remoteClient.lookupBroadcast("name", null));
    }

    @Test
    void lookupReturnsNullForNullName() throws Exception {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertNull(remoteClient.lookup(null));
    }

    @Test
    void getSocketDelegatesToMultiplexerClient() {
        TestMultiplexerClient mux = new TestMultiplexerClient();
        remoteClient = new TestRemoteClient(mux);

        assertSame(mux.socket, remoteClient.getSocket());
    }

    @Test
    void createBindNameUsesSocketConnectionIdAndIncreasingCounter() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());
        RequestInfo request = new RequestInfo();
        request.socket = new TestVirtualSocket(12, 2, "CtoS");

        String name1 = remoteClient.exposeCreateBindName(request);
        String name2 = remoteClient.exposeCreateBindName(request);

        assertTrue(name1.startsWith("C.12."));
        assertTrue(name2.startsWith("C.12."));
        assertNotEquals(name1, name2);
    }

    @Test
    void getProxyForCtoSReturnsCachedProxyAndBindInfo() throws Exception {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        Object proxy1 = remoteClient.exposeGetProxyForCtoS("lookup", Remote.class, false);
        Object proxy2 = remoteClient.exposeGetProxyForCtoS("lookup", Remote.class, false);
        BindInfo bind = remoteClient.exposeGetBindInfo("lookup");

        assertSame(proxy1, proxy2);
        assertTrue(Proxy.isProxyClass(proxy1.getClass()));
        assertSame(Remote.class, bind.interfaceClass);
        assertFalse(bind.usesQueue);
        assertSame(proxy1, bind.getObject());
    }

    @Test
    void getProxyForCtoSReturnsNullForNullName() throws Exception {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertNull(remoteClient.exposeGetProxyForCtoS(null, Remote.class, false));
    }

    @Test
    void getBindInfoRejectsNullNameOrInterface() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertThrows(IllegalArgumentException.class, () -> remoteClient.exposeCreateBindInfo(null, null, Remote.class, false, false));
        assertThrows(IllegalArgumentException.class, () -> remoteClient.exposeCreateBindInfo("name", null, null, false, false));
    }

    @Test
    void performDGCDoesNotRemoveLiveBindInfo() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());
        RemoteImpl impl = new RemoteImpl();
        BindInfo bind = remoteClient.exposeCreateBindInfo("name", impl, Remote.class, false, false);

        remoteClient.performDGC();

        assertSame(bind, remoteClient.exposeGetBindInfo("name"));
    }

    @Test
    void getBindInfoForObjectUsesIdentity() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());
        RemoteImpl impl = new RemoteImpl();
        BindInfo bind = remoteClient.exposeCreateBindInfo("name", impl, Remote.class, false, false);

        assertSame(bind, remoteClient.exposeGetBindInfoForObject(impl));
        assertNull(remoteClient.exposeGetBindInfoForObject(new RemoteImpl()));
    }

    @Test
    void methodCountersStartAtZero() {
        remoteClient = new TestRemoteClient(new TestMultiplexerClient());

        assertEquals(0, remoteClient.getMethodCallCount());
        assertEquals(0, remoteClient.getReceivedMethodCount());
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

    private static class TestRemoteClient extends OARemoteMultiplexerClient {
        TestRemoteClient(OAMultiplexerClient multiplexerClient) {
            super(multiplexerClient);
        }

        String exposeCreateBindName(RequestInfo ri) {
            return createBindName(ri);
        }

        Object exposeGetProxyForCtoS(String name, Class<?> type, boolean usesQueue) throws Exception {
            return getProxyForCtoS(name, type, usesQueue);
        }

        BindInfo exposeGetBindInfo(String name) {
            return getBindInfo(name);
        }

        BindInfo exposeCreateBindInfo(String name, Object obj, Class<?> type, boolean usesQueue, boolean broadcast) {
            return getBindInfo(name, obj, type, usesQueue, broadcast);
        }

        BindInfo exposeGetBindInfoForObject(Object obj) {
            return getBindInfoForObject(obj);
        }
    }

    private static class TestMultiplexerClient extends OAMultiplexerClient {
        final Socket socket = new Socket();

        TestMultiplexerClient() {
            super("localhost", 1);
        }

        @Override
        public Socket getSocket() {
            return socket;
        }

        @Override
        public VirtualSocket createSocket(String serverSocketName) {
            return new TestVirtualSocket(1, 1, serverSocketName);
        }
    }

    private static class TestVirtualSocket extends VirtualSocket {
        TestVirtualSocket(int connectionId, int id, String name) {
            super(connectionId, id, name);
        }

        @Override public int read(byte[] bs, int off, int len) { return -1; }
        @Override public int read() { return -1; }
        @Override public void write(byte[] bs, int off, int len) { }
        @Override public void write(int b) { }
        @Override public void close(boolean bSendCommand) { }
    }
}
