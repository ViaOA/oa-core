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
package com.viaoa.graph.sibling;

import java.util.ArrayList;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/*qqqqqqqqqq
CODEX

#7
  file/class/method: src/main/java/com/viaoa/graph/sibling/OASiblingHelper.java:143, src/main/java/com/viaoa/graph/
  service/hub/HubSortService.java:221, src/main/java/com/viaoa/runtime/OAThreadLocalService.java:1591
  exact concern: OASiblingHelper.setUseSameThread(true) is set by Hub sorting, but getUseSameThread() is never read
  anywhere in src/main/java.
  why it matters: the flag documents a same-thread correctness guarantee, but the runtime does not enforce it. Since
  OASiblingHelper mutates nodeLastFound and its learned node tree, accidental cross-thread reuse can produce wrong
  sibling paths.
  severity: invariant risk
  minimal fix: either remove the semantic claim and treat helpers as thread-local by registration only, or enforce
  the flag in OAThreadLocalService/sibling lookup by owner thread.
  suggested invariant ID/name: SIB-SAME-THREAD-ENFORCED
  suggested test coverage: create a helper with useSameThread=true, register/use it from another thread, verify it
  is ignored or explicitly rejected.


*/


/**
 * Learns and resolves property-paths from a root {@link Hub} so that
 * "sibling" data can be located efficiently. As references are accessed
 * (via {@code OAObject.getObject(...)} / {@code getHub(...)}), this helper
 * records the traversed link steps as a small tree of nodes. Later, given
 * an {@link OAObject} and a link/property name, it can reconstruct the
 * property path back to the root hub.
 *
 * <p>Paths are discovered in two ways:</p>
 * <ul>
 *   <li><b>Explicitly</b> via {@link #add(String)} using a property path
 *       starting at the hub's object class.</li>
 *   <li><b>Implicitly</b> via {@link #onGetReference(OAObject, String)} whenever
 *       references are read at runtime. Missing steps are created using
 *       {@link OALinkInfo} from the current node's {@link OAObjectInfo}.</li>
 * </ul>
 *
 * <p>If the terminal segment is a calculated property (annotated with
 * {@code @OACalculatedProperty}), any declared dependency properties are
 * expanded into additional learned paths, keeping sibling discovery aligned
 * with calculation inputs.</p>
 *
 * <p>This helper is intended for per-thread use (see {@code OAThreadLocal}).
 * It stores only link metadata (no strong references to live objects) and
 * never forces lazy loading.</p>
 *
 * @param <TYPE> the OAObject type contained by the root Hub
 *
 * @see Hub
 * @see OAObject
 * @see OAPath
 * @see OAObjectInfo
 * @see OALinkInfo
 */
public class OASiblingHelper<TYPE extends OAObject> {

	/**
	 * The root Hub from which all sibling property-path learning begins.
	 * All discovered or added paths originate from the objects contained
	 * in this Hub.
	 */
	private Hub<TYPE> hub;
	
	/**
	 * The root node of the learned property-path tree. Represents the
	 * starting point for resolving sibling paths back to the Hub’s
	 * object class. Initialized using the Hub’s OAObjectInfo.
	 */
	private final Node nodeRoot;
	
	/**
	 * Indicates whether this helper should restrict property-path
	 * resolution to the same thread in which it was created. Used
	 * for thread-local optimization and correctness guarantees.
	 */
	private boolean bUseSameThread;

	/**
	 * Creates a new helper for the given root Hub and initializes the root
	 * node using the Hub's OAObjectInfo.
	 *
	 * @param hub the root Hub used for sibling resolution
	 */
	public OASiblingHelper(Hub<TYPE> hub) {
		this.hub = hub;
		nodeRoot = new Node(null);
		nodeRoot.oi = hub.getOAObjectInfo();
	}

	// tree nodes for propertyPaths from hub
	protected class Node {
		public Node(Node parent) {
			this.nodeParent = parent;
		}

		/**
		 * Indicates whether this helper should restrict property-path
		 * resolution to the same thread in which it was created. Used
		 * for thread-local optimization and correctness guarantees.
		 */
		Node nodeParent;
		
		/**
		 * Metadata describing the OAObject type represented at this Node.
		 * Provides link information used when learning and reconstructing
		 * property paths.
		 */
		OAObjectInfo oi;

		/**
		 * Link metadata for the property that connects this Node to its
		 * parent. Represents the relationship segment that led from the
		 * parent to this child node.
		 */
		OALinkInfo li;
		
