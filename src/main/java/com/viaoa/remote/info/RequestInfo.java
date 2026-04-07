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
package com.viaoa.remote.info;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.comm.multiplexer.io.VirtualSocket;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OADateTime;

/**
 * Tracks all runtime details for a single synchronous remote request within
 * OA’s multiplexer remoting infrastructure. A new {@code RequestInfo} instance
 * is created for every client-to-server or server-to-client remote method
 * invocation.
 *
 * <h2>Captured Diagnostic Data</h2>
 * <ul>
 *   <li>High-resolution timestamps (ms and ns) for performance analysis.</li>
 *   <li>The request {@link Type}, determining queue usage and response rules.</li>
 *   <li>Bind information, socket identifiers, connection IDs, and message IDs.</li>
 *   <li>The target object, Java method, generated method signature, and
 *       associated {@link MethodInfo}.</li>
 *   <li>All invocation arguments (captured and formatted for logs).</li>
 *   <li>Return value, remote exception, or error message.</li>
 *   <li>Whether the call originated from an OARemoteThread.</li>
 *   <li>Flags showing whether the server queue processed the request.</li>
 *   <li>Tracking for queued broadcasts, queued requests, and remote thread routing.</li>
 * </ul>
 *
 * <h2>Type Model</h2>
 * The {@link Type} enumeration encodes the full set of possible remote
 * operations (queued, unqueued, broadcast, request/response pairs, etc.).
 * Each value defines:
 * <ul>
 *   <li>whether the request uses a queue,</li>
 *   <li>whether it expects a return value,</li>
 *   <li>whether the response must be returned on the queue thread.</li>
 * </ul>
 *
 * <h2>Logging Support</h2>
 * <p>
 * {@link #toLogString()} produces a compact log entry that includes timestamps,
 * connection and bind information, method name, exception, and parameter
 * previews. This supports high-volume, production-grade debugging of remote
 * traffic.
 * </p>
 * <p>
 * {@link #getLogHeader()} returns a header string matching the column order of
 * the log output format.
 * </p>
 *
 * <h2>Concurrency</h2>
 * Most fields are written once during request assembly. A few flags
 * ({@code methodInvoked}, {@code processedByServerQueue}) are marked
 * {@code volatile} to reliably track lifecycle transitions across threads.
 *
 * @author vvia
 */
public class RequestInfo {
	/**
	 * Global counter used to assign a unique incremental identifier to each
	 * {@code RequestInfo} instance.
	 */
    private final static AtomicInteger aiCount = new AtomicInteger();

    /**
     * Returns the {@link Type} enum value corresponding to the given ordinal
     * index. If the index is out of range, null is returned.
     *
     * @param val ordinal index of the type
     * @return the matching Type value, or null if out of bounds
     */
    public static Type getType(int val) {
        Type[] types = Type.values();
        if (val >= 0 && val < types.length) return types[val];
        return null;
    }
    
    /**
     * Defines the various categories of remote operations supported by the remoting
     * system, including queued/unqueued requests, broadcasts, and request/response
     * variants.
     */
    public enum Type {
    	/**
    	 * Client-to-server request to retrieve lookup information. Does not use a
    	 * queue and expects a return value.
    	 */
        CtoS_GetLookupInfo(false, true),

        /**
         * Client-to-server operation instructing removal of the session’s broadcast
         * thread. Does not use a queue and does not expect a return value.
         */
        CtoS_RemoveSessionBroadcastThread (false, false),

        /**
         * Client-to-server request to obtain the broadcast class information. Does not
         * use a queue and expects a return value.
         */
        CtoS_GetBroadcastClass(false, true),
        
        /**
         * Client-to-server request sent directly over the socket, expecting a
         * response on the same socket.
         */
        CtoS_SocketRequest(false, true),

