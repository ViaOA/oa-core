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
 * Filter that evaluates whether a property's value is less than a specified
 * comparison value.  Uses {@link OACompare#isLess(Object, Object)} for the
 * comparison, supporting both direct and property-path-based values.
 *
 * <p>
 * If the supplied property path navigates through a many-relationship,
 * an {@link OAFinder} is created to resolve the comparison target.
 * </p>
 */
public class OALessFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OALessFilter.class.getName());

    /**
     * Optional property path used to retrieve the value from the evaluated
     * object before applying the less-than comparison.
     */
    private OAPath pp;
    
    /**
     * Finder created when the property path traverses a many-relationship,
     * allowing the filter to locate the correct target object before
     * comparison.
     */
    private OAFinder finder;

    /**
     * The comparison value used to determine whether the evaluated value
     * is less than this value.
     */
    private Object value;
    
    /**
     * Creates a filter that evaluates whether the given object is less than
     * the supplied comparison value.
     *
     * @param value the value to compare against
     */
    public OALessFilter(Object value) {
        this.value = value;
    }
    
    /**
     * Creates a filter that evaluates whether the value obtained from the
     * supplied property path is less than the comparison value.
     *
     * @param pp the property path used to extract the value from the object
     * @param value the value to compare against
     */
    public OALessFilter(OAPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter
     * from a string expression.
     *
     * @param pp the property-path expression; may be {@code null}
     * @param value the value to compare against
     */
    public OALessFilter(String pp, Object value) {
        this(pp==null?null:new OAPath(pp), value);
    }

    /**
     * Indicates whether finder initialization has been performed to allow
     * lazy setup of many-relationship traversal.
     */
    private boolean bSetup;
    
    /**
     * Counter for tracking errors encountered during evaluation. Its
     * behavior is not expanded in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the value resolved through
     * the configured property path) is less than the configured comparison
     * value. Lazily initializes a finder when a many-relationship is present
     * in the property path and uses it to locate the comparison target.
     *
     * @param obj the object being evaluated
     * @return {@code true} if the evaluated value is less than the comparison
     *         value; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OALessFilter(fi.pp, value);
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
        return OACompare.isLess(obj, value);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is defined, the object itself is
     * returned.
     *
     * @param obj the source object
     * @return the extracted property value or the original object
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }

}

