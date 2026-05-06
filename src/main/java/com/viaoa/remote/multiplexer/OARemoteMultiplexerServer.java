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

import java.io.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.comm.multiplexer.OAMultiplexerServer;
import com.viaoa.comm.multiplexer.io.VirtualServerSocket;
import com.viaoa.comm.multiplexer.io.VirtualSocket;
import com.viaoa.io.OACompressWrapper;
import com.viaoa.object.*;
import com.viaoa.queue.OACircularQueue;
import com.viaoa.reflect.OAReflect;
import com.viaoa.remote.info.BindInfo;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OARemoteThread;
import com.viaoa.serialize.OAObjectSerializer;

/**
 * Server-side implementation of OA's remoting layer built on top of the
 * Multiplexer messaging subsystem. This class receives remote method calls
 * from connected clients, invokes the corresponding Java methods, and
 * returns results back using either direct socket response or async queues.
 *
 * <p>This is the central coordinator for:
 * <ul>
 *   <li>Binding server objects for client lookup</li>
 *   <li>Managing broadcast objects where one call fans out to all clients</li>
 *   <li>Maintaining session state for each connected client</li>
 *   <li>Performing distributed garbage collection (DGC) of remote objects</li>
 *   <li>Invoking methods on client-side remote objects (StoC)</li>
 *   <li>Handling queued async messaging through OACircularQueue</li>
 * </ul>
 *
 * <p>Major features:
 * <ol>
 *   <li><b>Lookup bindings</b> – server publishes remote objects (interfaces)</li>
 *   <li><b>Queue-based async message processing</b>
 *       where high-volume calls avoid waiting on socket round-trips</li>
 *   <li><b>Session tracking</b> – manages all sockets, virtual sockets, and
 *       class-descriptor caches per client</li>
 *   <li><b>Broadcast remoting</b> – a single method call triggers remote
 *       invocations on all subscribed clients</li>
 *   <li><b>Remote proxies for client objects</b> – server can invoke client-owned objects</li>
 *   <li><b>Distributed GC</b> – removes stale remote objects via WeakReference polling</li>
 * </ol>
 *
 * <p>Internally it uses:
 * <ul>
 *   <li>{@link com.viaoa.comm.multiplexer.OAMultiplexerServer}</li>
 *   <li>{@link com.viaoa.comm.multiplexer.io.VirtualServerSocket}</li>
 *   <li>{@link com.viaoa.remote.info.RequestInfo}</li>
 *   <li>{@link com.viaoa.remote.info.BindInfo}</li>
 * </ul>
 *
 * <p>This provides a lightweight, high-performance, Java-native RPC layer
 * that avoids the complexity of ORMs, proxies, and heavyweight frameworks.
 *
 * <p>It is a foundational building block that enables OA applications to
 * distribute object graphs, send messages, and call methods across
 * multiple JVMs without requiring REST, gRPC, or WebSockets.
 *
 * @author vvia
 */
public class OARemoteMultiplexerServer {
    private static Logger LOG = Logger.getLogger(OARemoteMultiplexerServer.class.getName());

    /**
     * Underlying multiplexer server used to create virtual server sockets
     * and manage client connection handling.
     */
    private OAMultiplexerServer multiplexerServer;

    /**
     * VirtualServerSocket used for handling Client-to-Server (CtoS)
     * remote method request connections.
     */
    private VirtualServerSocket ssCtoS;
 
    /**
     * VirtualServerSocket used for handling Server-to-Client (StoC)
     * method invocation channels.
     */
    private VirtualServerSocket ssStoC;

    /**
     * Counter used to assign unique bind names for dynamically created
     * remote object bindings.
     */
    private AtomicInteger aiBindCount = new AtomicInteger();

    /**
     * Mapping of bind names to their associated BindInfo instances.
     * Used to look up remote objects published on the server.
     */
    private ConcurrentHashMap<String, BindInfo> hmNameToBind = new ConcurrentHashMap<String, BindInfo>();

    /**
     * Reference queue used for distributed garbage collection (DGC)
     * to detect when weakly referenced remote objects have been reclaimed.
     */
    private ReferenceQueue referenceQueue = new ReferenceQueue();

    /**
     * Strong reference map holding objects published via bind operations
     * to prevent them from being garbage-collected prematurely.
     */
    private ConcurrentHashMap<BindInfo, Object> hmBindObject = new ConcurrentHashMap<BindInfo, Object>();

    /**
     * Collection of asynchronous circular queues used to store queued
     * RequestInfo messages for methods configured to use async dispatch.
     */
    private ConcurrentHashMap<String, OACircularQueue<RequestInfo>> hmAsyncCircularQueue = new ConcurrentHashMap<String, OACircularQueue<RequestInfo>>();

    /**
     * Tracks active client sessions keyed by connectionId.
     * Each Session manages sockets, bind state, and queued message routing
     * for its client.
     */
    private ConcurrentHashMap<Integer, Session> hmSession = new ConcurrentHashMap<Integer, Session>();

    
    /**
     * Constructs a new remote multiplexer server using the supplied
     * multiplexer infrastructure.
     *
     * @param server the underlying OAMultiplexerServer used to create
     *               virtual sockets and accept client connections
     */
    public OARemoteMultiplexerServer(OAMultiplexerServer server) {
        this.multiplexerServer = server;
    }

    /**
     * Returns the underlying multiplexer server instance used by this
     * remote server.
     *
     * @return the OAMultiplexerServer used for connection management
     */
    public OAMultiplexerServer getMultiplexerServer() {
        return this.multiplexerServer;
    }
    
    /**
     * Removes and disconnects the Session associated with the given
     * connectionId. Invoked when the multiplexer reports a client
     * disconnect event.
     *
     * @param connectionId identifier of the client session to remove
     */
    public void removeSession(int connectionId) {
        LOG.fine("removing session, connectionId="+connectionId);
        Session s = hmSession.remove(connectionId);
        if (s != null) {
            s.onDisconnect();
        }
    }
    
    /**
     * Creates or retrieves an existing Session for a newly connected client
     * and assigns its underlying real socket.
     *
     * @param socket        the physical socket for the connection
     * @param connectionId  the multiplexer-provided connection identifier
     */
    public void createSession(Socket socket, int connectionId) {
        Session session = getSession(connectionId, true);
        session.realSocket = socket;
    }

    /**
     * Retrieves the Session for the specified connectionId, optionally
     * creating it if none exists.
     *
     * @param connectionId   identifier of the desired client session
     * @param bCreateIfNull  if true, a new Session is allocated when missing
     * @return the corresponding Session instance or null if not found
     *         and creation is disabled
     */
    public Session getSession(final int connectionId, final boolean bCreateIfNull) {
        Session session = hmSession.computeIfAbsent(connectionId, k -> {
	        if (!bCreateIfNull) return null;
            Session session2 = new Session();
            session2.connectionId = connectionId;
            LOG.fine("create session, connectionId="+connectionId);
            return session2;
        });
        return session;
    }
    
    
    /**
     * Starts the remote server by initializing both the Client-to-Server
     * and Server-to-Client virtual server sockets.
     *
     * @throws Exception if either server socket fails to initialize
     */
    public void start() throws Exception {
        startServerSocketForCtoS();
        startServerSocketForStoC();
    }

