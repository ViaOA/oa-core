package com.viaoa.oa.service.object;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.compare.OACompare;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;

/**
 * Provides property-path search support starting from an OAObject.
 */
public class OAObjectFindService {
	private static final Logger LOG = Logger.getLogger(OAObjectFindService.class.getName());

	/**
	 * Performs OAObjectFindService behavior for the OA object service.
	 */
	public OAObjectFindService() {
	}

	
	/**
	 * Searches the OA model beginning at the specified {@link OAObject} for
	 * objects whose property value matches the supplied {@code findValue}, following
	 * the navigation defined by the {@code path}. This method implements
	 * the full recursive search logic for all {@code find(...)} overloads.
	 *
	 * <p>The {@code path} is a dot-separated sequence of property or link
	 * names beginning at {@code base}. Each segment may refer to either a simple
	 * property or a relationship link (one-to-one or one-to-many). The method
	 * traverses the path step by step and evaluates the final property value(s)
	 * against the provided {@code findValue}. If {@code bFindAll} is {@code false},
	 * the search stops as soon as the first match is found; otherwise, all matches
	 * reachable along the path are collected.</p>
	 *
	 * <h3>Traversal Behavior</h3>
	 * <ul>
	 *   <li>If {@code base} is {@code null} or the {@code path} is empty,
	 *       an empty result array is returned.</li>
	 *   <li>The method resolves each segment in the {@code path} using
	 *       {@link OAPath} metadata provided by {@code base}'s
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
	 * @param path the dot-separated property or link path to follow; must
	 *                     not be {@code null}.
	 * @param findValue    the value to compare against the resolved property value.
	 * @param bFindAll     if {@code true}, collect all matches; otherwise stop at the first match.
	 * @return null or an array containing matched values (or objects).
	 */
	public OAObject[] find(OAObject base, String path, Object findValue, boolean bFindAll) {
		final OACascade cascade = new OACascade();
		return _find(base, path, findValue, bFindAll, cascade);
	}
	/**
	 * Performs _find behavior for the OA object service.
	 *
	 * @param base method input
	 * @param path method input
	 * @param findValue method input
	 * @param bFindAll method input
	 * @param cascade method input
	 * @return result value
	 */
	protected OAObject[] _find(OAObject base, String path, Object findValue, boolean bFindAll, final OACascade cascade) {
		if (base == null || path == null || path.length() == 0) {
			return null;
		}
		if (cascade.wasCascaded(base, true)) return null;
		StringTokenizer st = new StringTokenizer(path, ".");
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
					OAObject[] objs = _find((OAObject) obj, pp, findValue, bFindAll, cascade);
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
