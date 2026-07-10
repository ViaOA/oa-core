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

import java.lang.reflect.Method;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Delegate class containing helper methods used internally by OA filter
 * implementations.  These utilities support finder creation, OAPath
 * evaluation, and reusable comparison logic shared across multiple filter
 * subclasses.
 *
 * <p>
 * Although not typically used directly by application-level code,
 * {@code OAFilterDelegate} centralizes the common mechanisms required to
 * evaluate nested OAPaths, handle many-relationships, and apply
 * finder-based filtering to deeply referenced objects.
 * </p>
 */
public class OAFilterDelegate {

	/**
	 * Container object returned by {@link OAFilterDelegate#createFinder(Class, OAPath)}.
	 * Holds a generated {@link OAFinder} and the remaining portion of the
	 * OAPath that should be applied by the filter after the finder
	 * performs its lookup.
	 */
    static public class FinderInfo {
        /**
         * The finder created to traverse Hub-based links in the OAPath
         * and locate the target object used for further filtering.
         */
        public OAFinder finder;

        /**
         * The remaining OAPath segment that the calling filter should
         * evaluate after the finder has located the appropriate object.
         */
        public String pp;  // remaining path to use by the filter
        
        /**
         * Constructs a FinderInfo wrapper containing a finder and the remaining
         * OAPath segment.
         *
         * @param f the finder used for Hub-traversal lookup
         * @param pp the remaining OAPath to be applied by the filter
         */
        public FinderInfo(OAFinder f, String pp) {
            this.finder = f;
            this.pp = pp;
        }
    }
    
    /**
     * Creates a finder for OAPaths that traverse Hub links. If the
     * OAPath includes at least one Hub-returning method, a corresponding
     * {@link OAFinder} is created and any remaining OAPath segment is
     * returned for continued filtering.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Validates the input class and OAPath,</li>
     *   <li>Ensures the path is initialized for the given class,</li>
     *   <li>Inspects the resolved getter methods for Hub-returning links,</li>
     *   <li>Creates a finder for the Hub-based portion of the path,</li>
     *   <li>Returns the remaining OAPath segment, if any.</li>
     * </ul>
     *
     * @param clazz the class defining the starting point of the OAPath
     * @param pp the OAPath to inspect for Hub traversal
     * @return a {@link FinderInfo} containing the finder and remaining path,
     *         or {@code null} if no Hub traversal is needed
     */
    public static FinderInfo createFinder(Class clazz, OAPath pp) {
        if (clazz == null || pp == null) return null;
        
        String s = pp.getPath();
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
            OAFinder f = new OAFinder(pp.getPath());
            return new FinderInfo(f, null);
        }

        int dcnt = OAString.dcount(pp.getPath(), '.');
        s = OAString.field(pp.getPath(), '.', 1, dcnt-1);
        OAFinder f = new OAFinder(s);
        s = OAString.field(pp.getPath(), '.', dcnt);
        
        return new FinderInfo(f, s);
    }

}
