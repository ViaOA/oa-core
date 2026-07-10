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
package com.viaoa.compare;

import java.util.*;

import com.viaoa.reflect.OAReflect;

import java.lang.reflect.*;

/*qqqqqqqqqqqqqqqqqqqqqq
CODEX


 - Method: compare(Object, Object)
  - Issue: No-property fallback directly calls ((Comparable)o1).compareTo(o2) without guarding incompatible
    Comparable types.
  - Why it is a problem: Valid mixed inputs can throw ClassCastException and fail sorting instead of returning a
    deterministic ordering.
  - Classification: CODEX/FIXNOW

 - Method: compare(Object, Object, Method[], boolean)
  - Issue: If neither Comparable can compare to the other, the fallback returns -1.
  - Why it is a problem: compare(a,b) and compare(b,a) can both return -1, violating the Comparator antisymmetry
    contract and potentially breaking sort behavior.
  - Classification: CODEX/FIXNOW

 - Method: compare(Object, Object, Method[], boolean)
  - Issue: Non-Comparable non-null property values compare as 0.
  - Why it is a problem: Distinct property values silently sort as equal. The Boolean branch inside this block is
    unreachable because Boolean already implements Comparable.
  - Classification: CODEX/CONTRACT

 - Method: compare(Object, Object, Method[], boolean)
  - Issue: Case-insensitive string sorting uses default-locale toUpperCase().
  - Why it is a problem: Sort order can differ by JVM default locale, which is risky for deterministic Hub sorting
    across distributed systems.
  - Classification: CODEX/DEFER


 #2 — OAComparator.init() / compare(...)

  File/class/method: src/main/java/com/viaoa/compare/OAComparator.java, init and compare(Object,Object)

  Concrete bug: lazy initialization publishes methodss before bAscendings is initialized.

  Runtime scenario: two threads share the same OAComparator during first use. Thread A assigns methodss; before it
  assigns bAscendings, Thread B enters compare, sees methodss != null, skips init, and compares all properties using
  the default bAscending. A path such as "lastName DESC, firstName ASC" can temporarily sort with the wrong direction.

  Why this violates OA/OG comparison semantics: Hub sorting must be deterministic. A first-use race can produce wrong
  ordering without an exception.

  Minimal fix direction: build local Method[][] and boolean[] fully, then publish them together under synchronization
  or with a volatile initialized-state. Alternatively make eager initialization happen in the constructor.


1. file/class/method
     src/main/java/com/viaoa/compare/OAComparator.java — init()
  2. concrete bug
     A space between ASC/DESC and the comma can shift per-property sort directions onto the wrong property.
  3. runtime scenario
     For an order string like:

  "lastName DESC , firstName DESC"

  tokenization sees the space after DESC, sets bAllowDesc = true, then the comma adds the default bAscending direction
  as an extra entry. The internal direction list becomes effectively:

  lastName -> DESC
  firstName -> default ASC

  The second explicit DESC is pushed out of alignment and ignored for firstName.

  4. why this violates OA/OG comparison semantics
     OA sort/order strings are user-facing through Hub sort/select and object-cache select paths. Whitespace around
     commas is normal order-clause formatting. A harmless formatting difference silently changes sort direction,
     causing wrong Hub/select ordering without an exception.
  5. minimal fix direction
     When ASC or DESC is consumed, keep a state that prevents a following whitespace token before , from re-enabling
     “missing direction” handling for the just-completed property. More robustly, parse each comma-separated segment
     independently, trim it, then detect trailing ASC/DESC per segment.
  6. suggested CODEX comment location
     In OAComparator.init(), near the StringTokenizer(paths, ", ", true) loop and the prop.equals(" ") branch.




*/

/**
 * Reflection-based Comparator supporting sorting by one or more
 * property paths. Each segment of the path may refer to nested
 * properties, including link traversal through OAObjects.
 *
 * <p>Sorting is driven by {@link OAReflect#getPropertyValue(Object, Method[])}
 * and therefore supports Hub active-object resolution.
 *
 * <p>Multiple property paths are comma-separated and may specify ASC or DESC
 * ordering per field.
 *
 * <pre>
 *     new OAComparator(Employee.class, "lastName, firstName", true);
 *     new OAComparator(Order.class, "customer.name DESC, created", true);
 * </pre>
 *
 * @see com.viaoa.hub.Hub#sort(String, boolean)
 */
public class OAComparator implements Comparator {
    
	/**
	 * The class whose properties are used for comparison.
	 */
	Class clazz;
    
	/**
	 * Comma-separated list of property paths used to extract values for comparison.
	 */
	String paths;
    
	/**
	 * Default sort direction used when no explicit direction is specified
	 * in the property paths.
	 */
	boolean bAscending;
    
	/**
	 * Cached arrays of {@link Method} chains corresponding to each property path.
	 */
	Method[][] methodss;
    
	/**
	 * Per-property sort direction flags corresponding to each method chain.
	 */
	boolean[] bAscendings; 

	/**
	 * Creates a new comparator using the specified class and property paths.
	 *
	 * @param clazz the class whose properties will be used for comparison
	 * @param paths comma-separated property paths, optionally including
	 *        ASC or DESC keywords
	 * @param bAscending default sort direction
	 */
    public OAComparator(Class clazz, String paths, boolean bAscending) {
        this.clazz = clazz;
        this.paths = paths;
        this.bAscending = bAscending;
    }

