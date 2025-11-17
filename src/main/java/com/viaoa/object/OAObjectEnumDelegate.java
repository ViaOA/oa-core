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
	 * Get name/value pairs (enum) for a property.
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
	 * Get the display name for name/value pairs (enum) for a property.
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
