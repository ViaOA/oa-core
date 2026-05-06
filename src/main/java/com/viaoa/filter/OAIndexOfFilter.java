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

import com.viaoa.compare.OACompare;
import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Filter that evaluates whether the string representation of a property
 * contains a given substring at or beyond a specified index.  This allows
 * substring matching with positional requirements.
 *
 * <p>
 * Supports deep property traversal via {@link OAPath}.  If the path
 * crosses a multi-valued reference, an {@link OAFinder} is used to resolve
 * the comparison target before applying the index-based match.
 * </p>
 *
 * <p>
 * Useful for prefix/suffix/contains logic where the substring must appear
 * at a certain location within the value.
 * </p>
 */
public class OAIndexOfFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAIndexOfFilter.class.getName());
    
    /**
     * The substring value whose index position will be searched for within
     * the evaluated property's string representation.
     */
    private Object value;
    
    /**
     * Flag indicating whether substring matching should ignore character
     * case.
     */
    private boolean bIgnoreCase;
    
    /**
     * Optional property path used to extract the target value from the
     * evaluated object prior to performing substring search.
     */
    private OAPath pp;
    
    /**
     * Finder created when the property path traverses a many-relationship,
     * allowing resolution of the appropriate object before evaluation.
     */
    private OAFinder finder;

    /**
     * Creates an index-of filter that evaluates the object's string
     * representation for the presence of the specified substring using
     * case-sensitive comparison.
     *
     * @param value the substring to search for
     */
    public OAIndexOfFilter(Object value) {
        this.value = value;
        bSetup = true;
    }

    /**
     * Creates a filter that evaluates substring position on a value
     * retrieved via a property path.
     *
     * @param pp the property path used to extract the value
     * @param value the substring to search for
     */
    public OAIndexOfFilter(OAPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor using a string-based property-path expression.
     *
     * @param pp the property path expression; may be {@code null}
     * @param value the substring to search for
     */
    public OAIndexOfFilter(String pp, Object value) {
        this(pp==null?null:new OAPath(pp), value);
    }

    /**
     * Creates an index-of filter that optionally ignores character case
     * during substring matching.
     *
     * @param value the substring to search for
     * @param bIgnoreCase {@code true} to ignore case
     */
    public OAIndexOfFilter(Object value, boolean bIgnoreCase) {
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
        bSetup = true;
    }
    
    /**
     * Creates a filter that evaluates substring position on the value
     * retrieved through a property path, using optional case-insensitive
     * comparison.
     *
     * @param pp the property path used to extract the value
     * @param value the substring to search for
     * @param bIgnoreCase whether to ignore case
     */
    public OAIndexOfFilter(OAPath pp, Object value, boolean bIgnoreCase) {
        this.pp = pp;
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
    }

    /**
     * Convenience constructor building a property-path–based filter with
     * optional case-insensitive substring matching.
     *
     * @param pp the property path expression; may be {@code null}
     * @param value the substring to search for
     * @param bIgnoreCase whether to ignore case
     */
    public OAIndexOfFilter(String pp, Object value, boolean bIgnoreCase) {
        this(pp==null?null:new OAPath(pp), value, bIgnoreCase);
    }
    
    /**
     * Tracks whether finder initialization has been performed to avoid
     * duplicate setup.
     */
    private boolean bSetup;

    /**
     * Tracks errors encountered during filter evaluation. Not further used
     * in the visible code.
     */
    private int cntError;
    
    /**
     * Evaluates whether the substring value exists within the string
     * representation of the evaluated object or its property-path value.
     * Initializes a finder if multi-valued traversal is required.
     *
     * @param obj the object being evaluated
     * @return {@code true} if the substring occurs at any index, otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OAIndexOfFilter(fi.pp, value, bIgnoreCase);
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
        
        String s1 = OAString.toString(obj);
        String s2 = OAString.toString(value);
        if (s1 == null || s2 == null) return false;
        
        if (bIgnoreCase) {
            s1 = s1.toUpperCase();
            s2 = s2.toUpperCase();
        }
        int x = s1.indexOf(s2);
        return x >= 0;
    }
    
    /**
     * Retrieves the value from the object using the configured property
     * path. If none is set, the object itself is used.
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

