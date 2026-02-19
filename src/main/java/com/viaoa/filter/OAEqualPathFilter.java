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

import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Filter that compares the values of two {@link OAPropertyPath} expressions
 * on the same object (or located target object) for equality.  Both property
 * paths are resolved and the resulting values are compared using standard
 * OA equality rules.
 *
 * <p>
 * This filter supports deep property traversal: if either property path
 * crosses a many-relationship, an {@link OAFinder} is automatically created
 * to locate the referenced object before evaluating equality.
 * </p>
 *
 * <p>
 * Typical usage: verify that two properties on an object match each other,
 * such as comparing foreign key fields, cross-object references, or
 * synchronized values in a validation rule or filtered Hub.
 * </p>
 */
public class OAEqualPathFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAEqualPathFilter.class.getName());

	/**
	 * The source object from which the "from" property path value is
	 * obtained. May be null when the filter is constructed with a Hub.
	 */
	private OAObject objFrom;

	/**
	 * Cached value obtained from evaluating the "from" property path on
	 * the source object. Used for comparison with the target path value.
	 */
	private Object objFromPPValue;

	/**
	 * Optional Hub whose active object (AO) supplies the base for resolving
	 * the "from" property path. When non-null, AO changes can affect filter
	 * behavior.
	 */
	private final Hub hubFrom; // uses AO
	
	/**
	 * Tracks the last active object of {@link #hubFrom} used during setup.
	 * If the AO changes, the filter must be reinitialized.
	 */
	private Object objHubFromAO; // last (AO) Object used for hubFrom

	/**
	 * Original "from" property-path expression supplied during construction.
	 * Used when rebuilding or shortening the property path.
	 */
	private final String strFromPropPathOrig;
	
	/**
	 * The working "from" property-path expression, which may differ from
	 * the original if the path is shortened based on Hub AO resolution.
	 */
	private String strFromPropPath;
	
	/**
	 * Parsed representation of the "from" property path used to resolve the
	 * comparison source value.
	 */
	private OAPropertyPath ppFrom;

	/**
	 * Property-path expression used to retrieve the comparison target value
	 * from the evaluated object.
	 */
	private String strToPropPath;
	
	/**
	 * Parsed representation of the "to" property path applied to the
	 * evaluated object during comparison.
	 */
	private OAPropertyPath ppTo;

	/**
	 * Optional finder created when the "from" property path traverses a
	 * multi-valued link. Used to locate the correct object for comparison.
	 */
	private OAFinder finder;
	
	/**
	 * Reserved for future use in situations where a second finder may be
	 * needed to locate comparison objects. Not used in visible code.
	 */
	private OAFinder finderCompareObject;
	
	/**
	 * Indicates whether either property path includes filter tokens, which
	 * affects how updateSelect behaves and whether additional SQL clauses
	 * must be generated.
	 */
	private boolean bHasFilter;

	/**
	 * Creates a filter that compares the value of a property path evaluated
	 * from the active object of the supplied Hub to a second property path
	 * evaluated on candidate objects.
	 *
	 * @param fromHub the Hub whose active object supplies the base value
	 * @param propPathFrom the property path used to extract the "from" value
	 * @param propPathTo the property path evaluated on candidate objects
	 */
	public OAEqualPathFilter(Hub fromHub, String propPathFrom, String propPathTo) {
		this.hubFrom = fromHub;
		this.objFrom = null;
		this.strFromPropPathOrig = propPathFrom;
		this.strToPropPath = propPathTo;
		setup();
	}

	/**
	 * Creates a filter that compares a property value obtained from a
	 * specific source object against a property value obtained from
	 * evaluated objects.
	 *
	 * @param objFrom the object supplying the "from" property value
	 * @param propPath property path used to extract the source value
	 * @param strPropPathCompareObject property path evaluated on candidate objects
	 */
	public OAEqualPathFilter(OAObject objFrom, String propPath, String strPropPathCompareObject) {
		this.objFrom = objFrom;
		this.hubFrom = null;
		this.strFromPropPathOrig = propPath;
		this.strToPropPath = strPropPathCompareObject;
		setup();
	}

	//qqqqqqq todo: filter needs to somehow get refreshed when a parentObj is changed qqqqqq
	// ex:   Depts.emps m/d hubs if dept AO changes and emp AO was null
	// needs to listen to this.hubFrom.getMasterHub AOchanges

	// 20210509 updateSelect now checks to see if AO changed

	/**
	 * Initializes internal state used to evaluate the filter. This includes:
	 * <ul>
	 *   <li>building the "from" property path,</li>
	 *   <li>determining whether filters are embedded in the path,</li>
	 *   <li>resolving the correct base object when a Hub is used,</li>
	 *   <li>shortening the property path when master-object traversal occurs,</li>
	 *   <li>caching the resolved "from" property value.</li>
	 * </ul>
	 */
	protected void setup() {
		strFromPropPath = strFromPropPathOrig;
		if (strFromPropPath == null) {
			// empty pp is valid
			// return;
		}
		if (objFrom == null && hubFrom == null) {
			return;
		}

		final Class clazz = hubFrom != null ? hubFrom.getObjectClass() : objFrom != null ? objFrom.getClass() : null;
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);

		this.ppFrom = new OAPropertyPath(clazz, strFromPropPath);

		if (!bHasFilter) {
			String[] ss = ppFrom.getFilterNames();
			if (ss != null) {
				for (String s : ss) {
					if (OAString.isNotEmpty(s)) {
						bHasFilter = true;
					}
				}
			}
		}

		if (hubFrom != null) {
			OALinkInfo[] lis = ppFrom.getLinkInfos();

			// use hubFrom.AO, if it is null, then check if pp uses it's masterObject(s), and if so then use objFrom=masterObj and shorten the pp.
			// reset

			objFrom = null;
			finder = null;
			objFromPPValue = null;

			int cntGetMasterObject = 0;
			Hub hubx = hubFrom;
			if (lis.length > 0) {
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
			} else {
				objFrom = (OAObject) hubFrom.getAO();
			}

			if (objFrom == null) {
				// filter will be empty
				return;
			}

			if (cntGetMasterObject > 0) {
				int pos = strFromPropPath.indexOf('.');
				int pos1 = strFromPropPath.indexOf('(');
				int pos2 = strFromPropPath.indexOf(')');

				String spp;
				if (pos1 >= 0 && pos > pos1 && pos > pos2) {
					strFromPropPath = OAString.field(strFromPropPath, ')', 2, 99);
					strFromPropPath = OAString.field(strFromPropPath, '.', cntGetMasterObject + 1, 99);
				} else {
					strFromPropPath = OAString.field(strFromPropPath, '.', cntGetMasterObject + 1, 99);
				}
				this.ppFrom = new OAPropertyPath(objFrom.getClass(), strFromPropPath);
			}
			objFromPPValue = ppFrom.getValue(objFrom);
		}
	}

	/**
	 * Returns the parsed "from" property path used to locate the source
	 * comparison value.
	 *
	 * @return the property path applied to the source object
	 */
	public OAPropertyPath getPropertyPath() {
		return ppFrom;
	}

	/**
	 * Evaluates the filter for the supplied object by:
	 * <ul>
	 *   <li>refreshing setup when the Hub's active object changes,</li>
	 *   <li>lazily constructing the "to" property path,</li>
	 *   <li>retrieving both source and target values,</li>
	 *   <li>comparing the values using direct equality,</li>
	 *   <li>supporting Hub-membership checks when the target is a Hub.</li>
	 * </ul>
	 *
	 * @param obj the object to evaluate
	 * @return {@code true} if the resolved values match; otherwise {@code false}
	 */
	@Override
	public boolean isUsed(Object obj) {

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
		if (objFrom == null) {
			return false;
		}

		if (ppTo == null) {
			ppTo = new OAPropertyPath(obj.getClass(), strToPropPath);
			//qqqqqqq put in OAInFilter ?
			if (!bHasFilter) {
				String[] ss = ppTo.getFilterNames();
				if (ss != null) {
					for (String s : ss) {
						if (OAString.isNotEmpty(s)) {
							bHasFilter = true;
						}
					}
				}
			}

		}

		
		Object objx = (obj instanceof OAObject) ? ppTo.getValue((OAObject) obj) : obj;;
		
		boolean b;
		if (objx instanceof Hub) {
			b = ((Hub) objx).contains(objFromPPValue); 
		}
		else {
			b = (objx == objFromPPValue);
		}
		return b;
	}

	/**
	 * Updates the select statement used for database filtering. If the
	 * "from" value is available and the "to" property path identifies a
	 * single-valued link, a reverse property path is generated and applied
	 * to the select as a where-object constraint. Returns whether additional
	 * filtering is required.
	 *
	 * @param select the select statement to update
	 * @return {@code true} if further filtering is needed; otherwise {@code false}
	 */
	@Override
	public boolean updateSelect(OASelect select) {
		OAObject obj = null;
		if (hubFrom != null) {
			obj = (OAObject) hubFrom.getAO();
			if (obj != objFrom) {
				setup();
			}
		}

		if (objFrom != null && ppTo != null && select.getWhereObject() == null) {
			if (ppTo != null) {
				OALinkInfo li = ppTo.getEndLinkInfo();
				if (li != null && li.getType() == OALinkInfo.MANY) {
					return true;
				}
				OAPropertyPath ppRev = ppTo.getReversePropertyPath(true);
				select.setWhereObject((OAObject) objFromPPValue, ppRev.getPropertyPath());
				if (bHasFilter) {
					return true;
				}
				return false;
			}
		}
		return OAFilter.super.updateSelect(select);
	}
}
