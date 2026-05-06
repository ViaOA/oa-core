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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Groups objects from a source {@link Hub} into multiple sub-Hubs based on the
 * value of a grouping property or key expression.
 * <p>
 * Each distinct key results in a child Hub containing all matching objects.
 * These grouped Hubs are themselves managed within a container Hub of groups.
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Listen to source Hub add/remove/update events and maintain grouping integrity.</li>
 *   <li>Create or dispose group Hubs dynamically as keys appear or disappear.</li>
 *   <li>Optionally cascade Active-Object changes across groups.</li>
 * </ul>
 *
 * <p>Used heavily in analytic or UI dashboards for pivot-like views.</p>
 */
public class HubGroupBy<F extends OAObject, G extends OAObject> {

	/**
	 * The source Hub containing objects that will be grouped based on
	 * the grouping property or key expression.
	 */
	private Hub<F> hubFrom;

	/**
	 * The concrete class type of objects contained in the source Hub.
	 */
	private final Class<F> classFrom;
	
	/**
	 * The class of the groupBy key objects. Determined lazily from runtime
	 * events when not explicitly supplied.
	 */
	private Class<G> classGroupBy;

	/**
	 * Optional Hub providing the set of explicit groupBy key objects,
	 * required when the property path contains a split or non-navigable segment.
	 */
	private Hub<G> hubGroupBy;

	/**
	 * The result Hub containing OAGroupBy entries, each representing a
	 * groupBy key and its associated grouped objects.
	 */
	private Hub<OAGroupBy<F, G>> hubCombined;

	/**
	 * Property-path expression used to extract the grouping value from
	 * each source object.
	 */
	private String propertyPath;

	/**
	 * Optional name of a Hub property within the groupBy object used for
	 * establishing HubCopy behavior.
	 */
	private String hubPropertyName;

	/**
	 * Internal calculated property name used when installing dependent
	 * property listeners on the source Hub.
	 */
	private String listenPropertyName;

	/**
	 * Hub containing synchronized groupBy key objects used for active-object
	 * propagation and master/detail navigation.
	 */
	private Hub<G> hubMaster;

	/**
	 * Detail Hub representing objects belonging to the active group in the
	 * combined results.
	 */
	private Hub<F> hubDetail;

	/**
	 * Internal flag used to suppress recursive active-object update handling
	 * while programmatically modifying AO values.
	 */
	private volatile boolean bIgnoreAOChange;
	
	/**
	 * Indicates whether a separate group should be maintained for objects
	 * whose grouping value is {@code null}.
	 */
	private boolean bCreateNullList;

	/**
	 * Counter used for generating unique internal names for calculated
	 * dependent properties.
	 */
	private final static AtomicInteger aiCnt = new AtomicInteger();

	/**
	 * Constructs a {@code HubGroupBy} that groups objects from the specified source
	 * Hub based on the given property path.
	 *
	 * @param hubB the source Hub containing objects to be grouped
	 * @param propertyPath the property path used to determine grouping
	 * @param bCreateNullList whether to create a group for {@code null} grouping values
	 */
	public HubGroupBy(Hub<F> hubB, String propertyPath, boolean bCreateNullList) {
		this.hubGroupBy = null;
		this.hubFrom = hubB;
		this.classFrom = hubB.getObjectClass();
		this.classGroupBy = null;

		this.propertyPath = propertyPath;
		this.bCreateNullList = bCreateNullList;
		setup();
	}

	/**
	 * Constructs a {@code HubGroupBy} that groups objects from the specified Hub
	 * using the given property path and enables creation of a null-group list.
	 *
	 * @param hubB the source Hub containing objects to be grouped
	 * @param propertyPath the property path used to determine grouping
	 */
	public HubGroupBy(Hub<F> hubB, String propertyPath) {
		this(hubB, propertyPath, true);
	}

	/**
	 * Constructs a {@code HubGroupBy} that groups objects from the given Hub using
	 * a property path, and associates each group with a link-many Hub property.
	 *
	 * @param hubB the source Hub containing objects to be grouped
	 * @param propertyPath the property path used for grouping
	 * @param hubPropertyName optional name of a Hub property within the groupBy object
	 */
	public HubGroupBy(Hub<F> hubB, String propertyPath, String hubPropertyName) {
		this.hubGroupBy = null;
		this.hubFrom = hubB;
		this.classFrom = hubB.getObjectClass();
		this.classGroupBy = null;

		this.propertyPath = propertyPath;
		this.hubPropertyName = hubPropertyName;
		this.bCreateNullList = false;
		setup();
	}

	/**
	 * Constructs a {@code HubGroupBy} that groups objects from a source Hub while
	 * synchronizing with an external Hub of groupBy keys.
	 *
	 * @param hubFrom the source Hub providing objects to be grouped
	 * @param hubGrpBy the Hub providing groupBy key objects
	 * @param propertyPath the property path from source to groupBy
	 * @param bCreateNullList whether to create a group for {@code null} grouping values
	 */
	public HubGroupBy(Hub<F> hubFrom, Hub<G> hubGrpBy, String propertyPath, boolean bCreateNullList) {
		this.hubFrom = hubFrom;
		this.hubGroupBy = hubGrpBy;
		this.classFrom = hubFrom.getObjectClass();
		this.classGroupBy = null;
		this.propertyPath = propertyPath;
		this.bCreateNullList = bCreateNullList;
		setup();
	}

	/**
	 * Constructs a {@code HubGroupBy} that groups objects from the source Hub using
	 * the given groupBy Hub and property path, with a null-group list enabled.
	 *
	 * @param hubFrom the source Hub providing objects to be grouped
	 * @param hubGrpBy the Hub providing groupBy key objects
	 * @param propertyPath the property path from source to groupBy
	 */
	public HubGroupBy(Hub<F> hubFrom, Hub<G> hubGrpBy, String propertyPath) {
		this(hubFrom, hubGrpBy, propertyPath, true);
	}

	/**
	 * Combines two existing {@code HubGroupBy} instances into a single merged
	 * grouping structure.
	 *
	 * @param hgb1 the first HubGroupBy instance
	 * @param hgb2 the second HubGroupBy instance
	 * @param bCreateNullList whether to create a group for {@code null} grouping values
	 */
	public HubGroupBy(HubGroupBy<F, G> hgb1, HubGroupBy<F, G> hgb2, boolean bCreateNullList) {
		if (hgb1 == null || hgb2 == null) {
			throw new IllegalArgumentException("hgb1 & hgb2 can not be null");
		}
		this.bCreateNullList = bCreateNullList;
		this.classFrom = hgb1.classFrom;
		this.classGroupBy = hgb1.classGroupBy;
		setupCombined(hgb1, hgb2);
	}

	/**
	 * Combines two existing {@code HubGroupBy} instances into a merged grouping
	 * using a null-group list.
	 *
	 * @param hgb1 the first HubGroupBy instance
	 * @param hgb2 the second HubGroupBy instance
	 */
	public HubGroupBy(HubGroupBy<F, G> hgb1, HubGroupBy<F, G> hgb2) {
		this(hgb1, hgb2, true);
	}