        /**
         * Client-to-server request sent on the socket without expecting any
         * response.
         */
        CtoS_SocketRequestNoResponse(false, false),
        
       
        // send on socket and have it returned on the same socket that the queue uses.
        //   note: does not get added to the queue, it is just written directly to the vsocket used by the queue.
        
        /**
         * Client-to-server request where the response must be returned on the queue’s
         * socket thread rather than the caller’s thread.
         */
        CtoS_ReturnOnQueueSocket(true, true, true),
        
        /**
         * Client-to-server request placed on the server queue and expecting a
         * response.
         */
        CtoS_QueuedRequest(true, true),

        /**
         * Client-to-server queued request that does not expect any return value.
         */
        CtoS_QueuedRequestNoResponse(true, false),
        
        /**
         * Client returning a result for a previously issued queued request.
         */
        CtoS_QueuedResponse(true, false), // client returning result from stoc_queuedRequest
        
        /**
         * Client-to-server broadcast request added to the server queue. A completion
         * signal is returned once the broadcast has been processed.
         */
        CtoS_QueuedBroadcast(true, false),   // will return to client, once it is processed (not invoked) on the server

        /**
         * Server-to-client instruction to create a new socket for communication. No
         * queue usage and no return value.
         */
        StoC_CreateNewStoCSocket(false, false),

        /**
         * Server-to-client broadcast message processed through the queue without
         * expecting a return value.
         */
        StoC_QueuedBroadcast(true, false),
        
        /**
         * Server-to-client queued request that expects a queued response from the
         * client.
         */
        StoC_QueuedRequest(true, true),   // server calling remote method on client, and get queued response CtoS_ResponseForQueuedRequest

        /**
         * Server-to-client queued request that does not expect a return value.
         */
        StoC_QueuedRequestNoResponse(true, false),  
        
        /**
         * Server-to-client direct socket request expecting a response on the socket.
         */
        StoC_SocketRequest(false, true),     // send request on socket.output and get result from socket.input
        
        /**
         * Server-to-client socket request that does not expect any response.
         */
        StoC_SocketRequestNoResponse(false, false),
        
        /**
         * Server-to-client queued response sent back for a previously issued queued
         * request.
         */
        StoC_QueuedResponse(true, false),
        
        /**
         * Server-to-client instruction indicating that an ObjectInputStream should be
         * started. Does not use a queue and does not expect a return value.
         */
        StoC_StartObjectInputStream(false, false),
        
        /**
         * Server-to-client instruction indicating that an ObjectInputStream should be
         * closed. Does not use a queue and does not expect a return value.
         */
        StoC_CloseObjectInputStream(false, false);

        
    	/**
    	 * Constructs a Type enum value specifying whether the request is processed
    	 * through a queue and whether a return value is expected. The return-on-queue
    	 * flag defaults to false.
    	 *
    	 * @param usesQueue true if the request should be processed via a queue
    	 * @param hasReturnValue true if the request expects a return value
    	 */
        Type(boolean usesQueue, boolean hasReturnValue) {
            this.usesQueue = usesQueue;
            this.hasReturnValue = hasReturnValue;
            this.bReturnOnQueueThread = false;
        }

        /**
         * Constructs a Type enum value including whether the response must return on
         * the queue-processing thread.
         *
         * @param usesQueue true if the request uses a queue
         * @param hasReturnValue true if a return value is expected
         * @param bReturnOnQueueThread true if the response must be delivered on the queue thread
         */
        Type(boolean usesQueue, boolean hasReturnValue, boolean bReturnOnQueueThread) {
            this.usesQueue = usesQueue;
            this.hasReturnValue = hasReturnValue;
            this.bReturnOnQueueThread = bReturnOnQueueThread;
        }

        /**
         * Indicates whether this request type is processed through a queue.
         */
        private final boolean usesQueue;
        
        /**
         * Indicates whether this request type expects a return value.
         */
        private final boolean hasReturnValue;
        
        /**
         * Indicates whether the response must be returned on the queue-thread rather
         * than on the caller’s thread.
         */
        private final boolean bReturnOnQueueThread;
        
