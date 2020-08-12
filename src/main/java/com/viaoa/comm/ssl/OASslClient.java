/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
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
 * SSLClient used to encrypt data
 * @author vvia
 *
 */
public abstract class OASslClient extends OASslBase {

    public OASslClient(String host, int port) {
        super(host, port);
    }
    
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

    protected SSLEngine createSSLEngine() throws Exception {
        SSLEngine sslEngine = getSSLContext().createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);
        sslEngine.setWantClientAuth(false);
        return sslEngine;
    }
    @Override
    protected void log(String msg) {
        System.out.println("CLIENT: "+msg);
    }
}
