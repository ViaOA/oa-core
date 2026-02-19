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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPerformance;
import com.viaoa.object.OATrigger;
import com.viaoa.object.OATriggerDelegate;
import com.viaoa.object.OATriggerListener;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAPropertyPath;

/**
 * June/July 2016 Not currently used. This was going to replace HubListenerTree. This has been tested to replace HubListenerTree. It will
 * create/use triggers for dependent propertyPaths, which seems unnecessary for most cases.
 */
public class HubListenerTrigger<TYPE extends OAObject> {
	private static Logger LOG = Logger.getLogger(HubListenerTrigger.class.getName());

	/**
	 * The Hub instance associated with this listener trigger manager. All
	 * listeners and triggers created by this class apply to this hub.
	 */
	private final Hub hub;
	
	/**
	 * Ordered array of HubListener instances registered on this Hub. Declared
	 * volatile to guarantee visibility of modifications across threads.
	 */
	private volatile HubListener[] listeners;
	
	/**
	 * Synchronization lock used to ensure thread-safe modifications to listener
	 * collections and internal structures.
	 */
	private final Object lock = new Object();
	
	/**
	 * Count of listeners that are flagged with InsertLocation.LAST so that new
	 * listeners are inserted in the correct relative order.
	 */
	private volatile int cntLast; // listeners that are flagged to be last

	/**
	 * Holds metadata for a HubListener that has dependent property listeners or
	 * triggers created on its behalf.
	 */
	private static class ListenerInfo {
		/**
		 * The originating HubListener associated with the dependent triggers and
		 * extra listening properties.
		 */
		HubListener hl;

		/**
		 * List of property names for which extra property-change listening has been
		 * established for this HubListener.
		 */
		ArrayList<String> alExtraListenerProperties;

		/**
		 * List of OATrigger instances created for dependent property paths for
		 * this HubListener.
		 */
		ArrayList<OATrigger> alTrigger;
	}

	/**
	 * Collection of ListenerInfo objects describing HubListeners that required
	 * dependent listener or trigger creation.
	 */
	private ArrayList<ListenerInfo> alListenerInfo;

	/**
	 * Structure for associating a property path with a created OATrigger.
	 */
	private static class TriggerInfo {
		/**
		 * The dependent property path represented by this trigger.
		 */
		String propertyPath;

		/**
		 * The trigger used to detect changes along the associated property path.
		 */
		OATrigger trigger;
	}

	/**
	 * Maps a property name (uppercase) to a list of calc-property names that
	 * depend on it, used for efficient property-change routing.
	 */
	private ConcurrentHashMap<String, ArrayList<String>> hsExtraProperties = new ConcurrentHashMap<String, ArrayList<String>>(); // prop.upper

	/**
	 * Extra HubListener that listens for simple property changes or direct
	 * one-link references when no triggers are required.
	 */
	private HubListener hlExtra; // extra hublistener that will listen to any of the local propertys or one links (not many)
	
	/**
	 * Maps uppercase property paths to their corresponding OATrigger instances,
	 * allowing reuse and cleanup when listeners are removed.
	 */
	private HashMap<String, OATrigger> hsTrigger; // propertyPath.upper

	/**
	 * Maps uppercase property paths to their corresponding OATrigger instances,
	 * allowing reuse and cleanup when listeners are removed.
	 */
	public HubListenerTrigger(Hub<TYPE> hub) {
		this.hub = hub;
	}
	/**
	 * Returns the ordered list of HubListeners currently registered on this
	 * HubListenerTrigger.
	 *
	 * @return array of HubListener instances or null if none exist
	 */

	public HubListener<TYPE>[] getHubListeners() {
		return this.listeners;
	}

