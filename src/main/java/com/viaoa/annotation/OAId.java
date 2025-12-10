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

/**
 * Identifies a getter method as a primary-key property for an
 * {@link OAObject}.  Supports GUIDs, auto-assignment, and multi-part keys.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAId {
	
	/**
	 * Indicates whether the primary-key value should be automatically
	 * assigned when a new object is created.
	 */
	boolean autoAssign() default true;

	/**
	 * Specifies that the primary-key value should be generated as a
	 * GUID/UUID rather than a numeric or other type.
	 */
	boolean guid() default false;

	/**
	 * Defines the position of this property within a multi-part
	 * primary key. A value of zero is typically used for single-part
	 * keys.
	 */
	int pos() default 0; // for multiple-part keys
}
