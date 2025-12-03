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
package com.viaoa.hub;

import com.viaoa.filter.OALikeFilter;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;

/**
 * Delegate for performing property-based searches within a {@link Hub}.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Use {@link com.viaoa.object.OAFinder} and {@link com.viaoa.filter.OALikeFilter}
 *       to locate objects by property path and value.</li>
 *   <li>Optionally set the Active Object when a match is found.</li>
 *   <li>Support incremental “find next” semantics through {@link OAFinder}.</li>
 * </ul>
 *
 * <p>Used internally by Hub’s {@code findFirst} and related lookup methods.
 */
public class HubFindDelegate {
	/**
	 * Finds the first object in the specified {@code Hub} whose property located by
	 * {@code propertyPath} matches the supplied {@code findValue} using a
	 * {@link com.viaoa.filter.OALikeFilter}.
	 *
	 * <p>If {@code bSetAO} is {@code true}, the found object is also set as the
	 * Hub’s active object.</p>
	 *
	 * @param thisHub the {@code Hub} to search; may be {@code null}
	 * @param propertyPath the property path to evaluate for matching
	 * @param findValue the value to compare against using a like-filter match
	 * @param bSetAO if {@code true}, sets the active object to the found object
	 * @param lastFoundObject the last object found, used by {@link com.viaoa.object.OAFinder#findNext}
	 * @return the first matching object, or {@code null} if none found
	 */
    public static Object findFirst(Hub thisHub, String propertyPath, final Object findValue, final boolean bSetAO, OAObject lastFoundObject) {
        if (thisHub == null) return null;
        
        OAFinder finder = new OAFinder();
        finder.addFilter(new OALikeFilter(propertyPath, findValue));
        Object foundObj = finder.findNext(thisHub, (OAObject) lastFoundObject);
        
        if (bSetAO) thisHub.setAO(foundObj);
        return foundObj;
	}
}

