/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
 * value does <em>not</em> match a SQL-style LIKE pattern.  Uses
 * {@link com.viaoa.util.OACompare#isLike(Object, Object)} internally and
 * negates the result.
 *
 * <p>
 * Nested property path traversal is supported via {@link OAPropertyPath}.
 * If a many-relationship is encountered, an {@link OAFinder} is created and
 * a nested {@code OANotLikeFilter} is added to the finder so that the LIKE
 * evaluation is performed on the located object.
 * </p>
 *
 * <p>
 * Useful for excluding values matching wildcard patterns such as
 * {@code "abc*"}, {@code "*xyz"}, etc.
 * </p>
 */
public class OANotLikeFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OANotLikeFilter.class.getName());
    private OAPropertyPath pp;
    private Object value;
    private OAFinder finder;
    
    public OANotLikeFilter(Object value) {
        this.value = value;
    }

    public OANotLikeFilter(OAPropertyPath pp, Object value) {
        this.pp = pp;
        this.value = value;
    }
    public OANotLikeFilter(String pp, Object value) {
        this(pp==null?null:new OAPropertyPath(pp), value);
    }

    private boolean bSetup;
    private int cntError;
    
    @Override
    public boolean isUsed(Object obj) {
        if (!bSetup && pp != null && obj != null) {
            // see if an oaFinder is needed
            bSetup = true;
            FinderInfo fi = OAFilterDelegate.createFinder(obj.getClass(), pp);
            if (fi != null) {
                this.finder = fi.finder;
                OAFilter f = new OANotLikeFilter(fi.pp, value);
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
        return !OACompare.isLike(obj, value);
    }

    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

