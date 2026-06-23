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
package com.viaoa.hub.view;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Maintains a combined {@link Hub} whose content is the union of multiple source
 * Hubs. Acts as a light wrapper around one or more {@link HubMerger} instances,
 * exposing a single merged Hub that tracks all changes bidirectionally.
 *
 * <p>Designed for UI or service layers needing a unified view of several
 * collections that share the same object type.</p>
 *
 * <p><b>Behavior</b>:
 * <ul>
 *   <li>Adds listeners to each source Hub to relay add/remove/update events.</li>
 *   <li>Automatically prevents duplicates based on object identity.</li>
 *   <li>Supports optional active-object propagation to keep selection consistent.</li>
 * </ul>
 */
public class HubCombined<TYPE extends OAObject> {
	private static Logger LOG = Logger.getLogger(HubCombined.class.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * The master hub that receives and maintains the combined contents
	 * of all tracked source hubs.
	 */
	protected final Hub<TYPE> hubMaster;

	/**
	 * Collection of source hubs whose objects are merged into the
	 * master hub.
	 */
	protected final ArrayList<Hub<TYPE>> alHub = new ArrayList<>();
	
	/**
	 * List of listeners attached to each tracked source hub to relay
	 * changes to the master hub.
	 */
	protected ArrayList<HubListener<TYPE>> alHubListener;
	
	/**
	 * Listener attached to the master hub to propagate its changes
	 * back to source hubs when appropriate.
	 */
	protected final HubListener<TYPE> hlMaster;
	
	/**
	 * Holds the first hub added to the combined collection, used for
	 * handling add-back behavior and active-object propagation logic.
	 */
	protected Hub<TYPE> hubFirst;
	
	/**
	 * Flag indicating whether the master hub is currently being
	 * updated, used to prevent recursive or duplicate event handling.
	 */
	protected volatile boolean bUpdatingMasterHub;

	/**
	 * Creates a HubCombined instance using the specified master hub and an optional
	 * list of source hubs. Each provided hub is added and tracked, and a master
	 * listener is created to keep the master hub synchronized with all sources.
	 *
	 * @param hubMaster the master hub receiving the combined contents
	 * @param hubs      optional list of source hubs whose objects form the union
	 */
	public HubCombined(final Hub<TYPE> hubMaster, final Hub<TYPE>... hubs) {
		this.hubMaster = hubMaster;

		if (hubs != null) {
			for (Hub h : hubs) {
				add(h);
			}
		}

		hlMaster = new HubListenerAdapter<TYPE>(this, "HubCombined.hubMaster", "") {
			@Override
			public void afterAdd(HubEvent<TYPE> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				TYPE objx = e.getObject();
				boolean bUsed = true;
				for (Hub h : hubs) {
					if (h.contains(objx)) {
						bUsed = false;
						break;
					}
				}
				if (bUsed && hubFirst != null) {
					if (hubFirst.isValid()) {
						hubFirst.add(e.getObject());
					} else {
						//int xx = 4;
						//xx++;
					}
				}
			}

			@Override
			public void afterInsert(HubEvent<TYPE> e) {
				afterAdd(e);
			}

			@Override
			public void afterRemove(HubEvent<TYPE> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				TYPE obj = e.getObject();
				for (Hub<TYPE> h : alHub) {
					h.remove(obj);
				}
			}

			@Override
			public void beforeRemoveAll(HubEvent<TYPE> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				for (TYPE obj : hubMaster) {
					for (Hub<TYPE> h : alHub) {
						h.remove(obj);
					}
				}
			}
		};
		hubMaster.addHubListener(hlMaster);
	}

	/**
	 * Returns the master hub that contains the combined contents of all added hubs.
	 *
	 * @return the master hub
	 */
	public Hub<TYPE> getMasterHub() {
		return hubMaster;
	}

	/**
	 * Removes all hub listeners from the tracked hubs and the master hub, clears
	 * internal lists, and stops all synchronization behavior.
	 */
	public void close() {
		int i = 0;
		if (alHubListener != null) {
			for (Hub h : alHub) {
				h.removeHubListener(alHubListener.get(i++));
			}
			alHubListener.clear();
		}
		alHub.clear();
		if (hlMaster != null) {
			hubMaster.removeHubListener(hlMaster);
		}
	}

	/**
	 * Returns the list of source hubs whose contents are merged into the master hub.
	 *
	 * @return list of tracked hubs
	 */
	public ArrayList<Hub<TYPE>> getHubs() {
		return alHub;
	}

	/**
	 * Creates a temporary hub that monitors the specified object's property and
	 * adds the property's value to this combined hub. A listener is added so that
	 * property changes update the combined content dynamically.
	 *
	 * @param object   the object whose property should be tracked
	 * @param property the property name whose value is added to the combined hub
	 */
	public void add(final TYPE object, final String property) {
		if (object == null) {
			return;
		}
		if (OAString.isEmpty(property)) {
			return;
		}

		final Hub<TYPE> hubNew = new Hub<TYPE>();
		TYPE obj = (TYPE) object.getProperty(property);
		if (obj != null) {
			hubNew.add(obj);
		}
		add(hubNew);

		final Hub<TYPE> hub = new Hub();
		hub.add(object);

		HubListener hl = new HubListenerAdapter(this, "HubCombined.object", "") {
			@Override
			public void afterPropertyChange(HubEvent e) {
				if (!property.equalsIgnoreCase(e.getPropertyName())) {
					return;
				}
				TYPE objn = (TYPE) e.getNewValue();
				Object objo = e.getOldValue();
				if (objn == objo) {
					return;
				}

				hub.clear();
				if (objn != null) {
					hub.add(objn);
				}
			}
		};
		hub.addHubListener(hl, property);
	}

	/**
	 * Adds a source hub to the combined set, establishes a listener to synchronize
	 * adds, removes, and list resets, and merges all existing objects from the
	 * source hub into the master hub.
	 *
	 * @param hub the hub to add to the combined collection
	 */
	public void add(Hub<TYPE> hub) {
		if (alHub.size() == 0) {
			hubFirst = hub;
		}
		alHub.add(hub);

		HubListener hl = new HubListenerAdapter<TYPE>() {
			@Override
			public void afterAdd(HubEvent<TYPE> e) {
				try {
					bUpdatingMasterHub = true;
					hubMaster.add(e.getObject());
				} finally {
					bUpdatingMasterHub = false;
				}
			}

			@Override
			public void afterInsert(HubEvent<TYPE> e) {
				afterAdd(e);
			}

			@Override
			public void afterRemove(HubEvent<TYPE> e) {
				TYPE obj = e.getObject();
				boolean bUsed = false;
				for (Hub<TYPE> hx : alHub) {
					if (hx.contains(obj)) {
						bUsed = true;
						break;
					}
				}
				if (!bUsed) {
					try {
						bUpdatingMasterHub = true;
						hubMaster.remove(obj);
					} finally {
						bUpdatingMasterHub = false;
					}
				}
			}

			@Override
			public void afterRemoveAll(HubEvent<TYPE> e) {
				onNewList(e);
			}

			@Override
			public void onNewList(HubEvent<TYPE> e) {
				final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
				boolean bWasLoading = srvcOAThreadLocal.setLoading(true);
				try {
					bUpdatingMasterHub = true;
					for (Object obj : hubMaster) {
						boolean bUsed = false;
						for (Hub<TYPE> hx : alHub) {
							if (hx.contains(obj)) {
								bUsed = true;
								break;
							}
						}
						if (!bUsed) {
							hubMaster.remove(obj);
						}
					}
					for (TYPE obj : e.getHub()) {
						hubMaster.add(obj);
					}
				} finally {
					bUpdatingMasterHub = false;
					srvcOAThreadLocal.setLoading(bWasLoading);
				}
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				og.internal().hubs().events().fireOnNewListEvent(hubMaster, true);
			}
		};
		hub.addHubListener(hl);
		if (alHubListener == null) {
			alHubListener = new ArrayList<HubListener<TYPE>>();
		}
		alHubListener.add(hl);

		for (TYPE obj : hub) {
			hubMaster.add(obj);
		}
	}

	/**
	 * Synchronizes the master hub with all tracked source hubs by removing objects
	 * no longer present in any source hub and adding all objects currently found in
	 * the sources.
	 */
	public void refresh() {
		for (Object obj : hubMaster) {
			boolean bUsed = false;
			for (Hub<TYPE> hx : alHub) {
				if (hx.contains(obj)) {
					bUsed = true;
					break;
				}
			}
			if (!bUsed) {
				hubMaster.remove(obj);
			}
		}
		for (Hub<TYPE> hx : alHub) {
			for (TYPE obj : hx) {
				hubMaster.add(obj);
			}
		}
	}

}
