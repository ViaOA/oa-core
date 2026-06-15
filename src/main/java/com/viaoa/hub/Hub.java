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

/*qqqqqqqqqq
CODEX

1. file/class/method: src/main/java/com/viaoa/hub/Hub.java:2789 addAll, src/main/java/com/viaoa/hub/Hub.java:2809
     addAll(int, Collection), src/main/java/com/viaoa/hub/Hub.java:2829 removeAll, src/main/java/com/viaoa/hub/
     Hub.java:2846 retainAll
  2. exact execution path: call addAll with an empty collection, all duplicates, or objects rejected by add; call
     removeAll with objects not present; or call retainAll with no membership changes. Each method returns true
     regardless of whether the Hub changed.
  3. why it is a real correctness bug: Hub implements List, and these methods use the Collection contract where the
     boolean reports whether the collection changed. Returning true for no-op or failed mutation creates false-
     success behavior for callers using the return value to drive persistence, events, retry, or tests.
  4. semantic/invariant violated: HUB_COLLECTION_MUTATION_RETURN_VALUE_REFLECTS_ACTUAL_CHANGE
  5. minimal fix or CODEX/defer recommendation: accumulate the result of each delegated add/remove/retain operation
     and return true only when membership actually changed. Null collection behavior should be decided explicitly;
     for List, null should throw, but if OA keeps null-as-no-op, return false.
  6. suggested regression test: addAll(Collections.emptyList()), addAll(existingObjects), removeAll(nonMembers), and
     retainAll(allCurrentObjects) should return false and leave Hub membership unchanged.

*/

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.stream.Stream;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.select.OASelect;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

/**
 * Core observable collection for OAObject graphs.
 * <p>
 * The Hub acts as an enhanced, observable {@link java.util.List} that maintains a
 * single "active object" (AO) and propagates all object and structural changes
 * throughout linked, shared, and detail Hubs. It is the foundation of OA’s
 * object-graph synchronization, event dispatch, and master-detail wiring.
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 *   <li>Maintain ordered membership of domain objects with optional sorting and filtering.</li>
 *   <li>Track and broadcast the current active object (AO) for UI and logic binding.</li>
 *   <li>Coordinate master-detail and link relationships among multiple Hubs.</li>
 *   <li>Support shared Hubs that reference a single data vector but have independent AOs.</li>
 *   <li>Integrate with OA’s data source layer via {@link com.viaoa.select.OASelect}.</li>
 *   <li>Propagate property, add/remove, and selection events through {@code HubEventDelegate}.</li>
 *   <li>Provide cloning, serialization, and change-tracking semantics for persistence and sync.</li>
 * </ul>
 *
 * <h3>Observability and Delegation</h3>
 * Hub delegates most behavior to specialized classes (e.g., {@code HubAddRemoveDelegate},
 * {@code HubAODelegate}, {@code HubDetailDelegate}, {@code HubShareDelegate}), keeping the
 * main class declarative and composition-based.  All observable mutations are funneled
 * through these delegates to ensure consistent event ordering, cascade rules, and
 * synchronization.
 *
 * <h3>Concurrency and Event Flow</h3>
 * Hubs are concurrency-aware: mutation paths avoid holding locks during listener
 * dispatch, and copy-on-write arrays are used for reentrant safety.  OAObjects forward
 * their property-change events to every Hub that references them, allowing Hubs to act
 * as the single observability root for UI and distributed clients.
 *
 * <h3>Typical Usage</h3>
 * <pre>{@code
 * Hub<Department> hubDept = new Hub<>(Department.class);
 * hubDept.select();                     // Load all Departments
 * Hub<Employee> hubEmp = hubDept.getDetailHub("employees");
 * hubDept.addHubListener(e -> { ... }); // Listen for changes
 * }</pre>
 *
 * This class is central to OA’s “object-automation” pattern, enabling reactive,
 * declarative binding between model, view, and persistence layers.
 */
public class Hub<TYPE extends OAObject> implements Serializable, List<TYPE>, Cloneable, Comparable<Hub<?>>, Iterable<TYPE> {
	
	/**
	 * Serialization version identifier used to validate compatibility when
	 * Hub instances are serialized and deserialized.
	 */
	static final long serialVersionUID = 1L; // used for object serialization

	/**
	 * Primary internal storage object containing the Hub's vector of objects
	 * and associated state such as size, type information, and refresh settings.
	 */
	protected volatile HubData<TYPE> data;

	/**
	 * Internal metadata object holding unique Hub configuration, including
	 * link-hub references, shared-hub pointer, default position, and detail lists.
	 */
	protected HubDataUnique<TYPE> datau;

	/**
	 * Internal structure storing active-object state, including the current
	 * active object and boundary flags (BOF/EOF).
	 */
	protected volatile HubDataActive<TYPE> dataa;

	/**
	 * Internal structure maintaining master-detail metadata, such as master
	 * object, master Hub, and link information used for navigation.
	 */
	protected HubDataMaster datam;

	/**
	 * Creates an empty Hub with no assigned object class. The Hub will accept
	 * objects of any type until an object is added, at which point the Hub’s
	 * object class is set.
	 */
	public Hub() {
		this((Class<TYPE>) null, 5);
	}

	/**
	 * Creates a Hub initialized with a single object. The Hub’s object class is
	 * set based on the supplied object.
	 *
	 * @param obj the initial object for the Hub
	 */
	public Hub(TYPE obj) {
		Class<TYPE> objClass = ((obj == null) ? (Class<TYPE>) null : (Class<TYPE>) obj.getClass());
		data = new HubData<>(objClass, 5);
		datau = new HubDataUnique<>();
		dataa = new HubDataActive<>();
		datam = new HubDataMaster();
		if (obj != null) {
			add(obj);
			this.setPos(0);
		}
	}

	/**
	 * Creates an empty Hub that is restricted to holding objects of the specified
	 * class.
	 *
	 * @param objClass the class of objects allowed in the Hub
	 */
	public Hub(Class<TYPE> objClass) {
		this(objClass, 5);
	}

	/**
	 * Creates an empty Hub for the specified object class and initializes the
	 * underlying storage with the given capacity.
	 *
	 * @param objClass the class of objects allowed in the Hub
	 * @param vecSize  the initial capacity
	 */
	public Hub(Class<TYPE> objClass, int vecSize) {
		data = new HubData<>(objClass, vecSize);
		datau = new HubDataUnique<>();
		dataa = new HubDataActive<>();
		datam = new HubDataMaster();
	}

	/**
	 * Creates an empty Hub for the specified object class with an initial
	 * capacity and a size increment used when growing the internal storage.
	 *
	 * @param objClass      the class of objects allowed in the Hub
	 * @param vecSize       the initial capacity
	 * @param incrementSize the amount to grow the internal storage when needed
	 */
	public Hub(Class<TYPE> objClass, int vecSize, int incrementSize) {
		data = new HubData<>(objClass, vecSize, incrementSize);
		datau = new HubDataUnique<>();
		dataa = new HubDataActive<>();
		datam = new HubDataMaster();
	}

	/**
	 * Creates a detail Hub linked to the specified master Hub. The object class
	 * is determined from the master Hub’s active object relationship.
	 *
	 * @param masterHub the master Hub for this detail Hub
	 */
	public Hub(Hub<TYPE> masterHub) {
		this(masterHub == null ? (Class) null : masterHub.getObjectClass(), 5);
		if (masterHub != null) {
			OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
			og.hubsInternal().callHubShareSetSharedHub(this, masterHub, false);
		}
	}

	/**
	 * Creates a Hub linked to a master object using the supplied link
	 * information. Optionally initializes a SelectHub when requested.
	 *
	 * @param clazz         the class of objects allowed in the Hub
	 * @param masterObject  the master object associated with this Hub
	 * @param linkInfo      the link information defining the relationship
	 * @param bCreateSelect true to create an associated SelectHub
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public Hub(Class<TYPE> clazz, OAObject masterObject, OALinkInfo linkInfo, boolean bCreateSelect) {
		this(clazz, 5);
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		if (linkInfo == null) {
			og.hubsInternal().callHubDetailSetMasterObject(this, masterObject);
		} else {
			og.hubsInternal().callHubDetailSetMasterObject(this, masterObject, linkInfo);

			if (bCreateSelect) {
				// create select, but dont call select.select(), since it could be
				// coming from server. See: OAObjectReflectDelegate.getReferenceHub(..)
				
				OASelect sel = og.hubsInternal().callHubSelectGetSelect( (Hub<? extends OAObject>) this, true);
				if (masterObject != null) {
					sel.setWhereObject(masterObject);
					sel.setPropertyFromWhereObject(linkInfo.getReverseName());
				}
			}
		}
	}

	/**
	 * Creates a Hub for objects of the given class and associates it with the
	 * specified master object.
	 *
	 * @param clazz         the class of objects allowed in the Hub
	 * @param masterObject  the master object associated with this Hub
	 */
	public Hub(Class<TYPE> clazz, OAObject masterObject) {
		this(clazz, masterObject, null, true);
	}

