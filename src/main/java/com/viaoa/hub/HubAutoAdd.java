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
 * Variant of {@link HubAutoMatch} that adds matching objects but never removes them.
 * <p>
 * Used when synchronization between hubs should only insert objects from the master
 * Hub to the target Hub, without pruning those no longer matching.
 *
 * <p><b>Behavior</b>:
 * <ul>
 *   <li>Delegates all logic to {@link HubAutoMatch} except that {@link #okToRemove(Object, Object)} always returns false.</li>
 *   <li>Supports linking by property name and optional manual-trigger flag.</li>
 * </ul>
 */
public class HubAutoAdd<TYPE, PROPTYPE> extends HubAutoMatch<TYPE, PROPTYPE> {

	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster, boolean bManuallyCalled) {
		super(hub, property, hubMaster, bManuallyCalled);
	}

	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster) {
		super(hub, property, hubMaster, false);
	}

	@Override
	public boolean okToRemove(Object obj, Object propertyValue) {
		return false;
	}
}
