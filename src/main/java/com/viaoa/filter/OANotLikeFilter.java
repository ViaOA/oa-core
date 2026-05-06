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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.compare.OACompare;
import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Filter that evaluates whether the string representation of a property
 * value does <em>not</em> match a SQL-style LIKE pattern.  Uses
 * {@link com.viaoa.compare.OACompare#isLike(Object, Object)} internally and
 * negates the result.
 *
 * <p>
 * Nested property path traversal is supported via {@link OAPath}.
 * If a many-relationship is encountered, an {@link OAFinder} is created and
 * a nested {@code OANotLikeFilter} is added to the finder so that the LIKE
 * evaluation is performed on the located object.
 * </p>
 *
 * <p>
 * Useful for excluding values matching wildcard patterns such as
 * {@code "abc*"}, {@code "*xyz"}, etc.
 * </p>
 */
public class OANotLikeFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANotLikeFilter.class.getName());
    
    /**
     * Optional property path used to retrieve the value from the evaluated
     * object before applying the NOT LIKE comparison.
     */
    private OAPath pp;
    
    /**
     * The SQL-style LIKE pattern whose negated match result determines
     * whether the filter condition is satisfied.
     */
    private Object value;
    
    /**
     * Finder created when the property path crosses a many-relationship.
     * Enables resolving the correct target object before performing the
     * NOT LIKE comparison.
     */
    private OAFinder finder;
    
    /**
     * Creates a filter that evaluates whether the object’s string
     * representation does not match the supplied LIKE pattern.
     *
     * @param value the LIKE pattern to test against
     */
    public OANotLikeFilter(Object value) {
        this.value = value;
    }

    /**
     * Creates a filter that evaluates whether the value retrieved through
     * the supplied property path does not match the LIKE pattern.
     *
     * @param pp the property path used to extract the value
     * @param value the LIKE pattern to compare against
     */
    public OANotLikeFilter(OAPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that builds a property-path–based filter from
     * a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     * @param value the LIKE pattern to compare against
     */
    public OANotLikeFilter(String pp, Object value) {
        this(pp==null?null:new OAPath(pp), value);
    }

    /**
     * Internal flag indicating whether finder initialization has already
     * occurred. Ensures setup executes only once.
     */
    private boolean bSetup;
    
    /**
     * Tracks errors encountered during evaluation. Not further used in the
     * visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the property-path–resolved
     * value) does <em>not</em> match the configured SQL-style LIKE pattern.
     *
     * <p>Evaluation steps:</p>
     * <ul>
     *   <li>Lazy finder creation when the property path involves a
     *       many-relationship,</li>
     *   <li>Using the finder to locate a target OAObject or Hub when
     *       appropriate,</li>
     *   <li>Resolving the property value when no finder is required,</li>
     *   <li>Applying {@link OACompare#isLike(Object, Object)} and negating
     *       the result.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value does not match the LIKE pattern;
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
                OAFilter f = new OANotLikeFilter(fi.pp, value);
                finder.addFilter(f);
            }
        }
        if (finder != null) {
            if (obj instanceof OAObject) {
                obj = finder.findFirst((OAObject)obj);
                return obj != null;
            }
            else if (obj instanceof Hub) {
                obj = finder.findFirst((Hub)obj);
                return obj != null;
            }
        }
		if (obj instanceof OAObject) obj = getPropertyValue((OAObject) obj);
        return !OACompare.isLike(obj, value);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. Returns the object unchanged if no path is defined.
     *
     * @param obj the source object
     * @return the property-path–resolved value or the object itself
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

