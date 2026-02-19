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
 * Filter that evaluates whether a property's value is <em>not null</em>.
 * A property path may be provided to locate the value.
 *
 * <p>
 * When a property path crosses a many-relationship, an {@link OAFinder}
 * is automatically built and a nested {@code OANotNullFilter} is assigned
 * to it, allowing the filter to operate on the resolved child object.
 * </p>
 *
 * <p>
 * Returns {@code true} only when the resolved value is non-null.
 * </p>
 */
public class OANotNullFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANotNullFilter.class.getName());

    /**
     * Optional property path used to extract the value from the evaluated
     * object before applying the not-null check.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the supplied property path crosses a
     * many-relationship. Allows the filter to locate the target object
     * before evaluating whether it is non-null.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is not null.
     */
    public OANotNullFilter() {
    }
    
    /**
     * Creates a filter that evaluates whether the value obtained from the
     * supplied property path is not null.
     *
     * @param pp the property path used to retrieve the value
     */
    public OANotNullFilter(OAPropertyPath pp) {
        this.pp = pp;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter
     * from a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     */
    public OANotNullFilter(String pp) {
        this(pp==null?null:new OAPropertyPath(pp));
    }
    
    /**
     * Internal flag indicating whether finder initialization has already
     * been performed. This prevents repeated setup.
     */
    private boolean bSetup;
    
    /**
     * Counter for tracking errors encountered during evaluation. Not used
     * further in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the value resolved through
     * its property path) is non-null.
     *
     * <p>Evaluation steps:</p>
     * <ul>
     *   <li>On first use, determines whether a finder is needed based on the
     *       property path and initializes it if required,</li>
     *   <li>When a finder is present, uses it to resolve the target object
     *       from an {@link OAObject} or {@link Hub}, returning {@code true}
     *       only when the result is non-null,</li>
     *   <li>Otherwise resolves the value using
     *       {@link #getPropertyValue(Object)},</li>
     *   <li>Returns {@code true} when the resolved value is not {@code null}.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the resolved value is non-null; otherwise
     *         {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OANotNullFilter(fi.pp);
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
        return obj != null;
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is defined, returns the original
     * object unchanged.
     *
     * @param obj the source object
     * @return the resolved property-path value or the original object
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

