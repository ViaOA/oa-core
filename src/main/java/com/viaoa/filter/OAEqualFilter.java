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

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.*;

/**
 * Filter that evaluates equality between a property value and a comparison
 * value.  Supports multiple comparison modes, including:
 *
 * <ul>
 *   <li>direct object equality,</li>
 *   <li>string equality (with optional ignore-case),</li>
 *   <li>decimal-place comparison for floating-point values,</li>
 *   <li>Hub membership when the property value is a {@link Hub}.</li>
 * </ul>
 *
 * <p>
 * A property path may be supplied to read nested values, and if the path
 * traverses a multi-valued reference, an {@link OAFinder} is generated so
 * that the comparison is applied to the located target object.
 * </p>
 *
 * @see com.viaoa.util.OACompare#isEqual(Object, Object)
 */
public class OAEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAEqualFilter.class.getName());
    
    /**
     * The comparison value used to determine equality with the evaluated
     * property value.
     */
    private Object matchValue;
    
    /**
     * Indicates whether string comparisons should ignore character case
     * during equality evaluation.
     */
    private boolean bIgnoreCase;//was: =true;
    
    /**
     * Optional property path used to obtain a nested value from the object
     * before performing the equality comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path traverses multi-valued
     * references, enabling the filter to evaluate the located target
     * object instead of the source object.
     */
    private OAFinder finder;
    
    /**
     * Optional number of decimal places to use when comparing floating-point
     * values. A negative value disables decimal-place comparison.
     */
    private int deciPlaces = -1;

    /**
     * Creates an equality filter using the supplied comparison value.  
     * Case-sensitive comparison is used by default.
     *
     * @param matchValue the value to compare against
     */
    public OAEqualFilter(Object matchValue) {
        this.matchValue = matchValue;
        bSetup = true;
    }

    /**
     * Convenience constructor that creates a property-path–based filter
     * using a string expression.
     *
     * @param pp the property path expression; may be {@code null}
     * @param matchValue the value to compare against
     */
    public OAEqualFilter(String pp, Object matchValue) {
        this(pp==null?null:new OAPropertyPath(pp), matchValue);
    }
    
    /**
     * Creates a filter that evaluates equality using the value resolved
     * through the supplied property path.
     *
     * @param pp the property path used to retrieve values from the object
     * @param matchValue the value to compare against
     */
    public OAEqualFilter(OAPropertyPath pp, Object matchValue) {
        this.pp = pp;
        this.matchValue = matchValue;
    }

    /**
     * Convenience constructor allowing case-insensitive evaluation with a
     * property path expressed as a string.
     *
     * @param pp the property path expression; may be {@code null}
     * @param matchValue the value to compare against
     * @param bIgnoreCase {@code true} to ignore case for string comparisons
     */
    public OAEqualFilter(String pp, Object matchValue,  boolean bIgnoreCase) {
        this(pp==null?null:new OAPropertyPath(pp), matchValue, bIgnoreCase);
    }
    
    /**
     * Creates a filter that uses the supplied property path and optional
     * case-insensitive comparison.
     *
     * @param pp the property path for retrieving the value from the object
     * @param matchValue the value to compare against
     * @param bIgnoreCase {@code true} to ignore case during string comparison
     */
    public OAEqualFilter(OAPropertyPath pp, Object matchValue, boolean bIgnoreCase) {
        this.pp = pp;
        this.matchValue = matchValue;
        this.bIgnoreCase = bIgnoreCase;
    }

    /**
     * Convenience constructor that creates a filter using a property path
     * expression and applies decimal-place comparison for floating-point
     * values.
     *
     * @param pp the property path expression; may be {@code null}
     * @param matchValue the value to compare against
     * @param deciPlaces number of decimal places to use for float comparison
     */
    public OAEqualFilter(String pp, Object matchValue, int deciPlaces) {
        this(pp==null?null:new OAPropertyPath(pp), matchValue, deciPlaces);
    }
    
    /**
     * Creates a filter using the supplied property path and decimal-place
     * precision for floating-point comparisons.
     *
     * @param pp the property path to evaluate
     * @param matchValue the value to compare against
     * @param deciPlaces number of decimal places for equality checks
     */
    public OAEqualFilter(OAPropertyPath pp, Object matchValue, int deciPlaces) {
        this.pp = pp;
        this.matchValue = matchValue;
        this.deciPlaces = deciPlaces;
    }
    
    
    /**
     * Sets whether string comparisons should ignore character case.
     *
     * @param b {@code true} to ignore case; otherwise case-sensitive
     */
    public void setIgnoreCase(boolean b) {
        this.bIgnoreCase = b;
    }

    /**
     * Sets the number of decimal places to use when comparing floating-point
     * values.
     *
     * @param dp the decimal-place precision to apply
     */
    public void setDeciPlaces(int dp) {
        this.deciPlaces = dp;
    }

    /**
     * Returns the number of decimal places configured for floating-point
     * comparison.
     *
     * @return the decimal-place precision, or a negative value if disabled
     */
    public int getDeciPlaces() {
        return this.deciPlaces;
    }
    
    /**
     * Internal flag used to ensure finder setup logic is performed only once.
     */
    private boolean bSetup;
    
    /**
     * Tracks the number of errors encountered during equality evaluation.
     * Its behavior is not expanded in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or a nested value resolved via
     * the property path) is equal to the configured comparison value. Finder
     * initialization occurs lazily if the property path traverses
     * multi-valued references. Special cases include:
     * <ul>
     *   <li>Hub membership checks,</li>
     *   <li>case-insensitive string comparison,</li>
     *   <li>decimal-place float comparison.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the values are considered equal, otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAEqualFilter f = new OAEqualFilter(fi.pp, matchValue);
                f.setIgnoreCase(bIgnoreCase);
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
        
        obj = getPropertyValue(obj);

        //  check to see if object is in a hub
        if (obj instanceof Hub) {
            Hub h = (Hub) obj;
            return h.contains(matchValue);
        }
        boolean b = bIgnoreCase && (obj instanceof String) && (matchValue instanceof String);
        if (b) return OACompare.isEqual(obj, matchValue, b);
        
        if (deciPlaces >= 0 && obj != null && matchValue != null) {
            if (OAReflect.isFloat(obj.getClass()) && OAReflect.isFloat(matchValue.getClass())) {
                return OACompare.isEqual(obj, matchValue, deciPlaces);
            }
        }

        return OACompare.isEqual(obj, matchValue);
    }
    
    /**
     * Retrieves the property's value using the configured property path,
     * if present. Otherwise returns the supplied object unchanged.
     *
     * @param obj the source object
     * @return the extracted property value or the original object
     */
    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

