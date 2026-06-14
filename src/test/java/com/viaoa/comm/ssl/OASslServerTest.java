package com.viaoa.comm.ssl;

import static org.junit.jupiter.api.Assertions.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.junit.jupiter.api.Test;

class OASslServerTest {

    @Test
    void constructorStoresHostAndPortForEngineCreation() throws Exception {
        TestServer server = new TestServer("localhost", 443);

        SSLEngine engine = server.exposeCreateSSLEngine();

        assertFalse(engine.getUseClientMode());
        assertFalse(engine.getNeedClientAuth());
        assertEquals("localhost", engine.getPeerHost());
        assertEquals(443, engine.getPeerPort());
    }

    @Test
    void createSSLContextLoadsBundledServerKeyStore() throws Exception {
        TestServer server = new TestServer("localhost", 443);

        SSLContext context = server.exposeCreateSSLContext();

        assertNotNull(context);
        assertEquals("TLS", context.getProtocol());
    }

    @Test
    void createSSLEngineConfiguresServerMode() throws Exception {
        TestServer server = new TestServer("localhost", 443);

        SSLEngine engine = server.exposeCreateSSLEngine();

        assertFalse(engine.getUseClientMode());
        assertFalse(engine.getNeedClientAuth());
    }

    @Test
    void logDoesNotThrow() {
        TestServer server = new TestServer("localhost", 443);

        assertDoesNotThrow(() -> server.exposeLog("message"));
    }

    private static class TestServer extends OASslServer {
        TestServer(String host, int port) {
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
        }
    }
}
