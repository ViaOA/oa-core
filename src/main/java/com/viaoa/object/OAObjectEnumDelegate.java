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
 * Utility delegate that exposes enumeration (name/value) metadata
 * defined in {@link OAPropertyInfo} for a property of an {@link OAObject}.
 *
 * <p>Provides helper methods to obtain either raw or display-formatted
 * name/value pairs through {@link com.viaoa.hub.Hub}s, enabling UI
 * components to bind directly to property-level enumerations.</p>
 */
public class OAObjectEnumDelegate {

	/**
	 * Retrieves the enumeration name/value pairs defined for the specified
	 * property of the given class. The enumeration metadata is obtained
	 * from the corresponding {@link OAPropertyInfo}.
	 *
	 * @param clazz the class containing the property
	 * @param propertyName the property whose enumeration values are requested
	 * @return a hub containing the name/value entries, or {@code null}
	 *         if the property does not define enumeration metadata
	 */
	public static Hub<String> getNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getNameValues();
	}

	/**
	 * Retrieves the display-form enumeration name/value pairs defined for
	 * the specified property of the given class. This returns the set of
	 * display labels associated with the underlying enumeration values.
	 *
	 * @param clazz the class containing the property
	 * @param propertyName the property whose display enumeration values
	 *                     are requested
	 * @return a hub containing display-name entries, or {@code null}
	 *         if the property does not define enumeration metadata
	 */
	public static Hub<String> getDisplayNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getDisplayNameValues();
	}

}
