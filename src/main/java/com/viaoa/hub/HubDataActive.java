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
package com.viaoa.hub;

/**
 * Lightweight structure holding the current Active Object (AO) for a Hub.
 * Shared Hubs that share the same AO reuse the same {@code HubDataActive}.
 *
 * <p>Cleared when a Hub is detached or a shared Hub that does not share
 * its AO resets to null.</p>
 */
class HubDataActive implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization
	
	/**
	    Current object in Hub that the active object.
	    @see Hub#setActiveObject
	    @see Hub#getActiveObject
	*/
	protected transient volatile Object activeObject;
	
	/**
	    Used by Hub.updateDetail() when calling setSharedHub, for Hubs that
	    do not shared same active object, so that active object is set to null.
	*/
	public void clear(boolean eof) {
	    activeObject = null;
	}
    public void clear() {
        activeObject = null;
    }
}