		/**
		 * Child nodes representing the next steps in the learned property-path
		 * tree. Each entry corresponds to a link/property reachable from the
		 * OAObject type represented by this Node.
		 */
		ArrayList<Node> alChildren;
	}

	/**
	 * Returns the root Hub used by this helper.
	 */
	public Hub<TYPE> getHub() {
		return hub;
	}

	/**
	 * Sets whether this helper should use the same thread when resolving
	 * property paths.
	 */
	public void setUseSameThread(boolean b) {
		this.bUseSameThread = b;
	}

	/**
	 * Returns whether this helper is configured to use the same thread when
	 * resolving property paths.
	 */
	public boolean getUseSameThread() {
		return this.bUseSameThread;
	}

	/**
	 * Adds a property path starting from the root Hub. If the supplied
	 * property path is empty, no action is taken.
	 *
	 * @param ppFromHub the property path from the Hub's object class
	 */
	public void add(String ppFromHub) {
		if (OAString.isEmpty(ppFromHub)) {
			return;
		}
		add(ppFromHub, 0);
	}

	/**
	 * Adds the property path at the specified recursion depth. Creates nodes
	 * for each link segment and expands calculated-property dependencies.
	 *
	 * @param ppFromHub the property path from the Hub's object class
	 * @param cnt       recursion depth for dependency expansion
	 */
	private void add(final String ppFromHub, final int cnt) {
		OAPath<TYPE> pp = new OAPath<TYPE>(hub.getObjectClass(), ppFromHub);
		OALinkInfo[] lis = pp.getLinkInfos();

		if (lis != null) {
			Node node = nodeRoot;
			for (OALinkInfo li : lis) {
				Node nodex = _add(node, li.getName());
				if (nodex == null) {
					break;
				}
				node = nodex;
			}
		}

		// see if last is a calc prop, and check dependent prop paths
		if (pp.getEndLinkInfo() != null) {
			return;
		}
		if (cnt > 3) {
			return;
		}

		OACalculatedProperty calc = pp.getOACalculatedPropertyAnnotation();
		if (calc == null) {
			return;
		}

		String[] dependProps = calc.properties();
		if (dependProps == null) {
			return;
		}

		String[] castNames = pp.getCastNames();
		String ppPrefix = "";
		if (lis != null) {
			for (int i = 0; i < lis.length; i++) {
				if (i > 0) {
					ppPrefix += ".";
				}
				if (castNames != null && castNames.length > i && castNames[i] != null && castNames[i].length() > 0) {
					ppPrefix += "(" + castNames[i] + ")";
				}
				ppPrefix += lis[i].getName();
			}
			if (ppPrefix.length() > 0) {
				ppPrefix += ".";
			}
		}

		for (String s : dependProps) {
			add(ppPrefix + s, cnt + 1);
		}
	}

	/**
	 * Adds or returns an existing child node for the given property name,
	 * creating a new node if the link metadata is valid.
	 *
	 * @param node the starting node
	 * @param prop the property name to add
	 * @return the matching or newly created child node, or null if none
	 */
	private Node _add(Node node, String prop) {
		// returns the node node that has this prop
		if (node == null) return null;
		Node nodeFound = null;
		if (node.alChildren == null) {
			node.alChildren = new ArrayList<>();
		} else {
			for (Node nodeChild : node.alChildren) {
				if (prop.equalsIgnoreCase(nodeChild.li.getName())) {
					nodeFound = nodeChild;
					break;
				}
			}
		}
		if (nodeFound == null) {
			OALinkInfo li = node.oi.getLinkInfo(prop);
			if (li != null && !li.getPrivateMethod()) {
				nodeFound = new Node(node);
				nodeFound.oi = li.getToObjectInfo();
				nodeFound.li = li;
				node.alChildren.add(nodeFound);
			}
		}
		return nodeFound;
	}

	/**
	 * Records reference access so that link steps can be learned when an
	 * object's property is retrieved.
	 *
	 * @param obj  the object whose reference was accessed
	 * @param prop the name of the accessed property
	 */
	public void onGetReference(final OAObject obj, final String prop) {
		if (obj == null || prop == null) {
			return;
		}
		_onGetReference(nodeRoot, obj, prop);
	}

