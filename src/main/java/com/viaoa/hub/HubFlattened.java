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

import com.viaoa.hub.HubListener.InsertLocation;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

/**
 * Flattens a recursive {@link Hub} structure into a single non-recursive Hub containing
 * all descendant objects from the root.
 *
 * <p>Typical for self-referential models (e.g., OrgUnits, Categories) where the
 * recursive link is defined by {@link OALinkInfo#getRecursiveLinkInfo(int)}.</p>
 *
 * <p><b>Mechanics</b>:
 * <ul>
 *   <li>Uses a {@link HubMerger} on the recursive link’s reverse name.</li>
 *   <li>Adds a listener so new objects created in the flattened Hub are attached back to the root chain.</li>
 *   <li>Validates that the source Hub’s type is truly recursive.</li>
 * </ul>
 */
public class HubFlattened<TYPE extends OAObject> {
	private Hub<TYPE> hubRoot;
	private Hub<TYPE> hubFlat;
	// private OALinkInfo liToRoot;
	private OALinkInfo liRecursiveToParent;
	private HubMerger<TYPE, TYPE> hm;

	public HubFlattened(Hub<TYPE> hubRoot, Hub<TYPE> hubFlat) {
		this.hubRoot = hubRoot;
		this.hubFlat = hubFlat;
		setup();
	}

	public Hub getRootHub() {
		return hubRoot;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		hm.close();
	}

	void setup() {
		if (hubFlat == null || hubRoot == null) {
			return;
		}

		// must be recursive
		OAObjectInfo oi = hubRoot.getOAObjectInfo();
		liRecursiveToParent = oi.getRecursiveLinkInfo(OALinkInfo.ONE);
		if (liRecursiveToParent == null) {
			throw new RuntimeException(hubRoot + " is not recursive");
		}

		// property that owns hubRoot
		// liToRoot = HubDetailDelegate.getLinkInfoFromDetailToMaster(hubRoot);

		hm = new HubMerger(hubRoot, hubFlat, liRecursiveToParent.getReverseName(), false, null, true, true, true);

		// make sure that any new object added to hubFlat(from a new command)  has correct link to parent/master
		HubListener hl = new HubListenerAdapter() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				if (e == null) {
					return;
				}

				if (liRecursiveToParent == null) {
					return;
				}
				if (liRecursiveToParent.getValue(e.getObject()) != null) {
					return;
				}
				hubRoot.add((TYPE) e.getObject()); // this will set it
			}
		};
		hl.setLocation(InsertLocation.LAST);
		hubFlat.addHubListener(hl);
	}
}
