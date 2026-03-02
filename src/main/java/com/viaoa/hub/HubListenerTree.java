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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAMany;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectAnnotationService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.graph.service.object.OAObjectPropertyService;
import com.viaoa.graph.service.object.OAObjectReflectService;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPerformance;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAArray;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAPropertyPath;

/**
 * Manages Hub listeners and their dependent property paths as a routed tree.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintain an ordered array of {@link HubListener}s with FIRST/NEXT/LAST placement.</li>
 *   <li>Build a dependency tree for calc/link properties so that changes in nested
 *       objects trigger the correct top-level listener property notifications.</li>
 *   <li>Resolve dependent paths using {@link com.viaoa.util.OAPropertyPath}, {@link com.viaoa.object.OALinkInfo},
 *       reverse links, and (when needed) {@link com.viaoa.object.OAFinder} fallbacks.</li>
 *   <li>Merge child hubs via {@link HubMerger} to watch nested collections (supports AO-only vs use-all modes,
 *       background thread option, and sibling scoping via {@link com.viaoa.object.OASiblingHelper}).</li>
 * </ul>
 * The tree node model caches reverse-link info and last remove context, and can
 * compute root objects to notify for deep changes. This allows precise, minimal
 * event routing for calc properties and deep link dependencies.
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
	        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
					OALinkInfo li = og.objectsInternal().callObjectInfoGetLinkInfo(c, tn.property);
					tn.liReverse = og.objectsInternal().callObjectInfoGetReverseLinkInfo(li);
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
			OAPropertyPath pp = null;
			if (spp != null) {
				pp = new OAPropertyPath(HubListenerTree.this.root.hub.getObjectClass(), spp);
				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis != null && lis.length > 0) {
					bUseOrig = false;
					for (OALinkInfo li : lis) {
						if (li == null || li.getReverseLinkInfo() == null) {
							// LOG.log(Level.FINE, "Link does not have a reverseLinkInfo, link.toClass="+li.getToClass()+", name="+li.getName()+", pp="+spp+", rootHub="+HubListenerTree.this.root.hub, new Exception("Found HubListenerTree issue qqqqqq"));
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
				        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph((OAObject) obja);
								objz = og.objectsInternal().callObjectPropertyGetProperty((OAObject) obja, lis[0].getName());
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

			long ts2 = System.currentTimeMillis();//qqqqqqqqq
			if ((ts2 - ts) > 1000) { //qqqqqqqq should not happen, can be removed
				OAPerformance.LOG.fine("fyi: getRootValues took " + (ts2 - ts) + "ms, rootHub=" + HubListenerTree.this.root.hub
						+ ", propPath=" + spp);
			}
			return objs;
		}

		//qqqqqqqqqq remove
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
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
				OALinkInfo li = og.objectsInternal().callObjectInfoGetLinkInfo(c, property);
				liReverse = og.objectsInternal().callObjectInfoGetReverseLinkInfo(li);
			}

			ArrayList<Object> alNewObjects = new ArrayList<Object>();

			Method m = null;
			for (Object obj : objs) {
				OAObject oaObj = (OAObject) obj;

				String propName = null;
				if (liReverse != null) {
					propName = liReverse.getName();
	        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
					OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(oaObj);
					m = og.objectsInternal().callObjectInfoGetMethod(oi, "get" + propName, 0);
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
		        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph((OAObject) objx);
						Object objz = og.objectsInternal().callObjectReflectGetProperty((OAObject) objx, this.property);
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

	public HubListenerTree(Hub hub) {
		root.hub = hub;
	}

	public HubListener[] getHubListeners() {
		return this.listeners;
	}

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

	public void addListener(HubListener hl, String property, boolean bActiveObjectOnly) {
		if (hl == null) {
			return;
		}
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(root.hub.getObjectClass());
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

	public void addListener(HubListener hl, final String property, String[] dependentPropertyPaths) {
		if (hl == null) {
			return;
		}
		addListener(hl, property, dependentPropertyPaths, false);
	}

	public void addListener(HubListener hl, final String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
		addListener(hl, property, dependentPropertyPaths, bActiveObjectOnly, false);
	}

	public void addListener(HubListener hl, boolean bActiveObjectOnly) {
		addListener(hl, null, null, bActiveObjectOnly, false);
	}

	public void addListener(HubListener hl, final String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly,
			boolean bAllowBackgroundThread) {
		if (hl == null) {
			return;
		}
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		try {
			srvcOAThreadLocal.setHubListenerTree(true);
			addListener(hl, property, bActiveObjectOnly); // this will check for dependent calcProps
			// now add the additional dependent properties
			if (dependentPropertyPaths != null && dependentPropertyPaths.length > 0) {
				addDependentListeners(property, hl, dependentPropertyPaths, bActiveObjectOnly, bAllowBackgroundThread);
			}
		} finally {
			srvcOAThreadLocal.setHubListenerTree(false);
			srvcOAThreadLocal.setIgnoreTreeListenerProperty(null);
		}
	}

	/**
	 * @param dependentPropertyPaths
	 * @param bActiveObjectOnly      if true, then dependent props only listen to the hub's AO
	 */
	private void addListenerMain(HubListener hl, final String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly,
			final boolean bAllowBackgroundThread) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		try {
			srvcOAThreadLocal.setHubListenerTree(true);
			this.addListener(hl);

			if (dependentPropertyPaths != null && dependentPropertyPaths.length > 0) {
				synchronized (root) { // 20200401
					addDependentListeners(property, hl, dependentPropertyPaths, bActiveObjectOnly, bAllowBackgroundThread);
				}
			}
		} finally {
			srvcOAThreadLocal.setHubListenerTree(false);
			srvcOAThreadLocal.setIgnoreTreeListenerProperty(null);
		}
	}

	private void addDependentListeners(final String origPropertyName, final HubListener origHubListener,
			final String[] dependentPropertyNames, final boolean bActiveObjectOnly, final boolean bAllowBackgroundThread) {
		//LOG.finer("Hub="+root.hub+", property="+origPropertyName);

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		// 20120826 check for endless loops
		if (srvcOAThreadLocal.getHubListenerTreeCount() > 25) {
			// need to bail out, before stackoverflow
			LOG.log(Level.WARNING, "srvcOAThreadLocal.getHubListenerTreeCount() > 25, will not continue to add listeners. PropertyName="
					+ origPropertyName, new Exception("detected possible overflow, will continue"));
			return;
		}

		String ignore = srvcOAThreadLocal.getIgnoreTreeListenerProperty();
		for (int i = 0; i < dependentPropertyNames.length; i++) {
			if (dependentPropertyNames[i] == null) {
				continue;
			}
			if (dependentPropertyNames[i].length() == 0) {
				continue;
				//LOG.finer("Hub="+root.hub+", property="+origPropertyName+", dependentProp="+dependentPropertyNames[i]);
			}

			// 20120826 if recursive prop then dont need to listen to more, since a hubMerger is already listening
			if (ignore != null && dependentPropertyNames[i].equalsIgnoreCase(ignore)) {
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
			final OAPropertyPath oaPropPath = new OAPropertyPath(dependPropName);
			try {
				String error = oaPropPath.setup(hub, hub.getObjectClass(), false);
				if (error != null) {
					if (oaPropPath.getNeedsDataToVerify()) {
						// 20150715 propPath is using generics and will have to be retried once data is in it.
						//    this will now set up a listener to try again
						final HubListener hl = new HubListenerAdapter() {
							public void afterAdd(HubEvent e) {
								update();
							}

							public void afterInsert(HubEvent e) {
								update();
							}

							public void onNewList(HubEvent e) {
								Hub h = e.getHub();
								if (h != null && h.size() > 0) {
									update();
								}
							}

							void update() {
								try {
									removeListener(this);
									addDependentListeners(	origPropertyName, origHubListener, new String[] { dependPropName },
															bActiveObjectOnly, bAllowBackgroundThread);
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
			        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubClass);
							hubClass = og.objectsInternal().callObjectAnnotationGetHubObjectClass(om, m);
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
								public OASiblingHelper getSiblingHelper() {
									if (siblingHelper == null) {
										siblingHelper = new OASiblingHelper<>(HubListenerTree.this.root.hub);
										siblingHelper.add(ppFromRoot);
									}
									return siblingHelper;
								}

								@Override
								protected void beforeRemoveRealHub(HubEvent e) {
									// get the parent reference object from the Hub.masterObject, since the
									//    reference in the object could be null once the remove is done
									Hub h = e.getHub();
									newTreeNode.lastRemoveObject = e.getObject();
									newTreeNode.lastRemoveMasterObject = h.getMasterObject();
									super.beforeRemoveRealHub(e);
								}

								@Override
								protected void afterAddRealHub(HubEvent e) {
									super.afterAddRealHub(e);
									onEvent(e);

								}

								@Override
								protected void afterRemoveRealHub(HubEvent e) {
									super.afterRemoveRealHub(e);
									onEvent(e);
								}

								@Override
								protected void afterRemoveAllRealHub(HubEvent e) {
									super.afterRemoveAllRealHub(e);
									onEvent(e);
								}

								private void onEvent(HubEvent e) {
									final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
									if (nodeThis == root) {
										for (String s : newTreeNode.getCalcPropertyNames()) {
											og.hubsInternal().callHubEventFireCalcPropertyChange(root.hub, e.getHub().getMasterObject(), s);
										}
									} else {
										if (bUseAll) {
											Object[] rootObjects = nodeThis.getRootValues(e.getHub().getMasterObject());
											if (rootObjects != null && rootObjects.length > 0) {
												for (Object obj : rootObjects) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>) root.hub, (OAObject) obj, s);
													}
												}
											}
										} else {
											Object aObj = root.hub.getAO();
											if (aObj != null) {
												Object[] rootObjects = nodeThis.getRootValues(e.getHub().getMasterObject());
												if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>) root.hub, (OAObject)aObj, s);
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
									public void afterPropertyChange(HubEvent e) {
										if (!property.equalsIgnoreCase(e.getPropertyName())) {
											return;
										}

										if (bUseAll) {
											Object[] rootObjects = newTreeNode.parent.getRootValues(e.getObject());
											if (rootObjects != null && rootObjects.length > 0) {
												final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(rootObjects[0].getClass());
												for (Object obj : rootObjects) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, s);
													}
												}
											}
										} else {
											Object aObj = root.hub.getAO();
											if (aObj != null) {
												final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(aObj.getClass());
												Object[] rootObjects = newTreeNode.parent.getRootValues(e.getObject());
												if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
													for (String s : newTreeNode.getCalcPropertyNames()) {
														og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)aObj, s);
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

					boolean bx; // might need to have a listener for last hub in propertyPath

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
							public void afterInsert(HubEvent e) {
								afterAdd(e);
							}

							// 20190120
							@Override
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
							public void afterRemoveAll(HubEvent e) {
								if (!srvcOAThreadLocal.isHubMergerChanging()) {
									final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
									og.hubsInternal().callHubEventFireCalcPropertyChange(root.hub, null, origPropertyName);
								}
							}

							private void onEvent(Object[] rootObjects) {
								if (rootObjects == null) {
									return;
								}
								final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
								for (Object obj : rootObjects) {
									if (obj != null) {
										og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, origPropertyName);
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

				// Add a hub listener to end of propertyPath

				if (hubClass == null) {
					//LOG.finer("creating dependent prop hubListener, dependProp="+property);
					final String propx = property;
					final HubListenerTreeNode nodeThis = node;
					HubListener hl = new HubListenerAdapter() {
						@Override
						public void afterPropertyChange(HubEvent e) {
							String prop = e.getPropertyName();
							if (prop == null) {
								return;
							}
							if (prop.equalsIgnoreCase(propx)) {
								if (bUseAll) {
									Object[] rootObjects = nodeThis.getRootValues(e.getObject());
									if (rootObjects != null) {
										final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
										for (Object obj : rootObjects) {
											og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)obj, origPropertyName);
										}
									}
								} else {
									Object aObj = root.hub.getAO();
									if (aObj != null) {
										Object[] rootObjects = nodeThis.getRootValues(e.getObject());
										final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(root.hub);
										if (rootObjects != null && OAArray.containsExact(rootObjects, aObj)) {
											og.hubsInternal().callHubEventFireCalcPropertyChange((Hub<OAObject>)root.hub, (OAObject)aObj, origPropertyName);
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

	public void removeListener(Hub thisHub, HubListener hl) {
		removeListener(hl);
	}

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
