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
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Filter that evaluates whether a property's value is greater than a
 * specified comparison value.  Comparison is performed using
 * {@link OACompare#isGreater(Object, Object)} and supports both direct
 * properties and values retrieved through an {@link OAPropertyPath}.
 *
 * <p>
 * If the property path crosses a many-relationship, an {@link OAFinder}
 * is created so that the comparison is applied to the located target
 * object.
 * </p>
 */
public class OAGreaterFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAGreaterFilter.class.getName());
    
    /**
     * Optional property path used to obtain a nested value from the evaluated
     * object before performing the greater-than comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * The comparison value used to determine whether the evaluated property
     * value is greater.
     */
    private Object value;
    
    /**
     * Finder created when the property path crosses a many-relationship.
     * Used to locate the correct target object before comparison.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is greater
     * than the specified comparison value.
     *
     * @param value the value to compare against
     */
    public OAGreaterFilter(Object value) {
        this.value = value;
    }

    /**
     * Creates a filter that evaluates whether the value obtained through
     * the given property path is greater than the specified comparison value.
     *
     * @param pp the property path used to retrieve the target value
     * @param value the value to compare against
     */
    public OAGreaterFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }

    /**
     * Convenience constructor that creates a property-path–based filter using
     * a string expression.
     *
     * @param pp the property path expression; may be {@code null}
     * @param value the value to compare against
     */
    public OAGreaterFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }
    
    /**
     * Internal flag indicating whether finder setup has already been performed.
     */
    private boolean bSetup;

    /**
     * Tracks internal errors during filter evaluation. Behavior not expanded
     * in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or a nested value retrieved
     * through its property path) is greater than the configured comparison
     * value. If needed, initializes a finder to resolve many-relationship
     * traversal before comparison.
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value is greater; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OAGreaterFilter(fi.pp, value);
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
        return OACompare.isGreater(obj, value);
    }

    /**
     * Retrieves the value from the object using the configured property
     * path. If no property path is defined, returns the object unchanged.
     *
     * @param obj the source object
     * @return the extracted value or the original object
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

