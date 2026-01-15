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
package com.viaoa.remote.multiplexer;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.comm.multiplexer.io.VirtualSocket;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.BindInfo;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OACompressWrapper;
import com.viaoa.util.OAPool;
import com.viaoa.util.OAReflect;
import com.viaoa.util.Tuple;


/**
 * Client-side implementation for OA's remote-method invocation (RMI) layer
 * when running over the Multiplexer communication system.
 *
 * <p>This class manages:
 * <ul>
 *   <li>Establishing virtual socket connections to the server
 *   <li>Looking up remote objects exposed by the server
 *   <li>Creating client-side proxy instances for remote objects
 *   <li>Sending method calls to the server using either
 *       synchronous socket requests or asynchronous queue-based delivery
 *   <li>Receiving callbacks from server-to-client remote objects
 * </ul>
 *
 * <p>The client integrates with:
 * <ul>
 *   <li>{@link com.viaoa.comm.multiplexer.OAMultiplexerClient}</li>
 *   <li>{@link com.viaoa.remote.multiplexer.io.RemoteObjectInputStream}</li>
 *   <li>{@link com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream}</li>
 *   <li>{@link com.viaoa.remote.info.RequestInfo} message formats</li>
 * </ul>
 *
 * <p>Key responsibilities:
 * <ol>
 *   <li>Maintain sessions and virtual sockets for Client→Server and Server→Client communication</li>
 *   <li>Manage BindInfo lookups and remote-interface metadata</li>
 *   <li>Create client-side remote proxies using Java dynamic proxies</li>
 *   <li>Handle asynchronous invoke-return patterns using circular queues</li>
 *   <li>Support broadcast remoting where a single call fans out to all servers</li>
 * </ol>
 *
 * <p>Debugging:
 * Remote calls respect OAObject.getDebugMode(), disabling timeouts so deep
 * debugging does not interrupt or kill remote method calls.
 *
 * <p>This is the core class that allows OA applications to
 * transparently call server-side services using standard Java interfaces
 * without requiring a heavyweight RPC framework.
 *
 * @author vvia
 */
public class OARemoteMultiplexerClient {
	private static Logger LOG = Logger.getLogger(OARemoteMultiplexerClient.class.getName());

	/**
	 * Underlying multiplexer client used as the transport layer for creating and
	 * managing virtual sockets to the server.
	 */
	private OAMultiplexerClient multiplexerClient;

	/**
	 * Cache of previously looked-up remote objects keyed by their lookup name so
	 * that subsequent calls can reuse existing proxy instances.
	 */
	private final ConcurrentHashMap<String, Object> hmLookup = new ConcurrentHashMap<String, Object>();

	/**
	 * Counter used to generate unique bind names for client-side objects that are
	 * exposed to the server.
	 */
	private final AtomicInteger aiBindCount = new AtomicInteger();

	/**
	 * Pool that manages reusable client-to-server {@link VirtualSocket} instances
	 * for issuing remote requests.
	 */
	private OAPool<VirtualSocket> poolVirtualSocketCtoS;

	/**
	 * Mapping from bind name to {@link BindInfo} containing metadata and weak
	 * references for each registered remote object.
	 */
	private final ConcurrentHashMap<String, BindInfo> hmNameToBind = new ConcurrentHashMap<String, BindInfo>();

	/**
	 * Reference queue used to detect when remote objects have been garbage
	 * collected so their associated bindings can be cleaned up.
	 */
	private final  ReferenceQueue referenceQueue = new ReferenceQueue();

	/**
	 * Cache of class descriptors received from the server, keyed by an integer
	 * identifier, to optimize deserialization of remote messages.
	 */
	private final ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDescInput = new ConcurrentHashMap<Integer, ObjectStreamClass>();

	/**
	 * Cache of class descriptors sent to the server, keyed by class name and
	 * mapped to an integer identifier, to optimize serialization overhead.
	 */
	private final ConcurrentHashMap<String, Integer> hmClassDescOutput = new ConcurrentHashMap<String, Integer>();
	
	/**
	 * Counter used to assign unique integer identifiers for class descriptors
	 * written to the output stream.
	 */
	private final AtomicInteger aiClassDescOutput = new AtomicInteger();

	/**
	 * Registry of in-flight asynchronous requests keyed by message id so that
	 * responses from the server can be matched back to their original calls.
	 */
	private final ConcurrentHashMap<Integer, RequestInfo> hmAsyncRequestInfo = new ConcurrentHashMap<Integer, RequestInfo>();
	
	/**
	 * Counter used to generate unique message identifiers for client-to-server
	 * requests.
	 */
	private final AtomicInteger aiMessageId = new AtomicInteger();

	/**
	 * Cache of client-to-server proxy instances keyed by bind or lookup name to
	 * ensure a single proxy per remote interface on this client.
	 */
    private final ConcurrentHashMap<String, Object> hmProxyCtoS = new ConcurrentHashMap<String, Object>();
    
    /**
     * Cache of broadcast proxy instances keyed by broadcast name so that
     * broadcast-capable remote interfaces can be reused.
     */
    private ConcurrentHashMap<String, Object> hmProxyBroadcast = new ConcurrentHashMap<String, Object>();

	
    /**
     * Constructs a new remote multiplexer client using the supplied
     * {@link OAMultiplexerClient} as the transport layer and initializes the
     * background threads required for processing remote requests and callbacks.
     *
     * @param multiplexerClient the underlying multiplexer client used to create
     *                          and manage virtual sockets; must not be {@code null}
     * @throws IllegalArgumentException if {@code multiplexerClient} is {@code null}
     */
	public OARemoteMultiplexerClient(OAMultiplexerClient multiplexerClient) {
		LOG.fine("new multiplexer client");
		if (multiplexerClient == null) {
			throw new IllegalArgumentException("multiplexerClient is required");
		}
		this.multiplexerClient = multiplexerClient;
		setupSyncRunnableQueueThread();
		setupSyncRequestQueueThread();
		setupRequestQueueThread();
	}

	/**
	 * Returns the underlying multiplexer client used by this remote client.
	 *
	 * @return the {@link OAMultiplexerClient} instance used for socket creation
	 *         and connectivity checks
	 */
	public OAMultiplexerClient getMultiplexerClient() {
		return multiplexerClient;
	}

	/**
	 * Flag indicating whether this client has been closed, used to signal worker
	 * threads to stop processing.
	 */
	private volatile boolean bClosed;

	/**
	 * Marks this client as closed so that background worker threads will stop
	 * polling and processing further requests.
	 */
	public void close() {
		bClosed = true;
	}

	/**
	 * Ensures the client is closed during garbage collection by invoking
	 * {@link #close()} before delegating to {@code super.finalize()}.
	 *
	 * @throws Throwable if the superclass finalizer throws an exception
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/**
	 * Registers a local callback object to receive broadcast invocations from the
	 * server for the specified lookup name.
	 *
	 * @param lookupName the broadcast name used by the server to identify this
	 *                   callback registration; must not be {@code null}
	 * @param callback   the local implementation that will receive broadcast
	 *                   method calls; must not be {@code null}
	 * @throws Exception if the broadcast lookup fails or the callback type is
	 *                   incompatible with the server-side interface
	 */
	public void registerBroadcast(final String lookupName, Object callback) throws Exception {
		lookupBroadcast(lookupName, callback);
	}

	public Object lookupBroadcast(final String lookupName, Object callback) throws Exception {
		if (lookupName == null) {
			throw new IllegalArgumentException("lookupName cant be null");
		}
		if (callback == null) {
			throw new IllegalArgumentException("callback cant be null");
		}
		Object proxyInstance = hmLookup.get(lookupName);
		if (proxyInstance != null) {
			return proxyInstance;
		}
		LOG.fine("lookupName=" + lookupName);

		VirtualSocket socket = getSocketForCtoS();
		RemoteObjectOutputStream oos = new RemoteObjectOutputStream(socket, hmClassDescOutput, aiClassDescOutput);

		oos.writeByte(RequestInfo.Type.CtoS_GetBroadcastClass.ordinal());
		oos.writeAsciiString(lookupName);
		oos.flush();
		oos.close(); // 20250318

		RemoteObjectInputStream ois = new RemoteObjectInputStream(socket, hmClassDescInput);
		Exception ex = null;
		Class c = null;
		if (!ois.readBoolean()) {
			ex = (Exception) ois.readObject();
		} else {
			c = (Class) ois.readObject();
		}
		ois.close(); // 20250318
		releaseSocketForCtoS(socket);
		LOG.fine("lookupName=" + lookupName + ", interface class=" + c);
		if (ex != null) {
			throw ex;
		}

		if (!c.isAssignableFrom(callback.getClass())) {
			throw new Exception("callback must be same class as " + c);
		}

		proxyInstance = getProxyForBroadcast(lookupName, c, callback);
		hmLookup.put(lookupName, proxyInstance);
		return proxyInstance;
	}

