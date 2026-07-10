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
package com.viaoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;



/**
 * Declares a named filter class that can be used in Hub filtering,
 * object queries, or view-model logic.
 *
 * <p>OAClassFilter identifies a filter implementation by name and provides
 * UI metadata along with optional auto-refresh semantics.  It is generally
 * applied to classes implementing custom Hub filter logic.</p>
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>Named filter: {@code name}, {@code displayName}, {@code description}.</li>
 *   <li>Optional input parameters for UI-driven filters.</li>
 *   <li>Automatic refresh interval for re-checking filtered objects.</li>
 *   <li>Support for lightweight query expressions.</li>
 * </ul>
 *
 * <p>Used by OAPath-based queries and Hub filtering systems.</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME) 

public @interface OAClassFilter {
    
	/**
	 * Specifies the internal name of the filter, used to identify the
	 * filter implementation in filtering or query operations.
	 */
	String name() default "";
    
	/**
	 * Provides a human-readable name for the filter, typically used in
	 * UI components such as drop-downs or configuration dialogs.
	 */
	String displayName() default "";
    
	/**
	 * Supplies descriptive text explaining the role or behavior of the
	 * filter for both developers and UI display.
	 */
	String description() default "";
    
	/**
	 * Indicates whether the filter accepts user-supplied input
	 * parameters, enabling more dynamic filtering behavior in UI
	 * contexts.
	 */
	boolean hasInputParams() default false;
    
	/**
	 * Defines the numeric refresh interval for automatically rechecking
	 * previously filtered objects to ensure they still satisfy the
	 * filter criteria.
	 */
    int autoRefreshInterval() default 0;

    /**
     * Specifies the {@link TimeUnit} associated with
     * {@code autoRefreshInterval()}, determining how often automatic
     * refresh checks should occur.
     */
    TimeUnit autoRefreshTimeUnit() default TimeUnit.DAYS;
    
    /**
     * Provides an optional lightweight query expression to evaluate
     * filter conditions without requiring a full filter class
     * implementation.
     */
    String query() default "";
}

