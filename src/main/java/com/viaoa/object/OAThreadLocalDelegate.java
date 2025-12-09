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
 * Central controller for OA thread-local execution state.
 * 
 * <p>This delegate wraps a thread-local {@link OAThreadLocal} instance and
 * coordinates access to:
 *
 * <ul>
 *   <li>Object graph loading and refresh mode indicators</li>
 *   <li>Distributed sync and remote invocation state</li>
 *   <li>Cache add mode, serialization mode and message suppression</li>
 *   <li>Object delete state tracking</li>
 *   <li>Undoable property change capture</li>
 *   <li>Hub event traversal, dependency resolution & batching</li>
 *   <li>Deadlock-aware fine-grained locking coordination</li>
 *   <li>OAContext propagation</li>
 * </ul>
 *
 * <p>Provides high-performance fast paths (zero atomic ops) when counters
 * indicate that a feature is inactive. Many operations use reference-counting
 * and scoped toggling to ensure state is always restored.
 *
 * <p>All thread-affecting logic in OA must route through this class rather than
 * manipulating {@link OAThreadLocal} directly.
 *
 * @author vvia
 * @see OAThreadLocal
 */
public class OAThreadLocalDelegate {

	private static Logger LOG = Logger.getLogger(OAThreadLocalDelegate.class.getName());

	/**
	 * Core thread-local container storing the OAThreadLocal instance associated
	 * with the current thread. All OAThreadLocal access routes through this
	 * reference to ensure correct isolation of per-thread state.
	 */
	private static final ThreadLocal<OAThreadLocal> threadLocal = new ThreadLocal<OAThreadLocal>();

	/**
	 * Global counter used for diagnostics to track how many threads increment
	 * or decrement the OAThreadLocal.loading flag. Guides performance and
	 * bulk-loading analysis.
	 */
	private static final AtomicInteger TotalIsLoading = new AtomicInteger();

	/**
	 * Diagnostic counter tracking how often threads modify their cacheAddMode
	 * value inside OAThreadLocal. Used for monitoring caching behavior.
	 */
	private static final AtomicInteger TotalObjectCacheAddMode = new AtomicInteger();
	
	/**
	 * Counts the number of times objectSerializer references are assigned or
	 * cleared within OAThreadLocal instances across all threads.
	 */
	private static final AtomicInteger TotalObjectSerializer = new AtomicInteger();
	
	/**
	 * Tracks the number of increments/decrements applied to the
	 * suppressCSMessages counter across all threads. Useful for analyzing
	 * client/server message suppression patterns.
	 */
	private static final AtomicInteger TotalSuppressCSMessages = new AtomicInteger();
	
	/**
	 * Global counter used to measure how frequently deletion-related thread-local
	 * states are entered or exited. Assists in understanding cascading delete
	 * operations and suppression behavior.
	 */
	private static final AtomicInteger TotalDelete = new AtomicInteger();
	
	/**
	 * Diagnostic counter tracking how many thread-scoped transactions are
	 * created, activated, or completed during runtime.
	 */
	private static final AtomicInteger TotalTransaction = new AtomicInteger();
	
	/**
	 * Counts the number of times threads enable or disable capture of undoable
	 * property changes. Used to analyze undo/redo batching frequency.
	 */
	private static final AtomicInteger TotalCaptureUndoablePropertyChanges = new AtomicInteger();
	
	/**
	 * Tracks the number of increments to hubMergerChangingCount across threads.
	 * Helps profile how often HubMerger-related internal operations occur.
	 */
	private static final AtomicInteger TotalHubMergerChanging = new AtomicInteger();
	
	//    private static final AtomicInteger TotalGetDetailHub = new AtomicInteger();
	
	/**
	 * Global counter tracking how many sibling-helper structures are allocated
	 * or referenced through thread-local state.
	 */
	private static final AtomicInteger TotalSiblingHelper = new AtomicInteger();
	
	/**
	 * Diagnostic counter that tracks how many times thread-local state related to
	 * remote multiplexer client assignments is incremented or decremented.
	 * 
	 * <p>This value represents the total number of active or recently modified
	 * RemoteMultiplexerClient references stored within OAThreadLocal instances.
	 * It is used strictly for debugging and performance visibility, particularly
	 * around remote messaging infrastructure.</p>
	 */
	private static final AtomicInteger TotalRemoteMultiplexerClient = new AtomicInteger();
	
	/**
	 * Tracks how many thread-local instances currently hold a non-null notifyObject.
	 * Used to determine whether wake-up processing is required for remote-thread
	 * coordination.
	 */
	private static final AtomicInteger TotalNotifyWaitingObject = new AtomicInteger();

	/**
	 * Global diagnostic counter tracking the number of active HubListenerTree
	 * traversal operations across all threads.
	 */
	private static AtomicInteger TotalHubListenerTreeCount = new AtomicInteger();
	
