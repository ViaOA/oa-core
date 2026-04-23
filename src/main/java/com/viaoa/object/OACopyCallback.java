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
package com.viaoa.object;

import com.viaoa.hub.Hub;


/**
 * Callback used by {@link OAObjectReflectDelegate} during deep copy
 * operations to customize how owned objects, properties, and link
 * references are duplicated.
 *
 * <p>Copying in OA proceeds through a recursive traversal of owned objects
 * and link relationships.  This callback allows applications to override
 * default behavior for selective copying, substitution, or suppression
 * of particular values.</p>
 *
 * <p><b>Override Points</b>:
 * <ul>
 *   <li>{@link #shouldCopyOwnedHub(OAObject, String, boolean)} —
 *       decide whether an owned detail Hub should be copied.</li>
 *   <li>{@link #createCopy(OAObject, String, Hub, OAObject)} —
 *       customize creation of the copied owned object.</li>
 *   <li>{@link #getPropertyValue(OAObject, String, Object)} —
 *       override the value used for simple properties or LinkType=One.</li>
 * </ul>
 *
 * <p>The default implementation simply returns the provided values, meaning
 * a normal deep copy will be produced. Subclasses can selectively disable
 * copying or replace values to implement domain-specific behaviors.</p>
 */
public class OACopyCallback {
    
	/**
	 * Determines whether an owned Hub should be copied during a deep copy
	 * operation. The default behavior returns the provided {@code bDefault}
	 * value, allowing the caller's default decision to stand.
	 *
	 * @param oaObj    the source object being copied
	 * @param path     the property path for the owned Hub
	 * @param bDefault the default decision supplied by the caller
	 * @return {@code true} to copy the owned Hub, {@code false} otherwise
	 */
    public boolean shouldCopyOwnedHub(OAObject oaObj, String path, boolean bDefault) {
        return bDefault;
    }
    
    /**
     * Creates or selects the object that will be inserted into the copied
     * Hub during a deep copy operation. The default behavior returns the
     * supplied {@code currentValue}, which results in a normal deep-copy of
     * that object.
     *
     * @param oaObj        the source object being copied
     * @param path         the property path to the owned Hub
     * @param hub          the Hub that will receive the copied object
     * @param currentValue the current object referenced in the source Hub
     * @return the object to use in the copied Hub, typically {@code currentValue}
     */
    public OAObject createCopy(OAObject oaObj, String path, Hub hub, OAObject currentValue) {
        return currentValue;
    }


    /**
     * Determines the value to use when copying a simple property or a
     * LinkType=One reference during a deep copy. The default implementation
     * returns the provided {@code currentValue}, preserving the original value.
     *
     * @param oaObj        the source object being copied
     * @param path         the property path being copied
     * @param currentValue the current value of the property
     * @return the value to assign in the copied object, typically {@code currentValue}
     */
    public Object getPropertyValue(OAObject oaObj, String path, Object currentValue) {
        return currentValue;
    }
    
}
