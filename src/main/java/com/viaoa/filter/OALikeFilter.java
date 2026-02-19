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
 * Filter that evaluates whether the string representation of a property
 * value matches a SQL-style LIKE pattern (using
 * {@link com.viaoa.util.OACompare#isLike(Object, Object)}).  The value may
 * be retrieved directly or through an {@link OAPropertyPath}.
 *
 * <p>
 * If the property path navigates through a many-relationship, an
 * {@link OAFinder} is created dynamically.  A nested {@code OALikeFilter}
 * is added to the finder so that the LIKE pattern is applied to the
 * located target object.
 * </p>
 *
 * <p>
 * Supports wildcard-based matching (e.g., {@code "abc*"}, {@code "*xyz"},
 * {@code "*mid*"}), depending on the rules in {@code OACompare.isLike}.
 * </p>
 */
public class OALikeFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OALikeFilter.class.getName());
    
    /**
     * Optional property path used to extract a nested value from the
     * evaluated object before applying the LIKE comparison.
     */
    private OAPropertyPath pp;
    
    /**
     * The SQL-style LIKE pattern used for comparison. Supports wildcard
     * matching as implemented by {@link OACompare#isLike(Object, Object)}.
     */
    private Object value;
    
    /**
     * Finder created when the property path traverses a many-relationship,
     * allowing the filter to locate the appropriate target object before
     * performing the LIKE comparison.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates whether the string representation of
     * the object matches the given LIKE pattern.
     *
     * @param value the LIKE pattern used for comparison
     */
    public OALikeFilter(Object value) {
        this.value = value;
    }
    
    /**
     * Creates a filter that evaluates whether the value retrieved through
     * the supplied property path matches the specified LIKE pattern.
     *
     * @param pp the property path used to extract the value
     * @param value the LIKE pattern used for comparison
     */
    public OALikeFilter(OAPropertyPath pp, Object value) {
        this. pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter
     * from a string expression.
     *
     * @param pp the property path expression; may be {@code null}
     * @param value the LIKE pattern used for comparison
     */
    public OALikeFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }
    
    /**
     * Internal flag indicating whether finder initialization has already
     * been performed. Used to prevent repeated setup work.
     */
    private boolean bSetup;
    
    /**
     * Counter for tracking errors during filter evaluation. Not further
     * used in the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object (or the value resolved through
     * its property path) matches the configured LIKE pattern.
     *
     * <p>Evaluation steps:</p>
     * <ul>
     *   <li>Initialize a finder on first use if a many-relationship appears
     *       in the property path,</li>
     *   <li>Use the finder to locate the target object for OAObjects or
     *       Hubs,</li>
     *   <li>Resolve the value through the property path when no finder is
     *       used,</li>
     *   <li>Apply {@link OACompare#isLike(Object, Object)} to evaluate the
     *       pattern match.</li>
     * </ul>
     *
     * @param obj the object being evaluated
     * @return {@code true} if the evaluated value matches the LIKE
     *         pattern; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OALikeFilter(fi.pp, value);
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
        return OACompare.isLike(obj, value);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path. If no property path is defined, returns the object
     * itself.
     *
     * @param obj the source object
     * @return the value retrieved using the property path, or the original
     *         object if no path is defined
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

