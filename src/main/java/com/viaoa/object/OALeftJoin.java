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

import com.viaoa.annotation.OAClass;

/**
 * Lightweight join object that pairs two {@link OAObject}s representing the
 * left and right sides of a logical "left join" relationship.
 *
 * <p>Used internally by {@code HubCombined} and {@code HubLeftJoin} to
 * produce composite views of data, analogous to a SQL left join.
 *
 * <p><b>Characteristics</b>:
 * <ul>
 *   <li>Maintains references to two typed OAObjects: {@code A} (left) and {@code B} (right).</li>
 *   <li>Supports property-change events for synchronization with bound Hubs.</li>
 *   <li>Transient runtime object; excluded from persistence.</li>
 * </ul>
 *
 * @param <A> left-side OAObject type
 * @param <B> right-side OAObject type
 */
@OAClass(addToCache=false, initialize=false, useDataSource=false, localOnly=true)
public class OALeftJoin<A extends OAObject, B extends OAObject> extends OAObject {
    static final long serialVersionUID = 1L;
    
    public static final String P_A = "A"; 
    public static final String P_B = "B"; 
    public static final String PROPERTY_A = "A"; 
    public static final String PROPERTY_B = "B"; 
    private A a;
    private B b;
    
    /**
     * Creates an empty left-join pair with no left or right objects assigned.
     */
    public OALeftJoin() {
    }
    
    /**
     * Creates a left-join pair initialized with the specified left and
     * right objects.
     *
     * @param a the left-side object
     * @param b the right-side object
     */
    public OALeftJoin(A a, B b) {
        setA(a);
        setB(b);
    }
    
    /**
     * Returns the left-side object associated with this join pair.
     *
     * @return the left-side object, or {@code null} if none assigned
     */
    public A getA() {
        return a;
    }
    
    /**
     * Sets the left-side object for this join pair.
     * <p>
     * This method fires before- and after-property-change events for
     * property {@code "A"}.
     * </p>
     *
     * @param obj the new left-side object
     */
    public void setA(A obj) {
        OAObject hold = this.a;
        fireBeforePropertyChange("A", hold, obj);
        this.a = obj;
        firePropertyChange("A", hold, obj);
    }

    /**
     * Returns the right-side object associated with this join pair.
     *
     * @return the right-side object, or {@code null} if none assigned
     */
    public B getB() {
        return b;
    }

    /**
     * Sets the right-side object for this join pair.
     * <p>
     * This method fires before- and after-property-change events for
     * property {@code "B"}.
     * </p>
     *
     * @param obj the new right-side object
     */
    public void setB(B obj) {
        OAObject hold = this.b;
        fireBeforePropertyChange("B", hold, obj);
        this.b = obj;
        firePropertyChange("B", hold, obj);
    }
}
