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
package com.viaoa.hub.listener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAMany;
import com.viaoa.compare.OACompare;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAStr;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.oa.sibling.OASiblingHelper;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.performance.OAPerformance;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Manages listeners for a root {@link Hub} and builds a shared dependency tree
 * for calculated properties, linked properties, and explicit dependent paths.
 * <p>
 * 
 * A Hub can listen not only to direct property changes on its active objects,
 * but also to calculated-property dependencies and dependent property paths.
 * This class expands those dependencies into a tree rooted at the Hub's object
 * type. Each tree node represents one segment of a dependent path and owns the
 * internal listeners, HubMergers, and child nodes needed to observe changes
 * below that point.
 * </p>
 * A {@code HubListenerTree} starts with one root Hub and expands each dependent
 * property path into a tree of reusable nodes. Paths that share a prefix reuse
 * the same nodes and supporting listeners.
 * </p>
 *
 * <pre>
 * Root Hub&lt;Order&gt;
 *
 * customer.address.city
 * customer.address.zip
 * items.product.price
 * items.product.cost
 *
 * becomes:
 *
 * Order
 * ├── customer
 * │   └── address
 * │       ├── city
 * │       └── zip
 * └── items
 *     └── product
 *         ├── price
 *         └── cost
 * </pre>
 *
 * <p>
 * This shared structure prevents each listener from independently constructing
 * duplicate listeners for the same path prefix. A node represents one property
 * segment beneath its parent node, so the identity of a node is determined by
 * both its property name and its location in the rooted dependency tree.
 * </p>
 *
 * <h2>Listener routing</h2>
 *
 * <p>
 * The tree installs the listeners and {@link HubMerger} instances needed to
 * observe changes below the root Hub. When a nested object, link, or Hub
 * membership changes, the tree determines which root objects are affected and
 * fires the corresponding calculated-property or dependent-property
 * notification on the root Hub.
 * </p>
 *
 * <p>
 * Reverse-link metadata is used when possible to navigate from the changed
 * object back to the affected root objects. When a usable reverse path is not
 * available, the implementation can fall back to Hub traversal or
 * {@link com.viaoa.find.OAFinder}-based lookup.
 * </p>
 *
 * <h2>Many-link paths</h2>
 *
 * <p>
 * A path segment that returns a Hub is tracked using {@link HubMerger}. The
 * merger maintains a derived Hub for the next tree node and allows the listener
 * tree to observe nested collections. Depending on listener configuration, the
 * tree can observe all objects in the path or only the active-object branch.
 * </p>
 *
 * <p>
 * Removal handling retains the most recent removed object and master object
 * because reverse references may already be cleared by the time an
 * after-remove event is delivered.
 * </p>
 *
 * <h2>Recursive dependency expansion</h2>
 *
 * <p>
 * The normal path-building algorithm is bounded: each property path contains a
 * finite number of segments, and existing child nodes are reused instead of
 * recreated. Recursive domain relationships are therefore not, by themselves,
 * a problem. A finite path such as {@code employee.manager.manager.name} can be
 * represented normally even when the underlying object model is recursive.
 * </p>
 *
 * <p>
 * Additional recursion can occur while registering a terminal property if that
 * property is itself calculated or declares further dependent properties. For
 * example, one calculated property can depend on another calculated property,
 * which can cause nested dependency registration.
 * </p>
 *
 * <p>
 * Thread-local tracking is used to avoid immediately rebuilding the same
 * dependent path, and a maximum nesting count provides a final safeguard
 * against invalid circular metadata such as:
 * </p>
 *
 * <pre>
 * propertyA depends on propertyB
 * propertyB depends on propertyA
 * </pre>
 *
 * <p>
 * These safeguards protect against circular dependency definitions. They are
 * not general object-graph visit tracking, because the listener tree itself
 * provides the canonical, shared structure for valid paths.
 * </p>
 *
 * <h2>Listener ownership and cleanup</h2>
 *
 * <p>
 * Dependent listeners created for an original listener are recorded at the
 * relevant tree nodes. Removing the original listener removes its generated
 * listeners recursively. Tree nodes and Hub mergers that are no longer used
 * are then removed and closed.
 * 
 * <p>
 * Listener ordering on the root Hub preserves
 * {@link HubListener.InsertLocation#FIRST},
 * normal, and {@link HubListener.InsertLocation#LAST} placement.
 * </p>
 *
 * <h2>Threading</h2>
 *
 * <p>
 * Tree construction and structural changes are synchronized using the root
 * node. Thread-local state is used while recursively registering dependent
 * listeners so nested listener creation can recognize the current construction
 * context. Some Hub mergers may optionally use a background thread, while
 * callers can require synchronous listener processing when necessary.
 * </p>
 *
 * @see HubListener
 * @see HubListenerAdapter
 * @see HubMerger
 * @see com.viaoa.path.OAPath
 * @see com.viaoa.metadata.OALinkInfo
 * @see com.viaoa.metadata.OACalcInfo
 */
public class HubListenerTree {
	private static Logger LOG = Logger.getLogger(HubListenerTree.class.getName());

	/**
	 * Ordered array of HubListeners registered on the root Hub, maintained with
	 * FIRST/NEXT/LAST placement rules.
	 */
	private volatile HubListener[] listeners;

	/**
	 * Root node of the listener dependency tree. Stores the root Hub and forms the
	 * entry point for routed dependent-property notifications.
	 */
	private final HubListenerTreeNode root = new HubListenerTreeNode();

	/**
	 * Global counter tracking the number of active HubListener instances registered
	 * across all HubListenerTree objects, used mainly for diagnostics.
	 */
	public static volatile int ListenerCount;

	/**
	 * Count of listeners explicitly added with InsertLocation.LAST, used to maintain
	 * correct ordering when inserting additional listeners.
	 */
	private volatile int lastCount; // number of listeners that are set as Last.



	private class HubListenerTreeNode {
		/**
		 * The Hub associated with this tree node, representing the collection on which
		 * dependent-property listeners are installed.
		 */
		Hub hub;

		/**
		 * The property name representing the segment of the dependent property path
		 * leading to this node.
		 */
		String property;

		/**
		 * HubMerger used to track nested collections when the dependent path includes
		 * a many-link or Hub-returning property.
		 */
		HubMerger hubMerger;

		/**
		 * Child nodes representing the next segments in the dependent-property path.
		 */
		HubListenerTreeNode[] children;

		/**
		 * Reference to the parent tree node, used to compute root values and navigate
		 * upward in the dependency tree.
		 */
		HubListenerTreeNode parent;

		/**
		 * Mapping of original HubListeners to the dependent listeners created for them
		 * within this subtree node.
		 */
		HashMap<HubListener, HubListener[]> hmListener; // list of HubListeners created for a HubListener

		/**
		 * Cached reverse-link information for the property represented by this node,
		 * used to compute root values for dependent-property notifications.
		 */
		private OALinkInfo liReverse;

		/**
		 * List of calculated property names dependent on this node’s portion of the path.
		 */
		private ArrayList<String> alCalcPropertyNames;

		/**
		 * Returns the CalcPropertyNames value.
		 *
		 * @return the CalcPropertyNames value
		 */
		public ArrayList<String> getCalcPropertyNames() {
			if (alCalcPropertyNames == null) {
				alCalcPropertyNames = new ArrayList<String>(3);
			}
			return alCalcPropertyNames;
		}

		// when an object is removed from a hub, the parent property reference could already be null.
		//    this will use the masterObject in the hub.
		//   note: if an object is deleted, it is done on the server and the removed object's parent reference will be null during the remove.
		/**
		 * Stores the object removed during the most recent hub.remove event on this node,
		 * used when parent references may already be cleared.
		 */
		Object lastRemoveObject; // object from last hub.remove event

		/**
		 * The master object associated with the most recent remove event, used when
		 * reconstructing the root-object notification targets.
		 */
		Object lastRemoveMasterObject; // master object from last hub.remove event

		/*
		 *  This allows getting all of the root objects that need to be notified when a change is made to an object "down" the tree from it.
		*/

		/**
		 * Computes the set of root Hub objects that should receive calc-property
		 * change notifications based on a change occurring at a deeper level in
		 * the dependency tree.
		 *
		 * <p>This method walks upward through the HubListenerTreeNode hierarchy,
		 * assembling a reverse property-path (when possible) using link metadata.
		 * If the reverse links are valid, the method attempts to navigate from the
		 * changed object back toward the root Hub. If reverse-link resolution is
		 * not possible—or link metadata is incomplete—it falls back to the original
		 * algorithm or to a finder-based lookup.</p>
		 *
		 * <p>The method evaluates whether a reverse path can be used, then selects
		 * one of three strategies:</p>
		 * <ul>
		 *   <li>Use the original upward-walk algorithm via {@code getRootValues_ORIG()}</li>
		 *   <li>Use a fallback finder-based search when the reverse path is not valid</li>
		 *   <li>Use a property-path based match for a single-level dependency case</li>
		 * </ul>
		 *
		 * @param obj the object where the change originated; this may be a detail object
		 *            deep within the dependency tree.
		 *
		 * @return an array of root-level objects that should receive the routed
		 *         calc-property notification; never {@code null}, though it may
		 *         be empty when no valid roots are found.
		 */
		Object[] getRootValues(final Object obj) {
			// 20171212 reworked to include option to use a finder
			long ts = System.currentTimeMillis();
			String spp = null;
			HubListenerTreeNode tn = this;
			for (; tn != null && tn.parent != null;) {
				// 20180531
				if (tn.liReverse == null) {
					Class c = tn.parent.hub.getObjectClass();
	        		final OA oa = OARuntime.oa(c);
					OALinkInfo li = oa.internal().objects().info().getLinkInfo(c, tn.property);
					tn.liReverse = oa.internal().objects().info().getReverseLinkInfo(li);
				}
				if (tn.liReverse == null || tn.liReverse.getReverseLinkInfo() == null) {
					spp = null;
					break;
				}
				if (tn.property != null) {
					if (spp == null) {
						spp = tn.property;
					} else {
						spp = tn.property + "." + spp;
					}
				}
				tn = tn.parent;
			}

			boolean bUseOrig = true;
			OAPath pp = null;
			if (spp != null) {
				pp = new OAPath(HubListenerTree.this.root.hub.getObjectClass(), spp);
				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis != null && lis.length > 0) {
					bUseOrig = false;
					for (OALinkInfo li : lis) {
						if (li == null || li.getReverseLinkInfo() == null) {

							bUseOrig = true;
							break;
						}
						if (li.getType() == OALinkInfo.TYPE_MANY) {
							bUseOrig = true;
							break;
						}
					}
				}
			}

			Object[] objs;
			if (bUseOrig) {
				objs = getRootValues_ORIG(obj, (spp != null));
			} else {
				objs = null;
			}
			if (objs == null && spp != null) {
				// 20200407
				List al = null;
				if (pp != null) {
					OALinkInfo[] lis = pp.getLinkInfos();
					if (lis != null && lis.length == 1 && pp.getEndLinkInfo() != null) {
						al = new ArrayList();
						for (final Object obja : HubListenerTree.this.root.hub) {
							Object objz;
							if (lis[0].getCalculated()) {
								objz = lis[0].getValue((OAObject) obja);
							} else {
				        		final OA oa = OARuntime.oa((OAObject) obja);
								objz = oa.internal().objects().property().getProperty((OAObject) obja, lis[0].getName());
							}
							if (OACompare.isEqual(obj, objz)) {
								al.add(obja);
							}
						}
					}
				}

				if (al == null) {
					OAFinder finder = new OAFinder();
					finder.addEqualFilter(spp, obj);
					al = finder.find(HubListenerTree.this.root.hub);
				}
				objs = new Object[al.size()];
				al.toArray(objs);
			}

			long ts2 = System.currentTimeMillis();
			if ((ts2 - ts) > 1000) {
				OAPerformance.LOG.fine("fyi: getRootValues took " + (ts2 - ts) + "ms, rootHub=" + HubListenerTree.this.root.hub
						+ ", propPath=" + spp);
			}
			return objs;
		}


		Object[] getRootValues_ORIG(Object obj, boolean bCanQuit) {
			if (obj == null) {
				return new Object[0];
			}

			Object[] objs = getRootValues(new Object[] { obj });

			// now make sure that all of the values are in the root.hub
			int cnt = 0;
			for (int i = 0; objs != null && i < objs.length; i++) {
				if (!root.hub.contains(objs[i])) {
					objs[i] = null;
				} else {
					cnt++;
				}
			}
			if (cnt == 0) {
				return null;
			}

			if (cnt == objs.length) {
				return objs;
			}

			Object[] newObjs = new Object[cnt];
			int j = 0;
			for (int i = 0; i < objs.length; i++) {
				if (objs[i] != null) {
					newObjs[j++] = objs[i];
				}
			}
			return newObjs;
		}

		/**
		 * Recursively walks up the HubListenerTreeNode hierarchy to compute the
		 * root-level objects that correspond to the supplied objects at this
		 * node in the dependency tree.
		 *
		 * <p>The method evaluates each object and determines its parent objects
		 * based on reverse-link metadata, direct property references, or Hub
		 * membership. For Hub-valued properties, all referenced children (or
		 * the active object, depending on HubMerger settings) are included.
		 * For OAObject-valued properties, the referenced parent object is added
		 * when available.</p>
		 *
		 * <p>Once all parent objects for this node are collected, the method
		 * recursively calls the parent node’s {@code getRootValues(Object[])}
		 * until the root node is reached. The final resulting array represents
		 * the complete set of root objects that should receive routed
		 * calc-property notifications for a change occurring at or below this
		 * node.</p>
		 *
		 * @param objs the objects at the current node for which parent/root
		 *             objects are being resolved; may be empty but not null.
		 *
		 * @return an array of root-level objects associated with the given
		 *         lower-level objects; never {@code null}, but may be empty.
		 */
		private Object[] getRootValues(Object[] objs) {
			if (parent == null) {
				return objs; // reached the root
			}
			if (objs == null) {
				return new Object[0];
			}

			if (liReverse == null) {
				Class c = parent.hub.getObjectClass();
        		final OA oa = OARuntime.oa(c);
				OALinkInfo li = oa.internal().objects().info().getLinkInfo(c, property);
				liReverse = oa.internal().objects().info().getReverseLinkInfo(li);
			}

			ArrayList<Object> alNewObjects = new ArrayList<Object>();

			Method m = null;
			for (Object obj : objs) {
				OAObject oaObj = (OAObject) obj;

				String propName = null;
				if (liReverse != null) {
					propName = liReverse.getName();
	        		final OA oa = OARuntime.oa(oaObj);
					OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(oaObj);
					m = oa.internal().objects().info().getMethod(oi, "get" + propName, 0);
				}

				if (oaObj == lastRemoveObject && lastRemoveMasterObject != null) {
					// from a remove
					if (alNewObjects.indexOf(lastRemoveMasterObject) < 0) {
						alNewObjects.add(lastRemoveMasterObject);
					}
					// 20190120 removed,could be called more than once during a remove
					// lastRemoveObject = null;
				} else if (m == null) {
					// method might not exist (or is private - from a reference that is not made accessible)
					// need to go up to parent to find all objects that have a reference to "obj"

					for (Object objx : parent.hub) {
		        		final OA oa = OARuntime.oa((OAObject) objx);
						Object objz = oa.internal().objects().reflect().getProperty((OAObject) objx, this.property);
						if (objz == obj || lastRemoveObject == obj) {
							// found a parent object that has a reference to child
							if (alNewObjects.indexOf(objx) < 0) {
								alNewObjects.add(objx);
							}
						} else if (objz instanceof Hub) {
							if (((Hub) objz).contains(obj)) {
								// found a parent object that has a reference to child
								if (alNewObjects.indexOf(objx) < 0) {
									alNewObjects.add(objx);
								}
							}
						}
					}
				} else {
					Object value = null;
					try {
						value = m.invoke(oaObj, (Object[]) null);
					} catch (Exception e) {
						LOG.log(Level.FINE, "error calling " + oaObj.getClass().getName() + ".getProperty(\"" + propName + "\")", e);
					}

					if (value instanceof Hub) {
						// 20160805
						if (root.hubMerger != null && !root.hubMerger.getUseAll()) {
							Object objx = root.hubMerger.getRootHub().getAO();
							if (objx != null && alNewObjects.indexOf(objx) < 0) {
								alNewObjects.add(objx);
							}
						} else {
							for (Object objx : ((Hub) value)) {
								if (alNewObjects.indexOf(objx) < 0) {
									alNewObjects.add(objx);
								}
							}
						}
					} else {
						if (value != null) {
							if (alNewObjects.indexOf(value) < 0) {
								alNewObjects.add(value);
							}
						}
					}
				}
			}
			objs = alNewObjects.toArray();
			objs = parent.getRootValues(objs);

			return objs;
		}
	}

	/**
	 * Creates a Hub helper instance.
	 */
	public HubListenerTree(Hub hub) {
		root.hub = hub;
	}

	/**
	 * Returns the HubListeners value.
	 *
	 * @return the HubListeners value
	 */
	public HubListener[] getHubListeners() {
		return this.listeners;
	}

	/**
	 * Adds a Hub listener registration.
	 * @param hl the listener parameter
	 */
	public void addListener(HubListener hl) {
		if (hl == null) {
			return;
		}

		// testing
		ListenerCount++;
		//if (ListenerCount%100==0)
		//        System.out.println("HubListenerTree.addListener, ListenerCount="+ListenerCount+", hl="+hl);
		//System.out.println("HubListenerTree.addListener, ListenerCount="+ListenerCount+", AutoSequenceHubListenerCount="+HubAutoSequence.AutoSequenceHubListenerCount+" ==>"+hl);
		//System.out.println("HubListenerTree.addListener, ListenerCount="+ListenerCount+" ==>"+hl+", hm.hl.cnt="+HubMerger.HubMergerHubListenerCount);
		// System.out.println("HubListenerTree.addListener() ListenerCount="+(ListenerCount));
		// StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		// hmAll.put(hl, stes);

		synchronized (root) {
			HubListener.InsertLocation loc = hl.getLocation();
			if (listeners == null || listeners.length == 0 || loc == HubListener.InsertLocation.LAST || (loc == null && lastCount == 0)) {
				if (loc == HubListener.InsertLocation.LAST) {
					lastCount++;
				}
				if (listeners == null || OAArray.indexOf(listeners, hl) < 0) {
					listeners = (HubListener[]) OAArray.add(HubListener.class, listeners, hl);
				}
			} else if (loc == HubListener.InsertLocation.FIRST) {
				listeners = (HubListener[]) OAArray.removeValue(HubListener.class, listeners, hl);
				listeners = (HubListener[]) OAArray.insert(HubListener.class, listeners, hl, 0);
			} else {
				// insert before first last
				boolean b = false;
				for (int i = listeners.length - 1; i >= 0; i--) {
					if (listeners[i].getLocation() != HubListener.InsertLocation.LAST) {
						if (OAArray.indexOf(listeners, hl) < 0) {
							listeners = (HubListener[]) OAArray.insert(HubListener.class, listeners, hl, i + 1);
						}
						b = true;
						break;
					}
				}
				if (!b) {
					if (listeners == null || OAArray.indexOf(listeners, hl) < 0) {
						listeners = (HubListener[]) OAArray.add(HubListener.class, listeners, hl);
					}
				}
			}
			if (listeners.length % 50 == 0) {
				LOG.fine("HubListenerTree.listeners.size()=" + listeners.length + ", hub=" + (root == null ? "null" : root.hub));
			}
		}
	}

	/**
	 * Used by Hub to store HubListers and dependent calcProperties
	 */
	public void addListener(HubListener hl, String property) {
		if (hl == null) {
			return;
		}
		this.addListener(hl, property, false);
	}

	/**
	 * Adds a Hub listener registration.
	 * @param hl the listener parameter
	 * @param property the listener parameter
	 * @param bActiveObjectOnly the listener parameter
	 */
	public void addListener(HubListener hl, String property, boolean bActiveObjectOnly) {
		if (hl == null) {
			return;
		}
		final OA oa = OARuntime.oa(root.hub);
		OAObjectInfo oi = oa.internal().objects().info().getObjectInfo(root.hub.getObjectClass());
		String[] calcProps = null;
		for (OACalcInfo ci : oi.getCalcInfos()) {
			if (ci.getName().equalsIgnoreCase(property)) {
				// System.out.println(">>>> "+property);
				calcProps = ci.getDependentProperties();
				property = ci.getName();
				break;
			}
		}
		if (calcProps == null) {
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getName().equalsIgnoreCase(property)) {
					continue;
				}

				calcProps = li.getCalcDependentProperties();
				property = li.getName();

				// 20221011
				if (li.getType() == OALinkInfo.MANY) {
					/* 20250924 removed, causes stackoverflow if there are dependentProps
					if (calcProps == null) {
						calcProps = new String[] { property };
					} else {
						calcProps = OAArray.add(calcProps, property);
					}
					*/
				}
				break;
			}
		}
		addListenerMain(hl, property, calcProps, bActiveObjectOnly, false);
	}

	/**
	 * Adds a Hub listener registration.
	 * @param hl the listener parameter
	 * @param property the listener parameter
	 * @param dependentPaths the listener parameter
	 */
	public void addListener(HubListener hl, final String property, String[] dependentPaths) {
		if (hl == null) {
			return;
		}
		addListener(hl, property, dependentPaths, false);
	}

	/**
	 * Adds a Hub listener registration.
	 * @param hl the listener parameter
	 * @param property the listener parameter
	 * @param dependentPaths the listener parameter
	 * @param bActiveObjectOnly the listener parameter
	 */
	public void addListener(HubListener hl, final String property, String[] dependentPaths, boolean bActiveObjectOnly) {
		addListener(hl, property, dependentPaths, bActiveObjectOnly, false);
	}

	/**
	 * Adds a Hub listener registration.
	 * @param hl the listener parameter
	 * @param bActiveObjectOnly the listener parameter
	 */
	public void addListener(HubListener hl, boolean bActiveObjectOnly) {
		addListener(hl, null, null, bActiveObjectOnly, false);
	}

	/**
	 * Adds a Hub listener registration.
	 */
	public void addListener(HubListener hl, final String property, String[] dependentPaths, boolean bActiveObjectOnly, boolean bAllowBackgroundThread) {
		if (hl == null) return;
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();

		final String holdProp = srvcOAThreadLocal.getIgnoreTreeListenerProperty();
		try {
			srvcOAThreadLocal.setHubListenerTree(true);
			addListener(hl, property, bActiveObjectOnly); // this will check for dependent calcProps
			// now add the additional dependent properties
			if (dependentPaths != null && dependentPaths.length > 0) {
				addDependentListeners(property, hl, dependentPaths, bActiveObjectOnly, bAllowBackgroundThread);
			}
		} finally {
			srvcOAThreadLocal.setHubListenerTree(false);
			srvcOAThreadLocal.setIgnoreTreeListenerProperty(holdProp);
		}
	}

	/**
	 * @param dependentPaths
	 * @param bActiveObjectOnly      if true, then dependent props only listen to the hub's AO
	 */
	private void addListenerMain(HubListener hl, final String property, String[] dependentPaths, boolean bActiveObjectOnly,
			final boolean bAllowBackgroundThread) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
		final String holdProp = srvcOAThreadLocal.getIgnoreTreeListenerProperty();
		try {
			srvcOAThreadLocal.setHubListenerTree(true);
			this.addListener(hl);

			if (dependentPaths != null && dependentPaths.length > 0) {
				synchronized (root) { // 20200401
					addDependentListeners(property, hl, dependentPaths, bActiveObjectOnly, bAllowBackgroundThread);
				}
			}
		} finally {
			srvcOAThreadLocal.setHubListenerTree(false);
			srvcOAThreadLocal.setIgnoreTreeListenerProperty(holdProp);
		}
	}

	private void addDependentListeners(final String origPropertyName, final HubListener origHubListener,
			final String[] dependentPropertyNames, 
			final boolean bActiveObjectOnly, 
			final boolean bAllowBackgroundThread) {

		//LOG.finer("Hub="+root.hub+", property="+origPropertyName);

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
		// 20120826 check for endless loops
		if (srvcOAThreadLocal.getHubListenerTreeCount() > 25) {
			// need to bail out, before stackoverflow
			LOG.log(Level.WARNING, "srvcOAThreadLocal.getHubListenerTreeCount() > 25, will not continue to add listeners. PropertyName="
					+ origPropertyName, new Exception("detected possible overflow, will continue"));
			return;
		}

		final String ignoreHold = srvcOAThreadLocal.getIgnoreTreeListenerProperty();
		for (int i = 0; i < dependentPropertyNames.length; i++) {
			if (OAStr.isEmpty(dependentPropertyNames[i])) continue;

			srvcOAThreadLocal.setIgnoreTreeListenerProperty(ignoreHold);
			
			
			// 20120826 if recursive prop then dont need to listen to more, since a hubMerger is already listening
			if (ignoreHold != null && dependentPropertyNames[i].equalsIgnoreCase(ignoreHold)) {
				// todo: might want to have a better check.  This will only check to see if a recursive property
				//   has the same dependency.  This might be good enough, since there is also a check (begin of method) for endless loop
				//LOG.fine("ignoring "+dependentPropertyNames[i]+", since it was already being listened to");
				continue;
			}
			if (dependentPropertyNames[i].indexOf('.') > 0) {
				srvcOAThreadLocal.setIgnoreTreeListenerProperty(dependentPropertyNames[i]);
			}

			HubListenerTreeNode node = root;
			Hub hub = root.hub;

			final String dependPropName = dependentPropertyNames[i];
			final OAPath oaPropPath = new OAPath(dependPropName);
			try {
				String error = oaPropPath.setup(hub, hub.getObjectClass(), false);
				if (error != null) {
					if (oaPropPath.getNeedsDataToVerify()) {
						// 20150715 propPath is using generics and will have to be retried once data is in it.
						//    this will now set up a listener to try again
						final HubListener hl = new HubListenerAdapter() {
							/**
							 * Handles the Hub after-add event.
							 * @param e the Hub event
							 */
							public void afterAdd(HubEvent e) {
								update();
							}

							/**
							 * Handles the Hub after-insert event.
							 * @param e the Hub event
							 */
							public void afterInsert(HubEvent e) {
								update();
							}

							/**
							 * Handles replacement or refresh of the Hub list.
							 * @param e the Hub event
							 */
							public void onNewList(HubEvent e) {
								Hub h = e.getHub();
								if (h != null && h.size() > 0) {
									update();
								}
							}

							void update() {
								try {
									removeListener(this);
									final String holdProp = srvcOAThreadLocal.getIgnoreTreeListenerProperty();
									srvcOAThreadLocal.setHubListenerTree(true);
									try {
										addDependentListeners(	origPropertyName, origHubListener, new String[] { dependPropName },
											bActiveObjectOnly, bAllowBackgroundThread);
									}
									finally {
										srvcOAThreadLocal.setIgnoreTreeListenerProperty(holdProp);
										srvcOAThreadLocal.setHubListenerTree(false);
									}
								} catch (Exception e) {
									return;
								}
							}
						};
						this.addListener(hl);
						continue;
					}
				}
			} catch (Exception e) {
				String s = ("cant find dependent prop, hub=" + hub + ", prop=" + origPropertyName + ", dependendProp="
						+ dependentPropertyNames[i]);
				LOG.warning(s);
				throw new RuntimeException(s, e);
			}
			if (oaPropPath.hasPrivateLink()) {
				String s = ("propPath has private method, hub=" + hub + ", prop=" + origPropertyName + ", dependendProp="
						+ dependentPropertyNames[i]);
				LOG.warning(s);
				throw new RuntimeException(s);
			}

			String[] pps = oaPropPath.getProperties();
			Method[] methods = oaPropPath.getMethods();
			Class[] classes = oaPropPath.getClasses();

			for (int j = 0; j < pps.length; j++) {
				final String property = pps[j];

				Class c = hub.getObjectClass();
				Method m = methods[j];
				Class returnClass = m.getReturnType();
				Class hubClass;
				boolean bIsHub = false;

				if (OAObject.class.isAssignableFrom(returnClass)) {
					if (j == pps.length - 1) {
						hubClass = null;
					} else {
						hubClass = classes[j];
					}
				} else if (Hub.class.isAssignableFrom(returnClass)) {
					bIsHub = true;
					hubClass = classes[j];
					if (Hub.class.equals(hubClass)) {
						OAMany om = m.getAnnotation(OAMany.class);
						if (om != null) {
			        		final OA oa = OARuntime.oa(hubClass);
							hubClass = oa.internal().objects().annotation().getHubObjectClass(om, m);
						} else {
							String s = ("getAnnotation OAMany=null for prop method=get" + property + ", hub=" + hub + ", prop="
									+ origPropertyName + ", dependendProp=" + dependentPropertyNames[i]);
							LOG.warning(s);
							throw new RuntimeException(s);
						}
					}
				} else {
					if (j != pps.length - 1) {
						String s = ("expected a reference prop, method=get" + property + ", hub=" + hub + ", prop=" + origPropertyName
								+ ", dependendProp=" + dependentPropertyNames[i]);
						LOG.warning(s);
						throw new RuntimeException(s);
					}
					hubClass = null;
				}

				final boolean bUseAll = (!bActiveObjectOnly || j > 0);

				String ppx = "";
				for (int k = 0; k <= j; k++) {
					// 20190307 added class check
					Class cx = methods[k].getReturnType();
					if (cx.equals(OAObject.class) && !classes[k].equals(cx)) {
						ppx = "(" + classes[k].getName() + ")";
					}
					if (k > 0) {
						ppx += ".";
					}
					ppx += pps[k];
				}
				final String ppFromRoot = ppx;

				if (hubClass != null) {
					boolean b = false;
					for (int k = 0; node.children != null && k < node.children.length; k++) {
						HubListenerTreeNode child = node.children[k];
						if (property.equalsIgnoreCase(child.property)) {
							if (j == 0 && (node.hubMerger != null) && (!bActiveObjectOnly != node.hubMerger.getUseAll())) {
								continue;
							}
							node = child;
							b = true;
							break;
						}
					}

					if (b) {
//qqqqqqqqqq needs to check case-insensitive						
						if (node.getCalcPropertyNames().indexOf(origPropertyName) < 0) {
							synchronized (node.getCalcPropertyNames()) {
								node.getCalcPropertyNames().add(origPropertyName);
							}
						}
						if (!bAllowBackgroundThread) {
							node.hubMerger.setUseBackgroundThread(false);
						}
					} else {
						//LOG.finer("creating hubMerger");
						final HubListenerTreeNode newTreeNode = new HubListenerTreeNode();
						newTreeNode.parent = node;
						newTreeNode.property = property;
						newTreeNode.hub = new Hub(hubClass);
						synchronized (newTreeNode.getCalcPropertyNames()) {
							newTreeNode.getCalcPropertyNames().add(origPropertyName);
						}

						String spp = "(" + hubClass.getName() + ")" + property;

						if (bIsHub) {
							final HubListenerTreeNode nodeThis = node;
							OAPerformance.LOG.finer("creating hubMerger for hub=" + hub + ", propPath=" + spp);

							newTreeNode.hubMerger = new HubMerger(hub, newTreeNode.hub, spp, true, bUseAll) {
								private OASiblingHelper siblingHelper;

								@Override
								/**
								 * Returns the SiblingHelper value.
								 *
								 * @return the SiblingHelper value
								 */
								public OASiblingHelper getSiblingHelper() {
									if (siblingHelper == null) {
										siblingHelper = new OASiblingHelper<>(HubListenerTree.this.root.hub);
										siblingHelper.add(ppFromRoot);
									}
									return siblingHelper;
								}

								@Override
								/**
								 * Handles the beforeRemoveRealHub event.
								 * @param e the Hub event
								 */
								protected void beforeRemoveRealHub(HubEvent e) {
									// get the parent reference object from the Hub.masterObject, since the
									//    reference in the object could be null once the remove is done
									Hub h = e.getHub();
									newTreeNode.lastRemoveObject = e.getObject();
									newTreeNode.lastRemoveMasterObject = h.getMasterObject();
									super.beforeRemoveRealHub(e);
								}

								@Override
								/**
								 * Handles the afterAddRealHub event.
								 * @param e the Hub event
								 */
								protected void afterAddRealHub(HubEvent e) {
									super.afterAddRealHub(e);
									onEvent(e);

								}

								@Override
								/**
								 * Handles the afterRemoveRealHub event.
								 * @param e the Hub event
								 */
								protected void afterRemoveRealHub(HubEvent e) {
									super.afterRemoveRealHub(e);
									onEvent(e);
								}

								@Override
								/**
								 * Handles the afterRemoveAllRealHub event.
								 * @param e the Hub event
								 */
								protected void afterRemoveAllRealHub(HubEvent e) {
									super.afterRemoveAllRealHub(e);
									onEvent(e);
								}

								private void onEvent(HubEvent e) {
									final OA oa = OARuntime.oa(root.hub);
									if (nodeThis == root) {
										for (String s : newTreeNode.getCalcPropertyNames()) {
											oa.internal().hubs().events().fireCalcPropertyChange(root.hub, e.getHub().getMasterObject(), s);
										}
									} else {
										if (bUseAll) {
											Object[] rootObjects = nodeThis.getRootValues(e.getHub().getMasterObject());
											if (rootObjects != null && rootObjects.length > 0) {
												for (Object obj : rootObjects) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>) root.hub, (OAObject) obj, s);
													}
												}
											}
										} else {
											Object aObj = root.hub.getAO();
											if (aObj != null) {
												Object[] rootObjects = nodeThis.getRootValues(e.getHub().getMasterObject());
												if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>) root.hub, (OAObject)aObj, s);
													}
												}
											}
										}
									}
								}
							};
							newTreeNode.hubMerger.setUseBackgroundThread(bAllowBackgroundThread);
						} else {
							if (OAObject.class.isAssignableFrom(returnClass)) {
								HubListenerAdapter hl = new HubListenerAdapter() {
									@Override
									/**
									 * Handles the Hub property-change event.
									 * @param e the Hub event
									 */
									public void afterPropertyChange(HubEvent e) {
										if (!property.equalsIgnoreCase(e.getPropertyName())) {
											return;
										}

										if (bUseAll) {
											Object[] rootObjects = newTreeNode.parent.getRootValues(e.getObject());
											if (rootObjects != null && rootObjects.length > 0) {
												final OA oa = OARuntime.oa(rootObjects[0].getClass());
												for (Object obj : rootObjects) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, s);
													}
												}
											}
										} else {
											Object aObj = root.hub.getAO();
											if (aObj != null) {
												final OA oa = OARuntime.oa(aObj.getClass());
												Object[] rootObjects = newTreeNode.parent.getRootValues(e.getObject());
												if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)aObj, s);
													}
												}
											}
										}
									}
								};
								hub.addHubListener(hl);

								HubListener[] hls;
								if (node.hmListener == null) {
									node.hmListener = new HashMap<HubListener, HubListener[]>(3, .75f);
									hls = null;
								} else {
									hls = node.hmListener.get(origHubListener);
								}

								hls = (HubListener[]) OAArray.add(HubListener.class, hls, hl);
								node.hmListener.put(origHubListener, hls);
							}

							OAPerformance.LOG.finer("creating hubMerger for hub=" + hub + ", propPath=" + spp);
							newTreeNode.hubMerger = new HubMerger(hub, newTreeNode.hub, spp, true, bUseAll) {
								OASiblingHelper siblingHelper;

								@Override
								/**
								 * Returns the SiblingHelper value.
								 *
								 * @return the SiblingHelper value
								 */
								public OASiblingHelper getSiblingHelper() {
									if (siblingHelper == null) {
										siblingHelper = new OASiblingHelper<>(HubListenerTree.this.root.hub);
										siblingHelper.add(ppFromRoot);
									}
									return siblingHelper;
								}
							};
							newTreeNode.hubMerger.setUseBackgroundThread(bAllowBackgroundThread);
						}

						node.children = (HubListenerTreeNode[]) OAArray.add(HubListenerTreeNode.class, node.children, newTreeNode);
						node = newTreeNode;
					}
					hub = node.hub;

					boolean bx; // might need to have a listener for last hub in path

					if (j == pps.length - 1) {
						bx = true;
					} else {
						bx = false;
						if (j == pps.length - 2) {
							// need to know if the last property is oaObj or Hub.  If not, then create a listener on this node
							Class cx = hub.getObjectClass();
							Method mx = methods[j + 1];
							if (mx != null) {
								cx = mx.getReturnType();
								if (cx == null || (!OAObject.class.isAssignableFrom(cx) && !Hub.class.isAssignableFrom(cx))) {
									bx = true;
								}
							}
						}
					}

					if (bx) {
						HubListener hl;
						final HubListenerTreeNode nodeThis = node;
						//LOG.finer("creating dependent prop hubListner for Hub");
						hl = new HubListenerAdapter() {
							@Override
							/**
							 * Handles the Hub after-add event.
							 * @param e the Hub event
							 */
							public void afterAdd(HubEvent e) {
								if (!srvcOAThreadLocal.isHubMergerChanging()) {
									Hub h = HubListenerTree.this.root.hub;
									if (bUseAll) {
										onEvent(nodeThis.getRootValues(e.getObject()));
									} else {
										Object aObj = root.hub.getAO();
										if (aObj != null) {
											Object[] rootObjects = nodeThis.getRootValues(e.getObject());
											if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
												onEvent(new Object[] { aObj });
											}
										}
									}
								}
							}

							@Override
							/**
							 * Handles the Hub after-insert event.
							 * @param e the Hub event
							 */
							public void afterInsert(HubEvent e) {
								afterAdd(e);
							}

							// 20190120
							@Override
							/**
							 * Handles the Hub before-remove event.
							 * @param e the Hub event
							 */
							public void beforeRemove(HubEvent e) {
								Hub h = HubListenerTree.this.root.hub;
								// get the parent reference object from the Hub.masterObject, since the
								//    reference in the object could be null
								Hub hubx = e.getHub();
								Object objx = hubx.getMasterObject();
								if (objx != null) {
									nodeThis.lastRemoveObject = e.getObject();
									nodeThis.lastRemoveMasterObject = objx;
								}
							}

							@Override
							/**
							 * Handles the Hub after-remove event.
							 * @param e the Hub event
							 */
							public void afterRemove(HubEvent e) {
								// ignore if masterHub is adding, removing (newList, clear)
								if (!srvcOAThreadLocal.isHubMergerChanging()) {
									if (bUseAll) {
										onEvent(nodeThis.getRootValues(e.getObject()));
									} else {
										Object aObj = root.hub.getAO();
										if (aObj != null) {
											Object[] rootObjects = nodeThis.getRootValues(e.getObject());
											if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
												onEvent(new Object[] { aObj });
											}
										}
									}
								}
							}

							@Override // 20140423
							/**
							 * Handles the Hub after-remove-all event.
							 * @param e the Hub event
							 */
							public void afterRemoveAll(HubEvent e) {
								if (!srvcOAThreadLocal.isHubMergerChanging()) {
									final OA oa = OARuntime.oa(root.hub);
									oa.internal().hubs().events().fireCalcPropertyChange(root.hub, null, origPropertyName);
								}
							}

							private void onEvent(Object[] rootObjects) {
								if (rootObjects == null) {
									return;
								}
								final OA oa = OARuntime.oa(root.hub);
								for (Object obj : rootObjects) {
									if (obj != null) {
										oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, origPropertyName);
									}
								}
							}
						};
						hub.addHubListener(hl);

						HubListener[] hls;
						if (node.hmListener == null) {
							node.hmListener = new HashMap<HubListener, HubListener[]>(3, .75f);
							hls = null;
						} else {
							hls = node.hmListener.get(origHubListener);
						}

						hls = (HubListener[]) OAArray.add(HubListener.class, hls, hl);
						node.hmListener.put(origHubListener, hls);
					}
				}
				if (j != pps.length - 1) {
					continue;
				}

				// Add a hub listener to end of path

				if (hubClass == null) {
					//LOG.finer("creating dependent prop hubListener, dependProp="+property);
					final String propx = property;
					final HubListenerTreeNode nodeThis = node;
					HubListener hl = new HubListenerAdapter() {
						@Override
						/**
						 * Handles the Hub property-change event.
						 * @param e the Hub event
						 */
						public void afterPropertyChange(HubEvent e) {
							String prop = e.getPropertyName();
							if (prop == null) {
								return;
							}
							if (prop.equalsIgnoreCase(propx)) {
								if (bUseAll) {
									Object[] rootObjects = nodeThis.getRootValues(e.getObject());
									if (rootObjects != null) {
										final OA oa = OARuntime.oa(root.hub);
										for (Object obj : rootObjects) {
											oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, origPropertyName);
										}
									}
								} else {
									Object aObj = root.hub.getAO();
									if (aObj != null) {
										Object[] rootObjects = nodeThis.getRootValues(e.getObject());
										final OA oa = OARuntime.oa(root.hub);
										if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
											oa.internal().hubs().events().fireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)aObj, origPropertyName);
										}
									}
								}
							}
						}
					};
					hub.addHubListener(hl, property, !bUseAll); // 20180923
					//was: hub.addHubListener(hl, property, bActiveObjectOnly);  // note: property could be another calc-property

					HubListener[] hls;
					if (node.hmListener == null) {
						node.hmListener = new HashMap<HubListener, HubListener[]>(3, .75f);
						hls = null;
					} else {
						hls = node.hmListener.get(origHubListener);
					}

					hls = (HubListener[]) OAArray.add(HubListener.class, hls, hl);

					node.hmListener.put(origHubListener, hls);
				}
				break;
			}
		}
	}

	/**
	 * Removes a Hub listener registration.
	 * @param thisHub the listener parameter
	 * @param hl the listener parameter
	 */
	public void removeListener(Hub thisHub, HubListener hl) {
		removeListener(hl);
	}

	/**
	 * Removes a Hub listener registration.
	 * @param hl the listener parameter
	 */
	public void removeListener(HubListener hl) {
		if (hl == null) {
			return;
		}
		// testing
		// hmAll.remove(hl);
		//LOG.finer("Hub="+thisHub);
		synchronized (root) {
			HubListener[] hold = listeners;
			listeners = (HubListener[]) OAArray.removeValue(HubListener.class, listeners, hl);
			if (hold == listeners) {
				return;
			}
			--ListenerCount;
			if (hl.getLocation() == HubListener.InsertLocation.LAST) {
				lastCount--;
				//System.out.println("HubListenerTree.removeListener, ListenerCount="+ListenerCount+", hl="+hl);
			}
		}
		//System.out.println("HubListenerTree.removeListener, ListenerCount="+ListenerCount+" ==>"+hl+", hm.hl.cnt="+HubMerger.HubMergerHubListenerCount);

		removeChildrenListeners(this.root, hl);
	}

	private void removeChildrenListeners(final HubListenerTreeNode node, final HubListener origHubListener) {

		if (node.hmListener != null) {
			HubListener[] hls = node.hmListener.remove(origHubListener);
			if (hls != null) {
				//LOG.finer("removing dependentProp listener, name="+node.property);
				for (HubListener hl : hls) {
					node.hub.removeHubListener(hl);
				}
			}
		}

		for (int k = 0; node.children != null && k < node.children.length; k++) {
			HubListenerTreeNode childNode = node.children[k];

			removeChildrenListeners(childNode, origHubListener); // recurse through the treeNodes

			// see if childNode can be removed - which will remove HubMerger
			if (childNode.hmListener == null || childNode.hmListener.size() == 0) {
				// remove child
				if (!isUsed(childNode)) {
					//LOG.finer("removing hubMerger for dependProp, name="+childNode.property);
					node.children = (HubListenerTreeNode[]) OAArray.removeAt(HubListenerTreeNode.class, node.children, k);
					if (childNode.hubMerger != null) {
						childNode.hubMerger.close();
					}
					k--;
				}
			}
		}
	}

	private boolean isUsed(HubListenerTreeNode node) {
		if (node.hmListener != null && node.hmListener.size() > 0) {
			return true;
		}
		if (node.children == null) {
			return false;
		}

		for (int k = 0; k < node.children.length; k++) {
			if (isUsed(node.children[k])) {
				return true;
			}
		}
		return false;
	}
}
