package com.viaoa.comm.ssl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.junit.jupiter.api.Test;

class OASslBaseTest {

    @Test
    void getSSLContextCachesCreatedContext() throws Exception {
        TestClient client = new TestClient("localhost", 1000);

        SSLContext context1 = client.exposeGetSSLContext();
        SSLContext context2 = client.exposeGetSSLContext();

        assertSame(context1, context2);
    }

    @Test
    void getSSLEngineCachesInitializedEngine() throws Exception {
        TestClient client = new TestClient("localhost", 1000);

        SSLEngine engine1 = client.exposeGetSSLEngine();
        SSLEngine engine2 = client.exposeGetSSLEngine();

        assertSame(engine1, engine2);
        assertArrayEquals(OASslBase.PREFERRED_CIPHER_NAMES, engine1.getEnabledCipherSuites());
    }

    @Test
    void resetSSLRestartsHandshakeOnExistingEngine() throws Exception {
        TestClient client = new TestClient("localhost", 1000);
        SSLEngine engine = client.exposeGetSSLEngine();

        assertDoesNotThrow(() -> client.resetSSL());

        assertSame(engine, client.exposeGetSSLEngine());
        assertNotNull(engine.getHandshakeStatus());
    }

    @Test
    void initializeCreatesEngine() throws Exception {
        TestClient client = new TestClient("localhost", 1000);

        client.initialize();

        assertNotNull(client.exposeGetSSLEngine());
    }

    @Test
    void receiveInputCopiesBytesIntoPendingInputBuffer() throws Exception {
        TestClient client = new TestClient("localhost", 1000);
        byte[] source = new byte[] { 9, 8, 7, 6 };

        client.exposeReceiveInput(source, 1, 2);
        source[1] = 0;

        assertArrayEquals(new byte[] { 8, 7 }, client.pendingInputBytes());
    }

    private static class TestClient extends OASslClient {
        final List<byte[]> sent = new ArrayList<>();

        TestClient(String host, int port) {
            super(host, port);
        }

        SSLContext exposeGetSSLContext() throws Exception {
            return getSSLContext();
        }

        SSLEngine exposeGetSSLEngine() throws Exception {
            return getSSLEngine();
        }

        void exposeReceiveInput(byte[] bs, int offset, int len) throws Exception {
            receiveInput(bs, offset, len);
        }

        byte[] pendingInputBytes() throws Exception {
            Field field = OASslBase.class.getDeclaredField("bsGetInput");
            field.setAccessible(true);
            return (byte[]) field.get(this);
        }

        @Override
        protected void sendOutput(byte[] bs, int offset, int len, boolean bHandshakeOnly) {
            byte[] copy = new byte[len];
            System.arraycopy(bs, offset, copy, 0, len);
            sent.add(copy);
        }
    }
}
