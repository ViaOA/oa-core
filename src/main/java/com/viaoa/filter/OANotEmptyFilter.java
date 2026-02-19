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
 * Filter that evaluates whether a property's value is <em>not</em> empty.
 * Uses {@link com.viaoa.util.OACompare#isEmpty(Object, boolean)} to test
 * emptiness.  A property path may be supplied to retrieve the value.
 *
 * <p>
 * When the property path crosses a many-relationship, an {@link OAFinder}
 * is automatically created, and a nested {@code OANotEmptyFilter} is
 * assigned to the finder.  Evaluation then applies to the resolved target.
 * </p>
 *
 * <p>
 * Empty values include:
 * <ul>
 *   <li>null,</li>
 *   <li>empty strings,</li>
 *   <li>empty Hubs, Collections, arrays, etc.</li>
 * </ul>
 * The filter returns {@code true} only when the value is not empty.
 * </p>
 */
public class OANotEmptyFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANotEmptyFilter.class.getName());

    /**
     * Optional property path used to extract the value from the evaluated
     * object before applying the not-empty check.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path crosses a many-relationship,
     * allowing the filter to locate the target object before evaluating
     * whether its value is non-empty.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the object itself is not empty.
     */
    public OANotEmptyFilter() {
    }
    
    /**
     * Creates a filter that evaluates whether the value obtained through
     * the supplied property path is not empty.
     *
     * @param pp the property path used to retrieve the value
     */
    public OANotEmptyFilter(OAPropertyPath pp) {
        this.pp = pp;
    }

    /**
     * Convenience constructor that creates a property-path–based filter from
     * a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     */
    public OANotEmptyFilter(String pp) {
        this(pp==null?null:new OAPropertyPath(pp));
    }
    
    /**
     * Internal flag indicating whether finder initialization has occurred.
     * Ensures setup is performed only once.
     */
    private boolean bSetup;
    
    /**
     * Counter for tracking errors encountered during evaluation. Not further
     * used in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the nested value obtained
     * through a property path) is not empty.
     *
     * <p>Evaluation process:</p>
     * <ul>
     *   <li>If needed, initializes a finder for many-relationship traversal,</li>
     *   <li>Uses the finder to locate the target OAObject or Hub,</li>
     *   <li>Otherwise resolves the value using {@link #getPropertyValue(Object)},</li>
     *   <li>Returns {@code true} when the resolved value is not empty according
     *       to {@link OACompare#isEmpty(Object, boolean)}.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value is not empty; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OANotEmptyFilter(fi.pp);
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
        return !OACompare.isEmpty(obj, true);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is provided, returns the object
     * itself.
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