        /**
         * Returns whether this request type is processed through a queue.
         *
         * @return true if queue-based processing is used
         */
        public boolean usesQueue() {
            return this.usesQueue;
        }

        /**
         * Returns whether this request type expects a return value.
         *
         * @return true if a return value is expected
         */
        public boolean hasReturnValue() {
            return this.hasReturnValue;
        }
    }
    
    /**
     * The specific {@link Type} describing how this request is processed,
     * including queue behavior and return-value rules.
     */
    public Type type;

    /**
     * Unique incrementing identifier assigned to this request instance at
     * construction time.
     */
    final public int cnt;
    
    /**
     * Millisecond timestamp indicating when this request began processing.
     */
    public long msStart;
    
    /**
     * Nanosecond timestamp captured at request start for high–resolution timing.
     */
    public long nsStart; 
    
    /**
     * Nanosecond timestamp captured when request processing completed or reached a
     * terminal point.
     */
    public long nsEnd;

    /**
     * Binding information for the target remote object on which the method
     * invocation is performed.
     */
    public BindInfo bind;
    
    /**
     * The virtual socket used to send or receive this request, depending on
     * direction (client-to-server or server-to-client).
     */
    public VirtualSocket socket;
    
    /**
     * Identifier for the connection associated with this request.
     */
    public int connectionId;
    
    /**
     * Identifier for the individual message within the connection, used for
     * correlating request/response cycles.
     */
    public int messageId;
    
    /**
     * Identifier for the virtual socket instance used by this request.
     */
    public int vsocketId;
    
    /**
     * Thread identifier. For server-to-client calls, this represents the server
     * thread number processing the request.
     */
    public int threadId;  // if StoC, then the Thread #
    
    /**
     * The bind-name associated with the target remote object for this request.
     * Used for routing and lookup in the remoting infrastructure.
     */
    public String bindName;
    
    /**
     * The target object instance on which the remote method will be invoked.
     * May be null if the method or binding has not yet been resolved.
     */
    public Object object;  // object that is being invoked 
    
    /**
     * The reflected {@link Method} object representing the method being remotely
     * invoked. May be assigned later if determined through metadata lookup.
     */
    public Method method;
    
    /**
     * A unique method signature string combining method name and parameter
     * characteristics, used to resolve overloaded methods remotely.
     */
    public String methodNameSignature;  // unique name for method, so that method overloading can be supported.
    
    /**
     * Metadata associated with the method being invoked, including remote return
     * types, queue behavior, and parameter handling rules.
     */
    public MethodInfo methodInfo;
    
    /**
     * Array of arguments supplied to the remote method invocation.
     */
    public Object[] args;
    
    /**
     * Indicates whether this request was sent over the network. A value of false
     * indicates that the invocation was treated as a local call.
     */
    public boolean bSent;  // false if a local call, ex: "hashCode(), toString(), etc"
    
    /**
     * Bind-name associated with the remote object returning the response, if
     * applicable.
     */
    public String responseBindName;
    
    /**
     * Indicates whether the response-producing remote object uses a queue for
     * dispatching its operations.
     */
    public boolean responseBindUsesQueue;
    
    /**
     * Exception thrown during remote invocation, if any, captured for return to
     * the caller.
     */
    public Exception exception;
    
    /**
     * Message-only version of the exception, used when an exception occurs but is
     * not directly serialized.
     */
    public String exceptionMessage;
    
    /**
     * The return value produced by the remote method invocation. Null if no
     * return value or if an exception occurred.
     */
    public Object response;

    /**
     * Indicates whether the originating thread for this request was an
     * OARemoteThread.
     */
    public boolean isRemoteThread;  // if this request was made by an oaRemoteThread
    
    
  //qqqqqqvvvvvvvv    
    public String replicationSource;
    

    
    /**
     * Flag set to true once the remote method has actually been invoked. Declared
     * volatile for visibility across threads.
     */
    public volatile boolean methodInvoked;  // set to true with the method has been invoked
    
