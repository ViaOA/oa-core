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


//qqqqqqqqqqqqqqqqq explain that OA uses abstract methods to define boundaries and hierarchy of control

/**
 * Indicates that an abstract method declares a required capability that is
 * provided by the owning parent/coordinator of this class.
 *
 * <p>This annotation is used to document <b>dependency-by-need</b>, not
 * inheritance-for-reuse and not framework-style dependency injection.</p>
 *
 * <p>An {@code @OAParentProvided} method expresses:
 * <ul>
 *   <li><b>What this class needs</b> in order to perform its responsibility</li>
 *   <li><b>Not how</b> the requirement is fulfilled</li>
 *   <li><b>Not which service</b> provides it</li>
 * </ul>
 * </p>
 *
 * <p>The implementation of the annotated method is supplied by the
 * owning parent/coordinator (for example {@code OAObjectService}) when the
 * instance is created. This keeps:
 * <ul>
 *   <li>coordination and wiring centralized</li>
 *   <li>sub-services free of service-locator or graph access</li>
 *   <li>responsibility boundaries explicit and enforceable</li>
 * </ul>
 * </p>
 *
 * <p>Classes containing {@code @OAParentProvided} methods are not intended
 * to be constructed or used standalone. They are managed as part of a
 * coordinated service family owned by the parent.</p>
 *
 * <p><b>Usage guidelines:</b>
 * <ul>
 *   <li>Annotated methods must represent <b>outcomes or actions</b>, not access to services</li>
 *   <li>Methods must not return or accept {@code *Service}, {@code OAGraph}, or Ops types</li>
 *   <li>Typically used on {@code protected abstract} methods in internal sub-services</li>
 * </ul>
 * </p>
 *
 * <p>This pattern aligns with OA's ownership-based architecture, where
 * parent components manage lifecycle and coordination, and child components
 * declare only the capabilities they require.</p>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OAParentProvided {

    /**
     * Name of the parent/coordinator responsible for providing the implementation.
     * This is informational and intended to make ownership explicit to readers.
     */
    String parentName() default "";

    /**
     * Optional explanation of why this capability is required.
     */
    String purpose() default "";
    
    String example() default "";
}