	/**
	 * Adds a HubListener to this trigger manager, positioning it according to
	 * its InsertLocation and updating the last-listener count.
	 *
	 * @param hl the listener to add
	 * @return true if the listener was added; false if already registered
	 */
	public boolean addListener(HubListener<TYPE> hl) {
		if (hl == null) {
			return false;
		}

		synchronized (lock) {
			if (OAArray.containsExact(listeners, hl)) {
				return false;
			}

			HubListener.InsertLocation loc = hl.getLocation();
			if (listeners == null || listeners.length == 0 || loc == HubListener.InsertLocation.LAST || (loc == null && cntLast == 0)) {
				if (loc == HubListener.InsertLocation.LAST) {
					cntLast++;
				}
				listeners = (HubListener[]) OAArray.add(HubListener.class, listeners, hl);
			} else if (loc == HubListener.InsertLocation.FIRST) {
				listeners = (HubListener[]) OAArray.insert(HubListener.class, listeners, hl, 0);
			} else {
				// insert before first last
				boolean b = false;
				for (int i = listeners.length - 1; i <= 0; i--) {
					if (listeners[i].getLocation() != HubListener.InsertLocation.LAST) {
						listeners = (HubListener[]) OAArray.insert(HubListener.class, listeners, hl, i + 1);
						b = true;
						break;
					}
				}
				if (!b) {
					listeners = (HubListener[]) OAArray.add(HubListener.class, listeners, hl);
				}
			}
			if (listeners.length % 50 == 0) {
				LOG.fine("HubListenerTree.listeners.size()=" + listeners.length + ", hub=" + hub);
			}
		}
		return true;
	}

	/**
	 * Adds a HubListener and associates it with the specified property. If the
	 * property has dependent paths or calculated dependencies, additional setup
	 * will occur.
	 *
	 * @param hl the listener to add
	 * @param property the property name to monitor
	 * @return true if the listener or its dependent behavior was added
	 */
	public boolean addListener(HubListener hl, String property) {
		if (hl == null) {
			return false;
		}
		return addListener(hl, property, null);
	}

	/**
	 * Adds a HubListener associated with a primary property along with any
	 * dependent property paths that should trigger calc-property updates.
	 * Creates a shared OATriggerListener used for routing events.
	 *
	 * @param hl the listener to add
	 * @param propertyName the main property monitored by the listener
	 * @param dependentPropertyPaths additional dependent property paths
	 * @return true if the listener or dependent behavior was added
	 */
	public boolean addListener(HubListener hl, final String propertyName, String[] dependentPropertyPaths) {
		if (hl == null) {
			return false;
		}

		String s = "";
		if (dependentPropertyPaths != null) {
			for (String triggerPropPath : dependentPropertyPaths) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += triggerPropPath;
			}
		}

		Class c = hub.getObjectClass();
		s = ((c == null ? "" : c.getSimpleName()) + ", property=" + propertyName + ", ppDepend=[" + s + "]");
		LOG.fine(s);
		if (OAPerformance.IncludeHubListeners) {
			OAPerformance.LOG.fine(s);
		}

		boolean bWasAdded = addListener(hl);

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(hub.getObjectClass());
		String[] calcProps = null;
		for (OACalcInfo ci : oi.getCalcInfos()) {
			if (ci.getName().equalsIgnoreCase(propertyName)) {
				calcProps = ci.getDependentProperties();
				break;
			}
		}

		if (calcProps == null || calcProps.length == 0) {
			if (dependentPropertyPaths == null || dependentPropertyPaths.length == 0) {
				return bWasAdded;
			}
		}