	/**
	 * Constructs a new {@code HubGroupBy} derived from an existing one while adding
	 * an additional property path to group by.
	 *
	 * @param hgb the base HubGroupBy instance
	 * @param pp the additional property path
	 * @param bCreateNullList whether to create a group for {@code null} grouping values
	 */
	public HubGroupBy(HubGroupBy<F, G> hgb, String pp, boolean bCreateNullList) {
		if (hgb == null) {
			throw new IllegalArgumentException("hgb can not be null");
		}
		this.classFrom = hgb.classFrom;
		this.classGroupBy = null;
		this.bCreateNullList = bCreateNullList;
		HubGroupBy<F, G> hgb2 = new HubGroupBy<F, G>(hgb.hubFrom, pp, bCreateNullList);
		setupCombined(hgb, hgb2);
	}

	/**
	 * Constructs a new {@code HubGroupBy} derived from an existing one using the
	 * specified property path and enabling null-group creation.
	 *
	 * @param hgb the base HubGroupBy instance
	 * @param pp the additional property path
	 */
	public HubGroupBy(HubGroupBy<F, G> hgb, String pp) {
		this(hgb, pp, true);
	}

	/**
	 * Returns the Hub containing {@code OAGroupBy} entries combining groupBy keys
	 * and their associated grouped objects.
	 *
	 * @return the combined Hub of grouped results
	 */
	public Hub<OAGroupBy<F, G>> getCombinedHub() {
		if (hubCombined != null) {
			return hubCombined;
		}
		hubCombined = new Hub(OAGroupBy.class);
		return hubCombined;
	}

	/**
	 * Returns the Hub of groupBy objects kept in sync with the combined grouping.
	 *
	 * @return the master Hub of groupBy keys
	 */
	public Hub<G> getMasterHub() {
		if (hubMaster == null) {
			if (hubGroupBy != null) {
				hubMaster = new Hub<G>(hubGroupBy.getObjectClass());
			} else {
				hubMaster = new Hub<G>();
			}
			new HubMerger(getCombinedHub(), hubMaster, OAGroupBy.P_GroupBy, true);

			hubMaster.addHubListener(new HubListenerAdapter() {
				@Override
				public void afterChangeActiveObject(HubEvent e) {
					if (bIgnoreAOChange) {
						return;
					}
					try {
						bIgnoreAOChange = true;
						final Object ao = e.getObject();
						hubFrom.setAO(null);
						if (hubDetail != null) {
							hubDetail.setAO(null);
						}

						if (ao == null) {
							getCombinedHub().setAO(null);
						} else {
							boolean bFound = false;
							for (OAGroupBy<F, G> bg : getCombinedHub()) {
								if (bg.getGroupBy() == ao) {
									getCombinedHub().setAO(bg);
									bFound = true;
									break;
								}
							}
							if (!bFound) {
								getCombinedHub().setAO(null);
							}
						}
					} finally {
						bIgnoreAOChange = false;
					}
				}

				@Override
				public void afterAdd(HubEvent e) {
					if (classGroupBy == null) {
						classGroupBy = (Class<G>) e.getObject().getClass();
					}
				}
			});
		}
		return hubMaster;
	}

