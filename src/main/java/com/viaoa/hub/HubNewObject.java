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
import com.viaoa.object.OAObjectDSDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;

/**
 * Manages the creation and staging of new {@link OAObject OAObjects} before they
 * are formally added to a main {@link Hub}.
 *
 * <p>This helper is used when a UI workflow or wizard needs to prepare new
 * objects without immediately committing them to the main Hub’s list.  A
 * temporary "new object" Hub mirrors the configuration of the main Hub so that
 * links, filters, and master relationships are preserved while allowing
 * isolation until submission.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Hub<Customer> hubMain = new Hub<>(Customer.class);
 * HubNewObject<Customer> hubNew = new HubNewObject<>(hubMain);
 *
 * Customer c = hubNew.createNewObject();
 * hubNew.getNewObjectHub().add(c);
 * ...
 * hubNew.submit();  // moves new Customer to hubMain
 * }</pre>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Creates a secondary Hub configured with the same select-where
 *       filters and master relationships as the main Hub.</li>
 *   <li>Automatically disables {@code autoAdd} for staged objects so they
 *       aren’t re-added by select-all Hubs.</li>
 *   <li>Re-enables {@code autoAdd} when objects are removed or cancelled.</li>
 *   <li>Handles ID assignment through {@link OAObjectDSDelegate#assignId}
 *       when submitting newly created objects that require persistence keys.</li>
 *   <li>Provides {@link #submit()} and {@link #cancel()} operations to
 *       either move staged objects to the main Hub or discard them safely.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Uses {@link HubCombined} and {@link HubFilter} to bind the temporary
 *       Hub into the main Hub’s relationship graph without polluting its data.</li>
 *   <li>Ensures {@link OAObjectDelegate#initializeAfterLoading} is called
 *       after reflection-based construction for proper default initialization.</li>
 *   <li>Thread-local safety handled via {@link OAThreadLocalDelegate#setLoading}
 *       to suppress side-effects during object creation.</li>
 * </ul>
 */
public class HubNewObject<F extends OAObject> {

	private final Hub<F> hubMain;
	private Hub<F> hubNewObject;

	/**
	 * @param hubMain      "real" hub that wants to have a second hub used for creating new objects.
	 * @param hubNewObject "temp" hub that is used for holding new objects, and then adding to hubMain when submit is called.
	 */
	public HubNewObject(Hub<F> hubMain, Hub<F> hubNewObject) {
		this.hubMain = hubMain;
		this.hubNewObject = hubNewObject;
		setup();
	}

	public HubNewObject(Hub<F> hubMain) {
		this.hubMain = hubMain;
		setup();
	}

	public Hub<F> getNewObjectHub() {
		return hubNewObject;
	}

	public Hub<F> getMainHub() {
		return hubMain;
	}

	protected void setup() {
		if (hubNewObject == null) {
			hubNewObject = new Hub(hubMain.getObjectClass());
		}

		hubNewObject.setSelectWhereHub(	HubSelectDelegate.getSelectWhereHub(hubMain),
										HubSelectDelegate.getSelectWhereHubPropertyPath(hubMain));

		// need to set up a filtered hub, so that hubNewObject can be associated with hubMain and it's masterObject/Hub, etc
		Hub hubEmptyFiltered = new Hub(hubMain.getObjectClass());
		HubFilter hf = new HubFilter(hubMain, hubEmptyFiltered) {
			public boolean isUsed(Object object) {
				return false;
			}
		};

		new HubCombined(new Hub(), hubEmptyFiltered, hubNewObject);

		hubNewObject.onAdd((event) -> {
			Object obj = event.getObject();
			if (!(obj instanceof OAObject)) {
				return;
			}
			((OAObject) obj).setAutoAdd(false);
			hubMain.remove(obj); // in case it's a selectAll Hub and it was added
		});

		hubNewObject.onRemove((event) -> {
			Object obj = event.getObject();
			if (!(obj instanceof OAObject)) {
				return;
			}
			((OAObject) obj).setAutoAdd(true);
		});
	}

	/**
	 * Move objects in hubNewObject to hubMain.
	 */
	public void submit() {
		OAObject obj = hubNewObject.getAO();

		if (obj != null) {
			OAObjectKey ok = obj.getObjectKey();
			if (obj.isNew() && !ok.hasValidObjectIds()) {
				// obj.setObjectDefaults(); // 20240507 this should be called when object is created. 
				if (OAObjectDSDelegate.getAssignIdOnCreate(obj)) {
					OAObjectDSDelegate.assignId(obj);
				}
			}
		}

		hubMain.add(hubNewObject);
		hubNewObject.clear();
		hubMain.setAO(obj);
	}

	public void cancel() {
		OAObject obj = hubNewObject.getAO();
		hubNewObject.clear();

		if (obj != null) {
			OAObjectKey ok = obj.getObjectKey();
			if (obj.isNew() && !ok.hasValidObjectIds()) {
				obj.delete();
			}
		}
	}

	/**
	 * Create a new object that can then be added to hubNewObject;
	 * 
	 * @return
	 */
	public F createNewObject() {
		F obj = null;
		try {
			OAThreadLocalDelegate.setLoading(true);
			Class<F> clazz = hubMain.getObjectClass();
			obj = (F) OAObjectReflectDelegate.createNewObject(clazz);
		} finally {
			OAThreadLocalDelegate.setLoading(false);
		}
		OAObjectDelegate.initializeAfterLoading((OAObject) obj);
		return obj;
	}

}
