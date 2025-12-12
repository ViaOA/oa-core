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

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Filter that evaluates whether a property's string value contains the
 * specified substring.  The comparison may be case-sensitive or
 * case-insensitive, and the target value can be retrieved through an
 * {@link OAPropertyPath}.
 *
 * <p>
 * When the property path traverses a many-relationship, an {@link OAFinder}
 * is created automatically and the contains check is applied to the located
 * object.
 * </p>
 */
public class OAContainsFilter implements OAFilter {
	private static Logger LOG = Logger.getLogger(OAContainsFilter.class.getName());

	/**
	 * The value whose string representation will be searched for within
	 * the evaluated property's string value.
	 */
	private Object value;
	
	/**
	 * Flag indicating whether the substring comparison should ignore
	 * character case during evaluation.
	 */
	private boolean bIgnoreCase;
	
	/**
	 * Optional property path used to extract a target value from the
	 * evaluated object. If {@code null}, the object itself is used.
	 */
	private OAPropertyPath pp;
	
	/**
	 * Optional finder created when the property path traverses a
	 * many-relationship, allowing lookup of the target value before
	 * performing the contains check.
	 */
	private OAFinder finder;

	/**
	 * Creates a filter that checks whether the object's string value
	 * contains the specified value, using case-sensitive comparison.
	 *
	 * @param value the value to search for
	 */
	public OAContainsFilter(Object value) {
		this.value = value;
		bSetup = true;
	}

	/**
	 * Creates a filter that applies a property path to obtain the target
	 * value before performing a case-sensitive contains comparison.
	 *
	 * @param pp the property path used to retrieve a value from the object
	 * @param value the substring to search for
	 */
	public OAContainsFilter(OAPropertyPath pp, Object value) {
		this.pp = pp;
		this.value = value;
	}

	/**
	 * Convenience constructor that creates a property-path–based filter
	 * using a string expression.
	 *
	 * @param pp the property path expression; may be {@code null}
	 * @param value the substring to search for
	 */
	public OAContainsFilter(String pp, Object value) {
		this(pp == null ? null : new OAPropertyPath(pp), value);
	}

	/**
	 * Creates a filter that checks whether the object's string value
	 * contains the specified value, using optional case-insensitive search.
	 *
	 * @param value the value to search for
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 */
	public OAContainsFilter(Object value, boolean bIgnoreCase) {
		this.value = value;
		this.bIgnoreCase = bIgnoreCase;
		bSetup = true;
	}

	/**
	 * Creates a filter that retrieves a value using the supplied property
	 * path and compares it against the target substring, optionally
	 * ignoring case.
	 *
	 * @param pp the property path for retrieving comparison values
	 * @param value the substring to search for
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 */
	public OAContainsFilter(OAPropertyPath pp, Object value, boolean bIgnoreCase) {
		this.pp = pp;
		this.value = value;
		this.bIgnoreCase = bIgnoreCase;
	}

	/**
	 * Convenience constructor using a string expression to create the
	 * property path for value retrieval, with optional case-insensitive
	 * comparison.
	 *
	 * @param pp the property path expression; may be {@code null}
	 * @param value the substring to search for
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 */
	public OAContainsFilter(String pp, Object value, boolean bIgnoreCase) {
		this(pp == null ? null : new OAPropertyPath(pp), value, bIgnoreCase);
	}

	/**
	 * Internal flag indicating whether finder initialization has been
	 * performed for the first time the filter is evaluated.
	 */
	private boolean bSetup;

	/**
	 * Counter used to track errors encountered while evaluating the
	 * filter. Its use is not expanded in the visible implementation.
	 */
	private int cntError;

	/**
	 * Evaluates the object using the configured property path, finder, and
	 * substring comparison rules. If a finder is required, it is lazily
	 * created on first use. When a finder is applied, the method returns
	 * whether a matching object is found. Otherwise, it performs a
	 * contains comparison against the converted string values.
	 *
	 * @param obj the object being evaluated
	 * @return {@code true} if the object's value contains the target
	 *         substring; otherwise {@code false}
	 */
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

	/**
	 * Retrieves the value from the object using the configured property
	 * path, if present. Otherwise returns the object unchanged.
	 *
	 * @param obj the source object
	 * @return the retrieved property value or the original object
	 */
	protected Object getPropertyValue(Object obj) {
		Object objx = obj;
		if (pp != null) {
			objx = pp.getValue(obj);
		}
		return objx;
	}
}
