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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

import com.viaoa.object.OACascade;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDSDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectDeleteDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectSaveDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OANullObject;

/**
 * Primary internal delegate that implements the operational logic of {@link Hub}.
 * <p>
 * The HubDelegate encapsulates all shared algorithms required by Hubs—object
 * identity resolution, uniqueness validation, master/detail synchronization,
 * and cascade-based change detection—so that the {@code Hub} class itself
 * remains a thin facade.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Evaluate Hub-level and object-level “changed” state using {@link com.viaoa.object.OACascade}.</li>
 *   <li>Enforce per-Hub uniqueness constraints via reflection-based property evaluation.</li>
 *   <li>Resolve canonical object identities through {@link com.viaoa.object.OAObjectCacheDelegate}.</li>
 *   <li>Maintain class and master-relationship metadata used by Hub detail wiring.</li>
 *   <li>Compute Hub validity and synchronization state across shared, linked, and merged graphs.</li>
 *   <li>Support safe re-linking and refresh detection through {@code HubCurrentStateEnum} logic.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * All methods are static and stateless; the delegate acts as a functional
 * utility layer shared by every Hub instance.  Internal helpers such as
 * {@code _getCurrentState} and {@code getControllingHub} are recursive graph
 * evaluators used by detail, merger, and filter Hubs to maintain coherence.
 *
 * <h3>Threading and Reentrancy</h3>
 * No mutable static state is maintained; all Hub instance data is passed in via
 * parameters.  Cascades, recursion guards, and {@link java.util.HashSet}
 * tracking prevent infinite traversal through cyclic Hub graphs.
 */
public class HubDelegate {
	private static Logger LOG = Logger.getLogger(HubDelegate.class.getName());
	public static final Boolean TRUE = Boolean.valueOf(true);
	public static final Boolean FALSE = Boolean.valueOf(false);

	/**
	 * Determines whether this hub or any of its contained OAObjects are marked as
	 * changed according to the supplied cascade rules.
	 *
	 * <p>
	 * The method first checks whether this hub has already been processed in the
	 * current cascade; if so, it returns {@code false}. It then evaluates the hub’s
	 * own changed state. If cascade rules allow, it iterates through each object in
	 * the hub and checks whether any OAObject reports a changed state.
	 *
	 * @param thisHub      the hub being evaluated
	 * @param iCascadeRule the cascade rule used to determine how far change
	 *                     detection should propagate
	 * @param cascade      the cascade tracker used to prevent reprocessing
	 * @return {@code true} if the hub or any contained OAObject is changed;
	 *         otherwise {@code false}
	 */
	public static boolean getChanged(Hub thisHub, int iCascadeRule, OACascade cascade) {
		if (cascade.wasCascaded(thisHub, true)) {
			return false;
		}

		if (HubDataDelegate.getChanged(thisHub)) {
			return true;
		}
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			return false;
		}

