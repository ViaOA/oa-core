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

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Creates a filter to see if the value from the propertyPath is indexOf >= 0 (string) the filter value.
 *
 * @author vvia
 * @see OACompare#isEqual(Object, Object)
 */
public class OAContainsFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAContainsFilter.class.getName());
	private Object value;
	private boolean bIgnoreCase;
	private OAPropertyPath pp;
	private OAFinder finder;

	public OAContainsFilter(Object value) {
		this.value = value;
		bSetup = true;
	}

	public OAContainsFilter(OAPropertyPath pp, Object value) {
		this.pp = pp;
		this.value = value;
	}

	public OAContainsFilter(String pp, Object value) {
		this(pp == null ? null : new OAPropertyPath(pp), value);
	}

	public OAContainsFilter(Object value, boolean bIgnoreCase) {
		this.value = value;
		this.bIgnoreCase = bIgnoreCase;
		bSetup = true;
	}

	public OAContainsFilter(OAPropertyPath pp, Object value, boolean bIgnoreCase) {
		this.pp = pp;
		this.value = value;
		this.bIgnoreCase = bIgnoreCase;
	}

	public OAContainsFilter(String pp, Object value, boolean bIgnoreCase) {
		this(pp == null ? null : new OAPropertyPath(pp), value, bIgnoreCase);
	}

	private boolean bSetup;
	private int cntError;

	@Override
	public boolean isUsed(Object obj) {
		if (!bSetup && pp != null && obj != null) {
			// see if an oaFinder is needed
			bSetup = true;
			FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
			if (fi != null) {
				this.finder = fi.finder;
				OAFilter f = new OAContainsFilter(fi.pp, value, bIgnoreCase);
				finder.addFilter(f);
			}
		}

		if (finder != null) {
			if (obj instanceof OAObject) {
				obj = finder.findFirst((OAObject) obj);
				return obj != null;
			} else if (obj instanceof Hub) {
				obj = finder.findFirst((Hub) obj);
				return obj != null;
			}
		}
		obj = getPropertyValue(obj);

		String s1 = OAString.toString(obj);
		String s2 = OAString.toString(value);
		if (s1 == null || s2 == null) {
			return false;
		}

		if (bIgnoreCase) {
			s1 = s1.toUpperCase();
			s2 = s2.toUpperCase();
		}
		boolean b = s1.indexOf(s2) >= 0;
		return b;
	}

	protected Object getPropertyValue(Object obj) {
		Object objx = obj;
		if (pp != null) {
			objx = pp.getValue(obj);
		}
		return objx;
	}
}
