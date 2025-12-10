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
 * Declares the foreign-key column(s) that implement a link for an
 * {@link OAOne} or {@link OAMany} relationship.
 *
 * <p>{@code fromProperty} is the local link name, and {@code toProperty}
 * identifies the target primary-key property.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAFkey {
	
	/**
	 * Specifies the local link property name that defines the
	 * relationship’s originating side.
	 */
 	String fromProperty() default "";

 	/**
 	 * Identifies the primary-key property on the target object
 	 * referenced by this foreign-key relationship.
 	 */
	String toProperty() default "";

	/**
	 * Deprecated: legacy definition of one or more foreign-key column
	 * names for older OA models. Modern models should not use this
	 * field.
	 */
	@Deprecated
	String[] columns() default {};
}
