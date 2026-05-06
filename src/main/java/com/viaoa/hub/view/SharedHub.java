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
package com.viaoa.hub.view;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubData;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Represents a {@link Hub} that shares the same data and object references
 * as another Hub, optionally sharing its active object (AO).
 *
 * <p>{@code SharedHub} enables multiple Hub instances to reflect the same
 * underlying object list, allowing independent navigation and selection
 * logic. When any shared object changes, all participating Hubs receive
 * the corresponding events.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<Department> hubDepartments = new Hub<>(Department.class);
 * hubDepartments.select();                 // populate list
 *
 * // Create a shared Hub that uses the same data
 * SharedHub<Department> hubDeptDropdown = new SharedHub<>(hubDepartments);
 *
 * // Or share both data and active object
 * SharedHub<Department> hubDeptMirror = new SharedHub<>(hubDepartments, true);
 * }</pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Use {@link HubShareDelegate#setSharedHub(Hub, Hub, boolean)} to
 *       wire this Hub to the source Hub’s {@link HubData} structure.</li>
 *   <li>Maintain synchronized object collections between all shared Hubs.</li>
 *   <li>Optionally share the same active object (AO) for unified selection
 *       behavior across views.</li>
 *   <li>Forward change notifications so all shared Hubs stay consistent.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Commonly used in UI contexts where the same objects appear in
 *       multiple components (e.g., master list + dropdown selector).</li>
 *   <li>Sharing avoids redundant {@code select()} calls and duplicate
 *       object instances while preserving independent navigation states.</li>
 *   <li>Created automatically via {@link Hub#createSharedHub(Hub)} when
 *       invoking {@code hub.createSharedHub(...)}.</li>
 * </ul>
 */
public class SharedHub<TYPE extends OAObject> extends Hub<TYPE> {
    
	/**
	 * Creates a SharedHub that mirrors the data and object references of the
	 * supplied Hub. Active-object sharing is disabled by default, allowing
	 * this Hub to maintain its own navigation state.
	 *
	 * @param hub the Hub whose data will be shared
	 */
    public SharedHub(Hub<TYPE> hub) {
    	this(hub, false);
    }

    /**
     * Creates a SharedHub that shares both the underlying data and, optionally,
     * the active object of the supplied Hub. When {@code bShareActiveObject}
     * is true, both Hubs track the same active object value.
     *
     * @param hub                 the Hub to share data with
     * @param bShareActiveObject  true to share active object state, false otherwise
     */
    public SharedHub(Hub<TYPE> hub, boolean bShareActiveObject) {
        if (hub != null) {
    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
            og.hubsInternal().callHubDataSetObjectClass(this, hub.getObjectClass());
            og.hubsInternal().callHubShareSetSharedHub(this, hub, bShareActiveObject);
        }
    }

    /**
     * Constructs a standalone SharedHub with the specified object type.
     * Data sharing must be configured later by linking it to another Hub.
     *
     * @param c the object class stored in this Hub
     */
    public SharedHub(Class<TYPE> c) {
        super(c);
    }
}

