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

import com.viaoa.object.OAObject;


/**
 * Convenience subclass of {@link Hub} that automatically builds its
 * contents using a {@link HubMerger}.
 *
 * <p>{@code MergedHub} dynamically merges the results of traversing
 * one or more property paths from a master-root Hub.  It is most
 * often used to flatten master/detail hierarchies into a single
 * read-only Hub view.</p>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Merge all OrderLine objects from every Order in hubOrders
 * Hub<OrderLine> hubLines = new MergedHub<>(OrderLine.class,
 *                                           hubOrders,
 *                                           "orderLines");
 *
 * // Merge with explicit options
 * Hub<OrderLine> hubAll = new MergedHub<>(OrderLine.class,
 *                                         hubOrders,
 *                                         "orderLines",
 *                                         true,     // share AO
 *                                         "id",     // sort order
 *                                         true);    // use all roots
 * }</pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Create and own a {@link HubMerger} that manages the flattened
 *       collection defined by the supplied property path.</li>
 *   <li>Expose the underlying {@link HubMerger} through
 *       {@link #getHubMerger()} for inspection or reconfiguration.</li>
 *   <li>Optionally construct an ad-hoc master Hub when initialized from
 *       a single {@link OAObject} instance.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Intended primarily as a shorthand for creating HubMerger-based
 *       views—no additional behavior beyond the wrapped {@code HubMerger}.</li>
 *   <li>Supports both shared-AO and independent-AO modes via constructor
 *       flags.</li>
 *   <li>Type-safe generic API ensures compile-time domain consistency.</li>
 * </ul>
 */
public class MergedHub<TYPE> extends Hub<TYPE> {
    
    private HubMerger hm;

    public MergedHub(Class<TYPE> clazz, Hub hubMasterRoot, String propertyPath) {
        super(clazz);
        this.hm = new HubMerger(hubMasterRoot, this, propertyPath, false, null, true); 
    }

    public MergedHub(Class<TYPE> clazz, Hub hubMasterRoot, String propertyPath, boolean bUseAll) {
        super(clazz);
        this.hm = new HubMerger(hubMasterRoot, this, propertyPath, false, null, bUseAll); 
    }
    
    public MergedHub(Class<TYPE> clazz, Hub hubMasterRoot, String propertyPath, boolean bShareActiveObject, String selectOrder, boolean bUseAll) {
    	super(clazz);
    	this.hm = new HubMerger(hubMasterRoot, this, propertyPath, bShareActiveObject, selectOrder, bUseAll); 
    }

    public HubMerger getHubMerger() {
        return this.hm;
    }

    public MergedHub(Class<TYPE> clazz, OAObject obj, String propertyPath) {
        super(clazz);
        
        Hub hubMasterRoot = new Hub(obj.getClass());
        hubMasterRoot.add(obj);
        hubMasterRoot.setPos(0);
        
        this.hm = new HubMerger(hubMasterRoot, this, propertyPath, false, null, true);
    }

}

