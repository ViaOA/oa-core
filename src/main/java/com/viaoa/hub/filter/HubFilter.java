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
package com.viaoa.hub.filter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.filter.OABetweenFilter;
import com.viaoa.filter.OABetweenOrEqualFilter;
import com.viaoa.filter.OABlockFilter;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAFalseFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.filter.OAGreaterFilter;
import com.viaoa.filter.OAGreaterOrEqualFilter;
import com.viaoa.filter.OALessFilter;
import com.viaoa.filter.OALessOrEqualFilter;
import com.viaoa.filter.OALikeFilter;
import com.viaoa.filter.OANotEqualFilter;
import com.viaoa.filter.OANotLikeFilter;
import com.viaoa.filter.OATrueFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.hub.HubAddRemoveService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubData;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubInternalBridge;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Base filter component used by a {@link Hub} to include or exclude objects
 * dynamically. Can be subclassed or composed to implement domain-specific filters.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 *   <li>Maintain inclusion rules that evaluate on Hub events (add/remove/AO).</li>
 *   <li>Expose {@link #isUsed(Object)} for real-time membership tests.</li>
 *   <li>Support filter chaining and event-driven refresh via HubListener hooks.</li>
 * </ul>
 *
 * <p>Common derivatives: search filters, permission filters, or computed subsets.</p>
 */
public class HubFilter<TYPE extends OAObject> extends HubListenerAdapter<TYPE> implements java.io.Serializable, OAFilter<TYPE> {
	private static Logger LOG = Logger.getLogger(HubFilter.class.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * The master Hub containing the full set of objects that the filter
	 * evaluates and selectively includes in the target Hub.
	 */
	protected Hub<TYPE> hubMaster;
	
	/**
	 * Weak reference to the filtered Hub, allowing it to be garbage-collected
	 * without preventing cleanup of this filter.
	 */
	protected WeakReference<Hub<TYPE>> weakHub;
	
	/**
	 * Indicates whether active-object changes should be shared between
	 * the master Hub and the filtered Hub.
	 */
	private boolean bShareAO;
	
	/**
	 * Flag indicating whether the filter has been closed and should no
	 * longer process events or updates.
	 */
	private volatile boolean bClosed;
	
	/**
	 * When true, the filter is intended for server-side filtering only;
	 * client-side filtering still receives updates.
	 */
	protected boolean bServerSideOnly;

	/**
	 * Counter used to generate unique internal names for dependent-property
	 * listener triggers.
	 */
	private static AtomicInteger aiUniqueNameCnt = new AtomicInteger();
	
	/**
	 * Name assigned to a calculated dependent property used for monitoring
	 * multi-level or calculated property changes.
	 */
	private volatile String calcDependentPropertyName;
	
	/**
	 * List of dependent property names or paths that cause the filter to
	 * refresh when they change.
	 */
	private String[] dependentPropertyNames;
	
	/**
	 * Listener registered to monitor dependent properties on the master Hub.
	 */
	private HubListener hlDependentProperties;

	/**
	 * Debug flag enabling additional diagnostics when true.
	 */
	public boolean DEBUG;
	
	/**
	 * Listener that monitors the master Hub for changes relevant to filtering.
	 */
	private HubListenerAdapter<TYPE> hlHubMaster;
	
	/**
	 * Indicates whether a new-list event is being processed to prevent
	 * recursive or redundant event handling.
	 */
	private volatile boolean bNewListFlag;

	/**
	 * Counter used to track when the filtered Hub is being cleared,
	 * preventing updates during that process.
	 */
	private final AtomicInteger aiClearing = new AtomicInteger();
	
	/**
	 * Counter indicating whether the filter is currently processing updates,
	 * used to suppress reentrant operations.
	 */
	private final AtomicInteger aiUpdating = new AtomicInteger();

	
	/**
	 * Listener monitoring changes in the Hub linked to the filtered Hub.
	 */
	private HubListener linkHubListener;
	
	/**
	 * The Hub that the filtered Hub is linked to, used for temporary
	 * additions based on link relationships.
	 */
	private Hub<?> hubLink;
	
	/**
	 * Temporary object used to hold the current linkHub value when the
	 * filtered Hub must include it regardless of filter rules.
	 */
	protected OAObject objTemp;
	
	/**
	 * Indicates whether the filter should refresh when the linked Hub’s
	 * active object changes.
	 */
	private boolean bRefreshOnLinkChange;
	
	/**
	 * Internal flag used to prevent recursive active-object updates
	 * between master and filtered Hubs.
	 */
	private volatile boolean bIgnoreSettingAO;

	/**
	 * Collection of filters applied to determine whether objects are
	 * included in the filtered Hub.
	 */
	private ArrayList<OAFilter<TYPE>> alFilters;

	/**
	 * Marks the starting index for constructing a block filter from
	 * multiple sequential filters.
	 */
	private int iBlockPos = -1;
	
	/**
	 * Tracks the initialization sequence to interrupt and restart
	 * initialization safely when required.
	 */
	private AtomicInteger aiInitializeCount = new AtomicInteger();
	
	
	/**
	 * Creates a new HubFilter using the supplied master Hub and target Hub.
	 *
	 * @param hubMaster the Hub containing the complete set of objects
	 * @param hub       the Hub that will contain the filtered objects
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub) {
		this(hubMaster, hub, false, false, null, null);
	}

	/**
	 * Creates a HubFilter using the supplied master Hub, target Hub, and filter.
	 *
	 * @param hubMaster the Hub with the complete list of objects
	 * @param hub       the Hub that will contain the filtered objects
	 * @param filter    an initial filter to apply
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, OAFilter<TYPE> filter) {
		this(hubMaster, hub, false, false, filter, null);
	}

	/**
	 * Creates a HubFilter with an initial filter and dependent property paths.
	 *
	 * @param hubMaster               the Hub containing all objects
	 * @param hub                     the Hub that will receive filtered objects
	 * @param filter                  the filter to apply
	 * @param dependentPropertyPaths  property paths whose changes trigger refresh
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, OAFilter<TYPE> filter, String... dependentPropertyPaths) {
		this(hubMaster, hub, false, false, filter, dependentPropertyPaths);
	}

	/**
	 * Creates a HubFilter and specifies whether to share active objects between
	 * the master Hub and the filtered Hub.
	 *
	 * @param hubMaster the master Hub
	 * @param hub       the filtered Hub
	 * @param bShareAO  flag indicating whether active objects are shared
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO) {
		this(hubMaster, hub, bShareAO, false, null, null);
	}

	/**
	 * Creates a HubFilter with active-object sharing and an initial filter.
	 *
	 * @param hubMaster the master Hub
	 * @param hub       the filtered Hub
	 * @param bShareAO  true to share active objects
	 * @param filter    an initial filter
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, OAFilter<TYPE> filter) {
		this(hubMaster, hub, bShareAO, false, filter, null);
	}

	/**
	 * Creates a HubFilter with full configuration options.
	 *
	 * @param hubMaster             the master Hub
	 * @param hub                   the filtered Hub
	 * @param bShareAO              true to share active objects
	 * @param bRefreshOnLinkChange  true to refresh when link Hub AO changes
	 * @param filter                an initial filter
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, boolean bRefreshOnLinkChange, OAFilter<TYPE> filter) {
		this(hubMaster, hub, bShareAO, bRefreshOnLinkChange, filter, null);
	}

	/**
	 * Creates a HubFilter with optional active-object sharing, filter, and
	 * dependent property paths.
	 *
	 * @param hubMaster              the master Hub
	 * @param hub                    the filtered Hub
	 * @param bShareAO               true to share active objects
	 * @param filter                 initial filter
	 * @param dependentPropertyPaths property paths monitored for changes
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, OAFilter<TYPE> filter, String... dependentPropertyPaths) {
		this(hubMaster, hub, bShareAO, false, filter, dependentPropertyPaths);
	}

	/**
	 * Creates a HubFilter with dependent property paths.
	 *
	 * @param hubMaster              the master Hub
	 * @param hub                    the filtered Hub
	 * @param dependentPropertyPaths property paths to monitor
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, String... dependentPropertyPaths) {
		this(hubMaster, hub, false, false, null, dependentPropertyPaths);
	}

	/**
	 * Creates a HubFilter with active-object sharing and dependent property paths.
	 *
	 * @param hubMaster              the master Hub
	 * @param hub                    the filtered Hub
	 * @param bShareAO               true to share active objects
	 * @param dependentPropertyPaths property paths monitored for changes
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, String... dependentPropertyPaths) {
		this(hubMaster, hub, bShareAO, false, null, dependentPropertyPaths);
	}

	/**
	 * Creates a HubFilter with full configuration except for an initial OAFilter.
	 *
	 * @param hubMaster              the master Hub
	 * @param hub                    the filtered Hub
	 * @param bShareAO               true to share active objects
	 * @param bRefreshOnLinkChange   true to refresh when link Hub AO changes
	 * @param dependentPropertyPaths property paths monitored for changes
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, boolean bRefreshOnLinkChange, String... dependentPropertyPaths) {
		this(hubMaster, hub, bShareAO, bRefreshOnLinkChange, null, dependentPropertyPaths);
	}

	/**
	 * Constructs a HubFilter with complete configuration, including optional
	 * active-object sharing, refresh behavior, filter, and dependent properties.
	 *
	 * @param hubMaster              the master Hub
	 * @param hub                    the filtered Hub
	 * @param bShareAO               true to share active objects
	 * @param bRefreshOnLinkChange   true to refresh on link Hub AO change
	 * @param filter                 an initial filter
	 * @param dependentPropertyPaths property paths monitored for changes
	 */
	public HubFilter(Hub<TYPE> hubMaster, Hub<TYPE> hub, boolean bShareAO, boolean bRefreshOnLinkChange, OAFilter<TYPE> filter,
			String... dependentPropertyPaths) {
		// note: bObjectCache will allow hubMaster to be null, which will then use the oaObjectCache
		if (hubMaster == null) {
			throw new IllegalArgumentException("hubMaster can not be null");
		}
		if (hub == null) { // 20131129 hub can now be null, used by Triggers
			// throw new IllegalArgumentException("hub can not be null");
		}

		this.hubMaster = hubMaster;
		if (hub != null) {
			this.weakHub = new WeakReference(hub);
		}
		this.bShareAO = bShareAO;
		this.bRefreshOnLinkChange = bRefreshOnLinkChange;
		if (filter != null) {
			alFilters = new ArrayList<OAFilter<TYPE>>();
			alFilters.add(filter);
		}
		setup();
		if (dependentPropertyPaths != null) {
			for (String s : dependentPropertyPaths) {
				addProperty(s);
			}
		}
	}

	/**
	 * Returns the filtered Hub referenced by this filter, or null if the weak
	 * reference has been cleared. If cleared, this filter is also closed.
	 *
	 * @return the filtered Hub, or null if no longer available
	 */
	public Hub<TYPE> getHub() {
		if (weakHub == null) {
			return null;
		}
		Hub<TYPE> h = weakHub.get();
		if (h == null) {
			close();
		}
		return h;
	}

	/**
	 * Marks the filter as server-side-only. When true, changes made on an
	 * OAClientThread will still be published to clients.
	 *
	 * @param b true if used only on the server
	 */
	public void setServerSideOnly(boolean b) {
		bServerSideOnly = b;
	}

	/**
	 * Ensures the filter is closed during finalization.
	 *
	 * @throws Throwable if the superclass finalize throws an exception
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	/**
	 * Closes the filter, removes all listeners, and prevents further updates.
	 * Subsequent calls have no effect.
	 */
	public void close() {
		// need to make sure that no more events get processed
		if (bClosed) {
			return;
		}
		this.bClosed = true;

		Hub<TYPE> hub = getHub();
		if (hub != null) {
			hub.removeHubListener(this);
		}
		if (hubMaster != null && hlHubMaster != null) {
			hubMaster.removeHubListener(hlHubMaster);
			hlHubMaster = null;
		}
		if (hubLink != null && linkHubListener != null) {
			hubLink.removeHubListener(linkHubListener);
			linkHubListener = null;
		}
		if (hlDependentProperties != null) {
			if (hubMaster != null) {
				hubMaster.removeHubListener(hlDependentProperties);
			}
			hlDependentProperties = null;
		}
		//qqqqqqqqqqq
		// todo remove any created triggers
	}

	/**
	 * Adds a dependent property and triggers a refresh.
	 *
	 * @param prop the property name or path
	 */
	public void addDependentProperty(String prop) {
		addDependentProperty(prop, true);
	}

	/**
	 * Adds a dependent property and optionally refreshes the filtered Hub.
	 *
	 * @param prop     the property name or path
	 * @param bRefesh  true to refresh after adding
	 */
	public void addDependentProperty(String prop, boolean bRefesh) {
		if (bClosed) {
			return;
		}
		_addProperty(prop, bRefesh);
	}

	/**
	 * Adds a trigger for the specified property path.
	 *
	 * @param propPath the property path to monitor
	 */
	public void addTrigger(String propPath) {
		this.addTrigger(propPath, false);
	}

	/**
	 * Adds a trigger for a property path with optional background execution.
	 *
	 * @param propPath            the property path
	 * @param useBackgroundThread true to execute in background
	 */
	public void addTrigger(String propPath, boolean useBackgroundThread) {
		final String name = "HubFilter" + (aiUniqueNameCnt.incrementAndGet());
		hubMaster.addTriggerListener(new HubListenerAdapter<TYPE>() {
			@Override
			public void afterPropertyChange(HubEvent<TYPE> e) {
				if (!name.equalsIgnoreCase(e.getPropertyName())) {
					return;
				}
				update(e.getObject(), false);
			}
		}, name, propPath, useBackgroundThread);
	}

	/**
	 * Adds a dependent property on an OAObject. A refresh is triggered when the
	 * property changes.
	 *
	 * @param obj  the OAObject to monitor
	 * @param prop the property name
	 */
	public void addDependentProperty(OAObject obj, String prop) {
		if (bClosed) {
			return;
		}
		if (prop == null || prop.length() == 0) {
			return;
		}
		if (obj == null) {
			return;
		}
		Hub h = new Hub(obj);
		addDependentProperty(h, prop);
	}

	/**
	 * Adds a dependent property based on the specified Hub. A refresh is triggered
	 * when the Hub's active object changes.
	 *
	 * @param hub the Hub to monitor
	 */
	public <T extends OAObject> void addDependentProperty(Hub<T> hub) {
		if (bClosed) {
			return;
		}
		if (hub == null) {
			return;
		}

		//todo: need to all remove hl on close
		hub.addHubListener(new HubListenerAdapter<T>() {
			@Override
			public void afterChangeActiveObject(HubEvent<T> e) {
				HubFilter.this.refresh();
			}
		});
	}

	/**
	 * Adds a dependent property from the specified Hub. A refresh is triggered
	 * when the property or the Hub's active object changes.
	 *
	 * @param hub  the Hub to monitor
	 * @param prop the property name or path
	 */
	public void addDependentProperty(final Hub<?> hub, String prop) {
		addDependentProperty(hub, prop, true);
	}

	/**
	 * Adds a dependent property from the specified Hub with optional active-object
	 * filtering. A refresh is triggered when the monitored property changes.
	 *
	 * @param hub               the Hub to monitor
	 * @param prop              the property name or path
	 * @param bActiveObjectOnly true to monitor only the active object
	 */
	public <T extends OAObject> void addDependentProperty(final Hub<T> hub, String prop, final boolean bActiveObjectOnly) {
		if (bClosed) {
			return;
		}
		if (prop == null || prop.length() == 0) {
			return;
		}
		if (hub == null) {
			return;
		}

		String s;
		if (prop.indexOf('.') < 0) {
			s = prop;
		} else {
			s = "HubFilter" + (aiUniqueNameCnt.incrementAndGet());
		}

		final String propName = s;

		//todo:  need to add remove hl on close
		HubListener<T> hl = new HubListenerAdapter<T>() {
			@Override
			public void afterChangeActiveObject(HubEvent<T> e) {
				HubFilter.this.refresh();
			}

			@Override
			public void afterPropertyChange(HubEvent<T> e) {
				if (bActiveObjectOnly && e.getObject() != hub.getAO()) {
					return;
				}
				if (propName.equalsIgnoreCase(e.getPropertyName())) {
					HubFilter.this.refresh();
				}
			}
		};

		if (prop.indexOf('.') < 0) {
			hub.addHubListener(hl, prop, bActiveObjectOnly);
		} else {
			hub.addHubListener(hl, propName, new String[] { prop }, bActiveObjectOnly);
		}
	}

	/**
	 * Adds a dependent property. Deprecated—use addDependentProperty instead.
	 *
	 * @param prop the property to monitor
	 * @deprecated use {@link #addDependentProperty(String)} instead
	 */
	public void addProperty(String prop) {
		_addProperty(prop, true);
	}

	/**
	 * Adds a property name to the list of dependent properties and optionally
	 * triggers a refresh. Updates internal listeners used to monitor changes
	 * to these properties on the master Hub. If the filter is closed or the
	 * property name is null/empty, this method returns immediately.
	 *
	 * @param prop      the property name or path to add
	 * @param bRefresh  true to trigger a refresh after adding the property
	 */
	private void _addProperty(final String prop, final boolean bRefresh) {
		if (bClosed) {
			return;
		}
		if (prop == null || prop.length() == 0) {
			return;
		}

		if (hubMaster != null && hlDependentProperties != null) {
			hubMaster.removeHubListener(hlDependentProperties);
			hlDependentProperties = null;
		}

		dependentPropertyNames = (String[]) OAArray.add(String.class, dependentPropertyNames, prop);

		if (calcDependentPropertyName == null) {
			boolean b = (prop.indexOf(".") >= 0);
			if (!b) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubMaster.getObjectClass());
				OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(hubMaster.getObjectClass());
				String[] calcProps = null;
				for (OACalcInfo ci : oi.getCalcInfos()) {
					if (ci.getName().equalsIgnoreCase(prop)) {
						b = true;
					}
				}
			}
			if (b) {
				calcDependentPropertyName = "HubFilter" + (aiUniqueNameCnt.incrementAndGet());
			}
		}

		if (calcDependentPropertyName != null) {
			hlDependentProperties = new HubListenerAdapter();
			if (hubMaster != null) {
				hubMaster.addHubListener(hlDependentProperties, calcDependentPropertyName, dependentPropertyNames);
			}
		}

		if (bRefresh) {
			refresh();
		}
	}

	/**
	 * Determines whether the specified object should be included in the filtered
	 * Hub. All filters must return true for inclusion.
	 *
	 * @param object the object to evaluate
	 * @return true if the object passes all filters, false otherwise
	 */
	public boolean isUsed(TYPE object) {
		if (alFilters == null) {
			return true;
		}

		for (OAFilter f : alFilters) {
			boolean b = f.isUsed(object);
			if (!b) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the object to insert into the filtered Hub when isUsed() is true.
	 * Subclasses may override to substitute a different object.
	 *
	 * @param object the original object
	 * @return the object to add to the Hub
	 */
	public TYPE getObject(TYPE object) {
		return object;
	}

	/**
	 * Returns the HubListener used to monitor the master Hub. Creates it on first
	 * access and configures update behavior for property changes, inserts, removes,
	 * active-object changes, and sorting.
	 *
	 * @return the master Hub listener
	 */
	protected HubListenerAdapter<TYPE> getMasterHubListener() {
		if (hlHubMaster != null) {
			return hlHubMaster;
		}

		hlHubMaster = new HubListenerAdapter<TYPE>(this, "HubFilter.hubMaster", "") {
			/** HubListener interface method, used to update filter. */
			public @Override void afterPropertyChange(HubEvent<TYPE> e) {
				if (bClosed) {
					return;
				}

				String propName = e.getPropertyName();
				if (propName == null) {
					return;
				}

				if (calcDependentPropertyName != null) {
					if (!calcDependentPropertyName.equalsIgnoreCase(propName)) {
						return;
					}
				} else {
					if (dependentPropertyNames == null) {
						return;
					}
					boolean b = false;
					for (String s : dependentPropertyNames) {
						if (s.equalsIgnoreCase(propName)) {
							b = true;
						}
					}
					if (!b) {
						return;
					}
				}
				update(e.getObject(), false);
			}

			/** HubListener interface method, used to update filter. */
			public @Override void afterInsert(HubEvent<TYPE> e) {
				afterAdd(e);
			}

			/** HubListener interface method, used to update filter. */
			public @Override void afterAdd(HubEvent<TYPE> e) {
				if (bClosed) {
					return;
				}

				// 20160105 removed isLoading check since OAObjectCacheFilter would work when a new object is created.
				// if (hubMaster == null || !hubMaster.isLoading()) {
				Hub<TYPE> hub = getHub();
				if (hub == null || !hub.contains(e.getObject())) {
					if (hubMaster == null || hubMaster.contains(e.getObject())) {
						try {
							aiUpdating.incrementAndGet();
							update(e.getObject(), false);
						} finally {
							aiUpdating.decrementAndGet();
						}
					}
				}
			}

			/** HubListener interface method, used to update filter. */
			public @Override void afterRemove(HubEvent<TYPE> e) {
				if (bClosed) {
					return;
				}
				final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
				boolean bWas = false;
				try {
					if (bServerSideOnly) {
						bWas = srvcOAThreadLocal.getSendSyncMessages();
						srvcOAThreadLocal.setSendSyncMessages(true);
					}
					if (hubMaster == null || !hubMaster.contains(e.getObject())) {
						removeObject(getObject(e.getObject()));
					}
				} finally {
					if (bServerSideOnly) {
						srvcOAThreadLocal.setSendSyncMessages(bWas);
					}
				}
			}

			/** HubListener interface method, used to update filter. */
			public @Override void onNewList(HubEvent<TYPE> e) {
				if (bClosed || bNewListFlag) {
					return;
				}
				afterChangeActiveObject(null);
				initialize();
			}

			/** HubListener interface method, used to update filter. */
			public @Override void afterSort(HubEvent<TYPE> e) {
				if (bClosed) {
					return;
				}
				if (hubMaster != null) {
					onNewList(e);
				}
			}

			public void afterChangeActiveObject(HubEvent<TYPE> e) {
				if (!bShareAO || hubMaster == null) {
					return;
				}
				Hub<TYPE> hub = getHub();
				if (hub == null) {
					return;
				}

				Object obj = HubFilter.this.hubMaster.getAO();
				if (obj != null && !hub.contains(obj)) {
					obj = null;
				}
				bIgnoreSettingAO = true;
				hub.setAO(obj);
				bIgnoreSettingAO = false;
			}
		};
		return hlHubMaster;
	}

	/**
	 * Returns the name of the calculated dependent property, if applicable.
	 *
	 * @return the calculated dependent property name, or null if none
	 */
	public String getCalcPropertyName() {
		return calcDependentPropertyName;
	}

	/**
	 * Returns the array of dependent property names.
	 *
	 * @return array of dependent property names, or null if none
	 */
	public String[] getDependentPropertyNames() {
		return dependentPropertyNames;
	}

	/**
	 * Sets up the filter, attaches listeners to the master Hub, initializes the
	 * filtered Hub, and configures link-Hub monitoring.
	 */
	protected void setup() {
		if (bClosed) {
			return;
		}
		Hub<TYPE> hub = getHub();
		hubMaster.addHubListener(getMasterHubListener());

		// this will call initialize
		getMasterHubListener().onNewList(null);

		if (hub != null) {
			hub.addHubListener(this);
		}
		setupLinkHubListener();
	}


	/**
	 * Configures a listener on the Hub linked to the filtered Hub. Updates the
	 * filter when the link Hub's active object changes.
	 */
	protected void setupLinkHubListener() {
		Hub<TYPE> hub = getHub();
		if (hub == null) {
			return;
		}
		if (hubLink != null) {
			hubLink.removeHubListener(linkHubListener);
		}
		hubLink = hub.getLinkHub(true);
		if (hubLink == null) {
			return;
		}

		linkHubListener = new HubListenerAdapter() {
			public @Override void afterChangeActiveObject(HubEvent evt) {
				Hub<TYPE> hub = getHub();
				if (hub == null || bClosed) {
					return;
				}
				if (objTemp != null) {
					OAObject objx = hubLink.getAO();
					if (objx != null) {
						final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
						objx = (TYPE) og.hubsInternal().callHubLinkGetPropertyValueInLinkedToHub(hub, objx);
					}
					if (objx != objTemp) {
						objTemp = getObject((TYPE) objTemp);
						try {
							aiUpdating.incrementAndGet();
							if (objTemp != null) {
								removeObject((TYPE) objTemp);
							}
						} finally {
							aiUpdating.decrementAndGet();
						}
						objTemp = null;
					}
				}
				if (bRefreshOnLinkChange) {
					refresh(); // 20110930 need to refresh since the linkTo hub has changed
				}
				Object obj = hubLink.getAO();
				if (objTemp == null && obj != null) {
					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
					obj = og.hubsInternal().callHubLinkGetPropertyValueInLinkedToHub(hub, (OAObject) obj);
					if (obj instanceof OAObject) {
						if (!hub.contains(obj)) {
							try {
								aiUpdating.incrementAndGet();
								objTemp = (OAObject) obj;
								addObject((TYPE) obj, false);
								hub.setAO(obj);
							} finally {
								aiUpdating.decrementAndGet();
							}
						}
					}
				}
			}
		};
		linkHubListener.afterChangeActiveObject(null);
		hubLink.addHubListener(linkHubListener);
	}


	/**
	 * Sets whether the filter should be refreshed when the linked Hub's active
	 * object changes.
	 *
	 * @param b true to refresh on link Hub AO changes
	 */
	public void setRefreshOnLinkChange(boolean b) {
		bRefreshOnLinkChange = b;
	}

	/**
	 * Updates the filtered Hub for a single object based on filter rules and
	 * initialization state. Adds or removes the object as needed.
	 *
	 * @param obj            the object to evaluate
	 * @param bIsInitialzing true if part of initialization
	 */
	protected void update(TYPE obj, boolean bIsInitialzing) {
		if (bClosed) {
			return;
		}
		Hub<TYPE> hub = getHub();
		if (aiClearing.get() != 0) {
			return;
		}
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
		boolean bWas = false;
		try {
			
			if (bServerSideOnly) { // 20120425
				bWas = srvcOAThreadLocal.getSendSyncMessages();
				srvcOAThreadLocal.setSendSyncMessages(true); // so that events will go out, even if OAClientThread
			}
			aiUpdating.incrementAndGet();
			obj = getObject(obj);
			if (obj != null) {
				if (hubMaster == null || hubMaster.getObjectClass().isAssignableFrom(obj.getClass())) {
					if (isUsed(obj)) {
					    // 20231109 added bIsInitialzing
						if (hub == null || bIsInitialzing || !hub.contains(obj)) {
							if (obj == objTemp) {
								objTemp = null;
							}
							if (hubMaster == null || bIsInitialzing || hubMaster.contains(obj)) {
								addObject(obj, bIsInitialzing);
							}
						}
					} else {
						// 2004/08/07 see if object is used by AO in HubLink
						if (hubLink != null) {
							OAObject objx = hubLink.getAO();
							if (objx != null) {
								final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
								objx = (TYPE) og.hubsInternal().callHubLinkGetPropertyValueInLinkedToHub(hub, objx);
								objx = getObject((TYPE) objx);
								if (obj == objx) {
									if (obj != objTemp) {
										if (objTemp != null) {
											if (!isUsed((TYPE) objTemp)) {
												removeObject((TYPE) objTemp);
											}
										}
										objTemp = obj;
										if (hubMaster == null || hubMaster.contains(obj)) {
											addObject(obj, bIsInitialzing);
										}
									}
									obj = null;
								}
							}
						}
						if (obj != null) {
							removeObject(obj);
						}
					}
				}
			}
		} finally {
			aiUpdating.decrementAndGet();
			if (bServerSideOnly) {
				srvcOAThreadLocal.setSendSyncMessages(bWas); 
			}
		}
	}

	/**
	 * Re-evaluates all objects in the master Hub and rebuilds the filtered Hub.
	 */
	public void refresh() {
		if (bClosed) {
			return;
		}
		initialize();
	}

	/**
	 * Re-evaluates a single object and adds or removes it from the filtered Hub
	 * accordingly.
	 *
	 * @param obj the object to refresh
	 */
	public void refresh(TYPE obj) {
		Hub<TYPE> hub = getHub();
		if (hub == null) {
			return;
		}
		boolean b = isUsed(obj);
		if (b) {
			obj = getObject(obj);
			if (obj != null && !hub.contains(obj)) {
				addObject(obj, true);
			}
		} else {
			obj = getObject(obj);
			if (obj != null) {
				removeObject(obj);
			}
		}
	}

	/**
	 * Called after initialization is complete. Default implementation does nothing.
	 */
	public void afterInitialize() {
	}

	/**
	 * Reinitializes the filtered Hub by clearing it, reloading all master objects,
	 * and triggering new-list events as appropriate.
	 */
	public void initialize() {
		final Hub<TYPE> hub = getHub();
		if (hub == null || bClosed) {
			return;
		}
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
		
		boolean bWas = false;
		if (bServerSideOnly) {
			bWas = srvcOAThreadLocal.getSendSyncMessages();
			srvcOAThreadLocal.setSendSyncMessages(true); // so that events will go out, even if OAClientThread
		}

		final int cnt = aiInitializeCount.incrementAndGet();

		boolean bCompleted = false;
		HubData hd = null;
		try {
			if (hub != null) {
				try {
					aiClearing.incrementAndGet();
					// clear needs to be called, so that each oaObj.weakHub[] will be updated correctly
					bIgnoreSettingAO = true;

					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
					og.hubsInternal().callHubAddRemoveClear(hub, false, false); // false:dont set AO to null,  false: dont send newList event

					objTemp = null;
				} finally {
					bIgnoreSettingAO = false;
					aiClearing.decrementAndGet();
				}
				HubInternalBridge faBridge = new HubInternalBridge();
				Hub.FriendAccess faHub = faBridge.getHubFriendAccess();
				hd = faHub.getHubData(hub);
			}

			try {
				final boolean bx = bServerSideOnly;
				boolean bWasLoading = false;
				try {
					if (!bx) {
						bWasLoading = srvcOAThreadLocal.setLoading(true);
					}
					bCompleted = _initialize(cnt);
				} finally {
					if (!bx) {
						srvcOAThreadLocal.setLoading(bWasLoading);
					}
				}
				if (hub != null && bCompleted) {
					bNewListFlag = true;
					if (!bServerSideOnly) {
						final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
						og.hubsInternal().callHubEventFireOnNewListEvent(hub, true);
					}
				}
			} finally {
				if (hub != null && bCompleted) {
					bNewListFlag = false;
				}
			}
		} finally {
			if (bServerSideOnly) {
				srvcOAThreadLocal.setSendSyncMessages(bWas);
			}
		}
		if (bCompleted) {
			afterInitialize();
		}
	}

	/**
	 * Performs the internal initialization pass for the filter by iterating
	 * through all objects in the master Hub and updating their inclusion
	 * status. Also evaluates link-to Hub values and sets the active object
	 * accordingly. Returns false if initialization is interrupted due to a
	 * changed initialization count or a closed filter.
	 *
	 * @param cnt the initialization sequence value used to detect interruption
	 * @return true if initialization completed successfully, false otherwise
	 */
	private boolean _initialize(final int cnt) {
		Hub<TYPE> hub = getHub();
		if (hub == null) {
			return false;
		}
		if (bClosed) {
			return false;
		}
		int i = 0;
		for (; hubMaster != null; i++) {
			TYPE obj = hubMaster.elementAt(i);
			if (obj == null) {
				break;
			}
			if (aiInitializeCount.get() != cnt) {
				return false;
			}
			update(obj, true);
		}

		// get linkToHub.prop value
		if (hubLink != null) {
			OAObject objx = hubLink.getAO();
			if (objx != null) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				objx = (TYPE) og.hubsInternal().callHubLinkGetPropertyValueInLinkedToHub(hub, objx);
				if (objx != null) {
					objx = getObject((TYPE) objx);
					if (objx != null && !hub.contains(objx)) {
						if (aiInitializeCount.get() != cnt) {
							return false;
						}
						addObject((TYPE) objx, true);
					}
				}
			}
			hub.setAO(objx);
			if (bShareAO && hubMaster != null) {
				if (hubMaster.getLinkHub(true) == null) {
					hubMaster.setAO(objx);
				}
			}
		}

		if (bShareAO && hubLink == null && hubMaster != null) {
			TYPE obj = hubMaster.getAO();
			if (obj != null && !hub.contains(obj)) {
				obj = null;
			}
			if (aiInitializeCount.get() != cnt) {
				return false;
			}
			hub.setAO(obj);
		}
		return true;
	}

	/**
	 * Returns the master Hub associated with this filter.
	 *
	 * @return the master Hub
	 */
	public Hub<TYPE> getMasterHub() {
		return this.hubMaster;
	}

	/**
	 * Adds an object to the filtered Hub. Subclasses may override to customize
	 * insertion behavior.
	 *
	 * @param obj            the object to add
	 * @param bIsInitialzing true if part of initialization
	 */
	protected void addObject(TYPE obj, boolean bIsInitialzing) {
		Hub<TYPE> hub = getHub();
		if (hub == null || bClosed) {
			return;
		}
	    
		// 20231109 faster way to add with calling contains
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		og.hubsInternal().callHubAddRemoveAdd(hub, obj, bIsInitialzing);
		// was:  hub.add(obj);
		
		if (bShareAO && hubMaster != null) {
			if (obj == hubMaster.getAO()) {
				bIgnoreSettingAO = true;
				hub.setAO(obj);
				bIgnoreSettingAO = false;
			}
		}
	}

	/**
	 * Removes an object from the filtered Hub, unless required temporarily by the
	 * link Hub relationship.
	 *
	 * @param obj the object to remove
	 */
	protected void removeObject(TYPE obj) {
		Hub<TYPE> hub = getHub();
		if (hub == null || bClosed) {
			return;
		}
		if (hubLink != null && aiUpdating.get() == 0) {
			// check to see if it is still needed by linkHub.linkProp and stored as objTemp
			OAObject objx = hubLink.getAO();
			if (objx != null) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				objx = (TYPE) og.hubsInternal().callHubLinkGetPropertyValueInLinkedToHub(hub, objx);
				if (objx == obj) {
					objTemp = obj; // dont remove yet
					return;
				}
			}
		}
		hub.remove(obj);
	}

	/**
	 * Returns whether active-object sharing is enabled.
	 *
	 * @return true if this filter shares the active object with the master Hub,
	 *         false otherwise
	 */
	public boolean isSharingAO() {
		return bShareAO;
	}

	/**
	 * Returns whether this filter is sharing the active object with the master Hub.
	 *
	 * @return true if active-object sharing is enabled, false otherwise
	 */
	public boolean getIsSharingAO() {
		return bShareAO;
	}

	// Hub Listener code for filtered Hub
	//    note: this needs to be here so that HubShareDelegate can find HubFilter for a hub

	/**
	 * HubListener callback that forwards the added object to {@link #afterAdd(Object)}.
	 *
	 * @param e the HubEvent containing the added object
	 */
	public @Override void afterAdd(HubEvent<TYPE> e) {
		afterAdd(e.getObject());
	}

	/**
	 * Handles an object being added directly to the filtered Hub. If the object
	 * does not already exist in the master Hub and satisfies master–detail link
	 * ownership rules, it is added to the master Hub.
	 *
	 * @param obj the added object
	 */
	public void afterAdd(TYPE obj) {
		if (aiUpdating.get() != 0) {
			return;
		}
		if (hubMaster == null || hubMaster.contains(obj)) {
			return;
		}
		// 20160904 dont allow it to reassign if it is masterObject does not match
		Object objMaster = hubMaster.getMasterObject();
		if (objMaster != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj.getClass());
			OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromDetailToMaster(hubMaster);
			if (li != null) {
				OALinkInfo rli = li.getReverseLinkInfo();
				if (li != null) {
					if (li.getOwner()) {
						Object objx = li.getValue(obj);
						if (objx != null && objx != objMaster) {
							return;
						}
					}
				}
			}
		}
		hubMaster.add(obj);
	}

	/**
	 * Handles property-change events for the filtered Hub. When the "Link"
	 * property changes, the linkHub listener is reinitialized.
	 *
	 * @param e the HubEvent describing the property change
	 */
	public @Override void afterPropertyChange(HubEvent<TYPE> e) {
		if (e.getPropertyName().equalsIgnoreCase("Link")) {
			setupLinkHubListener();
		}
	}

	/**
	 * HubListener callback that treats inserts the same as adds by delegating
	 * to {@link #afterAdd(HubEvent)}.
	 *
	 * @param e the HubEvent describing the insert
	 */
	@Override
	public void afterInsert(HubEvent<TYPE> e) {
		afterAdd(e);
	}

	/**
	 * HubListener callback for removals. If not currently updating or clearing,
	 * forwards the removed object to {@link #afterRemove(Object)}.
	 *
	 * @param e the HubEvent containing the removed object
	 */
	@Override
	public void afterRemove(HubEvent<TYPE> e) {
		if (aiUpdating.get() == 0 && aiClearing.get() == 0) {
			afterRemove(e.getObject());
		}
	}

	/**
	 * Called when an object is removed from the filtered Hub. Delegates to
	 * {@link #afterRemoveFromFilteredHub(Object)} if a master Hub exists.
	 *
	 * @param obj the removed object
	 */
	public void afterRemove(TYPE obj) {
		if (hubMaster != null) {
			HubFilter.this.afterRemoveFromFilteredHub(obj);
		}
	}

	/**
	 * Called when an object is removed directly from the filtered Hub. Intended
	 * for subclasses; the default implementation does nothing.
	 *
	 * @param obj the removed object
	 */
	protected void afterRemoveFromFilteredHub(TYPE obj) {
	}

	/**
	 * HubListener callback when all objects are removed from the filtered Hub.
	 * If not currently clearing, forwards to {@link #afterRemoveAllFromFilteredHub()}.
	 *
	 * @param e the HubEvent for the remove-all operation
	 */
	@Override
	public void afterRemoveAll(HubEvent<TYPE> e) {
		if (aiClearing.get() == 0) {
			afterRemoveAllFromFilteredHub();
		}
	}

	/**
	 * Called when all objects are removed directly from the filtered Hub.
	 * Intended for subclasses; the default implementation does nothing.
	 */
	protected void afterRemoveAllFromFilteredHub() {
	}

	/**
	 * Synchronizes active objects between filtered Hub and master Hub when
	 * active-object sharing is enabled. If the filtered Hub's active object
	 * is not in the master Hub, null is used instead.
	 *
	 * @param e the HubEvent describing the AO change
	 */
	@Override
	public void afterChangeActiveObject(HubEvent<TYPE> e) {
		Hub<TYPE> hub = getHub();
		if (bShareAO && hub != null && hubMaster != null) {
			Object obj = hub.getAO();
			if (obj != null && !HubFilter.this.hubMaster.contains(obj)) {
				obj = null;
			}
			if (!bIgnoreSettingAO) {
				HubFilter.this.hubMaster.setAO(obj);
			}
		}
	}

	/**
	 * Marks the current filter position so subsequent filters can be grouped
	 * into a block filter using {@link #endBlock()}.
	 */
	public void startBlock() {
		iBlockPos = alFilters == null ? 0 : alFilters.size();
	}

	/**
	 * Converts all filters added since {@link #startBlock()} into a single
	 * {@link OABlockFilter} and appends it to the filter list.
	 */
	public void endBlock() {
		if (iBlockPos >= 0 && alFilters != null) {
			int x = alFilters.size();
			if (x > iBlockPos) {
				OAFilter[] filters = new OAFilter[x - iBlockPos];
				for (int i = iBlockPos; i < x; i++) {
					filters[i - iBlockPos] = alFilters.remove(iBlockPos);
				}
				OAFilter f = new OABlockFilter(filters);
				addFilter(f);
			}
		}
		iBlockPos = -1;
	}

	/**
	 * Removes all filters associated with this HubFilter.
	 */
	public void clearFilters() {
		alFilters = null;
	}

	/**
	 * Adds the specified filter to the filter list and triggers a refresh.
	 *
	 * @param filter the filter to add
	 */
	public void addFilter(OAFilter<TYPE> filter) {
		if (alFilters == null) {
			alFilters = new ArrayList<OAFilter<TYPE>>();
		}
		alFilters.add(filter);
		refresh();
	}

	/**
	 * Adds a filter and registers dependent property paths that should trigger
	 * refresh when changed.
	 *
	 * @param f the filter to add
	 * @param dependentPropPaths property paths that affect the filter
	 */
	public void addFilter(OAFilter<TYPE> f, String... dependentPropPaths) {
		addFilter(f);
		if (dependentPropPaths == null) {
			return;
		}
		for (String pp : dependentPropPaths) {
			addDependentProperty(pp);
		}
	}

	/**
	 * Adds an equality filter for the specified property path.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the required value for equality
	 */
	public void addEqualFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OAEqualFilter(value));
	}

	/**
	 * Adds a filter that evaluates to true when the property value is non-null.
	 *
	 * @param propPath the property path to evaluate
	 */
	public void addTrueFilter(final String propPath) {
		_addFilter(propPath, new OATrueFilter());
	}

	/**
	 * Adds a filter that always evaluates to true. (Equivalent to a true filter.)
	 *
	 * @param propPath the property path to evaluate
	 */
	public void addFalseFilter(final String propPath) {
		_addFilter(propPath, new OAFalseFilter());
	}

	/**
	 * Adds a filter that evaluates true when the property value does not equal
	 * the specified value.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the value to compare against
	 */
	public void addNotEqualFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OANotEqualFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is between
	 * (or equal to) the specified range values.
	 *
	 * @param propPath the property path to evaluate
	 * @param value1   the lower bound
	 * @param value2   the upper bound
	 */
	public void addBetweenOrEqualFilter(final String propPath, final Object value1, final Object value2) {
		_addFilter(propPath, new OABetweenOrEqualFilter(value1, value2));
	}

	/**
	 * Adds a filter that evaluates true when the property value is strictly
	 * between the specified lower and upper values.
	 *
	 * @param propPath the property path to evaluate
	 * @param value1   the lower bound
	 * @param value2   the upper bound
	 */
	public void addBetween(final String propPath, final Object value1, final Object value2) {
		_addFilter(propPath, new OABetweenFilter(value1, value2));
	}

	/**
	 * Adds a filter that evaluates true when the property value is null.
	 *
	 * @param propPath the property path to evaluate
	 */
	public void addNullFilter(final String propPath) {
		if (OAString.isEmpty(propPath)) {
			return;
		}
		_addFilter(propPath, new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				return obj == null;
			}
		});
	}

	/**
	 * Adds a filter that evaluates true when the property value is not null.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    unused value parameter
	 */
	public void addNotNullFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				return obj != null;
			}
		});
	}

	/**
	 * Adds a filter that evaluates true when the property value is empty.
	 *
	 * @param propPath the property path to evaluate
	 */
	public void addEmptyFilter(final String propPath) {
		_addFilter(propPath, new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				return OAString.isEmpty(obj);
			}
		});
	}

	/**
	 * Adds a filter that evaluates true when the property value is not empty.
	 *
	 * @param propPath the property path to evaluate
	 */
	public void addNotEmptyFilter(final String propPath) {
		_addFilter(propPath, new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				return !OAString.isEmpty(obj);
			}
		});
	}

	/**
	 * Adds a filter that evaluates true when the property value matches
	 * the supplied value using pattern comparison.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the value or pattern to compare against
	 */
	public void addLikeFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OALikeFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value does not
	 * match the supplied value using pattern comparison.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the value or pattern to compare against
	 */
	public void addNotLikeFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OANotLikeFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is greater
	 * than the supplied value.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the lower bound comparison value
	 */
	public void addGreaterFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OAGreaterFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is greater
	 * than or equal to the supplied value.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the lower bound comparison value
	 */
	public void addGreaterOrEqualFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OAGreaterOrEqualFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is less than
	 * the supplied value.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the upper bound comparison value
	 */
	public void addLessFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OALessFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is less than
	 * or equal to the supplied value.
	 *
	 * @param propPath the property path to evaluate
	 * @param value    the upper bound comparison value
	 */
	public void addLessOrEqualFilter(final String propPath, final Object value) {
		_addFilter(propPath, new OALessOrEqualFilter(value));
	}

	/**
	 * Adds a filter that evaluates true when the property value is between
	 * the supplied lower and upper bounds.
	 *
	 * @param propPath the property path to evaluate
	 * @param value1   the lower bound
	 * @param value2   the upper bound
	 */
	public void addBetweenFilter(final String propPath, final Object value1, final Object value2) {
		_addFilter(propPath, new OABetweenFilter(value1, value2));
	}

	/**
	 * Internal helper used to add a filter based on a property path. Registers
	 * the dependent property and wraps the filter for property lookup and
	 * multi-level path resolution.
	 *
	 * @param propPath the property path used by the filter
	 * @param filter   the underlying filter to apply to the resolved value
	 */
	private void _addFilter(final String propPath, final OAFilter filter) {
		if (filter == null) {
			return;
		}
		addDependentProperty(propPath, false);

		OAFilter<TYPE> f;
		if (OAString.isEmpty(propPath)) {
			f = filter;
		} else if (OAString.dcount(propPath, '.') == 1) {
			f = new OAFilter<TYPE>() {
				@Override
				public boolean isUsed(TYPE obj) {
					if (obj == null) {
						return false;
					}
					Object objx = ((OAObject) obj).getProperty(propPath);
					return filter.isUsed(objx);
				}
			};
		} else {
			int dcnt = OAString.dcount(propPath, '.');
			final String prop = OAString.field(propPath, '.', 1, dcnt - 1);
			final String propLast = OAString.field(propPath, '.', dcnt);

			f = new OAFilter() {
				@Override
				public boolean isUsed(Object obj) {
					if (obj == null) {
						return false;
					}
					Object objx = ((OAObject) obj).getProperty(propLast);
					return filter.isUsed(objx);
				}
			};

			final OAFilter fx = f;

			f = new OAFilter() {
				public boolean isUsed(Object obj) {
					OAFinder find = new OAFinder(prop);
					find.addFilter(fx);
					return find.canFindFirst((OAObject) obj);
				}
			};
		}
		addFilter(f);
	}
}
