package com.viaoa.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.object.OAThreadLocal;
import com.viaoa.object.OAThreadLocalHubMergerCallback;
import com.viaoa.process.OAProcess;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.transaction.OATransaction;
import com.viaoa.undo.OAUndoManager;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;
import com.viaoa.util.Tuple3;


/**
 * Central service for OA thread-local execution state.
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
 * @see OAThreadLocal
 */
public class OAThreadLocalService {
	private static Logger LOG = Logger.getLogger(OAThreadLocalService.class.getName());

	private final OARuntime runtime;

	/**
	 * Global counter used for diagnostics to track how many threads increment
	 * or decrement the OAThreadLocal.loading flag. Guides performance and
	 * bulk-loading analysis.
	 */
	private final AtomicInteger aiTotalIsLoading = new AtomicInteger();

	/**
	 * Diagnostic counter tracking how often threads modify their cacheAddMode
	 * value inside OAThreadLocal. Used for monitoring caching behavior.
	 */
	private final AtomicInteger aiTotalObjectCacheAddMode = new AtomicInteger();
	
	/**
	 * Counts the number of times objectSerializer references are assigned or
	 * cleared within OAThreadLocal instances across all threads.
	 */
	private final AtomicInteger aiTotalObjectSerializer = new AtomicInteger();
	
	/**
	 * Tracks the number of increments/decrements applied to the
	 * suppressCSMessages counter across all threads. Useful for analyzing
	 * client/server message suppression patterns.
	 */
	private final AtomicInteger aiTotalSuppressCSMessages = new AtomicInteger();
	
	/**
	 * Global counter used to measure how frequently deletion-related thread-local
	 * states are entered or exited. Assists in understanding cascading delete
	 * operations and suppression behavior.
	 */
	private final AtomicInteger aiTotalDelete = new AtomicInteger();
	
	/**
	 * Diagnostic counter tracking how many thread-scoped transactions are
	 * created, activated, or completed during runtime.
	 */
	private final AtomicInteger aiTotalTransaction = new AtomicInteger();
	
	/**
	 * Counts the number of times threads enable or disable capture of undoable
	 * property changes. Used to analyze undo/redo batching frequency.
	 */
	private final AtomicInteger aiTotalCaptureUndoablePropertyChanges = new AtomicInteger();
	
	/**
	 * Tracks the number of increments to hubMergerChangingCount across threads.
	 * Helps profile how often HubMerger-related internal operations occur.
	 */
	private final AtomicInteger aiTotalHubMergerChanging = new AtomicInteger();
	
	//    private final AtomicInteger aiTotalGetDetailHub = new AtomicInteger();
	
	/**
	 * Global counter tracking how many sibling-helper structures are allocated
	 * or referenced through thread-local state.
	 */
	private final AtomicInteger aiTotalSiblingHelper = new AtomicInteger();
	
	/**
	 * Diagnostic counter that tracks how many times thread-local state related to
	 * remote multiplexer client assignments is incremented or decremented.
	 * 
	 * <p>This value represents the total number of active or recently modified
	 * RemoteMultiplexerClient references stored within OAThreadLocal instances.
	 * It is used strictly for debugging and performance visibility, particularly
	 * around remote messaging infrastructure.</p>
	 */
	private final AtomicInteger aiTotalRemoteMultiplexerClient = new AtomicInteger();
	
	/**
	 * Tracks how many thread-local instances currently hold a non-null notifyObject.
	 * Used to determine whether wake-up processing is required for remote-thread
	 * coordination.
	 */
	private final AtomicInteger aiTotalNotifyWaitingObject = new AtomicInteger();

	/**
	 * Global diagnostic counter tracking the number of active HubListenerTree
	 * traversal operations across all threads.
	 */
	private final AtomicInteger aiTotalHubListenerTreeCount = new AtomicInteger();
	
	/**
	 * Counts how many HubEvent objects are being processed across all threads,
	 * supporting diagnostics of event dispatch volume and sequencing.
	 */
	private final AtomicInteger aiTotalHubEvent = new AtomicInteger();

	/**
	 * Global map associating lock keys with the OAThreadLocal instances that
	 * currently hold read/write locks. Supports fine-grained locking and
	 * deadlock detection logic.
	 */
	public final Map<Object, OAThreadLocal[]> hmLock = new HashMap<Object, OAThreadLocal[]>(53, .75f);

	/**
	 * Counts how many threads have disabled automatic active-object adjustment
	 * on Hubs. Used to optimize hub-position update behavior.
	 */
	private final AtomicInteger aiTotalDontAdjustHub = new AtomicInteger();
	
	/**
	 * Diagnostic counter tracking usage of per-thread OAJson (Jackson) helpers.
	 * Useful for profiling JSON serialization operations.
	 */
	private final AtomicInteger aiTotalJackson = new AtomicInteger();
	
	/**
	 * Counts threads currently performing Hub.refresh operations. Enables
	 * optimized dirty-mode querying during bulk refresh sequences.
	 */
	private final AtomicInteger aiTotalIsRefreshing = new AtomicInteger();
	
	/**
	 * Timestamp used to throttle logging output for transaction-related
	 * diagnostic messages. Prevents excessive log frequency for high-volume
	 * transaction updates.
	 */
	private long msTransaction;
	

	/**
	 * Timestamp used to throttle diagnostic logging for load-related operations.
	 * Helps prevent excessive log output when object-graph loading occurs
	 * frequently on a given thread.
	 */
	private long msLoadingObject;
	
	
	private long msObjectCacheAddMode;
	private long msObjectSerializer;
	private long msSuppressCSMessages;
	private final ConcurrentHashMap<Object, AtomicInteger> hmDeleting = new ConcurrentHashMap<>();
	private long msDeleting;
	private long msFlag;
	private long timeLastStackTrace;
	private int errorCnt;
	// used for lock/unlock
	protected final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected volatile int openLockCnt;
	protected volatile int lockCnt;
	protected volatile int unlockCnt;
	protected int cntDeadlock;
	private long msHubMergerChanging;
	private long msCreateUndoablePropertyChanges;
	private long msUndoable;
	private long msSiblingHelper;
	private long msThrottleStackTrace;
	private long msHubListenerTree;
	private long msHubEvent;
	private long msRefreshingObject;
	
	
	/**
	 * Core thread-local container storing the OAThreadLocal instance associated
	 * with the current thread. All OAThreadLocal access routes through this
	 * reference to ensure correct isolation of per-thread state.
	 */
	private final ThreadLocal<OAThreadLocal> threadLocal = new ThreadLocal<OAThreadLocal>();
	
	
	OAThreadLocalService(OARuntime runtime) {
		this.runtime = runtime;
	}

