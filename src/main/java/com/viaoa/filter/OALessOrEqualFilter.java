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
 * Filter that evaluates whether a property's value is less than or equal to
 * a specified comparison value.  The property value may be taken directly
 * from the object or resolved through an {@link OAPropertyPath}.
 *
 * <p>
 * If the property path traverses a many-relationship, an {@link OAFinder}
 * is automatically created and a nested {@code OALessOrEqualFilter} is
 * attached to that finder.  During evaluation the finder locates the target
 * object and the comparison is applied to the resolved value.
 * </p>
 *
 * <p>
 * The final comparison uses
 * {@link com.viaoa.util.OACompare#isLessOrEqual(Object, Object)}.
 * </p>
 *
 * <p>
 * Supports filtering of:
 * <ul>
 *   <li>OAObjects,</li>
 *   <li>Hubs,</li>
 *   <li>nested values via property paths,</li>
 *   <li>multi-valued references via OAFinder.</li>
 * </ul>
 * </p>
 */
public class OALessOrEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OALessOrEqualFilter.class.getName());

    /**
     * Optional property path used to retrieve the value from the evaluated
     * object before applying the less-or-equal comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path traverses a many-relationship.
     * Used to locate the correct target object prior to comparison.
     */
    private OAFinder finder;

    /**
     * The comparison value used to evaluate whether the resolved property
     * value is less than or equal to this value.
     */
    private Object value;
    
    /**
     * Creates a filter that evaluates whether the object itself is less
     * than or equal to the supplied value.
     *
     * @param value the comparison value
     */
    public OALessOrEqualFilter(Object value) {
        this.value = value;
    }
    
    /**
     * Creates a filter that evaluates whether the value obtained from the
     * specified property path is less than or equal to the comparison value.
     *
     * @param pp the property path used to retrieve the value
     * @param value the comparison value
     */
    public OALessOrEqualFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter from
     * a string expression.
     *
     * @param pp the property path expression; may be {@code null}
     * @param value the comparison value
     */
    public OALessOrEqualFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }

    /**
     * Internal flag indicating whether finder initialization has been
     * performed. Ensures setup occurs only once.
     */
    private boolean bSetup;
    
    /**
     * Tracks the number of errors encountered during filter evaluation.
     * Not further used in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the value resolved through
     * the property path) is less than or equal to the configured comparison
     * value. If the property path requires traversal through a many-
     * relationship, a finder is created and a nested filter is attached.
     *
     * <p>Evaluation steps:</p>
     * <ul>
     *   <li>Initialize finder on first use when needed,</li>
     *   <li>Use finder to locate a target object for OAObjects or Hubs,</li>
     *   <li>Resolve the property value when no finder is used,</li>
     *   <li>Apply {@link OACompare#isLessOrEqual(Object, Object)}.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the value is less than or equal to the
     *         comparison value; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OALessOrEqualFilter(fi.pp, value);
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
        return OACompare.isLessOrEqual(obj, value);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is defined, the object itself is
     * returned.
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

