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

import java.lang.reflect.Constructor;
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
 * Creates a filter to see if an object is in a property path or Hub.
 *
 * @author vvia
 */
public class OAInFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAInFilter.class.getName());

	private OAObject objFrom;

	private final Hub hubFrom; // uses AO
	private Object objHubFromAO; // last (AO) Object used for hubFrom

	private final String strPropPathOrig;
	private String strPropPath;
	private OAPropertyPath pp;
	private Hub hubIn;

	private OAPropertyPath ppReverse;
	private String strReversePropPath;
	private OAFinder finder;
	private Object objFind;

	public OAInFilter(Hub hubIn) {
		this.hubIn = hubIn;
		objFrom = null;
		strPropPathOrig = null;
		pp = null;
		hubFrom = null;
	}

	/**
	 * Uses a fromHub.AO and property path for the list of objects that are used to match.
	 *
	 * @param fromHub  uses the AO
	 * @param propPath to use for finding objects
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

	public OAInFilter(OAObject fromObject, String propPath) {
		this.objFrom = fromObject;
		hubFrom = null;
		this.strPropPathOrig = propPath;
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

		if (!pp.isLastPropertyLinkInfo()) {
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
			this.pp = new OAPropertyPath(objFrom.getClass(), strPropPath);
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

		OAPropertyPath ppx = new OAPropertyPath(objFrom.getClass(), ppNew1);
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

		if (hubIn != null && strReversePropPath == null) {
			return hubIn.contains(obj);
		}

		objFind = obj;
		if (strReversePropPath != null) {
			if (ppReverse == null) {
				ppReverse = new OAPropertyPath(obj.getClass(), strReversePropPath);
			}
			objFind = ppReverse.getValue(obj);
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
			bResult = (objFrom == obj);
		} else {
			bResult = false;
		}
		return bResult;
	}

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
