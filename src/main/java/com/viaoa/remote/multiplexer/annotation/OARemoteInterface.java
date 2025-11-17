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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java interface as a remote interface that can be invoked across
 * OA's Multiplexer remoting system. This annotation must be applied to
 * the interface type itself, not to the implementing class.
 *
 * <p>
 * When {@code isOASync=true}, all method calls on this remote interface
 * will be queued and executed serially by a single {@link OARemoteThread}.
 * This guarantees strict ordering and prevents reentrancy issues for
 * interfaces that are not thread-safe.
 * </p>
 *
 * <p>
 * This annotation is read by the remote lookup and proxy-generation code
 * in {@code OARemoteMultiplexerClient} and
 * {@code OARemoteMultiplexerServer}.
 * </p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARemoteInterface {
    
    /**
     * If true, then all methods that are called will be put in a queue and processed by OARemoteThread serially. 
     */
    boolean isOASync() default false;
}