	/**
	 * Ensures that the Hub has internal storage capacity for at least the
	 * specified number of objects.
	 *
	 * @param size the required capacity
	 */
	public void ensureCapacity(int size) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDataEnsureCapacity(this, size);
	}

	/**
	 * Reduces the internal storage capacity to match the current number of
	 * objects in the Hub.
	 */
	public void resizeToFit() {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDataResizeToFit(this);
	}

	/**
	 * Custom serialization logic for writing the Hub’s state to the output
	 * stream.
	 *
	 * @param os the stream to write to
	 * @throws IOException if an error occurs during serialization
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSerializeWriteObject(this, stream);
	}

	/**
	 * Restores transient fields after deserialization and returns the resolved
	 * Hub instance.
	 *
	 * @return the resolved Hub
	 */
	protected Object readResolve() throws ObjectStreamException {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSerializeReadResolve(this);
	}

	/**
	 * Assigns a dynamic property value to this Hub under the specified name.
	 *
	 * @param name the property name
	 * @param obj  the value to assign
	 */
	public void setProperty(String name, Object obj) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubPropertySetProperty(this, name, obj);
	}

	/**
	 * Retrieves the value of a dynamic property assigned to this Hub.
	 *
	 * @param name the property name
	 * @return the stored value, or null if none exists
	 */
	public Object getProperty(String name) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubPropertyGetProperty(this, name);
	}

	/**
	 * Removes the dynamic property associated with the specified name.
	 *
	 * @param name the property to remove
	 */
	public void removeProperty(String name) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubPropertyRemoveProperty(this, name);
	}

	/**
	 * Returns a string representation of the Hub. Includes defensive logic to
	 * avoid infinite loops when encountered through nested Hub relationships.
	 *
	 * @return the string representation of this Hub
	 */
	public String toString() {
		return _toString(0, null);
	}

	/**
	 * Recursively builds the string representation of the Hub, tracking visited
	 * Hubs to prevent infinite recursion.
	 *
	 * @param cnt   the indentation or recursion depth
	 * @param alHub the list of Hubs already visited
	 * @return the generated string
	 */
	private String _toString(int cnt, ArrayList<Hub> alHub) {
		if (datau == null) {
			return "Hub";
		}
		String s = OAString.getClassName(this.getClass()) + "." + OAString.getClassName(data.objClass);
		// was:  super._toString() datau.objClass;

		if (alHub != null) {
			if (alHub.contains(this)) {
				OAObjectInfo oi = getOAObjectInfo();
				if (oi.getRecursiveLinkInfo(OALinkInfo.MANY) != null) {
					return " ... (recursive)";
				}
				return " - ERROR: hub has a  endless loop of references, current Hub=" + s;
			}
			if (alHub.size() > 20) {
				return " ... note: hub has more then 20+ ";
			}
		}

		if (datau.getSharedHub() != null) {
			if (cnt > 5) {
				if (alHub == null) {
					alHub = new ArrayList<Hub>(5);
				}
				alHub.add(this);
			}
			s += "->Shared:" + datau.getSharedHub()._toString(cnt + 1, alHub);
		} else {
			/* 20151111 dont call select methods, since this could cause a deadlock
			 *  ** alling toString should not have any side effects
			OASelect sel = data.getSelect();
			if (sel != null) {
			    boolean b = sel.isCounted();
			
			    if (!sel.hasBeenStarted()) {
			        if (b) s += " counted: " + sel.getCount() + ", ";
			        s += " not selected";
			    }
			    else {
			        if (sel.hasMore()) {
			            if (b) s += ",counted:" + sel.getCount() + ", ";
			            s += ",currentSize:" + getCurrentSize();
			            s += ",moreData=true";
			        }
			        else s += ",size:" + getSize();
			    }
			}
			else {
			*/
			s += ",csize:" + getCurrentSize();
			//}

			
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
			
			HubDataMaster dm = og.hubsInternal().callHubDetailGetDataMaster(this);
			if (dm.getMasterHub() != null) {
				if (cnt > 5) {
					if (alHub == null) {
						alHub = new ArrayList<Hub>();
					}
					alHub.add(this);
				}
				s += ">MasterHub:" + dm.getMasterHub()._toString(cnt + 1, alHub);
			} else if (dm.getMasterObject() != null) {
				s += ">MasterObject:" + dm.getMasterObject();
			}
		}
		return s;
	}

	/**
	 * Enables or disables the Hub’s automatic refresh behavior.
	 *
	 * @param b true to enable refresh, false to disable
	 */
	public void setRefresh(boolean b) {
		data.setRefresh(b);
	}

	/**
	 * Returns whether automatic refresh is enabled for this Hub.
	 *
	 * @return true if refresh is enabled
	 */
	public boolean getRefresh() {
		return data.isRefresh();
	}

	/**
	 * Returns whether this Hub or its objects contain changes according to the
	 * specified cascade rule.
	 *
	 * @param cascadeRule the cascade rule to apply
	 * @return true if changes are detected
	 */
	public boolean getChanged(int cascadeRule) {
		if (data.changed) {
			return true;
		}
		OACascade cascade = new OACascade();
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubStatusGetChanged(this, cascadeRule, cascade);
	}

	/**
	 * Sets the changed flag for this Hub, marking that structural modifications
	 * (add/insert/remove/replace) have occurred. Does not account for changes
	 * within individual objects stored in the Hub.
	 *
	 * @param b true to mark the Hub as changed, false to clear the flag
	 */
	public void setChanged(boolean b) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubStatusSetChanged(this, b);
	}

	/**
	 * Copies all loaded objects in this Hub into the supplied array. Ensures all
	 * data is loaded before copying.
	 *
	 * @param anArray destination array to populate
	 */
	public void copyInto(TYPE[] anArray) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectLoadAllData(this);
		og.hubsInternal().callHubDataCopyInto(this, anArray);
	}

	/**
	 * Returns a new array containing all objects currently in this Hub. Ensures
	 * all data is loaded before creating the array.
	 *
	 * @return array of the Hub's objects
	 */
	public TYPE[] toArray() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectLoadAllData(this);
		return (TYPE[]) og.hubsInternal().callHubDataToArray(this);
	}

	/**
	 * Copies all objects from this Hub into the supplied array. If the supplied
	 * array is not the same size as this Hub, a new array of the correct type
	 * and size is created. Ensures all data is loaded before copying.
	 *
	 * @param anArray array to populate, or replaced if sizes differ
	 * @return populated array
	 */
	@Override
	public <TYPE> TYPE[] toArray(TYPE[] anArray) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectLoadAllData(this);
		int x1 = anArray.length;
		int x2 = getSize();
		if (x1 != x2) {
			anArray = (TYPE[]) Array.newInstance(getObjectClass(), x2);
		}
		for (int i = 0; i < x2; i++) {
			Object obj = this.elementAt(i);
			if (obj == null) {
				break;
			}
			anArray[i] = (TYPE) obj;
		}
		return anArray;
	}

	/**
	 * Returns a List containing all objects in this Hub. Ensures that all data
	 * has been fully loaded before iterating.
	 *
	 * @return List of objects from this Hub
	 */
    public List<TYPE> toList() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
        og.hubsInternal().callHubSelectLoadAllData(this);
        List<TYPE> al = new ArrayList<>();
        for (TYPE obj : this) {
            al.add(obj);
        }
        return al;
    }
	
	
    /**
     * Copies all objects from this Hub into the supplied Hub. Objects already
     * present in the destination Hub are not added again.
     *
     * @param h Hub to copy objects into
     */
	public void copyInto(Hub<TYPE> h) {
		if (h == null) {
			return;
		}
		for (int i = 0;; i++) {
			TYPE obj = this.elementAt(i);
			if (obj == null) {
				break;
			}
			if (!h.contains(obj)) {
				h.add(obj);
			}
		}
	}

	/**
	 * Returns the Class of objects stored in this Hub.
	 *
	 * @return object Class
	 */
	public Class<TYPE> getObjectClass() {
		return data.objClass;
	}

	/**
	 * Cleanup during garbage collection. Removes this Hub from shared or select
	 * structures and unregisters Hub references from contained OAObjects.
	 *
	 * @throws Throwable if an error occurs during finalization
	 */
	protected void finalize() throws Throwable {
		super.finalize();

		Hub hx;
		if (this.datau != null) {
			hx = this.datau.getSharedHub();
		} else {
			hx = null;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		if (hx != null) {
			og.hubsInternal().callHubShareRemoveSharedHub(hx, this);
		} else {
			og.hubsInternal().callHubSelectCancelSelect(this, true);
			Vector vec = data.vector;
			if (vec != null) {
				try {
					int x = vec.size();
					for (int i = 0; i < x; i++) {
						Object obj = vec.get(i);
						if (obj instanceof OAObject) {
							OAGraphInternal ogi = (OAGraphInternal) OARuntime.graph(this);
							ogi.objectsInternal().callObjectHubRemoveHub((OAObject) obj, (Hub<OAObject>) this, true);
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
					//System.out.println("Hub.finalize exception="+e);
				}
			}
		}
	}

	/**
	 * Returns true if additional data remains to be loaded from the underlying
	 * OASelect.
	 *
	 * @return true if more data can be loaded
	 */
	public boolean isMoreData() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSelectIsMoreData(this);
	}

	/**
	 * Loads all remaining data from the underlying OASelect into this Hub.
	 */
	public void loadAllData() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectLoadAllData(this);
	}

	/**
	 * Returns the number of objects currently loaded into this Hub without
	 * triggering a select operation.
	 *
	 * @return count of loaded objects
	 */
	public int getCurrentSize() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDataGetCurrentSize(this);
	}

	/**
	 * Returns the number of objects in this Hub, performing a select operation
	 * if necessary to retrieve the full count.
	 *
	 * @return total number of objects
	 */
	public int getSize() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSizeGetSize(this);
	}

	/**
	 * Returns the number of objects in this Hub, equivalent to getSize().
	 *
	 * @return size of Hub
	 */
	public int size() {
		return getSize();
	}

	/**
	 * Waits until all data is loaded and returns the fully loaded size of this
	 * Hub.
	 *
	 * @return loaded size
	 */
	public int getLoadedSize() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSizeGetLoadedSize(this);
	}

	/**
	 * Saves all objects in this Hub using OAObject.save(), applying cascade
	 * rules. Aborts if any save operation fails.
	 */
	public void saveAll() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		boolean b = srvcOAThreadLocal.setAdmin(true);
		try {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
			og.hubsInternal().callHubSaveSaveAll(this, OAObject.CASCADE_LINK_RULES);
		} finally {
			if (!b) srvcOAThreadLocal.setAdmin(b);
		}
	}

	public void saveAll(int iCascadeRule) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSaveSaveAll(this, iCascadeRule);
	}

	/**
	 * Deletes all objects stored in this Hub. Does not abort if individual
	 * objects fail to delete.
	 */
	public void deleteAll() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDeleteDeleteAll(this);
	}

	/**
	 * Returns true if this Hub is in the process of deleting all of its objects.
	 *
	 * @return deletion-in-progress flag
	 */
	public boolean isDeletingAll() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDeleteIsDeletingAll(this);
	}

	/**
	 * Creates a shallow clone of this Hub. Loaded data is copied but listeners,
	 * active-object state, and unique metadata are not cloned. Objects
	 * themselves are not cloned.
	 *
	 * @return cloned Hub
	 * @throws CloneNotSupportedException if cloning fails
	 */
	public Object clone() throws CloneNotSupportedException {
		super.clone();
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectLoadAllData(this);
		Hub h = new Hub(this.getObjectClass());
		og.hubsInternal().callHubDataClone(this, h);
		return h;
	}

	@Override
	public int compareTo(Hub<?> obj) {
		if (obj == null) return 1;
		if (obj == this) return 0;
		return Integer.compare(System.identityHashCode(this), System.identityHashCode(obj));
	}
	
	
	/**
	 * Retrieves an object from this Hub by matching its key. Uses the object's
	 * hashCode() and equals() (or OAObjectKey comparison) to determine a match.
	 *
	 * @param key value used to locate a matching object
	 * @return matching object, or null if not found
	 */
	public TYPE getObject(Object key) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubDataGetObject(this, key);
	}

	/**
	 * Returns the object at the specified zero-based position or null if the
	 * position exceeds the Hub size.
	 *
	 * @param pos index of the object
	 * @return object at position or null
	 */
	public TYPE getObjectAt(int pos) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubDataGetObjectAt(this, pos);
	}

	/**
	 * Returns the object at the specified position. Same as getObjectAt().
	 *
	 * @param pos index of the object
	 * @return object at position or null
	 */
	public TYPE getAt(int pos) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubDataGetObjectAt(this, pos);
	}

	/**
	 * Returns the last object in this Hub, or null if the Hub is empty.
	 *
	 * @return last object or null
	 */
	public TYPE getLast() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		int pos = getSize() - 1;
		if (pos < 0) {
			return null;
		}
		return (TYPE) og.hubsInternal().callHubDataGetObjectAt(this, pos);
	}

	/**
	 * Returns whether the supplied object exists within this Hub.
	 *
	 * @param obj object to search for, can be OAObject or pkey value(s)
	 * @return true if the object is present
	 */
	@Override
	public boolean contains(Object obj) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDataContains(this, obj);
	}

	/**
	 * Returns the position of the supplied object in this Hub without adjusting
	 * master/detail Hubs. Returns -1 if the object is not found.
	 *
	 * @param obj object to locate
	 * @return position or -1
	 */
	@Override
	public int indexOf(Object obj) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDataGetPos(this, obj, false, false);
	}

	/**
	 * Returns the object at the specified position. Mimics Vector.elementAt().
	 *
	 * @param pos index of the object
	 * @return object at position or null
	 */
	public TYPE elementAt(int pos) { // mimic Vector
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubDataGetObjectAt(this, pos);
	}

	/**
	 * Returns the current active object (AO), or null if none is set.
	 *
	 * @return active object
	 */
	public TYPE getActiveObject() {
		return (TYPE) dataa.activeObject;
	}

	/**
	 * Convenience wrapper for getActiveObject().
	 *
	 * @return active object
	 */
	public TYPE getAO() {
		return (TYPE) getActiveObject();
	}

	/**
	 * Sets the active object based on the supplied position. If the position is
	 * invalid, the active object becomes null.
	 *
	 * @param pos index to activate
	 * @return newly active object or null
	 */
	public TYPE setActiveObject(int pos) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubAOSetActiveObject(this, pos);
	}

	public TYPE setActiveObject(Object obj) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubAOSetActiveObject(this, obj);
	}

	/**
	 * Sets the active object to the supplied object. If the object does not
	 * exist in this Hub, the active object is set to null.
	 *
	 * @param object object to activate
	 */
	public void setActiveObject(TYPE object) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAOSetActiveObject(this, object);
	}
	
	/**
	 * Convenience wrapper for setActiveObject(int).
	 *
	 * @param pos index to activate
	 * @return new active object or null
	 */
	public Object setAO(int pos) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubAOSetActiveObject(this, pos);
	}

	public TYPE setAO(Object obj) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubAOSetActiveObject(this, obj);
	}
	
	
	/**
	 * Convenience wrapper for setActiveObject(Object).
	 *
	 * @param object object to activate
	 */
	public void setAO(TYPE object) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAOSetActiveObject(this, object);
	}

	/**
	 * Reapplies the current active object to force update propagation.
	 */
	public void resetAO() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAOSetActiveObjectForce(this, getAO());
	}

	/**
	 * Returns the root Hub for recursive Hub structures, or null if this Hub is
	 * not part of a recursive hierarchy.
	 *
	 * @return root Hub or null
	 */
	public Hub getRootHub() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubRootGetRootHub(this);
	}

	/**
	 * Marks this Hub as a root Hub within recursive Hub structures.
	 */
	public void setRootHub() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubRootSetRootHub(this, true);
	}

	/**
	 * Sets the Hub that will receive this Hub's active object whenever the
	 * active object is changed. Clears the current AO afterward.
	 *
	 * @param addHub destination Hub
	 */
	public void setAddHub(Hub<TYPE> addHub) {
		datau.setAddHub(addHub);
		setAO(null);
	}

	/**
	 * Returns the Hub that receives the active objects from this Hub whenever
	 * the active object changes.
	 *
	 * @return addHub or null
	 */
	public Hub<TYPE> getAddHub() {
		return datau.getAddHub();
	}

	/**
	 * Returns the master Hub for shared Hub chains. If this Hub is not shared,
	 * this Hub itself is returned.
	 *
	 * @return underlying non-shared Hub
	 */
	public Hub getRealHub() {
		Hub h = this;
		for (;;) {
			if (h.datau.getSharedHub() == null) {
				break;
			}
			h = h.datau.getSharedHub();
		}
		return h;
	}

	/**
	 * Returns whether this Hub is owned by a master object.
	 *
	 * @return true if this Hub has a master owner
	 */
	public boolean isOwned() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailIsOwned(this);
	}

	/**
	 * Marks a property as unique within this Hub. No two objects in this Hub
	 * may have the same value for the specified property.
	 *
	 * @param propertyName name of property to enforce uniqueness on
	 */
	public void setUniqueProperty(String propertyName) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubPropertySetUniqueProperty(this, propertyName);
	}

	/**
	 * Sets the default active-object position to use whenever a new list is
	 * generated for this Hub. A value of -1 prevents automatically selecting an
	 * active object.
	 *
	 * @param pos default position
	 */
	public void setDefaultPos(int pos) {
		datau.setDefaultPos(pos);
	}

	/**
	 * Returns the default active-object position for this Hub.
	 *
	 * @return default position, or -1 if none
	 */
	public int getDefaultPos() {
		return datau.getDefaultPos();
	}

	/**
	 * Sets the active object to the object at the specified position. If the
	 * position is invalid, the active object becomes null.
	 *
	 * @param pos index of object to activate
	 * @return activated object or null
	 */
	public TYPE setPos(int pos) {
		return setActiveObject(pos);
	}

	/**
	 * Returns the position of the current active object within this Hub, or -1
	 * if the active object is null or not found.
	 *
	 * @return position of active object or -1
	 */
	public int getPos() {
		int result = getPos(dataa.activeObject);
		return result;
	}

	/**
	 * Returns the position of the supplied object within this Hub without
	 * adjusting master/detail relationships. Returns -1 if not found.
	 *
	 * @param object object whose position is requested
	 * @return position or -1
	 */
	public int getPos(Object object) {
		// 20150203 changed to not update master/detail if object is not in this hub
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDataGetPos(this, object, false, false);
		//was: return og.hubsInternal().callHubDataGetPos(this, object, true, true);
	}

	/**
	 * Returns the position of an object within this Hub, with optional
	 * adjustment of master/detail hubs when necessary.
	 *
	 * @param object        object to locate
	 * @param bAdjustMaster true to adjust master/detail hubs
	 * @return position or -1
	 */
	public int getPos(Object object, boolean bAdjustMaster) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDataGetPos(this, object, bAdjustMaster, false);
	}

	/**
	 * Adds an object to this Hub. If sorted, the object is inserted at its
	 * proper sorted location. All add events are dispatched.
	 *
	 * @param obj object to add
	 * @return true if the object was added
	 */
	@Override
	public boolean add(TYPE obj) {
		if (obj == null) return false;
		Class c = obj.getClass();
		if (data.getObjClass() == null) {
			data.setObjClass(c);
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		return og.hubsInternal().callHubAddRemoveAdd(this, obj);
	}

	/**
	 * Adds all objects from the supplied List into this Hub. Null lists are
	 * ignored.
	 *
	 * @param list List of objects to add
	 */
	public void add(List<TYPE> list) {
		if (list == null) return;
		Class c = this.getObjectClass();
		if (c == null && list.size() > 0) c = list.get(0).getClass();
		if (data.getObjClass() == null) {
			data.setObjClass(c);
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		for (TYPE obj : list) {
			og.hubsInternal().callHubAddRemoveAdd(this, obj);
		}
	}

	/**
	 * Adds all objects from the supplied Hub into this Hub. Null hubs are
	 * ignored.
	 *
	 * @param hub Hub containing objects to add
	 */
	public void add(Hub<TYPE> hub) {
		if (hub == null) {
			return;
		}
		/* 20200522 removed, caller should setLoading(..) if it's needed
		 * otherwise, events from add will check isLoading and not run code.  Ex: M2M wont be set
		
		boolean b = (getSize() == 0);
		if (b) {
			OARuntime.threadLocals().setLoading(true);
		}
		*/
		Class c = this.getObjectClass();
		if (c == null) c = hub.getObjectClass();
		if (data.getObjClass() == null) {
			data.setObjClass(c);
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		try {
			for (TYPE obj : hub) {
				og.hubsInternal().callHubAddRemoveAdd(this, obj);
			}
		} finally {
			/*
			if (b) {
				OARuntime.threadLocals().setLoading(false);
				og.hubsInternal().callHubEventFireOnNewListEvent(this, true);
			}
			*/
		}
	}

	/**
	 * Returns whether add/remove operations are permitted for this Hub. Takes
	 * disabled state and callback constraints into account.
	 *
	 * @return true if modification is allowed
	 */
	public boolean getEnabled() {
		if (data.isDisabled()) {
			return false;
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.objectsInternal().callObjectCallbackGetAllowEnabled(OAObjectCallback.CHECK_CallbackMethod, (Hub<OAObject>) this, null, null);
	}

	/**
	 * Enables or disables add/remove operations for this Hub.
	 *
	 * @param b true to enable, false to disable
	 */
	public void setEnabled(boolean b) {
		this.data.setDisabled(!b);
	}

	/**
	 * Adds an object to this Hub. Mimics Vector.addElement(). Delegates to
	 * add(obj).
	 *
	 * @param obj object to add
	 */
	public void addElement(TYPE obj) {
		add(obj);
	}

	/**
	 * Swaps the objects at the two supplied positions. If either position is
	 * invalid, no action is taken.
	 *
	 * @param pos1 first position
	 * @param pos2 second position
	 */
	public void swap(int pos1, int pos2) {
		Class c = this.getObjectClass();
		if (c == null) return;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		og.hubsInternal().callHubAddRemoveSwap(this, pos1, pos2);
	}

	/**
	 * Moves an object from one position to another within this Hub. Sends a
	 * move event to listeners.
	 *
	 * @param posFrom original position
	 * @param posTo   destination position
	 */
	public void move(int posFrom, int posTo) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAddRemoveMove(this, posFrom, posTo);
	}

	/**
	 * Inserts an object at the specified position. If the Hub is sorted, the
	 * object is placed according to sort order instead. Sends insert events.
	 *
	 * @param obj object to insert
	 * @param pos target position
	 * @return true if successful
	 */
	public boolean insert(TYPE obj, int pos) {
		if (obj == null) return false;
		Class c = obj.getClass();
		if (data.getObjClass() == null) {
			data.setObjClass(c);
		}
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		return og.hubsInternal().callHubAddRemoveInsert(this, obj, pos);
	}

	
	/**
	 * Removes and returns the object at the supplied position. Sends a remove
	 * event. If the position is invalid, returns null.
	 *
	 * @param pos index of object to remove
	 * @return removed object or null
	 */
	@Override
	public TYPE remove(int pos) {
		if (pos < 0) return null;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveRemove(this, pos);
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null) return false;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveRemove(this, obj);
	}
	
	public boolean remove(TYPE obj) {
		if (obj == null) return false;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj.getClass());
		return og.hubsInternal().callHubAddRemoveRemove(this, obj);
	}

	
	
	/**
	 * Convenience wrapper for remove(int).
	 *
	 * @param pos index of object to remove
	 * @return removed object or null
	 */
	public TYPE removeAt(int pos) {
		return this.remove(pos);
	}

	/**
	 * Replaces the object at the specified position with a new object. Removes
	 * the old object, inserts the new one, and adjusts the active object if
	 * necessary.
	 *
	 * @param pos position at which to replace
	 * @param obj new object
	 */
	public void replace(int pos, TYPE obj) {
		if (pos < 0) return;
		if (obj == null) {
			this.remove(pos);
		}
		else {
			int posx = getPos();
			remove(pos);
			insert(obj, pos);
			if (posx == pos) {
				setPos(pos);
			}
		}
	}

	/**
	 * Sets whether the active object should become null when the current active
	 * object is removed from this Hub.
	 *
	 * @param b true to clear the active object on removal
	 */
	public void setNullOnRemove(boolean b) {
		datau.setNullOnRemove(b);
	}

	/**
	 * Sets whether the active object should become null when the current active
	 * object is removed from this Hub.
	 *
	 * @param b true to clear the active object on removal
	 */
	public boolean getNullOnRemove() {
		return datau.isNullOnRemove();
	}

	/**
	 * Removes all objects from this Hub. Sends a hubRemoveAll event followed by
	 * a hubNewList event. Each individual removal uses remove() without sending
	 * hubRemove events.
	 */
	public void clear() {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		og.hubsInternal().callHubAddRemoveClear(this);
	}

	/**
	 * Removes all objects from this Hub. Same behavior as clear(). Sends a
	 * hubRemoveAll event and then a hubNewList event.
	 */
	public void removeAll() {
		this.clear();
	}

	/**
	 * Creates and returns a shared Hub that uses the same data as this Hub but
	 * maintains its own active object.
	 *
	 * @return newly created shared Hub
	 */
	public Hub<TYPE> createSharedHub() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubShareCreateSharedHub(this, false);
	}

	/**
	 * Creates and returns a shared Hub associated with this Hub. The shared Hub
	 * will optionally share the same active object.
	 *
	 * @param bShareAO true to share active object, false for separate AO
	 * @return newly created shared Hub
	 */
	public Hub<TYPE> createSharedHub(boolean bShareAO) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubShareCreateSharedHub(this, bShareAO);
	}

	/**
	 * Configures this Hub to share data from the specified master Hub. The
	 * active object may be shared or independent.
	 *
	 * @param masterHub Hub to share data from
	 * @param bShareAO  true to share active object
	 */
	public void setSharedHub(Hub<TYPE> masterHub, boolean bShareAO) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubShareSetSharedHub(this, masterHub, bShareAO);
	}

	/**
	 * Configures this Hub to share data from the supplied master Hub using a
	 * separate active object.
	 *
	 * @param masterHub Hub to share data from
	 */
	public void setSharedHub(Hub<TYPE> masterHub) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubShareSetSharedHub(this, masterHub, false);
	}

	/**
	 * Returns the Hub that this Hub is sharing data from. If this Hub is not a
	 * shared Hub, returns null.
	 *
	 * @return source shared Hub or null
	 */
	public Hub<TYPE> getSharedHub() {
		return datau.getSharedHub();
	}

	/**
	 * Returns a detail Hub created using the supplied property path. The detail
	 * Hub is populated from the active object's property and may optionally
	 * share the active object. A select order may also be supplied.
	 *
	 * @param path         property path to the detail collection
	 * @param bShareActive true to share active object
	 * @param selectOrder  select ordering for loading
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path, boolean bShareActive, String selectOrder) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, path, bShareActive, selectOrder);
	}

	/**
	 * Returns a detail Hub based on a property path, with optional active-object
	 * sharing.
	 *
	 * @param path         property path
	 * @param bShareActive true to share active object
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path, boolean bShareActive) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, path, bShareActive);
	}

	/**
	 * Returns a detail Hub based on a property path, using the supplied select
	 * order for data loading.
	 *
	 * @param path        property path
	 * @param selectOrder select ordering
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path, String selectOrder) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, path, selectOrder);
	}

	/**
	 * Returns a detail Hub based on a property path. Uses default active-object
	 * behavior and select ordering.
	 *
	 * @param path property path
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		Hub h = og.hubsInternal().callHubDetailGetDetailHub(this, path);
		return h;
	}

	/**
	 * Returns a detail Hub using the supplied property path and expected object
	 * class, with optional active-object sharing.
	 *
	 * @param path         property path
	 * @param objectClass  expected class for detail objects
	 * @param bShareActive true to share active object
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path, Class objectClass, boolean bShareActive) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, path, objectClass, bShareActive);
	}

	/**
	 * Returns a detail Hub for the supplied property path and object class,
	 * using default active-object behavior.
	 *
	 * @param path        property path
	 * @param objectClass expected object class
	 * @return detail Hub
	 */
	public Hub getDetailHub(String path, Class objectClass) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, path, objectClass, false);
	}

	/**
	 * Returns a detail Hub based on the supplied class, select ordering, and
	 * active-object sharing configuration.
	 *
	 * @param clazz        class of detail objects
	 * @param bShareActive whether to share active object
	 * @param selectOrder  order-by clause for selects
	 * @return detail Hub
	 */
	public Hub getDetailHub(Class clazz, boolean bShareActive, String selectOrder) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, clazz, bShareActive, selectOrder);
	}

	/**
	 * Returns a detail Hub for the supplied class with optional active-object
	 * sharing. Uses default select ordering.
	 *
	 * @param clazz        detail object class
	 * @param bShareActive true to share active object
	 * @return detail Hub
	 */
	public Hub getDetailHub(Class clazz, boolean bShareActive) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, clazz, bShareActive, null);
	}

	/**
	 * Returns a detail Hub for the supplied class using the provided select
	 * order.
	 *
	 * @param clazz        detail object class
	 * @param selectOrder  order-by clause
	 * @return detail Hub
	 */
	public Hub getDetailHub(Class clazz, String selectOrder) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, clazz, false, selectOrder);
	}

	/**
	 * Returns a detail Hub for the supplied class using default options.
	 *
	 * @param clazz detail object class
	 * @return detail Hub
	 */
	public Hub getDetailHub(Class clazz) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, clazz, false, null);
	}

	/**
	 * Returns a detail Hub by searching through the supplied array of classes
	 * for a matching detail relationship.
	 *
	 * @param classes array of potential detail classes
	 * @return detail Hub or null
	 */
	public Hub getDetailHub(Class[] classes) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetDetailHub(this, classes);
	}

	/**
	 * Sets the master Hub that controls this Hub in a master/detail
	 * relationship. Uses default path, shared, and select-order behavior.
	 *
	 * @param masterHub master Hub to associate with
	 */
	public void setMasterHub(Hub masterHub) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDetailSetMasterHub(this, masterHub, null, false, null);
	}

	/**
	 * Sets the master Hub for this Hub, specifying whether the active object
	 * should be shared between the master and detail Hub.
	 *
	 * @param masterHub master Hub to associate with
	 * @param bShared   true to share the active object
	 */
	public void setMasterHub(Hub masterHub, boolean bShared) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDetailSetMasterHub(this, masterHub, null, bShared, null);
	}

	/**
	 * Sets the master Hub and the property path needed to navigate from the
	 * master to this Hub.
	 *
	 * @param masterHub master Hub
	 * @param path      property path from master to this Hub
	 */
	public void setMasterHub(Hub masterHub, String path) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDetailSetMasterHub(this, masterHub, path, false, null);
	}

	/**
	 * Sets the master Hub along with its property path, with control over
	 * whether the active object is shared.
	 *
	 * @param masterHub master Hub to associate
	 * @param path      property path from master to this Hub
	 * @param bShared   whether to share active object
	 */
	public void setMasterHub(Hub masterHub, String path, boolean bShared) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDetailSetMasterHub(this, masterHub, path, bShared, null);
	}

	/**
	 * Sets the master Hub, expected class, property path, active-object sharing
	 * flag, and select-order behavior for this Hub.
	 *
	 * @param masterHub   controlling master Hub
	 * @param clazz       expected object class
	 * @param path        property path from master to this Hub
	 * @param bShared     whether to share active object
	 * @param selectOrder order-by clause for select
	 */
	public void setMasterHub(Hub masterHub, Class clazz, String path, boolean bShared, String selectOrder) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubDetailSetMasterHub(this, masterHub, path, bShared, selectOrder);
	}

	/**
	 * Returns the master Hub controlling this Hub, or null if not part of a
	 * master/detail relationship.
	 *
	 * @return master Hub or null
	 */
	public Hub getMasterHub() {
		
//qqqqqqqqqqqqq Important note: this is different then just using datam.masterHub, it will check shared hubs.
		//  use datam.masterHub to get thisHub's value
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		
		return og.hubsInternal().callHubDetailGetMasterHub(this);
	}

	/**
	 * Returns the master object associated with this Hub, or null if none.
	 *
	 * @return master object
	 */
	public OAObject getMasterObject() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);

