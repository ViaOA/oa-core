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
 * Automatically maintains a second {@link Hub} as a copy of another master Hub.
 *
 * <p>Whenever the master Hub’s list changes (via {@code onNewList}),
 * this class clears the copy Hub and repopulates it with all of the
 * master’s objects.  It is typically used for UI components that need
 * an isolated working list or multi-select view derived from another
 * Hub’s data.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Hub<Customer> hubAllCustomers = new Hub<>(Customer.class);
 * Hub<Customer> hubSelected     = new Hub<>(Customer.class);
 * new HubMakeCopy<>(hubAllCustomers, hubSelected);
 * }</pre>
 * The {@code hubSelected} Hub will always mirror the contents of
 * {@code hubAllCustomers}.
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Registers an {@link Hub#onNewList} listener on the master Hub.</li>
 *   <li>Invokes {@link #update()} immediately to initialize the copy.</li>
 *   <li>Implements a minimal two-Hub relationship without event recursion
 *       or shared data references.</li>
 *   <li>Intended for lightweight duplication, not deep cloning.</li>
 * </ul>
 */
public class HubMakeCopy<T extends OAObject> {

	/**
	 * References to the master Hub and the copy Hub.
	 * The copy Hub is kept synchronized with the master Hub’s contents.
	 */
	private Hub<T> hubMaster, hubCopy;

	/**
	 * Constructs a HubMakeCopy that keeps the specified copy Hub synchronized
	 * with the provided master Hub.
	 *
	 * @param hubMaster the source Hub whose list is mirrored
	 * @param hubCopy the Hub that receives and maintains the copied list
	 */
	public HubMakeCopy(Hub<T> hubMaster, Hub<T> hubCopy) {
		this.hubMaster = hubMaster;
		this.hubCopy = hubCopy;
		setup();
	}

	/**
	 * Initializes synchronization behavior between the master and copy Hubs.
	 * Registers an onNewList listener on the master Hub and performs the
	 * initial synchronization by calling {@link #update()}.
	 */
	protected void setup() {
		if (hubCopy == null || hubMaster == null) {
			return;
		}

		hubMaster.onNewList((e) -> {
			update();
		});
		update();
	}

	/**
	 * Clears the copy Hub and repopulates it with all objects from the
	 * master Hub. This provides lightweight list mirroring without deep cloning.
	 */
	protected void update() {
		hubCopy.clear();
		hubCopy.add(hubMaster);
	}
}
