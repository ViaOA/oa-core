/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.context.OAContext;
import com.viaoa.hub.*;
import com.viaoa.json.OAJson;
import com.viaoa.process.OAProcess;
import com.viaoa.remote.*;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.transaction.OATransaction;
import com.viaoa.undo.OAUndoManager;
import com.viaoa.util.*;

/**
 * Delegate class used to store information about the local thread. This is used internally throughout OA to set specific features for a
 * thread. Note: it is important to make sure to call the corresponding reverse value, so that the flags and counters will be unset and the
 * Thread will be removed from internal map.
 *
 * @author vvia
 */
public class OAThreadLocalDelegate {

	private static Logger LOG = Logger.getLogger(OAThreadLocalDelegate.class.getName());

	private static final ThreadLocal<OAThreadLocal> threadLocal = new ThreadLocal<OAThreadLocal>();

	private static final AtomicInteger TotalIsLoading = new AtomicInteger();
	private static final AtomicInteger TotalObjectCacheAddMode = new AtomicInteger();
	private static final AtomicInteger TotalObjectSerializer = new AtomicInteger();
	private static final AtomicInteger TotalSuppressCSMessages = new AtomicInteger();
	private static final AtomicInteger TotalDelete = new AtomicInteger();
	private static final AtomicInteger TotalTransaction = new AtomicInteger();
	private static final AtomicInteger TotalCaptureUndoablePropertyChanges = new AtomicInteger();
	private static final AtomicInteger TotalHubMergerChanging = new AtomicInteger();
	//    private static final AtomicInteger TotalGetDetailHub = new AtomicInteger();
	private static final AtomicInteger TotalSiblingHelper = new AtomicInteger();
	private static final AtomicInteger TotalRemoteMultiplexerClient = new AtomicInteger();
	private static final AtomicInteger TotalNotifyWaitingObject = new AtomicInteger();

	private static AtomicInteger TotalHubListenerTreeCount = new AtomicInteger();
	private static final AtomicInteger TotalHubEvent = new AtomicInteger();

	public static final HashMap<Object, OAThreadLocal[]> hmLock = new HashMap<Object, OAThreadLocal[]>(53, .75f);

	private static final AtomicInteger TotalDontAdjustHub = new AtomicInteger();
	private static final AtomicInteger TotalJackson = new AtomicInteger();
	private static final AtomicInteger TotalIsRefreshing = new AtomicInteger();

	protected static OAThreadLocal getThreadLocal(boolean bCreateIfNull) {
		OAThreadLocal ti = threadLocal.get();
		if (ti == null && bCreateIfNull) {
			ti = new OAThreadLocal();
			ti.time = System.currentTimeMillis();
			threadLocal.set(ti);
			// LOG.finest("new OAThreadLocal created");
		}
		return ti;
	}

	// Transaction -------------------
	private static long msTransaction;

	/**
	 * Called by OATransaction.start() Used internally by classes that work with the transaction. Use OATransaction instead of calling
	 * directly.
	 */
	public static void setTransaction(OATransaction t) {
		OAThreadLocal ti = getThreadLocal(true);
		ti.transaction = t;
		int x;
		if (t != null) {
			x = TotalTransaction.incrementAndGet();
		} else {
			x = TotalTransaction.decrementAndGet();
		}
		if (x > 7 || x < 0) {
			msTransaction = throttleLOG("TotalTransaction =" + x, msTransaction);
		}
	}

	public static OATransaction getTransaction() {
		if (TotalTransaction.get() == 0) {
			return null;
		}
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.transaction;
	}

	/**
	 * @see OAThreadLocal#loading
	 */
	public static boolean isLoading() {
		boolean b;
		if (OAThreadLocalDelegate.TotalIsLoading.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = isLoading(OAThreadLocalDelegate.getThreadLocal(false));
			// LOG.finest(""+b);
		}
		return b;
	}

	protected static boolean isLoading(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.loading > 0;
	}

