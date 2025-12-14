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
package com.viaoa.remote.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a Java interface as a REST-accessible remote interface. When a
 * client binds to this interface using {@link com.viaoa.remote.rest.OARestClient},
 * the {@code contextName} is appended to the client's base URL to form the
 * root path for all remote method calls.
 *
 * <p>
 * This annotation must be placed on the <b>interface</b>, not the implementing
 * class. Method-level behavior is defined using
 * {@link com.viaoa.remote.rest.annotation.OARestMethod}.
 * </p>
 *
 * <p>
 * Example:
 * <pre>
 *   @OARestClass(contextName = "customer")
 *   public interface CustomerService {
 *       ...
 *   }
 * </pre>
 * produces remote calls to:
 * <pre>
 *   https://server/.../customer/...
 * </pre>
 * </p>
 *
 * @author vvia
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARestClass {

	/**
	 * Context name used on webserver, which will be added to the baseURL.
	 * <p>
	 * example: "customer", will use "http://www.company.com/customer.."
	 */
	String contextName() default "";
}
