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
 * Creates a filter to see if the value of a Hub.AO propertyPath is the same as the propertyPath from 
 * a propertyPath of the objects being selected.
 *
 * @author vvia
 */
public class OAEqualPathFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAEqualPathFilter.class.getName());

	private OAObject objFrom;
	private Object objFromPPValue;

	private final Hub hubFrom; // uses AO
	private Object objHubFromAO; // last (AO) Object used for hubFrom

	private final String strFromPropPathOrig;
	private String strFromPropPath;
	private OAPropertyPath ppFrom;

	private String strToPropPath;
	private OAPropertyPath ppTo;

	private OAFinder finder;
	private OAFinder finderCompareObject;
	private boolean bHasFilter;

	/**
	 * Uses a fromHub.AO and property path to see if it matches the value from a property path of another object.
	 *
	 * @param fromHub  uses the AO
	 * @param propPathFrom to use for finding object that matches the object propertyPath of a compared object.
	 */
	public OAEqualPathFilter(Hub fromHub, String propPathFrom, String propPathTo) {
		this.hubFrom = fromHub;
		this.objFrom = null;
		this.strFromPropPathOrig = propPathFrom;
		this.strToPropPath = propPathTo;
		setup();
	}

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
	
	protected void setup() {
		strFromPropPath = strFromPropPathOrig;
		if (strFromPropPath == null) {
			return;
		}
		if (objFrom == null && hubFrom == null) {
			return;
		}

		final Class clazz = hubFrom != null ? hubFrom.getObjectClass() : objFrom != null ? objFrom.getClass() : null;

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

	public OAPropertyPath getPropertyPath() {
		return ppFrom;
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

		if (ppTo == null) {
			ppTo = new OAPropertyPath(obj.getClass(), strToPropPath);
			//qqqqqqq put in OAInFilter
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

		Object objx = ppTo.getValue(obj);

		boolean b = (objx == objFromPPValue);
		return b;
	}

	
	
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
			OAPropertyPath ppRev = ppTo.getReversePropertyPath();
			if (ppTo != null) {
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
