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
 * Implements an MRU ("Most Recently Used") pattern on top of {@link HubCopy}.
 * <p>
 * Keeps a secondary Hub that mirrors the master but moves the Active Object to
 * the first position whenever it changes.
 *
 * <p>Useful for recent-item lists or quick-access caches.</p>
 */
public class HubMru<T extends OAObject> extends HubCopy<T> {

	/**
	 * Constructs an MRU-enabled HubCopy.
	 *
	 * <p>Initializes the superclass with the given master and copy Hubs
	 * and registers a listener that updates the MRU ordering whenever
	 * the Active Object on the master Hub changes.</p>
	 *
	 * @param hubMaster the source Hub whose Active Object determines MRU ordering
	 * @param hubCopy   the target Hub that maintains a reordered MRU list
	 */
	public HubMru(Hub<T> hubMaster, Hub<T> hubCopy) {
		super(hubMaster, hubCopy, false);

		HubListener hlHubMaster = new HubListenerAdapter<T>(this, "HubMru", "") {
			@Override
			public void afterChangeActiveObject(HubEvent<T> e) {
				updateMru();
			}
		};
		hubMaster.addHubListener(hlHubMaster);
		updateMru();
	}

	/**
	 * Updates the MRU (Most Recently Used) ordering in the copied Hub.
	 *
	 * <p>Moves the current Active Object of the master Hub to the first
	 * position of the copy Hub if it is present but not already first.</p>
	 *
	 * <p>Does nothing if the master Active Object is null or if the
	 * copy Hub is null.</p>
	 */
	protected void updateMru() {
		Object obj = hubMaster.getAO();
		if (obj == null) {
			return;
		}
		Hub h = getHub();
		if (h == null) {
			return;
		}

		int pos = h.getPos(obj);
		if (pos > 0) {
			getHub().move(pos, 0);
			getHub().setPos(0);
		}
	}

}
