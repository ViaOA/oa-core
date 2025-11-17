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

/**
 * Annotation used to describe an action method on an {@link OAObject}.
 *
 * <p>These methods represent domain-level operations (e.g. approve, 
 * close, submit) that can be exposed to UI layers through generated menus,
 * buttons, or command bindings.</p>
 *
 * <p>Metadata includes display name, tooltip, description, and help text,
 * enabling OA-Web or OA-JFC to automatically present the method to the user.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OAMethod {
    String displayName() default "";
    String description() default "";
    String toolTip() default "";
    String help() default "";
}
