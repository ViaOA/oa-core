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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Utility for performing a deep comparison of two arbitrary object graphs.
 * Supports recursive descent, identity-based cycle detection, field-by-field
 * reflection, and unordered array comparison. Mismatches are reported through
 * {@link #foundOne(String, Object, Object)}, which may be overridden to collect
 * or format results. <p>
 *
 * Primitive and {@code java.*} types are compared using {@link Object#equals}.
 * Arrays are treated as unordered collections: elements are matched using
 * {@link #getKey(Object)} to locate corresponding values in the opposite
 * array, and nested contents are compared recursively. For non-array,
 * non-primitive types, all non-static, non-transient declared fields of the
 * class are compared via reflection; superclass fields are not traversed. <p>
 *
 * A visitor map based on {@link IdentityHashMap} prevents infinite recursion
 * when cycles are encountered. The class maintains per-instance comparison
 * state and is not thread-safe. Callers should create a new instance for each
 * independent comparison.
 */
public class OAObjectCompare {

	/**
	 * Tracks pairs of objects that have already been compared to prevent infinite
	 * recursion when cycles are encountered.
	 */
    IdentityHashMap<Object, Object> hmVisitor = new IdentityHashMap<Object, Object>();
    
    /**
     * Tracks pairs of objects that have already been compared to prevent infinite
     * recursion when cycles are encountered.
     */
    private String leftName;
    
    /**
     * Display name used to identify the right-hand object in comparison output.
     */
    private String rightName;
    
    /**
     * Creates a new object comparison instance with default object names.
     */
    public OAObjectCompare() {
        setObjectNames(null, null);
    }

    /**
     * Compares two objects starting at the root level.
     *
     * @param objLeft the left-hand object to compare
     * @param objRight the right-hand object to compare
     * @return true if the objects are considered equal
     * @throws IllegalAccessException if reflective field access fails
     */
    public boolean compare(Object objLeft, Object objRight) throws IllegalAccessException {
        String s = objLeft == null ? "" : objLeft.getClass().getName();
        int x = s.lastIndexOf('.');
        if (x > 0) s = s.substring(x+1);
        return _compare(s, objLeft, objRight);
    }    
    
    /**
     * Internal entry point that performs a comparison using the given property path.
     *
     * @param propertyPath the current property path
     * @param objLeft the left-hand object
     * @param objRight the right-hand object
     * @return true if the objects are considered equal
     * @throws IllegalAccessException if reflective field access fails
     */
    private boolean _compare(String propertyPath, Object objLeft, Object objRight) throws IllegalAccessException {
        boolean bResult = true;
        bResult = _compare(propertyPath, objLeft, objRight, true);
        return bResult;
    }

    /**
     * Sets the display names used for the left-hand and right-hand objects.
     *
     * @param leftName display name for the left-hand object
     * @param rightName display name for the right-hand object
     */
    public void setObjectNames(String leftName, String rightName) {
        setLeftObjectName(leftName);
        setRightObjectName(rightName);
    }

    /**
     * Returns the display name for the left-hand object.
     *
     * @return the left-hand object name
     */
    public String getLeftObjectName() {
        return leftName;
    }
    
    /**
     * Sets the display name for the left-hand object.
     *
     * @param name the display name to use
     */
    public void setLeftObjectName(String name) {
        if (name == null) name = "leftObj";
        leftName = name;
    }
    
    /**
     * Returns the display name for the right-hand object.
     *
     * @return the right-hand object name
     */
    public String getRightObjectName() {
        return rightName;
    }
    
    /**
     * Sets the display name for the right-hand object.
     *
     * @param name the display name to use
     */
    public void setRightObjectName(String name) {
        if (name == null) name = "rightObj";
        rightName = name;
    }
    
    
    /**
     * Performs a recursive comparison of two objects with optional mismatch reporting.
     *
     * @param propertyPath the current property path
     * @param objLeft the left-hand object
     * @param objRight the right-hand object
     * @param bReportNotEquals true to report mismatches as they are found
     * @return true if the objects are considered equal
     * @throws IllegalAccessException if reflective field access fails
     */
    protected boolean _compare(String propertyPath, Object objLeft, Object objRight, boolean bReportNotEquals) throws IllegalAccessException {
        if (objLeft == objRight) return true;
        if (objLeft == null || objRight == null) {
            if (bReportNotEquals) foundOne(propertyPath, objLeft, objRight);
            return false;
        }

        if (!objLeft.getClass().equals(objRight.getClass())) {
            if (bReportNotEquals) foundOne(propertyPath, objLeft, objRight);
            return false;
        }
        
        String s = objLeft.getClass().getName();
        if (s.indexOf("java.") == 0) {
            boolean b = objLeft.equals(objRight);
            if (!b && bReportNotEquals) {
                foundOne(propertyPath, objLeft, objRight);
            }
            return b;
        }
        
        if (objLeft.getClass().isArray()) {
            int x = Array.getLength(objLeft);
            boolean bMatch = true;
            if (Array.getLength(objRight) != x) {
                bMatch = false;
                if (bReportNotEquals) {
                    foundOne(propertyPath, "length="+Array.getLength(objLeft), "length="+Array.getLength(objRight));
                }
            }

            HashMap<Object, Object> hm = new HashMap<Object, Object>((int)(x * 1.5), .85f);
            for (int i=0; i<x; i++) {
                Object obj = Array.get(objLeft, i);
                Object key = getKey(obj);
                Object objx = hm.put(key, obj);
                if (objx != null) {
                    foundOne(propertyPath, "duplicate key in collection", key);
                }
            }
            if (hm.size() != x) {
                foundOne(propertyPath, "duplicate keys in collection", "");
            }
            x = Array.getLength(objRight);
            for (int i=0; i<x; i++) {
                Object objR = Array.get(objRight, i);
                Object key = getKey(objR);
                
                Object objL = hm.remove(key);
                if (objL == null) {
                    bMatch = false;
                    if (bReportNotEquals) {
                        foundOne(propertyPath+"["+i+"]", "not found", key);
                    }
                }
                else {
                    boolean b = _compare(propertyPath+"["+i+"]", objL, objR, bReportNotEquals);
                    if (!b) bMatch = false;
                }
            }
            for (Map.Entry<Object, Object> ex : hm.entrySet()) {
                Object key = ex.getKey();
                bMatch = false;
                if (bReportNotEquals) {
                    int pos = OAArray.indexOf((Object[]) objLeft, ex.getValue());
                    foundOne(propertyPath+"["+pos+"]", key, "not found");
                }
            }
            return bMatch;
        }

        boolean b = false;
        if (!bReportNotEquals) b = objLeft.equals(objRight);
        
        if (!b && bReportNotEquals) {
            b = _compareFields(propertyPath, objLeft, objRight, bReportNotEquals);
        }
        return b;
    }

    /**
     * Returns the key used to match elements when comparing unordered arrays.
     *
     * @param obj the array element
     * @return the key used for element matching
     */
    protected Object getKey(Object obj) {
        return obj;
    }
    
    /**
     * Compares all non-static, non-transient fields of two objects using reflection.
     *
     * @param propertyPath the current property path
     * @param objLeft the left-hand object
     * @param objRight the right-hand object
     * @param bReportNotEquals true to report mismatches as they are found
     * @return true if all fields are considered equal
     * @throws IllegalAccessException if reflective field access fails
     */
    private boolean _compareFields(String propertyPath, Object objLeft, Object objRight, boolean bReportNotEquals) throws IllegalAccessException {
        // check to see if these objects have already been compared
        Object objx = hmVisitor.get(objLeft);
        if (objx == objRight) {
            objx = hmVisitor.get(objRight);
            if (objx == objLeft) {
                return true; // already compared
            }
        }
        hmVisitor.put(objLeft, objRight);
        hmVisitor.put(objRight, objLeft);

        Field[] objFields = objLeft.getClass().getDeclaredFields();
        AccessibleObject.setAccessible(objFields, true);
        boolean bResult = true;
        for (Field field : objFields) {
            if (field.getName().indexOf('$') >= 0) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            Object oL = field.get(objLeft);
            Object oR = field.get(objRight);
            
            if (!_compare(propertyPath+"."+field.getName(), oL, oR, bReportNotEquals) ) {
                bResult = false;
            }
        }
        return bResult;
    }
    
    /**
     * Called when a mismatch is detected between two objects.
     *
     * @param propertyPath the property path where the mismatch occurred
     * @param objLeft the left-hand value
     * @param objRight the right-hand value
     */
    public void foundOne(String propertyPath, Object objLeft, Object objRight) {
        String s1 = objLeft+"";
        if (s1.length() > 40) s1 = s1.substring(0,40)+"...";
        String s2 = objRight+"";
        if (s2.length() > 40) s2 = s2.substring(0,40)+"...";
        System.out.println(propertyPath+": "+leftName+"="+s1+", "+rightName+"="+s2);
    }
    
    /**
     * Entry point used for simple testing of object comparison behavior.
     *
     * @param args command-line arguments
     * @throws Exception if an error occurs during execution
     */
    public static void main(String[] args) throws Exception {
        Object obj1 = "test";
        Object obj2 = null;
        
        OAObjectCompare oc = new OAObjectCompare() {
            @Override
            public void foundOne(String propertyPath, Object objLeft, Object objRight) {
                super.foundOne(propertyPath, objLeft, objRight);
            }
        };
        oc.compare(obj1, obj2);
    }
}
