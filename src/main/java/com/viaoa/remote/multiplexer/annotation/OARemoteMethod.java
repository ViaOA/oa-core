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
    
	/**
	 * Specifies whether the return value should be compressed when transmitted.
	 *
	 * @return {@code true} to compress the return value, otherwise {@code false}
	 */
    boolean compressedReturnValue() default false;

    /**
     * Specifies whether the method should not return a value.
     *
     * @return {@code true} if no return value should be sent, otherwise {@code false}
     */
    boolean noReturnValue() default false;
    
    /**
     * Specifies the timeout, in seconds, for the remote method invocation.
     *
     * @return the timeout value in seconds, or {@code 0} for no timeout
     */
    int timeoutSeconds() default 0;
    
    /**
     * Specifies whether the return value should bypass the message queue.
     *
     * @return {@code true} to bypass the queue for the return value, otherwise {@code false}
     */
    boolean dontUseQueueForReturnValue() default false;
    
    /**
     * Specifies whether the method invocation should bypass the message queue.
     *
     * @return {@code true} to bypass the queue, otherwise {@code false}
     */
    boolean dontUseQueue() default false;
    
    /**
     * Specifies whether the return value should be sent using the queue socket.
     *
     * @return {@code true} to send the return value on the queue socket, otherwise {@code false}
     */
    boolean returnOnQueueSocket() default false;
    
    /**
     * Specifies whether the server should execute the method in an {@code OARemoteThread}.
     *
     * @return {@code true} to execute in a remote thread, otherwise {@code false}
     */
    boolean runInRemoteThread() default false; 
}

