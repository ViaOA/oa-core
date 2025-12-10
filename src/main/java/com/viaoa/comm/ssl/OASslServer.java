/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.comm.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * SSL server implementation built on {@link OASslBase}. This class loads a
 * private-key–based keystore, initializes an {@link SSLContext} for server-side
 * TLS, and configures an {@link SSLEngine} that performs encrypted
 * bidirectional communication with connected SSL clients.
 *
 * <p>The keystore (<code>sslserver.jks</code>) contains the server certificate
 * and private key. During SSLContext initialization, these credentials are
 * installed into a {@link KeyManagerFactory}, enabling certificate-based
 * server authentication.</p>
 *
 * <p>Encryption, decryption, handshake control, and transport-level data flow
 * (wrap/unwrap) are all handled by {@link OASslBase}. This class provides only
 * the server-specific SSLContext and SSLEngine initialization.</p>
 *
 * <p>A subclass must still supply a concrete transport implementation by
 * overriding {@link OASslBase#sendOutput(byte[], int, int, boolean)}.</p>
 */
public abstract class OASslServer extends OASslBase {

	/**
	 * Constructs an SSL server instance for the specified host and port. These
	 * values are used when creating the underlying {@link SSLEngine}.
	 *
	 * @param host hostname advertised/reported by the SSLEngine
	 * @param port port number associated with the SSL session
	 */
    public OASslServer(String host, int port) {
        super(host, port);
    }

    /**
     * Creates and initializes the server-side {@link SSLContext}. Loads the server
     * keystore (<code>sslserver.jks</code>) using the configured password
     * (<code>"vince1"</code>), installs the key material into a
     * {@link KeyManagerFactory}, and initializes the SSLContext with those key
     * managers.
     *
     * @return initialized SSLContext for server-side TLS
     * @throws Exception if the keystore cannot be loaded or if SSLContext
     *                   initialization fails
     */
    protected SSLContext createSSLContext() throws Exception {
        // 20171118
        SSLContext sslContext = SSLContext.getInstance("TLS");
// SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        //was: SSLContext sslContext = SSLContext.getInstance("SSLv3");

        KeyStore keystore = KeyStore.getInstance("JKS");

        // see keystore.txt 
        InputStream is = OASslServer.class.getResourceAsStream("sslserver.jks");
        if (is == null) throw new IOException("sslserver.jks not found");
        keystore.load(is, "vince1".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "vince1".toCharArray());

        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    /**
     * Creates and configures the server-side {@link SSLEngine}. The engine is:
     * <ul>
     *   <li>Created using the SSLContext for the specified host and port</li>
     *   <li>Placed into <strong>server mode</strong></li>
     *   <li>Configured to <strong>not</strong> require client authentication</li>
     * </ul>
     *
     * @return configured server-side SSLEngine
     * @throws Exception if the SSLContext is unavailable or engine creation fails
     */
    protected SSLEngine createSSLEngine() throws Exception {
        SSLEngine sslEngine = getSSLContext().createSSLEngine(host, port);
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        return sslEngine;
    }

    /**
     * Creates and configures the server-side {@link SSLEngine}. The engine is:
     * <ul>
     *   <li>Created using the SSLContext for the specified host and port</li>
     *   <li>Placed into <strong>server mode</strong></li>
     *   <li>Configured to <strong>not</strong> require client authentication</li>
     * </ul>
     *
     * @return configured server-side SSLEngine
     * @throws Exception if the SSLContext is unavailable or engine creation fails
     */
    @Override
    protected void log(String msg) {
        System.out.println("SERVER: "+msg);
    }

    /**
     * Simple diagnostic entry point used to examine the default and supported
     * cipher suites of the underlying JVM’s SSL implementation.
     *
     * <p>This method does not start an SSL server. It only retrieves the cipher
     * suite lists from the default {@link SSLServerSocketFactory}.</p>
     *
     * @param args ignored
     * @throws Exception if cipher suite introspection fails
     */
    public static void main(String[] args) throws Exception {
        SSLServerSocketFactory ssf = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();

        String[] defaultCiphers = ssf.getDefaultCipherSuites();
        String[] availableCiphers = ssf.getSupportedCipherSuites();
        int xx = 4;
        xx++;
    }

}
