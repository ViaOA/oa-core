/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Filter that evaluates whether a property value lies strictly between two
 * comparison values (exclusive).  The comparison may operate directly on
 * the object or on a value obtained through an {@link OAPropertyPath}.
 *
 * <p>
 * If the property path resolves through a many-relationship, a nested
 * {@link OAFinder} is automatically constructed so that filtering can be
 * applied to the referenced object graph.
 * </p>
 *
 * @see com.viaoa.util.OACompare#isBetween(Object, Object, Object)
 */
public class OABetweenFilter<T> implements OAFilter {
	private static Logger LOG = Logger.getLogger(OABetweenFilter.class.getName());

	private OAPropertyPath pp;
	private OAFinder finder;
	private Object value1, value2;

	public OABetweenFilter(Object val1, Object val2) {
		this.value1 = val1;
		this.value2 = val2;
	}

	public OABetweenFilter(OAPropertyPath pp, Object val1, Object val2) {
		this.pp = pp;
		this.value1 = val1;
		this.value2 = val2;
	}

	public OABetweenFilter(String pp, Object val1, Object val2) {
		this(pp == null ? null : new OAPropertyPath(pp), val1, val2);
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
				OAFilter f = new OABetweenFilter(fi.pp, value1, value2);
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
		return OACompare.isBetween(obj, value1, value2);
	}

	protected Object getPropertyValue(Object obj) {
		Object objx = obj;
		if (pp != null) {
			objx = pp.getValue(obj);
		}
		return objx;
	}

}
