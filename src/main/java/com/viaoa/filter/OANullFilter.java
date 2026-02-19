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
 * Filter that evaluates whether a property's value is {@code null}.  The
 * value may be accessed directly or through an {@link OAPropertyPath}.
 *
 * <p>
 * If the property path traverses a many-relationship, an {@link OAFinder}
 * is created and a nested {@code OANullFilter} is added, allowing the null
 * check to apply to the resolved target object.
 * </p>
 *
 * <p>
 * The filter returns {@code true} only when the resolved value is null.
 * </p>
 */
public class OANullFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANullFilter.class.getName());
    
    /**
     * Optional property path used to extract the value from the evaluated
     * object before applying the null check.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path traverses a many-relationship.
     * Allows resolving a target object before performing the null check.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is null.
     */
    public OANullFilter() {
    }
    
    /**
     * Creates a filter that evaluates whether the value obtained using the
     * supplied property path is null.
     *
     * @param pp the property path used to retrieve the value
     */
    public OANullFilter(OAPropertyPath pp) {
        this.pp = pp;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter
     * from a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     */
    public OANullFilter(String pp) {
        this(pp==null?null:new OAPropertyPath(pp));
    }
    
    /**
     * Flag indicating whether finder initialization has occurred. Ensures
     * finder setup runs only once.
     */
    private boolean bSetup;
    
    /**
     * Counter for tracking errors encountered during evaluation. Not used
     * further in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the resolved value when a
     * property path is defined) is {@code null}.
     *
     * <p>Behavior includes:</p>
     * <ul>
     *   <li>Lazy finder creation when the property path traverses a
     *       many-relationship,</li>
     *   <li>Using the finder to locate a target OAObject or Hub, where the
     *       result is considered null only when no matching object is
     *       found,</li>
     *   <li>Otherwise resolving the value using
     *       {@link #getPropertyValue(Object)},</li>
     *   <li>Returning {@code true} only when the resolved value is null.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value is null; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OANullFilter(fi.pp);
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
        return obj == null;
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. Returns the original object when no path is defined.
     *
     * @param obj the source object
     * @return the resolved value or the original object if no path exists
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