	public static boolean setLoading(boolean b) {
		// LOG.finer(""+b);
		return setLoading(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msLoadingObject;

	protected static boolean setLoading(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		int x, x2;
		boolean bPreviousValue;
		if (b) {
			bPreviousValue = (ti.loading > 0);
			x = ++ti.loading;
			x2 = OAThreadLocalDelegate.TotalIsLoading.getAndIncrement();
		} else {
			bPreviousValue = (ti.loading > 0);
			x = --ti.loading;
			x2 = OAThreadLocalDelegate.TotalIsLoading.decrementAndGet();
		}
		if (x > 50 || x < 0 || x2 > 50 || x2 < 0) {
			msLoadingObject = throttleLOG("TotalIsLoading=" + x2 + ", ti=" + x, msLoadingObject);
		}
		return bPreviousValue;
	}

	// CacheAddMode ----------------------
	public static int getObjectCacheAddMode() {
		int mode;
		if (OAThreadLocalDelegate.TotalObjectCacheAddMode.get() == 0) {
			mode = OAObjectCacheDelegate.DefaultAddMode;
			// LOG.finest("fast");
		} else {
			mode = getObjectCacheAddMode(OAThreadLocalDelegate.getThreadLocal(false));
			// LOG.finest(""+mode);
		}
		return mode;
	}

	private static long msObjectCacheAddMode;

	public static void setObjectCacheAddMode(int mode) {
		// LOG.finer("mode="+mode);
		if (mode == OAObjectCacheDelegate.DefaultAddMode) {
			mode = 0;
		}
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(mode != 0);
		if (ti == null) {
			return;
		}

		int old = ti.cacheAddMode;
		if (old == mode) {
			return; // no change
		}
		ti.cacheAddMode = mode;

		if (old == 0 || mode == 0) { // dont update total if it has already been called for this ti
			if (mode == 0) {
				if (OAThreadLocalDelegate.TotalObjectCacheAddMode.get() > 0) {
					int x = OAThreadLocalDelegate.TotalObjectCacheAddMode.decrementAndGet();
					if (x < 0) {
						msObjectCacheAddMode = throttleLOG("TotalObjectCacheAddMode =" + x, msObjectCacheAddMode);
					}
				}
			} else {
				int x = OAThreadLocalDelegate.TotalObjectCacheAddMode.incrementAndGet();
				if (x > 15) {
					msObjectCacheAddMode = throttleLOG("TotalObjectCacheAddMode =" + x, msObjectCacheAddMode);
				}
			}
		}
	}

	protected static int getObjectCacheAddMode(OAThreadLocal ti) {
		if (ti == null) {
			return OAObjectCacheDelegate.DefaultAddMode;
		}
		if (ti.cacheAddMode == 0) {
			return OAObjectCacheDelegate.DefaultAddMode;
		}
		return ti.cacheAddMode;
	}

	// OAObjectSerializeInterface ---------------
	/**
	 * used by Serialization for the current thread. OAObjectSerializeInterface is called to return the type of serialization to perform.
	 * SERIALIZE_STRIP_NONE or SERIALIZE_STRIP_REFERENCES
	 */
	public static OAObjectSerializer getObjectSerializer() {
		OAObjectSerializer si;
		if (OAThreadLocalDelegate.TotalObjectSerializer.get() == 0) {
			si = null;
			// LOG.finest("fast");
		} else {
			si = getObjectSerializer(OAThreadLocalDelegate.getThreadLocal(false));
			// LOG.finest("OAObjectSerializer="+(si != null));
		}
		return si;
	}

	protected static OAObjectSerializer getObjectSerializer(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.objectSerializer;
	}

	public static void setObjectSerializer(OAObjectSerializer si) {
		// LOG.finer("OAObjectSerializer="+(si != null));
		setObjectSerializer(OAThreadLocalDelegate.getThreadLocal(si != null), si);
	}

	private static long msObjectSerializer;

	protected static void setObjectSerializer(OAThreadLocal ti, OAObjectSerializer si) {
		if (ti == null) {
			return;
		}
		if (ti.objectSerializer == si) {
			return;
		}
		OAObjectSerializer old = ti.objectSerializer;
		if (si == old) {
			return; // no change
		}
		ti.objectSerializer = si;

		if (old == null || si == null) { // dont update total if it has already been called for this ti
			int x;
			if (si != null) {
				x = OAThreadLocalDelegate.TotalObjectSerializer.incrementAndGet();
			} else {
				x = OAThreadLocalDelegate.TotalObjectSerializer.decrementAndGet();
			}
			if (x > 25 || x < 0) {
				msObjectSerializer = throttleLOG("TotalObjectSerializeInterface =" + x, msObjectSerializer);
			}
		}
	}

	// SuppressCSMessages -----------------------
	public static boolean isSuppressCSMessages() {
		boolean b;
		if (OAThreadLocalDelegate.TotalSuppressCSMessages.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = isSuppressCSMessages(OAThreadLocalDelegate.getThreadLocal(false));
			// LOG.finest(""+b);
		}
		return b;
	}

	public static boolean isSuppressCSMessages(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.suppressCSMessages > 0;
	}

	public static void setSuppressCSMessages(boolean b) {
		// LOG.finest(""+b);
		setSuppressCSMessages(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msSuppressCSMessages;

	public static void setSuppressCSMessages(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x, x2;
		if (b) {
			x = ++ti.suppressCSMessages;
			x2 = OAThreadLocalDelegate.TotalSuppressCSMessages.incrementAndGet();
		} else {
			x = --ti.suppressCSMessages;
			x2 = OAThreadLocalDelegate.TotalSuppressCSMessages.decrementAndGet();
		}
		if (x > 30 || x < 0 || x2 > 50 || x2 < 0) {
			msSuppressCSMessages = throttleLOG("TotalSuppressCSMessages =" + x2 + ", ti=" + x, msSuppressCSMessages);
		}
	}

	// Deleting -----------------------

	private final static ConcurrentHashMap hmDeleting = new ConcurrentHashMap<>();

	/**
	 * Is this thread currently deleting.
	 */
	public static boolean isDeleting() {
		if (OAThreadLocalDelegate.TotalDelete.get() == 0) {
			return false;
		}
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return false;
		}
		return ti.deleting != null && ti.deleting.length > 0;
	}

	/**
	 * Is any thread currently deleting an object.
	 */
	public static boolean isDeleting(Object obj) {
		if (obj == null) {
			return false;
		}
		return hmDeleting.contains(obj);
	}

	/**
	 * Is this thread currently deleting an object/hub.
	 */
	public static boolean isThreadDeleting(Object obj) {
		if (obj == null) {
			return false;
		}
		if (OAThreadLocalDelegate.TotalDelete.get() == 0) {
			// LOG.finest("fast");
			return false;
		}

		if (!hmDeleting.contains(obj)) {
			return false;
		}

		boolean b = isDeleting(OAThreadLocalDelegate.getThreadLocal(false), obj);
		// LOG.finest(""+b);

		return b;
	}

	protected static boolean isDeleting(OAThreadLocal ti, Object obj) {
		if (obj == null) {
			return false;
		}
		if (ti == null || ti.deleting == null) {
			return false;
		}
		int x = ti.deleting.length;
		if (x == 0) {
			return false;
		}
		for (int i = 0; i < x; i++) {
			if (ti.deleting[i] == obj) {
				return true;
			}
		}
		return false;
	}

	private static long msDeleting;

	public static void setDeleting(Object obj, boolean b) {
		// LOG.finer(""+b);
		if (obj == null) {
			return;
		}

		if (b) {
			hmDeleting.put(obj, obj);
			if (hmDeleting.size() > 25) {
				msDeleting = throttleLOG("TotalDeleting =" + hmDeleting.size(), msDeleting);
			}
		} else {
			hmDeleting.remove(obj);
		}

		setDeleting(OAThreadLocalDelegate.getThreadLocal(b), obj, b);
	}

	protected static void setDeleting(OAThreadLocal ti, Object obj, boolean b) {
		if (ti == null) {
			return;
		}
		if (obj == null) {
			return;
		}
		if (b) {
			if (ti.deleting == null) {
				ti.deleting = new Object[1];
			}
			int x = ti.deleting.length;
			for (int i = 0;; i++) {
				if (i == x) {
					Object[] objs = new Object[x + 3];
					System.arraycopy(ti.deleting, 0, objs, 0, x);
					ti.deleting = objs;
					ti.deleting[x] = obj;
					break;
				}
				if (ti.deleting[i] == obj) {
					return;
				}
				if (ti.deleting[i] == null) {
					ti.deleting[i] = obj;
					break;
				}
			}
			x = OAThreadLocalDelegate.TotalDelete.incrementAndGet();
			if (x > 100) {
				msDeleting = throttleLOG("TotalDelete =" + x, msDeleting);
			}
		} else {
			if (ti.deleting == null) {
				return;
			}
			int x = ti.deleting.length;
			boolean bAllNull = true;
			boolean bFound = false;
			for (int i = 0; i < x; i++) {
				if (ti.deleting[i] == obj) {
					bFound = true;
					ti.deleting[i] = null;
				} else {
					if (ti.deleting[i] != null) {
						bAllNull = false;
					}
				}
			}
			if (bFound) {
				OAThreadLocalDelegate.TotalDelete.decrementAndGet();
			}
			if (bAllNull) {
				ti.deleting = null;
			}
		}
	}

	/**
	 * getFlag ----------------------- Flag used for generic/misc purposes
	 */
	public static boolean isFlag(Object obj) {
		return isFlag(OAThreadLocalDelegate.getThreadLocal(false), obj);
	}

	protected static boolean isFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return false;
		}
		return OAArray.contains(ti.flags, obj);
	}

	private static long msFlag;

	public static void setFlag(Object obj) {
		setFlag(OAThreadLocalDelegate.getThreadLocal(true), obj);
	}

	protected static void setFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.flags = OAArray.add(Object.class, ti.flags, obj);
		if (ti.flags != null && ti.flags.length > 20) {
			msFlag = throttleLOG("OAThreadLocal.tiFlags.length =" + ti.flags.length, msFlag);
		}
	}

