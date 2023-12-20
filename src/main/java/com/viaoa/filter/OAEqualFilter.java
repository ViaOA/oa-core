/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.*;

/**
 * Creates a filter to see if the value from the propertyPath is equals the filter value.
 * 
 * @author vvia
 * @see OACompare#isEqual(Object, Object)
 */
public class OAEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OAEqualFilter.class.getName());
    private Object matchValue;
    private boolean bIgnoreCase;//was: =true;
    private OAPropertyPath pp;
    private OAFinder finder;
    private int deciPlaces = -1;

    public OAEqualFilter(Object matchValue) {
        this.matchValue = matchValue;
        bSetup = true;
    }

    public OAEqualFilter(String pp, Object matchValue) {
        this(pp==null?null:new OAPropertyPath(pp), matchValue);
    }
    
    public OAEqualFilter(OAPropertyPath pp, Object matchValue) {
        this.pp = pp;
        this.matchValue = matchValue;
    }

    public OAEqualFilter(OAPropertyPath pp, Object matchValue, boolean bIgnoreCase) {
        this.pp = pp;
        this.matchValue = matchValue;
        this.bIgnoreCase = bIgnoreCase;
    }

    public OAEqualFilter(OAPropertyPath pp, Object matchValue, int deciPlaces) {
        this.pp = pp;
        this.matchValue = matchValue;
        this.deciPlaces = deciPlaces;
    }
    
    
    /**
     * Default is true.
     */
    public void setIgnoreCase(boolean b) {
        this.bIgnoreCase = b;
    }

    public void setDeciPlaces(int dp) {
        this.deciPlaces = dp;
    }
    public int getDeciPlaces() {
        return this.deciPlaces;
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
                OAEqualFilter f = new OAEqualFilter(fi.pp, matchValue);
                f.setIgnoreCase(bIgnoreCase);
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

        //  20171212 check to see if object is in a hub
        if (obj instanceof Hub) {
            Hub h = (Hub) obj;
            return h.contains(matchValue);
        }
        boolean b = bIgnoreCase && (obj instanceof String) && (matchValue instanceof String);
        if (b) return OACompare.isEqual(obj, matchValue, b);
        
        if (deciPlaces >= 0 && obj != null && matchValue != null) {
            if (OAReflect.isFloat(obj.getClass()) && OAReflect.isFloat(matchValue.getClass())) {
                return OACompare.isEqual(obj, matchValue, deciPlaces);
            }
        }

        return OACompare.isEqual(obj, matchValue);
    }
    
    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