		OATriggerListener triggerListener = new OATriggerListener() {
			@Override
			public void onTrigger(final OAObject rootObject, final HubEvent hubEvent, final String propertyPathFromRoot) throws Exception {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(rootObject);
				if (rootObject != null) {
					if (HubListenerTrigger.this.hub.contains(rootObject)) {
						og.hubsInternal().callHubEventFireCalcPropertyChange(HubListenerTrigger.this.hub, rootObject, propertyName);
					}
					return;
				}

				// the reverse property could not be used to get objRoot - need to find root objs and send calc event
				/*qqqqqqq
				if (!hub.isOAObject()) {
					og.hubsInternal().callHubEventFireCalcPropertyChange(HubListenerTrigger.this.hub, rootObject, propertyName);
					return;
				}
				*/
				if (hub.getSize() == 0) {
					return;
				}

				// need to find all objects that are affected
				OAFinder finder = new OAFinder(propertyPathFromRoot) {
					protected boolean isUsed(OAObject obj) {
						if (obj == hubEvent.getObject()) {
							return true;
						}
						Hub h = hubEvent.getHub();
						if (h == null) {
							return false;
						}
						if (h.getMasterObject() == obj) {
							return true;
						}
						return false;
					}
				};
				finder.setUseOnlyLoadedData(true); // objects will be already loaded if calc prop already got it's value, otherwise the value has not been calculated yet.
				for (Object obj : hub) {
					try {
						if (finder.findFirst((OAObject) obj) != null) {
							og.hubsInternal().callHubEventFireCalcPropertyChange(HubListenerTrigger.this.hub, obj, propertyName);
						}
					} catch (Exception e) {
						break;
					}
				}
			}
		};

		if (calcProps != null && calcProps.length > 0) {
			if (addDependentListeners(triggerListener, hl, propertyName, calcProps)) {
				bWasAdded = true;
			}
		}

