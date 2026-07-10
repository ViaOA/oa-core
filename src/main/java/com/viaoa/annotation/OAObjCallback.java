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

import com.viaoa.callback.OAObjectCallback;


/**
 * Declares a method or class-level callback used by {@link OAObjectCallback}
 * to determine enabled/visible behavior for UI and workflow logic.
 *
 * <p>The annotation supports:
 * <ul>
 *   <li>Property-based enable/visible rules</li>
 *   <li>Model-user based rules tied to generated permission objects</li>
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
	
	/**
	 * Identifies a property whose value determines whether the
	 * annotated method or object should be enabled.
	 */
	String enabledProperty() default "";

	/**
	 * Specifies the value of {@code enabledProperty} that results in
	 * the annotated method or object being considered enabled.
	 */
	boolean enabledValue() default true;

	/**
	 * Identifies a property whose value determines whether the
	 * annotated method or object should be visible.
	 */
	String visibleProperty() default "";

	/**
	 * Specifies the value of {@code visibleProperty} that results in
	 * the annotated method or object being considered visible.
	 */
	boolean visibleValue() default true;

	/**
	 * Property evaluated on the current model user object to determine
	 * whether the annotated method or class should be enabled.
	 */
	String modelUserEnabledProperty() default "";

	/**
	 * Specifies the value of {@code modelUserEnabledProperty} that results
	 * in the method or class being considered enabled for the current model user.
	 */
	boolean modelUserEnabledValue() default true;

	/**
	 * Property evaluated on the current model user object to determine
	 * whether the annotated method or class should be visible.
	 */
	String modelUserVisibleProperty() default "";

	/**
	 * Specifies the value of {@code modelUserVisibleProperty} that results
	 * in the method or class being considered visible for the current model user.
	 */
	boolean modelUserVisibleValue() default true;

	/**
	 * Lists property paths whose changes should trigger reevaluation of
	 * enabled/visible state for the annotated method or class.
	 */
	String[] viewDependentProperties() default {};

	/**
	 * Lists model-user level properties whose changes should trigger
	 * reevaluation of UI state for the annotated element.
	 */
	String[] modelUserDependentProperties() default {};

	/**
	 * Declares the callback types that the annotated method expects,
	 * allowing OA to route enable/visible checks appropriately.
	 */
	OAObjectCallback.Type[] supportedTypes() default {};
}