	/**
	 * Looks up a remote object from the server using the specified lookup name.
	 * Results are cached so repeated lookups for the same name return the same
	 * proxy instance.
	 *
	 * @param lookupName the server-side name of the remote object to resolve;
	 *                   must not be {@code null}
	 * @return the proxy object representing the remote interface
	 * @throws Exception if the lookup request fails or the server returns an error
	 */
	public Object lookup(String lookupName) throws Exception {
		LOG.fine("lookupName=" + lookupName);
		if (lookupName == null) {
			return null;
		}
		Object proxyInstance = hmLookup.get(lookupName);
		if (proxyInstance != null) {
			return proxyInstance;
		}

		VirtualSocket socket = getSocketForCtoS();
		RemoteObjectOutputStream oos = new RemoteObjectOutputStream(socket, hmClassDescOutput, aiClassDescOutput);

		// 20130601 changed from boolean to byte
		oos.writeByte(RequestInfo.Type.CtoS_GetLookupInfo.ordinal());
		oos.writeAsciiString(lookupName);
		oos.flush();
        oos.close();

		RemoteObjectInputStream ois = new RemoteObjectInputStream(socket, hmClassDescInput);
		if (!ois.readBoolean()) {
		    ois.close();
			Exception ex = new Exception((String) ois.readObject());
			throw ex;
		}
		Object[] objs = (Object[]) ois.readObject();
        ois.close();
		Class c = (Class) objs[0];
		boolean bUsesQueue = (Boolean) objs[1];
		boolean bIsBroadcast = (Boolean) objs[2];
		if (bIsBroadcast) {
			throw new Exception("must use lookupBroadcast() for " + lookupName + ", instead of lookup()");
		}

		releaseSocketForCtoS(socket);
		LOG.fine("lookupName=" + lookupName + ", interface class=" + c);

		if (c != null) {
			proxyInstance = getProxyForCtoS(lookupName, c, bUsesQueue);
			hmLookup.put(lookupName, proxyInstance);
		}
		return proxyInstance;
	}

	/**
	 * Returns the underlying {@link Socket} used by this client for low-level
	 * communication with the remote multiplexer server.
	 *
	 * @return the active socket instance, or {@code null} if the client is not connected
	 */
	public Socket getSocket() {
		return multiplexerClient.getSocket();
	}

	/**
	 * Creates a unique bind name for a client-side remote object being
	 * exposed to the server. The name is composed of:
	 * <ul>
	 *   <li>A prefix identifying the client ("C.")</li>
	 *   <li>The connection id of the socket used for the request</li>
	 *   <li>A monotonically increasing counter used to ensure uniqueness</li>
	 * </ul>
	 *
	 * This bind name is later used by the server to reference the
	 * callback object on the client.
	 *
	 * @param ri the request information containing the socket and connection id;
	 *           must not be {@code null}
	 * @return a unique bind name for this client/session
	 */
	protected String createBindName(RequestInfo ri) {
		String bindName = "C." + ri.socket.getConnectionId() + "." + aiBindCount.incrementAndGet();
		return bindName;
	}

	
	/**
	 * Creates or retrieves a proxy instance used for client-to-server remote
	 * method calls. This overload determines whether queue-based messaging
	 * should be used based on the calling request and method metadata.
	 *
	 * The method delegates to the main {@link #getProxyForCtoS(String, Class, boolean)}
	 * implementation after resolving the correct queue usage rules.
	 *
	 * @param ri            request information for the current invocation
	 * @param name          the bind or lookup name used to identify the remote reference
	 * @param c             the remote interface class
	 * @param bDontUseQueue if true, socket calls are forced instead of queued calls
	 * @return the proxy implementing the remote interface
	 * @throws Exception if proxy creation fails
	 */
	protected Object getProxyForCtoS(RequestInfo ri, String name, Class c, boolean bDontUseQueue) throws Exception {
		return getProxyForCtoS(name, c, (ri.bind.usesQueue && !bDontUseQueue));
	}

