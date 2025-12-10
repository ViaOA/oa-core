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

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

/**
 * Base class for SSL client and server components that perform encrypted
 * communication using {@link SSLEngine}. This class abstracts the SSLContext
 * and SSLEngine setup, handshake coordination, encryption (wrap), and
 * decryption (unwrap), while delegating transport-specific behavior to
 * subclasses.
 *
 * <p>OASslBase provides:</p>
 * <ul>
 *   <li>Lazy creation of SSLContext and SSLEngine</li>
 *   <li>Preferred cipher selection</li>
 *   <li>Handshake coordination and state management</li>
 *   <li>Encryption via {@link SSLEngine#wrap(ByteBuffer, ByteBuffer)}</li>
 *   <li>Decryption via {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer)}</li>
 *   <li>Blocking input and output methods for SSL-secured channels</li>
 * </ul>
 *
 * <p>Subclasses supply:</p>
 * <ul>
 *   <li>Transport-specific delivery of encrypted bytes (sendOutput)</li>
 *   <li>Creation of the SSLContext</li>
 *   <li>Creation of the SSLEngine</li>
 * </ul>
 *
 * <p>The class manages handshake conditions such as NEED_WRAP and NEED_UNWRAP
 * by blocking and waking threads appropriately.</p>
 */
public abstract class OASslBase {
    private static Logger LOG = Logger.getLogger(OASslBase.class.getName());
    
    /**
     * List of preferred cipher suite names to be enabled on the SSLEngine.
     * These are applied during engine initialization.
     */
    public static final String[] PREFERRED_CIPHER_NAMES = new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256" }; 

    /** Lazily created SSLContext for this instance. */
    protected SSLContext sslContext;
    
    /** Lazily created SSLEngine used to encrypt and decrypt all traffic. */
    protected SSLEngine sslEngine;

    /**
     * Internal byte array used to hold encrypted bytes produced during SSL wrap
     * operations.
     */
    private byte[] bsWrap;

    /**
     * ByteBuffer wrapper over {@link #bsWrap} used during wrap() operations.
     */
    private ByteBuffer bbWrap;

    /**
     * Lock used to synchronize handshake-related WAIT/NOTIFY actions, primarily
     * when the SSLEngine requires inbound data (NEED_UNWRAP).
     */
    private final Object lock = new Object();

    /**
     * Empty placeholder buffer used when the SSLEngine requires a wrap operation
     * even though no application data is being sent (handshake-only).
     */
    private byte[] bsBlank;

    /**
     * Lock used to coordinate blocking reads. The input() method waits on this
     * lock until receiveInput(...) supplies decrypted data.
     */
    private final Object lockGetInput = new Object();

    /** Host value used when creating SSLContext or SSLEngine. */
    protected final String host;

    /** Port value used when creating SSLContext or SSLEngine. */
    protected final int port;

