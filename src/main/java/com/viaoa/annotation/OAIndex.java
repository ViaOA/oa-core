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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a database index applied to a table in {@link OATable}.
 *
 * <p>Can represent normal indexes, unique indexes, or foreign-key indexes.</p>
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface OAIndex {

	/**
	 * Specifies the name of the database index as it should appear
	 * in the underlying datasource.
	 */
	String name();

	/**
	 * Defines the list of columns that make up this index.
	 * The order of columns can affect index behavior and performance.
	 */
	OAIndexColumn[] columns();

	/**
	 * Indicates whether this index corresponds to a foreign-key
	 * constraint in the datasource.
	 */
	boolean fkey() default false; // is this index for an foreign key

	/**
	 * Marks the index as enforcing uniqueness across its defined
	 * columns, preventing duplicate combinations of values.
	 */
	boolean unique() default false;
}