	/**
	 * Creates or returns a cached client-side proxy for a remote server interface.
	 * The proxy uses a dynamic {@link InvocationHandler} to route method calls
	 * through {@link #onInvokeForCtoS(BindInfo, Object, Method, Object[])}.
	 *
	 * If an existing proxy is already cached for the name, it is reused.
	 * Otherwise, a new proxy is created, registered in bind metadata,
	 * and (if required) ensures that the first StoC socket is created
	 * for receiving callbacks.
	 *
	 * @param name       the lookup or bind name identifying the remote interface
	 * @param c          the interface class implemented by the remote object
	 * @param bUsesQueue true to route calls using the asynchronous queue
	 * @return the proxy instance
	 * @throws Exception if proxy creation or bind setup fails
	 */
	protected Object getProxyForCtoS(String name, Class c, boolean bUsesQueue) throws Exception {
		if (name == null) {
			return null;
		}
		Object proxy = hmProxyCtoS.get(name);
		if (proxy != null) {
			return proxy;
		}

		final BindInfo bind = getBindInfo(name, null, c, bUsesQueue, false);
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Object result = OARemoteMultiplexerClient.this.onInvokeForCtoS(bind, proxy, method, args);
				return result;
			}
		};
		proxy = Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, handler);
		hmProxyCtoS.put(name, proxy);
		bind.setObject(proxy, referenceQueue);

		if (bind.usesQueue && bUsesQueue) {
			if (!bFirstStoCsocketCreated) {
				createSocketForStoC(); // to process message from server to this object
			}
		}

		LOG.fine("Created proxy instance, class=" + c + ", name=" + name);
		return proxy;
	}

	/**
	 * Creates or retrieves a proxy for broadcast-capable remote interfaces.
	 * Broadcast proxies forward method calls to the server, which then
	 * fan-out to all registered listeners.
	 *
	 * A callback object is registered as the client-side implementation
	 * for receiving broadcast calls initiated by the server.
	 *
	 * @param name      the broadcast lookup identifier
	 * @param c         the remote broadcast interface class
	 * @param callback  client-side receiver for incoming broadcast calls
	 * @return a proxy instance used to send broadcast operations
	 * @throws Exception if proxy creation fails or binding cannot be established
	 */
	protected Object getProxyForBroadcast(String name, Class c, Object callback) throws Exception {
		if (name == null) {
			return null;
		}
		Object proxy = hmProxyBroadcast.get(name);
		if (proxy != null) {
			return proxy;
		}

		final BindInfo bind = getBindInfo(name, callback, c, true, true);
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Object result = OARemoteMultiplexerClient.this.onInvokeForCtoS(bind, proxy, method, args);
				return result;
			}
		};
		proxy = Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, handler);
		hmProxyBroadcast.put(name, proxy);

		if (!bFirstStoCsocketCreated) {
			createSocketForStoC(); // to process message from server to this object
		}
		LOG.fine("Created proxy instance, class=" + c + ", name=" + name);
		return proxy;
	}

	/**
	 * Handles a method invocation made on a client-side proxy and converts it
	 * into a remote request sent to the server. This method initializes the
	 * {@link RequestInfo}, configures timeout and metadata, chooses the correct
	 * message type, optionally waits for a response, and processes return values.
	 *
	 * Errors and timeout conditions are captured in the {@link RequestInfo}.
	 *
	 * @param bind   metadata describing the remote binding
	 * @param proxy  the proxy object on which the method was invoked
	 * @param method the Java method being invoked
	 * @param args   the invocation arguments (may be null)
	 * @return the return value from the server, or null if void
	 * @throws Throwable if a remote or local error occurs
	 */
	protected Object onInvokeForCtoS(BindInfo bind, Object proxy, Method method, Object[] args) throws Throwable {
		//LOG.fine(method.getName());
		aiMethodCallCnt.incrementAndGet();

		RequestInfo ri = new RequestInfo();
		// 1:CtoS_QueuedRequest start
		// 1:CtoS_QueuedRequestNoResponse

		if (Thread.currentThread() instanceof OARemoteThread) {
			//if (threadCheck++ < 50) LOG.log(Level.WARNING, "Info only: bind="+bind.name+", method="+method.getName(), new Exception("RemoteThread used for CtoS method call"));
		}

		VirtualSocket socket = getSocketForCtoS(); // used to send message
		try {
			ri.msStart = System.currentTimeMillis();
			ri.nsStart = System.nanoTime();
			ri.socket = socket;
			ri.connectionId = socket.getConnectionId();
			ri.messageId = aiMessageId.incrementAndGet();
			ri.vsocketId = socket.getId();
			ri.object = proxy;
			ri.bind = bind;
			ri.bindName = bind.name;
			ri.method = method;
			ri.args = args;
			ri.methodInfo = ri.bind.getMethodInfo(ri.method);
			if (ri.methodInfo != null) {
				ri.methodNameSignature = ri.methodInfo.methodNameSignature;
				ri.socket.setTimeoutSeconds(ri.methodInfo.timeoutSeconds);
			}
			ri.isRemoteThread = (Thread.currentThread() instanceof OARemoteThread);

			ri.bSent = _onInvokeForCtoS(ri);

			// 4:CtoS_QueuedRequestNoResponse END

			if (ri.bSent && (ri.bind.usesQueue && (ri.type.hasReturnValue() || ri.bind.isOASync))) {
				releaseSocketForCtoS(socket);
				socket = null;
				// 4:CtoS_QueuedRequest wait on return value from server
				synchronized (ri) {
					for (int i = 0;; i++) {
						if (ri.methodInvoked) {
							break;
						}
						if (i > 0) {
							if (!multiplexerClient.isConnected()) {
								break;
							}
							if (ri.methodInfo.timeoutSeconds > 0 && i >= ri.methodInfo.timeoutSeconds) {
								if (!OAObject.getDebugMode()) {
									break;
								}
							}
						}
						// if (i>5) System.out.println(i+" CLIENT IS Waiting on REQUEST TO RETURN "+ri.toLogString());						
						ri.wait(1000); // request timeout
					}
				}
				// 7:CtoS_QueuedRequest END
				if (!ri.methodInvoked) {
					if (!multiplexerClient.isConnected()) {
						ri.exceptionMessage = "socket disconnected";
					} else {
						ri.exceptionMessage = "timeout waiting on response from server";
					}
				} else {
					// 20160122 queue thread will wait for OARemoteThreadDelegate.startNextThread()
					//    to call OARuntime.get().threadLocals().notifyWaitingThread(), and wake up que thread waiting on ri lock
					if (ri.bind.isOASync) {
						OARuntime.get().threadLocals().setNotifyObject(ri);
					}
				}
			}
		} catch (Exception e) {
			ri.exception = e;
		} finally {
			ri.nsEnd = System.nanoTime();
			if (socket != null) {
				releaseSocketForCtoS(socket);
			}
		}
		afterInvokeForCtoS(ri);

		if (ri.exception != null) {
			throw ri.exception;
		}
		if (ri.exceptionMessage != null) {
			Exception ex = new Exception(ri.exceptionMessage + ", info: " + ri.toLogString());
			throw ex;
		}
		return ri.response;
	}

	/**
	 * Called after a client-to-server invocation completes. This logs warnings
	 * for exceptions or error messages contained in the {@link RequestInfo}.
	 *
	 * @param ri the request information for the completed invocation
	 */
	protected void afterInvokeForCtoS(RequestInfo ri) {
		if (ri == null || !ri.bSent) {
			return;
		}
		if (ri.exception != null || ri.exceptionMessage != null) {
			LOG.log(Level.WARNING, ri.toLogString(), ri.exception);
		}
	}

	/**
	 * Placeholder object used when invoking methods inherited from {@link Object}
	 * on proxy instances. This allows the client to locally simulate calls such as
	 * {@code toString()}, {@code hashCode()}, and {@code equals(Object)} without
	 * routing them through the remote invocation layer.
	 *
	 * <p>This avoids unnecessary remote calls for methods that have no meaning
	 * in the context of remote proxies and ensures consistent local behavior.
	 */
	private final Object stuntObject = new Object();

	/**
	 * Counter used to track the number of times a remote invocation attempt fails
	 * due to unsafe thread conditions as detected by
	 * {@link com.viaoa.remote.OARemoteThreadDelegate#isSafeToCallRemoteMethod()}.
	 *
	 * <p>The value is used for throttling and diagnostic logging to prevent
	 * excessive warning output when repeated unsafe-call scenarios occur.
	 */
	private int errorCnt;

	/**
	 * Implements the low-level send-logic for a client-to-server invocation.
	 * This performs:
	 * <ul>
	 *   <li>Argument compression</li>
	 *   <li>Remote-object parameter mapping</li>
	 *   <li>Message type determination</li>
	 *   <li>Asynchronous request registration</li>
	 *   <li>Socket-based or queue-based message transmission</li>
	 *   <li>Reading socket-based return values</li>
	 * </ul>
	 *
	 * The method updates the {@link RequestInfo} with the final state,
	 * exceptions, and response value.
	 *
	 * @param ri the request being transmitted
	 * @return true if the message was sent to the server
	 * @throws Exception if transmission fails
	 */
	protected boolean _onInvokeForCtoS(RequestInfo ri) throws Exception {
		if (ri.methodInfo == null) {
			// check to see if method from Object.class is being invoked
			if (ri.method.getDeclaringClass().equals(Object.class)) {
				if ("equals".equals(ri.method.getName())) {
					if (ri.args == null || ri.args.length != 1) {
						ri.response = false;
					} else {
						ri.response = (ri.args[0] == ri.object);
					}
				} else {
					try {
						OARuntime.get().threadLocals().setRemoteRequestInfo(ri);
						ri.response = ri.method.invoke(stuntObject, ri.args);
					} catch (InvocationTargetException e) {
						Exception ex = e;
						for (int i = 0; i < 10; i++) {
							Throwable t = ex.getCause();
							if (t == null || t == ex || !(t instanceof Exception)) {
								ri.exception = ex;
								break;
							}
							ex = (Exception) t;
							ri.exception = ex;
						}
					}
					OARuntime.get().threadLocals().setRemoteRequestInfo(null);
				}
			} else {
				ri.exceptionMessage = "Method not found in Methods";
			}
			return false;
		}

		// check if remoteThread, and if it has already processed it's msg before calling remote method
		if (!OARemoteThreadDelegate.isSafeToCallRemoteMethod()) {
			if (errorCnt++ < 25 || (errorCnt % 100 == 0)) {
				//Exception e = new Exception("isSafeToCallRemoteMethod is false");
				//LOG.log(Level.WARNING, "note: isSafeToCallRemoteMethod is false, will continue, starting another OARemoteThread", e);
			}
			OARemoteThreadDelegate.startNextThread();
		}

		// compress flagged arguments
		if (ri.methodInfo.compressedParams != null && ri.args != null) {
			for (int i = 0; i < ri.methodInfo.compressedParams.length && i < ri.args.length; i++) {
				if (ri.methodInfo.remoteParams != null && ri.methodInfo.remoteParams[i] != null) {
					continue;
				}
				if (ri.methodInfo.compressedParams[i]) {
					ri.args[i] = new OACompressWrapper(ri.args[i]);
				}
			}
		}

		// check to see if any of the args[] are remote objects, that will have
		// the server call the methods on this client.
		if (ri.methodInfo.remoteParams != null && ri.args != null) {
			for (int i = 0; i < ri.methodInfo.remoteParams.length && i < ri.args.length; i++) {
				if (ri.methodInfo.remoteParams[i] == null) {
					continue;
				}
				if (ri.args[i] == null) {
					continue;
				}

				BindInfo bindx = getBindInfoForObject((Object) ri.args[i]);
				Object objx = bindx != null ? bindx.weakRef.get() : null;
				if (bindx == null || objx == null) {

					boolean b = ri.methodInfo.dontUseQueues != null && ri.methodInfo.dontUseQueues[i];
					bindx = getBindInfo(ri, createBindName(ri), ri.args[i], ri.methodInfo.remoteParams[i], b);
					if (!bFirstStoCsocketCreated) {
						createSocketForStoC(); // to process message from server to this object
					}
				}
				ri.args[i] = bindx.name;
			}
		}

		RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, hmClassDescOutput, aiClassDescOutput);

		// set the correct type of message that this will be, which determines how it will be handled.
		if (ri.bind.usesQueue && ri.methodInfo.returnOnQueueSocket) {
			ri.type = RequestInfo.Type.CtoS_ReturnOnQueueSocket;
		} else if (ri.bind.usesQueue && !ri.methodInfo.dontUseQueue) {
			if (ri.bind.isBroadcast) {
				ri.type = RequestInfo.Type.CtoS_QueuedBroadcast;
			} else if (ri.methodInfo != null && ri.methodInfo.noReturnValue) {
				// 2:CtoS_QueuedRequestNoResponse
				ri.type = RequestInfo.Type.CtoS_QueuedRequestNoResponse;
			} else {
				// 2:CtoS_QueuedRequest send to server
				ri.type = RequestInfo.Type.CtoS_QueuedRequest;
			}
		} else {
			if (ri.methodInfo != null && ri.methodInfo.noReturnValue) {
				ri.type = RequestInfo.Type.CtoS_SocketRequestNoResponse;
			} else {
				ri.type = RequestInfo.Type.CtoS_SocketRequest;
			}
		}

		if (ri.type.usesQueue() && (ri.type.hasReturnValue() || ri.bind.isOASync)) {
			// 3:CtoS_QueuedRequest put in hm to wait on server response
			hmAsyncRequestInfo.put(ri.messageId, ri); // used to wait for server to send it back on StoC
			if (!bFirstStoCsocketCreated) {
				createSocketForStoC(); // to process message from server to this object
			}
		} else if (!ri.type.hasReturnValue()) {
			// 3:CtoS_QueuedRequestNoResponse
			ri.response = OAReflect.getEmptyPrimitive(ri.method.getReturnType());
		}

		oos.writeByte(ri.type.ordinal());
		oos.writeAsciiString(ri.bind.name);
		oos.writeAsciiString(ri.methodNameSignature);
		oos.writeObject(ri.args);

		if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
			oos.writeInt(ri.connectionId);
			oos.writeInt(ri.messageId);
		} else if (ri.type.usesQueue() && (ri.type.hasReturnValue() || ri.bind.isOASync)) {
			oos.writeInt(ri.messageId);
		}
		oos.flush();
        oos.close(); // 20250318

		if (ri.type == RequestInfo.Type.CtoS_SocketRequest) {
			RemoteObjectInputStream ois = new RemoteObjectInputStream(ri.socket, hmClassDescInput);
			int x = ois.readByte();
			if (x == 0) {
				ri.exception = (Exception) ois.readObject();
			} else if (x == 1) {
				ri.exceptionMessage = (String) ois.readObject();
			} else if (x == 2) {
				Object[] responses = (Object[]) ois.readObject();
				String bindName = (String) responses[0];

				BindInfo bindx = getBindInfo(bindName);
				Object objx = bindx != null ? bindx.weakRef.get() : null;
				if (bindx == null || objx == null) {
					boolean bUsesQueue = (Boolean) responses[1];
					Object obj = getProxyForCtoS(bindName, ri.methodInfo.remoteReturn, bUsesQueue);
					ri.response = obj;
				} else {
					ri.response = bindx.getObject();
				}
			} else {
				ri.response = ois.readObject();
				if (ri.response != null && ri.methodInfo.compressedReturn && ri.methodInfo.remoteReturn == null) {
					ri.response = ((OACompressWrapper) ri.response).getObject();
				}
			}
			ri.methodInvoked = true;
            ois.close(); // 20250318
		}
		return true;
	}

	/**
	 * Sets the minimum number of virtual sockets maintained in the
	 * client-to-server socket pool.
	 *
	 * @param x the minimum pool size
	 */
	public void setMinimumSocketsForCtoS(int x) {
		getVirtualSocketCtoSPool().setMinimum(x);
	}

	/**
	 * Returns the minimum size configured for the client-to-server socket pool.
	 *
	 * @return the minimum number of maintained sockets
	 */
	public int getMinimumSocketsForCtoS() {
		if (poolVirtualSocketCtoS == null) {
			return 1;
		}
		return getVirtualSocketCtoSPool().getMinimum();
	}

	/**
	 * Sets the maximum number of virtual sockets the client-to-server pool
	 * may allocate.
	 *
	 * @param x the maximum pool size
	 */
	public void setMaximumSocketsForCtoS(int x) {
		getVirtualSocketCtoSPool().setMaximum(x);
	}

	/**
	 * Returns the maximum number of sockets allowed in the client-to-server pool.
	 *
	 * @return the maximum socket count, or zero if the pool is not initialized
	 */
	public int getMaximumSocketsForCtoS() {
		if (poolVirtualSocketCtoS == null) {
			return 0;
		}
		return getVirtualSocketCtoSPool().getMaximum();
	}

	/**
	 * Lazily initializes and returns the pool managing client-to-server
	 * {@link VirtualSocket} instances. The pool provides configurable min/max
	 * sizes and closes sockets when removed.
	 *
	 * @return the socket pool instance
	 */
	protected OAPool<VirtualSocket> getVirtualSocketCtoSPool() {
		if (poolVirtualSocketCtoS != null) {
			return poolVirtualSocketCtoS;
		}
		poolVirtualSocketCtoS = new OAPool<VirtualSocket>(getMinimumSocketsForCtoS(), getMaximumSocketsForCtoS()) {
			@Override
			protected void removed(VirtualSocket vs) {
				try {
					vs.close();
				} catch (Exception e) {
					throw new RuntimeException("Error while closing vsocket", e);
				}
			}

			@Override
			protected VirtualSocket create() {
				VirtualSocket vs = null;
				try {
					vs = multiplexerClient.createSocket("CtoS");
				} catch (Exception e) {
					throw new RuntimeException("Error while creating a new vsocket", e);
				}
				return vs;
			}
		};
		poolVirtualSocketCtoS.setHighMarkTimeLimit(10000);
		return poolVirtualSocketCtoS;
	}

	/**
	 * Retrieves a virtual socket from the client-to-server pool for issuing
	 * a remote request.
	 *
	 * @return an available virtual socket
	 * @throws Exception if the pool cannot supply a socket
	 */
	protected VirtualSocket getSocketForCtoS() throws Exception {
		VirtualSocket vs = getVirtualSocketCtoSPool().get();
		return vs;
	}

	/**
	 * Returns a virtual socket to the pool or removes it if it has been closed.
	 * Resets its timeout state before releasing.
	 *
	 * @param vs the virtual socket to release
	 * @throws Exception if the pool rejects the socket
	 */
	protected void releaseSocketForCtoS(VirtualSocket vs) throws Exception {
		if (vs == null) {
			return;
		}
		vs.setTimeoutSeconds(0);
		if (vs.isClosed()) {
			getVirtualSocketCtoSPool().remove(vs);
		} else {
			getVirtualSocketCtoSPool().release(vs);
		}
	}

	// used to assign unique int for each StoC vsocket
	private final AtomicInteger aiCountForStoC = new AtomicInteger();
	// flag to know if the initial StoC vsocket has been created
	private volatile boolean bFirstStoCsocketCreated;

	/*
	 * These are vsockets used to listen/wait for method calls from server. This is used when a client sends a remote object to the server,
	 * so that server can then call methods on it, and have it invoked on the client. On the server, each client has a session that has a
	 * list of the StoC vsockets.
	 */
	/**
	 * Creates a new server-to-client virtual socket used for receiving callback
	 * invocations from the server. A dedicated thread is spawned to read and
	 * process incoming messages on this socket.
	 *
	 * @throws Exception if the socket cannot be created
	 */
	protected void createSocketForStoC() throws Exception {
		final VirtualSocket socket = (VirtualSocket) multiplexerClient.createSocket("StoC");
		final int id = aiCountForStoC.getAndIncrement();
		// accept new connections
		Thread t = new Thread(new Runnable() {
			public void run() {
				int errorCnt = 0;
				long msLastError = 0;
				/* 20151103 on hold for OAsyncCombinedClient work
				OARuntime.get().threadLocals().setRemoteMultiplexerClient(RemoteMultiplexerClient.this);
				*/
				RemoteObjectInputStream ois = null;
				for (int i = 0;; i++) {
					try {
						if (socket.isClosed()) {
							break;
						}
						ois = processStoCSocket(socket, id, ois);
					} catch (Exception e) {
						if (!socket.isClosed()) {
							errorCnt++;
							long ms = System.currentTimeMillis();
							if (msLastError == 0 || ms - msLastError > 5000 || errorCnt < 5) {
								LOG.log(Level.WARNING, "Exception in StoC thread, errorCnt=" + errorCnt, e);
								if (errorCnt > 50) {
									break;
								}
								msLastError = ms;
							}
						}
					}
				}
			}
		});
		t.setName("Remote.Socket.StoC." + socket.getConnectionId() + "." + socket.getId());
		t.setDaemon(true);
		t.start();
		bFirstStoCsocketCreated = true;
		LOG.fine("created StoC socket and thread, connectionId=" + socket.getConnectionId() + ", vid=" + id);
	}

	/**
	 * Counter tracking the total number of {@link OARemoteThread} instances
	 * ever created by this client. Used for diagnostics and scaling decisions.
	 */
	private final AtomicInteger aiRemoteThreadCount = new AtomicInteger();

	/**
	 * List of active or reusable {@link OARemoteThread} instances used to
	 * process incoming StoC (server-to-client) method invocations. Threads
	 * are reused whenever idle to minimize creation overhead.
	 */
	private final ArrayList<OARemoteThread> alRemoteThread = new ArrayList<OARemoteThread>();

	/**
	 * Acquires or creates a reusable {@link OARemoteThread} to process a
	 * server-to-client request. Threads are reused when idle and new ones
	 * are allocated only when necessary.
	 *
	 * @param ri            the request to associate with the thread
	 * @param bSendMessgage whether the thread should send outgoing messages
	 * @return a remote-thread ready to process the request
	 */
	private OARemoteThread getRemoteThread(RequestInfo ri, boolean bSendMessgage) {
		OARemoteThread remoteThread;
		synchronized (alRemoteThread) {
			for (int i = 0;; i++) {
				for (OARemoteThread rt : alRemoteThread) {
					if (rt.requestInfo != null) {
						continue;
					}
					synchronized (rt.Lock) {
						if (rt.requestInfo == null) {
							rt.requestInfo = ri;
							rt.setSendMessages(bSendMessgage);
							return rt;
						}
					}
				}

				// note: too many threads can increase the vsockets, and reduce the msgQue speed

				int x = alRemoteThread.size();
				
				if (x < 10) {
					break;
				}
				if (x < 15) {
					if (i > 2) {
						break; // 50ms
					}
				} else if (x < 20) {
					if (i > 4) {
						break; // 100ms
					}
				} else if (x < 30) {
					if (i > 8) {
						break; // 200ms
					}
				} else if (x < 40) {
					if (i > 20) {
						break; // 500ms
					}
				} else if (x < 50) {
					if (i > 40) {
						break; // 1 second
					}
				} else if (x < 100) {
					if (i > 60) {
						break; // 1.5 seconds
					}
				} else {
					// otherwise 100 is max and need to wait
					if (i > 0 && i % 100 == 0) {
						LOG.warning("waiting on free remoteThread to use, waitTime=" + (i * 25) + "ms");
					}
				}

				try {
					alRemoteThread.wait(25);
				} catch (Exception e) {
				}
			}
		}
		remoteThread = createRemoteThread();
		remoteThread.setSendMessages(bSendMessgage);

		synchronized (alRemoteThread) {
			remoteThread.requestInfo = ri;
			alRemoteThread.add(remoteThread);
		}
		LOG.fine("new remoteThread created, liveCount=" + alRemoteThread.size() + ", totalCreated=" + aiRemoteThreadCount.get());
		onRemoteThreadCreated(aiRemoteThreadCount.get(), alRemoteThread.size());
		return remoteThread;
	}

	/**
	 * Callback invoked whenever a new {@link OARemoteThread} is created.
	 * Subclasses may override to monitor thread creation rates.
	 *
	 * @param totalCount total threads ever created
	 * @param liveCount  current number of active threads
	 */
	protected void onRemoteThreadCreated(int totalCount, int liveCount) {
	}

	/**
	 * Allocates and starts a new {@link OARemoteThread}. The thread processes
	 * incoming StoC method requests and may execute queued runnables.
	 *
	 * @return the newly created remote thread
	 */
	private OARemoteThread createRemoteThread() {
		OARemoteThread t = new OARemoteThread() {
			@Override
			public void run() {
				/* 20151103 on hold for OAsyncCombinedClient work
				OARuntime.get().threadLocals().setRemoteMultiplexerClient(RemoteMultiplexerClient.this);
				*/
				boolean bReset = true;
				for (; !stopCalled;) {
					try {
						if (shouldClose(this)) {
							break;
						}
						synchronized (Lock) {
							if (bReset) {
								reset();
								bReset = false;
							}
							if (requestInfo == null) {
								Lock.wait(2000);
								if (requestInfo == null) {
									continue;
								}
							}
						}
						bReset = true;

						processMessageForStoC(requestInfo);
						this.setAllowRunnable(false); // turn back off, it could have been set to true in setupSyncRequestQueueThread

						this.msLastUsed = System.currentTimeMillis();

						synchronized (Lock) {
							if (requestInfo != null) {
								requestInfo.methodInvoked = true;
								this.requestInfo = null;
							}
							Lock.notifyAll();
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "error in OARemoteThread", e);
					}
				}
			}

			// 20160317
			@Override
			public void addRunnable(Runnable r) {
				if (!getAllowRunnable()) {
					super.addRunnable(r);
					return;
				}
				addSyncRunnable(requestInfo, r);
			}

			@Override
			public void startNextThread() {
				if (startedNextThread) {
					return;
				}
				super.startNextThread();
				synchronized (Lock) {
					if (requestInfo != null) {
						requestInfo.methodInvoked = true;
					}
					Lock.notifyAll();
				}
			}
		};
		t.setDaemon(true);
		t.setName("Remote.RemoteThread." + aiRemoteThreadCount.getAndIncrement());
		t.start();
		//LOG.fine("thread name=" + t.getName());
		return t;
	}

	/**
	 * Determines whether an idle {@link OARemoteThread} should be closed based
	 * on inactivity duration, thread pool size, and system load.
	 *
	 * @param remoteThread the thread under evaluation
	 * @return true if the thread should be terminated
	 */
	private boolean shouldClose(final OARemoteThread remoteThread) {
		if (remoteThread.requestInfo != null) {
			return false;
		}
		int x = alRemoteThread.size();
		if (x < 4) {
			return false;
		}

		int max;
		if (x > 50) {
			max = 100;
		} else if (x > 30) {
			max = 500;
		} else {
			max = 1000;
		}

		if (remoteThread.msLastUsed == 0 || (remoteThread.msLastUsed + max > System.currentTimeMillis())) {
			return false;
		}

		synchronized (alRemoteThread) {
			if (remoteThread.requestInfo != null) {
				return false;
			}
			if (alRemoteThread.size() < 4) {
				return false;
			}

			int cntUsed = 0;
			for (OARemoteThread rt : alRemoteThread) {
				if (rt.requestInfo != null) {
					cntUsed++;
				}
			}
			if (cntUsed + 3 > x) {
				return false;
			}

			alRemoteThread.remove(remoteThread);
			remoteThread.stopCalled = true;
		}
		return true;
	}

	/**
	 * Processes the next incoming server-to-client message on the given socket.
	 * Depending on message type, this may:
	 * <ul>
	 *   <li>Request new StoC sockets</li>
	 *   <li>Start or close stream reuse</li>
	 *   <li>Dispatch remote invocations to queues</li>
	 *   <li>Handle queued return values</li>
	 * </ul>
	 *
	 * The method returns either a reusable input stream or null
	 * if a new one should be created for the next cycle.
	 *
	 * @param socket   the StoC virtual socket
	 * @param threadId id of the worker thread handling the socket
	 * @param ois      the current input stream, or null to allocate a new one
	 * @return the input stream to use for subsequent reads (or null)
	 * @throws Exception if stream or message processing fails
	 */
	protected RemoteObjectInputStream processStoCSocket(final VirtualSocket socket, int threadId, RemoteObjectInputStream ois)
			throws Exception {
		if (socket.isClosed()) {
			return null;
		}

		boolean bHadOis;
		if (ois != null) {
			bHadOis = true;
		} else {
			ois = new RemoteObjectInputStream(socket, hmClassDescInput);
			bHadOis = false;
		}

		// wait for next message
		RequestInfo.Type type = RequestInfo.getType(ois.readByte());
		aiReceivedMethodCallCnt.incrementAndGet();

		if (type == RequestInfo.Type.StoC_CreateNewStoCSocket) {
			// server is requesting another vsocket "stoc"
			createSocketForStoC();
			if (bHadOis) {
				return ois;
			}
			return null;
		}

		if (type == RequestInfo.Type.StoC_StartObjectInputStream) {
			// server is requesting to reuse the ois
			return ois;
		}
		if (type == RequestInfo.Type.StoC_CloseObjectInputStream) {
			// server is requesting to close the ois
		    ois.close(); // 20250318
			return null;
		}

		RequestInfo ri = new RequestInfo();
		ri.type = type;
		ri.msStart = System.currentTimeMillis();
		ri.nsStart = System.nanoTime();
		ri.socket = socket;
		ri.connectionId = socket.getConnectionId();
		ri.vsocketId = socket.getId();
		ri.threadId = threadId;

		boolean b = false;
		try {
			b = _processSocket(ri, ois);
			// System.out.println(String.format("processStoCSocket ri=%s", ri.toLogString())); 
		} finally {
			ri.nsEnd = System.nanoTime();
			if (b) {
				afterInvokForStoC(ri);
			}
		}
		if (bHadOis) {
			return ois;
		}
		return null;
	}

	/**
	 * Queue of asynchronous server-to-client requests. Each entry represents
	 * a remote invocation that must be processed by an available
	 * {@link OARemoteThread}. This queue feeds the RequestQueueThread.
	 */
	private final LinkedBlockingQueue<RequestInfo> queRequestInfo = new LinkedBlockingQueue<RequestInfo>();

	/**
	 * Initializes and starts the worker thread responsible for processing
	 * asynchronous server-to-client requests. The thread continuously polls
	 * the request queue and assigns each request to a suitable
	 * {@link OARemoteThread} for execution.
	 */
	protected void setupRequestQueueThread() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				for (; !bClosed;) {
					try {
						RequestInfo ri = queRequestInfo.poll(4, TimeUnit.SECONDS);
						if (ri == null) {
							continue;
						}

						OARemoteThread t = getRemoteThread(ri, true);
						synchronized (t.Lock) {
							t.Lock.notifyAll(); // have RemoteClientThread call processMessageforStoC(..)
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "RequestQueueThread error", e);
					}
				}
			}
		});
		t.setName("Remote.RequestQueue");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Queue of synchronous server-to-client requests. These requests expect
	 * return values and must be processed in strict order. The SyncRequestQueueThread
	 * reads from this queue and dispatches work to remote threads.
	 */
	private final LinkedBlockingQueue<RequestInfo> queSyncRequestInfo = new LinkedBlockingQueue<RequestInfo>();

	/**
	 * Initializes and starts the worker thread responsible for processing
	 * synchronous server-to-client requests. These requests expect return
	 * values or acknowledgements and must be handled in order.
	 *
	 * The thread obtains a remote thread, signals it to process the request,
	 * and waits (with timeout rules) for completion.
	 */
	protected void setupSyncRequestQueueThread() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				for (; !bClosed;) {
					try {
						RequestInfo ri = queSyncRequestInfo.poll(4, TimeUnit.SECONDS); // blocks
						if (ri == null) {
							continue;
						}
						
						// System.out.println("SyncRequestQueueThread msg=  "+ri.toLogString());                        
						
						if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
							if (ri.bind != null && ri.bind.isOASync) {
								if (ri.connectionId == multiplexerClient.getConnectionId()) {
									// 20160122 this client called a oasync method. This is the return ack.
									RequestInfo rix = hmAsyncRequestInfo.remove(ri.messageId);
									synchronized (rix) {
										rix.response = OAReflect.getEmptyPrimitive(rix.method.getReturnType());
										rix.methodInvoked = true;
										rix.notifyAll(); // wake up waiting thread that made this request.  See onInvokeForCtoS(..)

										// 20160121 wait for OARemoteThreadDelegate.startNextThread to be called, which will
										//      then notify rix that sync msg is done, and then the next sync msg can be processed
										rix.wait(5);
									}
									continue;
								}
							}
						}

						// 20180225
						if (ri.type == RequestInfo.Type.CtoS_QueuedRequest || ri.type == RequestInfo.Type.CtoS_ReturnOnQueueSocket) {
							synchronized (ri) {
								ri.methodInvoked = true;
								ri.notifyAll(); // wake up waiting thread that made this request.  See onInvokeForCtoS(..)
							}
							continue;
						}

						int maxSeconds = Math.max(ri.methodInfo == null ? 0 : ri.methodInfo.timeoutSeconds, 0);
						if (maxSeconds < 3) {
							maxSeconds = 3;
						}

						OARemoteThread t = getRemoteThread(ri, false);

						// 20160317
						t.setAllowRunnable(true); // so that oasync methods will be able to have event processing done in a threadpool

						synchronized (t.Lock) {
							t.Lock.notifyAll(); // have RemoteClientThread call processMessageforStoC(..)
							for (int i = 0; t.requestInfo == ri && !ri.methodInvoked; i++) {
								if (i >= (maxSeconds * 10)) {
									if (!OAObject.getDebugMode()) {
										break;
									}
								}
								t.Lock.wait(100);
							}
							if (t.requestInfo == ri && !ri.methodInvoked) {
								StackTraceElement[] stes = t.getStackTrace();
								Exception ex = new Exception();
								ex.setStackTrace(stes);
								LOG.log(Level.WARNING,
										"timeout waiting for sync message to process, will continue, this is stacktrace for the remoteThread="
												+ t.getName() + ", request="
												+ ri.toLogString(),
										ex);
							}
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "SyncRequestQueueThread error", e);
					}
				}
			}
		});
		t.setName("Remote.SyncRequestQueue");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Queue of runnable tasks scheduled during synchronous remote processing.
	 * These runnables are associated with the originating {@link RequestInfo} and
	 * executed by worker threads to support OA event callbacks and sequencing.
	 */
	private LinkedBlockingQueue<Tuple<RequestInfo, Runnable>> queSyncRunnable = new LinkedBlockingQueue<Tuple<RequestInfo, Runnable>>();

	/**
	 * Enqueues a runnable that must be executed within a remote-thread
	 * context used for synchronous message processing. The runnable is paired
	 * with its associated {@link RequestInfo} for correct thread-local handling.
	 *
	 * @param ri the request context associated with the runnable
	 * @param r  the runnable to execute
	 */
	private void addSyncRunnable(RequestInfo ri, Runnable r) {
		int x = queSyncRunnable.size();
		
		if (x > 500) {
			LOG.fine("adding runnable, queSize=" + (queSyncRunnable.size() + 1));
		}
		try {
			queSyncRunnable.put(new Tuple(ri, r));
		} catch (Exception e) {
			LOG.log(Level.WARNING, "error calling addSyncRunnable", e);
		}

		int total = aiSyncRunnableQueueThread.get();
		if (total > 50) {
			return;
		}

		int busy = aiSyncRunnableQueueThreadBusy.get();
		int avail = total - busy;
		if (avail >= queSyncRunnable.size()) {
			return;
		}

		if (total > 5) {
			if (avail > 0) {
				return;
			}
		}

		boolean b = false;
		synchronized (lockRunnableQueue) {
			total = aiSyncRunnableQueueThread.get();
			if (total > 50) {
				return;
			}
			busy = aiSyncRunnableQueueThreadBusy.get();
			avail = total - busy;

			if (avail >= queSyncRunnable.size()) {
				return;
			}

			if (total > 5) {
				if (avail > 0) {
					return;
				}
			} else {
				b = true;
				aiSyncRunnableQueueThread.incrementAndGet();
			}
		}
		
		if (b) {
			//System.out.println("createSyncRunnableQueueThread =====> total="+total+", busy="+busy+", AVAIL="+avail+", queSize="+queSyncRunnable.size());
			createSyncRunnableQueueThread();
		}
	}

	/**
	 * Initializes multiple worker threads responsible for processing queued
	 * runnables submitted during synchronous remote processing. These threads
	 * assist in executing OA event callbacks while maintaining ordering rules.
	 */
	protected void setupSyncRunnableQueueThread() {
		LOG.fine("setup");
		for (int i = 0; i < 3; i++) {
			aiSyncRunnableQueueThread.incrementAndGet();
			createSyncRunnableQueueThread();
		}
	}

	/**
	 * Lock object used to coordinate scaling decisions and throttling behavior
	 * when adding threads to the synchronous runnable processing pool.
	 */
	private final Object lockRunnableQueue = new Object();

	/**
	 * Secondary lock used during cleanup and down-scaling of the synchronous
	 * runnable-queue thread pool. Ensures consistent state while evaluating load.
	 */
	private final Object lockRunnableQueue2 = new Object();
	
	/**
	 * Tracks the current number of active worker threads dedicated to processing
	 * synchronous runnable tasks. Adjusted dynamically as load increases or decreases.
	 */
	private final AtomicInteger aiSyncRunnableQueueThread = new AtomicInteger(0); // current size
	
	/**
	 * Total number of synchronous runnable-processing threads ever created.
	 * A diagnostic metric for scaling behavior over time.
	 */
	private final AtomicInteger aiSyncRunnableQueueThreadTotal = new AtomicInteger(0); // total created
	
	/**
	 * Counter indicating how many synchronous runnable-processing threads
	 * are currently executing work. Used for scaling and throttling decisions.
	 */
	private final AtomicInteger aiSyncRunnableQueueThreadBusy = new AtomicInteger(0); // number that are running

	/**
	 * Creates a single worker thread used to process runnables from the
	 * synchronous runnable queue. Threads terminate themselves when queue load
	 * is reduced, allowing dynamic scaling.
	 */
	protected void createSyncRunnableQueueThread() {
		OARemoteThread t = new OARemoteThread() {
			@Override
			public void run() {
				int x = aiSyncRunnableQueueThread.get();
				if (x > 10 && (x % 2) == 0) {
					try {
						Thread.sleep(2); // throttle
					} catch (Exception e) {
					}
				}
				final long tsStart = System.currentTimeMillis();
				for (int i = 0; !stopCalled && !bClosed; i++) {
					try {
						Tuple<RequestInfo, Runnable> tup = queSyncRunnable.poll(5, TimeUnit.SECONDS); // blocks
						if (tup == null) {
							continue;
						}
						Runnable r = tup.b;
						if (r == null) {
							continue;
						}
						reset();
						this.requestInfo = tup.a;
						this.setAllowRunnable(false);
						this.msLastUsed = System.currentTimeMillis();

						try {
							aiSyncRunnableQueueThreadBusy.incrementAndGet();
							r.run();
						} finally {
							aiSyncRunnableQueueThreadBusy.decrementAndGet();
						}
						if (i < 25) {
							if ((System.currentTimeMillis() - tsStart) < 1000) {
								continue;
							}
						}

						synchronized (lockRunnableQueue2) {
							x = queSyncRunnable.size();

							int x1 = aiSyncRunnableQueueThread.get();
							int x2 = aiSyncRunnableQueueThreadBusy.get();
							if (x1 - x2 < x) {
								continue;
							}

							if (x1 > 10) {
								break; // end
							}
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "error processing OARemoteThread runnable, requestInfo=" + requestInfo.toLogString(), e);
					}
				}
				aiSyncRunnableQueueThread.decrementAndGet();
			}
		};
		t.setDaemon(true);
		t.setName("Remote.SyncRunnableQueue." + aiSyncRunnableQueueThreadTotal.getAndIncrement());
		t.start();
		//LOG.fine("thread name=" + t.getName());
	}

	/**
	 * Processes a low-level incoming server-to-client message that has already
	 * been identified by its {@link RequestInfo.Type}. Depending on the message
	 * type, this may:
	 * <ul>
	 *   <li>Handle queued responses</li>
	 *   <li>Dispatch method invocations</li>
	 *   <li>Translate remote references into proxies</li>
	 *   <li>Queue work for threads</li>
	 * </ul>
	 *
	 * @param ri  the request information object
	 * @param ois the input stream used to read message content
	 * @return true if the message is complete and can be logged
	 * @throws Exception if message processing fails
	 */
	private boolean _processSocket(final RequestInfo ri, final RemoteObjectInputStream ois) throws Exception {

		if (ri.type == RequestInfo.Type.StoC_QueuedResponse || ri.type == RequestInfo.Type.CtoS_ReturnOnQueueSocket) {
			// 5:CtoS_QueuedRequest get back from server
			// response for CtoS_QueuedRequest
			int x = ois.readByte();

			// 20180225
			if (ri.type == RequestInfo.Type.StoC_QueuedResponse && x == 3) {
				ri.bHadOASyncEvent = ois.readBoolean();
			}

			Object objx = ois.readObject();

			ri.messageId = ois.readInt();
			RequestInfo rix = hmAsyncRequestInfo.remove(ri.messageId);

			if (x == 0) {
				ri.exception = (Exception) objx;
			} else if (x == 1) {
				ri.exceptionMessage = (String) objx;
			} else if (x == 2) {
				Object[] responses = (Object[]) objx;
				String bindName = (String) responses[0];

				if (rix != null) {
					BindInfo bindx = getBindInfo(bindName);
					objx = bindx != null ? bindx.weakRef.get() : null;
					if (bindx == null || objx == null) {
						boolean bUsesQueue = (Boolean) responses[1];
						Object obj = getProxyForCtoS(bindName, rix.methodInfo.remoteReturn, bUsesQueue);
						ri.response = obj;
					} else {
						ri.response = bindx.getObject();
					}
				}
			} else {
				ri.response = objx;
			}

			if (rix == null) {
				ri.exceptionMessage = "StoC requestInfo not found";
			} else {
				if (ri.response != null && rix.methodInfo.compressedReturn && rix.methodInfo.remoteReturn == null) {
					ri.response = ((OACompressWrapper) ri.response).getObject();
				}

				synchronized (rix) {
					rix.response = ri.response;
					rix.exception = ri.exception;
					rix.exceptionMessage = ri.exceptionMessage;
					rix.bHadOASyncEvent = ri.bHadOASyncEvent;
					
					if (ri.bHadOASyncEvent) {
						// 20180225 need to put on sync que, so that it waits for sync events to be processed first
						queSyncRequestInfo.put(rix);
					} else {
						rix.methodInvoked = true;
						// 6:CtoS_QueuedRequest  notify waiting thread from #4
						rix.notifyAll(); // wake up waiting thread that made this request.  See onInvokeForCtoS(..)
					}
				}
			}
			return true;
		}

		// put ri on queue to be processed by remoteClientThread

		if (ri.type == RequestInfo.Type.StoC_QueuedRequest) {
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();
			ri.messageId = ois.readInt();
			queRequestInfo.put(ri);
			return false;
		}

		if (ri.type == RequestInfo.Type.StoC_QueuedRequestNoResponse) {
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();

			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				ri.exceptionMessage = "could not find bind object";
				return false;
			}
			ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
			queRequestInfo.put(ri);
			return false;
		}

		if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
			ri.connectionId = ois.readInt();
			ri.messageId = ois.readInt();

			if (ri.connectionId == multiplexerClient.getConnectionId()) {
				// this client called a broadcast
				RequestInfo rix = hmAsyncRequestInfo.get(ri.messageId);
				if (rix == null) {
					ri.exceptionMessage = "StoC requestInfo not found";
					return true;
				}
				
				// if oasync was called by remoteThread, then dont put in queue, which would have made it take more remoteThreads to get to it.
				//     instead, notify it when it is received back from the server.
				if (rix.bind.isOASync && !rix.isRemoteThread) {
					ri.bind = rix.bind;
					putQueSyncRequestInfo(ri); // sync que will notify the original thread
				} else {
					hmAsyncRequestInfo.remove(ri.messageId);
					synchronized (rix) {
						rix.response = OAReflect.getEmptyPrimitive(rix.method.getReturnType());
						rix.methodInvoked = true;
						rix.notifyAll(); // wake up waiting thread that made this request.  See onInvokeForCtoS(..)
					}
				}
				return true;
			}

			// one client sent the broadcast, this is where other clients will process it
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();

			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				// ri.exceptionMessage = "could not find bind object", this client not set up to receive it.
				return false;
			} else {
				ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
			}

			if (ri.bind.isOASync) {
				putQueSyncRequestInfo(ri);
			} else {
				queRequestInfo.put(ri);
			}
			return false;
		}

		if (ri.type == RequestInfo.Type.StoC_QueuedBroadcast) {
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();
			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				return false;
			}
			ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);

			if (ri.bind.isOASync) {
				putQueSyncRequestInfo(ri);
			} else {
				queRequestInfo.put(ri);
			}
			return false;
		}

		if (ri.type == RequestInfo.Type.StoC_SocketRequest) {
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();
			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				ri.exceptionMessage = "invalid bind name";
			} else {
				ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
			}
			queRequestInfo.put(ri);
			return false;
		}

		if (ri.type == RequestInfo.Type.StoC_SocketRequestNoResponse) {
			ri.bindName = ois.readAsciiString();
			ri.methodNameSignature = ois.readAsciiString();
			ri.args = (Object[]) ois.readObject();
			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				ri.exceptionMessage = "invalid bind name";
			} else {
				ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
			}
			queRequestInfo.put(ri);
			return false;
		}

		ri.exceptionMessage = "invalid command";
		return true;
	}

	/**
	 * Processes a full server-to-client method invocation by delegating to
	 * {@link #_processMessageForStoC(RequestInfo)} and then sending a response
	 * back to the server when required.
	 *
	 * @param ri the request information containing invocation data
	 * @throws Exception if invocation or response transmission fails
	 */
	protected void processMessageForStoC(RequestInfo ri) throws Exception {
		try {
			_processMessageForStoC(ri); // invoke
			if (ri.type.hasReturnValue()) {
				sendResponseForStoC(ri);
			}
			afterInvokForStoC(ri);
		} catch (Exception e) {
			ri.exception = e;
		} finally {
			ri.methodInvoked = true;
		}
	}

	/**
	 * Places a synchronous message into the sync request queue while applying
	 * throttling rules to prevent excessive queueing under load.
	 *
	 * @param ri the request information to enqueue
	 * @throws Exception if queueing is interrupted
	 */
	private void putQueSyncRequestInfo(final RequestInfo ri) throws Exception {
		// add throttle, based on number of current remoteThreads, etc
		queSyncRequestInfo.put(ri); // sync que will notify the original thread

		int x = queSyncRequestInfo.size();
		if (x < 350) {
			return;
		}

		int max;
		if (hmAsyncRequestInfo.size() > 0) { // this is how many request that this client is waiting for.
			if (alRemoteThread.size() > 10) {
				max = 2500;
			} else {
				max = 500;
			}
		} else {
			// not waiting, just processing
			max = 350;
		}
		if (x < max) {
			return;
		}

		LOG.fine("throttle begin syncQue.size=" + queSyncRequestInfo.size() + ", remoteThread.cnt=" + alRemoteThread.size());
		for (int i = 0; i < 55; i++) { // cant wait more then a second, since circQue checks msLastRead
			Thread.sleep(15);
			x = queSyncRequestInfo.size();
			if (x < (max / 10)) {
				break;
			}
			if (hmAsyncRequestInfo.size() > 0) { // waiting on response
				if (alRemoteThread.size() > 12) {
					if (i > 3) {
						break;
					}
				}
			}
		}
	}

	/**
	 * Executes a server-to-client remote method invocation.
	 * Steps include:
	 * <ul>
	 *   <li>Resolving bind information</li>
	 *   <li>Locating the correct method</li>
	 *   <li>Restoring compressed arguments</li>
	 *   <li>Resolving remote-object parameters into proxies</li>
	 *   <li>Invoking the underlying method on the client-side implementation</li>
	 *   <li>Handling compressed or remote-object return values</li>
	 * </ul>
	 *
	 * @param ri the request containing invocation metadata
	 * @throws Exception if invocation fails
	 */
	private void _processMessageForStoC(RequestInfo ri) throws Exception {
		if (ri.bind == null) {
			ri.bind = getBindInfo(ri.bindName);
			if (ri.bind == null) {
				ri.exceptionMessage = "bind Object not found";
				return;
			}
		}
		if (ri.methodInfo == null) {
			ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
		}
		if (ri.methodInfo != null) {
			ri.method = ri.methodInfo.method;
		}

		if (ri.method == null) {
			ri.exceptionMessage = "method not found";
			return;
		}

		Object remoteObject = ri.bind.getObject();
		if (remoteObject == null) {
			ri.exceptionMessage = "remote Object has been garbage collected, class=" + ri.bind.interfaceClass;

			/*// send message to server to remove client remote object from session VirtualSocket socket
			 * = getSocketForCtoS(); // used to send message, and get response RemoteObjectOutputStream
			 * oos = new RemoteObjectOutputStream(ri.socket, hmClassDescOutput, aiClassDescOutput);
			 * oos.writeByte(CtoS_Command_RemoveSessionBroadcastThread);
			 * oos.writeAsciiString(ri.bind.name); oos.flush(); releaseSocketForCtoS(socket); */
			return;
		}

		// check for compressed params
		if (ri.methodInfo.compressedParams != null && ri.args != null) {
			for (int i = 0; i < ri.methodInfo.compressedParams.length && i < ri.args.length; i++) {
				if (ri.methodInfo.remoteParams != null && ri.methodInfo.remoteParams[i] != null) {
					continue;
				}
				if (!ri.methodInfo.compressedParams[i]) {
					continue;
				}
				ri.args[i] = ((OACompressWrapper) ri.args[i]).getObject();
			}
		}

		// check to see if any of the args[] are remote objects
		if (ri.methodInfo.remoteParams != null && ri.args != null) {
			for (int i = 0; i < ri.methodInfo.remoteParams.length && i < ri.args.length; i++) {
				if (ri.methodInfo.remoteParams[i] == null) {
					continue;
				}
				if (ri.args[i] == null) {
					continue;
				}
				if (!(ri.args[i] instanceof String)) {
					LOG.warning("expected remote object, recvd=" + ri.args[i] + ", will ignore, info:" + ri.toLogString());
					continue;
				}

				// convert the param to real object
				String bindName = (String) ri.args[i];

				BindInfo bindx = getBindInfo(bindName);
				Object objx = bindx != null ? bindx.weakRef.get() : null;
				if (bindx == null || objx == null) {
					boolean bDontUseQueue = (ri.methodInfo.dontUseQueues != null && ri.methodInfo.dontUseQueues[i]);
					Object obj = getProxyForCtoS(ri, bindName, ri.methodInfo.remoteParams[i], bDontUseQueue);

					ri.args[i] = obj;
				} else {
					ri.args[i] = bindx.getObject();
				}
			}
		}

		try {
			OARuntime.get().threadLocals().setRemoteRequestInfo(ri);

			// 20141217
			if (!ri.bind.isBroadcast) {
				OARemoteThreadDelegate.sendMessages(true);
			}
			ri.response = ri.method.invoke(ri.bind.getObject(), ri.args);
		} catch (InvocationTargetException e) {
			Exception ex = e;
			for (int i = 0; i < 10; i++) {
				Throwable t = ex.getCause();
				if (t == null || t == ex || !(t instanceof Exception)) {
					ri.exception = ex;
					break;
				}
				ex = (Exception) t;
				ri.exception = ex;
			}
		} finally {
			// 20141217
			if (!ri.bind.isBroadcast) {
				OARemoteThreadDelegate.sendMessages(false);
			}
		}
		OARuntime.get().threadLocals().setRemoteRequestInfo(null);

		if (ri.response != null && ri.methodInfo.remoteReturn != null) {
			BindInfo bindx = getBindInfoForObject((Object) ri.response);
			Object objx = bindx != null ? bindx.weakRef.get() : null;
			if (bindx == null || objx == null) {
				// make remote
				boolean b = ri.methodInfo.dontUseQueueForReturnValue;
				bindx = getBindInfo(ri, createBindName(ri), ri.response, ri.methodInfo.remoteReturn, b);
			}
			ri.responseBindName = bindx.name; // this will be the return value
		} else if (ri.methodInfo.compressedReturn && ri.methodInfo.remoteReturn == null) {
			ri.response = new OACompressWrapper(ri.response);
		}
		ri.nsEnd = System.nanoTime();
	}

	/**
	 * Sends a response back to the server for a server-to-client method
	 * invocation when the request type requires a return value. The response
	 * may include:
	 * <ul>
	 *   <li>Exceptions</li>
	 *   <li>Error messages</li>
	 *   <li>Return values</li>
	 *   <li>Remote-object bind references</li>
	 * </ul>
	 *
	 * @param ri the request whose result should be transmitted
	 * @throws Exception if the reply cannot be written or socket allocation fails
	 */
	protected void sendResponseForStoC(RequestInfo ri) throws Exception {
		if (ri.type.hasReturnValue()) {
			if (ri.socket == null || (ri.bind != null && ri.bind.usesQueue)) {
				// need to send back as async response message
				VirtualSocket socket = getSocketForCtoS();
				RemoteObjectOutputStream oos = new RemoteObjectOutputStream(socket, hmClassDescOutput, aiClassDescOutput);

				oos.writeByte(RequestInfo.Type.CtoS_QueuedResponse.ordinal());
				oos.writeInt(ri.messageId);
				if (ri.exception != null) {
					Object resp;
					if (ri.exception instanceof Serializable) {
						resp = ri.exception;
					} else {
						resp = new Exception(ri.exception.toString() + ", info: " + ri.toLogString());
					}
					oos.writeByte(0);
					oos.writeObject(resp);
				} else if (ri.exceptionMessage != null) {
					oos.writeByte(1);
					oos.writeObject(ri.exceptionMessage);
				} else if (ri.responseBindName != null) {
					oos.writeByte(2);
					oos.writeObject(new Object[] { ri.responseBindName, ri.responseBindUsesQueue });
				} else {
					oos.writeByte(3);
					oos.writeObject(ri.response);
				}
				oos.flush();
		        oos.close(); // 20250318
				releaseSocketForCtoS(socket);
			} else {
				RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, hmClassDescOutput, aiClassDescOutput);
				if (ri.exception != null) {
					Object resp;
					if (ri.exception instanceof Serializable) {
						resp = ri.exception;
					} else {
						resp = new Exception(ri.exception.toString() + ", info: " + ri.toLogString());
					}
					oos.writeByte(0); // false=error
					oos.writeObject(resp);
				} else if (ri.exceptionMessage != null) {
					oos.writeByte(1);
					oos.writeObject(ri.exceptionMessage);
				} else if (ri.responseBindName != null) {
					oos.writeByte(2);
					oos.writeObject(new Object[] { ri.responseBindName, ri.responseBindUsesQueue });
				} else {
					oos.writeByte(3);
					oos.writeObject(ri.response);
				}
				oos.flush();
		        oos.close(); // 20250318
			}
		} else {
			if (ri.exception != null) {
				LOG.warning("error processing StoC, exception=" + ri.exception.toString());
			} else if (ri.exceptionMessage != null) {
				LOG.warning("error processing StoC, exception=" + ri.exceptionMessage);
			}
		}
	}

	/**
	 * Called after a server-to-client method completes. Logs any errors or
	 * exceptions contained in the request information.
	 *
	 * @param ri the completed request information
	 */
	public void afterInvokForStoC(RequestInfo ri) {
		if (ri == null) {
			return;
		}
		if (ri.exception != null || ri.exceptionMessage != null) {
			LOG.log(Level.WARNING, ri.toLogString(), ri.exception);
		}
	}

	/**
	 * Retrieves bind metadata for the given bind name.
	 *
	 * @param name the unique bind identifier
	 * @return the associated {@link BindInfo}, or null if not found
	 */
	protected BindInfo getBindInfo(String name) {
		if (name == null) {
			return null;
		}
		return hmNameToBind.get(name);
	}

	/**
	 * Performs distributed garbage-collection cleanup by removing bind entries
	 * whose associated remote objects have been garbage collected. This scans
	 * the reference queue for collected weak references.
	 */
	public void performDGC() {
		for (;;) {
			WeakReference ref = (WeakReference) referenceQueue.poll();
			if (ref == null) {
				break;
			}

			for (Map.Entry<String, BindInfo> entry : hmNameToBind.entrySet()) {
				BindInfo bindx = entry.getValue();
				if (bindx.weakRef == ref) {
					hmNameToBind.remove(entry.getKey());
					break;
				}
			}
		}
	}

	/**
	 * Locates the bind information associated with a previously exposed
	 * remote object by scanning all known bindings.
	 *
	 * @param obj the remote object instance
	 * @return the bind info if found, otherwise null
	 */
	protected BindInfo getBindInfoForObject(Object obj) {
		if (obj == null) {
			return null;
		}
		for (BindInfo bindx : hmNameToBind.values()) {
			if (bindx.weakRef.get() == obj) {
				return bindx;
			}
		}
		return null;
	}

	/**
	 * Mapping of bind names to their associated {@link BindInfo} metadata.
	 * Each entry represents a remote object (either client-side or server-side)
	 * that has been registered with this client.
	 *
	 * <p>The map is used to:
	 * <ul>
	 *   <li>Resolve bind names during CtoS and StoC invocation processing</li>
	 *   <li>Locate the client-side callback object for incoming StoC requests</li>
	 *   <li>Track metadata such as interface class, queue usage, and weak references</li>
	 * </ul>
	 *
	 * <p>Entries are removed automatically when the associated weak references are
	 * cleared, allowing unused bindings to be reclaimed.
	 */
	private ConcurrentHashMap<String, BindInfo> hmBindInfo = new ConcurrentHashMap<String, BindInfo>();

	/**
	 * Creates or retrieves bind metadata for a remote object. If a new bind is
	 * created, its method metadata is loaded and it is registered in the bind map.
	 *
	 * @param name          the bind name
	 * @param obj           the associated client-side object (may be null)
	 * @param interfaceClass the interface defining remote methods
	 * @param bUsesQueue    whether queue-based messaging is used
	 * @param bIsBroadcast  whether this is for a broadcast remote interface
	 * @return the bind info instance
	 */
	protected BindInfo getBindInfo(String name, Object obj, Class interfaceClass, boolean bUsesQueue, boolean bIsBroadcast) {
		if (name == null || interfaceClass == null) {
			throw new IllegalArgumentException("name and interfaceClass can not be null");
		}
		BindInfo bind = hmBindInfo.get(name);
		if (bind != null) {
			return bind;
		}

		String qn;
		if (bUsesQueue) {
			qn = "qIsOnServer";
		} else {
			qn = null;
		}
		bind = new BindInfo(name, obj, interfaceClass, referenceQueue, bIsBroadcast, qn, -1);
		bind.loadMethodInfo();
		hmNameToBind.put(name, bind);
		return bind;
	}

	/**
	 * Convenience method that derives queue usage rules from an existing
	 * request's bind metadata and delegates to the primary bind creation
	 * method.
	 *
	 * @param ri            the originating request information
	 * @param name          the bind name to assign
	 * @param obj           the client-side remote object implementation
	 * @param interfaceClass the remote interface class
	 * @param bDontUseQueue true to force socket calls instead of queue usage
	 * @return the bind metadata for the object
	 */
	protected BindInfo getBindInfo(RequestInfo ri, String name, Object obj, Class interfaceClass, boolean bDontUseQueue) {
		return getBindInfo(name, obj, interfaceClass, (ri.bind.usesQueue && !bDontUseQueue), ri.bind.isBroadcast);
	}


	/**
	 * Counter tracking the total number of client-to-server method invocations
	 * made through this multiplexer client. Incremented for each outgoing
	 * remote call, primarily for diagnostics, monitoring, and debugging.
	 */
	private AtomicInteger aiMethodCallCnt = new AtomicInteger();

	/**
	 * Counter tracking the total number of server-to-client method invocations
	 * received and processed by this client. Incremented for each StoC call and
	 * used for diagnostics and performance monitoring.
	 */
	private AtomicInteger aiReceivedMethodCallCnt = new AtomicInteger();

	/**
	 * Returns the total number of remote method calls initiated by this client.
	 *
	 * @return the count of client-to-server invocations
	 */
	public long getMethodCallCount() {
		return aiMethodCallCnt.get();
	}

	/**
	 * Returns the number of remote method calls or broadcasts received from
	 * the server.
	 *
	 * @return the count of server-to-client invocations
	 */
	public long getReceivedMethodCount() {
		return aiReceivedMethodCallCnt.get();
	}
}