	/**
	 * Returns the detail Hub representing objects belonging to the active group
	 * within the combined Hub.
	 *
	 * @return the detail Hub of grouped objects
	 */
	public Hub<F> getDetailHub() {
		if (hubDetail == null) {
			String pp = "(" + classFrom.getName() + ") " + OAGroupBy.P_Hub;
			hubDetail = getCombinedHub().getDetailHub(pp);
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubFrom);
			og.hubsInternal().callHubDataSetObjectClass(hubDetail, classFrom);
			hubDetail.addHubListener(new HubListenerAdapter() {
				@Override
				public void afterChangeActiveObject(HubEvent e) {
					if (bIgnoreAOChange) {
						return;
					}
					try {
						bIgnoreAOChange = true;
						hubFrom.setAO(e.getObject());
					} finally {
						bIgnoreAOChange = false;
					}
				}
			});

		}
		return hubDetail;
	}

	/**
	 * Initializes the grouping logic by analyzing the property path and determining
	 * whether grouping requires a split configuration or can proceed through the main
	 * setup path.
	 */
	void setup() {
		OAPath opp = new OAPath(propertyPath);

		try {
			opp.setup(classFrom, (hubGroupBy != null));
		} catch (Exception e) {
			throw new RuntimeException("PropertyPath setup failed", e);
		}

		OALinkInfo[] lis = opp.getLinkInfos();
		Method[] ms = opp.getMethods();

		int posEmpty = 0;
		for (Method m : ms) {
			if (m == null) {
				break;
			}
			posEmpty++;
		}
		if (posEmpty >= ms.length || hubGroupBy == null) {
			setupMain();
			return; // does not need to be split
		}

		// need to have a 2way propPath, one from rootHub, and another from topDown hub
		String pp1 = OAString.field(propertyPath, ".", 1, posEmpty);

		String pp2 = "";
		for (int i = ms.length - 1; i >= posEmpty; i--) {
			if (pp2.length() > 0) {
				pp2 += ".";
			}
			pp2 += lis[i].getReverseName();
		}

		hgb1 = new HubGroupBy(hubFrom, pp1, bCreateNullList);
		hubGB1 = hgb1.getCombinedHub();

		hgb2 = new HubGroupBy(hubGroupBy, pp2, bCreateNullList);
		hubGB2 = hgb2.getCombinedHub();

		setupSplit();
	}

	// used by propertyPath that require a split
	private HubGroupBy hgb1;
	private Hub<OAGroupBy> hubGB1;

	private HubGroupBy hgb2;
	private Hub<OAGroupBy> hubGB2;

	/*  This is used to define the structure that is created for the split.
	 *  <pre><code>

	    Original HubGroupBy  new HubGroupBy(hubApplicationGroup, hubMRADClient, "MRADClient.Application.ApplicationType.ApplicationGroup")

	    Split:
	       GB1:     new HubGroupBy(hubMRADClient, "MRADClient.Application.ApplicationType")
	       GB2:     new HubGroupBy(hubApplicationGroup, "ApplicationTypes")
	       GBNew:   hubCombined is updated using setupSlit


	      OAGroupBy   GB1       GB2          GBNew
	      .A          appType   appType      appGroup
	      .hubB       mrads     appGroups    mrads

	 </code></pre>
	 * This is used when a propertyPath has a link where one of the createMethod=false. By having the source hub
	 * for the leftmost HubB, and must also have the source HubA for the rightmost, two separate hgb can be used to update a 3rd
	 * hgb. This will set up the listeners for hgb1 & hgb2 to update this.hubCombined.
	 */
	
	/**
	 * Configures grouping when the property path contains a non-contiguous or
	 * non-navigable segment requiring a two-way split.  
	 * <p>
	 * Establishes listeners on both split HubGroupBy instances so changes propagate
	 * into this combined grouping.
	 */
	private void setupSplit() {
		// A: hubGroup1 (hgb1) left part of pp, using hubB as the root
		// A.1: listen to hgb1 add/removes and update this.hubCombined
		hubGB1.addHubListener(new HubListenerAdapter<OAGroupBy>() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				OAGroupBy gb1 = (OAGroupBy) e.getObject();
				if (gb1.getHub().size() == 0) {
					return;
				}
				final Object gb1A = gb1.getGroupBy();

				OAGroupBy gb2Found = null;
				if (gb1A != null) {
					for (OAGroupBy gb2 : hubGB2) {
						if (gb2.getGroupBy() == gb1A) {
							gb2Found = gb2;
							break;
						}
					}
				}
				if (gb2Found == null || gb2Found.getHub().getSize() == 0) {
					// add to empty list
					if (!bCreateNullList) {
						return;
					}
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					Hub<OAObject> hubX = gb1.getHub();
					for (OAObject gb1B : hubX) {
						gbNewFound.getHub().add(gb1B);
					}
					return;
				}

				for (Object gb2B : gb2Found.getHub()) {
					OAObject objGB2b = (OAObject) gb2B;
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == objGB2b) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy((G) objGB2b);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					for (Object gb1B : gb1.getHub()) {
						gbNewFound.getHub().add((OAObject) gb1B);
					}
				}
				// remove from gbNew.A=null hubB
				if (!bCreateNullList) {
					return;
				}
				OAGroupBy gbNewFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == null) {
						gbNewFound = gbNew;
						break;
					}
				}
				if (gbNewFound != null) {
					for (Object gb1B : gb1.getHub()) {
						gbNewFound.getHub().remove(gb1B);
					}
				}
			}

			Object[] removeObjects;

			@Override
			public void beforeRemoveAll(HubEvent e) {
				removeObjects = hubGB1.toArray();
			}

			@Override
			public void afterRemoveAll(HubEvent e) {
				if (removeObjects != null) {
					for (Object obj : removeObjects) {
						remove((OAGroupBy) obj);
					}
					removeObjects = null;
				}
			}

			@Override
			public void afterRemove(HubEvent e) {
				OAGroupBy gb1 = (OAGroupBy) e.getObject();
				if (gb1.getHub().size() == 0) {
					return;
				}
				remove(gb1);
			}

			void remove(OAGroupBy gb1) {
				final OAObject gb1A = gb1.getGroupBy();
				OAGroupBy gb2Found = null;
				if (gb1A != null) {
					for (OAGroupBy gb2 : hubGB2) {
						if (gb2.getGroupBy() == gb1A) {
							gb2Found = gb2;
							break;
						}
					}
				}
				if (gb2Found == null || gb2Found.getHub().getSize() == 0) {
					// remove from empty list
					if (!bCreateNullList) {
						return;
					}
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						return;
					}
					for (Object gb1B : gb1.getHub()) {
						gbNewFound.getHub().remove(gb1B);
					}
					return;
				}

				for (Object gb2B : gb2Found.getHub()) {
					OAObject objGB2b = (OAObject) gb2B;
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == objGB2b) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						continue;
					}
					for (Object gb1B : gb1.getHub()) {
						gbNewFound.getHub().remove(gb1B);
					}
				}

				// see if it needs to be added to gbNew.A=null hubB
				if (!bCreateNullList) {
					return;
				}
				OAGroupBy gbNewFound = null;
				for (Object gb1B : gb1.getHub()) {
					if (!hubFrom.contains(gb1B)) {
						continue; // no longer in the From list
					}
					boolean bFound = false;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							continue;
						}
						if (gbNew.getHub().contains(gb1B)) {
							bFound = true;
							break;
						}
					}
					if (bFound) {
						continue;
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					gbNewFound.getHub().add((OAObject) gb1B);
				}
			}
		});

		// A.2: listen to changes to hgb1.hubB changes by using a hubmerger to get add/remove events and update this.hubCombined
		Hub<OAObject> hubTemp = new Hub<OAObject>(OAObject.class);
		HubMerger<OAGroupBy, OAObject> hm1 = new HubMerger<OAGroupBy, OAObject>(hubGB1, hubTemp, OAGroupBy.P_Hub, true) {
			@Override
			protected void afterInsertRealHub(HubEvent e) {
				afterAddRealHub(e);
			}

			@Override
			protected void afterAddRealHub(HubEvent e) {
				OAGroupBy gb = (OAGroupBy) (e.getHub()).getMasterObject();
				final OAObject gb1A = gb.getGroupBy();
				Object gb1B = e.getObject(); // object added

				OAGroupBy gb2Found = null;
				if (gb1A != null) {
					for (OAGroupBy gb2 : hubGB2) {
						if (gb2.getGroupBy() == gb1A) {
							gb2Found = gb2;
							break;
						}
					}
				}

				if (gb2Found == null || gb2Found.getHub().getSize() == 0) {
					// add to empty list
					if (!bCreateNullList) {
						return;
					}
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					gbNewFound.getHub().add((OAObject) gb1B);
					return;
				}

				for (Object gb2B : gb2Found.getHub()) {
					OAObject objGB2b = (OAObject) gb2B;
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == objGB2b) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy((G) objGB2b);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					gbNewFound.getHub().add((OAObject) gb1B);
				}
				//remove from null hub
				if (!bCreateNullList) {
					return;
				}
				OAGroupBy gbNewFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == null) {
						gbNew.getHub().remove(gb1B);
						break;
					}
				}
			}

			private Object[] removeAllObjects;

			@Override
			protected void beforeRemoveAllRealHub(HubEvent e) {
				removeAllObjects = e.getHub().toArray();
			}

			@Override
			protected void afterRemoveAllRealHub(HubEvent e) {
				if (removeAllObjects == null) {
					return;
				}
				OAGroupBy gb1 = (OAGroupBy) e.getHub().getMasterObject();
				for (Object obj : removeAllObjects) {
					remove(gb1, (OAGroupBy) obj);
				}
				removeAllObjects = null;
			}

			@Override
			protected void afterRemoveRealHub(HubEvent e) {
				OAGroupBy gb1 = (OAGroupBy) e.getHub().getMasterObject();
				Object gb1B = e.getObject();
				remove(gb1, gb1B);
			}

			void remove(final OAGroupBy gb1, final Object gb1B) {
				final OAObject gb1A = gb1.getGroupBy();

				OAGroupBy gb2Found = null;
				if (gb1A != null) {
					for (OAGroupBy gb2 : hubGB2) {
						if (gb2.getGroupBy() == gb1A) {
							gb2Found = gb2;
							break;
						}
					}
				}
				if (gb2Found == null || gb2Found.getHub().getSize() == 0) {
					// remove from empty list
					if (!bCreateNullList) {
						return;
					}
					if (hgb1.hubFrom.contains(gb1B)) {
						return;
					}

					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						return;
					}
					gbNewFound.getHub().remove(gb1B);
					return;
				}

				for (Object gb2B : gb2Found.getHub()) {
					OAObject objGB2b = (OAObject) gb2B;
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == objGB2b) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						continue;
					}
					gbNewFound.getHub().remove(gb1B);
				}

				if (!HubGroupBy.this.hubFrom.contains(gb1B)) {
					return;
				}

				// see if it needs to be added to gbNew.A=null hubB
				if (!bCreateNullList) {
					return;
				}
				OAGroupBy gbNewFound = null;
				boolean bFound = false;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == null) {
						gbNewFound = gbNew;
						continue;
					}
					if (gbNew.getHub().contains(gb1B)) {
						bFound = true;
						break;
					}
				}
				if (!bFound) {
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					gbNewFound.getHub().add((OAObject) gb1B);
				}
			}
		};

		// B: hubGroup2 (hgb2) right reverse part of pp, using hubA as the root
		// B.1: listen to hgb2 add/removes and update this.hubCombined
		// listen to GB2
		hubGB2.addHubListener(new HubListenerAdapter() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				OAGroupBy gb2 = (OAGroupBy) e.getObject();
				final OAObject gb2A = gb2.getGroupBy();

				OAGroupBy gb1Found = null;
				if (gb2A != null) {
					for (OAGroupBy gb1 : hubGB1) {
						if (gb1.getGroupBy() == gb2A) {
							gb1Found = gb1;
							break;
						}
					}
				}
				if (gb1Found == null) {
					for (Object gb2B : gb2.getHub()) {
						OAGroupBy gbNewFound = null;
						for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
							if (gbNew.getGroupBy() == gb2B) {
								gbNewFound = gbNew;
								break;
							}
						}
						if (gbNewFound == null) {
							gbNewFound = createGroupBy((G) gb2B);
							HubGroupBy.this.getCombinedHub().add(gbNewFound);
						}
					}
					return;
				}

				for (Object gb2B : gb2.getHub()) {
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == gb2B) {
							gbNewFound = gbNew;
							break;
						}
					}

					if (gbNewFound == null) {
						gbNewFound = createGroupBy((G) gb2B);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}

					for (Object gb1B : gb1Found.getHub()) {
						gbNewFound.getHub().add((OAObject) gb1B);
					}

					// might have been in gbNew.A=null gbNew.hubB
					if (!bCreateNullList) {
						continue;
					}
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							for (Object gb1B : gb1Found.getHub()) {
								gbNew.getHub().remove(gb1B);
							}
							break;
						}
					}
				}
			}

			Object[] removeObjects;

			@Override
			public void beforeRemoveAll(HubEvent e) {
				removeObjects = hubGB2.toArray();
			}

			@Override
			public void afterRemoveAll(HubEvent e) {
				if (removeObjects != null) {
					for (Object obj : removeObjects) {
						remove((OAGroupBy) obj);
					}
					removeObjects = null;
				}
			}

			@Override
			public void afterRemove(HubEvent e) {
				OAGroupBy gb2 = (OAGroupBy) e.getObject();
				remove(gb2);
			}

			void remove(OAGroupBy gb2) {
				final Object gb2A = gb2.getGroupBy();

				OAGroupBy gb1Found = null;
				if (gb2A != null) {
					for (OAGroupBy gb1 : hubGB1) {
						if (gb1.getGroupBy() == gb2A) {
							gb1Found = gb1;
							break;
						}
					}
				}
				if (gb1Found == null || gb1Found.getHub().getSize() == 0) {
					for (Object gb2B : gb2.getHub()) {
						if (hubGroupBy != null && hubGroupBy.contains(gb2B)) {
							continue;
						}
						for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
							if (gbNew.getGroupBy() == gb2B) {
								if (gbNew.getHub().size() == 0) {
									HubGroupBy.this.getCombinedHub().remove(gbNew);
								}
								break;
							}
						}
					}
					return;
				}

				for (Object gb2B : gb2.getHub()) {
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == gb2B) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						if (!bCreateNullList) {
							continue;
						}
						for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
							if (gbNew.getGroupBy() == null) {
								gbNewFound = gbNew;
								break;
							}
						}
						if (gbNewFound == null) {
							continue;
						}
					}

					for (Object gb1B : gb1Found.getHub()) {
						// ??? note: dont remove from hubB if it's still used for another path
						gbNewFound.getHub().remove(gb1B);
					}

					if (gbNewFound.getHub().size() == 0) {
						if (hubGroupBy == null || !hubGroupBy.contains(gbNewFound.getGroupBy())) {
							HubGroupBy.this.getCombinedHub().remove(gbNewFound);
						}
					}

					// add to gbNew.A=null gbNew.hubB
					if (!bCreateNullList) {
						continue;
					}
					gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					for (Object gb1B : gb1Found.getHub()) {
						gbNewFound.getHub().add((OAObject) gb1B);
					}
				}
			}
		});

		// B.2: listen to changes to hgb2.hubB changes by using a hubmerger to get add/remove events and update this.hubCombined
		Hub<OAObject> hubTemp2 = new Hub<OAObject>(OAObject.class);
		HubMerger<OAGroupBy, OAObject> hm2 = new HubMerger<OAGroupBy, OAObject>(hubGB2, hubTemp2, OAGroupBy.P_Hub, true) {
			@Override
			protected void afterInsertRealHub(HubEvent e) {
				afterAddRealHub(e);
			}

			@Override
			protected void afterAddRealHub(HubEvent e) {
				OAGroupBy gb2 = (OAGroupBy) e.getHub().getMasterObject();
				final Object gb2A = gb2.getGroupBy();
				Object gb2B = e.getObject(); // object added

				OAGroupBy gb1Found = null;
				if (gb2A != null) {
					for (OAGroupBy gb1 : hubGB1) {
						if (gb1.getGroupBy() == gb2A) {
							gb1Found = gb1;
							break;
						}
					}
				}
				if (gb1Found == null) {
					OAGroupBy gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == gb2B) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy((G) gb2B);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					return;
				}

				OAGroupBy gbNewFound = null;
				OAGroupBy gbNewNullFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == null) {
						gbNewNullFound = gbNew;
						if (gbNewFound != null) {
							break;
						}
					} else if (gbNew.getGroupBy() == gb2B) {
						gbNewFound = gbNew;
						if (gbNewNullFound != null) {
							break;
						}
					}
				}

				if (gbNewFound == null) {
					gbNewFound = createGroupBy((G) gb2B);
					HubGroupBy.this.getCombinedHub().add(gbNewFound);
				}
				if (gb1Found == null) {
					return;
				}
				for (Object gb1B : gb1Found.getHub()) {
					gbNewFound.getHub().add((OAObject) gb1B);

					// remove from null hub
					if (gbNewNullFound != null) {
						gbNewNullFound.getHub().remove(gb1B);
					}
				}
			}

			private Object[] removeAllObjects;

			@Override
			protected void beforeRemoveAllRealHub(HubEvent e) {
				removeAllObjects = e.getHub().toArray();
			}

			@Override
			protected void afterRemoveAllRealHub(HubEvent e) {
				if (removeAllObjects == null) {
					return;
				}
				OAGroupBy gb2 = (OAGroupBy) e.getHub().getMasterObject();
				for (Object obj : removeAllObjects) {
					remove(gb2, (OAGroupBy) obj);
				}
				removeAllObjects = null;
			}

			@Override
			protected void afterRemoveRealHub(HubEvent e) {
				OAGroupBy gb2 = (OAGroupBy) e.getHub().getMasterObject();
				Object gb2B = e.getObject();
				remove(gb2, gb2B);
			}

			void remove(OAGroupBy gb2, final Object gb2B) {
				final Object gb2A = gb2.getGroupBy();
				if (gb2A == null) {
					boolean bFound = false;
					for (OAGroupBy gb : hubGB2) {
						if (gb.getGroupBy() == null) {
							continue;
						}
						if (gb.getHub().contains(gb2B)) {
							bFound = true;
							break;
						}
					}
					if (!bFound) {
						for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
							if (gbNew.getGroupBy() == gb2B) {
								if (gbNew.getHub().size() == 0) {
									HubGroupBy.this.getCombinedHub().remove(gbNew);
								}
								break;
							}
						}
					}
					return;
				}

				OAGroupBy gb1Found = null;
				for (OAGroupBy gb1 : hubGB1) {
					if (gb1.getGroupBy() == gb2A) {
						gb1Found = gb1;
						break;
					}
				}
				if (gb1Found == null || gb1Found.getHub().getSize() == 0) {
					if (hubGroupBy.contains(gb2B)) {
						return;
					}
				}

				OAGroupBy gbNewFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == gb2B) {
						gbNewFound = gbNew;
						break;
					}
				}
				if (gbNewFound == null) {
					return;
				}

				if (gb1Found != null) {
					for (Object gb1B : gb1Found.getHub()) {
						// ??? note: dont remove from hubB if it's still used for another path
						gbNewFound.getHub().remove(gb1B);
					}
				}

				if (gbNewFound.getHub().size() == 0) {
					if (hubGroupBy == null || !hubGroupBy.contains(gbNewFound.getGroupBy())) {
						HubGroupBy.this.getCombinedHub().remove(gbNewFound);
					}
				}

				if (!bCreateNullList) {
					return;
				}
				if (gb1Found != null && gb2.getHub().size() == 0) {
					// need to add to gbNew.a=null hubB
					gbNewFound = null;
					for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
						if (gbNew.getGroupBy() == null) {
							gbNewFound = gbNew;
							break;
						}
					}
					if (gbNewFound == null) {
						gbNewFound = createGroupBy(null);
						HubGroupBy.this.getCombinedHub().add(gbNewFound);
					}
					for (Object gb1B : gb1Found.getHub()) {
						gbNewFound.getHub().add((OAObject) gb1B);
					}
				}
			}
		};

		// C: initial load for this.hubCombined using GB1
		for (OAGroupBy gb1 : hubGB1) {
			OAObject gb1A = (OAObject) gb1.getGroupBy();

			boolean bFound = false;
			OAGroupBy gb2Found = null;
			for (OAGroupBy gb2 : hubGB2) {
				if (gb2.getGroupBy() == gb1A) {
					gb2Found = gb2;
				}
			}

			if (gb2Found == null || gb2Found.getHub().getSize() == 0) {
				// add to empty list
				if (!bCreateNullList) {
					continue;
				}
				OAGroupBy gbNewFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == null) {
						gbNewFound = gbNew;
						break;
					}
				}
				if (gbNewFound == null) {
					gbNewFound = createGroupBy(null);
					HubGroupBy.this.getCombinedHub().add(gbNewFound);
				}
				for (Object gb1B : gb1.getHub()) {
					gbNewFound.getHub().add((OAObject) gb1B);
				}
				continue;
			}

			for (Object gb2B : gb2Found.getHub()) {
				OAObject objGB2b = (OAObject) gb2B;
				OAGroupBy gbNewFound = null;
				for (OAGroupBy gbNew : HubGroupBy.this.getCombinedHub()) {
					if (gbNew.getGroupBy() == objGB2b) {
						gbNewFound = gbNew;
						break;
					}
				}
				if (gbNewFound == null) {
					gbNewFound = createGroupBy((G) objGB2b);
					HubGroupBy.this.getCombinedHub().add(gbNewFound);
				}
				for (Object gb1B : gb1.getHub()) {
					gbNewFound.getHub().add((OAObject) gb1B);
				}
			}
		}
	}

	// main setup, if not needing a split
	/**
	 * Performs standard grouping setup when no property-path split is required.
	 * <p>
	 * Installs listeners on source and groupBy Hubs and maintains the combined grouping
	 * Hub in response to add, remove, update, AO changes, and new lists.
	 */
	void setupMain() {
		getCombinedHub().addHubListener(new HubListenerAdapter() {
			@Override
			public void afterChangeActiveObject(HubEvent e) {
				if (bIgnoreAOChange) {
					return;
				}

				try {
					// set the active object in hub A&B when hubCombine.AO is changed
					OAGroupBy obj = (OAGroupBy) e.getObject();
					if (obj == null) {
						if (hubGroupBy != null) {
							hubGroupBy.setAO(null);
						}
						if (hubMaster != null) {
							hubMaster.setAO(null);
						}
					} else {
						if (hubGroupBy != null) {
							hubGroupBy.setAO(obj.getGroupBy());
						}
						if (hubMaster != null) {
							hubMaster.setAO(obj.getGroupBy());
						}
					}
					hubFrom.setAO(null);
					if (hubDetail != null) {
						hubDetail.setAO(null);
					}
				} finally {
					bIgnoreAOChange = false;
				}
			}
		});

		if (hubGroupBy != null) {
			hubGroupBy.addHubListener(new HubListenerAdapter() {
				@Override
				public void afterInsert(HubEvent e) {
					afterAdd(e);
				}

				@Override
				public void afterAdd(HubEvent e) {
					G a = (G) e.getObject();
					boolean bFound = false;
					for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
						if (c.getGroupBy() == a) {
							bFound = true;
							break;
						}
					}
					if (!bFound) {
						OAGroupBy gbNewFound = createGroupBy(a);
						HubGroupBy.this.hubCombined.add(gbNewFound);
					}
				}

				Object[] removeObjects;

				@Override
				public void beforeRemoveAll(HubEvent e) {
					removeObjects = hubGroupBy.toArray();
				}

				@Override
				public void afterRemoveAll(HubEvent e) {
					if (removeObjects != null) {
						for (Object obj : removeObjects) {
							remove((G) obj);
						}
						removeObjects = null;
					}
				}

				@Override
				public void afterRemove(HubEvent e) {
					G a = (G) e.getObject();
					remove(a);
				}

				void remove(G a) {
					for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
						if (c.getGroupBy() == a) {
							HubGroupBy.this.hubCombined.remove(c);
							break;
						}
					}
				}

				@Override
				public void onNewList(HubEvent e) {
					HubGroupBy.this.hubCombined.clear();
					for (G a : hubGroupBy) {
						OAGroupBy gbNewFound = createGroupBy(a);
						HubGroupBy.this.hubCombined.add(gbNewFound);
					}
					addAll();
				}
			});
			for (G a : hubGroupBy) {
				boolean bFound = false;
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						bFound = true;
						break;
					}
				}
				if (!bFound) {
					OAGroupBy gbNewFound = createGroupBy(a);
					HubGroupBy.this.hubCombined.add(gbNewFound);
				}
			}
		}

		HubListener hl = new HubListenerAdapter() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				F b = (F) e.getObject();
				add(b);
			}

			Object[] removeObjects;

			@Override
			public void beforeRemoveAll(HubEvent e) {
				removeObjects = hubFrom.toArray();
			}

			@Override
			public void afterRemoveAll(HubEvent e) {
				if (removeObjects == null) return;
				for (Object obj : removeObjects) {
					remove((F) obj);
				}
				removeObjects = null;
			}

			@Override
			public void afterRemove(HubEvent e) {
				F b = (F) e.getObject();
				remove(b);
			}

			@Override
			public void afterPropertyChange(HubEvent e) {
				String s = e.getPropertyName();
				if (!listenPropertyName.equalsIgnoreCase(s)) {
					return;
				}
				update((F) e.getObject());
			}

			@Override
			public void onNewList(HubEvent e) {
				try {
					bIgnoreAOChange = true;
					HubGroupBy.this.getCombinedHub().clear();
				} finally {
					bIgnoreAOChange = false;
				}
				if (hubGroupBy != null) {
					for (G a : hubGroupBy) {
						OAGroupBy gbNewFound = createGroupBy(a);
						HubGroupBy.this.hubCombined.add(gbNewFound);
					}
				}
				addAll();
			}

			@Override
			public void afterChangeActiveObject(HubEvent e) {
				if (bIgnoreAOChange) {
					return;
				}
				bIgnoreAOChange = true;
				try {
					F ao = (F) e.getObject();
					if (ao == null) {
						HubGroupBy.this.hubCombined.setAO(null);
						if (hubMaster != null) {
							hubMaster.setAO(null);
						}
						if (hubDetail != null) {
							hubDetail.setAO(null);
						}
					} else {
						boolean bFound = false;
						for (OAGroupBy gb : HubGroupBy.this.getCombinedHub()) {
							Hub h = gb.getHub();
							if (!h.contains(ao)) {
								continue;
							}
							bFound = true;

							HubGroupBy.this.hubCombined.setAO(gb);
							h.setAO(ao);
							if (hubMaster != null) {
								hubMaster.setAO(gb.getGroupBy());
							}
							if (hubDetail != null) {
								hubDetail.setAO(ao);
							}
							break;
						}
						if (!bFound) {
							HubGroupBy.this.hubCombined.setAO(null);
							if (hubMaster != null) {
								hubMaster.setAO(null);
							}
							if (hubDetail != null) {
								hubDetail.setAO(null);
							}
						}
					}
				} finally {
					bIgnoreAOChange = false;
				}
			}
		};

		boolean b = false;
		if (propertyPath == null) {
			b = true;
		} else if (propertyPath.indexOf('.') < 0) {
			// propertyPath could be a hub
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(classFrom);
			OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(classFrom);
			OALinkInfo li = oi.getLinkInfo(propertyPath);
			if (li == null || li.getType() == li.ONE) {
				b = true;
			}
			// else it's a hub
		}

		if (b) {
			listenPropertyName = propertyPath;
			hubFrom.addHubListener(hl, propertyPath);
		} else {
			listenPropertyName = "hubGroupBy" + aiCnt.getAndIncrement();
			hubFrom.addHubListener(hl, listenPropertyName, new String[] { propertyPath });
		}
		addAll();
	}

	/**
	 * Adds all objects from the source Hub into the grouping structure, using a
	 * sibling helper to ensure correct detail resolution during property path access.
	 */
	private void addAll() {
		// this will tell the OASyncClient.getDetail which hub objects are being used
		final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(this.hubFrom);
		siblingHelper.add(this.propertyPath);
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.addSiblingHelper(siblingHelper);
		boolean bWas = srvcOAThreadLocal.getSendSyncMessages();
		try {
			srvcOAThreadLocal.setSendSyncMessages(false);
			for (F bx : hubFrom) {
				add(bx);
			}
		} finally {
			srvcOAThreadLocal.setSendSyncMessages(bWas);
			srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
		}
	}

	/**
	 * Adds the specified object to its appropriate group(s) using the grouping property.
	 *
	 * @param b the object being added
	 * @return the list of {@code OAGroupBy} groups the object belongs to, or {@code null}
	 */
	private ArrayList<OAGroupBy> add(F b) {
		return add(b, false);
	}

	/**
	 * Internal add method with optional return of the groups the object is assigned to.
	 *
	 * @param b the object being added
	 * @param bReturnList whether to return the list of groups the object is added to
	 * @return the list of groups the object was added to, or {@code null}
	 */
	private ArrayList<OAGroupBy> add(F b, boolean bReturnList) {
		if (b == null) {
			return null;
		}
		Object valueA = b.getProperty(propertyPath);

		ArrayList<OAGroupBy> al = null;

		if (valueA instanceof Hub) {
			Hub h = (Hub) valueA;
			for (int i = 0;; i++) {
				valueA = h.getAt(i);
				if (valueA == null) {
					break;
				}

				boolean bFound = false;
				for (OAGroupBy gb : HubGroupBy.this.getCombinedHub()) {
					if (gb.getGroupBy() != valueA) {
						continue;
					}
					if (bReturnList) {
						if (al == null) {
							al = new ArrayList<OAGroupBy>();
						}
						al.add(gb);
					}
					gb.getHub().add(b);
					bFound = true;
					break;
				}
				if (!bFound) {
					// create new
					OAGroupBy gbNewFound = createGroupBy((G) valueA);
					HubGroupBy.this.hubCombined.add(gbNewFound);
					gbNewFound.getHub().add(b);
					if (bReturnList) {
						if (al == null) {
							al = new ArrayList<OAGroupBy>();
						}
						al.add(gbNewFound);
					}
				}
			}

			// add to empty hub
			if (h.size() == 0 && bCreateNullList) {
				for (OAGroupBy gb : HubGroupBy.this.getCombinedHub()) {
					if (gb.getGroupBy() != null) {
						continue;
					}
					gb.getHub().add(b);
					if (bReturnList) {
						if (al == null) {
							al = new ArrayList<OAGroupBy>();
						}
						al.add(gb);
					}
					return al;
				}
				// create new
				OAGroupBy gb = createGroupBy(null);
				HubGroupBy.this.hubCombined.add(gb);
				gb.getHub().add(b);
				if (bReturnList) {
					if (al == null) {
						al = new ArrayList<OAGroupBy>();
					}
					al.add(gb);
				}
			}
		} else {
			if (!bCreateNullList && valueA == null) {
				return al;
			}
			for (OAGroupBy gb : HubGroupBy.this.getCombinedHub()) {
				if (gb.getGroupBy() != valueA) {
					continue;
				}
				gb.getHub().add(b);
				if (bReturnList) {
					if (al == null) {
						al = new ArrayList<OAGroupBy>();
					}
					al.add(gb);
				}
				return al;
			}

			// create new
			OAGroupBy<F, G> c = createGroupBy((G) valueA);
			HubGroupBy.this.hubCombined.add(c);
			c.getHub().add(b);
			if (bReturnList) {
				if (al == null) {
					al = new ArrayList<OAGroupBy>();
				}
				al.add(c);
			}
		}
		return al;
	}

	/**
	 * Removes the specified object from the group associated with the given groupBy key.
	 *
	 * @param a the groupBy key
	 * @param b the object to remove
	 */
	private void remove(G a, F b) {
		for (OAGroupBy gb : HubGroupBy.this.getCombinedHub()) {
			G ax = (G) gb.getGroupBy();
			if (ax != a) {
				continue;
			}
			Hub<F> h = gb.getHub();
			if (h.contains(b)) {
				h.remove(b);
				return;
			}
		}
	}

	/**
	 * Removes the specified object from all groups in which it appears and removes
	 * empty groups when appropriate.
	 *
	 * @param b the object to remove
	 */
	private void remove(F b) {
		for (OAGroupBy gb : getCombinedHub()) {
			Hub<F> h = gb.getHub();
			if (h.contains(b)) {
				h.remove(b);
				if (h.size() == 0) {
					if (hubGroupBy == null || !hubGroupBy.contains(gb.getGroupBy())) {
						hubCombined.remove(gb);
					}
				}
			}
		}
	}

	/**
	 * Re-evaluates the group membership of the specified object when the grouping
	 * property changes, adding it to new groups and removing it from old ones.
	 *
	 * @param b the object being updated
	 */
	private void update(F b) {
		ArrayList<OAGroupBy> al = add(b, true);
		for (OAGroupBy gb : getCombinedHub()) {
			Hub<F> h = gb.getHub();
			if (al != null) {
				if (al.contains(gb)) {
					continue;
				}
			}
			if (h.contains(b)) {
				h.remove(b);
			}
		}
	}

	/**
	 * Combines two HubGroupBy instances into a single grouped result set.
	 * <p>
	 * Installs listeners and merge logic so that changes in either source grouping
	 * propagate into the combined Hub.
	 *
	 * @param hgb1 the first HubGroupBy
	 * @param hgb2 the second HubGroupBy
	 */
	void setupCombined(HubGroupBy<F, G> hgb1, HubGroupBy<F, G> hgb2) {
		final Hub<OAGroupBy<F, G>> hub1 = hgb1.getCombinedHub();
		final Hub<OAGroupBy<F, G>> hub2 = hgb2.getCombinedHub();

		getCombinedHub();
		HubListener<OAGroupBy<F, G>> hl = new HubListenerAdapter<OAGroupBy<F, G>>() {
			/*
			Hub<OAGroupBy<F, G>> getOtherHub(HubEvent e) {
			    if (e.getSource() == hub1) return hub2;
			    return hub1;
			}
			*/

			@Override
			public void afterAdd(HubEvent<OAGroupBy<F, G>> e) {
				final OAGroupBy<F, G> gb = e.getObject();
				OAGroupBy gbFound = null;
				G a = (G) gb.getGroupBy();
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}

				if (gbFound == null) {
					gbFound = createGroupBy(a);
					HubGroupBy.this.getCombinedHub().add(gbFound);
				}
				for (OAObject obj : gb.getHub()) {
					gbFound.getHub().add(obj);
				}
			}

			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterRemove(HubEvent<OAGroupBy<F, G>> e) {
				final OAGroupBy<F, G> gb = e.getObject();
				OAGroupBy gbFound = null;
				G a = (G) gb.getGroupBy();

				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}
				if (gbFound == null) {
					return;
				}
				for (OAObject obj : gb.getHub()) {
					boolean bStillNeeded = false;
					for (OAGroupBy<F, G> gbx : hub1) {
						if (gbx.getGroupBy() == a) {
							if (gbx.getHub().contains(obj)) {
								bStillNeeded = true;
								break;
							}
						}
					}
					for (OAGroupBy<F, G> gbx : hub2) {
						if (gbx.getGroupBy() == a) {
							if (gbx.getHub().contains(obj)) {
								bStillNeeded = true;
								break;
							}
						}
					}
					if (!bStillNeeded) {
						gbFound.getHub().remove(obj);
					}
				}
			}

			void rebuild() {
				HubGroupBy.this.getCombinedHub().clear();
				OAGroupBy<F, G> gbFound = null;

				for (OAGroupBy<F, G> gb : hub1) {
					gbFound = createGroupBy(gb.getGroupBy());
					HubGroupBy.this.hubCombined.add(gbFound);
					for (F obj : gb.getHub()) {
						gbFound.getHub().add(obj);
					}
				}

				for (OAGroupBy<F, G> gb : hub2) {
					gbFound = null;
					G a = (G) gb.getGroupBy();
					for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
						if (c.getGroupBy() == a) {
							gbFound = c;
							break;
						}
					}
					if (gbFound == null) {
						gbFound = createGroupBy(a);
						HubGroupBy.this.hubCombined.add(gbFound);
					}
					for (F obj : gb.getHub()) {
						gbFound.getHub().add(obj);
					}
				}
			}

			@Override
			public void afterRemoveAll(HubEvent<OAGroupBy<F, G>> e) {
				rebuild();
			}

			@Override
			public void onNewList(HubEvent e) {
				rebuild();
			}
		};

		hub1.addHubListener(hl);
		hub2.addHubListener(hl);

		// set up hubMergers

		//qqqqqq this fails
		//Hub<F> hubTemp1 = new Hub<F>();
		//qq this works
		Hub<OAObject> hubTemp1 = new Hub<OAObject>(OAObject.class);
		HubMerger<OAGroupBy<F, G>, F> hm1 = new HubMerger<OAGroupBy<F, G>, F>(hub1, (Hub<F>) hubTemp1, OAGroupBy.P_Hub, true) {
			@Override
			protected void afterInsertRealHub(HubEvent e) {
				afterAddRealHub(e);
			}

			@Override
			protected void afterAddRealHub(HubEvent e) {
				final OAGroupBy<F, G> gb = (OAGroupBy<F, G>) e.getHub().getMasterObject();
				final F objAdd = (F) e.getObject();

				OAGroupBy<F, G> gbFound = null;
				G a = (G) gb.getGroupBy();
				if (a == null) {
					if (!bCreateNullList) {
						return;
					}
					// only add if its in the other hgb null
					for (OAGroupBy<F, G> gbx : hub2) {
						if (gbx.getGroupBy() == null) {
							if (!gbx.getHub().contains(objAdd)) {
								return;
							}
						}
					}
				}
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}

				if (gbFound == null) {
					gbFound = createGroupBy(a);
					HubGroupBy.this.getCombinedHub().add(gbFound);
				}
				gbFound.getHub().add(objAdd);
			}

			@Override
			protected void afterRemoveRealHub(HubEvent e) {
				OAGroupBy<F, G> gb = (OAGroupBy<F, G>) e.getHub().getMasterObject();
				F objRemove = (F) e.getObject();
				remove(gb, objRemove);
			}

			void remove(final OAGroupBy<F, G> gb, final F objRemove) {
				OAGroupBy<F, G> gbFound = null;
				final G a = (G) gb.getGroupBy();
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}
				if (gbFound == null) {
					return;
				}

				boolean bStillNeeded = false;
				if (a != null) {
					for (OAGroupBy<F, G> gbx : hub2) {
						if (gbx.getGroupBy() == a) {
							if (gbx.getHub().contains(objRemove)) {
								bStillNeeded = true;
								break;
							}
						}
					}
				}
				if (!bStillNeeded) {
					gbFound.getHub().remove(objRemove);
				}
			}

			private Object[] removeAllObjects;

			@Override
			protected void beforeRemoveAllRealHub(HubEvent e) {
				removeAllObjects = e.getHub().toArray();
			}

			@Override
			protected void afterRemoveAllRealHub(HubEvent e) {
				if (removeAllObjects == null) {
					return;
				}
				OAGroupBy gb1 = (OAGroupBy) e.getHub().getMasterObject();
				for (Object obj : removeAllObjects) {
					remove(gb1, (F) obj);
				}
				removeAllObjects = null;
			}

		};

		Hub<OAObject> hubTemp2 = new Hub<OAObject>(OAObject.class);
		HubMerger<OAGroupBy<F, G>, F> hm2 = new HubMerger<OAGroupBy<F, G>, F>(hub2, (Hub<F>) hubTemp2, OAGroupBy.P_Hub, true) {
			@Override
			protected void afterInsertRealHub(HubEvent e) {
				afterAddRealHub(e);
			}

			@Override
			protected void afterAddRealHub(HubEvent e) {
				final OAGroupBy<F, G> gb = (OAGroupBy<F, G>) e.getHub().getMasterObject();
				final F objAdd = (F) e.getObject();

				OAGroupBy<F, G> gbFound = null;
				G a = (G) gb.getGroupBy();
				if (a == null) {
					if (!bCreateNullList) {
						return;
					}
					// only add if its in the other hgb null
					for (OAGroupBy<F, G> gbx : hub2) {
						if (gbx.getGroupBy() == null) {
							if (!gbx.getHub().contains(objAdd)) {
								return;
							}
						}
					}
				}
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}

				if (gbFound == null) {
					gbFound = createGroupBy(a);
					HubGroupBy.this.hubCombined.add(gbFound);
				}
				gbFound.getHub().add(objAdd);
			}

			@Override
			protected void afterRemoveRealHub(HubEvent e) {
				OAGroupBy<F, G> gb = (OAGroupBy<F, G>) e.getHub().getMasterObject();
				F objRemove = (F) e.getObject();
				remove(gb, objRemove);
			}

			void remove(final OAGroupBy<F, G> gb, final F objRemove) {
				OAGroupBy<F, G> gbFound = null;
				final G a = (G) gb.getGroupBy();
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}
				if (gbFound == null) {
					return;
				}

				boolean bStillNeeded = false;
				if (a != null) {
					for (OAGroupBy<F, G> gbx : hub1) {
						if (gbx.getGroupBy() == a) {
							if (gbx.getHub().contains(objRemove)) {
								bStillNeeded = true;
								break;
							}
						}
					}
				}
				if (!bStillNeeded) {
					gbFound.getHub().remove(objRemove);
				}
			}

			private Object[] removeAllObjects;

			@Override
			protected void beforeRemoveAllRealHub(HubEvent e) {
				removeAllObjects = e.getHub().toArray();
			}

			@Override
			protected void afterRemoveAllRealHub(HubEvent e) {
				if (removeAllObjects == null) {
					return;
				}
				OAGroupBy gb1 = (OAGroupBy) e.getHub().getMasterObject();
				for (Object obj : removeAllObjects) {
					remove(gb1, (F) obj);
				}
				removeAllObjects = null;
			}
		};

		// initial load
		for (int i = 0; i < 2; i++) {
			Hub<OAGroupBy<F, G>> hub;
			if (i == 0) {
				hub = hub1;
			} else {
				hub = hub2;
			}
			for (OAGroupBy<F, G> gb : hub) {
				OAGroupBy<F, G> gbFound = null;
				G a = (G) gb.getGroupBy();
				if (!bCreateNullList && a == null) {
					continue;
				}
				for (OAGroupBy c : HubGroupBy.this.getCombinedHub()) {
					if (c.getGroupBy() == a) {
						gbFound = c;
						break;
					}
				}
				if (gbFound == null) {
					gbFound = createGroupBy(a);
					HubGroupBy.this.getCombinedHub().add(gbFound);
				}
				for (F obj : gb.getHub()) {
					gbFound.getHub().add(obj);
				}
			}
		}
	}

	/**
	 * Creates a new {@code OAGroupBy} instance for the specified groupBy key and
	 * initializes its internal Hub.
	 *
	 * @param grpBy the groupBy key, or {@code null} for a null-group list
	 * @return a newly created {@code OAGroupBy}
	 */
	private OAGroupBy<F, G> createGroupBy(G grpBy) {
		OAGroupBy<F, G> gb = new OAGroupBy<F, G>();
		if (grpBy != null) {
			gb.setGroupBy(grpBy);
		}
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubFrom);
		og.hubsInternal().callHubDataSetObjectClass(gb.getHub(), classFrom);

		// 20190418 if hubPropertyName!=null, then use a HubCopy
		if (OAString.isNotEmpty(hubPropertyName)) {
			new HubCopy(gb.getHub(), (Hub) grpBy.getProperty(hubPropertyName), false);
		}
		return gb;
	}

	/**
	 * Returns the property-path expression for the groupBy object class, or {@code null}
	 * if no explicit groupBy class has been determined.
	 *
	 * @return the property-path string or {@code null}
	 */
	public String getGroupByPP() {
		if (classGroupBy == null) {
			return null;
		}
		String s = "(" + classGroupBy.toString() + ")GroupBy";
		return s;
	}

	/**
	 * Returns the property-path expression for accessing grouped Hub contents.
	 *
	 * @return the property-path string for the Hub
	 */
	public String getHubByPP() {
		return "(" + classFrom.toString() + ")hub";
	}

}
