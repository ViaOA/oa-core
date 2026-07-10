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
 * Filter that evaluates whether a property value is between two comparison
 * values, including equality on either boundary.  Supports both direct
 * comparison and values obtained through an {@link OAPath}.
 *
 * <p>
 * If the OAPath traverses multiple objects, an {@link OAFinder}
 * will be dynamically created so the comparison can be applied to the
 * located target object.
 * </p>
 *
 * @see com.viaoa.compare.OACompare#isBetweenOrEqual(Object, Object, Object)
 */
public class OABetweenOrEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OABetweenOrEqualFilter.class.getName());

    /**
     * The lower and upper comparison values that define the inclusive range
     * used by this filter.
     */
    private Object value1, value2;
    
    /**
     * Optional OAPath used to extract the comparison value from the
     * evaluated object. When {@code null}, the object itself is used.
     */
    private OAPath pp;
    
    /**
     * Finder created when the OAPath traverses a multi-object
     * relationship. Used to locate the target object before comparison.
     */
    private OAFinder finder;

    /**
     * Creates a filter that evaluates objects directly using the supplied
     * lower and upper inclusive bound values.
     *
     * @param val1 the lower comparison value
     * @param val2 the upper comparison value
     */
    public OABetweenOrEqualFilter(Object val1, Object val2) {
        this.value1 = val1;
        this.value2 = val2;
    }

    /**
     * Creates a filter that evaluates whether a value obtained by the given
     * OAPath lies between or is equal to the supplied bounds.
     *
     * @param pp the OAPath used to extract a comparison value
     * @param val1 the lower comparison value
     * @param val2 the upper comparison value
     */
    public OABetweenOrEqualFilter(OAPath pp, Object val1, Object val2) {
        this.pp = pp;
        this.value1 = val1;
        this.value2 = val2;
    }

    /**
     * Creates a filter from a string representation of an OAPath. The
     * string is converted into an {@link OAPath} unless it is null.
     *
     * @param pp the OAPath expression
     * @param val1 the lower comparison value
     * @param val2 the upper comparison value
     */
    public OABetweenOrEqualFilter(String pp, Object val1, Object val2) {
        this(pp==null?null:new OAPath(pp), val1, val2);
    }

    /**
     * Flag indicating whether the filter has performed finder setup for the
     * current OAPath.
     */
    private boolean bSetup;

    /**
     * Counter reserved for tracking errors occurring during setup or
     * evaluation. Currently unused.
     */
    private int cntError;
    
    /**
     * Determines whether the supplied object satisfies the inclusive between
     * condition defined by this filter.
     * <p>
     * On first evaluation, if an OAPath is present, the filter checks
     * whether a finder is required. When a finder exists, the filter evaluates
     * the first located object in the referenced OA object state. Otherwise, the value
     * is obtained directly and compared.
     * </p>
     *
     * @param obj the object to evaluate
     * @return {@code true} if the value lies between or is equal to the bounds;
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
                OAFilter f = new OABetweenOrEqualFilter(fi.pp, value1, value2);
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
        return OACompare.isBetweenOrEqual(obj, value1, value2);
    }

    /**
     * Retrieves the value used for comparison. If an OAPath is defined,
     * it is used to extract the nested value; otherwise, the supplied object
     * is returned unchanged.
     *
     * @param obj the source object
     * @return the extracted comparison value
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

