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


import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Keeps the Active Object (AO) of two {@link Hub} instances synchronized.
 *
 * <p>This listener allows two Hubs to share the same selected (active)
 * object so that changing the AO in one Hub automatically updates the
 * other.  It can operate in one-way or two-way mode.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<Customer> hubPrimary = new Hub<>(Customer.class);
 * Hub<Customer> hubMirror  = new Hub<>(Customer.class);
 *
 * // Two-way synchronization
 * new HubShareAO(hubPrimary, hubMirror);
 *
 * // One-way: hubPrimary → hubMirror
 * new HubShareAO(hubPrimary, hubMirror, true);
 * }</pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Listen for {@code afterChangeActiveObject} events on one or both Hubs.</li>
 *   <li>Set the corresponding AO on the paired Hub when the event occurs.</li>
 *   <li>Use {@link HubShareDelegate#isUsingSameSharedAO(Hub, Hub)} to avoid
 *       redundant updates when the Hubs already share a common AO source.</li>
 *   <li>Provide {@link #close()} to detach listeners and prevent memory leaks.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Lightweight utility used primarily by {@link HubShareDelegate} and
 *       {@link HubShareDelegate#shareActiveObject(Hub, Hub)}.</li>
 *   <li>Ensures bidirectional AO synchronization without sharing HubData or
 *       collections.</li>
 *   <li>Implements {@link HubListenerAdapter} for minimal callback overhead.</li>
 * </ul>
 */
public class HubShareAO<TYPE extends OAObject> extends HubListenerAdapter<TYPE> {
	/**
	 * The first Hub participating in Active Object synchronization.
	 * Changes to this Hub’s Active Object may propagate to {@code hub2}.
	 */
	private Hub<TYPE> hub1;

	/**
	 * The second Hub participating in Active Object synchronization.
	 * Changes to this Hub’s Active Object may propagate to {@code hub1},
	 * unless operating in one-way-only mode.
	 */
	private Hub<TYPE> hub2;

	/**
	 * Creates a HubShareAO that synchronizes Active Object changes between
	 * two Hubs, either one-way or bidirectionally.
	 *
	 * @param hub1        the first Hub to synchronize
	 * @param hub2        the second Hub to synchronize
	 * @param bOneWayOnly if true, only AO changes in {@code hub1} update {@code hub2};
	 *                    if false, updates propagate both ways
	 */
    public HubShareAO(Hub<TYPE> hub1, Hub<TYPE> hub2, boolean bOneWayOnly) {
        this.hub1 = hub1;
        this.hub2 = hub2;

        hub1.addHubListener(this);
        if (!bOneWayOnly) hub2.addHubListener(this);
    }

    /**
     * Creates a bidirectional Active Object synchronizer for the two Hubs.
     *
     * @param hub1 the first Hub to synchronize
     * @param hub2 the second Hub to synchronize
     */
	public HubShareAO(Hub<TYPE> hub1, Hub<TYPE> hub2) {
	    this(hub1, hub2, false);
	}

	/**
	 * Responds to Active Object changes on either Hub and updates the other
	 * Hub’s AO accordingly, unless both already share the same AO source.
	 *
	 * @param evt the HubEvent describing the AO change
	 */
    @Override
    public void afterChangeActiveObject(HubEvent<TYPE> evt) {
		final OA oa = OARuntime.oa(hub1);
        if (oa.internal().hubs().share().isUsingSameSharedAO(hub1, hub2)) {
            return;
        }
        Hub h = evt.getHub();
        Object obj = h.getAO();
        if (h == hub1) hub2.setAO(obj);
        else if (h == hub2) hub1.setAO(obj);
    }

    /**
     * Detaches this listener from both Hubs, stopping AO synchronization and
     * preventing memory leaks.
     */
	public void close() {
        hub1.removeHubListener(this);
        hub2.removeHubListener(this);
	}

	/**
	 * Returns the first Hub participating in synchronization.
	 *
	 * @return the first Hub
	 */
	public Hub<TYPE> getHub1() {
	    return hub1;
	}

	/**
	 * Returns the second Hub participating in synchronization.
	 *
	 * @return the second Hub
	 */
	public Hub<TYPE> getHub2() {
        return hub2;
    }
}