		if (thisHub.isOAObject()) {
			for (int i = 0;; i++) {
				Object object = HubDataDelegate.getObjectAt(thisHub, i);
				if (object == null) {
					break;
				}
				if (object instanceof OAObject) {
					OAObject obj = (OAObject) object;
					if (OAObjectDelegate.getChanged(obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Verifies that the specified object's unique property value does not already
	 * exist in this hub. If the hub or object is null, or if the object is loading,
	 * uniqueness checking is bypassed. When a unique property is defined, its value
	 * is obtained either through a link property or a getter method. Null or blank
	 * values are not checked.
	 *
	 * <p>
	 * The method iterates through all hub elements and compares each object's
	 * unique property value to that of the given object. If an equal value is found
	 * on a different object, the uniqueness constraint fails.
	 *
	 * @param thisHub the hub in which uniqueness is validated
	 * @param object  the object whose property value is being checked
	 * @return {@code true} if the unique value does not conflict; otherwise
	 *         {@code false}
	 */
	public static boolean verifyUniqueProperty(final Hub thisHub, final Object object) {
		if (thisHub == null || object == null) {
			return true;
		}

		if (object instanceof OAObject) {
			if (OAThreadLocalDelegate.isLoading()) {
				return true;
			}
		}

		Object object2;
		Method m = null;
		String uniqueLinkPropName;
		try {
			uniqueLinkPropName = thisHub.data.getUniqueProperty();
			if (uniqueLinkPropName == null) {
				uniqueLinkPropName = thisHub.datam.getUniqueProperty();
			}
			if (uniqueLinkPropName != null) {
				OAObjectInfo oi = thisHub.getOAObjectInfo();
				if (oi.getLinkInfo(uniqueLinkPropName) == null) {
					uniqueLinkPropName = null;
				}
			}

			if (uniqueLinkPropName != null) {
				object2 = OAObjectPropertyDelegate.getProperty((OAObject) object, uniqueLinkPropName);
			} else {
				m = thisHub.data.getUniquePropertyGetMethod();
				if (m == null) {
					m = thisHub.datam.getUniquePropertyGetMethod();
					if (m == null) {
						return true;
					}
				}
				object2 = m.invoke(object, (Object[]) null);
				if (object2 == null) {
					return true;
				}
				if (object2 instanceof String && ((String) object2).equals("")) {
					return true;
				}
			}
		} catch (Exception e) {
			String s = m == null ? "" : m.getName();
			throw new RuntimeException("Error invoking " + s, e);
		}

		for (int i = 0;; i++) {
			Object obj = thisHub.elementAt(i);
			if (obj == null) {
				break;
			}
			if (obj == object) {
				continue;
			}

			try {
				if (uniqueLinkPropName != null) {
					Object obj2 = OAObjectPropertyDelegate.getProperty((OAObject) obj, uniqueLinkPropName);
					if (OACompare.compare(obj2, object2) == 0) {
						return false;
					}
					continue;
				}

				Object obj2 = m.invoke(obj, (Object[]) null);
				if (obj2 == null) {
					continue;
				}
				if (obj2 == object2 || obj2.equals(object2)) {
					return false;
				}
			} catch (Exception e) {
				String s = m == null ? "" : m.getName();
				throw new RuntimeException("Error invoking " + s, e);
			}
		}
		return true;
	}

	/**
	 * Resolves the canonical instance of the given object for this hub. If the
	 * object's class does not match the hub's object class, the cache is queried
	 * first; if no cached instance exists, the hub is asked to resolve the object,
	 * potentially triggering data loading.
	 *
	 * @param hub    the hub providing the object class and lookup context
	 * @param object the object or key to resolve
	 * @return the resolved object instance, or the original value if no resolution
	 *         occurs
	 */
	public static Object getRealObject(Hub hub, Object object) {
		//qqqqqqqq method was protected
		if (object != null && !object.getClass().equals(hub.getObjectClass())) {
			Object objx = OAObjectCacheDelegate.get(hub.getObjectClass(), object);
			if (objx != null) {
				return objx;
			}
			object = HubDataDelegate.getObject(hub, object); // might not have loaded all data yet (fetchMore will be called)
		}
		return object;
	}

	/**
	 * Builds a property path linking the hub's object class through a sequence of
	 * classes. For each class in the array, the method locates a matching link
	 * property that targets that class. If multiple matching links are found, an
	 * exception is thrown. If no matching link exists, {@code null} is returned.
	 *
	 * @param hub     the starting hub whose object class defines the first segment
	 * @param classes array of classes describing the traversal path
	 * @return a dot-delimited property path, or {@code null} if a segment cannot be
	 *         resolved
	 */
	protected static String getPropertyPathforClasses(Hub hub, Class[] classes) {
		if (classes == null) {
			return null;
		}
		Class c = hub.getObjectClass();
		String path = null;
		int x = classes.length;
		for (int i = 0; i < x; i++) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(c); // this never returns null

			// find property to use
			List al = oi.getLinkInfos();
			OALinkInfo liFound = null;
			for (int ii = 0; ii < al.size(); ii++) {
				OALinkInfo li = (OALinkInfo) al.get(ii);
				if (classes[i].equals(li.getToClass())) {
					if (li.getToClass() == null) {
						if (liFound != null) {
							continue;
						}
					}
					if (liFound != null) {
						throw new RuntimeException("more then one link for hubClass=" + c + ", find linkClass=" + classes[i]);
					}
					liFound = li;
					// if (li.getType() == li.ONE) break;  // try to find ONE type, but will settle on MANY
				}
			}
			if (liFound == null) {
				return null;
			}
			if (path == null) {
				path = liFound.getName();
			} else {
				path += "." + liFound.getName();
			}
			c = classes[i];
		}
		return path;
	}

	/**
	 * Returns the master OAObject associated with this hub. If no master
	 * relationship exists or the hub is null, {@code null} is returned.
	 *
	 * @param hub the hub whose master object is requested
	 * @return the master OAObject, or {@code null} if none exists
	 */
	public static OAObject getMasterObject(Hub hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = HubDetailDelegate.getDataMaster(hub, true);
		if (dm == null) {
			return null;
		}
		return dm.getMasterObject();
	}

	/**
	 * Returns the class of the hub's master OAObject. If the master object exists,
	 * its class is returned; otherwise, if a master hub exists, that hub's object
	 * class is used. If neither is available, {@code null} is returned.
	 *
	 * @param hub the hub whose master object's class is requested
	 * @return the master class, or {@code null} if unavailable
	 */
	public static Class getMasterClass(Hub hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = HubDetailDelegate.getDataMaster(hub, true);
		Object obj = dm.getMasterObject();
		if (obj != null) {
			return obj.getClass();
		}
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub().getObjectClass();
		}
		return null;
	}

	/**
	 * Assigns the object class for this hub. The class cannot be changed if the hub
	 * already contains objects, has detail hubs, has a master object, or is shared.
	 * If validation passes, the hub's object class is updated.
	 *
	 * @param thisHub  the hub whose object class is being changed
	 * @param objClass the new object class
	 * @throws RuntimeException if the object class cannot be changed due to
	 *                          existing state constraints
	 */
	public static void setObjectClass(Hub thisHub, Class objClass) {
		if (thisHub.data.objClass != null && !thisHub.data.objClass.equals(objClass) && !thisHub.data.objClass.equals(OAObject.class)) {
			if (HubDataDelegate.getCurrentSize(thisHub) > 0
					|| (thisHub.datau.getVecHubDetail() != null && thisHub.datau.getVecHubDetail().size() > 0)) {
				throw new RuntimeException("cant change object class if objects are in hub");
			}
			HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
			if (dm.getMasterHub() != null || thisHub.datam.getMasterObject() != null) {
				throw new RuntimeException("cant change object class if masterObject exists");
			}
			if (thisHub.datau.getSharedHub() != null || HubShareDelegate.getSharedWeakHubSize(thisHub) > 0) {
				throw new RuntimeException("cant change object class since this is a shared hub.");
			}
		}
		// 20141111 removed since the select could be valid
		// HubSelectDelegate.cancelSelect(thisHub, true);
		thisHub.data.objClass = objClass;

		/* 20141111 not needed here
		if (objClass != null) {
		    // find out if class is OAObject
			thisHub.data.setOAObjectFlag(OAObject.class.isAssignableFrom(objClass));
			// thisHub.data.setObjectInfo(OAObjectInfoDelegate.getOAObjectInfo(objClass));
		}
		else {
		    thisHub.data.setObjectInfo(null);
		    thisHub.data.setOAObjectFlag(false);
		}
		*/
	}

	/**
	 * Determines whether the hub is in a valid state. A hub is invalid if its
	 * master hub exists but has no active master object, or if any linked hub is
	 * invalid and cannot auto-create missing objects. If an addHub exists, its
	 * validity is also checked recursively.
	 *
	 * @param thisHub the hub being evaluated
	 * @return {@code true} if the hub is valid; otherwise {@code false}
	 */
	public static boolean isValid(final Hub thisHub) {
		HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub, true);
		if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
			return false;
		}

		// 20181119 reworked to check other hubs for hubWithLink
		Hub h = HubLinkDelegate.getHubWithLink(thisHub, true);
		if (h != null) {
			Hub hx = h.datau.getLinkToHub();
			if (hx != null) {
				if (!HubDelegate.isValid(hx)) {
					return false;
				}
				if (hx.dataa.activeObject == null) {
					if (!h.datau.isAutoCreate()) {
						return false;
					}
				}
			}
		}

		if (thisHub.datau.getAddHub() != null) {
			return HubDelegate.isValid(thisHub.datau.getAddHub());
		}
		return true;
	}

	/**
	 * Enumeration describing the synchronization state of a hub during updates.
	 *
	 * <ul>
	 *   <li>{@code InSync} – the hub is correctly aligned with its master or linked
	 *       state.</li>
	 *   <li>{@code DetailDisconectedFromMaster} – the detail hub does not match its
	 *       expected master state.</li>
	 *   <li>{@code DetailHubNotSameAsMasterObject} – the detail hub contains a
	 *       different object than the master hub’s active object.</li>
	 *   <li>{@code HubMergerNotUpdated} – a hub merger is not in sync with its
	 *       source hubs.</li>
	 * </ul>
	 */
	public static enum HubCurrentStateEnum {
		InSync,
		DetailDisconectedFromMaster,
		DetailHubNotSameAsMasterObject, // caused when object/hubs are in flux (hub event that is calling listeners and changing linkages)
		HubMergerNotUpdated
	}

	/**
	 * Evaluates the current synchronization state of the hub, optionally populating
	 * a replacement hub or list when a mismatch is detected. This is a wrapper that
	 * delegates to the internal recursive implementation.
	 *
	 * @param thisHub the hub being examined
	 * @param hubNew  optional hub to receive corrected state contents
	 * @param alNew   optional list to receive corrected state contents
	 * @return the hub’s synchronization status
	 */
    public static <T> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew) {
        return _getCurrentState(thisHub, hubNew, alNew, new HashSet<Hub>());
    }

