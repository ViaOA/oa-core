/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.filter;

import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Creates a filter to see if the value of a Hub.AO propertyPath is the same as the propertyPath from another object.
 *
 * @author vvia
 */
public class OAEqualPathFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAEqualPathFilter.class.getName());

	private OAObject objFrom;
	private Object objFromPPValue;

	private final Hub hubFrom; // uses AO
	private Object objHubFromAO; // last (AO) Object used for hubFrom

	private final String strPropPathOrig;
	private String strPropPath;
	private OAPropertyPath pp;

	private String strPropPathCompareObject;
	private OAPropertyPath ppCompareObject;

	private OAFinder finder;
	private OAFinder finderCompareObject;
	private boolean bHasFilter;

	/**
	 * Uses a fromHub.AO and property path to see if it matches the value from a property path of another object.
	 *
	 * @param fromHub  uses the AO
	 * @param propPath to use for finding object that matches the object propertyPath of a compared object.
	 */
	public OAEqualPathFilter(Hub fromHub, String propPath, String strPropPathCompareObject) {
		this.hubFrom = fromHub;
		this.objFrom = null;
		this.strPropPathOrig = propPath;
		this.strPropPathCompareObject = strPropPathCompareObject;
		setup();
	}

	public OAEqualPathFilter(OAObject objFrom, String propPath, String strPropPathCompareObject) {
		this.objFrom = objFrom;
		this.hubFrom = null;
		this.strPropPathOrig = propPath;
		this.strPropPathCompareObject = strPropPathCompareObject;
		setup();
	}

	//qqqqqqq todo: filter needs to somehow get refreshed when a parentObj is changed qqqqqq
	// ex:   Depts.emps m/d hubs if dept AO changes and emp AO was null
	// needs to listen to this.hubFrom.getMasterHub AOchanges

	protected void setup() {
		strPropPath = strPropPathOrig;
		if (strPropPath == null) {
			return;
		}
		if (objFrom == null && hubFrom == null) {
			return;
		}

		final Class clazz = hubFrom != null ? hubFrom.getObjectClass() : objFrom != null ? objFrom.getClass() : null;

		this.pp = new OAPropertyPath(clazz, strPropPath);

		if (!bHasFilter) {
			String[] ss = pp.getFilterNames();
			if (ss != null) {
				for (String s : ss) {
					if (OAString.isNotEmpty(s)) {
						bHasFilter = true;
					}
				}
			}
		}

		if (hubFrom != null) {
			OALinkInfo[] lis = pp.getLinkInfos();

			// use hubFrom.AO, if it is null, then check if pp uses it's masterObject(s), and if so then use objFrom=masterObj and shorten the pp.
			// reset
			objFrom = null;
			finder = null;
			objFromPPValue = null;

			int cntGetMasterObject = 0;
			Hub hubx = hubFrom;
			for (; cntGetMasterObject < lis.length; cntGetMasterObject++) {
				objFrom = (OAObject) hubx.getAO();
				if (objFrom != null) {
					break;
				}
				OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hubx);
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
				this.pp = new OAPropertyPath(objFrom.getClass(), strPropPath);
			}
			objFromPPValue = pp.getValue(objFrom);
		}
	}

	public OAPropertyPath getPropertyPath() {
		return pp;
	}

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

		if (ppCompareObject == null) {
			ppCompareObject = new OAPropertyPath(obj.getClass(), strPropPathCompareObject);
			//qqqqqqq put in OAInFilter
			if (!bHasFilter) {
				String[] ss = ppCompareObject.getFilterNames();
				if (ss != null) {
					for (String s : ss) {
						if (OAString.isNotEmpty(s)) {
							bHasFilter = true;
						}
					}
				}
			}

		}

		Object objx = ppCompareObject.getValue(obj);

		boolean b = (objx == objFromPPValue);
		return b;
	}

	@Override
	public boolean updateSelect(OASelect select) {
		if (objFrom != null && ppCompareObject != null && select.getWhereObject() == null) {
			OAPropertyPath ppRev = ppCompareObject.getReversePropertyPath();
			if (ppRev != null) {
				select.setWhereObject(objFrom, ppRev.getPropertyPath());
				if (bHasFilter) {
					return true;
				}
				return false;
			}
		}
		return OAFilter.super.updateSelect(select);
	}
}
