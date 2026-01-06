package com.viaoa.graph.object;

import java.util.*;
import java.util.logging.Logger;

import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.OASyncService;
import com.viaoa.object.OALock;
import com.viaoa.object.OAObject;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteSessionInterface;

public class OAObjectLockService {
	private static final Logger LOG = Logger.getLogger(OAObjectLockService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final OASyncService srvcSync;

	public OAObjectLockService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, OASyncService srvcSync) {
		if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
		this.srvcObject = srvcObject;
		if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
		if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
		this.srvcSync = srvcSync;
	}

	public OAObjectService getObjectService() {
		return srvcObject;
	}

	/**
	 * Shared in-JVM lock map that tracks active locks for OAObject instances.
	 * Each locked object is stored as a key mapped to an OALock instance,
	 * enabling local, non-distributed locking when no remote sync session
	 * is active.
	 */
    private final Map<Object, Object> hmLock = new HashMap<>(11, 0.75F);

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
	public void lock(OAObject object) {
	    if (object == null) throw new IllegalArgumentException("object can not be null");
	
	    RemoteSessionInterface rc = OASync.getRemoteSession(object.getClass());
	    if (rc != null) {
	        rc.setLock(object.getClass(), object.getObjectKey(), true);
	    	return;
	    }
	            
	    OALock newLock = new OALock(object, null, null);
	    synchronized (hmLock) {
	        for (;;) {
	            OALock lock = (OALock) hmLock.get(object);
	            if (lock == null) break;
	            try {
	                int x = lock.getWaitCount();
	                lock.setWaitCount(x + 1);
	                hmLock.wait();
	            }
	            catch (InterruptedException e) {
	            }
	        }
	        hmLock.put(object, newLock);
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
	public void unlock(OAObject object) {
	    if (object == null) return;

        RemoteSessionInterface rc = OASync.getRemoteSession(object.getClass());
        if (rc != null) {
            rc.setLock(object.getClass(), object.getObjectKey(), false);
            return;
        }
	    
	    synchronized (hmLock) {
	    	hmLock.remove(object);
	    	hmLock.notifyAll();
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
	public boolean isLocked(OAObject object) {
	    if (object == null) return false;

        RemoteSessionInterface rc = OASyncDelegate.getRemoteSession(object.getClass());
        if (rc != null) {
            return rc.isLocked(object.getClass(), object.getObjectKey());
        }
        synchronized (hmLock) {
            return (hmLock.get(object) != null);
        }
        
	}
    
}
