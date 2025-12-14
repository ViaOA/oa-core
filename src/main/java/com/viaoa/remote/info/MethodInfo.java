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

/**
 * Metadata describing a single remotely invocable method. Instances are created
 * while scanning a remote interface and contain all information needed to
 * serialize a request, determine routing behavior, and handle the return value.
 *
 * <h2>Captured Method Details</h2>
 * <ul>
 *   <li>The reflected {@link Method} object.</li>
 *   <li>A unique signature composed of method name and parameter types,
 *       allowing overloaded remote methods.</li>
 *   <li>Whether the return value is itself a remote interface.</li>
 *   <li>Flags for compressed return values and compressed parameters.</li>
 *   <li>Flags determining whether parameters or results bypass the queue.</li>
 *   <li>Timeout configuration, broadcast behavior, and queue-socket return mode.</li>
 *   <li>Whether the call should be executed on a dedicated remote thread.</li>
 * </ul>
 *
 * <h2>Role in Remoting</h2>
 * {@code MethodInfo} is looked up by the remoting layer during request
 * dispatch. It controls:
 * <ul>
 *   <li>how arguments are encoded,</li>
 *   <li>which communication channel is used,</li>
 *   <li>whether a return value is expected,</li>
 *   <li>how remote types are resolved and proxied.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Instances are created during {@link BindInfo#loadMethodInfo()} and remain
 * immutable afterward.
 *
 * @author vvia
 */
public class MethodInfo {
    
	/**
	 * The reflected {@link Method} instance representing the remotely callable
	 * method associated with this metadata record.
	 */
    public Method method;

    /**
     * A unique signature created from the method name and parameter types, used to
     * distinguish overloaded methods during remote invocation.
     */
    public String methodNameSignature;

    /**
     * The return type of the method when it represents a remote interface. Null
     * if the return value is not a remote object.
     */
    public Class remoteReturn;
 
    /**
     * Indicates whether the return value should be compressed before being
     * transmitted across the remoting channel.
     */
    public boolean compressedReturn;
    
    /**
     * Array of parameter types that represent remote objects. Null if no
     * parameters are remote interfaces.
     */
    public Class[] remoteParams;
    
    /**
     * Flags indicating which method parameters should be compressed when
     * serialized for remote invocation.
     */
    public boolean[] compressedParams;

    /**
     * Flags indicating which parameters should bypass asynchronous queue handling
     * during remote dispatch.
     */
    public boolean[] dontUseQueues;
    
    /**
     * True when the method does not expect a return value, typically for void
     * methods or those explicitly configured to suppress return handling.
     */
    public boolean noReturnValue;

    /**
     * Indicates whether the return value, if any, should bypass asynchronous queue
     * processing and be returned directly.
     */
    public boolean dontUseQueueForReturnValue;
    
    /**
     * True if the return value should be delivered on the queue socket rather
     * than through the normal request/response channel.
     */
    public boolean returnOnQueueSocket;
    
    /**
     * Determines whether the method invocation itself should bypass the
     * asynchronous queue mechanism.
     */
    public boolean dontUseQueue;
    
    /**
     * Number of seconds to wait before timing out a remote invocation. A value of
     * zero indicates no configured timeout.
     */
    public int timeoutSeconds = 0;
    
    /**
     * True if the method invocation should be executed in a dedicated remote
     * thread rather than in the calling context.
     */
    public boolean runInRemoteThread;
    
    /**
     * Default constructor. Performs no initialization; all fields retain their
     * default values.
     */
    public MethodInfo() {
    }
}
