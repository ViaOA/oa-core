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
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.json.OAJson;
import com.viaoa.process.OAProcess;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.transaction.OATransaction;

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


	// qqqqqqqqq remove this and fix code that calls it	
	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread.
	 * Creates a new instance when none exists and creation is allowed.
	 *
	 * @param bCreateIfNull true to create a new thread-local instance if missing
	 * @return the thread-local instance, or null if none exists and creation is disabled
	 */
	@Deprecated
	protected static OAThreadLocal getThreadLocal(boolean bCreateIfNull) {
		OAThreadLocal ti = OARuntime.get().threadService().getThreadLocal(bCreateIfNull);
		return ti;
	}

	/**
	 * Sets the current thread's transaction reference and updates the global
	 * transaction counter. Called internally by transaction-related classes.
	 *
	 * @param t the transaction to assign, or null to clear it
	 */
	public static void setTransaction(OATransaction t) {
		OARuntime.get().threadService().setTransaction(t);
	}

	/**
	 * Returns the current thread's active transaction, or null if no
	 * transaction is registered.
	 *
	 * @return the current transaction or null
	 */
	public static OATransaction getTransaction() {
		return OARuntime.get().threadService().getTransaction();
	}

	/**
	 * Returns whether the specified thread-local instance is in a loading state.
	 *
	 * @param ti the thread-local instance
	 * @return true if loading is greater than zero
	 */
	public static boolean isLoading() {
		return OARuntime.get().threadService().isLoading();
	}


	/**
	 * Updates the loading flag for the current thread and returns the previous
	 * loading state.
	 *
	 * @param b true to increase the loading count, false to decrease it
	 * @return previous loading flag before the update
	 */
	public static boolean setLoading(boolean b) {
		return OARuntime.get().threadService().setLoading(b);
	}

	/**
	 * Returns the current thread's object-cache add mode, or the default mode
	 * when no add mode is active.
	 *
	 * @return the current add mode
	 */
	public static int getObjectCacheAddMode() {
		return OARuntime.get().threadService().getObjectCacheAddMode();
	}


	/**
	 * Sets the object-cache add mode for the current thread and updates the
	 * global counter when transitioning between active and default modes.
	 *
	 * @param mode the add mode to assign
	 */
	public static void setObjectCacheAddMode(int mode) {
		OARuntime.get().threadService().setObjectCacheAddMode(mode);
	}

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
		return OARuntime.get().threadService().getObjectSerializer();
	}

	/**
	 * Sets the object serializer for the current thread and updates global
	 * serializer counters.
	 *
	 * @param si the serializer to assign, or null to clear it
	 */
	public static void setObjectSerializer(OAObjectSerializer si) {
		// LOG.finer("OAObjectSerializer="+(si != null));
		OARuntime.get().threadService().setObjectSerializer(si);
	}

	/**
	 * Returns whether the specified thread-local instance has message
	 * suppression enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if suppressed, otherwise false
	 */
	public static boolean isSuppressCSMessages() {
		return OARuntime.get().threadService().isSuppressCSMessages();
	}

	/**
	 * Enables or disables suppression of client/server messages for the
	 * current thread.
	 *
	 * @param b true to enable suppression, false to disable
	 */
	public static void setSuppressCSMessages(boolean b) {
		OARuntime.get().threadService().setSuppressCSMessages(b);
	}

	/**
	 * Returns whether the current thread is in a deleting state.
	 *
	 * @return true if deleting is active for this thread
	 */
	public static boolean isDeleting() {
		return OARuntime.get().threadService().isDeleting();
	}

	/**
	 * Returns whether the current thread is deleting the specified object.
	 *
	 * @param obj the object to check
	 * @return true if this thread is deleting the object
	 */
	public static boolean isDeleting(Object obj) {
		return OARuntime.get().threadService().isDeleting(obj);
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
		return OARuntime.get().threadService().isThreadDeleting(obj);
	}


	/**
	 * Adds or removes the specified object from the global deleting map and
	 * updates the deleting state for the current thread.
	 *
	 * @param obj the object to update
	 * @param b   true to mark as deleting, false to unmark
	 */
	public static void setDeleting(Object obj, boolean b) {
		OARuntime.get().threadService().setDeleting(obj, b);
	}

	/**
	 * Returns whether the specified flag object exists in the current
	 * thread-local flag list.
	 *
	 * @param obj the flag object
	 * @return true if present
	 */
	public static boolean isFlag(Object obj) {
		return OARuntime.get().threadService().isFlag(obj);
	}
	
	/**
	 * Adds the specified flag object to the current thread-local flag list.
	 *
	 * @param obj the flag object to add
	 */
	public static void setFlag(Object obj) {
		OARuntime.get().threadService().setFlag(obj);
	}

	/**
	 * Removes the specified flag object from the current thread-local flag list.
	 *
	 * @param obj the flag to remove
	 */
	public static void removeFlag(Object obj) {
		OARuntime.get().threadService().removeFlag(obj);
	}

	/**
	 * Acquires a lock on the specified object with a maximum number of wait
	 * attempts before force-acquiring the lock.
	 *
	 * @param object        the object to lock
	 * @param maxWaitTries  maximum wait attempts before taking the lock
	 */
	public static void lock(Object object, int maxWaitTries) {
		OARuntime.get().threadService().lock(object, maxWaitTries);
	}

	/**
	 * Acquires a lock on the specified object using a default maximum wait
	 * threshold.
	 *
	 * @param object the object to lock
	 */
	public static void lock(Object object) {
		OARuntime.get().threadService().lock(object);
	}

	/**
	 * Returns whether the current thread holds any locks.
	 *
	 * @return true if one or more locks are held
	 */
	public static boolean hasLock() {
		return OARuntime.get().threadService().hasLock();
	}

	/**
	 * Returns whether the current thread holds a lock on the specified object.
	 *
	 * @param obj the object to check
	 * @return true if the lock is held
	 */
	public static boolean hasLock(Object obj) {
		return OARuntime.get().threadService().hasLock(obj);
	}

	/**
	 * Returns all lock objects currently held by the thread.
	 *
	 * @return an array of locked objects, or null if none
	 */
	public static Object[] getLocks() {
		return OARuntime.get().threadService().getLocks();	
	}

	/**
	 * Returns whether any thread currently holds a lock on the specified object.
	 *
	 * @param object the object to check
	 * @return true if locked by any thread
	 */
	public static boolean isLocked(Object object) {
		return OARuntime.get().threadService().isLocked(object);
	}

	/**
	 * Returns whether the current thread is the owner of the lock for the
	 * specified object.
	 *
	 * @param object the object to check
	 * @return true if the current thread owns the lock
	 */
	public static boolean isLockOwner(Object object) {
		return OARuntime.get().threadService().isLockOwner(object);
	}

	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread,
	 * creating one if necessary.
	 *
	 * @return the thread-local instance
	 */
	public static OAThreadLocal getOAThreadLocal() {
		return OARuntime.get().threadService().getThreadLocal(true);
	}

	/**
	 * Returns the number of detected deadlocks encountered during lock
	 * acquisition.
	 *
	 * @return the deadlock count
	 */
	public static int getDeadlockCount() {
		return OARuntime.get().threadService().getDeadlockCount();
	}

	/**
	 * Releases all locks currently held by the thread-local instance for the
	 * current thread.
	 */
	public static void releaseAllLocks() {
		OARuntime.get().threadService().releaseAllLocks();
	}

	/**
	 * Releases the lock held by the current thread on the specified object.
	 *
	 * @param object the object to unlock
	 */
	public static void unlock(Object object) {
		OARuntime.get().threadService().unlock(object);
	}

	// HubListenerTree uses this to ignore dependent property changes caused by add/remove objects from hubMerger.hubMaster
	/**
	 * Returns whether any thread is currently modifying hub-merger state.
	 *
	 * @return true if hub-merger updates are active
	 */
	public static boolean isHubMergerChanging() {
		return OARuntime.get().threadService().isHubMergerChanging();
	}

	/**
	 * Enables or disables hub-merger change tracking for the current thread.
	 *
	 * @param b true to enable, false to disable
	 */
	public static void setHubMergerChanging(boolean b) {
		OARuntime.get().threadService().setHubMergerChanging(b);
	}


	/**
	 * Registers a callback to be executed once hub-merger changes finish for
	 * the current thread.
	 *
	 * @param cb the callback to register
	 */
    public static void addHubMergerCallback(OAThreadLocalHubMergerCallback cb) {
		OARuntime.get().threadService().addHubMergerCallback(cb);
    }
    
    /**
     * Enables or disables recording of undoable property changes for the
     * current thread.
     *
     * @param b true to enable, false to disable
     */
	public static void setCreateUndoablePropertyChanges(boolean b) {
		OARuntime.get().threadService().setCreateUndoablePropertyChanges(b);
	}

	/**
	 * Returns whether undoable property change recording is enabled for the
	 * current thread.
	 *
	 * @return true if active
	 */
	public static boolean getCreateUndoablePropertyChanges() {
		return OARuntime.get().threadService().getCreateUndoablePropertyChanges();
	}

	/**
	 * Begins a compound undoable sequence with the specified name for the
	 * current thread.
	 *
	 * @param compoundName the descriptive name for the compound edit
	 */
	public static void startUndoable(String compoundName) {
		OARuntime.get().threadService().startUndoable(compoundName);
	}

	/**
	 * Completes the current compound undoable sequence for the thread.
	 */
	public static void endUndoable() {
		OARuntime.get().threadService().endUndoable();
	}

	/**
	 * Convenience wrapper for starting a compound undoable sequence using the
	 * specified name.
	 *
	 * @param compoundName the compound edit name
	 */
	public static void startCompoundUndoable(String compoundName) {
		OARuntime.get().threadService().startCompoundUndoable(compoundName);
	}

	public static boolean isCreatingCompoundUndoable() {
		return OARuntime.get().threadService().isCreatingCompoundUndoable();
	}
	
	/**
	 * Convenience wrapper that ends the current compound undoable sequence
	 * for the thread.
	 */
	public static void endCompoundUndoable() {
		OARuntime.get().threadService().endCompoundUndoable();
	}

	/**
	 * Registers the specified sibling helper for the current thread.
	 *
	 * @param sh the sibling helper
	 * @return true if added, false if already present
	 */
	public static boolean addSiblingHelper(OASiblingHelper sh) {
		return OARuntime.get().threadService().addSiblingHelper(sh);
	}

	/**
	 * Removes the specified sibling helper from the current thread's list.
	 *
	 * @param sh the sibling helper to remove
	 */
	public static void removeSiblingHelper(OASiblingHelper sh) {
		OARuntime.get().threadService().removeSiblingHelper(sh);
	}

	/**
	 * Returns the list of sibling helpers associated with the current thread,
	 * or null if none exist.
	 *
	 * @return the list of sibling helpers, or null
	 */
	public static ArrayList<OASiblingHelper> getSiblingHelpers() {
		return OARuntime.get().threadService().getSiblingHelpers();
	}

	/**
	 * Returns whether the current thread has any registered sibling helpers.
	 *
	 * @return true if one or more sibling helpers exist
	 */
	public static boolean hasSiblingHelpers() {
		return OARuntime.get().threadService().hasSiblingHelpers();
	}

	/**
	 * Removes all sibling helpers from the current thread.
	 */
	public static void clearSiblingHelpers() {
		OARuntime.get().threadService().clearSiblingHelpers();
	}

	/**
	 * Logs a throttled warning message when sufficient time has passed since
	 * the previous log for the same category.
	 *
	 * @param msg    the message to log
	 * @param msLast the timestamp of the previous logged message
	 * @return the updated timestamp
	 */
	public static long throttleLOG(String msg, long msLast) {
		return OARuntime.get().threadService().throttleLOG(msg, msLast);
	}

	/**
	 * Returns a formatted dump of all stack traces for all threads.
	 *
	 * @return the formatted stack trace dump
	 */
	public static String getAllStackTraces() {
		return OARuntime.get().threadService().getAllStackTraces();
	}

	/**
	 * Returns the current thread’s stack trace formatted as a string.
	 *
	 * @return the stack trace for the current thread
	 */
	public static String getThreadDump() {
		return OARuntime.get().threadService().getThreadDump();
	}

	/**
	 * Sets the status message for the current thread-local instance.
	 *
	 * @param msg the status message
	 */
	public static void setStatus(String msg) {
		OARuntime.get().threadService().setStatus(msg);
	}

	/**
	 * Assigns the remote request information for the current thread.
	 *
	 * @param ri the request information
	 */
	public static void setRemoteRequestInfo(RequestInfo ri) {
		OARuntime.get().threadService().setRemoteRequestInfo(ri);
	}

	/**
	 * Returns the remote request information for the current thread.
	 *
	 * @return the RequestInfo instance, or null if none exists
	 */
	public static RequestInfo getRemoteRequestInfo() {
		return OARuntime.get().threadService().getRemoteRequestInfo();
	}

	/**
	 * Enables or disables sending of messages from OARemoteThread instances.
	 *
	 * @param b true to enable sending, false to disable
	 * @return the previous send-messages state
	 */
	public static boolean setSendMessages(boolean b) {
		return OARuntime.get().threadService().setSendMessages(b);
	}

	/**
	 * Assigns or clears an object used to notify the current thread when
	 * remote-thread operations need to resume.
	 *
	 * @param obj the object to assign, or null to clear
	 */
	public static void setNotifyObject(Object obj) {
		OARuntime.get().threadService().setNotifyObject(obj);
	}

	/**
	 * Notifies the thread waiting on the thread-local notify object, if one
	 * exists, and clears the notify reference.
	 */
	public static void notifyWaitingThread() {
		OARuntime.get().threadService().notifyWaitingThread();
	}

	/**
	 * Returns the recursive trigger count for the current thread.
	 *
	 * @return the trigger count
	 */
	public static int getRecursiveTriggerCount() {
		return OARuntime.get().threadService().getRecursiveTriggerCount();
	}


	/**
	 * Sets the recursive trigger count for the current thread-local instance.
	 *
	 * @param x the trigger count to assign
	 */
	public static void setRecursiveTriggerCount(int x) {
		OARuntime.get().threadService().setRecursiveTriggerCount(x);
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	public static int getHubListenerTreeCount() {
		return OARuntime.get().threadService().getHubListenerTreeCount();
	}


	/**
	 * Increments or decrements the hub-listener tree depth for the current thread.
	 *
	 * @param b true to increment, false to decrement
	 */
	public static void setHubListenerTree(boolean b) {
		OARuntime.get().threadService().setHubListenerTree(b);
	}

	/**
	 * Sets the property name to ignore during tree-listener processing for the
	 * current thread.
	 *
	 * @param prop the property name to ignore
	 */
	public static void setIgnoreTreeListenerProperty(String prop) {
		OARuntime.get().threadService().setIgnoreTreeListenerProperty(prop);
	}

	/**
	 * Returns the property name currently ignored by tree listeners for the
	 * thread.
	 *
	 * @return the ignored property name
	 */
	public static String getIgnoreTreeListenerProperty() {
		return OARuntime.get().threadService().getIgnoreTreeListenerProperty();
	}

	/**
	 * Returns the number of OA sync events recorded for the current thread.
	 *
	 * @return the sync event count
	 */
	public static int getOASyncEventCount() {
		return OARuntime.get().threadService().getOASyncEventCount();
	}

	/**
	 * Increments the OA sync event count for the current thread.
	 */
	public static void incrOASyncEventCount() {
		OARuntime.get().threadService().incrOASyncEventCount();
	}

	/**
	 * Returns the most recent HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the latest HubEvent or null
	 */
	public static HubEvent getCurrentHubEvent() {
		return OARuntime.get().threadService().getCurrentHubEvent();
	}

	/**
	 * Returns whether the supplied HubEvent is currently active for the thread.
	 *
	 * @param he the HubEvent to check
	 * @return true if the event is active
	 */
	public static boolean isOpenHubEvent(HubEvent he) {
		return OARuntime.get().threadService().isOpenHubEvent(he);
	}

	/**
	 * Returns the oldest HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the earliest HubEvent or null
	 */
	public static HubEvent getOldestHubEvent() {
		return OARuntime.get().threadService().getOldestHubEvent();
	}

	/**
	 * Adds the supplied HubEvent to the current thread’s active-event list.
	 *
	 * @param he the HubEvent to add
	 */
	public static void addHubEvent(HubEvent he) {
		OARuntime.get().threadService().addHubEvent(he);
	}

	/**
	 * Removes the supplied HubEvent from the current thread’s active-event list
	 * and updates global counters.
	 *
	 * @param he the HubEvent to remove
	 */
	public static void removeHubEvent(HubEvent he) {
		OARuntime.get().threadService().removeHubEvent(he);
	}

	/**
	 * Returns whether the current thread is in the process of sending one or more
	 * HubEvents.
	 *
	 * @return true if event sending is active
	 */
	public static boolean isSendingEvent() {
		return OARuntime.get().threadService().isSendingEvent();
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
		return OARuntime.get().threadService().hasSentCalcPropertyChange(thisHub, thisObj, propertyName);
	}

	/**
	 * Returns the context object associated with the current thread.
	 *
	 * @return the context object
	 */
	public static Object getContext() {
		return OARuntime.get().threadService().getContext();
	}

	/**
	 * Assigns the context object for the current thread.
	 *
	 * @param context the context to assign
	 */
	public static void setContext(Object context) {
		OARuntime.get().threadService().setContext(context);
	}

	
	
	/**
	 * Sets the admin flag for the current thread-local instance.
	 *
	 * @param b true to enable admin mode, false to disable
	 * @return the previous admin flag value
	 */
	public static boolean setAdmin(boolean b) {
		return OARuntime.get().threadService().setAdmin(b);
	}

	/**
	 * Convenience wrapper that sets the admin flag for the current thread-local
	 * instance.
	 *
	 * @param b true to enable admin mode
	 * @return the previous admin flag value
	 */
	public static boolean setIsAdmin(boolean b) {
		return OARuntime.get().threadService().setIsAdmin(b);
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
		return OARuntime.get().threadService().getIsAdmin();
	}


	/**
	 * Copies selected state fields from the supplied thread-local instance into
	 * the current thread-local instance.
	 *
	 * @param tl the thread-local instance to copy from
	 */
	public static void initialize(OAThreadLocal tl) {
		OARuntime.get().threadService().initialize(tl);
	}

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
	public static OAJson getOAJackson() {
		return OARuntime.get().threadService().getOAJackson();
	}

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
	public static OAJson setOAJackson(OAJson jackson) {
		return OARuntime.get().threadService().setOAJackson(jackson);
	}
	
	/**
	 * Registers the supplied hub as one that should not be auto-adjusted for the
	 * current thread.
	 *
	 * @param hub the hub to register
	 */
	public static void addDontAdjustHub(Hub hub) {
		OARuntime.get().threadService().addDontAdjustHub(hub);
	}

	/**
	 * Removes the supplied hub from the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to remove
	 */
	public static void removeDontAdjustHub(Hub hub) {
		OARuntime.get().threadService().removeDontAdjustHub(hub);
	}

	
	/**
	 * Returns whether the supplied hub is eligible for auto-adjustment based on
	 * the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to check
	 * @return true if auto-adjustment is allowed
	 */
	public static boolean getCanAdjustHub(Hub hub) {
		return OARuntime.get().threadService().getCanAdjustHub(hub);
	}

	/**
	 * Returns whether the current thread-local instance is marked as a sync
	 * thread.
	 *
	 * @return true if sync-thread mode is enabled
	 */
	public static boolean isSyncThread() {
		return OARuntime.get().threadService().isSyncThread();
	}

	/**
	 * Enables or disables sync-thread mode for the current thread-local
	 * instance.
	 *
	 * @param b true to enable, false to disable
	 * @return the previous sync-thread flag value
	 */
	public static boolean setSyncThread(boolean b) {
		return OARuntime.get().threadService().setSyncThread(b);
	}
	

	/**
	 * Returns whether the current thread is actively performing a refresh
	 * operation.
	 *
	 * @return true if refreshing
	 */
	public static boolean isRefreshing() {
		return OARuntime.get().threadService().isRefreshing();
	}


	/**
	 * Enables or disables refresh mode for the current thread-local instance.
	 *
	 * @param b true to increment, false to decrement
	 */
	public static void setRefreshing(boolean b) {
		OARuntime.get().threadService().setRefreshing(b);
	}


	/**
	 * Returns the hub used for fast-loading operations for the current thread,
	 * or null if none is assigned.
	 *
	 * @return the fast-loading hub or null
	 */
	public static Hub getFastLoadingHub() {
		return OARuntime.get().threadService().getFastLoadingHub();
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
		return OARuntime.get().threadService().isFastLoadingHub(h);
	}
	
	/**
	 * Assigns or clears the fast-loading hub for the current thread.
	 *
	 * @param hub the hub to assign or null to clear
	 */
	public static void setFastLoadingHub(Hub hub) {
		OARuntime.get().threadService().setFastLoadingHub(hub);
	}

	/**
	 * Returns the process assigned to the current thread-local instance.
	 *
	 * @return the process for the current thread, or null if none is set
	 */
	public static OAProcess getProcess() {
		return OARuntime.get().threadService().getProcess();
	}

	/**
	 * Assigns the specified process to the current thread-local instance.
	 *
	 * @param process the process to assign
	 */
	public static void setProcess(OAProcess process) {
		OARuntime.get().threadService().setProcess(process);
	}
}
