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
package com.viaoa.util;

import java.lang.reflect.*;
import java.util.IdentityHashMap;

/**
 * Utility for recursively searching an arbitrary object graph for null values.
 * The search inspects all non-static, non-transient fields (including private
 * fields) and follows references using reflection. Arrays are traversed by
 * element index. <p>
 *
 * A property-path string is constructed as the traversal descends into fields
 * and array elements. When a null reference is encountered, the method
 * {@link #foundOne(String)} is invoked with the full property path. Circular
 * references are detected and skipped using an {@link IdentityHashMap}. <p>
 *
 * This class does not use OA metadata and treats all objects as plain Java
 * objects. Subclasses may override {@code foundOne} to record or report the
 * results. The return value of {@code foundOne} is currently not used to
 * terminate the search.
 */
public class OAFindNull {
    
	/**
	 * Tracks visited objects to detect and prevent circular reference traversal.
	 */
	private IdentityHashMap<Object, Object> hm = new IdentityHashMap<Object, Object>();
    
	/**
	 * Initiates a recursive search for null values starting from the given object.
	 *
	 * @param obj the root object to inspect
	 * @return true if a null value was found
	 * @throws IllegalAccessException if field access fails
	 */
    public boolean findNull(Object obj) throws IllegalAccessException {
        hm.clear();
        String s = obj == null ? "" : obj.getClass().getName();
        int x = s.lastIndexOf('.');
        if (x > 0) s = s.substring(x+1);
        return _findNull(s, obj);
    }    

    /**
     * Recursively inspects the given object and its references for null values.
     *
     * @param propertyPath the current property path being inspected
     * @param obj the current object reference
     * @return true if a null value was found
     * @throws IllegalAccessException if field access fails
     */
    private boolean _findNull(String propertyPath, Object obj) throws IllegalAccessException {
        if (obj == null) {
            foundOne(propertyPath);
            return true;
        }
        if (hm.get(obj) != null) return false;
        hm.put(obj, obj);

        if (obj instanceof String) {
            return false;
        }
        
        if (obj.getClass().isArray()) {
            int x = Array.getLength(obj);

            boolean bMatch = true;
            for (int i=0; i<x; i++) {
                Object o1 = Array.get(obj, i);
                boolean b = _findNull(propertyPath+"["+i+"]", o1);
            }
            return bMatch;
        }

        boolean b = _findNullFields(propertyPath, obj);

        return b;
    }

    
    /**
     * Inspects all eligible fields of the given object for null values.
     *
     * @param propertyPath the current property path being inspected
     * @param obj the object whose fields are inspected
     * @return true if a null value was found in any field
     * @throws IllegalAccessException if field access fails
     */
    private boolean _findNullFields(String propertyPath, Object obj) throws IllegalAccessException {
        Field[] objFields = obj.getClass().getDeclaredFields();
        AccessibleObject.setAccessible(objFields, true);
        boolean bResult = false;
        for (Field field : objFields) {
            if (field.getName().indexOf('$') >= 0) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            Object ox = field.get(obj);
            if (_findNull(propertyPath+"."+field.getName(), ox)) bResult = true;
        }
        return bResult;
    }
    
    /**
     * Callback invoked when a null value is found at the given property path.
     *
     * @param propertyPath the property path where a null value was encountered
     * @return true to continue searching, false to stop
     */
    public boolean foundOne(String propertyPath) {
        System.out.println(propertyPath);
        return true;
    }
    
}
