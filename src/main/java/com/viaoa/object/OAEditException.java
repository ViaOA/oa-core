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
package com.viaoa.object;

/**
 * Exception thrown when an invalid or disallowed edit occurs on an {@link OAObject} property.
 * <p>
 * OAEditException is raised by framework or application logic that performs property-level
 * validation and detects a user-supplied value that cannot be accepted.  It carries the
 * property name and attempted value so higher-level components (such as editors, UI
 * bindings, or transaction managers) can react or roll back gracefully.
 *
 * <p><b>Key Points</b>:
 * <ul>
 *   <li>Subclasses {@link RuntimeException} for lightweight, unchecked propagation.</li>
 *   <li>Stores the target property name and the invalid {@code newValue}.</li>
 *   <li>Convenience constructors handle primitive types (long, double, boolean).</li>
 * </ul>
 *
 * Typical usage is inside an OAObject property-setter, converter, or business rule that
 * performs inline validation and needs to abort a change while preserving context.
 */
public class OAEditException extends RuntimeException {
    static final long serialVersionUID = 1L;
    private String property;
    private Object newValue;

    public OAEditException(OAObject obj, String property, Object newValue) {
        super("Invalid entry for "+property);
        this.property = property;
        this.newValue = newValue;
    }

    public OAEditException(OAObject obj, String property, long newValue) {
        this(obj, property, Long.valueOf(newValue));
    }

    public OAEditException(OAObject obj, String property, double newValue) {
        this(obj, property, Double.valueOf(newValue));
    }
    
    public OAEditException(OAObject obj, String property, boolean newValue) {
        this(obj, property, Boolean.valueOf(newValue));
    }
    
    public Object getNewValue() {
        return newValue;
    }
    
    public String getProperty() {
        return property;
    }
}

