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

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Filter that evaluates whether a property value is <em>not equal</em> to
 * a specified comparison value.  Supports optional case-insensitive string
 * comparison, Hub membership checks, and nested property path traversal.
 *
 * <p>
 * If the property path traverses a many-relationship, an {@link OAFinder}
 * is created and a nested {@code OANotEqualFilter} is added to the finder
 * for correct deep evaluation.
 * </p>
 *
 * <p>
 * Special handling applies when the resolved value is a {@link Hub}: the
 * filter returns {@code true} if the Hub does <em>not</em> contain the
 * comparison object.
 * </p>
 *
 * <p>
 * All other comparisons delegate to
 * {@link com.viaoa.util.OACompare#isEqual(Object, Object, boolean)} and
 * invert the result.
 * </p>
 */
public class OANotEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANotEqualFilter.class.getName());

    /**
     * The comparison value used to determine inequality.
     */
    private Object value;

    /**
     * Indicates whether string comparisons should ignore case sensitivity.
     */
    private boolean bIgnoreCase;
    
    /**
     * Optional property path used to retrieve the value from the evaluated
     * object before applying the not-equal comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path traverses a many-relationship.
     * Enables resolving the correct target object before the inequality
     * comparison is applied.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is not equal
     * to the supplied comparison value.
     *
     * @param value the comparison value
     */
    public OANotEqualFilter(Object value) {
        this.value = value;
    }
    
    /**
     * Creates a filter that evaluates whether the object is not equal to
     * the supplied comparison value, using optional case-insensitive
     * comparison for strings.
     *
     * @param value the comparison value
     * @param bIgnoreCase whether to ignore case when comparing strings
     */
    public OANotEqualFilter(Object value, boolean bIgnoreCase) {
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
    }

    /**
     * Creates a filter that evaluates whether the value obtained through
     * the supplied property path is not equal to the comparison value.
     *
     * @param pp the property path used to retrieve the value
     * @param value the comparison value
     */
    public OANotEqualFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter from
     * a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     * @param value the comparison value
     */
    public OANotEqualFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }

    /**
     * Creates a filter that evaluates whether the value retrieved through
     * the property path is not equal to the comparison value, with optional
     * case-insensitive comparison for strings.
     *
     * @param pp the property path used to retrieve the value
     * @param value the comparison value
     * @param bIgnoreCase whether to ignore case during comparison
     */
    public OANotEqualFilter(OAPropertyPath pp, Object value, boolean bIgnoreCase) {
        this.pp = pp;
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
    }
    
    /**
     * Convenience constructor creating a property-path–based filter from a
     * string expression, supporting case-insensitive string comparison.
     *
     * @param pp the property-path expression; may be {@code null}
     * @param value the comparison value
     * @param bIgnoreCase whether to ignore case during comparison
     */
    public OANotEqualFilter(String pp, Object value, boolean bIgnoreCase) {
        this(pp==null?null:new OAPropertyPath(pp), value, bIgnoreCase);
    }


    /**
     * Internal flag indicating whether finder setup has been performed.
     * Prevents multiple initialization passes.
     */
    private boolean bSetup;
    
    /**
     * Counter for errors encountered during evaluation. Not used further
     * in the visible code.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the value retrieved through
     * its property path) is not equal to the configured comparison value.
     *
     * <p>Evaluation behavior includes:</p>
     * <ul>
     *   <li>Lazy finder initialization for many-relationship traversal,</li>
     *   <li>Using the finder to locate a target OAObject or Hub when needed,</li>
     *   <li>Handling Hub values specially by checking membership against
     *       the comparison value,</li>
     *   <li>Delegating all other comparisons to
     *       {@link OACompare#isEqual(Object, Object, boolean)} and negating
     *       the result.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value is not equal to the comparison value;
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
                OAFilter f = new OANotEqualFilter(fi.pp, value, bIgnoreCase);
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
        
        // 20171212 check to see if object is in a hub
        if (obj instanceof Hub) {
            Hub h = (Hub) obj;
            return !h.contains(value);
        }
        
        return !OACompare.isEqual(obj, value, bIgnoreCase);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is provided, returns the object
     * unchanged.
     *
     * @param obj the source object
     * @return the resolved property value or the original object
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