	/**
	 * Counts how many HubEvent objects are being processed across all threads,
	 * supporting diagnostics of event dispatch volume and sequencing.
	 */
	private static final AtomicInteger TotalHubEvent = new AtomicInteger();

	/**
	 * Global map associating lock keys with the OAThreadLocal instances that
	 * currently hold read/write locks. Supports fine-grained locking and
	 * deadlock detection logic.
	 */
	public static final HashMap<Object, OAThreadLocal[]> hmLock = new HashMap<Object, OAThreadLocal[]>(53, .75f);

	/**
	 * Counts how many threads have disabled automatic active-object adjustment
	 * on Hubs. Used to optimize hub-position update behavior.
	 */
	private static final AtomicInteger TotalDontAdjustHub = new AtomicInteger();
	
	/**
	 * Diagnostic counter tracking usage of per-thread OAJson (Jackson) helpers.
	 * Useful for profiling JSON serialization operations.
	 */
	private static final AtomicInteger TotalJackson = new AtomicInteger();
	
	/**
	 * Counts threads currently performing Hub.refresh operations. Enables
	 * optimized dirty-mode querying during bulk refresh sequences.
	 */
	private static final AtomicInteger TotalIsRefreshing = new AtomicInteger();

	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread.
	 * Creates a new instance when none exists and creation is allowed.
	 *
	 * @param bCreateIfNull true to create a new thread-local instance if missing
	 * @return the thread-local instance, or null if none exists and creation is disabled
	 */
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
	/**
	 * Timestamp used to throttle logging output for transaction-related
	 * diagnostic messages. Prevents excessive log frequency for high-volume
	 * transaction updates.
	 */
	private static long msTransaction;

	/**
	 * Sets the current thread's transaction reference and updates the global
	 * transaction counter. Called internally by transaction-related classes.
	 *
	 * @param t the transaction to assign, or null to clear it
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

	/**
	 * Returns the current thread's active transaction, or null if no
	 * transaction is registered.
	 *
	 * @return the current transaction or null
	 */
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
	 * Returns whether the specified thread-local instance is in a loading state.
	 *
	 * @param ti the thread-local instance
	 * @return true if loading is greater than zero
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

	/**
	 * Returns whether the specified thread-local instance is in a loading state.
	 *
	 * @param ti the thread-local instance
	 * @return true if loading is greater than zero
	 */
	protected static boolean isLoading(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.loading > 0;
	}

