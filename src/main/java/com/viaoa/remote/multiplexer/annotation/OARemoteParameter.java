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
 * Specifies remoting behavior for a single parameter of a remote interface
 * method. This annotation must be placed on the parameter within the
 * remote interface definition.
 *
 * <ul>
 *   <li>{@code compressed=true} – compress the parameter when transmitted.</li>
 *   <li>{@code dontUseQueue=true} – when this parameter is a remote object,
 *       calls routed through it will bypass asynchronous queuing, even if the
 *       surrounding interface or method uses a queue.</li>
 * </ul>
 *
 * <p>
 * This allows fine-tuned control of message routing and transmission
 * optimization on a per-argument basis.
 * </p>
 *
 * @author vvia
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OARemoteParameter {
    
    // true if the param should be compressed when it is transmitted
    boolean compressed() default false;
    
    // if true and this param is a remote object, then it will not use a queue when messaging (even if parent uses a msg queue)
    boolean dontUseQueue() default false;
}
