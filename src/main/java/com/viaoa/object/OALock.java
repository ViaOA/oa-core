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

import java.lang.ref.*;  // java1.2

/**
 * Represents a distributed or local logical lock flag on an arbitrary object.
 *
 * <p>OALock does not enforce synchronization—it serves as a shared metadata
 * record indicating that an object is considered "locked" within the OA
 * runtime or synchronization context.  Enforcement of locking semantics
 * remains the responsibility of higher-level code.</p>
 *
 * <p><b>Design Details</b>:
 * <ul>
 *   <li>Associates a primary {@code object} with an optional
 *       {@link WeakReference} to a {@code refObject}; the lock expires
 *       automatically when the reference is GC’d.</li>
 *   <li>Includes an optional {@code miscObject} for custom metadata
 *       (e.g., user, timestamp, reason).</li>
 *   <li>Serializable for propagation through distributed OA-Sync channels.</li>
 * </ul>
 */
public class OALock implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    
    /**
     * The primary object associated with this logical lock; must not be null.
     */
    protected Object object;
    
    protected transient Thread thread;
    
    /**
     * Optional weak reference used to control the lifetime of this lock.
     * The lock is considered expired once the referenced object is garbage collected.
     */
    protected transient WeakReference ref;
    
    /**
     * Optional metadata object associated with this lock, such as a user,
     * timestamp, lock reason, or additional context.
     */
    protected Object miscObject;
    
    /**
     * Counter used to track how many wait operations have occurred on this lock.
     */
    protected int waitCnt;

    /**
     * Creates a new logical lock for the specified object.
     *
     * <p>If a {@code refObject} is supplied, a {@link WeakReference} is created
     * so that the lock will automatically expire once the reference object
     * becomes eligible for garbage collection.</p>
     *
     * @param object     the object being locked; must not be {@code null}
     * @param refObject  the reference object used to control lock lifetime,
     *                   or {@code null} if not used
     * @param miscObject an optional metadata object stored with the lock
     * @throws IllegalArgumentException if {@code object} is {@code null}
     */
    public OALock(Object object, Object refObject, Object miscObject) {
        if (object == null) throw new IllegalArgumentException("object can not be null");
        this.object = object;
        if (refObject != null) ref = new WeakReference(refObject);
        this.miscObject = miscObject;
        this.thread = Thread.currentThread();
    }
    
    /**
     * Returns the object associated with this lock.
     *
     * @return the locked object
     */
    public Object getObject() {
        return object;
    }
    
    /**
     * Returns the reference object associated with this lock.
     *
     * <p>The reference is stored as a {@link WeakReference}; if it has been
     * garbage collected, this method returns {@code null}.</p>
     *
     * @return the reference object, or {@code null} if none exists or it has expired
     */
    public Object getReferenceObject() {
        if (ref == null) return null;
        return ref.get();
    }
    
    /**
     * Returns the miscellaneous metadata object associated with this lock.
     *
     * @return the metadata object, or {@code null} if none was provided
     */
    public Object getMiscObject() {
        return miscObject;
    }

    //qqqqqqqqq created, move to FA??
    public int getWaitCount() {
    	return this.waitCnt;
    }
    
    public void setWaitCount(int x) {
    	this.waitCnt = x;
    }
    
    public Thread getThread() {
    	return this.thread;
    }
}





