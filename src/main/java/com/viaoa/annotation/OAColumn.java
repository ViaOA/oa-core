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
 * Maps an {@link OAProperty}-annotated getter to a datasource column.
 *
 * <p>Defines the physical SQL column name, type, max length, and indexing
 * hints used by OA’s JDBC datasource layer.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OAColumn {
	
	/**
	 * Specifies the physical column name in the underlying datasource
	 * that this property maps to.
	 */
    String name() default "";
    
    /**
     * Defines the JDBC SQL type for the column, using constants from
     * {@link java.sql.Types}. Defaults to {@code VARCHAR}.
     */
    int sqlType() default java.sql.Types.VARCHAR;
    
    /**
     * Indicates the maximum allowed length of the column value.
     * A value of zero implies that no explicit maximum is defined.
     */
    int maxLength() default 0;
    
    /**
     * Flags the column as participating in full-text indexing,
     * enabling search capabilities provided by the datasource.
     */
    boolean isFullTextIndex() default false;
    
    /**
     * Provides a lowercase variant of the column name, useful for
     * normalized lookups and case-insensitive mappings.
     */
    String lowerName() default "";
}
