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

import com.viaoa.object.OAObjectCallback;

/**
 * Declares a method or class-level callback used by {@link OAObjectCallback}
 * to determine enabled/visible behavior for UI and workflow logic.
 *
 * <p>The annotation supports:
 * <ul>
 *   <li>Property-based enable/visible rules</li>
 *   <li>Context-based rules tied to surrounding objects</li>
 *   <li>Dependent property paths (triggers recalculation)</li>
 *   <li>Expected callback types (AllowEnabled, AllowVisible, etc.)</li>
 * </ul>
 *
 * <p>Used heavily by OA-Web and OA-JFC to manage dynamic UI state.</p>
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OAObjCallback {
	String enabledProperty() default "";

	boolean enabledValue() default true;

	String visibleProperty() default "";

	boolean visibleValue() default true;

	String contextEnabledProperty() default "";

	boolean contextEnabledValue() default true;

	String contextVisibleProperty() default "";

	boolean contextVisibleValue() default true;

	// any properties that affect visiblity, enabled, or rendering
	String[] viewDependentProperties() default {};

	String[] contextDependentProperties() default {};

	// expected types that the method is expecting and will call ack() method when called.
	OAObjectCallback.Type[] supportedTypes() default {};
}
