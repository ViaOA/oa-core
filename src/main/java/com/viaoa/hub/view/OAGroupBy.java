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

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Generic link object used by {@code HubGroupBy} to represent a grouping node.
 *
 * <p>Each {@code OAGroupBy<G,F>} instance connects one <em>group-by object</em>
 * (for example, a Department) with the {@link com.viaoa.hub.Hub} of
 * <em>source/detail</em> objects that belong to that group (for example, its Employees).
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Provides typed references to both sides of the group relationship.</li>
 *   <li>Supports OA’s change-notification system via property-change firing.</li>
 *   <li>Marked {@code localOnly=true} to avoid persistence; it is a transient,
 *       runtime aggregation object.</li>
 * </ul>
 *
 * @param <F> Type of the member/detail OAObject
 * @param <G> Type of the group-by OAObject
 */
@OAClass(addToCache=false, initialize=false, useDataSource=false, localOnly=true)
public class OAGroupBy<F extends OAObject, G extends OAObject> extends OAObject {
    static final long serialVersionUID = 1L;
    
    public static final String P_GroupBy = "GroupBy"; 
    public static final String P_Hub = "Hub"; 
    public static final String PROPERTY_GroupBy = "GroupBy"; 
    public static final String PROPERTY_Hub = "Hub"; 
    private G groupBy;
    private Hub<F> hub;
    
    /**
     * Creates an empty grouping link object with no group-by value and no Hub.
     */
    public OAGroupBy() {
    }
    
    /**
     * Creates a grouping link object initialized with the specified group-by value.
     *
     * @param groupBy the group-by object to associate with this instance
     */
    public OAGroupBy(G groupBy) {
        setGroupBy(groupBy);
    }
    
    /**
     * Returns the group-by object associated with this instance.
     * <p>
     * If the value has not yet been resolved, it is retrieved using
     * {@link #getObject(String)} with {@link #PROPERTY_GroupBy}.
     * </p>
     *
     * @return the associated group-by object, or {@code null} if none assigned
     */
    @OAOne
    public G getGroupBy() {
        if (groupBy == null) {
            groupBy = (G) getObject(PROPERTY_GroupBy);
        }
        return this.groupBy;
    }
    
    /**
     * Sets the group-by object for this instance.
     * <p>
     * This method fires both before- and after-property-change events for
     * {@link #P_GroupBy}.
     * </p>
     *
     * @param obj the new group-by object
     */
    public void setGroupBy(G obj) {
        OAObject hold = this.groupBy;
        fireBeforePropertyChange(P_GroupBy, hold, obj);
        this.groupBy = obj;
        firePropertyChange(P_GroupBy, hold, obj);
    }
    
    /**
     * Returns the Hub of detail objects associated with this group.
     * <p>
     * If the Hub has not yet been resolved, it is retrieved using
     * {@link #getHub(String)} with {@link #PROPERTY_Hub}.
     * </p> 
     *
     * @return the Hub containing member/detail objects
     */
    @OAMany
    public Hub<F> getHub() {
        if (hub == null) {
            hub = (Hub<F>) getHub(PROPERTY_Hub);
        }
        return hub;
    }
}
