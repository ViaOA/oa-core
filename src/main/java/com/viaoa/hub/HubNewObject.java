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

import com.viaoa.graph.object.OAObjectReflectService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectDSDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;

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

	/**
	 * The primary Hub into which newly created objects will ultimately be submitted.
	 * Represents the authoritative list that staged objects are destined for.
	 */
	private final Hub<F> hubMain;

	/**
	 * Temporary Hub used to stage newly created objects before they are committed
	 * to {@code hubMain}. Mirrors the configuration of the main Hub so that links,
	 * filtering, and master relationships are preserved during staging.
	 */
	private Hub<F> hubNewObject;

	/**
	 * Creates a HubNewObject manager using an explicitly supplied temporary Hub.
	 * Immediately initializes internal configuration via {@link #setup()}.
	 *
	 * @param hubMain      the main Hub that will ultimately receive submitted objects
	 * @param hubNewObject the temporary staging Hub used for creating new objects
	 */
	public HubNewObject(Hub<F> hubMain, Hub<F> hubNewObject) {
		this.hubMain = hubMain;
		this.hubNewObject = hubNewObject;
		setup();
	}

	/**
	 * Creates a HubNewObject manager using a newly constructed temporary Hub.
	 * The temporary Hub is initialized in {@link #setup()} based on the main Hub.
	 *
	 * @param hubMain the main Hub that will ultimately receive submitted objects
	 */
	public HubNewObject(Hub<F> hubMain) {
		this.hubMain = hubMain;
		setup();
	}

	/**
	 * Returns the temporary Hub used for staging newly created objects.
	 *
	 * @return the Hub that holds staged, not-yet-submitted objects
	 */
	public Hub<F> getNewObjectHub() {
		return hubNewObject;
	}

	/**
	 * Returns the main Hub that will receive objects when {@link #submit()} is called.
	 *
	 * @return the primary Hub for committed objects
	 */
	public Hub<F> getMainHub() {
		return hubMain;
	}

	/**
	 * Configures the temporary staging Hub to mirror the main Hub’s filtering and
	 * relationship structure. Ensures correct master/child bindings and disables
	 * automatic addition of new objects until they are formally submitted.
	 *
	 * <p>Behavior includes:</p>
	 * <ul>
	 *   <li>Creates a new temporary Hub if none was supplied.</li>
	 *   <li>Copies select-where filtering configuration from the main Hub.</li>
	 *   <li>Creates a filtered Hub and links it through {@link HubCombined} to maintain
	 *       relationship integrity without polluting the main Hub’s data.</li>
	 *   <li>On add: disables {@code autoAdd} and removes the object from the main Hub
	 *       if it was implicitly added via select-all.</li>
	 *   <li>On remove: re-enables {@code autoAdd} for the removed object.</li>
	 * </ul>
	 */
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
	 * Commits all staged objects in {@code hubNewObject} to {@code hubMain}.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Retrieves the active object from the staging Hub.</li>
	 *   <li>If the object is new and lacks assigned IDs, assigns an ID when required
	 *       by the data source delegate.</li>
	 *   <li>Adds all objects from the staging Hub into the main Hub.</li>
	 *   <li>Clears the staging Hub afterward.</li>
	 *   <li>Restores the Active Object on the main Hub to the committed object.</li>
	 * </ul>
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

	/**
	 * Cancels all staged new objects without submitting them to the main Hub.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Retrieves the active staged object.</li>
	 *   <li>Clears the staging Hub.</li>
	 *   <li>If the object is new and lacks valid object IDs, deletes it to avoid
	 *       leaving an uncommitted orphan instance.</li>
	 * </ul>
	 */
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
	 * Creates a new instance of the Hub’s object type for staging in
	 * {@code hubNewObject}.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Sets a thread-local loading flag to suppress side effects during construction.</li>
	 *   <li>Uses reflection to instantiate the object.</li>
	 *   <li>Ensures proper post-construction initialization via
	 *       {@link OAObjectDelegate#initializeAfterLoading(OAObject)}.</li>
	 * </ul>
	 *
	 * @return a newly created object instance ready for staging
	 */
	public F createNewObject() {
		F obj = null;
		try {
			OARuntime.get().threadLocals().setLoading(true);
			Class<F> clazz = hubMain.getObjectClass();
			final OAObjectReflectService srvcOAObjectReflect = OARuntime.get().graph(clazz).objects().getOAObjectReflectService();
			obj = (F) srvcOAObjectReflect.createNewObject(clazz);
		} finally {
			OARuntime.get().threadLocals().setLoading(false);
		}
		OAObjectDelegate.initializeAfterLoading((OAObject) obj);
		return obj;
	}

}
