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
public class SharedHub<TYPE> extends Hub<TYPE> {
    
    /**
        Create a Hub that uses the same data/objects as another Hub.
        @param hub is the Hub that will be shared with.
    */
    public SharedHub(Hub<TYPE> hub) {
    	this(hub, false);
    }

    /**
        Create a Hub that uses the same data/objects as another Hub.
        @param hub is the Hub that will be shared with.
        @param bShareActiveObject if true then this Hub will also share/use the same active object as the hub.  Default is false.
    */
    public SharedHub(Hub<TYPE> hub, boolean bShareActiveObject) {
        if (hub != null) {
            HubDelegate.setObjectClass(this, hub.getObjectClass());
        	HubShareDelegate.setSharedHub(this, hub, bShareActiveObject);
        }
    }

    /**
        Create a Hub that will use the same data/objects as another Hub.
    */
    public SharedHub(Class<TYPE> c) {
        super(c);
    }
}