    /**
     * Internal recursive implementation for evaluating hub synchronization state.
     * Prevents cyclic traversal using the provided hub set. Traverses master hubs,
     * shared hubs, mergers, combined hubs, and filters to determine whether the hub
     * is aligned with its correct source.
     *
     * @param thisHub the hub being evaluated
     * @param hubNew  optional hub for corrected content
     * @param alNew   optional list for corrected content
     * @param hmHub   set of hubs visited to prevent cycles
     * @return the computed synchronization status, or {@code null} when a cycle is
     *         detected
     */
    protected static <T> HubCurrentStateEnum _getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew, final Set<Hub> hmHub) {
		if (thisHub == null) {
			return HubCurrentStateEnum.InSync;
		}
		if (hmHub.contains(thisHub)) {
            return null;
		}
		hmHub.add(thisHub);

		Hub hub = thisHub;
		Hub hubMaster;
		boolean bHasMaster = false;
		for (int i = 0;; i++, hub = hubMaster) {
			HubDataMaster dm = HubDetailDelegate.getDataMaster(hub, true);

			hubMaster = dm.getMasterHub();
			if (hubMaster == null) {
				break; // check for hubMerger
			}
			bHasMaster = true;

			final Object objMaster = hubMaster.getAO();
			if (objMaster == dm.getMasterObject()) {
				if (objMaster == null && thisHub.getSize() > 0) {
					return HubCurrentStateEnum.DetailDisconectedFromMaster;
				}
				continue;
			}

			if (i > 0) {
				return HubCurrentStateEnum.DetailDisconectedFromMaster;
			}

			if (objMaster != null && (hubNew != null || alNew != null)) {
				// find correct hub
				OALinkInfo li = dm.getDetailToMasterLinkInfo();
				if (li != null) {
					Object value = li.getReverseLinkInfo().getValue(objMaster);
					if (value != null) {
						if (value instanceof Hub) {
							if (hubNew != null) {
								hubNew.setSharedHub((Hub<T>) value);
							}
							if (alNew != null) {
								for (T objNext : ((Hub<T>) value)) {
									alNew.add(objNext);
								}
							}
						} else {
							if (hubNew != null) {
								hubNew.add((T) value);
							}
							if (alNew != null) {
								alNew.add((T) value);
							}
						}
					}
				}
			}
			return HubCurrentStateEnum.DetailHubNotSameAsMasterObject;
		}

		// check to see if hub is derived from another Hub, and check it

		hub = HubShareDelegate.getMainSharedHub(hub);

		HubMerger hubMerger = null;
		HubCombined hubCombined = null;
		HubFilter hubFilter = null;

		HubListener[] hls = HubEventDelegate.getAllListeners(hub);

		if (hls != null) {
			for (HubListener hl : hls) {
				if (!(hl instanceof HubListenerAdapter)) {
					continue;
				}
				HubListenerAdapter hla = (HubListenerAdapter) hl;
				Object listener = hla.getListener();
				if (listener instanceof HubMerger) {
					hubMerger = (HubMerger) hla.getListener();
					Hub hubx = hubMerger.getCombinedHub();
					if (hubx == hub) {
						break;
					}
					hubMerger = null;
				} else if (listener instanceof HubCombined) {
					hubCombined = (HubCombined) hla.getListener();
					Hub hubx = hubCombined.getMasterHub();
					if (hubx == hub) {
						break;
					}
					hubCombined = null;
				} else if (listener instanceof HubFilter) {
					hubFilter = (HubFilter) hla.getListener();
					Hub hubx = hubFilter.getHub();
					if (hubx == hub) {
						break;
					}
					hubFilter = null;
				}

			}
		}

		if (hubFilter != null) {
			Hub hubx = hubFilter.getMasterHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);
			if (hcs == HubCurrentStateEnum.InSync) {
				return hcs;
			}
			if (hubNew == null && alNew == null) {
				return hcs;
			}

			Hub hubTemp = new Hub();
			_getCurrentState(hubx, hubTemp, null, hmHub);

			for (Object objx : hubTemp) {
				if (!hubFilter.isUsed(objx)) {
					continue;
				}
				if (hubNew != null) {
					hubNew.add((T) objx);
				}
				if (alNew != null) {
					alNew.add((T) objx);
				}
			}

		} else if (hubCombined != null) {
			ArrayList<Hub> al = hubCombined.getHubs();
			if (al != null) {
				HubCurrentStateEnum hcs = null;
				for (Hub hubx : al) {
					hcs = _getCurrentState(hubx, null, null, hmHub);
					if (hcs != HubCurrentStateEnum.InSync) {
						break;
					}
				}
				if (hcs == null) {
					return HubCurrentStateEnum.InSync;
				}
				if (hubNew == null && alNew == null) {
					return hcs;
				}

				for (Hub hubx : al) {
					hcs = _getCurrentState(hubx, hubNew, alNew, hmHub);
				}
				return hcs;
			}

		} else if (hubMerger != null) {
			Hub hubx = hubMerger.getRootHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);

			if (hcs == HubCurrentStateEnum.InSync) {
				if (!OAThreadLocalDelegate.isHubMergerChanging() && !hubMerger.isLoadingCombinedHub()) {
					return hcs;
				}
			}

			if (hubNew == null && alNew == null) {
				return HubCurrentStateEnum.HubMergerNotUpdated;
			}

			Hub hubTemp = new Hub();

			_getCurrentState(hubx, hubTemp, null, hmHub);

			OAFinder finder = new OAFinder(hubMerger.getPath());

			ArrayList al;
			if (hubMerger.getUseAll()) {
				al = finder.find(hubTemp);
			} else {
				// ?? not sure that AO will be set
				al = finder.find((OAObject) hubTemp.getAO());
			}

			if (hubNew != null) {
				hubNew.add((List<T>) al);
			}
			if (alNew != null) {
				alNew.addAll((List<T>) al);
			}

			return HubCurrentStateEnum.HubMergerNotUpdated;
		}
		return HubCurrentStateEnum.InSync;
	}

    /**
     * Determines which hub controls this hub’s validity. If the hub has a master
     * hub, that master hub is returned. If a linked shared hub exists, its link
     * target or its controlling hub is returned. If an addHub is present, its
     * controlling hub is evaluated. Otherwise, this hub is returned.
     *
     * @param thisHub the hub whose controlling hub is requested
     * @return the controlling hub
     */
	public static Hub getControllingHub(Hub thisHub) {
		HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub, true);
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub();
		}

		// 20181119 find shared hub with link
		Hub hubWithLink = HubLinkDelegate.getHubWithLink(thisHub, true);

		if (hubWithLink != null && hubWithLink.datau.getLinkToHub() != null) {
			if (hubWithLink.datau.isAutoCreate()) {
				return getControllingHub(hubWithLink.datau.getLinkToHub());
			}
			return hubWithLink.datau.getLinkToHub();
		}
		if (thisHub.datau.getAddHub() != null) {
			return HubDelegate.getControllingHub(thisHub.datau.getAddHub());
		}
		return thisHub;
	}

	/**
	 * Returns this hub or any shared hub that has an addHub defined. Shared hubs
	 * are scanned using a filter to locate the first hub that supports additions.
	 *
	 * @param hub the hub to evaluate
	 * @return a hub with an addHub, or {@code null} if none exists
	 */
	public static Hub getAnyAddHub(final Hub hub) {
		if (hub.getAddHub() != null) {
			return hub;
		}

		// 20120716
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				return h.getAddHub() != null;
			}
		};
		Hub[] hubs = HubShareDelegate.getAllSharedHubs(hub, filter);

		// was: Hub[] hubs = HubShareDelegate.getAllSharedHubs(hub);
		for (int i = 0; i < hubs.length; i++) {
			if (hubs[i].getAddHub() != null) {
				return hubs[i];
			}
		}
		return null;
	}

	/**
	 * Updates link relationships for objects added to or removed from this hub.
	 * When objects are removed, the method determines whether the reverse link
	 * requires deletion, reference removal, or persistence based on the link type,
	 * master relationship, and cascade rules. Many-to-many links are updated when
	 * needed. New objects are skipped because they do not yet exist in the data
	 * source.
	 *
	 * @param thisHub       the hub whose add/remove state is being processed
	 * @param iCascadeRule  the cascade rule for save/delete operations
	 * @param cascade       the cascade tracker for preventing reprocessing
	 * @param bIsSaving     whether the caller is performing a save operation
	 */
	protected static void _updateHubAddsAndRemoves(final Hub thisHub, final int iCascadeRule, final OACascade cascade,
			final boolean bIsSaving) {
		// removed Objects need to be saved if reference = null.
		HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
		boolean bM2M = (dm != null && dm.liDetailToMaster != null && dm.liDetailToMaster.getType() == OALinkInfo.MANY);
		OALinkInfo liRev = null;
		if (dm != null && dm.liDetailToMaster != null) {
			liRev = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster);
		}

		boolean bHasMethod = true;
		if (dm == null) {
		} else if (bM2M) {
			bHasMethod = false;
			if (dm.getMasterObject() != null && dm.liDetailToMaster != null) {
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		} else {
			// 20120907 cases where there is not a public method created, and would use a link table.
			Method method = OAObjectInfoDelegate.getMethod(dm.liDetailToMaster);
			if (method == null || ((method.getModifiers() & (Modifier.PRIVATE)) != 0)) {
				bHasMethod = false;
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		}

		Object[] objs = HubDataDelegate.getRemovedObjects(thisHub);
		if (objs == null) {
			return;
		}

		for (int i = 0; i < objs.length; i++) {
			OAObject obj = (OAObject) objs[i];
			if (obj.getNew()) {
				continue; // does not exist in DS
			}
			if (liRev != null && liRev.isOwner()) {
				if (dm.liDetailToMaster != null) {
					Object ox = OAObjectReflectDelegate.getProperty(obj, dm.liDetailToMaster.getName());
					if (ox == null) {
						OAObjectDeleteDelegate.delete(obj, cascade);
					}
				}
			} else if (dm != null && dm.liDetailToMaster != null && bHasMethod) {
				Object ox = OAObjectReflectDelegate.getProperty(obj, dm.liDetailToMaster.getName());
				if (ox == null) { // else property has been reassigned
					// 20120925
					OAObjectDSDelegate.removeReference(obj, dm.liDetailToMaster);
					//was: OAObjectSaveDelegate._saveObjectOnly(obj, cascade);
				}
			} else if (bIsSaving && dm != null && dm.liDetailToMaster != null && !bHasMethod && OASync.isServer() && !obj.isDeleted()) {
				// 20181126 if it is a removed object from ServerRoot, need to save now
				OAObjectSaveDelegate.save(obj, iCascadeRule, cascade);
			}
		}
	}

	/**
	 * Synchronizes many-to-many link table entries for this hub. Added and removed
	 * objects are examined and cross-updated on the opposite hub. When changes
	 * occur, the link table is updated using the master object's reverse link
	 * property.
	 *
	 * @param thisHub the hub whose many-to-many links are being updated
	 * @param dm      the master relationship information for this hub
	 */
	private static void updateMany2ManyLinks(Hub thisHub, HubDataMaster dm) {
		if (dm == null || dm.liDetailToMaster == null) {
			return;
		}
		OAObject[] adds = HubAddRemoveDelegate.getAddedObjects(thisHub);
		OAObject[] removes = HubAddRemoveDelegate.getRemovedObjects(thisHub);

		boolean b = false;
		// cross update opposite hub vecAdd/Remove
		for (int i = 0; adds != null && i < adds.length; i++) {
			b = true;
			if (adds[i] == null) continue;
			OAObject obj = adds[i];
			if (obj.getNew()) continue;
			Object objx = OAObjectReflectDelegate.getRawReference(obj, dm.liDetailToMaster.getName());
			if (objx instanceof Hub) {
				HubDataDelegate.removeFromAddedList((Hub) objx, dm.getMasterObject());
			}
		}
		for (int i = 0; removes != null && i < removes.length; i++) {
			b = true;
			if (removes[i] == null) continue;
			OAObject obj = (OAObject) removes[i];
			Object objx = OAObjectReflectDelegate.getRawReference(obj, dm.liDetailToMaster.getName());
			if (objx instanceof Hub) {
				HubDataDelegate.removeFromRemovedList((Hub) objx, dm.getMasterObject());
			}
		}
		if (b) {
			String propFromMaster = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster).getName();
			HubDSDelegate.updateMany2ManyLinks(dm.getMasterObject(), adds, removes, propFromMaster);
		}
	}

	/**
	 * Configures the hub to enforce uniqueness based on the specified property.
	 * Validates that the property is not nested, that a corresponding getter
	 * method exists, and that the getter accepts no parameters. When {@code null}
	 * is supplied, the unique property is cleared.
	 *
	 * @param thisHub      the hub whose unique property is being set
	 * @param propertyName the name of the property used for uniqueness, or
	 *                     {@code null} to clear
	 * @throws IllegalArgumentException if the property is nested, lacks a getter,
	 *                                  or the getter requires parameters
	 */
	public static void setUniqueProperty(Hub thisHub, String propertyName) {
		if (propertyName == null) {
			thisHub.data.setUniqueProperty(null);
			thisHub.data.setUniquePropertyGetMethod(null);
			return;
		}
		if (propertyName.indexOf('.') >= 0) {
			throw new IllegalArgumentException(
					"Property " + propertyName + " can only be for a property in " + thisHub.getObjectClass().getName());
		}

		thisHub.data.setUniquePropertyGetMethod(OAObjectInfoDelegate.getMethod(thisHub.getObjectClass(), "get" + propertyName));
		if (thisHub.data.getUniquePropertyGetMethod() == null) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " not found");
		}
		if (thisHub.data.getUniquePropertyGetMethod().getParameterTypes().length > 0) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " expects parameters");
		}
		thisHub.data.setUniqueProperty(propertyName);
	}

	/**
	 * Enables automatic sequencing of objects in this hub by assigning sequential
	 * values to the specified property. Existing auto-sequence handlers are closed
	 * before creating a new one. Sorting is canceled to preserve sequence order.
	 * When the hub is a detail hub, sequencing is only enabled on the server side.
	 *
	 * @param thisHub     the hub whose objects will receive sequence values
	 * @param property    the property to update with sequence numbers
	 * @param startNumber the initial sequence number
	 * @param bKeepSeq    whether sequence values are preserved after removals
	 */
	public static void setAutoSequence(Hub thisHub, String property, int startNumber, boolean bKeepSeq) {
		// 20091030 only set for server for detail hubs
		boolean bServerOnly = false;
		if (thisHub.getMasterObject() != null) {
			if (!HubCSDelegate.isServer(thisHub)) {
				return; // only set up for server
			}
			bServerOnly = true;
		}
		if (thisHub.data.getAutoSequence() != null) {
			thisHub.data.getAutoSequence().close();
		}
		thisHub.cancelSort(); // 20090801 need to remove any sorters
		thisHub.data.setAutoSequence(new HubAutoSequence(thisHub, property, startNumber, bKeepSeq, bServerOnly));
	}

	/**
	 * Returns the auto-sequence controller for this hub, or {@code null} if none is
	 * assigned.
	 *
	 * @param thisHub the hub whose auto-sequence handler is requested
	 * @return the auto-sequence object, or {@code null} if not configured
	 */
	public static HubAutoSequence getAutoSequence(Hub thisHub) {
		return thisHub.data.getAutoSequence();
	}

	/**
	 * Recomputes sequence values for all objects in this hub when auto-sequence is
	 * enabled. If no auto-sequence handler exists, no action is taken.
	 *
	 * @param thisHub the hub whose sequence values will be recalculated
	 */
	public static void resequence(Hub thisHub) {
		if (thisHub.data.getAutoSequence() != null) {
			thisHub.data.getAutoSequence().resequence();
		}
	}

	/**
	 * Ensures that for every object in the master hub, there is a corresponding
	 * object in this hub whose specified property points to that master object.
	 * Existing auto-match handlers are closed before creating a new one. The match
	 * logic supports server-side restriction.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property on this hub's objects used to match
	 * @param hubMaster       the hub whose objects must be mirrored
	 * @param bServerSideOnly whether matching should only be enforced on the server
	 */
	public static void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly) {
		if (thisHub.data.getAutoMatch() != null) {
			thisHub.data.getAutoMatch().close();
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch am = new HubAutoMatch();
		thisHub.data.setAutoMatch(am);
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, null, null);
		// }
	}

	/**
	 * Variant of auto-match initialization that includes a stopping condition. For
	 * each object in the master hub, this hub ensures a corresponding object exists
	 * unless the match path encounters the specified stop object and property.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property used to link to master hub objects
	 * @param hubMaster       the hub being mirrored
	 * @param bServerSideOnly whether matching is server-only
	 * @param objStop         optional object used to limit matching
	 * @param stopProperty    the property that defines the stopping condition
	 */
	public static void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		if (thisHub.data.getAutoMatch() != null) {
			thisHub.data.getAutoMatch().close();
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch am = new HubAutoMatch();
		thisHub.data.setAutoMatch(am);
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, objStop, stopProperty);
		// }
	}

	/**
	 * Returns the auto-match controller for this hub, or {@code null} if no
	 * auto-match logic is configured.
	 *
	 * @param thisHub the hub whose auto-match handler is requested
	 * @return the auto-match object, or {@code null} if none exists
	 */
	public static HubAutoMatch getAutoMatch(Hub thisHub) {
		return thisHub.data.getAutoMatch();
	}

	/**
	 * Returns the logical size of this hub. If the hub is backed by a select with
	 * more data available, counting and fetch operations are used to determine the
	 * full size. If no select applies, the in-memory object count is returned.
	 *
	 * @param thisHub the hub whose size is requested
	 * @return the number of objects the hub represents
	 */
	public static int getSize(Hub thisHub) {
		if (HubSelectDelegate.isMoreData(thisHub)) {
			if (!HubSelectDelegate.isCounted(thisHub)) {
				if (HubDataDelegate.getCurrentSize(thisHub) == 0) {
					HubSelectDelegate.fetchMore(thisHub); // see if this will load it, before calling count on the select
					if (!HubSelectDelegate.isMoreData(thisHub)) {
						return getSize(thisHub);
					}
				}
			}
			int x = HubSelectDelegate.getCount(thisHub);
			if (x > 0) {
				return x;
			}
		}
		return HubDataDelegate.getCurrentSize(thisHub);
	}

	/**
	 * Ensures that all data is loaded into the hub and then returns its size. A
	 * {@code null} hub returns zero.
	 *
	 * @param thisHub the hub whose fully loaded size is requested
	 * @return the loaded size of the hub
	 */
	public static int getLoadedSize(Hub thisHub) {
		if (thisHub == null) {
			return 0;
		}
		thisHub.loadAllData();
		return getSize(thisHub);
	}

	private static int cntLoadedSizeError;

	/**
	 * Stores a named property value on the hub. Property names are normalized to
	 * uppercase. A {@link OANullObject} marker is stored when the value is
	 * {@code null}. A new property map is created on demand.
	 *
	 * @param thisHub the hub whose property map is updated
	 * @param name    the property name
	 * @param obj     the value to store, or {@code null}
	 */
	protected static void setProperty(Hub thisHub, String name, Object obj) {
		if (name == null) {
			return;
		}
		name = name.toUpperCase();
		if (thisHub.data.getHashProperty() == null) {
			thisHub.data.setHashProperty(new Hashtable(7));
		}
		thisHub.data.getHashProperty().put(name, (obj == null) ? OANullObject.instance : obj);
	}

	/**
	 * Retrieves a named property value previously stored on the hub. Property names
	 * are normalized to uppercase. A stored {@link OANullObject} resolves to
	 * {@code null}. If no property map exists, {@code null} is returned.
	 *
	 * @param thisHub the hub whose property is requested
	 * @param name    the property name
	 * @return the stored value, or {@code null} if not found
	 */
	protected static Object getProperty(Hub thisHub, String name) {
		if (thisHub.data.getHashProperty() == null) {
			return null;
		}

		name = name.toUpperCase();
		Object obj = thisHub.data.getHashProperty().get(name);
		if (obj instanceof OANullObject) {
			obj = null;
		}
		return obj;
	}

	/**
	 * Removes a property from the hub’s property map. Property names are converted
	 * to uppercase. If no property map exists, no action is taken.
	 *
	 * @param thisHub the hub whose property should be removed
	 * @param name    the name of the property to remove
	 */
	protected static void removeProperty(Hub thisHub, String name) {
		if (thisHub.data.getHashProperty() != null) {
			name = name.toUpperCase();
			thisHub.data.getHashProperty().remove(name);
		}
	}

	/**
	 * Updates referenceability settings for this hub and its parent objects. If the
	 * hub is server-side and the object class supports weak referencing, this method
	 * adjusts weak-reference behavior based on whether references should be
	 * maintained. When enabling referenceability, parent objects are also updated.
	 *
	 * @param hub            the hub whose referenceability is being updated
	 * @param bReferenceable whether objects referenced by this hub should remain
	 *                       strongly referenceable
	 */
	public static void setReferenceable(Hub hub, boolean bReferenceable) {
		if (hub == null) {
			return;
		}
		if (!OASyncDelegate.isServer(hub)) {
			return;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(hub.getObjectClass());
		if (!OAObjectInfoDelegate.isWeakReferenceable(oi)) {
			return;
		}
		boolean bSupportStorage = oi.getSupportsStorage();

		Object master = HubDelegate.getMasterObject(hub);
		if (master == null) return;

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		if (li == null) {
			return;
		}
		OALinkInfo liRev = li.getReverseLinkInfo();
		if (liRev == null) {
			return;
		}

		if (liRev.getCacheSize() > 0) {
			if (bReferenceable || bSupportStorage) {
				boolean b = OAObjectPropertyDelegate.setPropertyWeakRef((OAObject) master, liRev.getName(), !bReferenceable, hub);
				if (!b) {
					return; // already done, dont need to check/change parents
				}
			}
		}

		if (bReferenceable) {
			// make parents referenceable
			OAObjectPropertyDelegate.setReferenceable((OAObject) master, bReferenceable);
		}
	}
}