//qqqqqqqqqqqqq Important note: this is different then just using datam.masterHub, it will check shared hubs.
  	//  use datam.masterHub to get thisHub's value
				
		
		return og.hubsInternal().callHubDetailGetMasterObject(this);
	}

	/**
	 * Returns the Class of the master object for this Hub.
	 *
	 * @return master object class
	 */
	public Class getMasterClass() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailGetMasterClass(this);
	}

	/**
	 * Returns whether this Hub has any associated detail Hubs.
	 *
	 * @return true if at least one detail Hub exists
	 */
	public boolean hasDetailHubs() {
		int x = (datau.getVecHubDetail() == null) ? 0 : datau.getVecHubDetail().size();
		return x > 0;
	}

	/**
	 * Removes the supplied Hub from this Hub’s list of detail Hubs.
	 *
	 * @param hub detail Hub to remove
	 * @return true if the Hub was removed
	 */
	public boolean removeDetailHub(Hub hub) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubDetailRemoveDetailHub(this, hub);
	}

	/**
	 * Adds a HubListener that listens for changes to a specific property. If the
	 * property is calculated, internal listeners are automatically set up.
	 *
	 * @param hl       listener to add
	 * @param property property to listen for
	 */
	public void addHubListener(HubListener<TYPE> hl, String property) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, property);
	}

	/**
	 * Adds a HubListener for a specific property, restricting notifications to
	 * events involving the active object.
	 *
	 * @param hl                 listener to add
	 * @param property           property to observe
	 * @param bActiveObjectOnly  true to receive events only for active object
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, boolean bActiveObjectOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, bActiveObjectOnly);
	}

	/**
	 * Adds a HubListener that listens for all property changes, optionally
	 * restricted to the active object.
	 *
	 * @param hl                listener to add
	 * @param bActiveObjectOnly true to restrict events to active object
	 */
	public void addHubListener(HubListener<TYPE> hl, boolean bActiveObjectOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, bActiveObjectOnly);
	}

	/**
	 * Adds a HubListener for a property and its dependent property paths. Changes
	 * to any dependent property will trigger events for the supplied property.
	 *
	 * @param hl                     listener to add
	 * @param property               property name for events
	 * @param dependentPropertyPaths array of dependent properties
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, dependentPropertyPaths);
	}

	/**
	 * Adds a HubListener for a property and a single dependent property path.
	 *
	 * @param hl                   listener to add
	 * @param property             property for events
	 * @param dependentPropertyPath dependent property that triggers updates
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, String dependentPropertyPath) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		String[] ss;
		if (dependentPropertyPath != null && dependentPropertyPath.length() > 0) {
			ss = new String[] { dependentPropertyPath };
		} else {
			ss = null;
		}
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, ss);
	}

	/**
	 * Adds a HubListener for a property and one dependent property, with an
	 * option to restrict events to the active object.
	 *
	 * @param hl                   listener to add
	 * @param property             primary property for events
	 * @param dependentPropertyPath dependent property to observe
	 * @param bActiveObjectOnly     true to restrict events to active object
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, String dependentPropertyPath, boolean bActiveObjectOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		String[] ss;
		if (dependentPropertyPath != null && dependentPropertyPath.length() > 0) {
			ss = new String[] { dependentPropertyPath };
		} else {
			ss = null;
		}
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, ss, bActiveObjectOnly);
	}

	/**
	 * Adds a HubListener that listens for property changes in either the primary
	 * property or any listed dependent properties. Optional restriction to
	 * active-object events.
	 *
	 * @param hl                     listener to add
	 * @param property               property name
	 * @param dependentPropertyPaths dependent property paths
	 * @param bActiveObjectOnly      restrict to active object if true
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, dependentPropertyPaths, bActiveObjectOnly);
	}

	/**
	 * Adds a HubListener with dependent properties, optional active-object
	 * restriction, and support for running events on a background thread.
	 *
	 * @param hl                     listener to add
	 * @param property               property to observe
	 * @param dependentPropertyPaths dependent paths
	 * @param bActiveObjectOnly      true to restrict events to active object
	 * @param bUseBackgroundThread   true to run listener in background thread
	 */
	public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly,
			boolean bUseBackgroundThread) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
	}

	/**
	 * Adds a trigger listener that sends a calculated-property change event
	 * whenever the supplied property path triggers.
	 *
	 * @param hl          listener to add
	 * @param property    property name for event dispatch
	 * @param propertyPath property path whose changes will trigger updates
	 */
	public void addTriggerListener(final HubListener<TYPE> hl, final String property, String propertyPath) {
		OATriggerListener<TYPE> tl = new OATriggerListener<TYPE>() {
			@Override
			public void onTrigger(TYPE obj, HubEvent hubEvent, String propertyPath) throws Exception {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Hub.this);
				og.hubsInternal().callHubEventFireCalcPropertyChange(Hub.this, obj, property);
				if (hl != null) hl.afterPropertyChange(hubEvent);
			}
		};
		OATrigger trigger = new OATrigger(property, getObjectClass(), tl, new String[] { propertyPath }, true, false, false, true);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(getObjectClass());
        og.triggerInternal().addTrigger(trigger);
	}

	/**
	 * Adds a trigger listener for calculated-property change events, with an
	 * option to run listener logic on a background thread.
	 *
	 * @param hl                   listener to add
	 * @param property             property name for event dispatch
	 * @param propertyPath         property path that triggers update
	 * @param useBackgroundThread  true to run listener in background thread
	 */
	public void addTriggerListener(HubListener<TYPE> hl, final String property, String propertyPath, boolean useBackgroundThread) {
		OATriggerListener<TYPE> tl = new OATriggerListener<TYPE>() {
			@Override
			public void onTrigger(TYPE obj, HubEvent hubEvent, String propertyPath) throws Exception {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Hub.this);
				og.hubsInternal().callHubEventFireCalcPropertyChange(Hub.this, obj, property);
				if (hl != null) hl.afterPropertyChange(hubEvent);
			}
		};
		OATrigger trigger = new OATrigger(property, getObjectClass(), tl, new String[] { propertyPath }, true, false, useBackgroundThread, true);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(getObjectClass());
        og.triggerInternal().addTrigger(trigger);
	}

	/**
	 * Adds a HubListener that receives all Hub events for this Hub.
	 *
	 * @param hl listener to add
	 */
	public void addHubListener(HubListener<TYPE> hl) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl);
	}

	/**
	 * Convenience wrapper for addHubListener(HubListener).
	 *
	 * @param hl listener to add
	 */
	public void addListener(HubListener<TYPE> hl) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventAddHubListener(this, hl);
	}

	/**
	 * Removes the supplied HubListener from this Hub.
	 *
	 * @param hl listener to remove
	 */
	public void removeHubListener(HubListener<TYPE> hl) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventRemoveHubListener(this, hl);
	}

	/**
	 * Convenience wrapper for removeHubListener(HubListener).
	 *
	 * @param hl listener to remove
	 */
	public void removeListener(HubListener<TYPE> hl) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubEventRemoveHubListener(this, hl);
	}

	/**
	 * Enables auto-sequencing for the specified property, assigning each object
	 * a numeric value equal to its position in this Hub. Sequencing begins at 0.
	 *
	 * @param property property to update
	 */
	public void setAutoSequence(String property) {
		this.setAutoSequence(property, 0);
	}

	/**
	 * Enables auto-sequencing for the specified property, beginning with the
	 * supplied starting number.
	 *
	 * @param property    property to update
	 * @param startNumber initial sequence value
	 */
	public void setAutoSequence(String property, int startNumber) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSequenceSetAutoSequence(this, property, startNumber, true);
	}

	/**
	 * Recalculates and updates sequence values for all objects in this Hub
	 * according to current auto-sequence settings.
	 */
	public void resequence() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSequenceResequence(this);
	}

	/**
	 * Enables auto-sequencing for a property with optional behavior for whether
	 * sequence values should be preserved when objects are removed.
	 *
	 * @param property    property to update
	 * @param startNumber starting sequence value
	 * @param bKeepSeq    true to preserve sequence values after removal
	 */
	public void setAutoSequence(String property, int startNumber, boolean bKeepSeq) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSequenceSetAutoSequence(this, property, startNumber, bKeepSeq);
	}

	/**
	 * Ensures that for each object in the master Hub, there exists a
	 * corresponding object in this Hub whose property matches the master object.
	 *
	 * @param property   mapped property
	 * @param hubMaster  master Hub to match against
	 */
	public void setAutoMatch(String property, Hub hubMaster) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAutoMatchSetAutoMatch(this, property, hubMaster, false);
	}

	/**
	 * Sets automatic matching between this Hub and a master Hub, optionally
	 * restricting operations to server-side execution.
	 *
	 * @param property        mapped property
	 * @param hubMaster       master Hub
	 * @param bServerSideOnly true to restrict to server-side only
	 */
	public void setAutoMatch(String property, Hub hubMaster, boolean bServerSideOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAutoMatchSetAutoMatch(this, property, hubMaster, bServerSideOnly);
	}

	/**
	 * Sets automatic matching behavior with additional constraints, stopping
	 * processing when the supplied object and property value match.
	 *
	 * @param property        mapped property
	 * @param hubMaster       master Hub
	 * @param bServerSideOnly true for server-side mode only
	 * @param objStop         stopping object
	 * @param stopProperty    property used to detect stop condition
	 */
	public void setAutoMatch(String property, Hub<TYPE> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAutoMatchSetAutoMatch(this, property, hubMaster, bServerSideOnly, objStop, stopProperty);
	}
	
	/**
	 * Ensures that for each object in the master Hub, a corresponding object
	 * exists in this Hub. No specific property is required.
	 *
	 * @param hubMaster master Hub
	 */
	public void setAutoMatch(Hub hubMaster) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAutoMatchSetAutoMatch(this, null, hubMaster, false);
	}

	/**
	 * Performs automatic matching between this Hub and a master Hub, optionally
	 * restricted to server-side operation.
	 *
	 * @param hubMaster       master Hub
	 * @param bServerSideonly true to restrict to server-side only
	 */
	public void setAutoMatch(Hub hubMaster, boolean bServerSideonly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubAutoMatchSetAutoMatch(this, null, hubMaster, bServerSideonly);
	}

	/**
	 * Sorts the objects in this Hub using the supplied Comparator. Sorting
	 * persists for newly added objects.
	 *
	 * @param comp comparator used for sorting
	 */
	public void sort(Comparator comp) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortSort(this, null, true, comp);
	}

	/**
	 * Sorts this Hub using one or more property paths. Each path may contain
	 * comma-separated fields for multi-field sorting.
	 *
	 * @param propertyPaths list of property paths
	 */
	public void sort(String... propertyPaths) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		String s = "";
		for (int i = 0; propertyPaths != null && i < propertyPaths.length; i++) {
			if (propertyPaths[i] == null) {
				continue;
			}
			if (s.length() > 0) {
				s += ", ";
			}
			s += propertyPaths[i];
		}
		og.hubsInternal().callHubSortSort(this, s, true, null);
	}

	/**
	 * Sorts the Hub according to the supplied property path string, using the
	 * specified ascending/descending order.
	 *
	 * @param propertyPaths property path(s)
	 * @param bAscending    true for ascending, false for descending
	 */
	public void sort(String propertyPaths, boolean bAscending) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortSort(this, propertyPaths, bAscending, null);
	}

	/**
	 * Sorts this Hub using the supplied property paths, ordering direction, and
	 * optional custom comparator.
	 *
	 * @param propertyPaths property path(s)
	 * @param bAscending    sort direction
	 * @param comp          optional comparator
	 */
	public void sort(String propertyPaths, boolean bAscending, Comparator comp) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortSort(this, propertyPaths, bAscending, comp);
	}

	/**
	 * Returns whether this Hub currently has an active sorter maintaining its
	 * ordering.
	 *
	 * @return true if sorted
	 */
	public boolean isSorted() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSortIsSorted(this);
	}

	/**
	 * Removes any active HubSorter, stopping automatic sorting of objects in
	 * this Hub.
	 */
	public void cancelSort() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortCancelSort(this);
	}

	/**
	 * Re-sorts this Hub using parameters from the most recent sorting
	 * operation or select call.
	 */
	public void sort() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortSort(this);
	}

	/**
	 * Re-sorts this Hub using the parameters from the most recent sort or
	 * select operation. Same as sort().
	 */
	public void resort() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSortSort(this);
	}

	/**
	 * Finds and returns the first object whose property value matches the
	 * supplied value using OACompare.like(). Does not change the active object.
	 *
	 * @param propertyPath property to evaluate
	 * @param findValue    value to match
	 * @return first matching object or null
	 */
	public TYPE find(String propertyPath, Object findValue) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, false, null);
	}

	/**
	 * Finds the first object matching the supplied property/value pair and
	 * optionally sets it as the active object.
	 *
	 * @param propertyPath property to evaluate
	 * @param findValue    match value
	 * @param bSetAO       true to set active object
	 * @return matching object or null
	 */
	public TYPE find(String propertyPath, Object findValue, boolean bSetAO) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, bSetAO, null);
	}

	/**
	 * Finds the first object matching the supplied property/value pair,
	 * beginning the search at the specified starting object. Does not change
	 * the active object.
	 *
	 * @param fromObject   starting object
	 * @param propertyPath property to evaluate
	 * @param findValue    match value
	 * @return matching object or null
	 */
	public TYPE find(TYPE fromObject, String propertyPath, Object findValue) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, false, fromObject);
	}

	/**
	 * Finds the first matching object beginning at the specified starting
	 * object and optionally sets it as the active object.
	 *
	 * @param fromObject starting object
	 * @param propertyPath property to evaluate
	 * @param findValue     match value
	 * @param bSetAO        true to update active object
	 * @return matching object or null
	 */
	public TYPE find(TYPE fromObject, String propertyPath, Object findValue, boolean bSetAO) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, bSetAO, fromObject);
	}

	/**
	 * Finds the next object after the supplied starting object that matches the
	 * property/value pair. Does not change the active object.
	 *
	 * @param fromObject   starting object
	 * @param propertyPath property to evaluate
	 * @param findValue    value to match
	 * @return next matching object or null
	 */
	public TYPE findNext(TYPE fromObject, String propertyPath, Object findValue) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, false, fromObject);
	}

	/**
	 * Finds the next matching object after the supplied starting object and
	 * optionally sets it as the active object.
	 *
	 * @param fromObject   starting object
	 * @param propertyPath property to evaluate
	 * @param findValue    value to match
	 * @param bSetAO       true to set active object
	 * @return next matching object or null
	 */
	public TYPE findNext(TYPE fromObject, String propertyPath, Object findValue, boolean bSetAO) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return (TYPE) og.hubsInternal().callHubFindFindFirst(this, propertyPath, findValue, bSetAO, fromObject);
	}

	/**
	 * Sets the WHERE clause used for select operations on this Hub.
	 *
	 * @param s WHERE clause
	 */
	public void setSelectWhere(String s) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSetSelectWhere(this, s);
	}

	/**
	 * Returns the WHERE clause used for select operations.
	 *
	 * @return WHERE clause or null
	 */
	public String getSelectWhere() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSelectGetSelectWhere(this);
	}

	/**
	 * Sets the ORDER BY clause used for select operations on this Hub.
	 *
	 * @param s ORDER BY clause
	 */
	public void setSelectOrder(String s) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSetSelectOrder(this, s);
	}

	/**
	 * Sets a where-Hub constraint so that select operations are filtered based
	 * on objects from the supplied Hub, and applies a property path used for
	 * evaluating the relationship.
	 *
	 * @param fromHub     source Hub for filtering
	 * @param ppFromHub   property path from this Hub's objects to fromHub
	 */
	public void setSelectWhereHub(Hub fromHub, String ppFromHub) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSetSelectWhereHub(this, fromHub);
		og.hubsInternal().callHubSelectSetSelectWhereHubPropertyPath(this, ppFromHub);
	}

	/**
	 * Returns the ORDER BY clause used for select operations.
	 *
	 * @param thisHub unused parameter
	 * @return ORDER BY clause
	 */
	public String getSelectOrder(Hub thisHub) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSelectGetSelectOrder(this);
	}

	/**
	 * Executes a select operation to load objects from the data source that are
	 * associated with the supplied whereObject. Applies an optional ORDER BY
	 * clause.
	 *
	 * @param whereObject object used to filter results
	 * @param orderByClause sort ordering
	 */
	public void select(OAObject whereObject, String orderByClause) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, whereObject, null, null, orderByClause, false);
	}

	/**
	 * Selects all objects for this Hub from the data source. If a master object
	 * exists, selects objects related to it.
	 */
	public void select() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, false);
	}

	/**
	 * Selects objects using the supplied WHERE clause. If no master object is
	 * defined, selects all objects matching the clause.
	 *
	 * @param whereClause WHERE clause used for selection
	 */
	public void select(String whereClause) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, null, null, false);
	}

	/**
	 * Selects objects using the supplied WHERE clause and parameter list.
	 *
	 * @param whereClause WHERE clause
	 * @param params      parameter values
	 */
	public void select(String whereClause, Object[] params) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, params, null, false);
	}

	/**
	 * Selects objects using the supplied WHERE and ORDER BY clauses.
	 *
	 * @param whereClause WHERE clause
	 * @param orderBy     ORDER BY clause
	 */
	public void select(String whereClause, String orderBy) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, null, orderBy, false);
	}

	/**
	 * Selects objects using WHERE clause, ORDER BY clause, and a filter applied
	 * after retrieval.
	 *
	 * @param whereClause WHERE clause
	 * @param orderBy     ORDER BY clause
	 * @param filter      additional object filter
	 */
	public void select(String whereClause, String orderBy, OAFilter filter) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, null, orderBy, false, filter);
	}

	/**
	 * Selects objects using WHERE clause, parameter list, ORDER BY clause, and
	 * an optional filter.
	 *
	 * @param whereClause WHERE clause
	 * @param whereParams parameter values
	 * @param orderBy     ORDER BY clause
	 * @param filter      additional filter
	 */
	public void select(String whereClause, Object[] whereParams, String orderBy, OAFilter filter) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, whereParams, orderBy, false, filter);
	}

	/**
	 * Selects objects using WHERE clause, parameters, and ORDER BY clause.
	 *
	 * @param whereClause WHERE clause
	 * @param whereParams parameters
	 * @param orderBy     ORDER BY clause
	 */
	public void select(String whereClause, Object[] whereParams, String orderBy) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, whereParams, orderBy, false);
	}

	/**
	 * Selects objects using a WHERE clause and a single parameter value,
	 * applying the supplied ORDER BY clause.
	 *
	 * @param whereClause WHERE clause
	 * @param whereParam  single parameter value
	 * @param orderBy     ORDER BY clause
	 */
	public void select(String whereClause, Object whereParam, String orderBy) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		Object[] params = null;
		if (whereParam != null) {
			params = new Object[] { whereParam };
		}
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, params, orderBy, false);
	}

	/**
	 * Selects objects using a WHERE clause, single parameter, ORDER BY clause,
	 * and an additional filter applied after selection.
	 *
	 * @param whereClause WHERE clause
	 * @param whereParam  single parameter
	 * @param orderBy     ORDER BY clause
	 * @param filter      additional filter
	 */
	public void select(String whereClause, Object whereParam, String orderBy, OAFilter filter) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		Object[] params = null;
		if (whereParam != null) {
			params = new Object[] { whereParam };
		}
		og.hubsInternal().callHubSelectSelect(this, null, whereClause, params, orderBy, false, filter);
	}

	/**
	 * Populates this Hub with objects returned by the supplied OASelect instance.
	 *
	 * @param select OASelect object used for loading objects
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public void select(OASelect<? extends OAObject> select) { // This is the main select method for
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		
		Hub<OAObject> hubX = (Hub) this;
	    OASelect selX = (OASelect) select;
		
		og.hubsInternal().callHubSelectSelect(hubX, selX);
	}

	/**
	 * Executes a passthru select that sends the supplied native query fragments
	 * directly to the underlying data source.
	 *
	 * @param whereClause native WHERE clause beginning with "FROM tableName"
	 * @param orderClause native ORDER BY clause
	 */
	public void selectPassthru(String whereClause, String orderClause) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectSelectPassthru(this, whereClause, orderClause);
	}

	/**
	 * Returns the OASelect object currently associated with this Hub,
	 * or null if none exists.
	 *
	 * @return OASelect instance or null
	 */
	public OASelect<TYPE> getSelect() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubSelectGetSelect(this);
	}

	/**
	 * Returns the OASelect associated with this Hub, optionally creating
	 * a new instance if none exists.
	 *
	 * @param bCreateIfNull true to create if not present
	 * @return OASelect instance
	 */
	public OASelect<? extends OAObject> getSelect(boolean bCreateIfNull) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		OASelect sel = og.hubsInternal().callHubSelectGetSelect((Hub<? extends OAObject>) this, true);
		return sel;
	}

	/**
	 * Cancels further reading of records for the current OASelect. Depending on
	 * master/detail configuration, the select may or may not be removed from
	 * this Hub.
	 */
	public void cancelSelect() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		boolean bRemoveSelectFromHub;
		if (getMasterObject() != null) {
			OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromDetailToMaster(this);
			if (li != null && li.getType() == OALinkInfo.ONE && li.getPrivateMethod()) {
				bRemoveSelectFromHub = false;
			} else {
				bRemoveSelectFromHub = true;
			}
		} else {
			bRemoveSelectFromHub = false; // dont remove, so that it can be refreshed
		}
		og.hubsInternal().callHubSelectCancelSelect(this, bRemoveSelectFromHub);
	}

	/**
	 * Returns the link Hub associated with this Hub, optionally searching any
	 * shared or related Hubs for link settings.
	 *
	 * @param bSearchOtherHubs true to search shared/copy/filter Hubs
	 * @return linked Hub or null
	 */
	public Hub<? extends OAObject> getLinkHub(boolean bSearchOtherHubs) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		if (!bSearchOtherHubs) {
			return this.datau.getLinkToHub();
		}
		Hub hx = og.hubsInternal().callHubLinkGetHubWithLink(this, true);
		if (hx == null) {
			return null;
		}
		return hx.datau.getLinkToHub();
	}

	/**
	 * Links this Hub so that the position of its active object is stored in a
	 * numeric property of the supplied link Hub.
	 *
	 * @param linkHub  destination Hub
	 * @param property property to update with position
	 */
	public void setLinkHubOnPos(Hub linkHub, String property) {
		setLinkHub(null, linkHub, property, true);
	}

	/**
	 * Links this Hub to another Hub so that changes in active object update
	 * the specified property in the link Hub.
	 *
	 * @param linkHub  Hub to link to
	 * @param property property in link Hub to update
	 */
	public void setLinkHub(Hub linkHub, String property) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, property, false, false, false);
	}

	/**
	 * Links this Hub to another Hub using default property resolution rules
	 * (OAObjectInfo & OALinkInfo).
	 *
	 * @param linkHub Hub to link to
	 */
	public void setLinkHub(Hub linkHub) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, null, false, false, false);
	}

	/**
	 * Links a property of this Hub’s active object to a property in another
	 * Hub’s active object.
	 *
	 * @param fromProperty property from this Hub's AO
	 * @param linkHub      Hub to link to
	 * @param toProperty   property in link Hub to update
	 */
	public void setLinkHub(String fromProperty, Hub linkHub, String toProperty) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, fromProperty, linkHub, toProperty, false, false, false);
	}

	/**
	 * Removes any existing link relationship between this Hub and another Hub.
	 */
	public void removeLinkHub() {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, null, null, false, false, false);
	}

	/**
	 * Automatically creates a new object in the link Hub whenever this Hub’s
	 * active object changes, with optional duplicate creation control.
	 *
	 * @param linkHub                 Hub to populate
	 * @param bAutoCreate             true to create new linked objects
	 * @param bAutoCreateAllowDups    true to allow duplicates
	 */
	public void setLinkHub(Hub linkHub, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, null, false, bAutoCreate, bAutoCreateAllowDups);
	}

	/**
	 * Automatically creates a new object in the link Hub whenever this Hub’s
	 * active object changes. Duplicate creation is not allowed.
	 *
	 * @param linkHub     Hub to populate
	 * @param bAutoCreate true to enable auto-create mode
	 */
	public void setLinkHub(Hub linkHub, boolean bAutoCreate) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, null, false, bAutoCreate, false);
	}

	public void setLinkHub(Hub linkHub, String property, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, property, false, bAutoCreate, bAutoCreateAllowDups);
	}

	public void setLinkHub(Hub linkHub, String property, boolean bAutoCreate) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, null, linkHub, property, false, bAutoCreate, false);
	}

	
	/**
	 * Internal method that links this Hub to another Hub using the supplied
	 * property names and link position flag. All parameters are passed directly
	 * to {@link HubLinkDelegate#setLinkHub}.
	 *
	 * @param propertyFrom property name in this Hub’s active object
	 * @param linkHub      Hub to link to
	 * @param propertyTo   property name in the link Hub to update
	 * @param linkPosFlag  true to link position instead of value
	 */
	protected void setLinkHub(String propertyFrom, Hub linkHub, String propertyTo, boolean linkPosFlag) {
		// setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
		// propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubLinkSetLinkHub(this, propertyFrom, linkHub, propertyTo, linkPosFlag, false, false);
	}

	/**
	 * Returns whether this Hub is valid by delegating to
	 * {@link HubDelegate#isValid(Object)}.
	 *
	 * @return true if valid
	 */
	public boolean isValid() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubStatusIsValid(this);
	}

	/**
	 * Returns the link path for this Hub by delegating to
	 * {@link HubLinkDelegate#getLinkHubPath(Hub, boolean)}.
	 *
	 * @param bSearchOtherHubs true to search other related Hubs
	 * @return link path
	 */
	public String getLinkPath(boolean bSearchOtherHubs) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.hubsInternal().callHubLinkGetLinkHubPath(this, bSearchOtherHubs);
	}

	/**
	 * Returns OAObjectInfo for the supplied class by delegating to
	 * {@link OAObjectInfoDelegate#callInfoGetObjectInfo(Class)}.
	 *
	 * @param c class to lookup
	 * @return OAObjectInfo for the class
	 */
	public static OAObjectInfo getOAObjectInfo(Class c) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		return og.objectsInternal().callObjectInfoGetOAObjectInfo(c);
	}

	/**
	 * Returns OAObjectInfo for this Hub’s object class using
	 * {@link OAObjectInfoDelegate#callInfoGetObjectInfo(Class)}.
	 *
	 * @return OAObjectInfo for this Hub’s object class
	 */
	public OAObjectInfo getOAObjectInfo() {
		return data.getObjectInfo();
		//was: return OAObjectInfoDelegate.getOAObjectInfo(getObjectClass());
	}

	/**
	 * Convenience wrapper for {@link #setLinkHub(Hub)}.
	 *
	 * @param hub Hub to link to
	 */
	public void setLink(Hub hub) {
		this.setLinkHub(hub);
	}

	/**
	 * Convenience wrapper for {@link #createSharedHub()}.
	 *
	 * @return newly created shared Hub
	 */
	public Hub<TYPE> createShared() {
		return this.createSharedHub();
	}

	/**
	 * Updates a linked Hub property using the Hub returned by
	 * {@link HubLinkDelegate#getHubWithLink(Hub, boolean)} and passing it to
	 * {@link HubLinkDelegate#updateLinkedToHub(Hub, Hub, Object)}.
	 *
	 * @param obj   unused parameter
	 * @param value value to apply to the linked property
	 */
