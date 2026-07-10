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

import com.viaoa.metadata.OAObjectInfo;

/**
 * Base class for transient or local-only {@link OAObject} instances.
 * <p>
 * This type is useful for view-model, UI-state, or process-state objects that
 * need normal OAObject property and Hub behavior but should not be persisted,
 * cached, or automatically initialized as persistent model objects.
 * <p>
 * Its static metadata marks the class as local-only, datasource-disabled,
 * cache-disabled, and initialization-disabled.
 */
public class OAObjectLocal extends OAObject {

	/**
	 * Static metadata definition for this local-only object type.
	 * Configured in the class initializer to disable persistence,
	 * caching, and automatic initialization so that instances of
	 * this class operate solely in memory.
	 */
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
