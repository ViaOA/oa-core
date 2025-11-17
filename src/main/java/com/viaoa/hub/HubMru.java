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
 * Implements an MRU ("Most Recently Used") pattern on top of {@link HubCopy}.
 * <p>
 * Keeps a secondary Hub that mirrors the master but moves the Active Object to
 * the first position whenever it changes.
 *
 * <p>Useful for recent-item lists or quick-access caches.</p>
 */
public class HubMru<T extends OAObject> extends HubCopy<T> {

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
