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
package com.viaoa.hub.merge;

import java.lang.reflect.Method;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.view.HubGroupBy;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Companion utility for {@link HubGroupBy} that merges group changes and
 * re-synchronizes when the master or detail Hubs are refreshed.
 * <p>
 * Used when grouped views depend on multiple master Hubs or nested groupings,
 * ensuring consistent propagation of object additions/removals.
 *
 * <p>Supports deferred rebuilds to coalesce bursts of changes.</p>
 */
public class HubGroupByMerger<F extends OAObject, T extends OAObject> {

	/**
	 * Property path used to locate merger objects from the root Hub.
	 * If {@code null} or empty, the merger logic operates directly on the root Hub.
	 */
	private String mergerPath;

	/**
	 * Property path from the root objects to the target object that contains
	 * the grouping Hub used to store merged objects.
	 */
	private String groupByPath;

	/**
	 * Name of the Hub property within the group-by target object
	 * into which merger results are added or removed.
	 */
	private String groupByProperty;

	/**
	 * Number of parent-level steps required to move upward in the merger
	 * property path to reach the object that corresponds to the group-by
	 * starting position. Used to align merger and group-by resolution paths.
	 */
	private int cntAbove; // number of data.parent to go from mergePath to then use groupByPath

	/**
	 * Internal {@link HubMerger} instance used when a merger property path
	 * is supplied, enabling automatic propagation of add/remove events into
	 * grouped Hub structures.
	 */
	private HubMerger<F, T> hubMerger;


	/**
	 * Creates a {@code HubGroupByMerger} using the root Hub and group-by settings,
	 * without a merger property path. This configures direct add/remove listeners
	 * on the root Hub to maintain the grouped Hub structure.
	 *
	 * @param hubRoot the root Hub used as the source of objects to be grouped
	 * @param groupByPath property path from root objects to the target object
	 *                            that owns the grouping Hub
	 * @param groupByProperty the name of the Hub property within the group-by target
	 *                        object that stores grouped objects
	 */
	public HubGroupByMerger(Hub<F> hubRoot, String groupByPath, String groupByProperty) {
		this(hubRoot, null, groupByPath, groupByProperty);
	}

	/**
	 * Constructs a {@code HubGroupByMerger} that synchronizes grouped Hub relationships
	 * using the supplied property paths.
	 *
	 * @param hubRoot the root Hub used as the source for grouping and merging
	 * @param mergerPath property path from the root to merger objects; if empty,
	 *                           direct add/remove listeners are installed on {@code hubRoot}
	 * @param groupByPath property path from root objects to the object containing
	 *                            the grouping Hub
	 * @param groupByProperty the name of the Hub property within the group-by target object
	 *                        that stores merged objects
	 */
	public HubGroupByMerger(Hub<F> hubRoot, String mergerPath, String groupByPath, String groupByProperty) {
		this.mergerPath = mergerPath;
		this.groupByPath = groupByPath;
		this.groupByProperty = groupByProperty;

		final OAPath ppGroupByPath = new OAPath(hubRoot.getObjectClass(), groupByPath);
		Method[] msGroupByPath = ppGroupByPath.getMethods();

		if (OAString.isEmpty(mergerPath)) {
			hubRoot.addHubListener(new HubListenerAdapter() {
				@Override
				/**
				 * Handles the Hub after-add event.
				 * @param e the Hub event
				 */
				public void afterAdd(HubEvent e) {
					OAObject objFrom = (OAObject) e.getObject();
					OAObject objTo = (OAObject) ppGroupByPath.getValue(objFrom);
					if (objTo != null) {
						Hub hub = (Hub) objTo.getProperty(groupByProperty);
						hub.add(objFrom);
					}
				}

				@Override
				/**
				 * Handles the Hub after-remove event.
				 * @param e the Hub event
				 */
				public void afterRemove(HubEvent e) {
					OAObject objFrom = (OAObject) e.getObject();
					OAObject objTo = (OAObject) ppGroupByPath.getValue(objFrom);
					if (objTo != null) {
						Hub hub = (Hub) objTo.getProperty(groupByProperty);
						hub.remove(objFrom);
					}
				}
			});
		} else {
			final OAPath ppMergerPath = new OAPath(hubRoot.getObjectClass(), mergerPath);
			Method[] msMergerPath = ppMergerPath.getMethods();

			int cnt = 0;
			for (; cnt < msGroupByPath.length && cnt < msMergerPath.length; cnt++) {
				if (!msGroupByPath[cnt].equals(msMergerPath[cnt])) {
					break;
				}
			}

			// find how much of the the groupBy PP is same as merger
			this.cntAbove = msMergerPath.length - (cnt + 1);

			final int groupByPathStartPos = cnt;

			hubMerger = new HubMerger(hubRoot, null, mergerPath, false, null, true, false, false) {
				@Override
				/**
				 * Performs the onAddToCombined operation for this Hub component.
				 */
				protected void onAddToCombined(Data data, final OAObject obj) {
					OAObject objFrom = obj;
					if (cntAbove >= 0) {
						for (int i = 0; data != null && i < cntAbove; i++) {
							data = data.parent;
						}
						if (data == null) {
							objFrom = null;
						} else {
							objFrom = data.parentObject;
						}
					}
					if (objFrom != null) {
						OAObject objTo = (OAObject) ppGroupByPath.getValue(objFrom, groupByPathStartPos);
						if (objTo != null) {
							Hub hub = (Hub) objTo.getProperty(groupByProperty);
							hub.add(obj);
						}
					}
				}

				@Override
				/**
				 * Performs the onRemoveFromCombined operation for this Hub component.
				 */
				protected void onRemoveFromCombined(Data data, OAObject obj) {
					for (int i = 0; data != null && i < cntAbove; i++) {
						data = data.parent;
					}
					if (data != null) {
						OAObject objx = (OAObject) ppGroupByPath.getValue(data.parentObject);
						Hub hub = (Hub) objx.getProperty(groupByProperty);
						hub.remove(obj);
					}
				}
			};

			hubMerger.setServerSideOnly(true);
		}
	}

}
