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

import com.viaoa.object.OAObject;

/**
 * Lightweight structure holding the current Active Object (AO) for a Hub.
 * Shared Hubs that share the same AO reuse the same {@code HubDataActive}.
 *
 * <p>Cleared when a Hub is detached or a shared Hub that does not share
 * its AO resets to null.</p>
 */
public class HubDataActive<TYPE extends OAObject> implements java.io.Serializable {
//qqqqqqqq class was package protected	
	/**
	 * Serialization identifier used to maintain version compatibility when
	 * HubDataActive instances are serialized.
	 */
    static final long serialVersionUID = 1L;  // used for object serialization
	
    /**
     * The current Active Object (AO) for the owning Hub. Shared Hubs share
     * this value if configured to use the same active-object state.
     */
	protected transient volatile TYPE activeObject;
	
	
	/**
	 * Clears the current active object by setting it to {@code null}.
	 * The {@code eof} parameter is ignored.
	 *
	 * @param eof unused flag
	 */
	public void clear(boolean eof) {
	    activeObject = null;
	}
	
	/**
	 * Clears the current active object by setting it to {@code null}.
	 */
	public void clear() {
        activeObject = null;
    }

	public TYPE getActiveObject() {
		return activeObject;
	}
	public void setActiveObject(TYPE obj) {
		activeObject = obj;
	}
	
	
}

