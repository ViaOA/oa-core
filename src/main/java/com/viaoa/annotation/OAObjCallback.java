/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.viaoa.object.OAObjectCallback;

/**
 * Used to define OAObject callbacks, and dependent property paths for OAObjectCallback.enabled and visible
 *
 * @see com.viaoa.object.OAObjectCallback
 * @author vvia
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