    /**
     * Returns the configured property paths string.
     *
     * @return the property paths
     */
    public String getPaths() {
        return paths;
    }

    /**
     * Returns the default ascending flag.
     *
     * @return {@code true} if ascending, {@code false} if descending
     */
    public boolean getAsc() {
        return bAscending;
    }
    
    /**
     * Compares two objects using the configured property paths.
     *
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return a negative value, zero, or a positive value as required by {@link Comparator}
     */
    public int compare(Object o1, Object o2) {
        int x = preCheck(o1, o2);
        if (x < 5) return x;

        if (methodss == null) {
            init();
        }

        if (methodss == null || methodss.length == 0) {
            x = 0;
            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                x = ((Comparable)o1).compareTo(((Comparable)o2));
            }
            if (!bAscending) {
                if (x < 0) return 1;
                if (x > 0) return -1;
            }
            return x;
        }
        
        for (int i=0; i<methodss.length; i++) {
        	boolean bAscend = bAscending;
        	if (bAscendings != null && i<bAscendings.length) bAscend = bAscendings[i];
            x = compare(o1, o2, methodss[i], bAscend);
            if (x != 0) return x;
        }
        
        return 0;
    }

    /**
     * Compares two objects using a specific chain of accessor methods.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @param methods method chain used to extract comparable values
     * @param bAscend {@code true} for ascending order, {@code false} for descending
     * @return comparison result
     */
    private int compare(Object o1, Object o2, Method[] methods, boolean bAscend) {
        if (methods != null && methods.length != 0) {
            o1 = OAReflect.getPropertyValue(o1, methods);
            o2 = OAReflect.getPropertyValue(o2, methods);
        }

        int x = 0;
        if (o1 == null || o2 == null) {
            if (o1 == o2) x = 0;
            else if (o1 == null) x = -1;
            else x = 1;
        }
        else {
            boolean bComparable = true;
            if (!(o1 instanceof Comparable)) bComparable = false;
            else if (!(o2 instanceof Comparable)) bComparable = false;
            
            if (!bComparable) {
                x = 0;
                if (o1 instanceof Boolean && o2 instanceof Boolean) {
                    boolean b1 = ((Boolean) o1).booleanValue();
                    boolean b2 = ((Boolean) o2).booleanValue();
                    if (b1 == b2) x = 0;
                    else if (b1) x = 1;
                    else x = -1;
                    if (!bAscend && x != 0) x = -x;
                }
                return x;
            }

            // Strings will use a case insensitive search
            if (o1 instanceof String) o1 = ((String) o1).toUpperCase();
            if (o2 instanceof String) o2 = ((String) o2).toUpperCase();

            Comparable c1 = (Comparable) o1;
            Comparable c2 = (Comparable) o2;

            try {
                x = c1.compareTo(c2);
            }
            catch (Exception e) {
                try {
                    x = -c2.compareTo(c1);
                }
                catch (Exception ex) {
                    x = -1;
                }
            }
        }
        if (bAscend || x == 0) return x;
        return -x;
    }

    /**
     * Performs a preliminary comparison check for null values.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return comparison result, or a sentinel value indicating further comparison
     */
    protected int preCheck(Object o1, Object o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) {
            if (bAscending) return -1;
            return 1;
        }
        if (o2 == null) {
            if (bAscending) return 1;
            return -1;
        }
        return 5;
    }

    /**
     * Initializes internal method chains and sort direction flags based on
     * the configured property paths.
     */
    protected void init() {
        if (clazz == null) return;
        if (paths == null || paths.length() == 0) {
            // sort on object itself
            methodss = new Method[0][];
            return;
        }

        ArrayList al = new ArrayList(7);
        ArrayList alAsc = new ArrayList(7);
        StringTokenizer st = new StringTokenizer(paths, ", ", true);
        Method[] ms = null;
        boolean bAllowDesc = paths.equalsIgnoreCase("desc");
        for ( ; st.hasMoreElements() ; ) {
            String prop = (String) st.nextElement();

            if (prop.equals(" ")) {
                bAllowDesc = true;
                continue;
            }
            if (prop.equals(",")) {
                if (bAllowDesc) alAsc.add(new Boolean(bAscending));
                bAllowDesc = false;
                continue;
            }
            if (prop.equalsIgnoreCase("desc") && bAllowDesc) {
                bAllowDesc = false;
                alAsc.add(Boolean.valueOf(false));
                continue;
            }
            if (prop.equalsIgnoreCase("asc") && bAllowDesc) {
                bAllowDesc = false;
                alAsc.add(Boolean.valueOf(true));
                continue;
            }
            
            try {
                ms = OAReflect.getMethods(clazz, prop);
                bAllowDesc = true;
            }
            catch (Exception e) {
                if (prop.equalsIgnoreCase("by")) continue;
            	throw new RuntimeException(e);
            }
            al.add(ms);
        }
        if (bAllowDesc) alAsc.add(Boolean.valueOf(bAscending));
        methodss = new Method[al.size()][];

        al.toArray(methodss);
        
        // 2006/10/25
        int x = alAsc.size();
        bAscendings = new boolean[x];
        for (int i=0; i<x; i++) {
        	Boolean b = (Boolean) alAsc.get(i);
        	bAscendings[i] = b.booleanValue();
        }
        if (x == 1) bAscending = bAscendings[0];
    }

}