    /**
     * Initializes and launches the Client-to-Server (CtoS) server socket
     * thread responsible for accepting incoming virtual socket connections
     * used for remote method requests.
     *
     * @throws Exception if the server socket cannot be created
     */
    protected void startServerSocketForCtoS() throws Exception {
        if (ssCtoS != null) return;
        ssCtoS = multiplexerServer.createServerSocket("CtoS");

        // accept new connections
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (;;) {
                    try {
                        Socket socket = ssCtoS.accept();
                        onNewConnectionForCtoS(socket);
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "Exception on new CtoS socket", e);
                    }
                }
            }
        });
        t.setName("Remote.ServerSocket.CtoS");
        t.setDaemon(true);
        t.start();
        //LOG.config("created Client to Server serversocket thread");
    }

    /**
     * Handles a newly accepted CtoS virtual socket by spawning a dedicated
     * thread to process all remote method requests coming through it.
     *
     * @param socket the accepted VirtualSocket for CtoS communication
     */
    protected void onNewConnectionForCtoS(Socket socket) {
    	if (!(socket instanceof VirtualSocket)) return;
        final VirtualSocket vSocket = (VirtualSocket) socket;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processSocketCtoS(vSocket);
                }
                catch (Exception e) {
                    if (!vSocket.isClosed()) {
                        LOG.log(Level.WARNING, "error processing socket request", e);
                    }
                }
            }
        });
        t.setName("Remote.Socket.CtoS." + vSocket.getConnectionId() + "." + vSocket.getId());
        t.setDaemon(true);
        t.start();
    }

    /**
     * Main processing loop for handling all CtoS remote invocation requests
     * on a dedicated thread. Continues until the virtual socket closes.
     *
     * @param socket the virtual socket receiving remote method requests
     * @throws Exception if low-level stream or processing errors occur
     */
    protected void processSocketCtoS(final VirtualSocket socket) throws Exception {
        final int socketId = socket.getId();
        final int connectionId = socket.getConnectionId();
        final Session session = getSession(connectionId, true);
        
        for (;;) {
            if (socket.isClosed()) break;

            RequestInfo ri = new RequestInfo();
            ri.socket = socket;
            ri.connectionId = ri.socket.getConnectionId();
            ri.vsocketId = socketId;
            
            boolean b = _processSocketCtoSRequest(ri, session);
            ri.nsEnd = System.nanoTime();

            aiReceivedMethodCallCnt.incrementAndGet();            
            if (b) {
                afterInvokeForCtoS(ri);
            }
        }
    }

    /**
     * Processes a single CtoS request by opening a RemoteObjectInputStream,
     * delegating the actual request handling, and closing the stream when
     * complete.
     *
     * @param ri      RequestInfo to populate and process
     * @param session the client Session associated with the request
     * @return true if post-invoke processing should occur, false otherwise
     * @throws Exception if message reading fails or request handling errors occur
     */
    private boolean _processSocketCtoSRequest(final RequestInfo ri, final Session session) throws Exception {
        final RemoteObjectInputStream ois = new RemoteObjectInputStream(ri.socket, session.hmClassDescInput);
        boolean b = _processSocketCtoSRequest(ri, session, ois);
        ois.close(); // 20250318
        return b;
    }
    
    /**
     * Reads and processes a Client-to-Server request from the supplied
     * RemoteObjectInputStream. Determines request type, extracts parameters,
     * resolves bindings, and performs the requested remote invocation or
     * queueing behavior.
     *
     * @param ri      the RequestInfo instance containing request state
     * @param session the Session for the client issuing the request
     * @param ois     the input stream used to read request data
     * @return true if the request was immediately processed and should
     *         trigger post-invoke handling; false if queued or handled
     *         asynchronously
     * @throws Exception if request parsing or processing fails
     */
    private boolean _processSocketCtoSRequest(final RequestInfo ri, final Session session, final RemoteObjectInputStream ois) throws Exception {
        // wait for next message
        ri.type = RequestInfo.getType(ois.readByte());
        // 1:CtoS_QueuedRequest recv from client
        // 1:CtoS_QueuedRequestNoResponse
        
        ri.nsStart = System.nanoTime();
        ri.msStart = System.currentTimeMillis();

        if (ri.type == RequestInfo.Type.CtoS_GetLookupInfo) {
            // lookup, needs to return Java Interface class.
            ri.bindName = ois.readAsciiString();
            BindInfo bind = getBindInfo(ri.bindName);
            RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, session.hmClassDescOutput, session.aiClassDescOutput);
            if (bind != null) {
                ri.response = new Object[] { bind.interfaceClass, bind.usesQueue, bind.isBroadcast };
                if (bind.usesQueue) {
                    session.setupAsyncQueueSender(bind.asyncQueueName);
                }
                oos.writeBoolean(true); // valid response
                oos.writeObject(ri.response);
            }
            else {
                ri.exceptionMessage = "object not found";
                oos.writeBoolean(false);
                oos.writeObject(ri.exceptionMessage);
            }
            oos.flush();
            oos.close(); // 20250318
            return true;
        }
        if (ri.type == RequestInfo.Type.CtoS_GetBroadcastClass) {
            ri.bindName = ois.readAsciiString();
            BindInfo bind = getBindInfo(ri.bindName);
            RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, session.hmClassDescOutput, session.aiClassDescOutput);
            if (bind != null) {
                if (!bind.isBroadcast) {
                    ri.exceptionMessage = "found, but not a broadcast remote object";
                    oos.writeBoolean(false);
                    oos.writeObject(ri.exceptionMessage);
                }
                else {
                    ri.response = bind.interfaceClass;
                    oos.writeBoolean(true);
                    oos.writeObject(ri.response);
                    session.setupAsyncQueueSender(bind.asyncQueueName);
                }
            }
            else {
                ri.exceptionMessage = "object not found";
                oos.writeBoolean(false);
                oos.writeObject(ri.exceptionMessage);
            }
            oos.flush();
            oos.close(); // 20250318
            return true;
        }
        if (ri.type == RequestInfo.Type.CtoS_RemoveSessionBroadcastThread) {
            // remove StoC thread used for broadcast object
            ri.bindName = ois.readAsciiString();
            session.removeBindInfo(ri.bindName);
            return true;
        }

        // reading based on type
        if (ri.type == RequestInfo.Type.CtoS_SocketRequest) {
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
        }
        else if (ri.type == RequestInfo.Type.CtoS_SocketRequestNoResponse) {
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
        }
        else if (ri.type == RequestInfo.Type.CtoS_ReturnOnQueueSocket) {
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
            ri.messageId = ois.readInt();
        }
        else if (ri.type == RequestInfo.Type.CtoS_QueuedRequest) {
            // 2:CtoS_QueuedRequest read from client
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
            ri.messageId = ois.readInt();
        }
        else if (ri.type == RequestInfo.Type.CtoS_QueuedRequestNoResponse) {
            // 2:CtoS_QueuedRequestNoResponse
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
        }
        else if (ri.type == RequestInfo.Type.CtoS_QueuedResponse) {
            ri.messageId = ois.readInt();
            byte b = ois.readByte();
            Object objx = ois.readObject();
            if (b == 0) ri.exception = (Exception) objx;
            else if (b == 1) ri.exceptionMessage = (String) objx;
            else {
                ri.response = objx;
            }
        }
        else if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
            ri.bindName = ois.readAsciiString();
            ri.methodNameSignature = ois.readAsciiString();
            ri.args = (Object[]) ois.readObject();
            ri.connectionId = ois.readInt();
            ri.messageId = ois.readInt();
        }
        
        if (ri.bindName != null) {
            ri.bind = getBindInfo(ri.bindName);
            if (ri.bind == null) {
                ri.bind = session.getBindInfo(ri.bindName);
            }
            if (ri.bind == null) {
                ri.exceptionMessage = "bind Object not found on server";
            }
            else {
                ri.methodInfo = ri.bind.getMethodInfo(ri.methodNameSignature);
                if (ri.methodInfo != null) ri.method = ri.methodInfo.method;
                if (ri.method == null) {
                    ri.exceptionMessage = "method not found";
                }
            }
        }

        // processing based on type
        if (ri.type == RequestInfo.Type.CtoS_SocketRequest) {
            // send back on same socket, in same thread
            if (ri.exceptionMessage == null) {
                invokeUsingRemoteThread(ri, false);
            }
            
            Object resp = null;
            RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, session.hmClassDescOutput, session.aiClassDescOutput);
            if (ri.exception != null) {
                if (ri.exception instanceof Serializable) {
                    resp = ri.exception;
                }
                else resp = new Exception(ri.exception.toString());
                oos.writeByte(0);
            }
            else if (ri.exceptionMessage != null) {
                resp = ri.exceptionMessage;
                oos.writeByte(1);
            }
            else if (ri.responseBindName != null) {
                oos.writeByte(2);
                resp = new Object[] { ri.responseBindName, ri.responseBindUsesQueue };
            }
            else {
                oos.writeByte(3);
                resp = ri.response;
            }

			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
            try {
                srvcOAThreadLocal.addObjectSerializer(session.oaObjectSerializer);
                oos.writeObject(resp);
            }
            finally {
                srvcOAThreadLocal.removeObjectSerializer(session.oaObjectSerializer);
            }
            oos.flush();
            oos.close(); // 20250318
            return false;            
        }

        if (ri.type == RequestInfo.Type.CtoS_SocketRequestNoResponse) {
            if (ri.exceptionMessage != null) return true;
            invokeUsingRemoteThread(ri, false);
            return false;            
        }

        // invoke now, return result using the queue socket
        if (ri.type == RequestInfo.Type.CtoS_ReturnOnQueueSocket) {
            if (ri.exceptionMessage != null) return true;
            invokeUsingRemoteThread(ri, false);
            return false;            
        }
        
        if (ri.type == RequestInfo.Type.CtoS_QueuedRequest) {
            // 3:CtoS_QueuedRequest put in queue
            // unless there is an error, then this will be invoked by the queue thread on the server
            if (ri.exceptionMessage != null) {
                ri.methodInvoked = true;
            }
            session.setupAsyncQueueSender(ri.bind.asyncQueueName);
            OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
            cq.addMessageToQueue(ri);
            return false;            
        }
        
        if (ri.type == RequestInfo.Type.CtoS_QueuedRequestNoResponse) {
            // 3:CtoS_QueuedRequestNoResponse
            // this will be invoked by the queue thread on the server
            if (ri.exceptionMessage != null) return true;
            session.setupAsyncQueueSender(ri.bind.asyncQueueName);
            OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
            cq.addMessageToQueue(ri);
            return false;            
        }
        
        if (ri.type == RequestInfo.Type.CtoS_QueuedResponse) {
            // received the response from a prev onInvokeForStoC, type:StoC_QueuedRequest, put response in queue
            RequestInfo rix = hmClientCallbackRequestInfo.remove(ri.messageId);
            if (rix != null) {
                rix.exception = ri.exception;
                rix.exceptionMessage = ri.exceptionMessage;
                rix.response = ri.response;
                processCtoSReturnValue(rix, session);
                rix.type = RequestInfo.Type.CtoS_QueuedResponse; 
                OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(rix.bind.asyncQueueName);
                cq.addMessageToQueue(rix);  // which will then set methodInvoked=true and notify orig thread
            }
            else {
                ri.exceptionMessage = "original message timed out waiting for response";
            }
            return false;            
        }
        
        if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
            OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
            int x = Math.min(1500, cq.getSize() / 2);
            cq.addMessageToQueue(ri, x, session.connectionId);
            return false;
        }
        
        ri.exception = new Exception("invalid request command, it could not be processed");
        return true;
    }

    /**
     * Logs warnings when a Client-to-Server invocation results in an
     * exception or error message.
     *
     * @param ri the RequestInfo containing results of the invocation
     */
    protected void afterInvokeForCtoS(RequestInfo ri) {
        if (ri == null) return;
        if (ri.exception != null || ri.exceptionMessage != null) {
            LOG.log(Level.WARNING, ri.toLogString(), ri.exception);
        }        
    }

    /**
     * Initializes the Server-to-Client (StoC) server socket used for
     * server-initiated method invocations on client-side remote objects.
     * Starts a dedicated accept loop thread.
     *
     * @throws Exception if the server socket cannot be created
     */
    protected void startServerSocketForStoC() throws Exception {
        if (ssStoC != null) return;
        ssStoC = multiplexerServer.createServerSocket("StoC");

        // accept new connections
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (;;) {
                    try {
                        Socket socket = ssStoC.accept();
                        onNewConnectionForStoC(socket);
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "Exception on new StoC socket", e);
                    }
                }
            }
        });
        t.setName("Remote.ServerSocket.StoC");
        t.setDaemon(true);
        t.start();
        //LOG.config("created Server to Client serversocket thread");
    }

    /**
     * Handles a newly accepted StoC virtual socket by registering it with
     * the associated Session so the server can invoke methods on client
     * remote objects.
     *
     * @param socket the accepted VirtualSocket for StoC communication
     */
    protected void onNewConnectionForStoC(Socket socket) {
    	if (!(socket instanceof VirtualSocket)) return;
        final VirtualSocket vSocket = (VirtualSocket) socket;
        int connectionId = vSocket.getConnectionId();
        Session session = getSession(connectionId, true);
        session.addSocketForStoC(vSocket);
    }

    /**
     * Creates a dynamic proxy instance representing a client-side remote
     * object, allowing the server to invoke methods on the client.
     *
     * @param session  the Session associated with the client
     * @param c        the interface implemented by the remote object
     * @param bindName the identifier of the remote object on the client
     * @return proxy instance forwarding method calls to the client
     */
    protected Object createProxyForStoC(final Session session, Class c, final String bindName) {
        Object obj = null;
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = onInvokeForStoC(proxy, session, bindName, method, args);
                return result;
            }
        };
        obj = Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, handler);
        return obj;
    }

    
    // list of requests sent to client in queue that are waiting on a return
    private ConcurrentHashMap<Integer, RequestInfo> hmClientCallbackRequestInfo = new ConcurrentHashMap<Integer, RequestInfo>();
    private AtomicInteger aiMessageId = new AtomicInteger();
    
    /**
     * Handles invocation of a method on a client-owned remote object.
     * Constructs a RequestInfo, sends the request to the client, waits for
     * the result if needed, and returns the response or exception.
     *
     * @param proxyInstance the server-side proxy instance
     * @param session       the Session representing the client
     * @param bindName      the bind name of the remote object
     * @param method        the method being invoked
     * @param args          parameters for the remote method call
     * @return the client's returned value for the invocation
     * @throws Exception if communication or remote processing fails
     */
    protected Object onInvokeForStoC(Object proxyInstance, Session session, String bindName, Method method, Object[] args) throws Exception {
        aiMethodCallCnt.incrementAndGet();
        RequestInfo ri = new RequestInfo();
        try {
            ri.connectionId = 0;
            ri.msStart = System.currentTimeMillis();
            ri.nsStart = System.nanoTime();
            ri.object = proxyInstance;
            ri.bind = getBindInfo(bindName);
            if (ri.bind == null) ri.bind = session.getBindInfo(bindName);
            ri.bindName = bindName;
            ri.method = method;
            ri.args = args;
            ri.messageId = aiMessageId.incrementAndGet();
            ri.isRemoteThread = (Thread.currentThread() instanceof OARemoteThread);
            
            onInvokeForStoC(session, ri);
        }
        catch (Exception e) {
            ri.exception = e;
        }
        finally {
            ri.nsEnd = System.nanoTime();
            if (ri.socket != null) {
                session.addSocketForStoC(ri.socket);
            }
        }
        
        afterInvokeForStoC(ri);

        if (ri.exception != null) throw ri.exception;
        if (ri.exceptionMessage != null) {
            Exception ex = new Exception(ri.exceptionMessage + ", info: " + ri.toLogString());
            throw ex;
        }
        return ri.response;
    }

    /**
     * Placeholder object used to handle calls to methods inherited from
     * Object.class when these are invoked on remote proxy instances.
     */
    private final Object stuntObject = new Object();

    /**
     * Performs the low-level execution of a Server-to-Client method
     * invocation based on information in the RequestInfo. Handles queue
     * routing, socket sends, waiting for responses, and fallback behaviors.
     *
     * @param session the Session associated with the client
     * @param ri      RequestInfo describing the remote invocation
     * @throws Exception if communication or processing fails
     */
    private void onInvokeForStoC(Session session, RequestInfo ri) throws Exception {
        if (ri.bind == null) {
            ri.bind = session.getBindInfo(ri.bindName);
            if (ri.bind == null) ri.bind = getBindInfo(ri.bindName);
            if (ri.bind == null) {
                ri.exceptionMessage = "object was removed on client (GCd)";
                return;
            }
        }        
        
        ri.methodInfo = ri.bind.getMethodInfo(ri.method);

        if (ri.methodInfo == null) {
            // check to see if method from Object.class is being invoked
            if (ri.method.getDeclaringClass().equals(Object.class)) {
                if ("equals".equals(ri.method.getName())) {
                    if (ri.args == null || ri.args.length != 1) {
                        ri.response = false;
                    }
                    else ri.response = (ri.args[0] == ri.object);
                }
                else {
                    try {
                        ri.response = ri.method.invoke(stuntObject, ri.args);
                    }
                    catch (InvocationTargetException e) {
                        Exception ex = e;
                        for (int i=0 ; i<10; i++) {
                            Throwable t = ex.getCause();
                            if (t == null || t == ex || !(t instanceof Exception)) { 
                                ri.exception = ex;
                                break;
                            }
                            ex = (Exception) t;
                            ri.exception = ex;
                        }
                    }
                    catch (Throwable tx) {
                        ri.exception = new Exception(tx.toString(), tx);
                    }
                }
            }
            else ri.exceptionMessage = "Method  not found";
            return;
        }

        
        if (ri.bind != null && ri.bind.usesQueue && (ri.methodInfo == null || !ri.methodInfo.dontUseQueue)) {
            ri.connectionId = session.connectionId;  // so that the _writeQueueMessages will send to only the client (not all clients)
  
            if (ri.methodInfo != null && ri.methodInfo.noReturnValue) {
                ri.type = RequestInfo.Type.StoC_QueuedRequestNoResponse;
                ri.response = OAReflect.getEmptyPrimitive(ri.method.getReturnType());
            }
            else {
                hmClientCallbackRequestInfo.put(ri.messageId, ri);
                ri.type = RequestInfo.Type.StoC_QueuedRequest;
            }
                
            OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
            cq.addMessageToQueue(ri);
            waitForProcessedByServer(ri);

            if (ri.type == RequestInfo.Type.StoC_QueuedRequest) {
                // need to wait for return value 
                int maxSeconds = Math.max(ri.methodInfo == null ? 0 : ri.methodInfo.timeoutSeconds, 0);
                for (int i=0; ; i++) {
                    try {
                        if (waitForMethodInvoked(ri, 1)) break;  //wait for response back from client, which puts it in the queue                      
                        if (session.bDisconnected) {
                            ri.exceptionMessage = "disconnected from remote client";
                            break;
                        }
                        if (maxSeconds > 0 && i >= maxSeconds) {
                            if (!OAObject.getDebugMode()) {
                                ri.exceptionMessage = "timeout waiting for response";
                                break;
                            }
                        }
                    }
                    catch (Exception e) {
                        ri.exception = e;
                        break;
                    }
                }
                hmClientCallbackRequestInfo.remove(ri.messageId);
            }
        }
        else {
            if (ri.methodInfo != null && ri.methodInfo.noReturnValue) {
                ri.type = RequestInfo.Type.StoC_SocketRequestNoResponse;
                ri.response = OAReflect.getEmptyPrimitive(ri.method.getReturnType());
            }
            else {
                ri.type = RequestInfo.Type.StoC_SocketRequest;
            }
            
            processStoCArguments(ri, session);
            ri.socket = session.getSocketForStoC();
    
            RemoteObjectOutputStream oos = new RemoteObjectOutputStream(ri.socket, session.hmClassDescOutput, session.aiClassDescOutput);
            oos.writeByte(ri.type.ordinal());
            oos.writeAsciiString(ri.bind.name);
            oos.writeAsciiString(ri.methodInfo.methodNameSignature);
            
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
            try {
                srvcOAThreadLocal.addObjectSerializer(session.oaObjectSerializer);
                oos.writeObject(ri.args);
            }
            finally {
                srvcOAThreadLocal.removeObjectSerializer(session.oaObjectSerializer);
            }
            oos.flush();
            oos.close(); // 20250318

            if (ri.type == RequestInfo.Type.StoC_SocketRequest) {
                RemoteObjectInputStream ois = new RemoteObjectInputStream(ri.socket, session.hmClassDescInput);
                byte b = ois.readByte();
                Object objx = ois.readObject();
                ois.close(); // 20250318
                
                if (b == 0) ri.exception = (Exception) objx;
                else if (b == 1) ri.exceptionMessage = (String) objx;
                else {
                    ri.response = objx;
                    processCtoSReturnValue(ri, session);
                }
            }      
            session.releaseSocketForStoC(ri.socket);
            ri.socket = null;
        }
        processStoCReturnValue(ri, session);
        notifyMethodInvoked(ri);
    }
    
    /**
     * Prepares outbound StoC arguments by applying compression and converting
     * remote object references to bind names. Ensures referenced objects are
     * bound and preserved for the client.
     *
     * @param ri      the RequestInfo containing argument data
     * @param session the client Session used for remote lookups
     * @throws Exception if serialization or binding setup fails
     */
    private void processStoCArguments(final RequestInfo ri, final Session session) throws Exception {
        if (ri.methodInfo.compressedParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.compressedParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams != null && ri.methodInfo.remoteParams[i] != null) continue;
                if (ri.methodInfo.compressedParams[i]) {
                    ri.args[i] = new OACompressWrapper(ri.args[i]);
                }
            }
        }
        // check to see if any of the args[] are remote objects
        if (ri.methodInfo.remoteParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.remoteParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams[i] == null) continue;
                if (ri.args[i] == null) continue;

                BindInfo bindx = getBindInfo((Object) ri.args[i]);
                if (bindx == null) bindx = session.getBindInfo((Object) ri.args[i]);
                Object objx = bindx != null ? bindx.weakRef.get() : null;
                if (bindx == null || objx == null) {
                    if (bindx == null) {
                        String bindNamex = "server." + aiBindCount.incrementAndGet();
                        
                        boolean b = ri.methodInfo.dontUseQueues != null && ri.methodInfo.dontUseQueues[i]; 
                        bindx = getBindInfo(ri.bind, bindNamex, ri.args[i], ri.methodInfo.remoteParams[i], b);
                    }
                    else {
                        bindx.setObject(ri.args[i], referenceQueue);
                    }
                }
                session.hmBindObject.put(bindx, ri.args[i]); // hold the remote object from getting GCd
                ri.args[i] = bindx.name;
            }
        }
    }

    /**
     * Variant of StoC argument processing that does not use session-level
     * lookups. Compresses parameters as configured and replaces remote
     * object references with bind names.
     *
     * @param ri the RequestInfo holding argument information
     * @throws Exception if binding or compression setup fails
     */
    private void processStoCArguments(final RequestInfo ri) throws Exception {
        if (ri.methodInfo.compressedParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.compressedParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams != null && ri.methodInfo.remoteParams[i] != null) continue;
                if (ri.methodInfo.compressedParams[i]) {
                    ri.args[i] = new OACompressWrapper(ri.args[i]);
                }
            }
        }
        // check to see if any of the args[] are remote objects
        if (ri.methodInfo.remoteParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.remoteParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams[i] == null) continue;
                if (ri.args[i] == null) continue;

                BindInfo bindx = getBindInfo((Object) ri.args[i]);
                Object objx = bindx != null ? bindx.weakRef.get() : null;
                if (bindx == null || objx == null) {
                    if (bindx == null) {
                        String bindNamex = "server." + aiBindCount.incrementAndGet();
                        boolean b = ri.methodInfo.dontUseQueues != null && ri.methodInfo.dontUseQueues[i]; 
                        bindx = getBindInfo(ri.bind, bindNamex, ri.args[i], ri.methodInfo.remoteParams[i], b);
                    }
                    else {
                        bindx.setObject(ri.args[i], referenceQueue);
                    }
                }
                hmBindObject.put(bindx, ri.args[i]); // hold the remote object from getting GCd
                ri.args[i] = bindx.name;
            }
        }
    }
    
    /**
     * Converts a StoC return value back to a server-side object instance.
     * Handles remote object proxies and decompression of return values.
     *
     * @param ri      the RequestInfo with response data
     * @param session the Session used to resolve or create bindings
     * @throws Exception if proxy creation or unwrapping fails
     */
    private void processStoCReturnValue(final RequestInfo ri, final Session session) throws Exception {
        // check to see if return value is a remote object
        if (ri.methodInfo.noReturnValue) return;
        if (ri.response != null && ri.methodInfo.remoteReturn != null) {
            String bindNamex = (String) ri.response;
            BindInfo bindx = session.getBindInfo(bindNamex);
            Object objx = bindx != null ? bindx.weakRef.get() : null;
            if (bindx == null || objx == null) {
                if (bindx == null) {
                    bindx = getBindInfo(bindNamex);
                    objx = bindx != null ? bindx.weakRef.get() : null;
                    if (objx == null) bindx = null;
                }
                else bindx = null;
                if (bindx == null) {
                    Object obj = createProxyForStoC(session, ri.methodInfo.remoteReturn, bindNamex);
                    boolean b = ri.methodInfo.dontUseQueueForReturnValue; 
                    bindx = getBindInfo(ri.bind, bindNamex, obj, ri.methodInfo.remoteReturn, b);
                }
            }
            ri.response = bindx.getObject();
        }
        else if (ri.response != null && ri.methodInfo.compressedReturn && ri.methodInfo.remoteReturn == null) {
            ri.response = ((OACompressWrapper) ri.response).getObject();
        }
    }
    
    /**
     * Logs warnings when a Server-to-Client invocation results in an error
     * or exception.
     *
     * @param ri the RequestInfo containing invocation results
     */
    protected void afterInvokeForStoC(RequestInfo ri) {
        if (ri == null) return;
        if (ri.exception != null || ri.exceptionMessage != null) {
            LOG.log(Level.WARNING, ri.toLogString(), ri.exception);
        }        
    }

    /**
     * Performs distributed garbage collection by removing bind entries whose
     * weakly referenced remote objects have been collected.
     */
    public void performDGC() {
        for (;;) {
            WeakReference ref = (WeakReference) referenceQueue.poll();
            if (ref == null) break;

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
     * Registers a local object under a bind name so that clients may look it
     * up remotely.
     *
     * @param name           the lookup name clients will use
     * @param obj            the object implementation to expose
     * @param interfaceClass the interface representing the remote contract
     */
    public void createLookup(String name, Object obj, Class interfaceClass) {
        createLookup(name, obj, interfaceClass, null, -1);
    }

    /**
     * Registers a lookup object with optional asynchronous queue support for
     * return values.
     *
     * @param name           the lookup identifier
     * @param obj            the implementation object
     * @param interfaceClass the Java interface clients will proxy
     * @param queueName      optional queue name for async responses
     * @param queueSize      maximum size of the async queue
     */
    public void createLookup(String name, Object obj, Class interfaceClass, String queueName, int queueSize) {
        BindInfo bind = getBindInfo(name, obj, interfaceClass, queueName, queueSize);
        hmBindObject.put(bind, obj);
    }

    /**
     * Removes the lookup binding associated with the given name.
     *
     * @param name the bind name to remove
     * @return true if the binding existed and was removed, false otherwise
     */
    public boolean removeLookup(String name) {
        BindInfo bind = getBindInfo(name);
        if (bind == null) return false;
        hmBindObject.remove(bind);
        hmNameToBind.remove(name);
        return true;
    }

    /**
     * Retrieves the BindInfo associated with a given bind name.
     *
     * @param name the lookup name
     * @return the BindInfo entry, or null if not found
     */
    protected BindInfo getBindInfo(String name) {
        if (name == null) return null;
        return hmNameToBind.get(name);
    }

    /**
     * Retrieves the BindInfo associated with the given implementation object.
     *
     * @param obj the object instance used for lookup
     * @return the associated BindInfo, or null if no binding exists
     */
    protected BindInfo getBindInfo(Object obj) {
        if (obj == null) return null;
        for (BindInfo bindx : hmNameToBind.values()) {
            if (bindx.weakRef != null && bindx.weakRef.get() == obj) {
                return bindx;
            }
        }
        return null;
    }

    
    /**
     * Creates or retrieves a BindInfo entry using a parent binding for
     * inheritance of queue configuration.
     *
     * @param biParent       parent BindInfo providing queue settings
     * @param name           the bind name
     * @param obj            implementation object
     * @param interfaceClass interface implemented by the object
     * @param bDontUseQueue  true to disable queue usage for this binding
     * @return the resulting BindInfo
     */
    protected BindInfo getBindInfo(BindInfo biParent, String name, Object obj, Class interfaceClass, boolean bDontUseQueue) {
        return getBindInfo(biParent, name, obj, interfaceClass, false, null, 0, bDontUseQueue);
    }

    /**
     * Creates or retrieves a BindInfo entry using explicit queue name and
     * size configuration.
     *
     * @param name           bind name identifier
     * @param obj            implementation object to expose
     * @param interfaceClass the interface clients will use
     * @param queueName      optional async queue name
     * @param queueSize      max size of the queue
     * @return the created or retrieved BindInfo
     */
    protected BindInfo getBindInfo(String name, Object obj, Class interfaceClass, String queueName, int queueSize) {
        return getBindInfo(null, name, obj, interfaceClass, false, queueName, queueSize, false);
    }
    
    /**
     * Creates or retrieves a BindInfo entry with full configuration options,
     * including broadcast support, queue settings, and inheritance of parent
     * queue parameters when applicable.
     *
     * @param biParent       optional parent BindInfo for inheriting queue settings
     * @param name           the bind name used for lookup
     * @param obj            the implementation object to bind
     * @param interfaceClass the interface representing the remote contract
     * @param bIsBroadcast   true if the bound object represents a broadcast channel
     * @param queueName      async queue name to use for method dispatch
     * @param queueSize      capacity of the async circular queue
     * @param bDontUseQueue  true to disable queue use for this binding
     * @return the created or retrieved BindInfo instance
     */
    protected BindInfo getBindInfo(final BindInfo biParent, final String name, final Object obj, final Class interfaceClass, 
    		final boolean bIsBroadcast, final String queueName, final int queueSize, final boolean bDontUseQueue) {
    	
        if (name == null || interfaceClass == null) {
            throw new IllegalArgumentException("name and interfaceClass can not be null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("interfaceClass must be a Java interface");
        }
        
        BindInfo bindx = hmNameToBind.computeIfAbsent(name, k -> {
        	String queueName2 = queueName;
        	int queueSize2 = queueSize; 
	        if (biParent != null) {
	            queueName2 = biParent.asyncQueueName;  
	            queueSize2 = biParent.asyncQueueSize;
	        }
	        
	        final BindInfo bind = new BindInfo(name, obj, interfaceClass, referenceQueue, bIsBroadcast, queueName2, queueSize2);
	        bind.loadMethodInfo();
	        
	        if (bind.usesQueue && !bDontUseQueue) {
		    	final String queueName3 = queueName2;
		        hmAsyncCircularQueue.computeIfAbsent(bind.asyncQueueName, k2 -> {
		        	OACircularQueue<RequestInfo> cqNew = new OACircularQueue<RequestInfo>(bind.asyncQueueSize) {
		                @Override
		                protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
		                    if (msSinceLastRead > 5000) return false;  // dont wait over 5 seconds
		                    Session session = getSession(sessionId, false);
		                    if (session == null) return false;
		                    if (session.bDisconnected) return false;
		                    if (session.realSocket == null) return false;
		                    if (session.realSocket.isClosed()) return false;
		                    return true;
		                }
		            };
		            cqNew.setName(queueName3);
		            return cqNew;
		        });
	        }
	        return bind;
        });
        return bindx;
    }

    /**
     * Creates a broadcast proxy allowing the server to send method calls to
     * all connected clients. Uses the specified queue configuration.
     *
     * @param bindName      name used by clients to look up the broadcast object
     * @param interfaceClass interface defining the broadcast contract
     * @param queueName     name of the async circular queue
     * @param queueSize     size of the circular queue
     * @param <T>           interface type returned
     * @return proxy instance for broadcasting method calls
     */
    public <T> T createBroadcast(final String bindName, Class<T> interfaceClass, String queueName, int queueSize) {
        return createBroadcast(bindName, null, interfaceClass, queueName, queueSize);
    }

    /**
     * Creates a broadcast channel where method calls fan out to all clients.
     * Optionally registers a callback object on the server to handle inbound
     * broadcast messages from clients.
     *
     * @param bindName       lookup name for the broadcast object
     * @param callback       optional server-side handler for client broadcasts
     * @param interfaceClass interface shared by clients and server
     * @param queueName      async queue name for broadcast messages
     * @param queueSize      size of the broadcast queue
     * @param <T>            interface type returned
     * @return proxy used to broadcast messages to all clients
     */
    public <T> T createBroadcast(String bindName, Object callback, Class<T> interfaceClass, String queueName, int queueSize) {
        if (bindName == null) throw new IllegalArgumentException("bindName can not be null");
        if (interfaceClass == null) throw new IllegalArgumentException("interfaceClass can not be null");
        if (callback != null && !interfaceClass.isAssignableFrom(callback.getClass())) {
            throw new IllegalArgumentException("callback must be same class as " + interfaceClass);
        }
        if (queueSize < 100) {
            queueSize = 100;
        }

        if (queueName == null) queueName = bindName;
        final BindInfo bind = getBindInfo(null, bindName, callback, interfaceClass, true, queueName, queueSize, false);
        if (callback != null) hmBindObject.put(bind, callback); // hold from getting gc'd

        InvocationHandler handler = new InvocationHandler() {
            int errorCnt;
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                RequestInfo ri = onInvokeBroadcast(bind, method, args);
                return ri.response;
            }
        };
        T obj = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, handler);

        // need to have the server process the queue, since clients wait for server to "catch up"
        //if (callback != null) {
        // create thread to get messages from queue
        setupBroadcastQueueReader(bind.asyncQueueName, bind.name);
        return obj;
    }


    /**
     * Handles a server-originated broadcast method invocation. Prepares
     * arguments, packages the request, enqueues it for all clients, and waits
     * if necessary until the server has processed the message.
     *
     * @param bind   BindInfo for the broadcast object
     * @param method method invoked on the broadcast proxy
     * @param args   parameters for the broadcast call
     * @return the populated RequestInfo representing the invocation
     * @throws Exception if argument processing, queuing, or dispatch fails
     */
    protected RequestInfo onInvokeBroadcast(BindInfo bind, Method method, Object[] args) throws Exception {
        aiMethodCallCnt.incrementAndGet();
        RequestInfo ri = new RequestInfo();
        ri.connectionId = 0;
        ri.msStart = System.currentTimeMillis();
        ri.nsStart = System.nanoTime();
        ri.bindName = bind.name;
        ri.method = method;
        ri.args = args;
        ri.bind = bind;
        ri.type = RequestInfo.Type.StoC_QueuedBroadcast;
        ri.isRemoteThread = (Thread.currentThread() instanceof OARemoteThread);

      //qqqqqqqqqqqqvvvvvvvvvv 20260403           
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
   		ri.replicationSource = srvcOAThreadLocal.getReplicationSource(); 
        
        
        ri.methodInfo = ri.bind.getMethodInfo(ri.method);
        ri.object = ri.bind.getObject();

        // 20180225
        if (ri.bind.isOASync) {
            srvcOAThreadLocal.incrOASyncEventCount();
        }
        
        if (ri.methodInfo == null) {
            // check to see if method from Object.class is being invoked
            if (ri.method.getDeclaringClass().equals(Object.class)) {
                if ("equals".equals(ri.method.getName())) {
                    if (ri.args == null || ri.args.length != 1) {
                        ri.response = false;
                    }
                    else ri.response = (ri.args[0] == ri.object);
                }
                else {
                    try {
                        srvcOAThreadLocal.setRemoteRequestInfo(ri);
                        ri.response = ri.method.invoke(stuntObject, ri.args);
                    }
                    catch (InvocationTargetException e) {
                        Exception ex = e;
                        for (int i=0 ; i<10; i++) {
                            Throwable t = ex.getCause();
                            if (t == null || t == ex || !(t instanceof Exception)) { 
                                ri.exception = ex;
                                break;
                            }
                            ex = (Exception) t;
                            ri.exception = ex;
                        }
                    }
                    catch (Throwable tx) {
                        ri.exception = new Exception(tx.toString(), tx);
                    }
                    srvcOAThreadLocal.setRemoteRequestInfo(null);
                }
            }
            else ri.exceptionMessage = "Method  not found";
            return ri;
        }

        processStoCArguments(ri);        
        if (ri.response == null) ri.response = OAReflect.getEmptyPrimitive(ri.method.getReturnType());


        Thread t = Thread.currentThread();
        RequestInfo rix;
        if (ri.isRemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            rix = rt.requestInfo;
        }
        else rix = null;

        // put "ri" in circular queue for clients to pick up.       
        OACircularQueue<RequestInfo> cque = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
        
        int x;
        if (rix == null) {  
            // command running on the server
            x = Math.min(1000, cque.getSize() / 2);
            cque.addMessageToQueue(ri, x, 0);  // this will throttle
        }
        else {
            // command running because of a client request (rix) that triggered that message           
            x = Math.min(650, cque.getSize() / 2);
            cque.addMessageToQueue(ri, x, rix.connectionId);  // this will throttle
        }
        
        if (rix == null) {
            waitForProcessedByServer(ri);
        }
        
        ri.nsEnd = System.nanoTime();
        notifyMethodInvoked(ri);
        afterInvokeForStoC(ri);
        
        return ri;
    }

    private ConcurrentHashMap<String, String> hmAsyncQueue = new ConcurrentHashMap<String, String>();

    /**
     * Sets up a dedicated server thread that reads messages from a broadcast
     * async queue and processes them on the server, allowing clients to
     * remain synchronized with server dispatch.
     *
     * @param asyncQueueName name of the broadcast async queue
     * @param bindName       name of the associated broadcast binding
     */
    protected void setupBroadcastQueueReader(final String asyncQueueName, final String bindName) {
        final OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(asyncQueueName);
        if (cq == null) throw new RuntimeException("circular queue, name="+asyncQueueName+", does not exist");
        synchronized (hmAsyncQueue) {
            if (hmAsyncQueue.get(asyncQueueName) != null) return;
            hmAsyncQueue.put(asyncQueueName, "");
        }

        final long qPos = cq.registerSession(0);

        // set up thread that will get messages from queue and send to client
        final String threadName = "Remote.ServerQueueProcessor." + asyncQueueName;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    try {
                        processQueueMessagesOnServer(cq, bindName, qPos);
                    }
                    catch (Exception e) {
                        String s = "processQueueMessagesOnServer thread exception, thread="+threadName+", queue=" + asyncQueueName;
                        LOG.log(Level.WARNING, s, e);
                    }
                }
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Reads queued messages from the broadcast or async queue and executes
     * their server-side handling, including invoking methods, updating state,
     * and waking waiting threads when necessary.
     *
     * @param cque     the async or broadcast circular queue
     * @param bindName name of the associated binding
     * @param qpos     starting queue position for reading
     * @throws Exception if queue reading or message processing fails
     */
    private void processQueueMessagesOnServer(final OACircularQueue<RequestInfo> cque, final String bindName, long qpos) throws Exception {
        if (cque == null) return;
        for (;;) {
            RequestInfo[] ris;
            ris = cque.getMessages(0, qpos, 20, 10000);
            
            if (ris == null) {
                continue;
            }
            
            qpos += ris.length;
            
            for (RequestInfo ri : ris) {
                if (ri == null) {
                    continue;
                }

                boolean bNotifyMethodInvoked = false;

                if (ri.type == RequestInfo.Type.CtoS_QueuedRequest) {
                    // 4:CtoS_QueuedRequest invoke
                    invokeUsingRemoteThread(ri, true);
                }
                else if (ri.type == RequestInfo.Type.CtoS_QueuedRequestNoResponse) {
                    // 4:CtoS_QueuedRequestNoResponse
                    invokeUsingRemoteThread(ri, true);
                    // no clients get this
                }
                else if (ri.type == RequestInfo.Type.CtoS_QueuedResponse) {
                    bNotifyMethodInvoked = true; // waiting thread will wake up on ri.notifyAll()
                    // clients need to ignore this
                    // client is returning value for a S2C request
                }
                else if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
                    invokeUsingRemoteThread(ri, true);
                }
                else if (ri.type == RequestInfo.Type.StoC_QueuedRequest) {
                    // only one client gets this
                }
                else if (ri.type == RequestInfo.Type.StoC_QueuedRequestNoResponse) {
                    // only one client gets this
                }
                else if (ri.type == RequestInfo.Type.StoC_QueuedBroadcast) {
                   if (ri.methodInfo != null && ri.methodInfo.runInRemoteThread) {
                       invokeUsingRemoteThread(ri, true);
                   }
                }
                else if (ri.type == RequestInfo.Type.StoC_QueuedResponse) {
                    // 8:CtoS_QueuedRequest flag as processed
                }
                
                if (!ri.processedByServerQueue) {
                    notifyProcessedByServer(ri);
                }
                if (bNotifyMethodInvoked) {
                    notifyMethodInvoked(ri);
                }
            }
        }
    }
    
    
    private AtomicInteger aiRemoteClientThreadPos = new AtomicInteger();
    
    /**
     * Dispatches a Client-to-Server request to a dedicated OARemoteThread
     * for execution. Waits for completion unless invoked from a queue
     * processing thread.
     *
     * @param ri                       request to invoke in a remote thread
     * @param bFromServerQueueThread   true if invoked by the queue processor
     * @throws Exception if thread dispatch or execution fails
     */
    protected void invokeUsingRemoteThread(final RequestInfo ri, boolean bFromServerQueueThread) throws Exception {
        if (ri == null) return;
        if (ri.methodInvoked) return; 
        // sent by client, invoke method on object
        Object obj = ri.bind.getObject();
        if (obj == null) {
            if (ri.exceptionMessage == null) ri.exceptionMessage = "remote object impl is null";
            notifyMethodInvoked(ri);
            return;
        }

        // 5:CtoS_QueuedRequest remoteThread invokes the request
        // 5:CtoS_QueuedRequestNoResponse
        
        OARemoteThread remoteThread = null;
        synchronized (alRemoteClientThread) {    
            for (int cnt=0; ; cnt++) {
                int x = alRemoteClientThread.size();
                for (int i=0; i<x; i++) {
                    OARemoteThread rct = alRemoteClientThread.get( aiRemoteClientThreadPos.incrementAndGet()%x );
                    synchronized (rct.Lock) {
                        if (rct.requestInfo == null) {
                            remoteThread = rct;
                            rct.requestInfo = ri;
                            rct.Lock.notifyAll(); 
                            break;
                        }
                    }
                }
                if (remoteThread != null || x < 50 || cnt > 5) break;
                try {
                    alRemoteClientThread.wait(50);
                }
                catch (Exception e) {
                }
            }
        }

        if (remoteThread == null) {
            remoteThread = createRemoteClientThread();
            synchronized (alRemoteClientThread) {    
                alRemoteClientThread.add(remoteThread);
                synchronized (remoteThread.Lock) {
                    remoteThread.requestInfo = ri;
                    remoteThread.Lock.notifyAll(); 
                }
            }
            if (alRemoteClientThread.size() > 50) {
                LOG.warning("alRemoteClientThread.size() = " + alRemoteClientThread.size());
            }
        }
        
        int maxSeconds = Math.max(ri.methodInfo == null ? 0 : ri.methodInfo.timeoutSeconds, 0); 
        long ms1 = System.currentTimeMillis();

        // remoteThread is now processing the request
        
        if (bFromServerQueueThread) {  // if true, then need to get back to queue asap
            if (ri.bind.isOASync) {
                // note: the remoteThread.startNextThread will call notifyProcessedByServer 
                waitForProcessedByServer(ri);
            }
            // need to continue to get requests from the queue
            return;
        }
        
        if ((ri.type != RequestInfo.Type.CtoS_SocketRequestNoResponse) && (ri.type != RequestInfo.Type.CtoS_ReturnOnQueueSocket)) {
            // the calling thread is waiting for this request to be completed
            for (;;) {
                if (waitForMethodInvoked(ri, maxSeconds)) break;
                if (!OAObject.getDebugMode()) {
                    ri.exceptionMessage = "timeout waiting for response";
                    break;
                }
            }

            long ms2 = System.currentTimeMillis();
            // this can be removed, sanity check only
            if (maxSeconds > 0 && (ms2-ms1) >= (maxSeconds * 1000L)) {
                StackTraceElement[] stes = remoteThread.getStackTrace();
                Exception ex = new Exception();
                ex.setStackTrace(stes);
                LOG.log(Level.WARNING, "timeout waiting for message, will continue, this is stacktrace for the remoteThread, request="
                        + ri.toLogString(), ex);
            }
            ri.nsEnd = System.nanoTime();
        }
    }

    /**
     * Performs the actual method invocation on a remote thread, including
     * argument preparation, method execution, error handling, and packaging
     * of the return value.
     *
     * @param rt      the OARemoteThread executing the request
     * @param ri      the RequestInfo describing the invocation
     * @param session client session for resolving remote arguments
     * @throws Exception if the invoked method or related processing fails
     */
    protected void _invokeByRemoteThread(final OARemoteThread rt, final RequestInfo ri, final Session session) throws Exception {
        if (ri == null) return;

        if (ri.methodInfo == null) {
            if (ri.exceptionMessage == null) ri.exceptionMessage = "method not found";
            return;
        }
        
        processCtoSArguments(ri, session);
        
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
        int x = srvcOAThreadLocal.getOASyncEventCount();
        try {
            srvcOAThreadLocal.setRemoteRequestInfo(ri);
            // 20180225 added code for threadlocal.oasynceventcount
            ri.response = ri.method.invoke(ri.bind.getObject(), ri.args);
        }
        catch (InvocationTargetException e) {
            Exception ex = e;
            for (int i=0 ; i<10; i++) {
                Throwable t = ex.getCause();
                if (t == null || t == ex || !(t instanceof Exception)) { 
                    ri.exception = ex;
                    break;
                }
                ex = (Exception) t;
                ri.exception = ex;
            }
        }
        catch (Throwable tx) {
            ri.exception = new Exception(tx.toString(), tx);
        }
        int x2 = srvcOAThreadLocal.getOASyncEventCount();
        ri.bHadOASyncEvent = (x != x2);
        srvcOAThreadLocal.setRemoteRequestInfo(null);
        processCtoSReturnValue(ri, session);
        ri.nsEnd = System.nanoTime();

        if (ri.type == RequestInfo.Type.CtoS_QueuedRequest) {
            // 7:CtoS_QueuedRequest put result back in queue
            ri.type = RequestInfo.Type.StoC_QueuedResponse;
            OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(ri.bind.asyncQueueName);
            cq.addMessageToQueue(ri);
        }
        
        if (ri.type == RequestInfo.Type.CtoS_SocketRequestNoResponse) {
            // thread is not waiting
        }
        else if (ri.type == RequestInfo.Type.CtoS_ReturnOnQueueSocket) {
            if (ri.exceptionMessage != null) {
                ri.methodInvoked = true;
            }
            session.setupAsyncQueueSender(ri.bind.asyncQueueName);
            try {
                session.writeOnQueueSocket(ri);
            }
            catch (Exception e) {
                ri.exception = e;
            }
        }
        else {
            // notify waiting thread
            notifyMethodInvoked(ri);
        }
        afterInvokeForCtoS(ri);
    }
    
    /**
     * Converts inbound CtoS method arguments by unwrapping compression
     * wrappers and resolving remote object references to local proxy
     * instances.
     *
     * @param ri      RequestInfo holding inbound arguments
     * @param session session used for resolving client-bound remote objects
     * @throws Exception if deserialization or proxy creation fails
     */
    private void processCtoSArguments(final RequestInfo ri, final Session session) throws Exception {
        if (ri.methodInfo.compressedParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.compressedParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams != null && ri.methodInfo.remoteParams[i] != null) continue;
                if (!ri.methodInfo.compressedParams[i]) continue;
                ri.args[i] = ((OACompressWrapper) ri.args[i]).getObject();
            }
        }

        // check to see if any of the args[] are remote objects
        if (session != null && ri.methodInfo.remoteParams != null && ri.args != null) {
            for (int i = 0; i < ri.methodInfo.remoteParams.length && i < ri.args.length; i++) {
                if (ri.methodInfo.remoteParams[i] == null) continue;
                // convert the param to real object (proxy)
                final String bindName = (String) ri.args[i];
                if (bindName == null) continue;
                BindInfo bindx = session.getBindInfo(bindName);
                Object objx = bindx != null ? bindx.weakRef.get() : null;
                if (bindx == null || objx == null) {
                    if (bindx != null) {
                        bindx = getBindInfo(bindName);
                        objx = bindx != null ? bindx.weakRef.get() : null;
                        if (objx == null) { // object was gc'd
                            bindx = null;
                        }
                    }
                    else bindx = null;
                    if (bindx == null) {
                        Object obj = createProxyForStoC(session, ri.methodInfo.remoteParams[i], bindName);
                        bindx = session.createBindInfo(ri.bind, bindName, obj, ri.methodInfo.remoteParams[i]);
                    }
                }
                ri.args[i] = bindx.getObject();
            }
        }
    }
    
     
    /**
     * Processes return values from server-side method executions to determine
     * whether they must be sent as compressed data or transformed into remote
     * object bindings for the client.
     *
     * @param ri      RequestInfo containing return data
     * @param session client session receiving the return value
     * @throws Exception if binding or compression conversion fails
     */
    private void processCtoSReturnValue(final RequestInfo ri, final Session session) throws Exception {
        // check the return value to see if it is a remote object, and if it needs compression
        if (ri.methodInfo.noReturnValue) return;
        
        if (session != null && ri.response != null && ri.methodInfo.remoteReturn != null) {
            BindInfo bindx = getBindInfo(ri.response);
            Object objx = bindx != null ? bindx.weakRef.get() : null; // make sure obj is not gc'd
            if (bindx == null || objx == null) {
                if (bindx == null) {
                    bindx = session.getBindInfo(ri.response);
                    objx = bindx != null ? bindx.weakRef.get() : null;
                    if (objx == null) { // object was gc'd
                        bindx = null;
                    }
                }
                if (bindx == null) {
                    // make remote
                    String bindNamex = "server." + aiBindCount.incrementAndGet(); // this will be sent to client
                    bindx = session.createBindInfo(ri.bind, bindNamex, ri.response, ri.methodInfo.remoteReturn);
                }
            }
            ri.responseBindName = bindx.name; // this will be returned to client
            ri.responseBindUsesQueue = bindx.usesQueue && !ri.methodInfo.dontUseQueueForReturnValue;
            session.hmBindObject.put(bindx, ri.response); // make sure it wont get gc'd
        }
        else if (ri.methodInfo.compressedReturn && ri.methodInfo.remoteReturn == null) {
            ri.response = new OACompressWrapper(ri.response);
        }
    }

    
    
    // use OARemoteThread to process broadcast messages on the server
    /**
     * Counter used to assign unique identifiers to created remote client
     * threads.
     */
    private final AtomicInteger aiClientThreadCount = new AtomicInteger();

    /**
     * Thread pool of OARemoteThread instances used to process asynchronous
     * Client-to-Server requests.
     */
    private final ArrayList<OARemoteThread> alRemoteClientThread = new ArrayList<OARemoteThread>();

    /**
     * Creates and starts a new OARemoteThread used to execute queued
     * Client-to-Server requests asynchronously. Manages thread naming and
     * initialization.
     *
     * @return the newly created remote thread
     */
    private OARemoteThread createRemoteClientThread() {
        OARemoteThread t = new OARemoteThread() {
            @Override
            public void run() {
                for ( ;!stopCalled; ) {
                    try {
                        if (shouldClose(this)) break;
                        synchronized (Lock) {
                            if (requestInfo == null) {
                                if (alRemoteClientThread.size() > 15) {
                                    Lock.wait(1000);
                                }
                                else Lock.wait(10000);
                                if (requestInfo == null) continue;
                            }
                        }
                        
                        Session session;
                        if (requestInfo.connectionId != 0) {
                            session = getSession(requestInfo.connectionId, false);
                        }
                        else session = null;
                        // 6:CtoS_QueuedRequest invoke
                        // 6:CtoS_QueuedRequestNoResponse

                        setDefaultSendSyncMessages(requestInfo.bind.isBroadcast == false);
                        reset();
                        
                        _invokeByRemoteThread(this, requestInfo, session);
                    }
                    catch (Exception e) {
                        String s = requestInfo == null ? "null" : requestInfo.toLogString();
                        LOG.log(Level.WARNING, "error in remoteThread loop, will continue. requestInfo="+s, e);
                    }

                    this.msLastUsed = System.currentTimeMillis();
                    synchronized (Lock) {
                        if (requestInfo != null) {
                            if (!requestInfo.processedByServerQueue) {
                                notifyProcessedByServer(requestInfo);
                            }
                            this.requestInfo = null;
                        }
                        Lock.notifyAll();
                    }
                }
            }

            @Override
            public void startNextThread() {
                if (startedNextThread) return;
                super.startNextThread();
                if (requestInfo != null) {
                    if (!requestInfo.processedByServerQueue) {
                        notifyProcessedByServer(requestInfo);
                    }
                }
            }
        };
        t.setName("Remote.RemoteThread." + aiClientThreadCount.getAndIncrement());
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Determines whether a remote thread should be terminated based on idle
     * duration, thread pool size, and system load.
     *
     * @param remoteThread the thread to evaluate for shutdown
     * @return true if the thread should be closed, false otherwise
     */
    private boolean shouldClose(final OARemoteThread remoteThread) {
        final int x = alRemoteClientThread.size();
        if (x < 4) return false;
        int max;
        if (x > 100) max = 100;
        else if (x > 50) max = 500;
        else max = 1000;
        if (remoteThread.requestInfo != null) return false;            
        
        if (remoteThread.msLastUsed == 0 || (remoteThread.msLastUsed + max > System.currentTimeMillis()) ) return false;
        synchronized (alRemoteClientThread) {
            if (remoteThread.requestInfo != null) return false;            
            if (alRemoteClientThread.size() < 4) return false;
            
            int cntUsed = 0;
            for (OARemoteThread rt : alRemoteClientThread) {
                if (rt.requestInfo != null) cntUsed++;
            }
            if (cntUsed + 3 > x) return false;
            
            alRemoteClientThread.remove(remoteThread);
            remoteThread.stopCalled = true;
        }
        return true;
    }
    
    
    /**
     * Handles exceptions occurring during remote processing. The default
     * implementation does nothing; subclasses may override to provide custom
     * logging or handling.
     *
     * @param connectionId    affected client connection
     * @param title           short description of the failure
     * @param msg             detailed message
     * @param e               exception thrown
     * @param bWillDisconnect true if the session is expected to disconnect
     */
    protected void onException(int connectionId, String title, String msg, Exception e, boolean bWillDisconnect) {
    }

    
    /**
     * Holds state and stream information for a VirtualSocket used for async
     * message delivery, including output stream lifecycle, write counters,
     * and reset logic.
     */
    private  static class VirtualSocketInfo {
        VirtualSocket vs;
        RemoteObjectOutputStream oos;
        private long tsLastReset = System.currentTimeMillis();
        int cntWrite;
        int cntUnflushed;
        
        /**
         * Constructs a VirtualSocketInfo instance and initializes its state,
         * including resetting output stream counters and timestamps.
         */
        public VirtualSocketInfo() {
            reset();
        }
        
        /**
         * Determines whether the associated ObjectOutputStream should be closed
         * and reset based on write activity and elapsed time. Helps control
         * object stream cache growth.
         *
         * @return true if the output stream should be reset, false otherwise
         */
        public boolean shouldClose() {
            // note: oos internally has a reference cache ([]) that can get large - need to limit how long it lasts. 
            if (oos == null) return false;
            if (cntWrite == 0) return false;
            if (cntWrite > 50) return true;
            if (tsLastReset + 5000 < System.currentTimeMillis()) return true;
            return false;
        }
        
        /**
         * Resets the VirtualSocketInfo by flushing and closing its current
         * ObjectOutputStream, clearing counters, and updating the timestamp for
         * the last reset.
         */
        public void reset() {
            if (oos != null) {
                try {
                    oos.flush();
                    oos.close();
                }
                catch (IOException ex)  {}
                oos = null;
            }
            tsLastReset = System.currentTimeMillis();
            cntWrite = 0;
            cntUnflushed = 0;
        }
    }
    
    /**
     * Represents a connected client session. Tracks sockets, queued message
     * senders, remote object bindings, serialization context, and connection
     * state.
     */
    public class Session {
    	/**
    	 * Identifier assigned by the multiplexer representing this client
    	 * session's connection.
    	 */
        public int connectionId;

        /**
         * The underlying real (non-virtual) socket associated with this session,
         * used primarily for connection state validation.
         */
        public Socket realSocket;

        /**
         * Flag indicating whether the session has been disconnected. Used to
         * prevent further socket operations after a disconnect event.
         */
        private volatile boolean bDisconnected;
        
        /**
         * Counter tracking the total number of messages received by this
         * session. Used for diagnostic and monitoring purposes.
         */
        int cntTotalMsgs;        

        /**
         * Counter tracking the total number of messages sent from this session
         * to the client.
         */
        int cntTotalMsgsSent;        
        
        /**
         * Mapping of async queue names to VirtualSocketInfo instances used for
         * sending queued messages back to the client.
         */
        private HashMap<String, VirtualSocketInfo> hmAsyncQueueSocket = new HashMap<String, VirtualSocketInfo>();  

        /**
         * Tracks GUIDs of OAObjects serialized for this session. The Boolean
         * value indicates whether all references for that object have been sent.
         */
        private final ConcurrentHashMap<UUID, Boolean> hmGuid = new ConcurrentHashMap(); 

        /**
         * Serializer instance configured for this session to manage OAObject
         * serialization behavior and reference tracking.
         */
        private final OAObjectSerializer oaObjectSerializer;
        
        public Session() {
            oaObjectSerializer = new OAObjectSerializer(null, false, new OAObjectSerializerCallback() {
                @Override
                public void beforeSerialize(OAObject obj) {
                    UUID x = obj.getGuid();
                    hmGuid.putIfAbsent(x, false);
                }
                @Override
                public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, boolean bDefault) {
                    return false; // ignore all
                }
            }); 
        }
        
        /**
         * Returns the GUID tracking map used to record which objects and
         * references have been serialized for this session.
         *
         * @return map of GUIDs to serialization-completion flags
         */
        public ConcurrentHashMap<UUID, Boolean> getGuidHashMap() {
            return hmGuid;
        }
        
        /**
         * Cache used to optimize class descriptor output during object
         * serialization, mapping class names to assigned descriptor IDs.
         */
        ConcurrentHashMap<String, Integer> hmClassDescOutput = new ConcurrentHashMap<String, Integer>();

        /**
         * Counter used to assign unique class descriptor IDs for serialized
         * classes sent to the client.
         */
        AtomicInteger aiClassDescOutput = new AtomicInteger();
        
        /**
         * Cache of incoming class descriptors indexed by descriptor ID, used
         * during RemoteObjectInputStream deserialization.
         */
        ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDescInput = new ConcurrentHashMap<Integer, ObjectStreamClass>();


        /**
         * Local mapping of bind names to BindInfo objects for remote objects
         * created or referenced within this session.
         */
        ConcurrentHashMap<String, BindInfo> hmNameToBind = new ConcurrentHashMap<String, BindInfo>();

        /**
         * List of available VirtualSockets that the server can use to send
         * Server-to-Client (StoC) method invocation messages.
         */
        ArrayList<VirtualSocket> alSocketFromStoC = new ArrayList<VirtualSocket>();

        /**
         * Strong references to objects associated with BindInfo entries within
         * this session, preventing premature garbage collection.
         */
        ConcurrentHashMap<BindInfo, Object> hmBindObject = new ConcurrentHashMap<BindInfo, Object>();

        /**
         * Tracks async queues for which sender threads have been created for this
         * session. The presence of a key indicates the queue is active.
         */
        ConcurrentHashMap<String, String> hmAsyncQueue = new ConcurrentHashMap<String, String>();

        /**
         * Retrieves a session-specific BindInfo for the supplied name.
         *
         * @param name bind name
         * @return matching BindInfo or null if not found
         */
        protected BindInfo getBindInfo(String name) {
            if (name == null) return null;
            return hmNameToBind.get(name);
        }

        /**
         * Removes and returns the BindInfo entry associated with the given bind
         * name from this session.
         *
         * @param name bind identifier
         * @return removed BindInfo instance, or null if not present
         */
        protected BindInfo removeBindInfo(String name) {
            if (name == null) return null;
            return hmNameToBind.remove(name);
        }

        /**
         * Retrieves the BindInfo associated with the supplied implementation
         * object by comparing weak reference targets.
         *
         * @param obj implementation object to match
         * @return matching BindInfo or null if none found
         */
        protected BindInfo getBindInfo(Object obj) {
            if (obj == null) return null;
            for (BindInfo bindx : hmNameToBind.values()) {
                if (obj.equals(bindx.weakRef.get())) {
                    return bindx;
                }
            }
            return null;
        }

        /**
         * Marks the session as disconnected and notifies any threads waiting on
         * StoC socket availability.
         */
        void onDisconnect() {
            synchronized (alSocketFromStoC) {
                bDisconnected = true;
                alSocketFromStoC.notifyAll();
            }
        }

        /**
         * Obtains an available StoC VirtualSocket for sending method invocation
         * requests to the client. If none are available, waits briefly for one to
         * be added. May request that the client create additional StoC sockets.
         *
         * @return a usable VirtualSocket for StoC communication
         * @throws Exception if no socket becomes available or the session is disconnected
         */
        public VirtualSocket getSocketForStoC() throws Exception {
            VirtualSocket socket = null;
            boolean bWaitedForFirst = false;
            for (int i = 0; socket == null; i++) {
                boolean bRequestNew = false;
                synchronized (alSocketFromStoC) {
                    if (bDisconnected) {
                        throw new Exception("closed connection/session=" + connectionId);
                    }
                    int x = alSocketFromStoC.size();
                    if (x > 0) {
                        socket = alSocketFromStoC.remove(0);
                        if (x == 1) bRequestNew = true;
                    }
                    else if (!bWaitedForFirst) {
                        alSocketFromStoC.wait(250);
                        bWaitedForFirst = true;
                    }
                    else if (i > 50) {
                        throw new Exception("no StoC sockets available for connection/session=" + connectionId);
                    }
                    else {
                        alSocketFromStoC.wait(100);
                    }
                }
                if (bRequestNew) {
                    RemoteObjectOutputStream oos = new RemoteObjectOutputStream(socket);
                    oos.writeByte(RequestInfo.Type.StoC_CreateNewStoCSocket.ordinal());
                    oos.flush();
                    oos.close(); // 20250318
                }
            }
            return socket;
        }

        /**
         * Returns a VirtualSocket back to the StoC pool if capacity allows. If
         * too many sockets are already in the pool, the socket is closed.
         *
         * @param socket the StoC VirtualSocket to release
         * @throws Exception if closing the socket fails
         */
        public void releaseSocketForStoC(VirtualSocket socket) throws Exception {
            if (socket == null) return;
            if (socket.isClosed()) return;
            synchronized (alSocketFromStoC) {
                if (alSocketFromStoC.size() < 3) {
                    alSocketFromStoC.add(socket);
                    alSocketFromStoC.notifyAll();
                    return;
                }
            }
            socket.close();
        }

        /**
         * Adds a newly accepted StoC VirtualSocket to the pool of available
         * sockets and wakes any threads waiting for a socket.
         *
         * @param socket the StoC VirtualSocket to add
         */
        public void addSocketForStoC(VirtualSocket socket) {
            if (socket == null) return;
            // LOG.fine("connectionId="+connectionId+", vid="+socket.getId());
            synchronized (alSocketFromStoC) {
                alSocketFromStoC.add(socket);
                alSocketFromStoC.notifyAll();
            }
        }

        /**
         * Creates a session-local BindInfo for a remote object originating from
         * the client. Inherits queue settings from the parent BindInfo.
         *
         * @param biParent       parent binding holding queue properties
         * @param name           bind name for the remote object
         * @param obj            proxy object for the client-side instance
         * @param interfaceClass interface representing the remote contract
         * @return the created BindInfo associated with this session
         */
        public BindInfo createBindInfo(BindInfo biParent, String name, Object obj, Class interfaceClass) {
            if (name == null || interfaceClass == null) {
                throw new IllegalArgumentException("name and interfaceClass can not be null");
            }
            if (!interfaceClass.isInterface()) {
                throw new IllegalArgumentException("interfaceClass must be a Java interface");
            }
            
            BindInfo bind = new BindInfo(name, obj, interfaceClass, null, false, 
                    biParent.asyncQueueName, biParent.asyncQueueSize); 

            bind.loadMethodInfo();
            hmNameToBind.put(name, bind);
            return bind;
        }
        
        /**
         * Writes a queued return value or response message to the client's async
         * queue socket using the session's OAObjectSerializer context.
         *
         * @param ri the RequestInfo containing response data to be sent
         * @throws Exception if writing to the queue socket fails
         */
        public void writeOnQueueSocket(final RequestInfo ri) throws Exception {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
            try {
                srvcOAThreadLocal.addObjectSerializer(oaObjectSerializer);
                _writeOnQueueSocketX(ri);
            }
            finally {
                srvcOAThreadLocal.removeObjectSerializer(oaObjectSerializer);
            }
        }
        
        /**
         * Low-level implementation for sending a queued response over the
         * associated VirtualSocket. Handles stream initialization, message
         * framing, and reset logic for object output streams.
         *
         * @param ri the RequestInfo to send
         * @throws Exception if socket output or stream operations fail
         */
        protected void _writeOnQueueSocketX(final RequestInfo ri) throws Exception {
            String qname = ri.bind.asyncQueueName;

            VirtualSocketInfo vsi = hmAsyncQueueSocket.get(qname);
            if (vsi == null) {
                ri.exceptionMessage = "message queue does not have a virtualSocket, qname="+qname;
                return;
            }
            
            final VirtualSocket vsocket = vsi.vs;
            
            synchronized (vsocket) {
                if (vsi.oos == null) {
                    vsi.oos = new RemoteObjectOutputStream(vsi.vs, hmClassDescOutput, aiClassDescOutput);
                    vsi.oos.writeByte(RequestInfo.Type.StoC_StartObjectInputStream.ordinal());
                }
                
                vsi.oos.writeByte(ri.type.ordinal());
                
                if (ri.exception != null) {
                    vsi.oos.writeByte(0);
                    vsi.oos.writeObject(ri.exception);
                }
                else if (ri.exceptionMessage != null) {
                    vsi.oos.writeByte(1);
                    vsi.oos.writeObject(ri.exceptionMessage);
                }
                else if (ri.responseBindName != null) {
                    vsi.oos.writeByte(2);
                    vsi.oos.writeObject(new Object[] {ri.responseBindName, ri.responseBindUsesQueue} );
                }
                else {
                    vsi.oos.writeByte(3);
                    vsi.oos.writeObject(ri.response);
                }
                vsi.oos.writeInt(ri.messageId);
                
                // flush to stream
                vsi.cntWrite++;
                if (vsi.shouldClose()) {
                    vsi.oos.writeByte(RequestInfo.Type.StoC_CloseObjectInputStream.ordinal());
                    vsi.reset();
                }
                else {
                    vsi.oos.flush();
                }
                vsi.cntUnflushed = 0;
            }
        }
        
        
        /**
         * Initializes and starts a dedicated thread for sending asynchronous
         * queue messages to the client for the specified queue. Ensures only one
         * sender thread exists per queue per session.
         *
         * @param asyncQueueName name of the async queue to send messages for
         */
        public void setupAsyncQueueSender(final String asyncQueueName) {
            if (hmAsyncQueue.get(asyncQueueName) != null) return;
            synchronized (hmAsyncQueue) {
                if (hmAsyncQueue.get(asyncQueueName) != null) return;

                hmAsyncQueue.put(asyncQueueName, "");
                final OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(asyncQueueName);
                final long qPos = cq.registerSession(connectionId);
                
                // set up thread that will get messages from queue and send to client
                final String threadName = "Remote.Client." + connectionId + ".circQueWriter." + asyncQueueName;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            writeQueueMessages(asyncQueueName, cq, qPos);
                        }
                        catch (Exception e) {
                            if (realSocket != null && !realSocket.isClosed()) {
                                String s = "async queue thread exception, thread=" + threadName + ", thread is stopping, "
                                        + "which will stop message from being sent to this client, queue=" + asyncQueueName;
                                LOG.log(Level.WARNING, s, e);
                            }
                        }
                    }
                });
                t.setName(threadName);
                t.setDaemon(true);
                t.start();
            }
        }

        /**
         * Establishes the StoC VirtualSocket used for sending queued messages
         * and delegates to the internal loop that streams queued RequestInfo
         * entries to the client.
         *
         * @param asyncQueueName name of the async queue
         * @param cque           circular queue holding messages
         * @param startQuePos    initial read position for the queue
         * @throws Exception if socket setup or message streaming fails
         */
        private void writeQueueMessages(final String asyncQueueName,  final OACircularQueue<RequestInfo> cque, final long startQuePos) throws Exception {
            final VirtualSocket vsocket = getSocketForStoC();
            
            VirtualSocketInfo vsi = new VirtualSocketInfo();
            vsi.vs = vsocket;
            
            hmAsyncQueueSocket.put(asyncQueueName, vsi);

			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
            try {
                srvcOAThreadLocal.addObjectSerializer(oaObjectSerializer);
                _writeQueueMessages(cque, vsi, startQuePos);
            }
            finally {
                srvcOAThreadLocal.removeObjectSerializer(oaObjectSerializer);
                cque.unregisterSession(connectionId);
                releaseSocketForStoC(vsocket);
            }
        }
        
        /**
         * Continuously reads queued RequestInfo messages from the circular queue
         * and writes them to the client using the provided VirtualSocketInfo.
         * Handles batching, flushing, keep-alive behavior, and queue position
         * progression.
         *
         * @param cque the circular queue containing messages
         * @param vsi  VirtualSocketInfo managing socket output streams
         * @param qpos starting queue position for iteration
         * @throws Exception if socket output fails or the connection closes
         */
        private void _writeQueueMessages(final OACircularQueue<RequestInfo> cque, final VirtualSocketInfo vsi, long qpos)
                throws Exception {
            
            final VirtualSocket vsocket = vsi.vs;
            final int connectionId = vsocket.getConnectionId();
            final HashSet<Integer> hsQueuedRequest = new HashSet<Integer>();
            long msSetKeepAlive = System.currentTimeMillis();

            for (int i=0;;i++) {
                if (vsocket.isClosed()) {
                    if (realSocket != null && !realSocket.isClosed()) {
                        throw new Exception("vsocket has been closed, but real socket is still open");
                    }
                    return;
                }

                
                synchronized (vsocket) {
                    // check to see if stream should be flushed
                    if (vsi.oos != null) {
                        if (vsi.shouldClose()) {
                            vsi.oos.writeByte(RequestInfo.Type.StoC_CloseObjectInputStream.ordinal());
                            vsi.reset();
                        }
                        else if (cque.getHeadPostion() == qpos) {
                            if (vsi.cntUnflushed > 0) {
                                vsi.oos.flush();
                                vsi.cntUnflushed = 0;
                            }
                        }
                        else {
                            if (vsi.cntUnflushed > 25) {
                                vsi.oos.flush();
                                vsi.cntUnflushed = 0;
                            }
                        }
                    }
                }
                
                RequestInfo[] ris = null;
                try {
                    ris = cque.getMessages(connectionId, qpos, 100, 2000);
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, "Message queue overrun with msg CircularQueue", e);
                    onException(connectionId, "Message queue overrun", "Message queue overrun", e, true);
                    throw e;
                }
                if (ris == null) {
                    continue;
                }

                for (RequestInfo ri : ris) {
                    qpos++;
                    if (vsocket.isClosed()) return;
                    if (ri == null || ri.bind == null) {
                        continue;
                    }
                    
                    cntTotalMsgs++;
                    
                    // 30250318 filter
                    if (ri.connectionId != connectionId) {
                        if (ri.bind.isOASync) {
                            if (!shouldSendSyncMessageToClient(ri, this.hmGuid)) continue;
                        }
                    }
                    
                    if (ri.type == RequestInfo.Type.StoC_QueuedBroadcast) {
                    }                    
                    else if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
                        if (ri.connectionId == connectionId) {
                            if (!ri.type.hasReturnValue() && !ri.bind.isOASync) {
                                continue;
                            }
                        }
                    }                    
                    else if (ri.type == RequestInfo.Type.CtoS_QueuedRequest) {
                        if (ri.connectionId == connectionId) {
                            hsQueuedRequest.add(ri.messageId);
                        }
                        continue;
                    }
                    else if (ri.type == RequestInfo.Type.StoC_QueuedResponse) {
                        // 9:CtoS_QueuedRequest send back to client
                        if (ri.connectionId != connectionId) {
                            continue;
                        }
                        if (!hsQueuedRequest.remove(ri.messageId)) {
                            hsQueuedRequest.add(ri.messageId);
                            continue;  // wait for it to show up the second time
                        }
                    }
                    else if (ri.type == RequestInfo.Type.CtoS_QueuedRequestNoResponse) {
                        // 7:CtoS_QueuedRequestNoResponse END
                        continue;
                    }
                    else if (ri.type == RequestInfo.Type.CtoS_QueuedResponse) {
                        continue;  
                    }                    
                    else if (ri.type == RequestInfo.Type.StoC_QueuedRequest) {
                        if (ri.connectionId != connectionId) {
                            continue;
                        }
                    }
                    else if (ri.type == RequestInfo.Type.StoC_QueuedRequestNoResponse) {
                        continue;
                    }
                    else {
                        continue;
                    }

                    cntTotalMsgsSent++;
//qqqqqqqqqqqqqqqq                    
 // System.out.println(String.format("%,d/%,d) Session._writeQueueMessages  msg=%s", cntTotalMsgsSent, cntTotalMsgs, ri.toLogString()));                    
                    
                    waitForProcessedByServer(ri);
                    long msNow = System.currentTimeMillis();
                    if (msSetKeepAlive + 5000 < msNow) {
                        cque.keepAlive(connectionId);
                        msSetKeepAlive = msNow;
                    }

                    synchronized (vsocket) {
                        vsi.cntUnflushed++;
                        vsi.cntWrite++;
                        
                        if (vsi.oos == null) {
                            vsi.oos = new RemoteObjectOutputStream(vsocket, hmClassDescOutput, aiClassDescOutput);
                            vsi.oos.writeByte(RequestInfo.Type.StoC_StartObjectInputStream.ordinal());
                        }
                        RemoteObjectOutputStream oos = vsi.oos;
                        
                        oos.writeByte(ri.type.ordinal());
    
                        if (ri.type == RequestInfo.Type.StoC_QueuedResponse) {
                            // 10:CtoS_QueuedRequest write to client END 
                            if (ri.exception != null) {
                                oos.writeByte(0);
                                oos.writeObject(ri.exception);
                            }
                            else if (ri.exceptionMessage != null) {
                                oos.writeByte(1);
                                oos.writeObject(ri.exceptionMessage);
                            }
                            else if (ri.responseBindName != null) {
                                oos.writeByte(2);
                                oos.writeObject(new Object[] {ri.responseBindName, ri.responseBindUsesQueue} );
                            }
                            else {
                                oos.writeByte(3);
                                // 20180225
                                oos.writeBoolean(ri.bHadOASyncEvent);
                                oos.writeObject(ri.response);
                            }
                            oos.writeInt(ri.messageId);
                        }
                        else if (ri.type == RequestInfo.Type.CtoS_QueuedBroadcast) {
                            oos.writeInt(ri.connectionId);
                            oos.writeInt(ri.messageId);
                            if (ri.connectionId != connectionId) {
                                oos.writeAsciiString(ri.bindName);
                                oos.writeAsciiString(ri.methodInfo.methodNameSignature);
                                oos.writeObject(ri.args);
                            }
                        }
                        else if (ri.type == RequestInfo.Type.StoC_QueuedRequest) {
                            oos.writeAsciiString(ri.bindName);
                            oos.writeAsciiString(ri.methodInfo.methodNameSignature);
                            processStoCArguments(ri, Session.this);  // this is only done once, right before it's sent
                            oos.writeObject(ri.args);
                            oos.writeInt(ri.messageId);
                        }
                        else if (ri.type == RequestInfo.Type.StoC_QueuedRequestNoResponse) {
                            oos.writeAsciiString(ri.bindName);
                            oos.writeAsciiString(ri.methodInfo.methodNameSignature);
                            processStoCArguments(ri, Session.this);  // this is only done once, right before it's sent
                            oos.writeObject(ri.args);
                        }
                        else if (ri.type == RequestInfo.Type.StoC_QueuedBroadcast) {
                            oos.writeAsciiString(ri.bindName);
                            oos.writeAsciiString(ri.methodInfo.methodNameSignature);
                            oos.writeObject(ri.args);  // args are already be processed (processStoCArguments)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Marks the RequestInfo as having had its method invoked and notifies any
     * thread waiting for invocation completion.
     *
     * @param ri the RequestInfo whose method execution has completed
     */
    protected void notifyMethodInvoked(RequestInfo ri) {
        if (ri == null) return;
        synchronized (ri) {
            ri.methodInvoked = true;
            ri.notifyAll();
        }
    }
    protected boolean waitForMethodInvoked(RequestInfo ri) {
        return waitForMethodInvoked(ri, 0);
    }

    /**
     * Waits for a RequestInfo to reach the "method invoked" state, optionally
     * applying a timeout. Used for synchronous request handling.
     *
     * @param ri             the RequestInfo representing the remote call
     * @param timeoutSeconds maximum time to wait; 0 means wait indefinitely
     * @return true if invoked, false if timeout or interruption occurs
     */
    protected boolean waitForMethodInvoked(RequestInfo ri, int maxSeconds) {
        if (ri == null) return false;
        boolean bResult = true;
        synchronized (ri) {
            for (int i=0; !ri.methodInvoked; i++) {
                try {
                    if (maxSeconds > 0) {
                        if (i >= maxSeconds) {
                            bResult = false;
                            break;
                        }
                    }
                    ri.wait(1000);
                }
                catch (Exception e) {}
            }
        }
        return bResult;
    }
    
    /**
     * Notifies threads waiting for confirmation that the server has processed
     * a queued request by marking the RequestInfo accordingly and issuing a
     * wake-up signal.
     *
     * @param ri the RequestInfo that has been processed
     */
    protected void notifyProcessedByServer(RequestInfo ri) {
        if (ri == null) return;
        synchronized (ri) {
            ri.processedByServerQueue = true;
            ri.notifyAll();
        }
    }
    
    /**
     * Causes the calling thread to wait until the given RequestInfo has been
     * acknowledged as processed by the server queue.
     *
     * @param ri the RequestInfo to monitor
     * @return true when processed, false if interrupted
     */
    protected void waitForProcessedByServer(RequestInfo ri) {
        if (ri == null) return;
        if (ri.processedByServerQueue) return;
        if (!ri.bind.usesQueue) return;
        
        // 20160215 dont wait if thread is already processing a que request
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            RequestInfo rix = rt.requestInfo;
            if (rix != null && ri != rix) {
                if (ri.bind.usesQueue && rix.bind.usesQueue) {
                    return;
                }
            }
        }
        
        synchronized (ri) {
            for (int i=0; !ri.processedByServerQueue; i++) {
                try {
                    ri.wait(100);
                }
                catch (Exception e) {}
            }
            return;
        }
    }

    // 20160202
    private AtomicInteger aiMethodCallCnt = new AtomicInteger();
    private AtomicInteger aiReceivedMethodCallCnt = new AtomicInteger();
    
    /**
     * number of remote methods called.
     */
    public long getMethodCallCount() {
        return aiMethodCallCnt.get();
    }
    /*
     * number of methods/broadcast received
     */
    public long getReceivedMethodCount() {
        return aiReceivedMethodCallCnt.get();
    }
    
    public long getQueueHeadPos() {
        for (Map.Entry<String, OACircularQueue<RequestInfo>> entry : this.hmAsyncCircularQueue.entrySet()) { 
            OACircularQueue<RequestInfo> cq = entry.getValue();
            return cq.getHeadPostion();
        }        
        return 0;
    }
    
    /**
     * Used to filter out broadcast msgs that get sent to clients.
     */
    protected boolean shouldSendSyncMessageToClient(RequestInfo ri, ConcurrentHashMap<UUID, Boolean> hmGuid) {
        return true;
    }

    public OACircularQueue<RequestInfo> getCircularQueue(String queName) {
    	OACircularQueue<RequestInfo> cq = hmAsyncCircularQueue.get(queName);
    	return cq;
    }
    
}
