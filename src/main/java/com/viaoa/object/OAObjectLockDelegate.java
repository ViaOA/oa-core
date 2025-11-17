/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.util.*;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteSessionInterface;

/**
 * Provides distributed locking services for {@link OAObject} instances,
 * ensuring that concurrent clients or threads can coordinate access to
 * shared entities.
 *
 * <p>Locks are advisory: they act as flags rather than hard enforcement,
 * allowing applications to define business-level rules for locked objects.</p>
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>Local in-JVM lock map for standalone use.</li>
 *   <li>Delegation to {@link com.viaoa.sync.OASync} for remote,
 *       multi-client lock propagation.</li>
 *   <li>Non-blocking {@link #isLocked(OAObject)} checks.</li>
 *   <li>Automatic wake-up of waiting threads when a lock releases.</li>
 * </ul>
 *
 * <p>Used primarily in OA-Sync deployments to coordinate record-level edits
 * between users or clustered servers.</p>
 */
public class OAObjectLockDelegate {
    
    private static final Map<Object, Object> hmLock = new HashMap<>(11, 0.75F);
	
	
    /** 
	    Used to set a lock on an Object.
	    see #lock(Object,Object,Object) lock
	*/
	public static void lock(OAObject object) {
	    if (object == null) throw new IllegalArgumentException("object can not be null");
	
	    RemoteSessionInterface rc = OASync.getRemoteSession(object.getClass());
	    if (rc != null) {
	        rc.setLock(object.getClass(), object.getObjectKey(), true);
	    	return;
	    }
	            
	    OALock newLock = new OALock(object, null, null);
	    synchronized (OAObjectLockDelegate.hmLock) {
	        for (;;) {
	            OALock lock = (OALock) OAObjectLockDelegate.hmLock.get(object);
	            if (lock == null) break;
	            try {
	                lock.waitCnt++;
	                OAObjectLockDelegate.hmLock.wait();
	            }
	            catch (InterruptedException e) {
	            }
	        }
	        OAObjectLockDelegate.hmLock.put(object, newLock);
	    }
	}
	
	/** 
	    Removes lock from table.
	    @param object to release
	*/
	public static void unlock(OAObject object) {
	    if (object == null) return;

        RemoteSessionInterface rc = OASync.getRemoteSession(object.getClass());
        if (rc != null) {
            rc.setLock(object.getClass(), object.getObjectKey(), false);
            return;
        }
	    
	    synchronized (OAObjectLockDelegate.hmLock) {
	    	OAObjectLockDelegate.hmLock.remove(object);
	    	OAObjectLockDelegate.hmLock.notifyAll();
	    }
	}
	
	/** 
	    Used to check to see if an object is locked. This is nonblocking. 
	*/
	public static boolean isLocked(OAObject object) {
	    if (object == null) return false;

        RemoteSessionInterface rc = OASyncDelegate.getRemoteSession(object.getClass());
        if (rc != null) {
            return rc.isLocked(object.getClass(), object.getObjectKey());
        }
        synchronized (OAObjectLockDelegate.hmLock) {
            return (OAObjectLockDelegate.hmLock.get(object) != null);
        }
        
	}
	
    
}


