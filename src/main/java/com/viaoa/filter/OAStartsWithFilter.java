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
import com.viaoa.util.OAString;

/**
 * Filter that evaluates whether a property's string value begins with a
 * specified prefix.  The property may be obtained directly from the object
 * or through an {@link OAPropertyPath}.
 *
 * <p><b>Deep property support:</b><br>
 * If the property path crosses a many-relationship, an {@link OAFinder} is
 * created using {@link OAFilterDelegate#createFinder(Class, OAPropertyPath)}.
 * A nested {@code OAStartsWithFilter} is attached to the finder so that the
 * match is executed on the resolved target object.
 * </p>
 *
 * <p><b>Case sensitivity:</b><br>
 * Matching may be performed with or without case sensitivity based on the
 * {@code bIgnoreCase} flag.
 * </p>
 *
 * <p><b>Matching rules:</b><br>
 * Both the object’s property value and the filter’s comparison value are
 * converted to strings via {@link OAString#toString(Object)} before
 * performing the {@code startsWith()} test.
 * </p>
 *
 * <p>
 * This filter is useful for prefix-based text search, auto-complete lists,
 * name/code lookups, and property-driven UI filtering.
 * </p>
 */
public class OAStartsWithFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAStartsWithFilter.class.getName());
    
    /**
     * Value whose string representation is used as the prefix for the
     * {@code startsWith} comparison against the target property value.
     */
    private Object value;
    
    /**
     * Flag indicating whether the {@code startsWith} comparison should be
     * performed without regard to case by uppercasing both operands.
     */
    private boolean bIgnoreCase;
    
    /**
     * Optional property path used to resolve the value to be compared from
     * the target object. When non-{@code null}, this path is evaluated to
     * obtain the object whose string value is tested.
     */
    private OAPropertyPath pp;
    
    /**
     * Optional {@link com.viaoa.object.OAFinder} created when the property path
     * crosses a many-relationship, allowing this filter to evaluate matches
     * against related objects resolved by the finder.
     */
    private OAFinder finder;

    /**
     * Creates a {@link OAStartsWithFilter} that compares the string representation
     * of the given value as a prefix against the target object's property value.
     * The comparison is case-sensitive.
     *
     * @param value the comparison value whose string representation is used as
     *              the prefix in the {@code startsWith} test
     */
    public OAStartsWithFilter(Object value) {
        this.value = value;
        bSetup = true;
    }

    /**
     * Creates a {@link OAStartsWithFilter} that evaluates the value obtained from
     * the supplied property path on the target object and compares it to the
     * supplied value using a case-sensitive {@code startsWith} test.
     *
     * @param pp    the property path used to resolve the value from the target
     *              object; may be {@code null} to use the object itself
     * @param value the comparison value whose string representation is used as
     *              the prefix in the {@code startsWith} test
     */
    public OAStartsWithFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    
    /**
     * Convenience constructor that builds an {@link com.viaoa.util.OAPropertyPath}
     * from the supplied path string and creates a case-sensitive
     * {@link OAStartsWithFilter}.
     *
     * @param pp    the property path expression string; {@code null} to indicate
     *              that the target object itself should be used
     * @param value the comparison value whose string representation is used as
     *              the prefix in the {@code startsWith} test
     */
    public OAStartsWithFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }

    /**
     * Creates a {@link OAStartsWithFilter} that compares the string representation
     * of the given value as a prefix against the target object's property value,
     * optionally ignoring case.
     *
     * @param value       the comparison value whose string representation is used
     *                    as the prefix in the {@code startsWith} test
     * @param bIgnoreCase {@code true} to perform a case-insensitive comparison by
     *                    uppercasing both operands, {@code false} for a case-sensitive
     *                    comparison
     */
    public OAStartsWithFilter(Object value, boolean bIgnoreCase) {
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
        bSetup = true;
    }
    
    /**
     * Creates a {@link OAStartsWithFilter} that evaluates the value obtained from
     * the supplied property path on the target object and compares it to the given
     * value using a {@code startsWith} test with optional case-insensitivity.
     *
     * @param pp          the property path used to resolve the value from the target
     *                    object; may be {@code null} to use the object itself
     * @param value       the comparison value whose string representation is used
     *                    as the prefix in the {@code startsWith} test
     * @param bIgnoreCase {@code true} to perform a case-insensitive comparison by
     *                    uppercasing both operands, {@code false} for a case-sensitive
     *                    comparison
     */
    public OAStartsWithFilter(OAPropertyPath pp, Object value, boolean bIgnoreCase) {
        this.pp = pp;
        this.value = value;
        this.bIgnoreCase = bIgnoreCase;
    }

    /**
     * Convenience constructor that builds an {@link com.viaoa.util.OAPropertyPath}
     * from the supplied path string and creates an {@link OAStartsWithFilter} with
     * optional case-insensitive comparison.
     *
     * @param pp          the property path expression string; {@code null} to indicate
     *                    that the target object itself should be used
     * @param value       the comparison value whose string representation is used
     *                    as the prefix in the {@code startsWith} test
     * @param bIgnoreCase {@code true} to perform a case-insensitive comparison by
     *                    uppercasing both operands, {@code false} for a case-sensitive
     *                    comparison
     */
    public OAStartsWithFilter(String pp, Object value, boolean bIgnoreCase) {
        this(pp==null?null:new OAPropertyPath(pp), value, bIgnoreCase);
    }
    

    /**
     * Indicates whether this filter has been initialized, including any creation
     * and configuration of an {@link com.viaoa.object.OAFinder} based on the
     * property path.
     */
    private boolean bSetup;
    
    /**
     * Internal counter field intended for tracking errors encountered during
     * filter evaluation. Currently not used within this implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the supplied object is accepted by this filter.
     * <ul>
     *   <li>If the filter has not been set up and a property path and non-null
     *       object are available, it calls
     *       {@link com.viaoa.filter.OAFilterDelegate#createFinder(Class, com.viaoa.util.OAPropertyPath)}
     *       to determine if an {@link com.viaoa.object.OAFinder} is needed. When
     *       a finder is returned, it is stored and a nested {@link OAStartsWithFilter}
     *       is added to the finder.</li>
     *   <li>If a finder is present and the object is an {@link com.viaoa.object.OAObject}
     *       or {@link com.viaoa.hub.Hub}, the finder is used to resolve the first
     *       matching related object; the method returns {@code true} if a match is
     *       found and {@code false} otherwise.</li>
     *   <li>Otherwise, the value to test is obtained via {@link #getPropertyValue(Object)},
     *       converted to a string along with the configured comparison value using
     *       {@link com.viaoa.util.OAString#toString(Object)}, and both are compared
     *       using {@code startsWith}. If either string is {@code null}, the method
     *       returns {@code false}. When {@code bIgnoreCase} is {@code true}, both
     *       strings are uppercased before comparison.</li>
     * </ul>
     *
     * @param obj the object to evaluate against this filter; may be an
     *            {@link com.viaoa.object.OAObject} or {@link com.viaoa.hub.Hub},
     *            or any object compatible with the configured property path
     * @return {@code true} if the object (or a related object resolved by a finder)
     *         has a value whose string representation starts with the configured
     *         comparison value, {@code false} otherwise
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OAStartsWithFilter(fi.pp, value, bIgnoreCase);
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
        
        String s1 = OAString.toString(obj);
        String s2 = OAString.toString(value);
        if (s1 == null || s2 == null) return false;
        
        if (bIgnoreCase) {
            s1 = s1.toUpperCase();
            s2 = s2.toUpperCase();
        }
        boolean b = s1.startsWith(s2);
        return b;
    }
    
    /**
     * Resolves the value to be tested from the supplied object.
     * <ul>
     *   <li>If a property path is configured, the path's {@code getValue} method
     *       is invoked on the supplied object and the result is returned.</li>
     *   <li>If no property path is configured, the supplied object is returned
     *       unchanged.</li>
     * </ul>
     *
     * @param obj the source object from which the value should be resolved
     * @return the value obtained by applying the configured property path to the
     *         supplied object, or the object itself when no path is configured
     */
    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