	public static void removeFlag(Object obj) {
		setFlag(OAThreadLocalDelegate.getThreadLocal(false), obj);
	}

	protected static void removeFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.flags = OAArray.removeValue(Object.class, ti.flags, obj);
	}

	// 20110104
	// Locking -----------------------
	/**
	 * This locking was created to prevent deadlocks. If a thread is waiting on an object, and the thread already has a lock, then it can be
	 * allowed to also have the lock - after waiting a set amount of time. Each Object that is locked keeps track of the threadLocals that
	 * are using it. The first threadLocal in the array is the owner. Once it is done (unlocked), it will notify the next threadLocal, etc.
	 * Ideally, only one threadLocal at a time will have access to the Object - while the other threads wait. If another thread already has
	 * lock(s) on other objects, then it can also be allowed to use the object - after waiting a certain amount of time, and still not given
	 * the lock.
	 *
	 * @param maxWaitTries (default=10) max number of waits (each 50 ms) to wait before taking the lock - 0 to wait until notified. This
	 *                     will only be used if the current threadLocal has 1+ locks already and object is locked by another threadLocal.
	 */
	public static void lock(Object object, int maxWaitTries) {
		lock(OAThreadLocalDelegate.getThreadLocal(true), object, maxWaitTries);
	}

	public static void lock(Object object) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		lock(ti, object, 2);
	}

	public static boolean hasLock() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		return (ti != null && ti.locks != null && ti.locks.length > 0);
	}

	public static boolean hasLock(Object obj) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return false;
		}
		Object[] objs = ti.locks;
		if (objs == null) {
			return false;
		}
		for (Object objx : objs) {
			if (objx == obj) {
				return true;
			}
		}
		return false;
	}

	public static Object[] getLocks() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.locks;
	}

	public static boolean isLocked(Object object) {
		synchronized (hmLock) {
			OAThreadLocal[] tis = hmLock.get(object); // threadLocals that are using object (locked or waiting)
			return (tis != null && tis.length > 0);
		}
	}

	public static boolean isLockOwner(Object object) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return false;
		}
		synchronized (hmLock) {
			OAThreadLocal[] tis = hmLock.get(object); // threadLocals that are using object (locked or waiting)
			return (tis != null && tis.length > 0 && tis[0] == ti);
		}
	}

	public static OAThreadLocal getOAThreadLocal() {
		return OAThreadLocalDelegate.getThreadLocal(true);
	}

	private static long timeLastStackTrace;
	private static int errorCnt;

	// used for lock/unlock
	protected static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	static volatile int openLockCnt;
	static volatile int lockCnt;
	static volatile int unlockCnt;

	protected static void lock(OAThreadLocal tiThis, Object thisLockObject, int maxWaitTries) {
		//System.out.println((++lockCnt)+") ****** OAThreadLocalDelegate.lock obj="+thisLockObject+", activeLocks="+(++openLockCnt));
		if (thisLockObject == null || tiThis == null) {
			return;
		}

		OARemoteThread rt = null;

		for (int tries = 0;; tries++) {

			rwLock.writeLock().lock();
			try {
				boolean b = _lock(tiThis, thisLockObject, maxWaitTries, tries);
				if (b) {
					break;
				}

				// theadLocal will need to wait
				tiThis.bIsWaitingOnLock = true;

				if (tiThis.locks.length > 1) {
					// need to wake up any threads that are waiting on this thread
					releaseDeadlock(tiThis, thisLockObject);
				}
			} finally {
				rwLock.writeLock().unlock();
			}

			// wait on ThreadLocal
			synchronized (tiThis) {
				if (!tiThis.bIsWaitingOnLock) {
					continue; // it's been notified by thread that had the lock, try again
				}

				if (tries == 0) {
					Thread t = Thread.currentThread();
					if (t instanceof OARemoteThread) {
						rt = (OARemoteThread) t;
						rt.setWaitingOnLock(true);
					}
				}

				int msWait;
				if (tiThis.locks != null && tiThis.locks.length > 1) {
					msWait = 5; // could be deadlock situation
				} else {
					msWait = 25;
				}

				try {
					tiThis.wait(msWait); // wait for wake up
				} catch (InterruptedException e) {
					// System.out.println("ERRROR");
				}
			}
		}
		if (rt != null) {
			rt.setWaitingOnLock(false);
		}
	}

	// returns true if it's ok to continue, and not if wait on lock to be released
	private static boolean _lock(OAThreadLocal tlThis, Object thisLockObject, int maxWaitTries, int tries) {
		OAThreadLocal[] tls = hmLock.get(thisLockObject); // threadLocals that are using object (locked or waiting)

		if (tls != null && tls.length > 0 && tls[0] == tlThis) {
			// this ThreadLocal already is the owner for this object
			if (tries == 0) {
				// need to add it to ti.locks, since it will be released more then once
				tlThis.locks = OAArray.add(Object.class, tlThis.locks, thisLockObject);
			}
			// check locks to make sure that it is not getting too big
			if (tlThis.locks.length > 39 && (tlThis.locks.length % 10) == 0) {
				// see if all objects are still locked
				String s = "";
				for (Object objx : tlThis.locks) {
					OAThreadLocal[] tisx = hmLock.get(objx);
					if (tisx == null) {
						s = ", error: there are objects in ti.locks that are no longer locked";
					}
				}
				s = "OAThreadLocal.locks size=" + tlThis.locks.length + s;
				LOG.warning(s);
			}
			tlThis.bIsWaitingOnLock = false;
			return true; // already is the lock owner
		}

		if (tries == 0) {
			// must be inside sync: add to list of objects that this TI is locking
			tlThis.locks = OAArray.add(Object.class, tlThis.locks, thisLockObject);

			if (tls == null) {
				tls = new OAThreadLocal[] { tlThis };
			} else {
				tls = (OAThreadLocal[]) OAArray.add(OAThreadLocal.class, tls, tlThis);
			}
			hmLock.put(thisLockObject, tls);
		}

		if (tls[0] == tlThis) {
			tlThis.bIsWaitingOnLock = false;
			return true; // this thread owns the lock
		}

		if (maxWaitTries > 0 && tries >= maxWaitTries && tries > 1) {
			if (tls[1] != tlThis) {
				// need to be second in list, since the owner (at pos [0]) will notify [1] when it is done - and not another threadLocal
				tls = (OAThreadLocal[]) OAArray.removeValue(OAThreadLocal.class, tls, tlThis);
				tls = (OAThreadLocal[]) OAArray.insert(OAThreadLocal.class, tls, tlThis, 1);
				hmLock.put(thisLockObject, tls);
			}
			tlThis.bIsWaitingOnLock = false;
			if (maxWaitTries > 2) {
				String s = "this.thread " + Thread.currentThread().getName() + ", timedout waiting for:" + thisLockObject + ", locked by:"
						+ tls[0].threadName;
				LOG.fine(s);
			}
			return true; // done trying
		}
		return false;
	}

	public static int cntDeadlock;

	public static int getDeadlockCount() {
		return cntDeadlock;
	}

	// this should be called with rwLock.write locked
	private static void releaseDeadlock(OAThreadLocal tiThis, Object lockObject) {
		OAThreadLocal[] tls = hmLock.get(lockObject);
		if (tls == null) {
			return;
		}
		OAThreadLocal tlOwner = tls[0];

		Object[] ownerLocks = tlOwner.locks;
		if (ownerLocks == null) {
			return;
		}

		for (Object ownerLockObj : ownerLocks) {
			if (ownerLockObj == lockObject) {
				continue;
			}
			tls = hmLock.get(ownerLockObj);
			if (tls == null || tls[0] != tiThis) {
				continue; // not locked by ti
			}

			int pos = OAArray.indexOf(tls, tlOwner);
			if (pos < 0) {
				continue;
			}
			tls[0] = tlOwner;
			tls[pos] = tiThis;

			if (pos != 1) {
				tls[pos] = tls[1];
				tls[1] = tiThis;
			}

			cntDeadlock++;
			synchronized (tlOwner) {
				tlOwner.bIsWaitingOnLock = false;
				tlOwner.notify();
			}

			LOG.warning("LOCK:OAThreadLocalDelegate: Found Deadlock, obj=" + lockObject + ", releasing one of the locks");
			break;
		}
	}

	public static void releaseAllLocks() {
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return;
		}
		Object[] locks = tl.locks;
		if (locks == null) {
			return;
		}
		for (Object obj : locks) {
			unlock(obj);
		}
	}

	public static void unlock(Object object) {
		unlock(OAThreadLocalDelegate.getThreadLocal(true), object);
	}

	protected static void unlock(OAThreadLocal ti, Object object) {
		//System.out.println((++unlockCnt)+") ****** OAThreadLocalDelegate.unlock obj="+object+", activeLocks="+(--openLockCnt));
		try {
			rwLock.writeLock().lock();
			_unlock(ti, object);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	private static void _unlock(OAThreadLocal tl, Object object) {
		final int pos = OAArray.indexOf(tl.locks, object);
		if (pos < 0) {
			return;
		}

		final boolean bMoreLocks = OAArray.indexOf(tl.locks, object, pos + 1) >= 0;

		OAThreadLocal[] tls = hmLock.get(object);
		if (tls != null) {
			boolean bIsLockOwner = (tls.length > 0 && tls[0] == tl);

			if (tls.length == 1) {
				if (bIsLockOwner && !bMoreLocks) {
					hmLock.remove(object);
				}
				tls = null;
			} else {
				if (!bMoreLocks) {
					tls = (OAThreadLocal[]) OAArray.removeValue(OAThreadLocal.class, tls, tl);
					hmLock.put(object, tls);
				}
			}

			if (tls != null && bIsLockOwner && !bMoreLocks) {
				synchronized (tls[0]) {
					tls[0].bIsWaitingOnLock = false; // notify the next one waiting
					tls[0].notify();
				}
			}
		}
		tl.locks = OAArray.removeAt(Object.class, tl.locks, pos); // must be inside sync
	}

	// HubListenerTree uses this to ignore dependent property changes caused by add/remove objects from hubMerger.hubMaster
	public static boolean isHubMergerChanging() {
		boolean b;
		if (OAThreadLocalDelegate.TotalHubMergerChanging.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = isHubMergerChanging(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return b;
	}

	protected static boolean isHubMergerChanging(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.hubMergerChangingCount > 0;
	}

	public static void setHubMergerChanging(boolean b) {
		// LOG.finer(""+b);
		setHubMergerChanging(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msHubMergerChanging;

	protected static void setHubMergerChanging(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x;

		if (b) {
			ti.hubMergerChangingCount++;
			x = OAThreadLocalDelegate.TotalHubMergerChanging.getAndIncrement();
		} else {
			ti.hubMergerChangingCount--;
			x = OAThreadLocalDelegate.TotalHubMergerChanging.decrementAndGet();
		}
		if (x > 200 || x < 0) {
			msHubMergerChanging = throttleLOG("TotalHubMergerChanging=" + x, msHubMergerChanging);
		}
	}

	// UndoablePropertyChanges -----------------------

	/**
	 * Flag to know if property changes should be adding undoable events. <br>
	 *
	 * @see OAUndoManager
	 */
	public static void setCreateUndoablePropertyChanges(boolean b) {
		// LOG.finer(""+b);
		setCreateUndoablePropertyChanges(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msCreateUndoablePropertyChanges;

	protected static void setCreateUndoablePropertyChanges(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		if (ti.compoundUndoableName != null) {
			return;
		}
		int x;
		ti.createUndoablePropertyChanges = b;
		if (b) {
			x = OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.getAndIncrement();
		} else {
			x = OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.decrementAndGet();
		}
		if (x > 50 || x < 0) {
			msCreateUndoablePropertyChanges = throttleLOG("TotalCaptureUndoablePropertyChanges=" + x + ", ti.createUndoablePropertyChanges="
					+ ti.createUndoablePropertyChanges, msCreateUndoablePropertyChanges);
		}
	}

	public static boolean getCreateUndoablePropertyChanges() {
		boolean b;
		if (OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = getCreateUndoablePropertyChanges(OAThreadLocalDelegate.getThreadLocal(false));
			// LOG.finest(""+b);
		}
		return b;
	}

	protected static boolean getCreateUndoablePropertyChanges(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.createUndoablePropertyChanges;
	}

	/**
	 * Start a compound undoable.
	 */
	public static void startUndoable(String compoundName) {
		startUndoable(OAThreadLocalDelegate.getThreadLocal(true), compoundName);
	}

	public static void startCompoundUndoable(String compoundName) {
		startUndoable(OAThreadLocalDelegate.getThreadLocal(true), compoundName);
	}

	private static long msUndoable;

	public static boolean isCreatingCompoundUndoable() {
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return false;
		}
		return tl.createUndoablePropertyChanges;
	}

	protected static void startUndoable(OAThreadLocal ti, String compoundName) {
		if (ti == null) {
			return;
		}
		if (compoundName == null) {
			compoundName = "changes";
		}
		ti.createUndoablePropertyChanges = true;
		ti.compoundUndoableName = compoundName;
		OAUndoManager.startCompoundEdit(compoundName);

		int x = OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.getAndIncrement();
		if (x > 50 || x < 0) {
			msUndoable = throttleLOG("TotalCaptureUndoablePropertyChanges=" + x + ", ti.createUndoablePropertyChanges="
					+ ti.createUndoablePropertyChanges, msUndoable);
		}
	}

	public static void endUndoable() {
		endUndoable(OAThreadLocalDelegate.getThreadLocal(true));
	}

	public static void endCompoundUndoable() {
		endUndoable(OAThreadLocalDelegate.getThreadLocal(true));
	}

	protected static void endUndoable(OAThreadLocal ti) {
		if (ti == null) {
			return;
		}
		ti.createUndoablePropertyChanges = false;
		ti.compoundUndoableName = null;
		OAUndoManager.endCompoundEdit();

		OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.decrementAndGet();
	}

	// 20180704
	public static boolean addSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return false;
		}
		return addSiblingHelper(OAThreadLocalDelegate.getThreadLocal(true), sh);
	}

	public static void removeSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return;
		}
		if (TotalSiblingHelper.get() == 0) {
			return;
		}
		removeSiblingHelper(OAThreadLocalDelegate.getThreadLocal(true), sh);
	}

	public static ArrayList<OASiblingHelper> getSiblingHelpers() {
		if (TotalSiblingHelper.get() == 0) {
			return null;
		}
		return getSiblingHelpers(OAThreadLocalDelegate.getThreadLocal(true));
	}

	public static ArrayList<OASiblingHelper> getSiblingHelpers(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.alSiblingHelper;
	}

	public static boolean hasSiblingHelpers() {
		if (TotalSiblingHelper.get() == 0) {
			return false;
		}
		ArrayList<OASiblingHelper> al = getSiblingHelpers(OAThreadLocalDelegate.getThreadLocal(true));
		return (al != null && al.size() > 0);
	}

	public static void clearSiblingHelpers() {
		if (TotalSiblingHelper.get() == 0) {
			return;
		}
		ArrayList<OASiblingHelper> al = getSiblingHelpers(OAThreadLocalDelegate.getThreadLocal(true));
		if (al != null) {
			al.clear();
		}
	}

	private static long msSiblingHelper;

	protected static boolean addSiblingHelper(OAThreadLocal ti, OASiblingHelper sh) {
		if (ti == null || sh == null) {
			return false;
		}
		if (ti.alSiblingHelper == null) {
			ti.alSiblingHelper = new ArrayList<>();
		} else if (ti.alSiblingHelper.contains(sh)) {
			return false;
		}

		int x = TotalSiblingHelper.incrementAndGet();
		ti.alSiblingHelper.add(sh);
		if (x > 20 || x < 0 || ti.alSiblingHelper.size() > 10) {
			msSiblingHelper = throttleLOG("TotalSiblingHelper.add, tot=" + x + ", this.size=" + ti.alSiblingHelper.size() + ", thread="
					+ Thread.currentThread(), msSiblingHelper);
		}
		return true;
	}

	protected static void removeSiblingHelper(OAThreadLocal ti, OASiblingHelper sh) {
		if (ti == null || sh == null) {
			return;
		}
		int x = TotalSiblingHelper.decrementAndGet();

		if (ti.alSiblingHelper == null) {
			return;
		}
		ti.alSiblingHelper.remove(sh);

		if (x > 20 || x < 0 || ti.alSiblingHelper.size() > 10) {
			msSiblingHelper = throttleLOG("TotalSiblingHelper.remove, tot=" + x + ", this.size=" + ti.alSiblingHelper.size() + ", thread="
					+ Thread.currentThread(), msSiblingHelper);
		}
	}

	private static long msThrottleStackTrace;

	public static long throttleLOG(String msg, long msLast) {
		long ms = System.currentTimeMillis();
		if (ms > msLast + 5000) {
			LOG.warning(msg);
			/*qqqqqqq
			if (ms > msThrottleStackTrace + 30000) {
			    if (msThrottleStackTrace != 0) LOG.warning("ThreadLocalDelegate.stackTraces\n"+getAllStackTraces());
			    msThrottleStackTrace = ms;
			}
			*/
		} else {
			ms = msLast;
		}
		return ms;
	}

	public static String getAllStackTraces() {
		String result = "";
		String s = "DumpAllStackTraces " + (new OADateTime());
		result += s + "\n";

		Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		Iterator it = map.entrySet().iterator();
		for (int i = 1; it.hasNext(); i++) {
			Map.Entry me = (Map.Entry) it.next();
			Thread t = (Thread) me.getKey();
			s = i + ") " + t.getName();
			result += s + "\n";

			StackTraceElement[] stes = (StackTraceElement[]) me.getValue();
			if (stes == null) {
				continue;
			}
			for (StackTraceElement ste : stes) {
				s = "  " + ste.toString(); //was: ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
				result += s + "\n";
			}
		}
		return result;
	}

	public static String getThreadDump() {
		StringBuilder sb = new StringBuilder(1024 * 4);
		Thread t = Thread.currentThread();
		String s = t.getName();
		sb.append(s + OAString.NL);
		StackTraceElement[] stes = t.getStackTrace();
		if (stes != null) {
			for (StackTraceElement ste : stes) {
				s = "  " + ste.toString(); //was:  ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
				sb.append(s + OAString.NL);
			}
		}
		return new String(sb);
	}

	public static void setStatus(String msg) {
		getOAThreadLocal().status = msg;
	}

	// 20140121
	public static void setRemoteRequestInfo(RequestInfo ri) {
		getOAThreadLocal().requestInfo = ri;
	}

	public static RequestInfo getRemoteRequestInfo() {
		return getOAThreadLocal().requestInfo;
	}

	/**
	 * Flag that can be set to allow messages from OARemoteThread to be sent to other clients/server.
	 */
	public static boolean setSendMessages(boolean b) {
		return OARemoteThreadDelegate.sendMessages(b);
	}

	/* 20151103 on hold for OAsyncCombinedClient work
	public static void setRemoteMultiplexerClient(RemoteMultiplexerClient rmc) {
	    setRemoteMultiplexerClient(OAThreadLocalDelegate.getThreadLocal(true), rmc);
	}
	protected static void setRemoteMultiplexerClient(OAThreadLocal ti, RemoteMultiplexerClient rmc) {
	    ti.remoteMultiplexerClient = rmc;
	    int x;
	    if (rmc != null) x = TotalRemoteMultiplexerClient.incrementAndGet();
	    else x = TotalRemoteMultiplexerClient.decrementAndGet();
	    //if (x > 25 || x < 0) LOG.warning("TotalRemoteMultiplexerClient="+x);
	}
	protected static void setSyncClient(OASyncClient sc) {
	    if (sc != null) setRemoteMultiplexerClient(sc.getRemoteMultiplexerClient());
	    else setRemoteMultiplexerClient(null);
	}
	
	
	public static RemoteMultiplexerClient getRemoteMultiplexerClient() {
	    RemoteMultiplexerClient mc;
	    if (OAThreadLocalDelegate.TotalRemoteMultiplexerClient.get() == 0) {
	        mc = null;
	    }
	    else {
	        OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
	        if (tl == null) mc = null;
	        else mc = tl.remoteMultiplexerClient;
	    }
	    return mc;
	}
	*/

	// 20160121 allows an object to wait to be notified by OARemoteThreadDelegate.startNextThread()
	public static void setNotifyObject(Object obj) {
		if (obj == null) {
			if (OAThreadLocalDelegate.TotalNotifyWaitingObject.get() == 0) {
				return;
			}
			OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
			if (tl != null && (tl.notifyObject != null)) {
				TotalNotifyWaitingObject.decrementAndGet();
				tl.notifyObject = obj;
			}
		} else {
			OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(true);
			if (tl.notifyObject == null) {
				TotalNotifyWaitingObject.incrementAndGet();
			}
			tl.notifyObject = obj;
		}
	}

	public static void notifyWaitingThread() {
		if (OAThreadLocalDelegate.TotalNotifyWaitingObject.get() == 0) {
			return;
		}

		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return;
		}
		if (tl.notifyObject == null) {
			return;
		}
		synchronized (tl.notifyObject) {
			tl.notifyObject.notifyAll();
		}
		setNotifyObject(null);
	}

	// recursiveTriggerCount -----------------------
	public static int getRecursiveTriggerCount() {
		int x = getRecursiveTriggerCount(getThreadLocal(false));
		return x;
	}

	protected static int getRecursiveTriggerCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.recursiveTriggerCount;
	}

	public static void setRecursiveTriggerCount(int x) {
		setRecursiveTriggerCount(OAThreadLocalDelegate.getThreadLocal(true), x);
	}

	protected static void setRecursiveTriggerCount(OAThreadLocal ti, int x) {
		if (ti == null) {
			return;
		}
		ti.recursiveTriggerCount = x;
	}

	// HubListenerTree used to determine how deep tree is, caused by listening to dependent props (like calcs, etc)
	public static int getHubListenerTreeCount() {
		int x;
		if (OAThreadLocalDelegate.TotalHubListenerTreeCount.get() == 0) {
			x = 0;
		} else {
			x = getHubListenerTreeCount(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return x;
	}

	protected static int getHubListenerTreeCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.hubListenerTreeCount;
	}

	public static void setHubListenerTree(boolean b) {
		setHubListenerTree(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msHubListenerTree;

	protected static void setHubListenerTree(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x;

		if (b) {
			ti.hubListenerTreeCount++;
			x = OAThreadLocalDelegate.TotalHubListenerTreeCount.getAndIncrement();
		} else {
			ti.hubListenerTreeCount--;
			x = OAThreadLocalDelegate.TotalHubListenerTreeCount.decrementAndGet();
		}
		if (x > 20 || x < 0) {
			msHubListenerTree = throttleLOG("TotalHubListenerTreeCount=" + x, msHubListenerTree);
		}
	}

	public static void setIgnoreTreeListenerProperty(String prop) {
		getThreadLocal(true).ignoreTreeListenerProperty = prop;
	}

	public static String getIgnoreTreeListenerProperty() {
		return getThreadLocal(true).ignoreTreeListenerProperty;
	}

	// 20180223
	public static int getOASyncEventCount() {
		return getThreadLocal(true).oaSyncEventCount;
	}

	public static void incrOASyncEventCount() {
		getThreadLocal(true).oaSyncEventCount++;
	}

	// HubEvent  ---------------
	private static long msHubEvent;

	public static HubEvent getCurrentHubEvent() {
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			return null;
		}
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return null;
		}
		if (tl.alHubEvent == null) {
			return null;
		}
		int x = tl.alHubEvent.size();
		if (x == 0) {
			return null;
		}
		return tl.alHubEvent.get(x - 1);
	}

	public static boolean isOpenHubEvent(HubEvent he) {
		if (he == null) {
			return false;
		}
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			return false;
		}
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null || tl.alHubEvent == null || tl.alHubEvent.size() == 0) {
			return false;
		}
		boolean b = tl.alHubEvent.contains(he);
		return b;
	}

	public static HubEvent getOldestHubEvent() {
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			return null;
		}
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return null;
		}
		if (tl.alHubEvent == null) {
			return null;
		}
		int x = tl.alHubEvent.size();
		if (x == 0) {
			return null;
		}
		return tl.alHubEvent.get(0);
	}

	public static void addHubEvent(HubEvent he) {
		if (he == null) {
			return;
		}
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(true);
		if (tl.alHubEvent == null) {
			tl.alHubEvent = new ArrayList<>();
		}
		if (!tl.alHubEvent.contains(he)) {
			tl.alHubEvent.add(he);
		}

		TotalHubEvent.incrementAndGet();
		int x = tl.alHubEvent.size();
		if (x > 25 || TotalHubEvent.get() > 250) {
			msHubEvent = throttleLOG("TotalHubEvent this=" + x + ", all=" + TotalHubEvent.get(), msHubEvent);
		}
	}

	public static void removeHubEvent(HubEvent he) {
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			return;
		}
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
		if (tl == null) {
			return;
		}
		if (tl.alHubEvent == null) {
			return;
		}
		tl.alHubEvent.remove(he);

		if (tl.alHubEvent.size() == 0) {
			tl.calcPropertyEvents = null;
			;
		}

		TotalHubEvent.decrementAndGet();
		int x = tl.alHubEvent.size();
		if (x > 25 || TotalHubEvent.get() > 250 || TotalHubEvent.get() < 0) {
			msHubEvent = throttleLOG("TotalHubEvent this=" + x + ", all=" + TotalHubEvent.get(), msHubEvent);
		}
	}

	public static boolean isSendingEvent() {
		boolean b;
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			b = false;
		} else {
			b = isSendingEvent(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return b;
	}

	protected static boolean isSendingEvent(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.alHubEvent != null && ti.alHubEvent.size() > 0;
	}

	public static boolean hasSentCalcPropertyChange(Hub thisHub, OAObject thisObj, String propertyName) {
		if (thisHub == null || propertyName == null || thisObj == null) {
			return false;
		}
		if (!isSendingEvent()) {
			return false;
		}

		Hub hubMain = thisHub;
		for (; hubMain.getSharedHub() != null;) {
			hubMain = hubMain.getSharedHub();
		}

		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(true);

		if (tl.calcPropertyEvents == null) {
			tl.calcPropertyEvents = new Tuple3[1];
			tl.calcPropertyEvents[0] = new Tuple3(hubMain, thisObj, propertyName);
			return false;
		}
		for (Tuple3<Hub, OAObject, String> tup : tl.calcPropertyEvents) {
			if (tup.a == hubMain && tup.b == thisObj) {
				if (propertyName.equalsIgnoreCase(tup.c)) {
					return true;
				}
			}
		}
		int x = tl.calcPropertyEvents.length;
		Tuple3<Hub, OAObject, String>[] temp = new Tuple3[x + 1];
		System.arraycopy(tl.calcPropertyEvents, 0, temp, 0, x);
		temp[x] = new Tuple3<Hub, OAObject, String>(hubMain, thisObj, propertyName);
		tl.calcPropertyEvents = temp;

		/*
		if (x % 100 == 0) {
		    LOG.warning("tl.calcPropertyEvents.size = "+tl.calcPropertyEvents.length);
		}
		*/
		return false;
	}

	/**
	 * Get the context that can be used to call OAContext.getContext
	 */
	public static Object getContext() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return ti.context;
	}

	/**
	 * Sets the context that is being used for this Thread.
	 *
	 * @see OAContext#getContextObject()
	 */
	public static void setContext(Object context) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		ti.context = context;
	}

	public static boolean setAdmin(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return setIsAdmin(ti, b);
	}

	public static boolean setIsAdmin(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return setIsAdmin(ti, b);
	}

	public static boolean setIsAdmin(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		boolean b2 = ti.isAdmin;
		ti.isAdmin = b;
		return b2;
	}

	public static boolean isAdmin() {
		return getIsAdmin();
	}

	public static boolean getIsAdmin() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return getIsAdmin(ti);
	}

	public static boolean getIsAdmin(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.isAdmin;
	}

	public static void initialize(OAThreadLocal tl) {
		if (tl == null) {
			return;
		}
		OAThreadLocal tlx = getThreadLocal(true);
		tlx.isAdmin = tl.isAdmin;
		tlx.context = tl.context;
	}

	public static OAJson getOAJackson() {
		if (TotalJackson.get() == 0) {
			return null;
		}
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.oajackson;
	}

	public static OAJson setOAJackson(OAJson jackson) {
		if (jackson == null && TotalJackson.get() == 0) {
			return null;
		}

		if (jackson != null) {
			TotalJackson.incrementAndGet();
		} else {
			TotalJackson.decrementAndGet();
		}

		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		OAJson hold = ti.oajackson;
		ti.oajackson = jackson;
		return hold;
	}

	// 20200121
	/**
	 * Hubs that should not be adjusted when trying to set activeObject and adjusting masterHub.AO
	 */
	public static void addDontAdjustHub(Hub hub) {
		if (hub == null) {
			return;
		}
		hub = HubShareDelegate.getMainSharedHub(hub);
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		ti.dontAdjustHubs = (Hub[]) OAArray.add(Hub.class, ti.dontAdjustHubs, hub);
		TotalDontAdjustHub.incrementAndGet();
		int x = ti.dontAdjustHubs.length;
		if (x > 25 || TotalDontAdjustHub.get() > 250 || TotalDontAdjustHub.get() < 0) {
			msHubEvent = throttleLOG("total DontAdjustHub this=" + x + ", all=" + TotalDontAdjustHub.get(), msHubEvent);
		}
	}

	public static void removeDontAdjustHub(Hub hub) {
		if (hub == null) {
			return;
		}
		if (TotalDontAdjustHub.get() == 0) {
			return;
		}
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return;
		}
		hub = HubShareDelegate.getMainSharedHub(hub);
		ti.dontAdjustHubs = (Hub[]) OAArray.removeValue(Hub.class, ti.dontAdjustHubs, hub);
		TotalDontAdjustHub.decrementAndGet();
	}

	public static boolean getCanAdjustHub(Hub hub) {
		if (hub == null) {
			return false;
		}
		if (TotalDontAdjustHub.get() == 0) {
			return true;
		}

		hub = HubShareDelegate.getMainSharedHub(hub);

		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return true;
		}

		if (ti.dontAdjustHubs == null || ti.dontAdjustHubs.length == 0) {
			return true;
		}

		for (Hub hubx : ti.dontAdjustHubs) {
			Hub hubm = hubx.getMasterHub();
			for (int i = 0; hubm != null && i < 10; i++, hubm = hubm.getMasterHub()) {
				if (HubShareDelegate.getMainSharedHub(hubm) == hub) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isSyncThread() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		return (ti != null && ti.bIsSyncThread);
	}

	public static boolean setSyncThread(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(b);

		if (ti == null) {
			return false;
		}
		boolean b2 = ti.bIsSyncThread;
		ti.bIsSyncThread = b;
		return b2;
	}

	public static boolean isRefreshing() {
		boolean b;
		if (OAThreadLocalDelegate.TotalIsRefreshing.get() == 0) {
			b = false;
		} else {
			b = isRefreshing(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return b;
	}

	protected static boolean isRefreshing(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.refreshing > 0;
	}

	public static void setRefreshing(boolean b) {
		// LOG.finer(""+b);
		setRefreshing(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msRefreshingObject;

	protected static boolean setRefreshing(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		int x, x2;
		boolean bPreviousValue;
		if (b) {
			bPreviousValue = (ti.refreshing > 0);
			x = ++ti.refreshing;
			x2 = OAThreadLocalDelegate.TotalIsRefreshing.getAndIncrement();
		} else {
			bPreviousValue = (ti.refreshing > 0);
			x = --ti.refreshing;
			x2 = OAThreadLocalDelegate.TotalIsRefreshing.decrementAndGet();
		}
		if (x > 50 || x < 0 || x2 > 50 || x2 < 0) {
			msRefreshingObject = throttleLOG("TotalIsRefreshing=" + x2 + ", ti=" + x, msRefreshingObject);
		}
		return bPreviousValue;
	}

	/**
	 * @see OAThreadLocal#fastLoading
	 */
	public static Hub getFastLoadingHub() {
		return getFastLoadingHub(OAThreadLocalDelegate.getThreadLocal(false));
	}

	protected static Hub getFastLoadingHub(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.fastLoadingHub;
	}

	
	public static boolean isFastLoadingHub(Hub h) {
		if (h == null) return false;
		Hub hx = getFastLoadingHub();
		if (hx == null) return false;
		if (h == hx) return true;
		return HubShareDelegate.isUsingSameSharedHub(h, hx);
	}
	
	public static void setFastLoadingHub(Hub hub) {
		setFastLoadingHub(OAThreadLocalDelegate.getThreadLocal(true), hub);
	}

	protected static void setFastLoadingHub(OAThreadLocal ti, Hub hub) {
		if (ti == null) {
			return ;
		}
		if (ti.fastLoadingHub != null) {
			HubEventDelegate.fireOnNewListEvent(ti.fastLoadingHub, true);
		}
		ti.fastLoadingHub = hub;
	}


	/**
	 * @see OAThreadLocal#process
	 */
	public static OAProcess getProcess() {
		return getProcess(OAThreadLocalDelegate.getThreadLocal(false));
	}

	protected static OAProcess getProcess(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.process;
	}

	
	public static void setProcess(OAProcess process) {
		setProcess(OAThreadLocalDelegate.getThreadLocal(true), process);
	}

	protected static void setProcess(OAThreadLocal ti, OAProcess process) {
		if (ti == null) {
			return ;
		}
		ti.process = process;
	}

	
	
}
