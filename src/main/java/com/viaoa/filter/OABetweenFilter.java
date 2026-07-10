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

import com.viaoa.compare.OACompare;
import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Filter that evaluates whether a property value lies strictly between two
 * comparison values (exclusive).  The comparison may operate directly on
 * the object or on a value obtained through an {@link OAPath}.
 *
 * <p>
 * If the OAPath resolves through a many-relationship, a nested
 * {@link OAFinder} is automatically constructed so that filtering can be
 * applied to the referenced OA object state.
 * </p>
 *
 * @see com.viaoa.compare.OACompare#isBetween(Object, Object, Object)
 */
public class OABetweenFilter<T> implements OAFilter {
	private static Logger LOG = Logger.getLogger(OABetweenFilter.class.getName());

	/**
	 * Optional OAPath used to extract a nested value from the object
	 * being evaluated. If {@code null}, the object itself is compared.
	 */
	private OAPath pp;

	/**
	 * Optional finder constructed when the OAPath traverses a
	 * many-relationship. Used to locate the target object before comparison.
	 */
	private OAFinder finder;
	
	/**
	 * The lower and upper comparison values used for strict between evaluation.
	 */
	private Object value1, value2;

	/**
	 * Creates a filter that compares objects directly against the supplied
	 * lower and upper values.
	 *
	 * @param val1 the lower comparison value
	 * @param val2 the upper comparison value
	 */
	public OABetweenFilter(Object val1, Object val2) {
		this.value1 = val1;
		this.value2 = val2;
	}

	/**
	 * Creates a filter that evaluates whether a value obtained from the given
	 * OAPath lies strictly between the supplied bounds.
	 *
	 * @param pp the OAPath used to extract the comparison value
	 * @param val1 the lower comparison value
	 * @param val2 the upper comparison value
	 */
	public OABetweenFilter(OAPath pp, Object val1, Object val2) {
		this.pp = pp;
		this.value1 = val1;
		this.value2 = val2;
	}

	/**
	 * Creates a filter using a string OAPath expression. The string is
	 * converted into an {@link OAPath} unless {@code null}.
	 *
	 * @param pp the OAPath expression, or {@code null}
	 * @param val1 the lower comparison value
	 * @param val2 the upper comparison value
	 */
	public OABetweenFilter(String pp, Object val1, Object val2) {
		this(pp == null ? null : new OAPath(pp), val1, val2);
	}

	/**
	 * Flag indicating whether finder setup has been attempted for this filter.
	 */
	private boolean bSetup;

	/**
	 * Counter used to record setup or evaluation errors. Reserved for future
	 * diagnostic or throttling logic.
	 */
	private int cntError;

	/**
	 * Evaluates whether the supplied object satisfies the strict between
	 * condition.
	 * <p>
	 * On first use, if an OAPath exists, the method determines whether
	 * a finder is needed. When a finder is present, the filter operates on the
	 * first located object in the referenced OA object state. Otherwise, the comparison
	 * is performed on the extracted (or direct) value.
	 * </p>
	 *
	 * @param obj the object to evaluate
	 * @return {@code true} if the value is between {@code value1} and {@code value2};
	 *         otherwise {@code false}
	 */
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
		if (obj instanceof OAObject) obj = getPropertyValue((OAObject) obj);
		return OACompare.isBetween(obj, value1, value2);
	}

	/**
	 * Extracts the value to compare from the supplied object. If a property
	 * path is defined, it is used to retrieve the nested value.
	 *
	 * @param obj the object from which to extract a value
	 * @return the extracted value, or the original object if no path is defined
	 */
	protected Object getPropertyValue(OAObject obj) {
		Object objx = obj;
		if (pp != null) {
			objx = pp.getValue(obj);
		}
		return objx;
	}

}