	public OAThreadLocal getThreadLocal() {
		return getThreadLocal(true);
	}
	
	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread.
	 * Creates a new instance when none exists and creation is allowed.
	 *
	 * @param bCreateIfNull true to create a new thread-local instance if missing
	 * @return the thread-local instance, or null if none exists and creation is disabled
	 */
	public OAThreadLocal getThreadLocal(boolean bCreateIfNull) {
		OAThreadLocal ti = threadLocal.get();
		if (ti == null && bCreateIfNull) {
			ti = new OAThreadLocal();
			ti.setTime(System.currentTimeMillis());
			threadLocal.set(ti);
		}
		return ti;
	}
	
	public void clear() {
		threadLocal.remove();
	}

	
	/**
	 * Sets the current thread's transaction reference and updates the global
	 * transaction counter. Called internally by transaction-related classes.
	 *
	 * @param t the transaction to assign, or null to clear it
	 */
	public void setTransaction(OATransaction t) {
		OAThreadLocal ti = getThreadLocal(t != null);
		if (ti == null) return;
		ti.setTransaction(t);
		int x;
		if (t != null) {
			x = aiTotalTransaction.incrementAndGet();
		} else {
			x = aiTotalTransaction.decrementAndGet();
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
	public OATransaction getTransaction() {
		if (aiTotalTransaction.get() == 0) {
			return null;
		}
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.getTransaction();
	}
	
	/**
	 * Returns whether the specified thread-local instance is in a loading state.
	 *
	 * @param ti the thread-local instance
	 * @return true if loading is greater than zero
	 */
	public boolean isLoading() {
		boolean b;
		if (aiTotalIsLoading.get() == 0) {
			b = false;
		} else {
			b = isLoading(getThreadLocal(false));
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
	protected boolean isLoading(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.getLoading() > 0;
	}
	
	/**
	 * Updates the loading flag for the current thread and returns the previous
	 * loading state.
	 *
	 * @param b true to increase the loading count, false to decrease it
	 * @return previous loading flag before the update
	 */
	public boolean setLoading(boolean b) {
		// LOG.finer(""+b);
		return setLoading(getThreadLocal(b), b);
	}
	
	/**
	 * Updates the loading count for the specified thread-local instance and the
	 * global loading counter. Logs throttled warnings when limits are exceeded.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment loading, false to decrement it
	 * @return previous loading state
	 */
	protected boolean setLoading(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		int x, x2;
		boolean bPreviousValue;
		x = ti.getLoading();
		bPreviousValue = (x > 0);

		if (b) {
			x++;
			x2 = aiTotalIsLoading.getAndIncrement();
		} else {
			x--;
			x2 = aiTotalIsLoading.decrementAndGet();
		}
		ti.setLoading(x);
		if (x > 50 || x < 0 || x2 > 50 || x2 < 0) {
			msLoadingObject = throttleLOG("TotalIsLoading=" + x2 + ", ti=" + x, msLoadingObject);
		}
		return bPreviousValue;
	}
	
	/**
	 * Returns the current thread's object-cache add mode, or the default mode
	 * when no add mode is active.
	 *
	 * @return the current add mode
	 */
	public int getObjectCacheAddMode() {
		int mode;
		if (aiTotalObjectCacheAddMode.get() == 0) {
			mode = getObjectCacheAddMode(null);
		} else {
			mode = getObjectCacheAddMode(getThreadLocal(false));
		}
		return mode;
	}
	
	/**
	 * Returns the object-cache add mode for the specified thread-local
	 * instance, or the default if none is set.
	 *
	 * @param ti the thread-local instance
	 * @return the add mode
	 */
	protected int getObjectCacheAddMode(OAThreadLocal ti) {
		if (ti == null) {
			return 0;  // not set in threadLocal, check in OAObjectCacheService
		}
		int x = ti.getCacheAddMode();
		if (x <= 0) {
			return 0;  // not set in threadLocal, check in OAObjectCacheService
		}
		return x;
	}
	
	/**
	 * Sets the object-cache add mode for the current thread and updates the
	 * global counter when transitioning between active and default modes.
	 *
	 * @param mode the add mode to assign
	 */
	public void setObjectCacheAddMode(int mode) {
		// LOG.finer("mode="+mode);
		
		if (mode < 0) mode = 0;
		OAThreadLocal ti = getThreadLocal(mode != 0);
		if (ti == null) {
			return;
		}

		int old = ti.getCacheAddMode();
		if (old == mode) {
			return; // no change
		}
		ti.setCacheAddMode(mode);

		if (old == 0 || mode == 0) { // dont update total if it has already been called for this ti
			if (mode == 0) {
				if (aiTotalObjectCacheAddMode.get() > 0) {
					int x = aiTotalObjectCacheAddMode.decrementAndGet();
					if (x < 0) {
						msObjectCacheAddMode = throttleLOG("TotalObjectCacheAddMode =" + x, msObjectCacheAddMode);
					}
				}
			} else {
				int x = aiTotalObjectCacheAddMode.incrementAndGet();
				if (x > 15) {
					msObjectCacheAddMode = throttleLOG("TotalObjectCacheAddMode =" + x, msObjectCacheAddMode);
				}
			}
		}
	}
	
	
	/**
	 * Returns the current thread's object serializer, or null if serialization
	 * stripping is not active.
	 *
	 * @return the serializer or null
	 */
	public OAObjectSerializer getObjectSerializer() {
		OAObjectSerializer si;
		if (aiTotalObjectSerializer.get() == 0) {
			si = null;
		} else {
			si = getObjectSerializer(getThreadLocal(false));
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
	protected OAObjectSerializer getObjectSerializer(OAThreadLocal ti) {
		if (ti == null) {
			return null;
		}
		return ti.getObjectSerializer();
	}
	
	/**
	 * Sets the object serializer for the current thread and updates global
	 * serializer counters.
	 *
	 * @param si the serializer to assign, or null to clear it
	 */
	public void setObjectSerializer(OAObjectSerializer si) {
		// LOG.finer("OAObjectSerializer="+(si != null));
		setObjectSerializer(getThreadLocal(si != null), si);
	}
	
	/**
	 * Assigns the serializer to the specified thread-local instance and updates
	 * the global serializer counter when transitioning to or from a null state.
	 *
	 * @param ti the thread-local instance
	 * @param si the serializer to assign
	 */
	protected void setObjectSerializer(OAThreadLocal ti, OAObjectSerializer si) {
		if (ti == null) {
			return;
		}
		if (ti.getObjectSerializer() == si) {
			return;
		}
		OAObjectSerializer old = ti.getObjectSerializer();
		if (si == old) {
			return; // no change
		}
		ti.setObjectSerializer(si);

		if (old == null || si == null) { // dont update total if it has already been called for this ti
			int x;
			if (si != null) {
				x = aiTotalObjectSerializer.incrementAndGet();
			} else {
				x = aiTotalObjectSerializer.decrementAndGet();
			}
			if (x > 25 || x < 0) {
				msObjectSerializer = throttleLOG("TotalObjectSerializeInterface =" + x, msObjectSerializer);
			}
		}
	}
	
	/**
	 * Returns whether the specified thread-local instance has message
	 * suppression enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if suppressed, otherwise false
	 */
	public boolean isSuppressCSMessages() {
		boolean b;
		if (aiTotalSuppressCSMessages.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = isSuppressCSMessages(getThreadLocal(false));
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
	public boolean isSuppressCSMessages(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.getSuppressCSMessages() > 0;
	}

	/**
	 * Enables or disables suppression of client/server messages for the
	 * current thread.
	 *
	 * @param b true to enable suppression, false to disable
	 */
	public void setSuppressCSMessages(boolean b) {
		setSuppressCSMessages(getThreadLocal(b), b);
	}

	/**
	 * Updates suppression counts for the specified thread-local instance and
	 * the global suppression counter. Logs throttled warnings when thresholds
	 * are exceeded.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment suppression, false to decrement
	 */
	public void setSuppressCSMessages(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x, x2;
		x = ti.getSuppressCSMessages();
		if (b) {
			x++;
			x2 = aiTotalSuppressCSMessages.incrementAndGet();
		} else {
			x--;
			x2 = aiTotalSuppressCSMessages.decrementAndGet();
		}
		ti.setSuppressCSMessages(x);
		if (x > 30 || x < 0 || x2 > 50 || x2 < 0) {
			msSuppressCSMessages = throttleLOG("TotalSuppressCSMessages =" + x2 + ", ti=" + x, msSuppressCSMessages);
		}
	}

	
	
	
	/**
	 * Returns whether the current thread is in a deleting state.
	 *
	 * @return true if deleting is active for this thread
	 */
	public boolean isDeleting() {
		if (aiTotalDelete.get() == 0) {
			return false;
		}
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return false;
		}
		Object[] objs = ti.getDeleting();
		return objs != null && objs.length > 0;
	}
	
	/**
	 * Returns whether the any thread is deleting the specified object.
	 *
	 * @param obj the object to check
	 * @return true if this thread is deleting the object
	 */
	public boolean isDeleting(Object obj) {
		if (obj == null) {
			return false;
		}
		return hmDeleting.containsKey(obj);
	}
	
	/**
	 * Returns whether the given thread-local instance is deleting the specified
	 * object.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object to check
	 * @return true if the instance is deleting the object
	 */
	public boolean isThreadDeleting(Object obj) {
		if (obj == null) {
			return false;
		}
		if (aiTotalDelete.get() == 0) {
			return false;
		}

		if (!hmDeleting.containsKey(obj)) {
			return false;
		}
		boolean b = isDeleting(getThreadLocal(false), obj);
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
	protected boolean isDeleting(OAThreadLocal ti, Object obj) {
		if (obj == null) {
			return false;
		}
		if (ti == null || ti.getDeleting() == null) {
			return false;
		}
		int x = ti.getDeleting().length;
		if (x == 0) {
			return false;
		}
		for (int i = 0; i < x; i++) {
			if (ti.getDeleting()[i] == obj) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds or removes the specified object from the global deleting map and
	 * updates the deleting state for the current thread.
	 *
	 * @param obj the object to update
	 * @param b   true to mark as deleting, false to unmark
	 */
	public void setDeleting(Object obj, boolean b) {
		if (obj == null) {
			return;
		}

		if (b) {
			hmDeleting.compute(obj, (k, v) -> {
			    if (v == null) return new AtomicInteger(1);
			    v.incrementAndGet();
			    return v;
			});			
			if (hmDeleting.size() > 25) {
				msDeleting = throttleLOG("TotalDeleting =" + hmDeleting.size(), msDeleting);
			}
		} else {
			hmDeleting.computeIfPresent(obj, (k, v) -> {
			    return (v.decrementAndGet() <= 0) ? null : v;
			});
		}

		setDeleting(getThreadLocal(b), obj, b);
	}
	
	/**
	 * Updates the deleting state for the specified thread-local instance and
	 * adjusts the global delete counter.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object being updated
	 * @param b   true to add, false to remove
	 */
	protected void setDeleting(OAThreadLocal ti, Object obj, boolean b) {
		if (ti == null) {
			return;
		}
		if (obj == null) {
			return;
		}

		Object[] objs = ti.getDeleting();
		if (b) {
			if (objs == null) {
				objs = new Object[1];
				ti.setDeleting(objs);
			}
			int x = objs.length;
			for (int i = 0;; i++) {
				if (i == x) {
					Object[] objs2 = new Object[x + 3];
					System.arraycopy(objs, 0, objs2, 0, x);
					objs = objs2;
					ti.setDeleting(objs);
					objs[x] = obj;
					break;
				}
				if (objs[i] == obj) {
					return;
				}
				if (objs[i] == null) {
					objs[i] = obj;
					break;
				}
			}
			x = aiTotalDelete.incrementAndGet();
			if (x > 100) {
				msDeleting = throttleLOG("TotalDelete =" + x, msDeleting);
			}
		} else {
			if (objs == null) {
				return;
			}
			int x = objs.length;
			boolean bAllNull = true;
			boolean bFound = false;
			for (int i = 0; i < x; i++) {
				if (objs[i] == obj) {
					bFound = true;
					objs[i] = null;
				} else {
					if (objs[i] != null) {
						bAllNull = false;
					}
				}
			}
			if (bFound) {
				aiTotalDelete.decrementAndGet();
			}
			if (bAllNull) {
				ti.setDeleting(null);
			}
		}
	}
	
	
	/**
	 * Returns whether the specified flag object exists in the current
	 * thread-local flag list.
	 *
	 * @param obj the flag object
	 * @return true if present
	 */
	public boolean isFlag(Object obj) {
		return isFlag(getThreadLocal(false), obj);
	}

	/**
	 * Returns whether the specified flag object exists in the given
	 * thread-local instance.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the flag object
	 * @return true if present
	 */
	protected boolean isFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return false;
		}
		return OAArray.contains(ti.getFlags(), obj);
	}

	/**
	 * Adds the specified flag object to the current thread-local flag list.
	 *
	 * @param obj the flag object to add
	 */
	public void setFlag(Object obj) {
		setFlag(getThreadLocal(true), obj);
	}

	/**
	 * Adds the specified flag to the thread-local instance and logs warnings
	 * when the flag list grows beyond safe thresholds.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the flag object
	 */
	protected void setFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.setFlags(OAArray.add(Object.class, ti.getFlags(), obj));
		if (ti.getFlags() != null && ti.getFlags().length > 20) {
			msFlag = throttleLOG("OAThreadLocal.tiFlags.length =" + ti.getFlags().length, msFlag);
		}
	}
	
	/**
	 * Removes the specified flag object from the current thread-local flag list.
	 *
	 * @param obj the flag to remove
	 */
	public void removeFlag(Object obj) {
		removeFlag(getThreadLocal(false), obj);
	}

	/**
	 * Removes a thread-local flag associated with the specified object.
	 * <p>
	 * This method clears the given object from the {@link OAThreadLocal} flag set
	 * for the current thread. Flags are used to track per-thread state markers
	 * (such as reentrancy guards, suppression markers, or transient execution
	 * conditions) without affecting global or cross-thread behavior.
	 * <p>
	 * If the thread-local flag collection becomes empty after removal, it may be
	 * cleared entirely to reduce memory usage and simplify subsequent checks.
	 *
	 * @param ti  the thread-local context for the current thread; may be {@code null},
	 *            in which case this method has no effect
	 * @param obj the flag object to remove; if {@code null} or not present,
	 *            this method has no effect
	 */
	protected void removeFlag(OAThreadLocal ti, Object obj) {
		if (ti == null) {
			return;
		}
		ti.setFlags(OAArray.removeValue(Object.class, ti.getFlags(), obj));
	}

	
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
	public void lock(Object object, int maxWaitTries) {
		lock(getThreadLocal(true), object, maxWaitTries);
	}

	/**
	 * Acquires a lock on the specified object using a default maximum wait
	 * threshold.
	 *
	 * @param object the object to lock
	 */
	public void lock(Object object) {
		OAThreadLocal ti = getThreadLocal(true);
		lock(ti, object, 2);
	}

	/**
	 * Returns whether the current thread holds any locks.
	 *
	 * @return true if one or more locks are held
	 */
	public boolean hasLock() {
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) return false;
		Object[] objs = ti.getLocks();
		return (objs != null && objs.length > 0);
	}

	/**
	 * Returns whether the current thread holds a lock on the specified object.
	 *
	 * @param obj the object to check
	 * @return true if the lock is held
	 */
	public boolean hasLock(Object obj) {
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return false;
		}
		Object[] objs = ti.getLocks();
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
	public Object[] getLocks() {
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return null;
		}
		return ti.getLocks();
	}

	/**
	 * Returns whether any thread currently holds a lock on the specified object.
	 *
	 * @param object the object to check
	 * @return true if locked by any thread
	 */
	public boolean isLocked(Object object) {
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
	public boolean isLockOwner(Object object) {
		OAThreadLocal ti = getThreadLocal(false);
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
	public OAThreadLocal getOAThreadLocal() {
		return getThreadLocal(true);
	}
	
	/**
	 * Internal implementation for acquiring a lock using the provided
	 * thread-local instance. Handles waiting, deadlock detection, and
	 * thread notification logic.
	 *
	 * @param tiThis         the thread-local instance
	 * @param thisLockObject the object to lock
	 * @param maxWaitTries   maximum wait attempts before force-acquiring
	 */
	protected void lock(OAThreadLocal tiThis, Object thisLockObject, int maxWaitTries) {
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
				tiThis.setWaitingOnLock(true);

				if (tiThis.getLocks().length > 1) {
					// need to wake up any threads that are waiting on this thread
					releaseDeadlock(tiThis, thisLockObject);
				}
			} finally {
				rwLock.writeLock().unlock();
			}

			// wait on ThreadLocal
			synchronized (tiThis) {
				if (!tiThis.getWaitingOnLock()) {
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
				if (tiThis.getLocks() != null && tiThis.getLocks().length > 1) {
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
	private boolean _lock(OAThreadLocal tlThis, Object thisLockObject, int maxWaitTries, int tries) {
		OAThreadLocal[] tls = hmLock.get(thisLockObject); // threadLocals that are using object (locked or waiting)

		if (tls != null && tls.length > 0 && tls[0] == tlThis) {
			// this ThreadLocal already is the owner for this object
			if (tries == 0) {
				// need to add it to ti.locks, since it will be released more then once
				tlThis.setLocks(OAArray.add(Object.class, tlThis.getLocks(), thisLockObject));
			}
			// check locks to make sure that it is not getting too big
			if (tlThis.getLocks().length > 39 && (tlThis.getLocks().length % 10) == 0) {
				// see if all objects are still locked
				String s = "";
				for (Object objx : tlThis.getLocks()) {
					OAThreadLocal[] tisx = hmLock.get(objx);
					if (tisx == null) {
						s = ", error: there are objects in ti.locks that are no longer locked";
					}
				}
				s = "OAThreadLocal.locks size=" + tlThis.getLocks().length + s;
				LOG.warning(s);
			}
			tlThis.setWaitingOnLock(false);
			return true; // already is the lock owner
		}

		if (tries == 0) {
			// must be inside sync: add to list of objects that this TI is locking
			tlThis.setLocks(OAArray.add(Object.class, tlThis.getLocks(), thisLockObject));

			if (tls == null) {
				tls = new OAThreadLocal[] { tlThis };
			} else {
				tls = (OAThreadLocal[]) OAArray.add(OAThreadLocal.class, tls, tlThis);
			}
			hmLock.put(thisLockObject, tls);
		}

		if (tls[0] == tlThis) {
			tlThis.setWaitingOnLock(false);
			return true; // this thread owns the lock
		}

		if (maxWaitTries > 0 && tries >= maxWaitTries && tries > 1) {
			if (tls[1] != tlThis) {
				// need to be second in list, since the owner (at pos [0]) will notify [1] when it is done - and not another threadLocal
				tls = (OAThreadLocal[]) OAArray.removeValue(OAThreadLocal.class, tls, tlThis);
				tls = (OAThreadLocal[]) OAArray.insert(OAThreadLocal.class, tls, tlThis, 1);
				hmLock.put(thisLockObject, tls);
			}
			tlThis.setWaitingOnLock(false);
			if (maxWaitTries > 2) {
				String s = "this.thread " + Thread.currentThread().getName() + ", timedout waiting for:" + thisLockObject + ", locked by:"
						+ tls[0].getThreadName();
				LOG.fine(s);
			}
			return true; // done trying
		}
		return false;
	}
	
	
	/**
	 * Returns the number of detected deadlocks encountered during lock
	 * acquisition.
	 *
	 * @return the deadlock count
	 */
	public int getDeadlockCount() {
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
	private void releaseDeadlock(OAThreadLocal tiThis, Object lockObject) {
		OAThreadLocal[] tls = hmLock.get(lockObject);
		if (tls == null) {
			return;
		}
		OAThreadLocal tlOwner = tls[0];

		Object[] ownerLocks = tlOwner.getLocks();
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
				tlOwner.setWaitingOnLock(false);
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
	public void releaseAllLocks() {
		OAThreadLocal tl = getThreadLocal(false);
		if (tl == null) {
			return;
		}
		Object[] locks = tl.getLocks();
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
	public void unlock(Object object) {
		unlock(getThreadLocal(false), object);
	}
	
	/**
	 * Internal implementation for releasing a lock using the specified
	 * thread-local instance.
	 *
	 * @param ti     the thread-local instance
	 * @param object the object to unlock
	 */
	protected void unlock(OAThreadLocal ti, Object object) {
		if (ti == null) return;
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
	private void _unlock(OAThreadLocal tl, Object object) {
		final int pos = OAArray.indexOf(tl.getLocks(), object);
		if (pos < 0) {
			return;
		}

		final boolean bMoreLocks = OAArray.indexOf(tl.getLocks(), object, pos + 1) >= 0;

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
					tls[0].setWaitingOnLock(false); // notify the next one waiting
					tls[0].notifyAll();
				}
			}
		}
		tl.setLocks(OAArray.removeAt(Object.class, tl.getLocks(), pos)); // must be inside sync
	}

	/**
	 * Returns whether any thread is currently modifying hub-merger state.
	 *
	 * @return true if hub-merger updates are active
	 */
	public boolean isHubMergerChanging() {
		boolean b;
		if (aiTotalHubMergerChanging.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = isHubMergerChanging(getThreadLocal(false));
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
	protected boolean isHubMergerChanging(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.getHubMergerChangingCount() > 0;
	}
	
	/**
	 * Enables or disables hub-merger change tracking for the current thread.
	 *
	 * @param b true to enable, false to disable
	 */
	public void setHubMergerChanging(boolean b) {
		// LOG.finer(""+b);
		setHubMergerChanging(getThreadLocal(b), b);
	}
	
	
	/**
	 * Updates hub-merger change counts for the specified thread-local instance
	 * and executes any pending callbacks when the change count reaches zero.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
	public void setHubMergerChanging(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x;

		if (b) {
			ti.setHubMergerChangingCount(ti.getHubMergerChangingCount() + 1);
			x = aiTotalHubMergerChanging.getAndIncrement();
		} else {
			ti.setHubMergerChangingCount(ti.getHubMergerChangingCount() - 1);
			x = aiTotalHubMergerChanging.decrementAndGet();
			
			if (ti.getHubMergerChangingCount() == 0) {
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
    public void addHubMergerCallback(OAThreadLocalHubMergerCallback cb) {
        if (cb == null) return;
        addHubMergerCallback(getThreadLocal(true), cb);
    }
	
    /**
     * Adds a hub-merger callback to the specified thread-local instance, or
     * executes it immediately if no hub-merger changes are pending.
     *
     * @param ti the thread-local instance
     * @param cb the callback to register
     */
    protected void addHubMergerCallback(OAThreadLocal ti, OAThreadLocalHubMergerCallback cb) {
        if (ti == null) return;
        if (cb == null) return;
        
        if (ti.getHubMergerChangingCount() == 0) {
            cb.callback();
            return;
        }
        ti.hubMergerCallback = (OAThreadLocalHubMergerCallback[]) OAArray.add(OAThreadLocalHubMergerCallback.class, ti.hubMergerCallback, cb);
    }

    /**
     * Enables or disables recording of undoable property changes for the
     * current thread.
     *
     * @param b true to enable, false to disable
     */
	public void setCreateUndoablePropertyChanges(boolean b) {
		// LOG.finer(""+b);
		setCreateUndoablePropertyChanges(getThreadLocal(b), b);
	}

	/**
	 * Updates undoable-change tracking for the specified thread-local instance
	 * and updates the global counter.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to enable, false to disable
	 */
	protected void setCreateUndoablePropertyChanges(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		if (ti.getCompoundUndoableName() != null) {
			return;
		}
		int x;
		ti.setCreateUndoablePropertyChanges(b);
		if (b) {
			x = aiTotalCaptureUndoablePropertyChanges.getAndIncrement();
		} else {
			x = aiTotalCaptureUndoablePropertyChanges.decrementAndGet();
		}
		if (x > 50 || x < 0) {
			msCreateUndoablePropertyChanges = throttleLOG("TotalCaptureUndoablePropertyChanges=" + x + ", ti.createUndoablePropertyChanges="
					+ ti.isCreateUndoablePropertyChanges(), msCreateUndoablePropertyChanges);
		}
	}

	/**
	 * Returns whether undoable property change recording is enabled for the
	 * current thread.
	 *
	 * @return true if active
	 */
	public boolean getCreateUndoablePropertyChanges() {
		boolean b;
		if (aiTotalCaptureUndoablePropertyChanges.get() == 0) {
			// LOG.finest("fast");
			b = false;
		} else {
			b = getCreateUndoablePropertyChanges(getThreadLocal(false));
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
	protected boolean getCreateUndoablePropertyChanges(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.isCreateUndoablePropertyChanges();
	}

	/**
	 * Begins a compound undoable sequence with the specified name for the
	 * current thread.
	 *
	 * @param compoundName the descriptive name for the compound edit
	 */
	public void startUndoable(String compoundName) {
		startUndoable(getThreadLocal(true), compoundName);
	}

	/**
	 * Internal implementation to start tracking a compound undoable change
	 * using the specified thread-local instance.
	 *
	 * @param ti           the thread-local instance
	 * @param compoundName the compound edit name
	 */
	protected void startUndoable(OAThreadLocal ti, String compoundName) {
		if (ti == null) {
			return;
		}
		if (compoundName == null) {
			compoundName = "changes";
		}
		ti.setCreateUndoablePropertyChanges(true);
		ti.setCompoundUndoableName(compoundName);
		OAUndoManager.startCompoundEdit(compoundName);

		int x = aiTotalCaptureUndoablePropertyChanges.getAndIncrement();
		if (x > 50 || x < 0) {
			msUndoable = throttleLOG("TotalCaptureUndoablePropertyChanges=" + x + ", ti.createUndoablePropertyChanges="
					+ ti.isCreateUndoablePropertyChanges(), msUndoable);
		}
	}
 
	/**
	 * Completes the current compound undoable sequence for the thread.
	 */
	public void endUndoable() {
		endUndoable(getThreadLocal(true));
	}

	/**
	 * Convenience wrapper for starting a compound undoable sequence using the
	 * specified name.
	 *
	 * @param compoundName the compound edit name
	 */
	public void startCompoundUndoable(String compoundName) {
		startUndoable(getThreadLocal(true), compoundName);
	}
    
	public boolean isCreatingCompoundUndoable() {
		OAThreadLocal tl = getThreadLocal(false);
		if (tl == null) {
			return false;
		}
		return tl.isCreateUndoablePropertyChanges();
	}

	/**
	 * Convenience wrapper that ends the current compound undoable sequence
	 * for the thread.
	 */
	public void endCompoundUndoable() {
		endUndoable(getThreadLocal(true));
	}

	/**
	 * Internal implementation for completing a compound undoable sequence for
	 * the specified thread-local instance. Resets undoable flags and updates
	 * global counters.
	 *
	 * @param ti the thread-local instance
	 */
	protected void endUndoable(OAThreadLocal ti) {
		if (ti == null) {
			return;
		}
		ti.setCreateUndoablePropertyChanges(false);
		ti.setCompoundUndoableName(null);
		OAUndoManager.endCompoundEdit();

		aiTotalCaptureUndoablePropertyChanges.decrementAndGet();
	}

	/**
	 * Registers the specified sibling helper for the current thread.
	 *
	 * @param sh the sibling helper
	 * @return true if added, false if already present
	 */
	public boolean addSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return false;
		}
		return addSiblingHelper(getThreadLocal(true), sh);
	}

	/**
	 * Internal implementation to register a sibling helper for the specified
	 * thread-local instance and update global counters.
	 *
	 * @param ti the thread-local instance
	 * @param sh the sibling helper to add
	 * @return true if the helper was added
	 */
	protected boolean addSiblingHelper(OAThreadLocal ti, OASiblingHelper sh) {
		if (ti == null || sh == null) {
			return false;
		}
		if (ti.alSiblingHelper == null) {
			ti.alSiblingHelper = new ArrayList<>();
		} else if (ti.alSiblingHelper.contains(sh)) {
			return false;
		}

		int x = aiTotalSiblingHelper.incrementAndGet();
		ti.alSiblingHelper.add(sh);
		if (x > 20 || x < 0 || ti.alSiblingHelper.size() > 10) {
			msSiblingHelper = throttleLOG("TotalSiblingHelper.add, tot=" + x + ", this.size=" + ti.alSiblingHelper.size() + ", thread="
					+ Thread.currentThread(), msSiblingHelper);
		}
		return true;
	}
	
	/**
	 * Removes the specified sibling helper from the current thread's list.
	 *
	 * @param sh the sibling helper to remove
	 */
	public void removeSiblingHelper(OASiblingHelper sh) {
		if (sh == null) {
			return;
		}
		if (aiTotalSiblingHelper.get() == 0) {
			return;
		}
		removeSiblingHelper(getThreadLocal(true), sh);
	}

	/**
	 * Internal implementation that removes the specified sibling helper from
	 * the thread-local instance and updates global counters.
	 *
	 * @param ti the thread-local instance
	 * @param sh the sibling helper to remove
	 */
	protected void removeSiblingHelper(OAThreadLocal ti, OASiblingHelper sh) {
		if (ti == null || sh == null) {
			return;
		}
		int x = aiTotalSiblingHelper.decrementAndGet();

		if (ti.alSiblingHelper == null) {
			return;
		}
		ti.alSiblingHelper.remove(sh);

		if (x > 20 || x < 0 || ti.alSiblingHelper.size() > 10) {
			msSiblingHelper = throttleLOG("TotalSiblingHelper.remove, tot=" + x + ", this.size=" + ti.alSiblingHelper.size() + ", thread="
					+ Thread.currentThread(), msSiblingHelper);
		}
	}

	/**
	 * Returns the list of sibling helpers associated with the current thread,
	 * or null if none exist.
	 *
	 * @return the list of sibling helpers, or null
	 */
	public ArrayList<OASiblingHelper> getSiblingHelpers() {
		if (aiTotalSiblingHelper.get() == 0) {
			return null;
		}
		return getSiblingHelpers(getThreadLocal(true));
	}

	/**
	 * Returns the sibling helpers stored in the specified thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @return the list of sibling helpers, or null
	 */
	public ArrayList<OASiblingHelper> getSiblingHelpers(OAThreadLocal ti) {
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
	public boolean hasSiblingHelpers() {
		if (aiTotalSiblingHelper.get() == 0) {
			return false;
		}
		ArrayList<OASiblingHelper> al = getSiblingHelpers(getThreadLocal(true));
		return (al != null && al.size() > 0);
	}

	/**
	 * Removes all sibling helpers from the current thread.
	 */
	public void clearSiblingHelpers() {
		if (aiTotalSiblingHelper.get() == 0) {
			return;
		}
		ArrayList<OASiblingHelper> al = getSiblingHelpers(getThreadLocal(true));
		if (al != null) {
			al.clear();
		}
	}

	/**
	 * Sets the status message for the current thread-local instance.
	 *
	 * @param msg the status message
	 */
	public void setStatus(String msg) {
		getOAThreadLocal().setStatus(msg);
	}

	/**
	 * Assigns the remote request information for the current thread.
	 *
	 * @param ri the request information
	 */
	public void setRemoteRequestInfo(RequestInfo ri) {
		getOAThreadLocal().setRequestInfo(ri);
	}

	/**
	 * Returns the remote request information for the current thread.
	 *
	 * @return the RequestInfo instance, or null if none exists
	 */
	public RequestInfo getRemoteRequestInfo() {
		return getOAThreadLocal().getRequestInfo();
	}

	
	/**
	 * Enables or disables sending of messages from OARemoteThread instances.
	 *
	 * @param b true to enable sending, false to disable
	 * @return the previous send-messages state
	 */
	public boolean setSendMessages(boolean b) {
		return OARemoteThreadDelegate.sendMessages(b);
	}

	/**
	 * Assigns or clears an object used to notify the current thread when
	 * remote-thread operations need to resume.
	 *
	 * @param obj the object to assign, or null to clear
	 */
	public void setNotifyObject(Object obj) {
		if (obj == null) {
			if (aiTotalNotifyWaitingObject.get() == 0) {
				return;
			}
			OAThreadLocal tl = getThreadLocal(false);
			if (tl != null && (tl.getNotifyObject() != null)) {
				aiTotalNotifyWaitingObject.decrementAndGet();
				tl.setNotifyObject(null);
			}
		} else {
			OAThreadLocal tl = getThreadLocal(true);
			if (tl.getNotifyObject() == null) {
				aiTotalNotifyWaitingObject.incrementAndGet();
			}
			tl.setNotifyObject(obj);
		}
	}
	
	/**
	 * Notifies the thread waiting on the thread-local notify object, if one
	 * exists, and clears the notify reference.
	 */
	public void notifyWaitingThread() {
		if (aiTotalNotifyWaitingObject.get() == 0) {
			return;
		}

		OAThreadLocal tl = getThreadLocal(false);
		if (tl == null) {
			return;
		}
		if (tl.getNotifyObject() == null) {
			return;
		}
		synchronized (tl.getNotifyObject()) {
			tl.getNotifyObject().notifyAll();
		}
		setNotifyObject(null);
	}
	
	/**
	 * Returns the recursive trigger count for the current thread.
	 *
	 * @return the trigger count
	 */
	public int getRecursiveTriggerCount() {
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
	protected int getRecursiveTriggerCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.getRecursiveTriggerCount();
	}

	/**
	 * Sets the recursive trigger count for the current thread-local instance.
	 *
	 * @param x the trigger count to assign
	 */
	public void setRecursiveTriggerCount(int x) {
		setRecursiveTriggerCount(getThreadLocal(true), x);
	}
	
	/**
	 * Updates the recursive trigger count on the supplied thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @param x  the trigger count to assign
	 */
	protected void setRecursiveTriggerCount(OAThreadLocal ti, int x) {
		if (ti == null) {
			return;
		}
		ti.setRecursiveTriggerCount(x);
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	public int getHubListenerTreeCount() {
		int x;
		if (aiTotalHubListenerTreeCount.get() == 0) {
			x = 0;
		} else {
			x = getHubListenerTreeCount(getThreadLocal(false));
		}
		return x;
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	protected int getHubListenerTreeCount(OAThreadLocal ti) {
		if (ti == null) {
			return 0;
		}
		return ti.getHubListenerTreeCount();
	}

	/**
	 * Increments or decrements the hub-listener tree depth for the current thread.
	 *
	 * @param b true to increment, false to decrement
	 */
	public void setHubListenerTree(boolean b) {
		setHubListenerTree(getThreadLocal(b), b);
	}

	/**
	 * Adjusts the hub-listener tree depth on the supplied thread-local instance
	 * and updates global counters.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
	protected void setHubListenerTree(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return;
		}
		int x;

		if (b) {
			ti.setHubListenerTreeCount(ti.getHubListenerTreeCount() + 1);
			x = aiTotalHubListenerTreeCount.getAndIncrement();
		} else {
			ti.setHubListenerTreeCount(ti.getHubListenerTreeCount() - 1);
			x = aiTotalHubListenerTreeCount.decrementAndGet();
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
	public void setIgnoreTreeListenerProperty(String prop) {
		getThreadLocal(true).setIgnoreTreeListenerProperty(prop);
	}
	
	/**
	 * Returns the property name currently ignored by tree listeners for the
	 * thread.
	 *
	 * @return the ignored property name
	 */
	public String getIgnoreTreeListenerProperty() {
		return getThreadLocal(true).getIgnoreTreeListenerProperty();
	}
	
	/**
	 * Returns the number of OA sync events recorded for the current thread.
	 *
	 * @return the sync event count
	 */
	public int getOASyncEventCount() {
		return getThreadLocal(true).oaSyncEventCount;
	}

	/**
	 * Increments the OA sync event count for the current thread.
	 */
	public void incrOASyncEventCount() {
		getThreadLocal(true).oaSyncEventCount++;
	}

	
	/**
	 * Returns the most recent HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the latest HubEvent or null
	 */
	public  HubEvent getCurrentHubEvent() {
		if (aiTotalHubEvent.get() == 0) {
			return null;
		}
		OAThreadLocal tl = getThreadLocal(false);
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
	public boolean isOpenHubEvent(HubEvent he) {
		if (he == null) {
			return false;
		}
		if (aiTotalHubEvent.get() == 0) {
			return false;
		}
		OAThreadLocal tl = getThreadLocal(false);
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
	public HubEvent getOldestHubEvent() {
		if (aiTotalHubEvent.get() == 0) {
			return null;
		}
		OAThreadLocal tl = getThreadLocal(false);
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
	public void addHubEvent(HubEvent he) {
		if (he == null) {
			return;
		}
		OAThreadLocal tl = getThreadLocal(true);
		if (tl.alHubEvent == null) {
			tl.alHubEvent = new ArrayList<>();
		}
		if (!tl.alHubEvent.contains(he)) {
			tl.alHubEvent.add(he);
		}

		aiTotalHubEvent.incrementAndGet();
		int x = tl.alHubEvent.size();
		if (x > 25 || aiTotalHubEvent.get() > 250) {
			msHubEvent = throttleLOG("TotalHubEvent this=" + x + ", all=" + aiTotalHubEvent.get(), msHubEvent);
		}
	}
	
	/**
	 * Removes the supplied HubEvent from the current thread’s active-event list
	 * and updates global counters.
	 *
	 * @param he the HubEvent to remove
	 */
	public void removeHubEvent(HubEvent he) {
		if (aiTotalHubEvent.get() == 0) {
			return;
		}
		OAThreadLocal tl = getThreadLocal(false);
		if (tl == null) {
			return;
		}
		if (tl.alHubEvent == null) {
			return;
		}
		tl.alHubEvent.remove(he);

		if (tl.alHubEvent.size() == 0) {
			tl.setCalcPropertyEvents(null);
		}

		aiTotalHubEvent.decrementAndGet();
		int x = tl.alHubEvent.size();
		if (x > 25 || aiTotalHubEvent.get() > 250 || aiTotalHubEvent.get() < 0) {
			msHubEvent = throttleLOG("TotalHubEvent this=" + x + ", all=" + aiTotalHubEvent.get(), msHubEvent);
		}
	}
	
	/**
	 * Returns whether the current thread is in the process of sending one or more
	 * HubEvents.
	 *
	 * @return true if event sending is active
	 */
	public boolean isSendingEvent() {
		boolean b;
		if (aiTotalHubEvent.get() == 0) {
			b = false;
		} else {
			b = isSendingEvent(getThreadLocal(false));
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
	protected boolean isSendingEvent(OAThreadLocal ti) {
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
	public boolean hasSentCalcPropertyChange(Hub thisHub, OAObject thisObj, String propertyName) {
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

		OAThreadLocal tl = getThreadLocal(true);

		if (tl.getCalcPropertyEvents() == null) {
			tl.setCalcPropertyEvents(new Tuple3[1]);
			tl.getCalcPropertyEvents()[0] = new Tuple3(hubMain, thisObj, propertyName);
			return false;
		}
		for (Tuple3<Hub, OAObject, String> tup : tl.getCalcPropertyEvents()) {
			if (tup.a == hubMain && tup.b == thisObj) {
				if (propertyName.equalsIgnoreCase(tup.c)) {
					return true;
				}
			}
		}
		int x = tl.getCalcPropertyEvents().length;
		Tuple3<Hub, OAObject, String>[] temp = new Tuple3[x + 1];
		System.arraycopy(tl.getCalcPropertyEvents(), 0, temp, 0, x);
		temp[x] = new Tuple3<Hub, OAObject, String>(hubMain, thisObj, propertyName);
		tl.setCalcPropertyEvents(temp);

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
	public Object getContext() {
		OAThreadLocal ti = getThreadLocal(true);
		return ti.context;
	}

	/**
	 * Assigns the context object for the current thread.
	 *
	 * @param context the context to assign
	 */
	public void setContext(Object context) {
		OAThreadLocal ti = getThreadLocal(true);
		ti.context = context;
	}

	/**
	 * Sets the admin flag for the current thread-local instance.
	 *
	 * @param b true to enable admin mode, false to disable
	 * @return the previous admin flag value
	 */
	public boolean setAdmin(boolean b) {
		OAThreadLocal ti = getThreadLocal(true);
		return setIsAdmin(ti, b);
	}
	
	/**
	 * Convenience wrapper that sets the admin flag for the current thread-local
	 * instance.
	 *
	 * @param b true to enable admin mode
	 * @return the previous admin flag value
	 */
	public boolean setIsAdmin(boolean b) {
		OAThreadLocal ti = getThreadLocal(true);
		return setIsAdmin(ti, b);
	}

	/**
	 * Updates the admin flag on the supplied thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @param b  the new admin flag value
	 * @return the previous admin flag value
	 */
	public boolean setIsAdmin(OAThreadLocal ti, boolean b) {
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
	public boolean isAdmin() {
		return getIsAdmin(getThreadLocal(false));
	}

	public boolean getIsAdmin() {
		return getIsAdmin(getThreadLocal(false));
	}
	
	/**
	 * Returns whether the supplied thread-local instance has admin mode enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if admin mode is enabled
	 */
	public boolean getIsAdmin(OAThreadLocal ti) {
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
	public void initialize(OAThreadLocal tl) {
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
	public OAJson getOAJackson() {
		if (aiTotalJackson.get() == 0) {
			return null;
		}
		OAThreadLocal ti = getThreadLocal(false);
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
	public OAJson setOAJackson(OAJson jackson) {
		if (jackson == null && aiTotalJackson.get() == 0) {
			return null;
		}

		if (jackson != null) {
			aiTotalJackson.incrementAndGet();
		} else {
			aiTotalJackson.decrementAndGet();
		}

		OAThreadLocal ti = getThreadLocal(true);
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
	public void addDontAdjustHub(Hub hub) {
		if (hub == null) {
			return;
		}
		hub = HubShareDelegate.getMainSharedHub(hub);
		OAThreadLocal ti = getThreadLocal(true);
		ti.dontAdjustHubs = (Hub[]) OAArray.add(Hub.class, ti.dontAdjustHubs, hub);
		aiTotalDontAdjustHub.incrementAndGet();
		int x = ti.dontAdjustHubs.length;
		if (x > 25 || aiTotalDontAdjustHub.get() > 250 || aiTotalDontAdjustHub.get() < 0) {
			msHubEvent = throttleLOG("total DontAdjustHub this=" + x + ", all=" + aiTotalDontAdjustHub.get(), msHubEvent);
		}
	}
	
	/**
	 * Removes the supplied hub from the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to remove
	 */
	public void removeDontAdjustHub(Hub hub) {
		if (hub == null) {
			return;
		}
		if (aiTotalDontAdjustHub.get() == 0) {
			return;
		}
		OAThreadLocal ti = getThreadLocal(false);
		if (ti == null) {
			return;
		}
		hub = HubShareDelegate.getMainSharedHub(hub);
		ti.dontAdjustHubs = (Hub[]) OAArray.removeValue(Hub.class, ti.dontAdjustHubs, hub);
		aiTotalDontAdjustHub.decrementAndGet();
	}

	public boolean getCanAdjustHub(Hub hub) {
		if (hub == null) {
			return false;
		}
		if (aiTotalDontAdjustHub.get() == 0) {
			return true;
		}

		hub = HubShareDelegate.getMainSharedHub(hub);

		OAThreadLocal ti = getThreadLocal(false);
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
	public boolean isSyncThread() {
		OAThreadLocal ti = getThreadLocal(false);
		return (ti != null && ti.isSyncThread());
	}
	
	/**
	 * Enables or disables sync-thread mode for the current thread-local
	 * instance.
	 *
	 * @param b true to enable, false to disable
	 * @return the previous sync-thread flag value
	 */
	public boolean setSyncThread(boolean b) {
		OAThreadLocal ti = getThreadLocal(b);

		if (ti == null) {
			return false;
		}
		boolean b2 = ti.isSyncThread();
		ti.setIsSyncThread(b);
		return b2;
	}
	
	
	/**
	 * Returns whether the current thread is actively performing a refresh
	 * operation.
	 *
	 * @return true if refreshing
	 */
	public boolean isRefreshing() {
		boolean b;
		if (aiTotalIsRefreshing.get() == 0) {
			b = false;
		} else {
			b = isRefreshing(getThreadLocal(false));
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
	protected boolean isRefreshing(OAThreadLocal ti) {
		if (ti == null) {
			return false;
		}
		return ti.getRefreshing() > 0;
	}
	
	/**
	 * Enables or disables refresh mode for the current thread-local instance.
	 *
	 * @param b true to increment, false to decrement
	 */
	public void setRefreshing(boolean b) {
		setRefreshing(getThreadLocal(b), b);
	}

	/**
	 * Adjusts the refresh counter on the supplied thread-local instance and
	 * updates the global refresh counter.
	 *
	 * @param ti the thread-local instance
	 * @param b  true to increment, false to decrement
	 */
	protected boolean setRefreshing(OAThreadLocal ti, boolean b) {
		if (ti == null) {
			return false;
		}
		int x, x2;
		boolean bPreviousValue;
		x = ti.getRefreshing();
		if (b) {
			bPreviousValue = (x > 0);
			x++;
			x2 = aiTotalIsRefreshing.getAndIncrement();
		} else {
			bPreviousValue = (x > 0);
			x--;
			x2 = aiTotalIsRefreshing.decrementAndGet();
		}
		ti.setRefreshing(x);;
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
	public Hub getFastLoadingHub() {
		return getFastLoadingHub(getThreadLocal(false));
	}
	
	/**
	 * Returns the fast-loading hub stored in the supplied thread-local instance,
	 * or null if none is set.
	 *
	 * @param ti the thread-local instance
	 * @return the fast-loading hub or null
	 */
	protected Hub getFastLoadingHub(OAThreadLocal ti) {
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
	public boolean isFastLoadingHub(Hub h) {
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
	public void setFastLoadingHub(Hub hub) {
		setFastLoadingHub(getThreadLocal(true), hub);
	}
	
	/**
	 * Assigns the specified hub as the fast-loading hub for the given thread-local
	 * instance. If a previous fast-loading hub exists, its list-refresh event is
	 * triggered before replacing it.
	 *
	 * @param ti  the thread-local instance to update
	 * @param hub the hub to mark as fast-loading
	 */
	protected void setFastLoadingHub(OAThreadLocal ti, Hub hub) {
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
	public OAProcess getProcess() {
		return getProcess(getThreadLocal(false));
	}
	
	/**
	 * Returns the process assigned to the specified thread-local instance.
	 *
	 * @param ti the thread-local instance
	 * @return the associated process, or null if none is set
	 */
	protected OAProcess getProcess(OAThreadLocal ti) {
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
	public void setProcess(OAProcess process) {
		setProcess(getThreadLocal(true), process);
	}
	
	/**
	 * Sets the process for the given thread-local instance.
	 *
	 * @param ti      the thread-local instance
	 * @param process the process to assign
	 */
	protected void setProcess(OAThreadLocal ti, OAProcess process) {
		if (ti == null) {
			return;
		}
		ti.process = process;
	}
	
	
	
	
	/**
	 * Logs a throttled warning message when sufficient time has passed since
	 * the previous log for the same category.
	 *
	 * @param msg    the message to log
	 * @param msLast the timestamp of the previous logged message
	 * @return the updated timestamp
	 */
	public long throttleLOG(String msg, long msLast) {
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
	public String getAllStackTraces() {
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
	public String getThreadDump() {
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
	
}
