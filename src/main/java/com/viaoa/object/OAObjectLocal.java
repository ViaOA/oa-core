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

/**
 * Base class for transient or local-only {@link OAObject} instances
 * that are excluded from persistence, caching, and initialization.
 *
 * <p>Used for view-model or UI-state objects that still need full
 * Hub and property-change behavior but should never touch a data source.</p>
 *
 * <p><b>Configuration</b> (applied in static initializer):
 * <ul>
 *   <li>{@code setLocalOnly(true)}</li>
 *   <li>{@code setUseDataSource(false)}</li>
 *   <li>{@code setAddToCache(false)}</li>
 *   <li>{@code setInitializeNewObjects(false)}</li>
 * </ul>
 *
 * <p>This provides a lightweight object type for transient logic within
 * an otherwise persistent OA graph.</p>
 */
public class OAObjectLocal extends OAObject {

	// Object Info 
	protected static OAObjectInfo oaObjectInfo;
	
	/**
	 * Returns the {@link OAObjectInfo} instance used to configure this
	 * local-only object type. The returned metadata disables persistence,
	 * caching, and automatic initialization so that instances operate only
	 * within memory.
	 *
	 * @return the metadata definition for this transient object class.
	 */
	public static OAObjectInfo getOAObjectInfo() {
	    return oaObjectInfo;
	}
	static {
	    oaObjectInfo = new OAObjectInfo(new String[] {});
	     
	    oaObjectInfo.setLocalOnly(true);
	    oaObjectInfo.setUseDataSource(false);
	    oaObjectInfo.setAddToCache(false);
	    oaObjectInfo.setInitializeNewObjects(false);
	}
	
}
