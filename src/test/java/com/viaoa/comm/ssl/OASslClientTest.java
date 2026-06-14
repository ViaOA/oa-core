package com.viaoa.comm.ssl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.junit.jupiter.api.Test;

class OASslClientTest {

    @Test
    void constructorStoresHostAndPortForEngineCreation() throws Exception {
        TestClient client = new TestClient("localhost", 12345);

        SSLEngine engine = client.exposeCreateSSLEngine();

        assertTrue(engine.getUseClientMode());
        assertFalse(engine.getWantClientAuth());
        assertEquals("localhost", engine.getPeerHost());
        assertEquals(12345, engine.getPeerPort());
    }

    @Test
    void createSSLContextLoadsBundledClientTrustStore() throws Exception {
        TestClient client = new TestClient("localhost", 12345);

        SSLContext context = client.exposeCreateSSLContext();

        assertNotNull(context);
        assertEquals("TLS", context.getProtocol());
    }

    @Test
    void createSSLEngineConfiguresClientMode() throws Exception {
        TestClient client = new TestClient("localhost", 12345);

        SSLEngine engine = client.exposeCreateSSLEngine();

        assertTrue(engine.getUseClientMode());
        assertFalse(engine.getWantClientAuth());
    }

    @Test
    void logDoesNotAffectSslState() {
        TestClient client = new TestClient("localhost", 12345);

        client.exposeLog("message");

        assertTrue(client.sent.isEmpty());
    }

    private static class TestClient extends OASslClient {
        final List<byte[]> sent = new ArrayList<>();

        TestClient(String host, int port) {
            super(host, port);
        }

        SSLContext exposeCreateSSLContext() throws Exception {
            return createSSLContext();
        }

        SSLEngine exposeCreateSSLEngine() throws Exception {
            return createSSLEngine();
        }

        void exposeLog(String msg) {
            log(msg);
        }

        @Override
        protected void sendOutput(byte[] bs, int offset, int len, boolean bHandshakeOnly) {
            byte[] copy = new byte[len];
            System.arraycopy(bs, offset, copy, 0, len);
            sent.add(copy);
        }
    }
}