		// now add the additional dependent properties
		if (dependentPropertyPaths != null && dependentPropertyPaths.length > 0) {
			if (addDependentListeners(triggerListener, hl, propertyName, dependentPropertyPaths)) {
				bWasAdded = true;
			}
		}
		return bWasAdded;
	}

	/**
	 * Synchronized wrapper to create dependent listeners or triggers for the
	 * given HubListener across all property paths in dependentPropertyPaths.
	 *
	 * @param triggerListener shared listener used by created triggers
	 * @param hl the originating HubListener
	 * @param propertyName the root property monitored
	 * @param dependentPropertyPaths dependent paths requiring listeners/triggers
	 * @return true if any dependent listeners or triggers were created
	 */
	private boolean addDependentListeners(OATriggerListener triggerListener, final HubListener<TYPE> hl, final String propertyName,
			final String[] dependentPropertyPaths) {
		if (dependentPropertyPaths == null || dependentPropertyPaths.length == 0) {
			return false;
		}
		synchronized (lock) {
			return _addDependentListeners(triggerListener, hl, propertyName, dependentPropertyPaths);
		}
	}

	/**
	 * Internal implementation for establishing dependent listeners/triggers.
	 * Creates or retrieves ListenerInfo entries and associates properties
	 * and triggers as needed.
	 *
	 * @param triggerListener the trigger listener used for notifications
	 * @param hl the originating HubListener
	 * @param propertyName the primary property being monitored
	 * @param dependentPropertyPaths property paths for dependency tracking
	 * @return true if new listeners/triggers were added
	 */
	private boolean _addDependentListeners(final OATriggerListener triggerListener, final HubListener<TYPE> hl, final String propertyName,
			final String[] dependentPropertyPaths) {
		ListenerInfo li = null;

		if (alListenerInfo != null) {
			for (ListenerInfo lix : alListenerInfo) {
				if (lix.hl == hl) {
					li = lix;
					break;
				}
			}
		}
		if (li == null) {
			li = new ListenerInfo();
			li.hl = hl;
		}

		boolean bUsed = false;

		boolean bWasAdded = false;

		for (String dpp : dependentPropertyPaths) {
			if (dpp == null || dpp.length() == 0) {
				continue;
			}

			if (_addDependentListener(triggerListener, 0, li, propertyName, dpp)) {
				bWasAdded = true;
			}
			;
			if (bWasAdded && !bUsed) {
				if (alListenerInfo == null) {
					alListenerInfo = new ArrayList<HubListenerTrigger.ListenerInfo>();
				}
				if (!alListenerInfo.contains(li)) {
					alListenerInfo.add(li);
				}
				bUsed = true;
			}
		}

		if (hlExtra == null && hsExtraProperties.size() > 0) {
			hlExtra = new HubListenerAdapter() {
				public void afterPropertyChange(HubEvent e) {
					String prop = e.getPropertyName();
					if (prop == null) {
						return;
					}

					ArrayList<String> al = hsExtraProperties.get(prop.toUpperCase());
					if (al == null) {
						return;
					}

					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
					for (String s : al) {
						og.hubsInternal().callHubEventFireCalcPropertyChange(hub, e.getObject(), s);
					}
				}
			};
			if (addListener(hlExtra)) {
				bWasAdded = true;
			}
		}
		;
		return bWasAdded;
	}

	/**
	 * Creates dependent listeners or triggers for a single property path.
	 * Handles recursion through calc-property dependencies, determines when
	 * a trigger is required, and updates ListenerInfo structures.
	 *
	 * @param triggerListener listener used for trigger callbacks
	 * @param cnter recursion depth to prevent infinite loops
	 * @param listenerInfo metadata for the originating HubListener
	 * @param propertyName name of the monitored property
	 * @param dependentPropertyPath the dependent property path to register
	 * @return true if a listener or trigger was added
	 */
	private boolean _addDependentListener(final OATriggerListener triggerListener, final int cnter, final ListenerInfo listenerInfo,
			final String propertyName, final String dependentPropertyPath) {
		if (cnter > 15) {
			return false;
		}

		// 20160720 if hub is groupBy, then 
		Class c = hub.getObjectClass();
		if (OAGroupBy.class.equals(c)) {

		}

		final OAPropertyPath pp = new OAPropertyPath(hub.getObjectClass(), dependentPropertyPath);
		final String[] props = pp.getProperties();
		final OALinkInfo[] lis = pp.getLinkInfos();
		boolean bWasAdded = false;

		if ((lis.length > 0 && lis[0].getType() == OALinkInfo.ONE) || (lis.length == 0 && props.length == 1)) {
			ArrayList<String> al = hsExtraProperties.computeIfAbsent(props[0].toUpperCase(), k -> new ArrayList<String>()); 
			
			if (propertyName != null && !al.contains(propertyName.toUpperCase())) {
				al.add(propertyName.toUpperCase());
				bWasAdded = true;
			}

			if (listenerInfo.alExtraListenerProperties == null) {
				listenerInfo.alExtraListenerProperties = new ArrayList<String>();
			}
			if (!listenerInfo.alExtraListenerProperties.contains(props[0].toUpperCase())) {
				listenerInfo.alExtraListenerProperties.add(props[0].toUpperCase());
				bWasAdded = true;
			}

			boolean bNeedsTrigger = (props.length > 1);

			if (lis.length == 0) {
				// could be a calcProp
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(hub.getObjectClass());
				String[] calcProps = null;
				for (OACalcInfo ci : oi.getCalcInfos()) {
					if (ci.getName().equalsIgnoreCase(props[0])) {
						// make recursive
						String[] ps = ci.getDependentProperties();
						if (ps == null) {
							break;
						}
						for (String p : ps) {
							if (_addDependentListener(triggerListener, cnter + 1, listenerInfo, propertyName, p)) {
								bWasAdded = true;
							}
							;
						}
						break;
					}
				}
			}
			if (!bNeedsTrigger) {
				return bWasAdded;
			}
		}

		// see if a trigger has already been created for this listener
		OATrigger trigger;
		if (hsTrigger == null) {
			hsTrigger = new HashMap<String, OATrigger>();
		} else {
			trigger = hsTrigger.get(dependentPropertyPath.toUpperCase());
			if (trigger != null) {
				if (listenerInfo.alTrigger == null) {
					listenerInfo.alTrigger = new ArrayList<OATrigger>();
				}
				if (!listenerInfo.alTrigger.contains(trigger)) {
					listenerInfo.alTrigger.add(trigger);
					bWasAdded = true;
				}
				return bWasAdded;
			}
		}

		trigger = new OATrigger(propertyName, hub.getObjectClass(), triggerListener, dependentPropertyPath, true, false, false, true);
		OATriggerDelegate.createTrigger(trigger, true);

		hsTrigger.put(dependentPropertyPath.toUpperCase(), trigger);

		if (listenerInfo.alTrigger == null) {
			listenerInfo.alTrigger = new ArrayList<OATrigger>();
		}
		if (!listenerInfo.alTrigger.contains(trigger)) {
			listenerInfo.alTrigger.add(trigger);
		}
		return true;
	}

	/**
	 * Removes a HubListener and cleans up dependent listeners and triggers.
	 *
	 * @param hl the listener being removed
	 * @return true if the listener was removed; false otherwise
	 */
	public boolean removeListener(HubListener hl) {
		if (hl == null) {
			return false;
		}
		synchronized (lock) {
			return _removeListener(hl);
		}
	}

	/**
	 * Internal implementation for listener removal. Removes the listener from
	 * the ordered list, updates LAST-listener count, removes dependent ListenerInfo,
	 * cleans up extra-property listeners, and deletes unused triggers.
	 *
	 * @param hl the listener being fully removed
	 * @return true if removal succeeded
	 */
	private boolean _removeListener(HubListener hl) {
		HubListener[] hold = listeners;
		listeners = (HubListener[]) OAArray.removeValue(HubListener.class, listeners, hl);
		if (hold == listeners) {
			return false;
		}

		// 1: remove hubListener 
		if (hl.getLocation() == HubListener.InsertLocation.LAST) {
			cntLast--;
		}

		if (alListenerInfo == null) {
			return true;
		}

		// 2: remove any listenerInfo
		ListenerInfo li = null;
		for (ListenerInfo lix : alListenerInfo) {
			if (lix.hl != hl) {
				continue;
			}
			li = lix;
			break;
		}

		if (li == null) {
			return true; // none required
		}
		alListenerInfo.remove(li);

		// 3: remove any hlExtra properties that this hl had for the hlExtra propertyChange events
		if (hlExtra != null && hsExtraProperties != null && li.alExtraListenerProperties != null) {
			// see if this is the only listener for each of the extra properties
			for (String p : li.alExtraListenerProperties) {
				boolean b = false;
				// check other listenerInfo
				for (ListenerInfo lix : alListenerInfo) {
					if (lix.hl == hl) {
						continue;
					}
					if (lix.alExtraListenerProperties == null) {
						continue;
					}
					if (lix.alExtraListenerProperties.contains(p.toUpperCase())) {
						b = true;
						break;
					}
				}
				if (!b) {
					// dont listen to it anymore
					hsExtraProperties.remove(p.toUpperCase());
				}
			}
		}

		// 4: check to see if the hlExtra is still needed
		if (hlExtra != null && hsExtraProperties != null && hsExtraProperties.size() == 0) {
			HubListener hlx = hlExtra;
			hlExtra = null;
			_removeListener(hlx);
		}

		// 5: check if any of the triggers can be removed
		if (li.alTrigger != null) {
			// see if this is the last listener for a trigger
			for (OATrigger t : li.alTrigger) {
				boolean b = false;
				for (ListenerInfo lix : alListenerInfo) {
					if (lix.hl == hl) {
						continue;
					}
					if (lix.alTrigger == null) {
						continue;
					}
					if (lix.alTrigger.contains(t)) {
						b = true;
						break;
					}
				}
				if (!b) {
					OATriggerDelegate.removeTrigger(t);
					for (Map.Entry<String, OATrigger> me : hsTrigger.entrySet()) {
						if (me.getValue() == t) {
							hsTrigger.remove(me.getKey());
							break;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Ensures that all triggers created by this HubListenerTrigger are removed
	 * before garbage collection. Invokes OATriggerDelegate.removeTrigger for
	 * each stored trigger.
	 *
	 * @throws Throwable if finalization fails
	 */
	@Override
	protected void finalize() throws Throwable {
		if (hsTrigger != null) {
			for (OATrigger t : hsTrigger.values()) {
				OATriggerDelegate.removeTrigger(t);
			}
		}
		super.finalize();
	}
}
