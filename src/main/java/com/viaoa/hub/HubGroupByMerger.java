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

import java.lang.reflect.Method;

import com.viaoa.object.OAObject;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

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

	private String mergerPropertyPath;
	private String groupByPropertyPath;
	private String groupByProperty;

	private int cntAbove; // number of data.parent to go from mergePropertyPath to then use groupByPropertyPath

	private HubMerger<F, T> hubMerger;

	public HubGroupByMerger(Hub<F> hubRoot, String groupByPropertyPath, String groupByProperty) {
		this(hubRoot, null, groupByPropertyPath, groupByProperty);
	}

	/**
	 * @param hubRoot
	 * @param mergerPropertyPath  PP from Root to merger objects
	 * @param groupByPropertyPath PP from hubRoot objects to the object where there is a calc Hub<T> for storing the found merger objects.
	 *                            *NOTE: this PP must start from same root as mergerPropertyPath. <T>
	 * @param groupByProperty     name of property Hub<T> in groupByPP for storing the found merger objects.
	 */
	public HubGroupByMerger(Hub<F> hubRoot, String mergerPropertyPath, String groupByPropertyPath, String groupByProperty) {
		this.mergerPropertyPath = mergerPropertyPath;
		this.groupByPropertyPath = groupByPropertyPath;
		this.groupByProperty = groupByProperty;

		final OAPropertyPath ppGroupByPropertyPath = new OAPropertyPath(hubRoot.getObjectClass(), groupByPropertyPath);
		Method[] msGroupByPropertyPath = ppGroupByPropertyPath.getMethods();

		if (OAString.isEmpty(mergerPropertyPath)) {
			hubRoot.addHubListener(new HubListenerAdapter() {
				@Override
				public void afterAdd(HubEvent e) {
					OAObject objFrom = (OAObject) e.getObject();
					OAObject objTo = (OAObject) ppGroupByPropertyPath.getValue(objFrom);
					if (objTo != null) {
						Hub hub = (Hub) objTo.getProperty(groupByProperty);
						hub.add(objFrom);
					}
				}

				@Override
				public void afterRemove(HubEvent e) {
					OAObject objFrom = (OAObject) e.getObject();
					OAObject objTo = (OAObject) ppGroupByPropertyPath.getValue(objFrom);
					if (objTo != null) {
						Hub hub = (Hub) objTo.getProperty(groupByProperty);
						hub.remove(objFrom);
					}
				}
			});
		} else {
			final OAPropertyPath ppMergerPropertyPath = new OAPropertyPath(hubRoot.getObjectClass(), mergerPropertyPath);
			Method[] msMergerPropertyPath = ppMergerPropertyPath.getMethods();

			int cnt = 0;
			for (; cnt < msGroupByPropertyPath.length && cnt < msMergerPropertyPath.length; cnt++) {
				if (!msGroupByPropertyPath[cnt].equals(msMergerPropertyPath[cnt])) {
					break;
				}
			}

			// find how much of the the groupBy PP is same as merger
			this.cntAbove = msMergerPropertyPath.length - (cnt + 1);

			final int groupByPropertyPathStartPos = cnt;

			hubMerger = new HubMerger(hubRoot, null, mergerPropertyPath, false, null, true, false, false) {
				@Override
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
						OAObject objTo = (OAObject) ppGroupByPropertyPath.getValue(objFrom, groupByPropertyPathStartPos);
						if (objTo != null) {
							Hub hub = (Hub) objTo.getProperty(groupByProperty);
							hub.add(obj);
						}
					}
				}

				@Override
				protected void onRemoveFromCombined(Data data, OAObject obj) {
					for (int i = 0; data != null && i < cntAbove; i++) {
						data = data.parent;
					}
					if (data != null) {
						OAObject objx = (OAObject) ppGroupByPropertyPath.getValue(data.parentObject);
						Hub hub = (Hub) objx.getProperty(groupByProperty);
						hub.remove(obj);
					}
				}
			};

			hubMerger.setServerSideOnly(true);
		}
	}

}