	/**
	 * Updates the loading flag for the current thread and returns the previous
	 * loading state.
	 *
	 * @param b true to increase the loading count, false to decrease it
	 * @return previous loading flag before the update
	 */
	public static boolean setLoading(boolean b) {
		// LOG.finer(""+b);
		return setLoading(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	/**
	 * Timestamp used to throttle diagnostic logging for load-related operations.
	 * Helps prevent excessive log output when object-graph loading occurs
	 * frequently on a given thread.
	 */
	private static long msLoadingObject;

	/**
	 * Updates the loading count for the specified thread-local instance and the
	 * global loading counter. Logs throttled warnings when limits are exceeded.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment loading, false to decrement it
	 * @return previous loading state
	 */
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
	/**
	 * Returns the current thread's object-cache add mode, or the default mode
	 * when no add mode is active.
	 *
	 * @return the current add mode
	 */
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

	/**
	 * Sets the object-cache add mode for the current thread and updates the
	 * global counter when transitioning between active and default modes.
	 *
	 * @param mode the add mode to assign
	 */
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

	/**
	 * Returns the object-cache add mode for the specified thread-local
	 * instance, or the default if none is set.
	 *
	 * @param ti the thread-local instance
	 * @return the add mode
	 */
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
	/*
	 * used by Serialization for the current thread. OAObjectSerializeInterface is called to return the type of serialization to perform.
	 * SERIALIZE_STRIP_NONE or SERIALIZE_STRIP_REFERENCES
	 */
	/**
	 * Returns the current thread's object serializer, or null if serialization
	 * stripping is not active.
	 *
	 * @return the serializer or null
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

	/**
	 * Returns the object serializer assigned to the specified thread-local
	 * instance.
	 *
	 * @param ti the thread-local instance
	 * @return the serializer or null
	 */
	protected static OAObjectSerializer getObjectSerializer(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.objectSerializer;
	}

	/**
	 * Sets the object serializer for the current thread and updates global
	 * serializer counters.
	 *
	 * @param si the serializer to assign, or null to clear it
	 */
	public static void setObjectSerializer(OAObjectSerializer si) {
		// LOG.finer("OAObjectSerializer="+(si != null));
		setObjectSerializer(OAThreadLocalDelegate.getThreadLocal(si != null), si);
	}

	private static long msObjectSerializer;

	/**
	 * Assigns the serializer to the specified thread-local instance and updates
	 * the global serializer counter when transitioning to or from a null state.
	 *
	 * @param ti the thread-local instance
	 * @param si the serializer to assign
	 */
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
	/**
	 * Returns whether the specified thread-local instance has message
	 * suppression enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if suppressed, otherwise false
	 */
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

	/**
	 * Returns whether the specified thread-local instance has message
	 * suppression enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if suppressed, otherwise false
	 */
	public static boolean isSuppressCSMessages(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.suppressCSMessages > 0;
	}

	/**
	 * Enables or disables suppression of client/server messages for the
	 * current thread.
	 *
	 * @param b true to enable suppression, false to disable
	 */
	public static void setSuppressCSMessages(boolean b) {
		// LOG.finest(""+b);
		setSuppressCSMessages(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msSuppressCSMessages;

	/**
	 * Updates suppression counts for the specified thread-local instance and
	 * the global suppression counter. Logs throttled warnings when thresholds
	 * are exceeded.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment suppression, false to decrement
	 */
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
	 * Returns whether the current thread is in a deleting state.
	 *
	 * @return true if deleting is active for this thread
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
	 * Returns whether the current thread is deleting the specified object.
	 *
	 * @param obj the object to check
	 * @return true if this thread is deleting the object
	 */
	public static boolean isDeleting(Object obj) {
		if (obj == null) {
			return false;
		}
		return hmDeleting.contains(obj);
	}

	/**
	 * Returns whether the given thread-local instance is deleting the specified
	 * object.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object to check
	 * @return true if the instance is deleting the object
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

	/**
	 * Returns whether the given thread-local instance is deleting the specified
	 * object.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object to check
	 * @return true if the instance is deleting the object
	 */
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

	/**
	 * Adds or removes the specified object from the global deleting map and
	 * updates the deleting state for the current thread.
	 *
	 * @param obj the object to update
	 * @param b   true to mark as deleting, false to unmark
	 */
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

	/**
	 * Updates the deleting state for the specified thread-local instance and
	 * adjusts the global delete counter.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object being updated
	 * @param b   true to add, false to remove
	 */
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

	/*
	 * Flag used for generic/misc purposes
	 */
	/**
	 * Returns whether the specified flag object exists in the current
	 * thread-local flag list.
	 *
	 * @param obj the flag object
	 * @return true if present
	 */
	public static boolean isFlag(Object obj) {
		return isFlag(OAThreadLocalDelegate.getThreadLocal(false), obj);
	}

	/**
	 * Returns whether the specified flag object exists in the given
	 * thread-local instance.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the flag object
	 * @return true if present
	 */
	protected static boolean isFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return false;
		}
		return OAArray.contains(ti.flags, obj);
	}

	private static long msFlag;

	/**
	 * Adds the specified flag object to the current thread-local flag list.
	 *
	 * @param obj the flag object to add
	 */
	public static void setFlag(Object obj) {
		setFlag(OAThreadLocalDelegate.getThreadLocal(true), obj);
	}

	/**
	 * Adds the specified flag to the thread-local instance and logs warnings
	 * when the flag list grows beyond safe thresholds.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the flag object
	 */
	protected static void setFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.flags = OAArray.add(Object.class, ti.flags, obj);
		if (ti.flags != null && ti.flags.length > 20) {
			msFlag = throttleLOG("OAThreadLocal.tiFlags.length =" + ti.flags.length, msFlag);
		}
	}

	/**
	 * Removes the specified flag object from the current thread-local flag list.
	 *
	 * @param obj the flag to remove
	 */
	public static void removeFlag(Object obj) {
		setFlag(OAThreadLocalDelegate.getThreadLocal(false), obj);
	}

	/**
	 * Acquires a lock on the specified object with a maximum number of wait
	 * attempts before force-acquiring the lock.
	 *
	 * @param object        the object to lock
	 * @param maxWaitTries  maximum wait attempts before taking the lock
	 */
	protected static void removeFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.flags = OAArray.removeValue(Object.class, ti.flags, obj);
	}

	// 20110104
	// Locking -----------------------
	/*
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
	/**
	 * Acquires a lock on the specified object with a maximum number of wait
	 * attempts before force-acquiring the lock.
	 *
	 * @param object        the object to lock
	 * @param maxWaitTries  maximum wait attempts before taking the lock
	 */
	public static void lock(Object object, int maxWaitTries) {
		lock(OAThreadLocalDelegate.getThreadLocal(true), object, maxWaitTries);
	}

