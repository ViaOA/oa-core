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
package com.viaoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.viaoa.hub.HubEvent;

/**
 * Declares a method as an OA trigger handler, invoked when one or more
 * property paths change anywhere in the object graph.
 *
 * <p>Triggers allow OA to avoid expensive deep property-path listeners.
 * Instead of having each object listen to a long path, OA reverses the
 * path and invokes the trigger on the affected objects.</p>
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>{@code properties}: property paths that cause this method to run</li>
 *   <li>{@code onlyUseLoadedData}: limit to in-memory objects</li>
 *   <li>{@code runOnServer}: trigger only on server-side</li>
 *   <li>{@code runInBackgroundThread}: optionally asynchronous</li>
 * </ul>
 *
 * <p>Method signature must be:<br>
 * {@code public void nameTrigger(HubEvent hubEvent)}.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OATriggerMethod {

    /** 
     * @return Property paths that will automatically call this method when the propPath is changed.
     */
    String[] properties() default {};
    
    
    /**
     * @return if true (default), then triggers are only made on objects that are in memory.
     */
    boolean onlyUseLoadedData() default true;
    
    /**
     * @return if true (default), then only run on the server.
     */
    boolean runOnServer() default true;
    
    /**
     * @return If true, then this will be ran in another thread. Otherwise false (default), run in the current thread.
     */
    boolean runInBackgroundThread() default false;
}
