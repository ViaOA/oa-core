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

	/**
	 * Creates a new {@code HubCopy} that mirrors all objects between the
	 * master and copy hubs.
	 *
	 * @param hubMaster the master hub that will be kept in sync
	 * @param hubCopy   the hub whose contents will mirror the master
	 * @param bShareAO  whether to share Active-Object (AO) state
	 */
	public HubCopy(Hub<T> hubMaster, Hub<T> hubCopy, boolean bShareAO) {
		super(hubMaster, hubCopy, bShareAO);
	}

	/**
	 * Called after an object is removed from the filtered hub.
	 * If the master hub still contains the object, it is removed
	 * from the master hub as well.
	 *
	 * @param obj the object removed from the filtered hub
	 */
	@Override
	protected void afterRemoveFromFilteredHub(T obj) {
		if (hubMaster != null && hubMaster.contains(obj)) {
			hubMaster.remove(obj);
		}
	}

	/**
	 * Determines whether the specified object is considered “used.”
	 * Returns {@code true} when the object is not the temporary object,
	 * or when the master hub contains the object.
	 *
	 * @param object the object to evaluate
	 * @return {@code true} if the object is used; otherwise {@code false}
	 */
	@Override
	public boolean isUsed(T object) {
		if (object != objTemp) {
			return true;
		}
		return hubMaster.contains(object);
	}

	/**
	 * Invoked after all objects have been removed from the filtered hub.
	 * Removes all objects from the master hub if it exists.
	 */
	@Override
	public void afterRemoveAllFromFilteredHub() {
		if (hubMaster != null) {
			hubMaster.removeAll();
		}
	}

	/**
	 * Handles a new list event by repopulating the master hub
	 * with all objects from the filtered hub, if both exist.
	 *
	 * @param e the event signaling that a new list is available
	 */
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