/*qqqqq 20260220 not sure this is being used, LinkToHub has wrong arg values	
	public void updateLinkProperty(Object obj, Object value) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		Hub<TYPE> h = og.hubsInternal().callHubLinkGetHubWithLink(this, true);
		if (h == null) {
			return;
		}
		og.hubsInternal().callHubLinkUpdateLinkedToHub(h, h.getLinkHub(false), value);
	}
*/	

	/**
	 * Returns whether this Hub’s object class is considered server-side by
	 * delegating to {@link OASyncDelegate#isServer(Class)}.
	 *
	 * @return true if server-side
	 */
/*qqqqqqqqqq remove	
	public boolean isServer() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.syncInternal().isServer();
	}
	public boolean isclient() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		return og.syncInternal().isClient();
	}
*/
	/**
	 * Returns whether an object can be added to this Hub by delegating to
	 * {@link HubAddRemoveDelegate#canAdd(Hub, Object)}.
	 *
	 * @return true if addition is allowed
	 */
	public boolean canAdd() {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveCanAdd(this, null);
	}

	/**
	 * Returns whether the supplied object can be added to this Hub using
	 * {@link HubAddRemoveDelegate#canAdd(Hub, Object)}.
	 *
	 * @param obj object to test
	 * @return true if the object can be added
	 */
	public boolean canAdd(TYPE obj) {
		if (obj == null) return false;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveCanAdd(this, obj);
	}

	/**
	 * Returns the message describing why the supplied object cannot be added to
	 * this Hub by delegating to {@link HubAddRemoveDelegate#getCanAddMessage(Hub, OAObject)}.
	 *
	 * @param obj object to evaluate
	 * @return message describing add restriction, or null
	 */
	public String getCanAddMessage(TYPE obj) {
		if (obj == null) return null;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveCanAddMsg(this, obj);
	}

	/**
	 * Returns whether adding is allowed by delegating to
	 * og.hubsInternal().callHubAddRemoveCanAdd(this, null).
	 *
	 * @return true if adding is allowed
	 */
	public boolean getAllowAdd() {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveCanAdd(this, null);
	}

	/**
	 * Returns whether the specified object can be added by delegating to
	 * og.hubsInternal().callHubAddRemoveCanAdd(this, obj).
	 *
	 * @param obj the object to evaluate
	 * @return true if the object can be added
	 */
	public boolean getAllowAdd(TYPE obj) {
		if (obj == null) return false;
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this.getObjectClass());
		return og.hubsInternal().callHubAddRemoveCanAdd(this, obj);
	}

	/**
	 * Uses OAObjectCallbackDelegate to determine whether the supplied object
	 * is allowed to be added for the given check type.
	 *
	 * @param checkType callback check type
	 * @param obj       object to evaluate
	 * @return true if adding is permitted
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public boolean getAllowAdd(int checkType, TYPE obj) {
		if (!(obj instanceof OAObject)) {
			return true;
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		
		Hub<OAObject> hub = (Hub) this;
	    OAObject oaObj = (OAObject) obj;
	    
		return og.objectsInternal().callObjectCallbackGetAllowAdd(hub, oaObj, checkType);
	}

	/**
	 * Uses OAObjectCallbackDelegate to determine whether the supplied object
	 * is allowed to be removed for the given check type.
	 *
	 * @param checkType callback check type
	 * @param obj       object to evaluate
	 * @return true if removing is permitted
	 */
	public boolean getAllowRemove(int checkType, TYPE obj) {
		if (!(obj instanceof OAObject)) {
			return true;
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		
		Hub<OAObject> hub = (Hub) this;
	    OAObject oaObj = (OAObject) obj;
		
		return og.objectsInternal().callObjectCallbackGetAllowRemove(hub, oaObj, checkType);
	}

	/**
	 * Uses OAObjectCallbackDelegate to verify whether removal of the supplied
	 * object is allowed for the given check type.
	 *
	 * @param checkType callback check type
	 * @param obj       object to evaluate
	 * @return true if removal is verified as allowed
	 */
	public boolean getVerifyRemove(int checkType, TYPE obj) {
		if (!(obj instanceof OAObject)) {
			return true;
		}
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		Hub<OAObject> hub = (Hub) this;
	    OAObject oaObj = (OAObject) obj;
		return og.objectsInternal().callObjectCallbackGetVerifyRemove(hub, oaObj, checkType);
	}

	/**
	 * Determines whether all objects can be removed by checking for a
	 * non-null message from og.hubsInternal().callHubAddRemoveGetCantRemoveAllMessage.
	 *
	 * @param bCheckObjectCallback flag indicating whether to check callbacks
	 * @param checkType            callback check type
	 * @return true if all objects can be removed
	 */
	public boolean getAllowRemoveAll(final boolean bCheckObjectCallback, final int checkType) {
		OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		String s = og.hubsInternal().callHubAddRemoveGetCantRemoveAllMessage(this, checkType);
		return s == null;
	}
	
	/**
	 * Sets the loading flag by delegating to OARuntime.threadLocals().setLoading.
	 *
	 * @param b true to enable loading mode
	 */
	public boolean setLoading(boolean b) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		return srvcOAThreadLocal.setLoading(b);
	}

	/**
	 * Returns whether the current thread is marked as loading by delegating to
	 * OARuntime.threadLocals().isLoading().
	 *
	 * @return true if loading mode is enabled
	 */
	public boolean isLoading() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		return srvcOAThreadLocal.isLoading();
	}

	/**
	 * Notifies clients that this Hub has changed and should be refreshed by
	 * delegating to HubCSDelegate.sendRefresh.
	 */
	public void sendRefresh() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubCSSendRefresh(this);
	}

	/**
	 * Returns true if this Hub has zero objects. Equivalent to (getSize() == 0).
	 *
	 * @return true if the Hub is empty
	 */
	@Override
	public boolean isEmpty() {
		return getSize() == 0;
	}

	/**
	 * Returns true if all objects in the supplied Collection are contained in
	 * this Hub. A null Collection returns true. Iterates through each object
	 * and checks containment using contains(obj).
	 *
	 * @param c the Collection to check
	 * @return true if all objects are contained
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null) {
			return true;
		}
		for (Object obj : c) {
			if (!contains(obj)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds all objects from the supplied Collection to this Hub by delegating
	 * each addition to add((TYPE) obj). A null Collection returns true.
	 *
	 * @param c the Collection of objects to add
	 * @return true after completion
	 */
	@Override
	public boolean addAll(Collection<? extends TYPE> c) {
		if (c == null) {
			return true;
		}
		for (Object obj : c) {
			add((TYPE) obj);
		}
		return true;
	}

	/**
	 * Inserts all objects from the supplied Collection starting at the
	 * specified index. Delegates each insertion to insert((TYPE) obj, index++)
	 * so the insert position increments. A null Collection returns true.
	 *
	 * @param index starting index
	 * @param c     the Collection of objects to insert
	 * @return true after completion
	 */
	@Override
	public boolean addAll(int index, Collection<? extends TYPE> c) {
		if (c == null) {
			return true;
		}
		for (Object obj : c) {
			insert((TYPE) obj, index++);
		}
		return true;
	}

	/**
	 * Removes all objects in the supplied Collection from this Hub by calling
	 * remove(obj) for each element. A null Collection returns true.
	 *
	 * @param c the Collection of objects to remove
	 * @return true after completion
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		if (c == null) {
			return true;
		}
		for (Object obj : c) {
			remove(obj);
		}
		return true;
	}

	/**
	 * Retains only the objects contained in the supplied Collection. Iterates
	 * through the Hub by index using get(i); removes any object not in the
	 * Collection by calling removeAt(i). A null Collection returns true.
	 *
	 * @param c the Collection of objects to retain
	 * @return true after completion
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		if (c == null) {
			return true;
		}
		for (int i = 0;;) {
			Object obj = get(i);
			if (obj == null) {
				break;
			}
			if (c.contains(obj)) {
				i++;
			} else {
				if (removeAt(i) == null) {
					i++;
				}
			}
		}
		return true;
	}

	/**
	 * Returns the object at the specified index by delegating to getAt(index).
	 *
	 * @param index position to retrieve
	 * @return object at the index
	 */
	@Override
	public TYPE get(int index) {
		return getAt(index);
	}

	/**
	 * Replaces the object at the specified index with the supplied element.
	 * If the element is null, no change is made and null is returned.
	 * The existing object at the index is removed, and if a non-null object
	 * was removed, the new element is inserted at the same index.
	 *
	 * @param index   position to replace
	 * @param element new object to set
	 * @return the previously stored object, or null
	 */
	@Override
	public TYPE set(int index, TYPE element) {
		if (element == null) {
			return null;
		}
		TYPE objx = remove(index);
		if (objx != null) {
			insert(element, index);
		}
		return objx;
	}

	/**
	 * Inserts the supplied element at the specified index.
	 * If the element is null, no action is taken.
	 * Delegates the insertion to insert(element, index).
	 *
	 * @param index   position at which to insert
	 * @param element object to insert
	 */
	@Override
	public void add(int index, TYPE element) {
		if (element == null) {
			return;
		}
		insert(element, index);
	}

	/**
	 * Returns the last index of the supplied object by delegating to indexOf(o).
	 *
	 * @param o object to locate
	 * @return last index of the object, or -1 if not found
	 */
	@Override
	public int lastIndexOf(Object o) {
		return indexOf(o);
	}

	/**
	 * Returns an Iterator for this Hub by creating and returning the
	 * ListIterator produced by listIterator().
	 *
	 * @return Iterator over Hub elements
	 */
	@Override
	public Iterator<TYPE> iterator() {
		ListIterator<TYPE> listIterator = listIterator();
		return listIterator;
	}

	/**
	 * Returns a ListIterator over a snapshot of this Hub's elements.
	 * Creates an immutable snapshot using toArray(), wraps it with
	 * Arrays.asList, and returns a ListIterator that supports forward
	 * and backward navigation. Mutating operations delegate back to
	 * the Hub where appropriate.
	 *
	 * @return ListIterator over a snapshot of this Hub
	 */
	@Override
	public ListIterator<TYPE> listIterator() {
		// create a snapshot, so that concurrent issues dont happen
		final List<TYPE> list = new ArrayList<>(Arrays.asList(toArray()));

		ListIterator<TYPE> iter = new ListIterator<TYPE>() {
			int pos = -1;
			TYPE currentObject;

			@Override
			public boolean hasNext() {
				int x = list.size();
				return (x > 0 && pos < (x - 1));
			}

			@Override
			public void remove() {
				int size = list.size();
				if (pos >= 0 && pos < size) {
					Object objx = list.remove(pos);
					if (objx != null) {
						Hub.this.remove(objx);
					}
					size--;
					if (pos >= size) {
						pos = size-1;
					}
				}
			}

			@Override
			public TYPE next() throws NoSuchElementException {
				int x = list.size();
				if (pos < x) {
					++pos;
					if (pos < x) {
						currentObject = list.get(pos);
						return currentObject;
					}
				}
				return null;
			}

			@Override
			public boolean hasPrevious() {
				return (pos > 0);
			}

			@Override
			public TYPE previous() {
				if (pos >= 0) {
					--pos;
					if (pos >= 0) {
						currentObject = list.get(pos);
						return currentObject;
					}
				}
				return null;
			}

			@Override
			public int nextIndex() {
				int x = list.size();
				if (pos == x) {
					return pos;
				}
				return pos + 1;
			}

			@Override
			public int previousIndex() {
				if (pos < 0) {
					return pos;
				}
				return pos - 1;
			}

			@Override
			public void set(TYPE e) {
				if (pos >= 0 && pos < list.size()) {
					Hub.this.remove(currentObject);
					Hub.this.insert(e, pos);
					list.set(pos, e);
					currentObject = e;
				}
			}

			@Override
			public void add(TYPE e) {
				if (Hub.this.contains(e)) {
					return;
				}
				list.add(e);
				Hub.this.add(e);
			}
		};
		return iter;
	}

	/**
	 * Returns a ListIterator starting at the specified index.
	 * Obtains a ListIterator using listIterator() and then advances
	 * it by calling next() index times.
	 *
	 * @param index starting position
	 * @return ListIterator positioned at the specified index
	 */
	@Override
	public ListIterator<TYPE> listIterator(int index) {
		ListIterator li = listIterator();
		for (int i = 0; i < index; i++) {
			if (li.next() == null) break;
		}
		return li;
	}

	/**
	 * Returns a sublist of this Hub between the specified indices.
	 * Iterates from fromIndex to toIndex, retrieving elements with
	 * getAt(i). Stops early if a null element is encountered.
	 *
	 * @param fromIndex start index (inclusive)
	 * @param toIndex   end index (exclusive)
	 * @return List containing the elements in the range
	 */
	@Override
	public List<TYPE> subList(int fromIndex, int toIndex) {
		ArrayList al = new ArrayList();
		for (int i = fromIndex; i < toIndex; i++) {
			Object objx = getAt(i);
			if (objx == null) {
				break;
			}
			al.add(objx);
		}
		return al;
	}

	/**
	 * Returns a Stream of the objects in this Hub by delegating to
	 * the underlying data vector's stream().
	 *
	 * @return Stream of Hub elements
	 */
	public Stream<TYPE> stream() {
		return this.data.vector.stream();
	}

	/*
	    hub.onChangeAO( event -> {
	
	    });
	 */
	/**
	 * Registers a listener to notify when the active object changes.
	 * If onEvent is null, no action is taken. Adds a HubListener that
	 * calls onEvent.onEvent(e) in afterChangeActiveObject().
	 *
	 * @param onEvent callback to invoke on active object change
	 */
	public void onChangeAO(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void afterChangeActiveObject(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Registers a listener to notify after any property change.
	 * If onEvent is null, no action is taken. Adds a HubListener that
	 * calls onEvent.onEvent(e) in afterPropertyChange().
	 *
	 * @param onEvent callback to invoke on property change
	 */
	public void onPropertyChange(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void afterPropertyChange(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Registers a listener for changes to a specific property name.
	 * If onEvent is null or propName is empty, no action is taken.
	 * Adds a HubListener that invokes onEvent.onEvent(e) only when
	 * the event's property name matches propName (case-insensitive).
	 *
	 * @param onEvent  callback to invoke on matching property change
	 * @param propName property name to filter on
	 */
	public void onPropertyChange(HubOnEventInterface onEvent, String propName) {
		if (onEvent == null || OAString.isEmpty(propName)) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void afterPropertyChange(HubEvent e) {
				if (propName.equalsIgnoreCase(e.getPropertyName())) {
					onEvent.onEvent(e);
				}
			}
		});
	}

	/**
	 * Registers a callback to be invoked after an object is added to this Hub.
	 * If the supplied callback is null, the method returns without adding a listener.
	 *
	 * @param onEvent the callback to invoke after an add event
	 */
	public void onAdd(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void afterAdd(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Registers a callback to be invoked before this Hub is refreshed.
	 * If the supplied callback is null, the method returns without adding a listener.
	 * Adds a HubListener that calls onEvent.onEvent(e) from beforeRefresh(HubEvent).
	 *
	 * @param onEvent the callback to invoke before a refresh
	 */
	public void onBeforeRefresh(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void beforeRefresh(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Registers a callback to be invoked when this Hub receives a new list.
	 * If the supplied callback is null, the method exits without adding a listener.
	 * Adds a HubListener that calls onEvent.onEvent(e) from onNewList(HubEvent).
	 *
	 * @param onEvent the callback to invoke on a new list event
	 */
	public void onNewList(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void onNewList(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Registers a callback to be invoked after an object is removed
	 * from this Hub. If the supplied callback is null, the method
	 * returns without adding a listener.
	 * Adds a HubListener that calls onEvent.onEvent(e) from
	 * afterRemove(HubEvent).
	 *
	 * @param onEvent the callback to invoke after a remove event
	 */
	public void onRemove(HubOnEventInterface onEvent) {
		if (onEvent == null) {
			return;
		}
		addHubListener(new HubListenerAdapter() {
			@Override
			public void afterRemove(HubEvent e) {
				onEvent.onEvent(e);
			}
		});
	}

	/**
	 * Creates and returns a new Hub that is filtered by the supplied
	 * OAFilter. A new Hub is constructed for the same object class,
	 * and a HubFilter is created to manage filtering behavior using
	 * this Hub as the master.
	 *
	 * @param filter                   filter to apply
	 * @param dependentPropertyPaths   optional dependent property paths
	 * @return the newly created filtered Hub
	 */
	public Hub<TYPE> createFilteredHub(OAFilter filter, String... dependentPropertyPaths) {
		Hub h = new Hub(this.getObjectClass());
		HubFilter f = new HubFilter(this, h, filter, dependentPropertyPaths);
		return h;
	}

	public transient boolean DEBUG; // for debugging

	/**
	 * Refreshes this Hub by reselecting its data from the data source.
	 * Delegates the operation to og.hubsInternal().callHubSelectRefresh(this).
	 */
	public void refresh() {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(this);
		og.hubsInternal().callHubSelectRefresh(this);
	}


	public static final class FriendAccess {
		private FriendAccess() {
		}
		public <T extends OAObject> HubData<T> getHubData(Hub<T> hub) {
			return hub.data;
		}
		public <T extends OAObject> void setHubData(Hub<T> hub, HubData<T> data) {
			hub.data = data;
		}

		public <T extends OAObject> HubDataActive<T> getHubDataActive(Hub<T> hub) {
			return hub.dataa;
		}
		public void setHubDataActive(Hub<?> hub, HubDataActive dataa) {
			hub.dataa = dataa;
		}

		public <T extends OAObject> HubDataUnique<T> getHubDataUnique(Hub<T> hub) {
			return hub.datau;
		}
		public HubDataMaster getHubDataMaster(Hub<?> hub) {
			return hub.datam;
		}
		public void setHubDataMaster(Hub<?> hub, HubDataMaster dm) {
			hub.datam = dm;
		}
	}

	private final static FriendAccess friendAccess = new FriendAccess();
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}

	public OAGraph getGraph() {
		return OARuntime.graph(this);
	}

}
