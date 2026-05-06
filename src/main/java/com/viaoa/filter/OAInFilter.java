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
package com.viaoa.filter;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import com.viaoa.find.OAFinder;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

/**
 * Filter that evaluates whether the property's value appears within a target
 * collection, array, or {@link Hub}.  This supports membership checks—
 * “is this object's property one of these values?”.
 *
 * <p>
 * The comparison value may be:
 * </p>
 *
 * <ul>
 *   <li>a {@link Hub},</li>
 *   <li>a Java array,</li>
 *   <li>a {@link java.util.Collection}.</li>
 * </ul>
 *
 * <p>
 * When using a {@link Hub}, membership is tested using {@code hub.contains()}.
 * When using arrays/collections, standard equality rules apply.
 * </p>
 *
 * <p>
 * Nested property paths are fully supported.  If the path crosses a
 * many-relationship, an {@link OAFinder} locates the target object before
 * performing the membership check.
 * </p>
 */
public class OAInFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAInFilter.class.getName());

	/**
	 * The source object whose property-path value is used to evaluate
	 * membership against the target collection or Hub.
	 */
	private OAObject objFrom;

	/**
	 * Hub whose active object (AO) supplies the base object for resolving
	 * the property path when the filter is constructed with a Hub.
	 */
	private final Hub hubFrom; // uses AO

	/**
	 * Tracks the last active object from {@link #hubFrom}. If the AO changes,
	 * the filter must be reinitialized.
	 */
	private Object objHubFromAO; // last (AO) Object used for hubFrom

	/**
	 * The original property-path expression supplied during construction.
	 * Used when recalculating shortened or modified paths.
	 */
	private final String strPropPathOrig;
	
	/**
	 * Working property-path expression used by the filter. This may differ
	 * from the original when Hub-based path shortening occurs.
	 */
	private String strPropPath;
	
	/**
	 * Parsed representation of the property path used to retrieve the
	 * value that will be tested for membership.
	 */
	private OAPath pp;
	
	/**
	 * The Hub used for membership testing. If set, the filter evaluates
	 * whether the resolved property-path value is contained in this Hub.
	 */
	private Hub hubIn;

	/**
	 * Reverse-direction property path used when the filter must first
	 * traverse a reverse link before performing the membership lookup.
	 */
	private OAPath ppReverse;
	
	/**
	 * String representation of the reverse property-path expression.
	 * Constructed during setup when reverse lookup is needed.
	 */
	private String strReversePropPath;
	
	/**
	 * Finder used when the property path ends in a many-relationship and
	 * membership must be resolved by traversing linked objects.
	 */
	private OAFinder finder;
	
	/**
	 * Temporary holder for the comparison object during evaluation.
	 * Used when either forward or reverse traversal is required.
	 */
	private Object objFind;

	/**
	 * Creates an IN filter that checks whether candidate objects appear
	 * within the supplied Hub.
	 *
	 * @param hubIn the Hub used for membership testing
	 */
	public OAInFilter(Hub hubIn) {
		this.hubIn = hubIn;
		objFrom = null;
		strPropPathOrig = null;
		pp = null;
		hubFrom = null;
	}

	/**
	 * Creates an IN filter that derives its comparison list from a property
	 * path evaluated on the active object of the supplied Hub.
	 *
	 * @param fromHub the Hub whose AO supplies the base object
	 * @param propPath the property path used to retrieve the comparison list
	 */
	public OAInFilter(Hub fromHub, String propPath) {
		this.hubFrom = fromHub;
		this.objFrom = null;
		this.strPropPathOrig = propPath;
		if (fromHub != null && OAString.isEmpty(propPath)) {
			hubIn = fromHub;
		}
		setup();
	}

	/**
	 * Creates an IN filter using a specific source object and property path.
	 *
	 * @param fromObject the object supplying the comparison list
	 * @param propPath the property path used to obtain the list of values
	 */
	public OAInFilter(OAObject fromObject, String propPath) {
		this.objFrom = fromObject;
		hubFrom = null;
		this.strPropPathOrig = propPath;
		setup();
	}

	//qqqqqqq todo: filter needs to somehow get refreshed when a parentObj is changed qqqqqq
	// ex:   Depts.emps m/d hubs if dept AO changes and emp AO was null
	// needs to listen to this.hubFrom.getMasterHub AOchanges

	/**
	 * Initializes the filter by:
	 * <ul>
	 *   <li>building or shortening the property path,</li>
	 *   <li>resolving the base object from a Hub's active object hierarchy,</li>
	 *   <li>splitting the path into forward and reverse components,</li>
	 *   <li>detecting Hub-based endpoints suitable for direct membership checks,</li>
	 *   <li>initializing a finder when required for many-relationship traversal.</li>
	 * </ul>
	 */
	protected void setup() {
		strPropPath = strPropPathOrig;
		if (strPropPath == null) {
			return;
		}
		if (objFrom == null && hubFrom == null) {
			return;
		}

		final Class clazz = hubFrom != null ? hubFrom.getObjectClass() : objFrom != null ? objFrom.getClass() : null;
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);

		this.pp = new OAPath(clazz, strPropPath);

		if (pp.getEndLinkInfo() == null) {
			throw new RuntimeException("invalid propPath " + strPropPath + ", must end in a Link");
		}

		OALinkInfo[] lis = pp.getLinkInfos();

		if (hubFrom != null) {
			// use hubFrom.AO, if it is null, then check if pp uses it's masterObject(s), and if so then use objFrom=masterObj and shorten the pp.
			// reset
			objFrom = null;
			strReversePropPath = null;
			hubIn = null;
			finder = null;

			int cntGetMasterObject = 0;
			Hub hubx = hubFrom;
			for (; cntGetMasterObject < lis.length; cntGetMasterObject++) {
				objFrom = (OAObject) hubx.getAO();
				if (objFrom != null) {
					break;
				}
				OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromDetailToMaster(hubx);
				if (li == null) {
					break;
				}
				if (li.getType() == li.TYPE_MANY) {
					break;
				}
				if (li != lis[cntGetMasterObject]) {
					break;
				}
				hubx = hubx.getMasterHub();
			}
			if (objFrom == null) {
				// filter will be empty
				return;
			}

			if (cntGetMasterObject > 0) {
				int pos = strPropPath.indexOf('.');
				int pos1 = strPropPath.indexOf('(');
				int pos2 = strPropPath.indexOf(')');

				String spp;
				if (pos1 >= 0 && pos > pos1 && pos > pos2) {
					strPropPath = OAString.field(strPropPath, ')', 2, 99);
					strPropPath = OAString.field(strPropPath, '.', cntGetMasterObject + 1, 99);
				} else {
					strPropPath = OAString.field(strPropPath, '.', cntGetMasterObject + 1, 99);
				}
				this.pp = new OAPath(objFrom.getClass(), strPropPath);
				lis = pp.getLinkInfos();
			}
		}

		// see if we can forward through Link=one for the objFrom
		for (; lis != null && lis.length > 0;) {
			if (lis[0].getType() != OALinkInfo.TYPE_ONE) {
				break;
			}

			Object objx = lis[0].getValue(objFrom);
			if (!(objx instanceof OAObject)) {
				break;
			}

			objFrom = (OAObject) objx;
			strPropPath = OAString.field(strPropPath, '.', 2, 99);
			this.pp = new OAPath(objFrom.getClass(), strPropPath);
			lis = pp.getLinkInfos();
		}

		Constructor[] cs = pp.getFilterConstructors();
		OALinkInfo[] lisRecursive = pp.getRecursiveLinkInfos();

		/*
		 * Split the pp into two path(s):
		 * 1: from reverse direction, as long as it's linkTyp=one
		 * 2: from forward, removing reverse pp
		 * Then use the reverse pp and forward pp to find a match in the middle
		 */

		// follow reverse pp as long as linkType=one and not recursive and no filter
		String ppNew1 = strPropPath;
		String ppNew2 = "";
		for (int i = lis.length - 1; i >= 0; i--) {
			if (cs[i] != null) {
				break;
			}
			if (lisRecursive[i] != null) {
				break;
			}
			OALinkInfo li = lis[i];
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev == null) {
				break;
			}
			if (liRev.getType() != OALinkInfo.ONE) {
				break;
			}
			if (liRev.getPrivateMethod()) {
				break;
			}

			ppNew1 = OAString.field(ppNew1, '.', 1, OAString.dcount(ppNew1, '.') - 1);
			if (ppNew2.length() > 0) {
				ppNew2 += ".";
			}
			ppNew2 += liRev.getName();
		}

		if (ppNew2.length() > 0) {
			strReversePropPath = ppNew2;
		}

		if (ppNew1 == null || ppNew1.length() == 0) {
			// only needs to use reverse
			return;
		}

		OAPath ppx = new OAPath(objFrom.getClass(), ppNew1);
		// see if it ends in a Hub and does not have any other many links before it
		lis = ppx.getLinkInfos();
		for (int i = 0; i < lis.length; i++) {
			OALinkInfo li = lis[i];
			if (li.getType() == OALinkInfo.MANY) {
				if (i == lis.length - 1) {
					hubIn = (Hub) ppx.getValue(objFrom);
				}
				break;
			}
		}

		if (hubIn == null) {
			finder = new OAFinder(ppNew1) {
				@Override
				protected boolean isUsed(OAObject obj) {
					return (obj == objFind);
				}
			};
		}
	}

	/**
	 * Returns the parsed property path used for membership extraction.
	 *
	 * @return the property path associated with this filter
	 */
	public OAPath getPropertyPath() {
		return pp;
	}

	/**
	 * Evaluates whether the supplied object matches the IN condition.
	 * This includes:
	 * <ul>
	 *   <li>refreshing setup when the Hub AO changes,</li>
	 *   <li>applying reverse-path traversal if required,</li>
	 *   <li>performing Hub membership tests when a Hub is present,</li>
	 *   <li>using a finder when the property path ends in a many-link,</li>
	 *   <li>comparing resolved values directly when no Hub or finder is used.</li>
	 * </ul>
	 *
	 * @param obj the object being evaluated
	 * @return {@code true} if the object satisfies the IN condition; otherwise {@code false}
	 */
	@Override
	public boolean isUsed(final Object obj) {
		if (hubFrom != null) {
			Hub hubx = hubFrom;
			Object objx = null;
			for (; hubx != null; hubx = hubx.getMasterHub()) {
				objx = hubx.getAO();
				if (objx != null) {
					break;
				}
			}

			if (objx != objHubFromAO) {
				// reset
				objHubFromAO = objx;
				setup();
			}
		}

		if (hubIn != null && strReversePropPath == null) {
			return hubIn.contains(obj);
		}

		objFind = obj;
		if (strReversePropPath != null) {
			if (ppReverse == null) {
				ppReverse = new OAPath(obj.getClass(), strReversePropPath);
			}
			if (obj instanceof OAObject) objFind = ppReverse.getValue((OAObject) obj);
			else objFind = obj;
			if (objFind == null) {
				return false;
			}
		}

		boolean bResult;
		if (hubIn != null) {
			bResult = hubIn.contains(objFind);
		} else if (finder != null) {
			Object objx = finder.findFirst(objFrom);
			bResult = (objx != null);
		} else if (objFrom != null) {
			bResult = (objFrom == objFind);
		} else {
			bResult = false;
		}
		return bResult;
	}

	/**
	 * Updates a select statement to optimize server-side filtering when
	 * reverse property paths are available. If a reverse path can identify
	 * a unique where-object constraint, the select is updated accordingly.
	 *
	 * @param select the select query being prepared
	 * @return {@code false} if the filter has applied a where-object;
	 *         otherwise the default filter behavior result
	 */
	@Override
	public boolean updateSelect(OASelect select) {
		if (hubFrom != null) {
			Hub hubx = hubFrom;
			Object objx = null;
			for (; hubx != null; hubx = hubx.getMasterHub()) {
				objx = hubx.getAO();
				if (objx != null) {
					break;
				}
			}

			if (objx != objHubFromAO) {
				// reset
				objHubFromAO = objx;
				setup();
			}
		}

		if (objFrom != null && OAString.isNotEmpty(strReversePropPath) && select.getWhereObject() == null) {
			select.setWhereObject(objFrom, strPropPath);
			return false;
		}
		return OAFilter.super.updateSelect(select);
	}

}
