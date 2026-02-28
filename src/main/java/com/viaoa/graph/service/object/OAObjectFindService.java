package com.viaoa.graph.service.object;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.util.OACompare;

public class OAObjectFindService {
	private static final Logger LOG = Logger.getLogger(OAObjectFindService.class.getName());

	public OAObjectFindService() {
	}

	
	/**
	 * Searches the object graph beginning at the specified {@link OAObject} for
	 * objects whose property value matches the supplied {@code findValue}, following
	 * the navigation defined by the {@code propertyPath}. This method implements
	 * the full recursive search logic for all {@code find(...)} overloads.
	 *
	 * <p>The {@code propertyPath} is a dot-separated sequence of property or link
	 * names beginning at {@code base}. Each segment may refer to either a simple
	 * property or a relationship link (one-to-one or one-to-many). The method
	 * traverses the path step by step and evaluates the final property value(s)
	 * against the provided {@code findValue}. If {@code bFindAll} is {@code false},
	 * the search stops as soon as the first match is found; otherwise, all matches
	 * reachable along the path are collected.</p>
	 *
	 * <h3>Traversal Behavior</h3>
	 * <ul>
	 *   <li>If {@code base} is {@code null} or the {@code propertyPath} is empty,
	 *       an empty result array is returned.</li>
	 *   <li>The method resolves each segment in the {@code propertyPath} using
	 *       {@link OAPropertyPath} metadata provided by {@code base}'s
	 *       {@link OAObjectInfo}.</li>
	 *   <li>For link segments:
	 *     <ul>
	 *       <li>One-to-one links: the referenced object becomes the next traversal node.</li>
	 *       <li>One-to-many or many-to-many links: each object in the associated hub
	 *           is recursively processed for the remaining path.</li>
	 *     </ul>
	 *   </li>
	 *   <li>For the final segment:
	 *     <ul>
	 *       <li>If it is a property, its value is retrieved via the object's getter.</li>
	 *       <li>A match occurs if {@code findValue == null} and the property value is {@code null},
	 *           or if {@code findValue.equals(propertyValue)} is {@code true}.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <h3>Results</h3>
	 * <ul>
	 *   <li>Returns an array of all matching values if {@code bFindAll} is {@code true}.</li>
	 *   <li>Returns a single-element array containing the first match if
	 *       {@code bFindAll} is {@code false}.</li>
	 *   <li>Returns an empty array if no matches are found.</li>
	 * </ul>
	 *
	 * @param base         the root object from which the property path traversal
	 *                     begins; may be {@code null}.
	 * @param propertyPath the dot-separated property or link path to follow; must
	 *                     not be {@code null}.
	 * @param findValue    the value to compare against the resolved property value.
	 * @param bFindAll     if {@code true}, collect all matches; otherwise stop at the first match.
	 * @return an array containing matched values (or objects), never {@code null}.
	 */
	public OAObject[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll) {
		if (propertyPath == null || propertyPath.length() == 0) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		Object result = base;
		for (; st.hasMoreTokens();) {
			String s = st.nextToken();
			base = (OAObject) result; // previous object
			result = base.getProperty(s);

			if (!st.hasMoreTokens()) {
				// last property, check against findValue
				if (result == findValue || (result != null && OACompare.compare(result, findValue) == 0)) {
					OAObject[] objs = new OAObject[] { base };
					return objs;
				}
				return null;
			}

			if (result == null) {
				return null;
			}

			if (result instanceof Hub) {
				String pp = null;
				for (; st.hasMoreTokens();) {
					s = st.nextToken();
					if (pp == null) {
						pp = s;
					} else {
						pp += "." + s;
					}
				}
				ArrayList<OAObject> al = null;
				Hub<?> h = (Hub) result;
				for (int ii = 0;; ii++) {
					OAObject obj = h.elementAt(ii);
					if (obj == null) {
						break;
					}
					OAObject[] objs = find((OAObject) obj, pp, findValue, bFindAll);
					if (objs != null) {
						if (!bFindAll) {
							return objs;
						}
						if (al == null) {
							al = new ArrayList<OAObject>(10);
						}
						for (int i3 = 0; i3 < objs.length; i3++) {
							al.add(objs[i3]);
						}
					}
				}
				if (al == null) {
					return null;
				}
				OAObject[] objs = new OAObject[al.size()];
				objs = al.toArray(objs);
				return objs;
			}
			if (!(result instanceof OAObject)) {
				return null;
			}
		}
		return null;
	}
	
	
}
