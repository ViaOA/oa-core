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
 * Filter that evaluates whether a property value is considered empty,
 * according to {@link com.viaoa.util.OACompare#isEmpty(Object, boolean)}.
 * A property path may be supplied to evaluate nested values.
 *
 * <p>
 * When the property path routes through multi-valued references, an
 * {@link OAFinder} will be generated and the empty check is applied to the
 * located object.
 * </p>
 */
public class OAEmptyFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAEmptyFilter.class.getName());
    
    /**
     * Optional property path used to extract a nested value from the
     * evaluated object before performing the empty check.
     */
    private OAPropertyPath pp;
    
    /**
     * Finder created when the property path traverses multi-valued
     * references, allowing resolution of a target object prior to
     * evaluating emptiness.
     */
    private OAFinder finder;

    /**
     * Creates an empty filter that evaluates the object itself for
     * emptiness without applying any property path.
     */
    public OAEmptyFilter() {
    }

    /**
     * Creates an empty filter that evaluates the value obtained using the
     * supplied property path.
     *
     * @param pp the property path used to retrieve a nested value
     */
    public OAEmptyFilter(OAPropertyPath pp) {
        this.pp = pp;
    }
    
    /**
     * Convenience constructor that creates a property-path–based filter
     * using a string expression.
     *
     * @param pp the property path expression; may be {@code null}
     */
    public OAEmptyFilter(String pp) {
        this(pp==null?null:new OAPropertyPath(pp));
    }
    
    /**
     * Internal flag used to ensure that finder initialization logic is
     * executed only once on first evaluation.
     */
    private boolean bSetup;
    
    /**
     * Tracks the number of errors encountered during filter evaluation.
     * Its behavior is not expanded within the visible implementation.
     */
    private int cntError;
    
    /**
     * Evaluates whether the object (or a nested value retrieved through
     * the property path) is considered empty. Lazily initializes a finder
     * if the property path traverses multi-valued references. If a finder
     * is used, the method returns whether a matching object is located.
     * Otherwise, emptiness is determined using
     * {@link com.viaoa.util.OACompare#isEmpty(Object, boolean)}.
     *
     * @param obj the object to evaluate
     * @return {@code true} if the evaluated value is empty; otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OAEmptyFilter(fi.pp);
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
        return OACompare.isEmpty(obj, true);
    }

    /**
     * Retrieves the value from the supplied object using the configured
     * property path, if one is present. Otherwise returns the original
     * object.
     *
     * @param obj the source object
     * @return the extracted property value or the object itself
     */
    protected Object getPropertyValue(OAObject obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

