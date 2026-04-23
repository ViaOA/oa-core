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
package com.viaoa.object;


import com.viaoa.converter.OAConverterBoolean;
import com.viaoa.filter.OAEmptyFilter;
import com.viaoa.filter.OANotEmptyFilter;
import com.viaoa.filter.OANotNullFilter;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.*;

/**
 * Utility class that traverses an OAObject hierarchy and returns the first
 * property value satisfying a supplied filter.
 *
 * <p>OAHierFinder supports recursive and linked hierarchies (for example, a
 * Location with parent Locations) and can evaluate nested property paths such as:
 * <pre>
 *   Employee.department.location.region.country
 * </pre>
 * The search begins with a starting object and proceeds along the property path,
 * optionally following recursive parent links, until a value passes the given
 * {@link com.viaoa.filter.OAFilter}.
 *
 * <p><b>Capabilities</b>:
 * <ul>
 *   <li>Evaluates {@code OAPropertyPath} dynamically using metadata from
 *       {@link OAObjectInfoDelegate}.</li>
 *   <li>Supports predefined filters like {@code OANotEmptyFilter},
 *       {@code OANotNullFilter}, and {@code OAEmptyFilter}.</li>
 *   <li>Includes helper methods such as {@code findFirstTrue()} that
 *       interpret boolean-convertible values.</li>
 * </ul>
 *
 * @param <F> starting OAObject type for the traversal
 */
public class OAHierFinder<F extends OAObject> {
    
	/**
	 * Name of the property whose value should be evaluated at each step
	 * of the hierarchy traversal.
	 */
	private final String property;
    
	/**
	 * The raw string form of the property path used to navigate through
	 * recursive or linked object hierarchies.
	 */
	private final String strPropertyPath;
    
	/**
	 * Parsed representation of the property path, produced on first use,
	 * and used to walk the hierarchy during evaluation.
	 */
	private OAPropertyPath propertyPath;
    
	/**
	 * The first property value encountered during traversal that satisfies
	 * the supplied filter; becomes the return value of the search.
	 */
	private Object foundValue;
    
	/**
	 * Indicates whether the starting object should be evaluated before
	 * traversing child or parent links.
	 */
	private boolean bIncludeFromObject;

    /**
     * Creates a new hierarchy finder using the specified property name and
     * property path. The starting object will be included in evaluation.
     *
     * @param propertyName the property whose value is evaluated
     * @param propertyPath the hierarchical path used for traversal
     */
    public OAHierFinder(String propertyName, String propertyPath) {
        this(propertyName, propertyPath, true);
    }
    
    /**
     * Creates a new hierarchy finder with control over whether the starting
     * object should be evaluated.
     *
     * @param propertyName the property whose value is evaluated
     * @param propertyPath the hierarchical path used for traversal
     * @param bIncludeFromObject {@code true} to include the starting object
     *                           in evaluation, otherwise {@code false}
     */
    public OAHierFinder(String propertyName, String propertyPath, boolean bIncludeFromObject) {
        this.property = propertyName;
        this.strPropertyPath = propertyPath;
        this.bIncludeFromObject = bIncludeFromObject;
    }
    
    /**
     * Begins a hierarchical search starting from the supplied object and
     * returns the first property value that satisfies the given filter.
     *
     * @param fromObject the starting object
     * @param filter the filter used to test values
     * @return the first matching value found, or {@code null} if none
     */
    public Object findFirst(F fromObject, OAFilter filter) {
        if (fromObject == null) return null;

        Class c = fromObject.getClass();
        if (propertyPath == null) propertyPath = new OAPropertyPath(c, strPropertyPath);
        
        foundValue = null;
        findFirstValue(fromObject, filter, 0);
        return foundValue;
    }

    
    /**
     * Convenience method that searches for the first non-empty property
     * value using an {@link OANotEmptyFilter}.
     *
     * @param fromObject the starting object
     * @return the first non-empty value, or {@code null} if none
     */
    public Object findFirst(F fromObject) {
        return findFirst(fromObject, new OANotEmptyFilter());
    }
    
    /**
     * Convenience method that searches for the first non-empty property
     * value using an {@link OANotEmptyFilter}.
     *
     * @param fromObject the starting object
     * @return the first non-empty value, or {@code null} if none
     */
    public Object findFirstNotEmpty(F fromObject) {
        return findFirst(fromObject, new OANotEmptyFilter());
    }
    
    /**
     * Searches for the first empty property value using an
     * {@link OAEmptyFilter}.
     *
     * @param fromObject the starting object
     * @return the first empty value, or {@code null} if none
     */
    public Object findFirstEmpty(F fromObject) {
        return findFirst(fromObject, new OAEmptyFilter());
    }
    
    /**
     * Searches for the first non-null property value using an
     * {@link OANotNullFilter}.
     *
     * @param fromObject the starting object
     * @return the first non-null value, or {@code null} if none
     */
    public Object findFirstNotNull(F fromObject) {
        return findFirst(fromObject, new OANotNullFilter());
    }

    /**
     * Searches for the first property value that converts to a boolean
     * {@code true} using {@link OAConverterBoolean}.
     *
     * @param fromObject the starting object
     * @return the first truthy value, or {@code null} if none
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
    
    /**
     * Evaluates the value at the current hierarchy position. This delegates
     * to the extended form of this method with recursive-check disabled.
     *
     * @param obj the current object
     * @param filter the filter used to test values
     * @param pos the current property-path index
     * @return {@code true} if a matching value was found
     */
    protected boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos) {
        return findFirstValue(obj, filter, pos, false);
    }

    /**
     * Evaluates the value at the current hierarchy position with optional
     * recursive-check-only behavior. Delegates to the full internal variant.
     *
     * @param obj the current object
     * @param filter the filter used to test values
     * @param pos the current property-path index
     * @param bRecursiveCheckOnly whether only recursive-parent evaluation
     *                            should be performed
     * @return {@code true} if a matching value was found
     */
    protected boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos, final boolean bRecursiveCheckOnly) {
        return findFirstValue(obj, filter, pos, bRecursiveCheckOnly, 0);
    }
    
    /**
     * Internal recursive evaluator that traverses the object hierarchy
     * following both the specified property path and recursive parent links.
     * <p>
     * The method checks:
     * <ol>
     *   <li>Whether the starting object should be evaluated.</li>
     *   <li>The value of the configured property on the current object.</li>
     *   <li>Recursive parent links as defined by {@link OALinkInfo}.</li>
     *   <li>Child links defined by the property path.</li>
     *   <li>Additional upward recursion when allowed.</li>
     * </ol>
     * When a value satisfies the supplied filter, it is stored and the
     * search stops.
     *
     * @param obj the current object being evaluated
     * @param filter the filter used to determine match criteria
     * @param pos the current property-path index
     * @param bRecursiveCheckOnly whether only recursive-parent paths
     *                            should be checked at this stage
     * @param cntRecursive the current recursive-depth counter
     * @return {@code true} if a matching value has been found
     */
    private boolean findFirstValue(final OAObject obj, OAFilter filter, final int pos, final boolean bRecursiveCheckOnly, final int cntRecursive) {
        if (obj == null) return false;
        
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
        boolean b = true;
        if (pos == 0) {
            if (!bIncludeFromObject) {
                if (bRecursiveCheckOnly) return false;
                b = false;
            }
            else {
                OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(obj.getClass());
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
        OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(obj.getClass());
        OALinkInfo liRecursive = og.objectsInternal().callObjectInfoGetRecursiveLinkInfo(oi, OALinkInfo.ONE);
        
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
            if (cntRecursive > 100) return false;
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
