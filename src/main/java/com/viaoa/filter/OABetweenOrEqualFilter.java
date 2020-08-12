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
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Creates a filter to see if the value from the propertyPath is between or equal two values.
 * @see OACompare#isBetweenOrEqual(Object, Object, Object)
 * @author vvia
 */
public class OABetweenOrEqualFilter implements OAFilter {
    private static Logger LOG = Logger.getLogger(OABetweenOrEqualFilter.class.getName());

    private Object value1, value2;
    private OAPropertyPath pp;
    private OAFinder finder;

    public OABetweenOrEqualFilter(Object val1, Object val2) {
        this.value1 = val1;
        this.value2 = val2;
    }
    public OABetweenOrEqualFilter(OAPropertyPath pp, Object val1, Object val2) {
        this.pp = pp;
        this.value1 = val1;
        this.value2 = val2;
    }
    public OABetweenOrEqualFilter(String pp, Object val1, Object val2) {
        this(pp==null?null:new OAPropertyPath(pp), val1, val2);
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
        obj = getPropertyValue(obj);
        return OACompare.isBetweenOrEqual(obj, value1, value2);
    }

    protected Object getPropertyValue(Object obj) {
        Object objx = obj;
        if (pp != null) {
            objx = pp.getValue(obj);
        }
        return objx;
    }
}

