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
 * Declares the link (join) table used for many-to-many relationships.
 * Maps the table name and the foreign-key columns from the local object.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OALinkTable {

	/**
	 * Specifies the physical name of the link (join) table used to
	 * implement a many-to-many relationship.
	 */
	String name();
    
	/**
	 * Lists the foreign-key column names in the link table that
	 * correspond to the primary-key columns of the local object.
	 */
	String[] columns();  // these match the pkey columns for the object that this is used in.
    
	/**
	 * Defines the name of the index applied to the link table for
	 * optimizing many-to-many operations.
	 */
	String indexName();
}