	/**
	 * Acquires a lock on the specified object using a default maximum wait
	 * threshold.
	 *
	 * @param object the object to lock
	 */
	public static void lock(Object object) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		lock(ti, object, 2);
	}

	/**
	 * Returns whether the current thread holds any locks.
	 *
	 * @return true if one or more locks are held
	 */
	public static boolean hasLock() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		return (ti != null && ti.locks != null && ti.locks.length > 0);
	}

	/**
	 * Returns whether the current thread holds a lock on the specified object.
	 *
	 * @param obj the object to check
	 * @return true if the lock is held
	 */
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

	/**
	 * Returns all lock objects currently held by the thread.
	 *
	 * @return an array of locked objects, or null if none
	 */
	public static Object[] getLocks() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.locks;
	}

	/**
	 * Returns whether any thread currently holds a lock on the specified object.
	 *
	 * @param object the object to check
	 * @return true if locked by any thread
	 */
	public static boolean isLocked(Object object) {
		synchronized (hmLock) {
			OAThreadLocal[] tis = hmLock.get(object); // threadLocals that are using object (locked or waiting)
			return (tis != null && tis.length > 0);
		}
	}

	/**
	 * Returns whether the current thread is the owner of the lock for the
	 * specified object.
	 *
	 * @param object the object to check
	 * @return true if the current thread owns the lock
	 */
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

	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread,
	 * creating one if necessary.
	 *
	 * @return the thread-local instance
	 */
	public static OAThreadLocal getOAThreadLocal() {
		return OAThreadLocalDelegate.getThreadLocal(true);
	}

	private static long timeLastStackTrace;
	private static int errorCnt;

	// used for lock/unlock
	protected static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	static volatile int openLockCnt;
	static volatile int lockCnt;
	static volatile int unlockCnt;

	/**
	 * Internal implementation for acquiring a lock using the provided
	 * thread-local instance. Handles waiting, deadlock detection, and
	 * thread notification logic.
	 *
	 * @param tiThis         the thread-local instance
	 * @param thisLockObject the object to lock
	 * @param maxWaitTries   maximum wait attempts before force-acquiring
	 */
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

	/**
	 * Attempts to acquire the lock for the specified object. Returns true if the
	 * lock can proceed immediately, or false if the caller must wait.
	 *
	 * @param tlThis         the thread-local instance
	 * @param thisLockObject the object to lock
	 * @param maxWaitTries   maximum wait attempts
	 * @param tries          current attempt count
	 * @return true if the lock is acquired or can proceed
	 */
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

	/**
	 * Returns the number of detected deadlocks encountered during lock
	 * acquisition.
	 *
	 * @return the deadlock count
	 */
	public static int getDeadlockCount() {
		return cntDeadlock;
	}

	// this should be called with rwLock.write locked
	/**
	 * Attempts to resolve a detected deadlock by reordering lock ownership for
	 * the thread-local instances associated with the specified lock object.
	 *
	 * @param tiThis     the thread-local instance requesting the lock
	 * @param lockObject the object involved in the deadlock
	 */
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
				tlOwner.notifyAll();
			}

			LOG.warning("LOCK:OAThreadLocalDelegate: Found Deadlock, obj=" + lockObject + ", releasing one of the locks");
			break;
		}
	}

	/**
	 * Releases all locks currently held by the thread-local instance for the
	 * current thread.
	 */
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

	/**
	 * Releases the lock held by the current thread on the specified object.
	 *
	 * @param object the object to unlock
	 */
	public static void unlock(Object object) {
		unlock(OAThreadLocalDelegate.getThreadLocal(true), object);
	}

	/**
	 * Internal implementation for releasing a lock using the specified
	 * thread-local instance.
	 *
	 * @param ti     the thread-local instance
	 * @param object the object to unlock
	 */
	protected static void unlock(OAThreadLocal ti, Object object) {
		//System.out.println((++unlockCnt)+") ****** OAThreadLocalDelegate.unlock obj="+object+", activeLocks="+(--openLockCnt));
		try {
			rwLock.writeLock().lock();
			_unlock(ti, object);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the specified lock from the thread-local instance and updates the
	 * global lock-tracking structure.
	 *
	 * @param tl     the thread-local instance
	 * @param object the object being unlocked
	 */
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
					tls[0].notifyAll();
				}
			}
		}
		tl.locks = OAArray.removeAt(Object.class, tl.locks, pos); // must be inside sync
	}

	// HubListenerTree uses this to ignore dependent property changes caused by add/remove objects from hubMerger.hubMaster
	/**
	 * Returns whether any thread is currently modifying hub-merger state.
	 *
	 * @return true if hub-merger updates are active
	 */
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

	/**
	 * Returns whether the specified thread-local instance is modifying
	 * hub-merger state.
	 *
	 * @param ti the thread-local instance
	 * @return true if active
	 */
	protected static boolean isHubMergerChanging(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.hubMergerChangingCount > 0;
	}

	/**
	 * Enables or disables hub-merger change tracking for the current thread.
	 *
	 * @param b true to enable, false to disable
	 */
	public static void setHubMergerChanging(boolean b) {
		// LOG.finer(""+b);
		setHubMergerChanging(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msHubMergerChanging;

	/**
	 * Updates hub-merger change counts for the specified thread-local instance
	 * and executes any pending callbacks when the change count reaches zero.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
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
			
			if (ti.hubMergerChangingCount == 0) {
		        if (ti.hubMergerCallback != null) {
		            for (OAThreadLocalHubMergerCallback cb : ti.hubMergerCallback) {
		                cb.callback();
		            }
		            ti.hubMergerCallback = null;
		        }
			}
		}
		if (x > 200 || x < 0) {
			msHubMergerChanging = throttleLOG("TotalHubMergerChanging=" + x, msHubMergerChanging);
		}
	}

	/**
	 * Registers a callback to be executed once hub-merger changes finish for
	 * the current thread.
	 *
	 * @param cb the callback to register
	 */
    public static void addHubMergerCallback(OAThreadLocalHubMergerCallback cb) {
        if (cb == null) return;
        addHubMergerCallback(OAThreadLocalDelegate.getThreadLocal(true), cb);
    }
	
    /**
     * Adds a hub-merger callback to the specified thread-local instance, or
     * executes it immediately if no hub-merger changes are pending.
     *
     * @param ti the thread-local instance
     * @param cb the callback to register
     */
    protected static void addHubMergerCallback(OAThreadLocal ti, OAThreadLocalHubMergerCallback cb) {
        if (ti == null) return;
        if (cb == null) return;
        
        if (ti.hubMergerChangingCount == 0) {
            cb.callback();
            return;
        }
        ti.hubMergerCallback = (OAThreadLocalHubMergerCallback[]) OAArray.add(OAThreadLocalHubMergerCallback.class, ti.hubMergerCallback, cb);
    }
	
	
	
	// UndoablePropertyChanges -----------------------

    /**
     * Enables or disables recording of undoable property changes for the
     * current thread.
     *
     * @param b true to enable, false to disable
     */
	public static void setCreateUndoablePropertyChanges(boolean b) {
		// LOG.finer(""+b);
		setCreateUndoablePropertyChanges(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msCreateUndoablePropertyChanges;

	/**
	 * Updates undoable-change tracking for the specified thread-local instance
	 * and updates the global counter.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to enable, false to disable
	 */
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

	/**
	 * Returns whether undoable property change recording is enabled for the
	 * current thread.
	 *
	 * @return true if active
	 */
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

	/**
	 * Returns whether undoable property change recording is enabled for the
	 * specified thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @return true if enabled
	 */
	protected static boolean getCreateUndoablePropertyChanges(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.createUndoablePropertyChanges;
	}

	/**
	 * Begins a compound undoable sequence with the specified name for the
	 * current thread.
	 *
	 * @param compoundName the descriptive name for the compound edit
	 */
	public static void startUndoable(String compoundName) {
		startUndoable(OAThreadLocalDelegate.getThreadLocal(true), compoundName);
	}

	/**
	 * Convenience wrapper for starting a compound undoable sequence using the
	 * specified name.
	 *
	 * @param compoundName the compound edit name
	 */
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

	/**
	 * Internal implementation to start tracking a compound undoable change
	 * using the specified thread-local instance.
	 *
	 * @param ti           the thread-local instance
	 * @param compoundName the compound edit name
	 */
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

	/**
	 * Completes the current compound undoable sequence for the thread.
	 */
	public static void endUndoable() {
		endUndoable(OAThreadLocalDelegate.getThreadLocal(true));
	}

	/**
	 * Convenience wrapper that ends the current compound undoable sequence
	 * for the thread.
	 */
	public static void endCompoundUndoable() {
		endUndoable(OAThreadLocalDelegate.getThreadLocal(true));
	}

	/**
	 * Internal implementation for completing a compound undoable sequence for
	 * the specified thread-local instance. Resets undoable flags and updates
	 * global counters.
	 *
	 * @param ti the thread-local instance
	 */
	protected static void endUndoable(OAThreadLocal ti) {
		if (ti == null) {
			return;
		}
		ti.createUndoablePropertyChanges = false;
		ti.compoundUndoableName = null;
		OAUndoManager.endCompoundEdit();

		OAThreadLocalDelegate.TotalCaptureUndoablePropertyChanges.decrementAndGet();
	}

	/**
	 * Registers the specified sibling helper for the current thread.
	 *
	 * @param sh the sibling helper
	 * @return true if added, false if already present
	 */
	public static boolean addSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return false;
		}
		return addSiblingHelper(OAThreadLocalDelegate.getThreadLocal(true), sh);
	}

	/**
	 * Removes the specified sibling helper from the current thread's list.
	 *
	 * @param sh the sibling helper to remove
	 */
	public static void removeSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return;
		}
		if (TotalSiblingHelper.get() == 0) {
			return;
		}
		removeSiblingHelper(OAThreadLocalDelegate.getThreadLocal(true), sh);
	}

	/**
	 * Returns the list of sibling helpers associated with the current thread,
	 * or null if none exist.
	 *
	 * @return the list of sibling helpers, or null
	 */
	public static ArrayList<OASiblingHelper> getSiblingHelpers() {
		if (TotalSiblingHelper.get() == 0) {
			return null;
		}
		return getSiblingHelpers(OAThreadLocalDelegate.getThreadLocal(true));
	}

	/**
	 * Returns the sibling helpers stored in the specified thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @return the list of sibling helpers, or null
	 */
	public static ArrayList<OASiblingHelper> getSiblingHelpers(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.alSiblingHelper;
	}

	/**
	 * Returns whether the current thread has any registered sibling helpers.
	 *
	 * @return true if one or more sibling helpers exist
	 */
	public static boolean hasSiblingHelpers() {
		if (TotalSiblingHelper.get() == 0) {
			return false;
		}
		ArrayList<OASiblingHelper> al = getSiblingHelpers(OAThreadLocalDelegate.getThreadLocal(true));
		return (al != null && al.size() > 0);
	}

	/**
	 * Removes all sibling helpers from the current thread.
	 */
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

	/**
	 * Internal implementation to register a sibling helper for the specified
	 * thread-local instance and update global counters.
	 *
	 * @param ti the thread-local instance
	 * @param sh the sibling helper to add
	 * @return true if the helper was added
	 */
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

	/**
	 * Internal implementation that removes the specified sibling helper from
	 * the thread-local instance and updates global counters.
	 *
	 * @param ti the thread-local instance
	 * @param sh the sibling helper to remove
	 */
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

	/**
	 * Logs a throttled warning message when sufficient time has passed since
	 * the previous log for the same category.
	 *
	 * @param msg    the message to log
	 * @param msLast the timestamp of the previous logged message
	 * @return the updated timestamp
	 */
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

	/**
	 * Returns a formatted dump of all stack traces for all threads.
	 *
	 * @return the formatted stack trace dump
	 */
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

	/**
	 * Returns the current thread’s stack trace formatted as a string.
	 *
	 * @return the stack trace for the current thread
	 */
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

	/**
	 * Sets the status message for the current thread-local instance.
	 *
	 * @param msg the status message
	 */
	public static void setStatus(String msg) {
		getOAThreadLocal().status = msg;
	}

	/**
	 * Assigns the remote request information for the current thread.
	 *
	 * @param ri the request information
	 */
	public static void setRemoteRequestInfo(RequestInfo ri) {
		getOAThreadLocal().requestInfo = ri;
	}

	/**
	 * Returns the remote request information for the current thread.
	 *
	 * @return the RequestInfo instance, or null if none exists
	 */
	public static RequestInfo getRemoteRequestInfo() {
		return getOAThreadLocal().requestInfo;
	}

	/**
	 * Enables or disables sending of messages from OARemoteThread instances.
	 *
	 * @param b true to enable sending, false to disable
	 * @return the previous send-messages state
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

	/**
	 * Assigns or clears an object used to notify the current thread when
	 * remote-thread operations need to resume.
	 *
	 * @param obj the object to assign, or null to clear
	 */
	public static void setNotifyObject(Object obj) {
		if (obj == null) {
			if (OAThreadLocalDelegate.TotalNotifyWaitingObject.get() == 0) {
				return;
			}
			OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(false);
			if (tl != null && (tl.notifyObject != null)) {
				TotalNotifyWaitingObject.decrementAndGet();
				tl.notifyObject = null;
			}
		} else {
			OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(true);
			if (tl.notifyObject == null) {
				TotalNotifyWaitingObject.incrementAndGet();
			}
			tl.notifyObject = obj;
		}
	}

	/**
	 * Notifies the thread waiting on the thread-local notify object, if one
	 * exists, and clears the notify reference.
	 */
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

	/**
	 * Returns the recursive trigger count for the current thread.
	 *
	 * @return the trigger count
	 */
	public static int getRecursiveTriggerCount() {
		int x = getRecursiveTriggerCount(getThreadLocal(false));
		return x;
	}

	/**
	 * Returns the recursive trigger count stored in the supplied thread-local
	 * instance. When the instance is null, zero is returned.
	 *
	 * @param ti the thread-local instance to query
	 * @return the recursive trigger count or zero if the instance is null
	 */
	protected static int getRecursiveTriggerCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.recursiveTriggerCount;
	}

	/**
	 * Sets the recursive trigger count for the current thread-local instance.
	 *
	 * @param x the trigger count to assign
	 */
	public static void setRecursiveTriggerCount(int x) {
		setRecursiveTriggerCount(OAThreadLocalDelegate.getThreadLocal(true), x);
	}

	/**
	 * Updates the recursive trigger count on the supplied thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @param x  the trigger count to assign
	 */
	protected static void setRecursiveTriggerCount(OAThreadLocal ti, int x) {
		if (ti == null) {
			return;
		}
		ti.recursiveTriggerCount = x;
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	public static int getHubListenerTreeCount() {
		int x;
		if (OAThreadLocalDelegate.TotalHubListenerTreeCount.get() == 0) {
			x = 0;
		} else {
			x = getHubListenerTreeCount(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return x;
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	protected static int getHubListenerTreeCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.hubListenerTreeCount;
	}

	/**
	 * Increments or decrements the hub-listener tree depth for the current thread.
	 *
	 * @param b true to increment, false to decrement
	 */
	public static void setHubListenerTree(boolean b) {
		setHubListenerTree(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msHubListenerTree;

	/**
	 * Adjusts the hub-listener tree depth on the supplied thread-local instance
	 * and updates global counters.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
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

	/**
	 * Sets the property name to ignore during tree-listener processing for the
	 * current thread.
	 *
	 * @param prop the property name to ignore
	 */
	public static void setIgnoreTreeListenerProperty(String prop) {
		getThreadLocal(true).ignoreTreeListenerProperty = prop;
	}

	/**
	 * Returns the property name currently ignored by tree listeners for the
	 * thread.
	 *
	 * @return the ignored property name
	 */
	public static String getIgnoreTreeListenerProperty() {
		return getThreadLocal(true).ignoreTreeListenerProperty;
	}

	/**
	 * Returns the number of OA sync events recorded for the current thread.
	 *
	 * @return the sync event count
	 */
	public static int getOASyncEventCount() {
		return getThreadLocal(true).oaSyncEventCount;
	}

	/**
	 * Increments the OA sync event count for the current thread.
	 */
	public static void incrOASyncEventCount() {
		getThreadLocal(true).oaSyncEventCount++;
	}

	// HubEvent  ---------------
	private static long msHubEvent;

	/**
	 * Returns the most recent HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the latest HubEvent or null
	 */
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

	/**
	 * Returns whether the supplied HubEvent is currently active for the thread.
	 *
	 * @param he the HubEvent to check
	 * @return true if the event is active
	 */
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

	/**
	 * Returns the oldest HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the earliest HubEvent or null
	 */
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

	/**
	 * Adds the supplied HubEvent to the current thread’s active-event list.
	 *
	 * @param he the HubEvent to add
	 */
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

	/**
	 * Removes the supplied HubEvent from the current thread’s active-event list
	 * and updates global counters.
	 *
	 * @param he the HubEvent to remove
	 */
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

	/**
	 * Returns whether the current thread is in the process of sending one or more
	 * HubEvents.
	 *
	 * @return true if event sending is active
	 */
	public static boolean isSendingEvent() {
		boolean b;
		if (OAThreadLocalDelegate.TotalHubEvent.get() == 0) {
			b = false;
		} else {
			b = isSendingEvent(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return b;
	}

	/**
	 * Returns whether the supplied thread-local instance is currently sending
	 * HubEvents.
	 *
	 * @param ti the thread-local instance
	 * @return true if sending events
	 */
	protected static boolean isSendingEvent(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.alHubEvent != null && ti.alHubEvent.size() > 0;
	}

	/**
	 * Returns whether a calc-property change for the supplied hub, object, and
	 * property name has already been recorded during the current event cycle.
	 *
	 * @param thisHub      the hub involved
	 * @param thisObj      the object involved
	 * @param propertyName the property name
	 * @return true if the event has been previously recorded
	 */
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
	 * Returns the context object associated with the current thread.
	 *
	 * @return the context object
	 */
	public static Object getContext() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return ti.context;
	}

	/**
	 * Assigns the context object for the current thread.
	 *
	 * @param context the context to assign
	 */
	public static void setContext(Object context) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		ti.context = context;
	}

	/**
	 * Sets the admin flag for the current thread-local instance.
	 *
	 * @param b true to enable admin mode, false to disable
	 * @return the previous admin flag value
	 */
	public static boolean setAdmin(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return setIsAdmin(ti, b);
	}

	/**
	 * Convenience wrapper that sets the admin flag for the current thread-local
	 * instance.
	 *
	 * @param b true to enable admin mode
	 * @return the previous admin flag value
	 */
	public static boolean setIsAdmin(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return setIsAdmin(ti, b);
	}

	/**
	 * Updates the admin flag on the supplied thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @param b  the new admin flag value
	 * @return the previous admin flag value
	 */
	public static boolean setIsAdmin(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		boolean b2 = ti.isAdmin;
		ti.isAdmin = b;
		return b2;
	}

	/**
	 * Returns whether the current thread-local instance is in admin mode.
	 *
	 * @return true if admin mode is enabled
	 */
	public static boolean isAdmin() {
		return getIsAdmin();
	}

	/**
	 * Returns whether the current thread-local instance is in admin mode.
	 *
	 * @return true if admin mode is enabled
	 */
	public static boolean getIsAdmin() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(true);
		return getIsAdmin(ti);
	}

	/**
	 * Returns whether the supplied thread-local instance has admin mode enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if admin mode is enabled
	 */
	public static boolean getIsAdmin(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.isAdmin;
	}

	/**
	 * Copies selected state fields from the supplied thread-local instance into
	 * the current thread-local instance.
	 *
	 * @param tl the thread-local instance to copy from
	 */
	public static void initialize(OAThreadLocal tl) {
		if (tl == null) {
			return;
		}
		OAThreadLocal tlx = getThreadLocal(true);
		tlx.isAdmin = tl.isAdmin;
		tlx.context = tl.context;
	}

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
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

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
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


	/**
	 * Registers the supplied hub as one that should not be auto-adjusted for the
	 * current thread.
	 *
	 * @param hub the hub to register
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

	/**
	 * Removes the supplied hub from the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to remove
	 */
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

	/**
	 * Returns whether the supplied hub is eligible for auto-adjustment based on
	 * the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to check
	 * @return true if auto-adjustment is allowed
	 */
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

	/**
	 * Returns whether the current thread-local instance is marked as a sync
	 * thread.
	 *
	 * @return true if sync-thread mode is enabled
	 */
	public static boolean isSyncThread() {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(false);
		return (ti != null && ti.bIsSyncThread);
	}

	/**
	 * Enables or disables sync-thread mode for the current thread-local
	 * instance.
	 *
	 * @param b true to enable, false to disable
	 * @return the previous sync-thread flag value
	 */
	public static boolean setSyncThread(boolean b) {
		OAThreadLocal ti = OAThreadLocalDelegate.getThreadLocal(b);

		if (ti == null) {
			return false;
		}
		boolean b2 = ti.bIsSyncThread;
		ti.bIsSyncThread = b;
		return b2;
	}

	/**
	 * Returns whether the current thread is actively performing a refresh
	 * operation.
	 *
	 * @return true if refreshing
	 */
	public static boolean isRefreshing() {
		boolean b;
		if (OAThreadLocalDelegate.TotalIsRefreshing.get() == 0) {
			b = false;
		} else {
			b = isRefreshing(OAThreadLocalDelegate.getThreadLocal(false));
		}
		return b;
	}

	/**
	 * Returns whether the supplied thread-local instance is actively performing a
	 * refresh operation.
	 *
	 * @param ti the thread-local instance
	 * @return true if refreshing
	 */
	protected static boolean isRefreshing(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.refreshing > 0;
	}

	/**
	 * Enables or disables refresh mode for the current thread-local instance.
	 *
	 * @param b true to increment, false to decrement
	 */
	public static void setRefreshing(boolean b) {
		// LOG.finer(""+b);
		setRefreshing(OAThreadLocalDelegate.getThreadLocal(b), b);
	}

	private static long msRefreshingObject;

	/**
	 * Adjusts the refresh counter on the supplied thread-local instance and
	 * updates the global refresh counter.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
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
	 * Returns the hub used for fast-loading operations for the current thread,
	 * or null if none is assigned.
	 *
	 * @return the fast-loading hub or null
	 */
	public static Hub getFastLoadingHub() {
		return getFastLoadingHub(OAThreadLocalDelegate.getThreadLocal(false));
	}

	/**
	 * Returns the fast-loading hub stored in the supplied thread-local instance,
	 * or null if none is set.
	 *
	 * @param ti the thread-local instance
	 * @return the fast-loading hub or null
	 */
	protected static Hub getFastLoadingHub(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.fastLoadingHub;
	}

	
	/**
	 * Convenience check that returns true when the hub has fast-loading enabled.
	 * This is determined by consulting {@link Hub#getFastLoading()} on the
	 * supplied hub instance.
	 *
	 * @param h the hub to inspect
	 * @return true if the hub reports fast-loading enabled, otherwise false
	 */
	public static boolean isFastLoadingHub(Hub h) {
		if (h == null) return false;
		Hub hx = getFastLoadingHub();
		if (hx == null) return false;
		if (h == hx) return true;
		return HubShareDelegate.isUsingSameSharedHub(h, hx);
	}
	
	/**
	 * Assigns or clears the fast-loading hub for the current thread.
	 *
	 * @param hub the hub to assign or null to clear
	 */
	public static void setFastLoadingHub(Hub hub) {
		setFastLoadingHub(OAThreadLocalDelegate.getThreadLocal(true), hub);
	}

	/**
	 * Assigns the specified hub as the fast-loading hub for the given thread-local
	 * instance. If a previous fast-loading hub exists, its list-refresh event is
	 * triggered before replacing it.
	 *
	 * @param ti  the thread-local instance to update
	 * @param hub the hub to mark as fast-loading
	 */
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
	 * Returns the process assigned to the current thread-local instance.
	 *
	 * @return the process for the current thread, or null if none is set
	 */
	public static OAProcess getProcess() {
		return getProcess(OAThreadLocalDelegate.getThreadLocal(false));
	}

	/**
	 * Returns the process assigned to the specified thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @return the associated process, or null if none is set
	 */
	protected static OAProcess getProcess(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.process;
	}

	/**
	 * Assigns the specified process to the current thread-local instance.
	 *
	 * @param process the process to assign
	 */
	public static void setProcess(OAProcess process) {
		setProcess(OAThreadLocalDelegate.getThreadLocal(true), process);
	}

	/**
	 * Sets the process for the given thread-local instance.
	 *
	 * @param ti      the thread-local instance
	 * @param process the process to assign
	 */
	protected static void setProcess(OAThreadLocal ti, OAProcess process) {
		if (ti == null) {
			return ;
		}
		ti.process = process;
	}

	
}
