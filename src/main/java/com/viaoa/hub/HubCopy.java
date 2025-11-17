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
 * Mirrors all objects between two {@link Hub}s so that they contain the same elements,
 * allowing independent ordering and Active-Object (AO) management.
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Keeps {@code hubMaster} and {@code hubCopy} synchronized on adds/removes.</li>
 *   <li>Overrides removal hooks to propagate deletes back to {@code hubMaster}.</li>
 *   <li>Handles {@code onNewList} to repopulate {@code hubMaster} from the copy.</li>
 * </ul>
 *
 * <p>Useful when you need multiple sorted or filtered views of the same data set.</p>
 */
public class HubCopy<T extends OAObject> extends HubFilter<T> {

	public HubCopy(Hub<T> hubMaster, Hub<T> hubCopy, boolean bShareAO) {
		super(hubMaster, hubCopy, bShareAO);
	}

	// if object is directly removed from filtered hub, then remove from hubMaster
	@Override
	protected void afterRemoveFromFilteredHub(T obj) {
		if (hubMaster != null && hubMaster.contains(obj)) {
			hubMaster.remove(obj);
		}
	}

	@Override
	public boolean isUsed(T object) {
		if (object != objTemp) {
			return true;
		}
		return hubMaster.contains(object);
	}

	@Override
	public void afterRemoveAllFromFilteredHub() {
		if (hubMaster != null) {
			hubMaster.removeAll();
		}
	}

	@Override
	public void onNewList(HubEvent<T> e) {
		if (hubMaster == null) {
			return;
		}
		Hub h = weakHub.get();
		if (h == null) {
			return;
		}
		for (Object obj : h) {
			hubMaster.add((T) obj);
		}
	}

}
