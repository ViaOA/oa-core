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
package com.viaoa.object;

import java.util.HashSet;

import com.viaoa.util.*;
import com.viaoa.util.converter.OAConverterBoolean;
import com.viaoa.util.filter.OAEmptyFilter;
import com.viaoa.util.filter.OANotEmptyFilter;
import com.viaoa.util.filter.OANotNullFilter;

/**
 * This is used to find the first value in an object hierarchy (including recursive) that 
 * has a matching value in the first object, or one of the objects in it's hierarchy,
 * as defined by propertyPaths.
 *
 * example:
 *  Employee.department.location.region.country;
 *  
 *  where location is recursive (has parent locations)
 *  and each object in the hierarchy has a property to know if it has "specialFlag" or not.
 *  
 *  OAHierFinder f = new OAHierFinder(EmployeePP.specialFlag, EmployeePP.location().region().country())
 *
 *  f.findFirstValue(employee, filter);
 *  
 */
public class OAHierFinder<F extends OAObject> {
    private final String property;
    private final String strPropertyPath;
    private OAPropertyPath propertyPath;
    private Object foundValue;
    private boolean bIncludeFromObject;

    
    public OAHierFinder(String propertyName, String propertyPath) {
        this(propertyName, propertyPath, true);
    }
    public OAHierFinder(String propertyName, String propertyPath, boolean bIncludeFromObject) {
        this.property = propertyName;
        this.strPropertyPath = propertyPath;
        this.bIncludeFromObject = bIncludeFromObject;
    }
    
    public Object findFirst(F fromObject, OAFilter filter) {
        if (fromObject == null) return null;

        Class c = fromObject.getClass();
        propertyPath = new OAPropertyPath(c, strPropertyPath);
        
        foundValue = null;
        findFirstValue(fromObject, filter, 0);
        return foundValue;
    }

    
    public Object findFirst(F fromObject) {
        return findFirst(fromObject, new OANotEmptyFilter());
    }
    public Object findFirstNotEmpty(F fromObject) {
        return findFirst(fromObject, new OANotEmptyFilter());
    }
    public Object findFirstEmpty(F fromObject) {
        return findFirst(fromObject, new OAEmptyFilter());
    }
    public Object findFirstNotNull(F fromObject) {
        return findFirst(fromObject, new OANotNullFilter());
    }

    /**
     * Find first that is converts to True.
     */
    public Object findFirstTrue(F fromObject) {
        Object objx = findFirst(fromObject, new OAFilter() {
            OAConverterBoolean cb = new OAConverterBoolean(); 
            @Override
            public boolean isUsed(Object obj) {
                Boolean boo = (Boolean) cb.convert(Boolean.class, obj, null);
                return (boo != null && ((Boolean) boo).booleanValue());
            }
        });
        return objx;
    }
    
    protected boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos) {
        return findFirstValue(obj, filter, pos, false);
    }

    protected boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos, final boolean bRecursiveCheckOnly) {
        return findFirstValue(obj, filter, pos, bRecursiveCheckOnly, 0);
    }
    
    private boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos, final boolean bRecursiveCheckOnly, final int cntRecursive) {
        if (obj == null) return false;
        
        boolean b = true;
        if (pos == 0) {
            if (!bIncludeFromObject) {
                if (bRecursiveCheckOnly) return false;
                b = false;
            }
            else {
                OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
                OAPropertyInfo pi = oi.getPropertyInfo(property);
                if (pi == null) {
                    OALinkInfo li = oi.getLinkInfo(property);
                    if (li == null) b = false;
                    else {
                        if (li.getCalculated()) {
                            if (li.getCalcDependentProperties() != null) {
                                b = false;
                            }
                        }
                    }
                }
            }
        }
        if (b) {
            Object val = obj.getProperty(property);
            if (filter.isUsed(val)) {
                foundValue = val;
                return true;
            }
        }        

        // check recursive parent 
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
        OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(oi, OALinkInfo.ONE);
        
        if (liRecursive != null) {
            OALinkInfo[] lis  = propertyPath.getLinkInfos();
            if (lis != null && pos < lis.length) {
                OALinkInfo li = lis[pos];
                if (li != null) {
                    li = li.getReverseLinkInfo();
                    if (li != null && !li.getRecursive()) liRecursive = null;
                }
            }
        }        
        
        if (liRecursive != null) {
            if (cntRecursive > 50) return false;
            OAObject parent = (OAObject) liRecursive.getValue(obj);
            if (parent != null) {
                if (findFirstValue(parent, filter, pos, true, cntRecursive+1)) return true;
            }
        }
        
        if (bRecursiveCheckOnly) return false;
        
        String[] props = propertyPath.getProperties();
        if (props != null && pos < props.length) {
            OALinkInfo[] lis  = propertyPath.getLinkInfos();
            if (lis != null && pos < lis.length) {
                final OALinkInfo li = lis[pos];
                OAObject objx = (OAObject) li.getValue(obj);
                if (findFirstValue(objx, filter, pos+1)) return true;
            }
        }

        // go up using recursive parent 
        if (liRecursive != null) {
            OAObject parent = (OAObject) liRecursive.getValue(obj);
            if (parent != null) {
                if (findFirstValue(parent, filter, pos)) return true;
            }
        }
        
        return false;
    }
    
}