	/**
	 * Recursively searches the node tree to locate or add the node associated
	 * with the given object's class and property access.
	 *
	 * @param node the current node
	 * @param obj  the object whose reference was accessed
	 * @param prop the property name
	 * @return the discovered or created node, or null if not found
	 */
	private Node _onGetReference(final Node node, final OAObject obj, final String prop) {
		final Class cz = obj.getClass();
		if (node.oi.getForClass().equals(cz)) {
			Node nodex = _add(node, prop);
			return nodex;
		}

		if (node.alChildren != null) {
			for (Node nodeChild : node.alChildren) {
				Node nodex = _onGetReference(nodeChild, obj, prop);
				if (nodex != null) {
					return nodex;
				}
			}
		}

		// see if there is a link prop match
		for (OALinkInfo li : node.oi.getLinkInfos()) {
			if (li.getToClass().equals(cz) && !li.getPrivateMethod()) {
				Node nodex = _add(node, li.getName());
				nodex = _add(nodex, prop);
				return nodex;
			}
		}

		return null;
	}

	/**
	 * Returns the property path from the root Hub to the specified object's
	 * property by delegating to the overloaded method with default parameters.
	 *
	 * @param obj  the object used as the target
	 * @param prop the property name
	 * @return the property path, or null if none found
	 */
	public String getPropertyPath(OAObject obj, String prop) {
		return getPropertyPath(obj, prop, false);
	}

	/**
	 * Returns the property path from the root Hub to the specified object's
	 * property. Searches the node tree and reconstructs the path by walking
	 * up the node hierarchy.
	 *
	 * @param obj            the object used as the target
	 * @param prop           the property name
	 * @param bFromLastNode  whether to prioritize the last-found node
	 * @return the property path, or null if none found
	 */
	public String getPropertyPath(OAObject obj, String prop, boolean bFromLastNode) {
		Node node = _findNode(nodeRoot, obj, prop, false, bFromLastNode);
		if (node == null && !bFromLastNode) {
			node = _findNode(nodeRoot, obj, prop, true, false);
		}
		nodeLastFound = node;

		String pp = null;
		for (; node != null && node != nodeRoot; node = node.nodeParent) {
			if (pp == null) {
				pp = node.li.getName();
			} else {
				pp = node.li.getName() + "." + pp;
			}
		}
		return pp;
	}

	/**
	 * Tracks the most recently matched Node during property-path
	 * resolution. Used to improve search prioritization in
	 * {@code getPropertyPath(...)} by optionally preferring the
	 * last successfully located Node.
	 */
	private Node nodeLastFound;

	/**
	 * Searches the node tree for a node matching the object's class and
	 * property. Optionally retries using link-based traversal.
	 *
	 * @param node          the current node
	 * @param obj           the target object
	 * @param prop          the property name to locate
	 * @param bRetry        whether to retry using link matching
	 * @param bFromLastNode whether to prioritize the last-found node
	 * @return the matching node, or null if none is found
	 */
	private Node _findNode(final Node node, final OAObject obj, final String prop, final boolean bRetry, final boolean bFromLastNode) {
		if (node == null || obj == null) return null;
		final Class cz = obj.getClass();

		if (node.oi.getForClass().equals(cz)) {

			boolean bCheckLinks = true;
			if (node.alChildren != null && prop != null) {
				for (Node nodeChild : node.alChildren) {
					if (prop.equalsIgnoreCase(nodeChild.li.getName())) {
						if (bFromLastNode && nodeLastFound != null) {
							if (nodeChild == nodeLastFound) {
								nodeLastFound = null;
								bCheckLinks = false;
							}
						} else {
							return nodeChild;
						}
					}
				}
			}

			if (bCheckLinks) {
				for (OALinkInfo li : node.oi.getLinkInfos()) {
					if (li.getName().equalsIgnoreCase(prop) && !li.getPrivateMethod()) {
						Node nodex = _add(node, li.getName());
						if (bFromLastNode && nodeLastFound != null) {
							if (nodex == nodeLastFound) {
								nodeLastFound = null;
							}
						} else {
							return nodex;
						}
					}
				}
			}
		}

		if (bRetry) {
			// see if there is a link prop match
			for (OALinkInfo li : node.oi.getLinkInfos()) {
				if (!li.getToClass().equals(cz) || li.getPrivateMethod()) {
					continue;
				}
				if (li.getCalculated()) {
					continue;
				}
				Node nodex = _add(node, li.getName());
				nodex = _add(nodex, prop);
				return nodex;
			}
		}

		if (node.alChildren != null) {
			for (Node nodeChild : node.alChildren) {
				Node nodex = _findNode(nodeChild, obj, prop, bRetry, bFromLastNode);
				if (nodex != null) {
					return nodex;
				}
			}
		}

		return null;
	}

}
