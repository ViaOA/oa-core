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
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Filter that evaluates whether a property's value is greater than or equal
 * to a specified comparison value.  Uses
 * {@link OACompare#isGreaterOrEqual(Object, Object)} for comparison.
 *
 * <p>
 * Supports nested properties through {@link OAPropertyPath} and automatically
 * constructs an {@link OAFinder} when the property path encounters a
 * many-relationship.
 * </p>
 */
public class OAGreaterOrEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAGreaterOrEqualFilter.class.getName());
    
    /**
     * Optional property path used to retrieve a nested value from the
     * evaluated object before applying the comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * The comparison value used to determine whether the evaluated value
     * is greater than or equal to this value.
     */
    private Object value;
    
    /**
     * Finder created when the property path requires traversal across a
     * many-relationship. Used to locate the correct target object for
     * comparison.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is greater
     * than or equal to the specified comparison value.
     *
     * @param value the value to compare against
     */
    public OAGreaterOrEqualFilter(Object value) {
        this.value = value;
    }
    
    /**
     * Creates a filter that evaluates whether the value retrieved through
     * the supplied property path is greater than or equal to the comparison
     * value.
     *
     * @param pp the property path used to obtain the value to compare
     * @param value the comparison value
     */
    public OAGreaterOrEqualFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that creates a property-path-based filter
     * from a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     * @param value the comparison value
     */
    public OAGreaterOrEqualFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }

    /**
     * Internal flag indicating whether finder initialization has been
     * performed to avoid repeated setup.
     */
    private boolean bSetup;
    
    /**
     * Tracks the number of errors encountered during evaluation.
     * Not further utilized in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or a nested value resolved
     * through the property path) is greater than or equal to the configured
     * comparison value. Automatically initializes a finder when the property
     * path traverses a many-relationship.
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value satisfies the greater-or-equal condition;
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
                OAFilter f = new OAGreaterOrEqualFilter(fi.pp, value);
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
        return OACompare.isGreaterOrEqual(obj, value);
    }

    /**
     * Retrieves the value from the object using the configured property
     * path. If no property path is defined, the object itself is returned.
     *
     * @param obj the source object
     * @return the resolved property value or the original object
     */
    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

