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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSL client implementation built on {@link OASslBase}. This class configures
 * an {@link SSLContext} and {@link SSLEngine} suitable for client-side TLS
 * communication and provides certificate-based server authentication using a
 * local truststore.
 *
 * <p>The SSLContext is initialized from a bundled JKS file
 * (<code>sslclient.jks</code>), which contains trusted server certificates.
 * The resulting SSLEngine is configured for client mode and does not request
 * client authentication.</p>
 *
 * <p>Encryption, decryption, handshake coordination, and transport-level
 * output are performed by {@link OASslBase}.</p>
 */
public abstract class OASslClient extends OASslBase {

	/**
	 * Constructs an SSL client for the specified host and port. The host and port
	 * are used to initialize the SSLEngine once the SSLContext is created.
	 *
	 * @param host remote host for the SSL connection
	 * @param port remote port for the SSL connection
	 */
    public OASslClient(String host, int port) {
        super(host, port);
    }
    
    /**
     * Creates and initializes an {@link SSLContext} for TLS client use. Loads the
     * bundled truststore <code>sslclient.jks</code>, initializes a
     * {@link TrustManagerFactory}, and installs the resulting trust managers into
     * the SSLContext.
     *
     * @return initialized SSLContext
     * @throws Exception if the truststore cannot be loaded or if SSLContext
     *                   initialization fails
     */
    protected SSLContext createSSLContext() throws Exception {
        // 20171118
        SSLContext sslContext = SSLContext.getInstance("TLS");
// SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        //was: SSLContext sslContext = SSLContext.getInstance("SSLv3");

        KeyStore keystore = KeyStore.getInstance("JKS");

        // see keystore.txt 
        InputStream is = OASslClient.class.getResourceAsStream("sslclient.jks");
        if (is == null) throw new IOException("sslclient.jks not found");
        keystore.load(is, "vince1".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);

        TrustManager[] trustManagers = tmf.getTrustManagers();

        sslContext.init(null, trustManagers, null);
        return sslContext;
    }

    /**
     * Creates and configures the {@link SSLEngine} for client-side operation.
     * The engine is:
     * <ul>
     *   <li>Created from the SSLContext using the configured host and port</li>
     *   <li>Placed into client mode</li>
     *   <li>Configured not to request client authentication</li>
     * </ul>
     *
     * @return configured SSLEngine instance
     * @throws Exception if the SSLContext has not been created or the engine
     *                   initialization fails
     */
    protected SSLEngine createSSLEngine() throws Exception {
        SSLEngine sslEngine = getSSLContext().createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);
        sslEngine.setWantClientAuth(false);
        return sslEngine;
    }
    
    /**
     * Client-side logging hook. Writes messages to standard output prefixed with
     * <code>"CLIENT: "</code>.
     *
     * @param msg text to log
     */
    @Override
    protected void log(String msg) {
        System.out.println("CLIENT: "+msg);
    }
}
