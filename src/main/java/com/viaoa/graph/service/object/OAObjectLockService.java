package com.viaoa.graph.service.object;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public abstract class OAObjectLockService {
	private static final Logger LOG = Logger.getLogger(OAObjectLockService.class.getName());

	public OAObjectLockService() {
	}

	/**
	 * Shared in-JVM lock map that tracks active locks for OAObject instances.
	 * Each locked object is stored as a key mapped to an OALock instance,
	 * enabling local, non-distributed locking when no remote sync session
	 * is active.
	 */
    private final Map<OAObject, OALock> hmObjectLock = new HashMap<>(11, 0.75F);

	// property locking
	private final Map<String, PropertyLock> hmPropertyLock = new ConcurrentHashMap<>();
	private final Map<Thread, Thread> hmWaitingOnPropertyLock = new ConcurrentHashMap<>();

	private static final class PropertyLock {
		final Thread thread;
		boolean done;
		boolean hasWait;
		public PropertyLock(Thread thread) {
			this.thread = thread;
		}
	}
    
    
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
	public boolean lock(OAObject object) {
	    if (object == null) throw new IllegalArgumentException("object can not be null");

	    if (callSyncIsServer() || callSyncIsClient()) {
	    	// locks will be under RemoteSessionImpl.hashLock
	    	return callSyncSetLock(object.getClass(), object.getObjectKey(), true);
	    }
		final Thread threadThis = Thread.currentThread();
	    
	    synchronized (hmObjectLock) {
	        for (;;) {
	            OALock lock = hmObjectLock.get(object);
	            if (lock == null) break;
	            if (lock.getThread() == threadThis) return false; // already locked by this thread
	            try {
	                lock.setWaitCount(lock.getWaitCount() + 1);
	                hmObjectLock.wait();
	            }
	            catch (InterruptedException e) {
	            	Thread.currentThread().interrupt();	            	
	            	return false;
	            }
	        }
		    OALock newLock = new OALock(object, null, null);
	        hmObjectLock.put(object, newLock);
	    }
	    return true;
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
	public boolean unlock(OAObject object) {
	    if (object == null) return false;

	    if (callSyncIsServer() || callSyncIsClient()) {
	    	// locks will be under RemoteSessionImpl.hashLock
	    	return callSyncSetLock(object.getClass(), object.getObjectKey(), false);
	    }
	    
		synchronized (hmObjectLock) {
            OALock lock = hmObjectLock.get(object);
            if (lock != null) {
                if (lock.getThread() == Thread.currentThread()) {
                	hmObjectLock.remove(object);
                	hmObjectLock.notifyAll();
                	return true;
                }
            }
	    }
		return false;
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

	    if (callSyncIsServer() || callSyncIsClient()) {
	    	// locks will be under RemoteSessionImpl.hashLock
	    	return callSyncIsLocked(object.getClass(), object.getObjectKey());
	    }
	    
	    OALock lock;
        synchronized (hmObjectLock) {
        	lock = hmObjectLock.get(object);
        }
        return (lock != null);
	}

	public boolean isLockedByAnotherThread(OAObject object) {
	    if (object == null) return false;

	    if (callSyncIsServer() || callSyncIsClient()) {
	    	// locks will be under RemoteSessionImpl.hashLock
	    	return callSyncIsLocked(object.getClass(), object.getObjectKey());
	    }
	    
	    OALock lock;
        synchronized (hmObjectLock) {
        	lock = hmObjectLock.get(object);
        }
    	if (lock == null) return false;
        return (lock.getThread() != Thread.currentThread());
	}
	
	
	/**
	 * Attempts to acquire an exclusive lock for the specified property.  
	 * This call will wait if necessary until the lock becomes available.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to lock
	 * @return true if the lock is successfully acquired; false otherwise
	 */
	public boolean setPropertyLock(OAObject oaObj, String name) {
		return _setPropertyLock(oaObj, name, true);
	}

	/**
	 * Attempts to acquire an exclusive lock for the specified property
	 * without waiting.  
	 * If the lock is already held by another thread, this method returns
	 * immediately with {@code false}.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to lock
	 * @return true if the lock is acquired; false if it is already held
	 */
	public boolean attemptPropertyLock(OAObject oaObj, String name) {
		return _setPropertyLock(oaObj, name, false);
	}

	/**
	 * Core implementation for acquiring a property-level lock.  
	 * Creates or reuses a lock entry and manages waiting behavior, deadlock
	 * detection, and re-entry checks depending on the supplied flags.
	 *
	 * @param oaObj              the target object
	 * @param name               the property name to lock
	 * @param bWaitIfNeeded      true to wait until the lock becomes available;
	 *                           false to return immediately if locked
	 * @return true if the lock is acquired according to the requested rules;
	 *         false otherwise
	 */
	private boolean _setPropertyLock(final OAObject oaObj, final String name, final boolean bWaitIfNeeded) {
		if (oaObj == null || name == null) {
			return false;
		}

		final Thread threadThis = Thread.currentThread();
		final String key = oaObj.getGuid() + "." + name.toUpperCase(Locale.ROOT);

		for (int iOuter=0;; iOuter++) {
			PropertyLock lock = hmPropertyLock.computeIfAbsent(key, k -> new PropertyLock(threadThis));
			if (lock.thread == threadThis) {
				return true;
			}
	
			try {
				hmWaitingOnPropertyLock.put(threadThis, lock.thread);
				if (iOuter == 0) callRemoteThreadStartNextThread();
				synchronized (lock) {
					if (!bWaitIfNeeded) {
						return false;
					}
					long ms = 0;
					for (int i = 0;; i++) {
						if (i > 3) {
							// see if the thread that thisThread is waiting on is waiting on another thread (possible deadlock)
							Thread tx = hmWaitingOnPropertyLock.get(lock.thread);
							if (tx != null) {
								if (OAObject.getDebugMode()) {
									String s = oaObj.getObjectKey().toString();
									s = "thread with lock is waiting on a lock, obj=" + oaObj + ", key=" + s + ", prop=" + name
											+ ", this.Thread=" + Thread.currentThread().getName() + ", waiting on Thread="
											+ lock.thread.getName() + " (see next stacktrace), will continue";
									LOG.log(Level.WARNING, s, new Exception("fyi: avoiding deadlock, will continue"));
									StackTraceElement[] stes = lock.thread.getStackTrace();
									Exception ex = new Exception();
									ex.setStackTrace(stes);
									LOG.log(Level.WARNING, "... waiting on this Thread=" + lock.thread.getName(), ex);
								}
								break; // retry
							}
	
							if (ms == 0) {
								ms = System.currentTimeMillis();
							} else if (System.currentTimeMillis() - ms > 60000) {
								if (OAObject.getDebugMode()) {
									String s = oaObj.getObjectKey().toString();
									s = "wait time exceeded for lock, obj=" + oaObj + ", key=" + s + ", prop=" + name + ", this.Thread="
											+ Thread.currentThread().getName() + ", waiting on Thread=" + lock.thread.getName()
											+ " (see next stacktrace), will continue";
									LOG.log(Level.WARNING, s, new Exception("fyi: wait time exceeded, will continue"));
									StackTraceElement[] stes = lock.thread.getStackTrace();
									Exception ex = new Exception();
									ex.setStackTrace(stes);
									LOG.log(Level.WARNING, "... waiting on this Thread=" + lock.thread.getName(), ex);
								}
								return false; // bail out, ouch
							}
						}
						if (lock.done) {
							break;  // retry getting lock
						}
						lock.hasWait = true;
						try {
							lock.wait(100);
						}
			            catch (InterruptedException e) {
			            	threadThis.interrupt();
			            	return false;
						}
					}
				}
			} finally {
				hmWaitingOnPropertyLock.remove(threadThis);
			}
		}
	}
	
	/**
	 * Releases the lock associated with the specified property, if one exists.
	 * Any threads waiting on the lock are notified so they may attempt to
	 * acquire it.
	 *
	 * @param oaObj the target object
	 * @param name  the property name whose lock should be released
	 */
	public boolean releasePropertyLock(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		final String key = oaObj.getGuid() + "." + name.toUpperCase(Locale.ROOT);
		PropertyLock lock = hmPropertyLock.get(key);
		if (lock == null) return false;
		
		final Thread threadThis = Thread.currentThread();
		synchronized (lock) {
			if (lock.thread != threadThis) {
				return false;
			}
			lock.done = true;
			hmPropertyLock.remove(key);
			if (lock.hasWait) {
				lock.notifyAll();
			}
		}
		return true;
	}
	
	/**
	 * Checks whether a lock exists for the specified property.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to check
	 * @return true if the property is currently locked; false otherwise
	 */
	public boolean isPropertyLocked(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		String key = oaObj.getGuid() + "." + name.toUpperCase(Locale.ROOT);
		PropertyLock lock = hmPropertyLock.get(key);
		return (lock != null);
	}

	public boolean isPropertyLockedByAnotherThread(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		String key = oaObj.getGuid() + "." + name.toUpperCase(Locale.ROOT);
		PropertyLock lock = hmPropertyLock.get(key);
		return (lock != null && lock.thread != Thread.currentThread());
	}
	
	public abstract boolean callSyncIsClient();
	public abstract boolean callSyncIsServer();
	public abstract boolean callSyncSetLock(Class<? extends OAObject> objectClass, OAObjectKey objectKey, boolean bLock);
	public abstract boolean callSyncIsLocked(Class<? extends OAObject> objectClass, OAObjectKey objectKey);
	public abstract void callRemoteThreadStartNextThread();
	
}
