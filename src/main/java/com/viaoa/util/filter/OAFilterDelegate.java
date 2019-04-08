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
package com.viaoa.util.filter;

import java.lang.reflect.Method;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Helper to used for OAFilters.
 * @author vvia
 *
 */
public class OAFilterDelegate {

    static public class FinderInfo {
        public OAFinder finder;
        public String pp;  // remaining propertyPath to use by the filter
        public FinderInfo(OAFinder f, String pp) {
            this.finder = f;
            this.pp = pp;
        }
    }
    
    /**
     * Create a finder that should be used by a filter, since there
     * are Hub links in the property path.
     * This will return the OAFinder to use, and the remaining property that should be filtered.
     * see the OAxxxFilters for examples. 
     */
    public static FinderInfo createFinder(Class clazz, OAPropertyPath pp) {
        if (clazz == null || pp == null) return null;
        
        String s = pp.getPropertyPath();
        if (s == null || s.indexOf('.') < 0) return null;

        if (pp.getFromClass() == null) {
            pp.setup(clazz);
        }
        Method[] ms = pp.getMethods();

        if (ms == null || ms.length < 2) {
            return null;
        }
        
        boolean b = false;
        for (Method m : ms) {
            if (!m.getReturnType().equals(Hub.class)) continue;
            b = true;
            break;
        }
        if (!b) return null;
        
        Method m = ms[ms.length-1];
        Class c = m.getReturnType();
        if (c.equals(OAObject.class) || c.equals(Hub.class)) {
            OAFinder f = new OAFinder(pp.getPropertyPath());
            return new FinderInfo(f, null);
        }

        int dcnt = OAString.dcount(pp.getPropertyPath(), '.');
        s = OAString.field(pp.getPropertyPath(), '.', 1, dcnt-1);
        OAFinder f = new OAFinder(s);
        s = OAString.field(pp.getPropertyPath(), '.', dcnt);
        
        return new FinderInfo(f, s);
    }

}
