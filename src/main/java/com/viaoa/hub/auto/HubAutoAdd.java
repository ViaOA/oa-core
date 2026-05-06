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
package com.viaoa.hub.auto;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

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
public class HubAutoAdd<TYPE extends OAObject, PROPTYPE extends OAObject> extends HubAutoMatch<TYPE, PROPTYPE> {

	/**
	 * Creates a HubAutoAdd instance that synchronizes additions from the master hub
	 * based on the specified property. Matching objects are added automatically, but
	 * never removed. Delegates construction to the superclass.
	 *
	 * @param hub             the target hub that will receive matching objects
	 * @param property        the property name used for matching
	 * @param hubMaster       the master hub providing source objects
	 * @param bManuallyCalled whether synchronization is triggered manually
	 */
	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster, boolean bManuallyCalled) {
		super(hub, property, hubMaster, bManuallyCalled);
	}

	/**
	 * Creates a HubAutoAdd instance with automatic synchronization behavior.
	 * Matching objects are added from the master hub but never removed.
	 * Delegates construction to the superclass, using {@code false} for
	 * manual-trigger mode.
	 *
	 * @param hub       the target hub that will receive matching objects
	 * @param property  the property name used for matching
	 * @param hubMaster the master hub providing source objects
	 */
	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster) {
		super(hub, property, hubMaster, false);
	}

	/**
	 * Always returns {@code false}, indicating that matching objects should never
	 * be removed from the target hub when using HubAutoAdd.
	 *
	 * @param obj           the object considered for removal
	 * @param propertyValue the evaluated property value
	 * @return {@code false} always
	 */
	@Override
	public boolean okToRemove(Object obj, Object propertyValue) {
		return false;
	}
}
