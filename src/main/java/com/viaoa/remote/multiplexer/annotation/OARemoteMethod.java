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
package com.viaoa.remote.multiplexer.annotation;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines remoting behavior for a specific method on a remote interface.
 * This annotation must be placed on the interface method (never on the
 * implementation class).
 *
 * <p>The values control how the client and server handle:</p>
 * <ul>
 *   <li>compression of return values,</li>
 *   <li>whether a return value is transmitted at all,</li>
 *   <li>whether the method call or its return value should bypass
 *       the asynchronous message queue,</li>
 *   <li>whether the server should execute the method inside an
 *       {@link OARemoteThread} (useful for broadcast or fan-out behavior),</li>
 *   <li>the timeout applied to the invocation.</li>
 * </ul>
 *
 * <p>
 * These settings allow each method to fine-tune performance, ordering,
 * and routing behavior based on application needs. They override any
 * interface-level defaults specified using {@link OARemoteInterface}.
 * </p>
 *
 * @author vvia
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARemoteMethod {
    
    // true if the return value should be compressed when it is transmitted
    boolean compressedReturnValue() default false;

    // true if return value should not be returned
    boolean noReturnValue() default false;
    
    int timeoutSeconds() default 0;
    
    /**
     * if true, then it will not use a queue for the return value (even if parent uses a msg queue)
     */
    boolean dontUseQueueForReturnValue() default false;
    
    /**
     * Do not use queue (even if parent uses a msg queue).
     */
    boolean dontUseQueue() default false;
    
    /**
     * send return value using the socket that writes queued messages from the server to the client
     */
    boolean returnOnQueueSocket() default false;
    
    /**
     * Server side broadcast option to have the server runn the method using an OARemoteThread.
     */
    boolean runInRemoteThread() default false; 
}

