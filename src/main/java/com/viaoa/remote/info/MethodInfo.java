/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
    
    public MethodInfo() {
        
    }
    
    public Method method;
    // unique name based on methodName and params
    public String methodNameSignature;

    // if return value is a remote object
    public Class remoteReturn;
    // flag to know if return value should be compressed 
    public boolean compressedReturn;
    
    // if any of the params are remote object
    public Class[] remoteParams;
    
    public boolean[] compressedParams;
    public boolean[] dontUseQueues;
    
    // true if dont wait for return value (void methods)
    public boolean noReturnValue;

    public boolean dontUseQueueForReturnValue;
    
    public boolean returnOnQueueSocket;
    
    public boolean dontUseQueue;
    
    public int timeoutSeconds = 0;
    
    /** option for server broadcast to run in RemoteThread */
    public boolean runInRemoteThread;
}
