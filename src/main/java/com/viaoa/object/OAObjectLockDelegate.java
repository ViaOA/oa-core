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
    
	/**
	 * Shared in-JVM lock map that tracks active locks for OAObject instances.
	 * Each locked object is stored as a key mapped to an OALock instance,
	 * enabling local, non-distributed locking when no remote sync session
	 * is active.
	 */
    private static final Map<Object, Object> hmLock = new HashMap<>(11, 0.75F);
	
	
    /**
     * Attempts to acquire a lock for the specified {@link OAObject}.  
     * <p>
     * If the object’s class is associated with a remote sync session, the
     * lock request is delegated to the {@link RemoteSessionInterface} so
     * that distributed clients are notified. In this case, the method
     * returns immediately after the remote lock is set.
     * </p>
     * <p>
     * For local (non-sync) environments, a new {@code OALock} instance is
     * created and stored in the shared lock map. If another lock already
     * exists for the object, the calling thread waits until the lock is
     * released. The wait counter on the existing lock is incremented before
     * the thread enters the wait state.
     * </p>
     *
     * @param object the object to lock; must not be {@code null}
     * @throws IllegalArgumentException if {@code object} is {@code null}
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
	 * Releases the lock held for the specified {@link OAObject}.  
	 * <p>
	 * If the object’s class participates in a remote sync session, the
	 * unlock operation is delegated to the corresponding
	 * {@link RemoteSessionInterface} so that distributed clients are
	 * updated. In such cases, the method returns immediately.
	 * </p>
	 * <p>
	 * For local operation, the lock entry is removed from the shared lock
	 * map and all waiting threads are notified so that one of them may
	 * acquire the lock.
	 * </p>
	 *
	 * @param object the object whose lock is to be released; ignored if
	 *               {@code null}
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
	 * Determines whether the specified {@link OAObject} is currently locked.
	 * <p>
	 * If the object’s class is associated with a remote sync session, the
	 * query is delegated to the corresponding {@link RemoteSessionInterface}
	 * for distributed lock status. Otherwise, the method checks the local
	 * lock map. This method is non-blocking.
	 * </p>
	 *
	 * @param object the object to check; if {@code null} this method returns
	 *               {@code false}
	 * @return {@code true} if the object is locked either remotely or
	 *         locally; otherwise {@code false}
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