    /**
     * Creates a new SSL base instance for the specified host and port. These
     * values are used by subclasses when constructing the SSLContext and
     * SSLEngine.
     *
     * @param host hostname for SSL connection
     * @param port port associated with the SSL connection
     */
    public OASslBase(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Lazily creates and returns the SSLContext by delegating to
     * {@link #createSSLContext()}. Subsequent calls return the cached instance.
     *
     * @return SSLContext used for this secured connection
     * @throws Exception if SSLContext creation fails
     */
    protected SSLContext getSSLContext() throws Exception {
        if (sslContext == null) {
            sslContext = createSSLContext();
        }
        return sslContext;
    }

    /**
     * Lazily creates and configures the SSLEngine for this connection. Enables
     * the preferred cipher suites and begins the SSL handshake.
     *
     * @return initialized SSLEngine
     * @throws Exception if SSLEngine creation fails
     */
    protected SSLEngine getSSLEngine() throws Exception {
        if (sslEngine == null) {
            sslEngine = createSSLEngine();
            sslEngine.setEnabledCipherSuites(PREFERRED_CIPHER_NAMES);
            sslEngine.beginHandshake();
        }
        return sslEngine;
    }

    /**
     * Resets the SSL session by invalidating the engine's session and restarting
     * the handshake. Intended primarily for testing.
     *
     * @throws Exception if the handshake cannot be restarted
     */
    public void resetSSL() throws Exception {
        sslEngine.getSession().invalidate();
        sslEngine.beginHandshake();
    }

    /**
     * Forces the SSLEngine to be created and initialized. Not required, but
     * available for callers who want to explicitly initialize SSL state in
     * advance.
     *
     * @throws Exception if initialization fails
     */
    public void initialize() throws Exception {
        //log("initialize");
        getSSLEngine();
    }
    
    /**
     * Encrypts and sends application data to the peer. This method ensures that
     * all handshake requirements are satisfied before each wrap() call, including
     * yielding to unwrap() when the engine is waiting for inbound handshake data.
     *
     * @param bs source buffer containing application data
     * @param offset starting offset in the buffer
     * @param len number of bytes to send
     * @throws Exception if encryption or lower-level transport sending fails
     */
    public void output(final byte[] bs, final int offset, final int len) throws Exception {
        //log("ouput");
        getSSLEngine();
        int consumed = 0;
        for (;;) {
            for (;;) {
                needUnwrap();
                if (!needWrap()) break;
            }
            consumed += wrap(bs, offset + consumed, len - consumed, false);
            if (consumed >= len) break;
        }
    }

    /**
     * Waits while the SSLEngine's handshake requires inbound data
     * (HandshakeStatus.NEED_UNWRAP). The method blocks on {@link #lock} until
     * notified by an inbound unwrap performed inside {@link #input(byte[], int, boolean)}.
     *
     * @throws Exception if engine access fails
     */
    private void needUnwrap() throws Exception {
        for (int i=0;; i++) {
            synchronized (lock) {
                SSLEngineResult.HandshakeStatus hs = getSSLEngine().getHandshakeStatus();
                if (hs != hs.NEED_UNWRAP) break;
                try {
                    log("need_unwrap, i="+i);
                    lock.wait(250); // wait for input to unwrap
                }
                catch (Exception e) {
                }
            }
        }
    }

    /**
     * Checks whether the SSLEngine's handshake requires outbound data
     * (HandshakeStatus.NEED_WRAP). If so, performs an empty wrap using
     * a zero-length buffer and sends the resulting handshake bytes.
     *
     * @return true if a wrap was required and performed; false otherwise
     * @throws Exception if wrap fails
     */
    private boolean needWrap() throws Exception {
        SSLEngineResult.HandshakeStatus hs = getSSLEngine().getHandshakeStatus();
        if (hs != hs.NEED_WRAP) return false;
        log("need_wrap");
        if (bsBlank == null) bsBlank = new byte[0];
        wrap(bsBlank, 0, 0, true);
        return true;
    }

    /**
     * Encrypts the given plaintext bytes and forwards the resulting ciphertext to
     * {@link #sendOutput(byte[], int, int, boolean)}. Handles:
     * <ul>
     *   <li>Dynamic buffer resizing for BUFFER_OVERFLOW conditions</li>
     *   <li>Delegated tasks required by the SSLEngine</li>
     *   <li>Handshake-only wrapping when len == 0</li>
     * </ul>
     *
     * @param bs plaintext buffer
     * @param offset offset into plaintext buffer
     * @param len number of bytes to encrypt
     * @param bHandshakeOnly true if wrapping only for handshake progress
     * @return number of plaintext bytes consumed
     * @throws Exception if encryption or delegated tasks fail
     */
    private int wrap(final byte[] bs, final int offset, final int len, final boolean bHandshakeOnly) throws Exception {
        // log("wrap");
        int consumed = 0;
        for (;;) {
            if (bsWrap == null) {
                int max = getSSLEngine().getSession().getPacketBufferSize();
                bsWrap = new byte[max];
                bbWrap = ByteBuffer.wrap(bsWrap, 0, max);
            }
            else bbWrap.clear();

            ByteBuffer bb = ByteBuffer.wrap(bs, offset, len);
            SSLEngineResult result = getSSLEngine().wrap(bb, bbWrap);

            if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                bsWrap = new byte[bsWrap.length + 1024];
                bbWrap = ByteBuffer.wrap(bsWrap, 0, bsWrap.length);
                continue;
            }

            if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = sslEngine.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            consumed = result.bytesConsumed();
            if (result.bytesProduced() > 0) {
                sendOutput(bsWrap, 0, result.bytesProduced(), bHandshakeOnly);
            }
            break;
        }
        return consumed;
    }

    /**
     * Processes inbound encrypted bytes during SSL communication. Decrypts them
     * using {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer)} and, for application
     * data, forwards the plaintext to {@link #receiveInput(byte[], int, int)}.
     *
     * <p>Also performs delegated tasks and notifies any threads waiting on
     * handshake progress.</p>
     *
     * @param bs buffer containing encrypted bytes
     * @param len number of encrypted bytes available
     * @param bHandshakeOnly whether data is being processed exclusively for handshake
     * @throws Exception if unwrap fails or a protocol error occurs
     */
    protected void input(final byte[] bs, final int len, final boolean bHandshakeOnly) throws Exception {
        //log("input");
        ByteBuffer bb = ByteBuffer.wrap(bs, 0, len);

        // this will use the same buffer to unwrap the data. This assumes that the unwrapped data is <=
        // the encrypted data.
        ByteBuffer bb2 = ByteBuffer.wrap(bs, 0, bs.length);

        synchronized (lock) {
            SSLEngineResult result = getSSLEngine().unwrap(bb, bb2);
            switch (result.getStatus()) {
            case BUFFER_OVERFLOW: // should never happen for unwrap
                throw new SSLException("Buffer_Overflow, should not happen for an unwrap");
            case BUFFER_UNDERFLOW: // not enough data to do SSL, should never happen for unwrap: since
                                   // we make sure all data is in buffer
                throw new SSLException("Buffer_Underflow, should not happen for an unwrap");
            }

            if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = getSSLEngine().getDelegatedTask()) != null) {
                    runnable.run();
                }
            }
            if (!bHandshakeOnly) {
                receiveInput(bs, 0, result.bytesProduced());
            }
            lock.notifyAll();
        }
    }

    /**
     * Creates and returns the SSLContext appropriate for client or server use.
     *
     * @return SSLContext instance
     * @throws Exception if SSLContext creation fails
     */
    protected abstract SSLContext createSSLContext() throws Exception;

    /**
     * Creates and returns the SSLEngine for this SSL connection. Subclasses may
     * configure client/server mode or hostname settings.
     *
     * @return SSLEngine instance
     * @throws Exception if SSLEngine creation fails
     */
    protected abstract SSLEngine createSSLEngine() throws Exception;
    
    
    /**
     * Sends encrypted output bytes to the peer. Subclasses implement the
     * mechanism (e.g., socket write, multiplexer forwarding, etc.).
     *
     * @param bs buffer containing ciphertext
     * @param offset offset into ciphertext buffer
     * @param len number of bytes to send
     * @param bHandshakeOnly true if sending handshake-only bytes
     * @throws Exception if sending fails
     */
    protected abstract void sendOutput(final byte[] bs, final int offset, final int len, final boolean bHandshakeOnly) throws Exception;

    /**
     * Temporary buffer holding the latest decrypted data returned to input().
     */
    private byte[] bsGetInput;

    /**
     * Called after decrypting inbound SSL data. Stores the plaintext in an
     * internal buffer and wakes any thread blocked inside {@link #input()}.
     *
     * @param bs decrypted bytes
     * @param offset offset into decrypted bytes
     * @param len number of decrypted bytes
     * @throws Exception never thrown in this implementation
     */
    protected void receiveInput(final byte[] bs, final int offset, final int len) throws Exception {
        //log("receiveInput");
        synchronized (lockGetInput) {
            bsGetInput = new byte[len];
            if (len > 0) System.arraycopy(bs, offset, bsGetInput, 0, len);
            lockGetInput.notifyAll();
        }
    }

    /**
     * Performs a blocking read of decrypted application data. This method waits
     * until a preceding call to {@link #receiveInput(byte[], int, int)} provides
     * unencrypted bytes.
     *
     * <p>Handshake progress is managed via needUnwrap()/needWrap(), ensuring that
     * all handshake states are satisfied before waiting for application data.</p>
     *
     * @return decrypted application bytes
     * @throws Exception if handshake fails or receiving fails
     */
    public byte[] input() throws Exception {
        //log("input");
        int consumed = 0;

        byte[] bs;
        for (;;) {
            for (;;) {
                needUnwrap();
                if (!needWrap()) break;
            }
            synchronized (lockGetInput) {
                if (bsGetInput != null) {
                    bs = bsGetInput;
                    bsGetInput = null;
                    break;
                }
                try {
                    lockGetInput.wait(500);
                }
                catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
        return bs;
    }
    
    /**
     * Simple logging hook. Prints the message to stdout. Subclasses may override
     * to provide alternate logging behavior.
     *
     * @param msg text to log
     */
    protected void log(String msg) {
        System.out.println(msg);
    }
}