    /**
     * Indicates whether the server-side queue has processed this request. Declared
     * volatile for thread visibility.
     */
    public volatile boolean processedByServerQueue;  // flag set on server after it's processed

    //  public boolean isFromRemoteThread; // know if the thread making the remote call is a remoteThread
    
    /**
     * Marks whether an OA Sync event occurred while processing this remote method
     * invocation.
     */
    public boolean bHadOASyncEvent;  // 20180223 flag to know if there was an oasync event while calling this remote method
    
    
    /**
     * Constructs a new {@code RequestInfo}, assigning a unique identifier using
     * the global counter.
     */
    public RequestInfo() {
        this.cnt = aiCount.incrementAndGet();
    }

    /**
     * Builds and returns a formatted log entry for this request, including
     * timestamps, connection identifiers, bind information, method name, exception
     * details, and parameter previews.
     *
     * @return formatted log string
     */
    public String toLogString() {
        String msg = String.format("%1$tm/%1$td|%1$tH:%1$tM:%1$tS.%1$tL", new Date(msStart));

        double d;
        if (nsStart == 0 || nsEnd == 0) d = -1.0d;
        else d = (nsEnd - nsStart) / 1000000.0D;

        msg += String.format("|%.1f",  d);
        
        msg += "|" + connectionId;
        msg += "|" + bindName;
        msg += "|" + type;

        if (method == null && methodInfo != null) {
            method = methodInfo.method;
        }
        
        if (method != null) {
            Class c = method.getDeclaringClass();
            String s;
            if (c != null) {
                s = c.getSimpleName();
            }
            else s = "";
            msg += "|" + s;
            msg += "|" + method.getName();
        }
        else {
            msg += "|";
            msg += "|";
        }
                
        if (exception != null) {
            msg += "|"+exception;
        }
        else if (exceptionMessage != null) {
            msg += "|"+exceptionMessage;
        }
        else {
            msg += "|";
        }
        msg += "|";
        
        if (method == null) return msg;
        Class[] cs = method.getParameterTypes();

        if (cs == null || cs.length == 0) return msg;
        
        int i = 0;
        for (Class c : cs) {
            String s;
            if (args != null && args.length > i) {
                Object obj = args[i];
                if (obj == null) s = "";
                else if (obj instanceof Class) {
                    s = ((Class) obj).getSimpleName();
                }
                else if (obj instanceof String) {
                    s = (String) obj;
                    if (s.length() > 30) s = s.substring(0,28)+"..";
                }
                else if (obj instanceof Number) {
                    s = obj.toString();
                }
                else if (obj instanceof Boolean) {
                    s = ((Boolean)obj).toString();
                }
                else if (obj instanceof OADateTime) {
                    s = obj.toString();
                }
                else if (obj instanceof OAObjectKey) {
                    OAObjectKey key = (OAObjectKey) obj;
                    Object[] ids = key.getObjectIds();
                    if (ids != null && ids.length > 0 && ids[0] != null) s = "id:"+ids[0];
                    else s = "guid:"+((OAObjectKey) obj).getGuid();
                }
                else {
                    s = obj.getClass().getSimpleName();
                }
            }
            else s = "";
            if (i > 0) msg += "|";
            msg += "["+i+"]="+s;
            i++;
        }
        
        
        return msg;
    }
    
    /**
     * Returns a header line describing the column layout used by
     * {@link #toLogString()}, allowing log files to include a standardized header.
     *
     * @return formatted header string
     */
    public static String getLogHeader() {
        String msg = "Date|Time";
        msg += "|ms";
        msg += "|ConnectionId";
        msg += "|BindName";
        msg += "|Type";
        msg += "|Object";
        msg += "|Method";
        msg += "|exception";
        msg += "|arguments";
        return msg;
    }
    
}
