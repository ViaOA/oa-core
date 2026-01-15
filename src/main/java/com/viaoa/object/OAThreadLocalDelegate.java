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
	private static OAThreadLocal getThreadLocal(boolean bCreateIfNull) {
		OAThreadLocal ti = OARuntime.get().threadLocalService().getThreadLocal(bCreateIfNull);
		return ti;
	}

	/**
	 * Sets the current thread's transaction reference and updates the global
	 * transaction counter. Called internally by transaction-related classes.
	 *
	 * @param t the transaction to assign, or null to clear it
	 */
	private static void setTransaction(OATransaction t) {
		OARuntime.get().threadLocalService().setTransaction(t);
	}

	/**
	 * Returns the current thread's active transaction, or null if no
	 * transaction is registered.
	 *
	 * @return the current transaction or null
	 */
	private static OATransaction getTransaction() {
		return OARuntime.get().threadLocalService().getTransaction();
	}

	/**
	 * Returns whether the specified thread-local instance is in a loading state.
	 *
	 * @param ti the thread-local instance
	 * @return true if loading is greater than zero
	 */
	private static boolean isLoading() {
		return OARuntime.get().threadLocalService().isLoading();
	}


	/**
	 * Updates the loading flag for the current thread and returns the previous
	 * loading state.
	 *
	 * @param b true to increase the loading count, false to decrease it
	 * @return previous loading flag before the update
	 */
	private static boolean setLoading(boolean b) {
		return OARuntime.get().threadLocalService().setLoading(b);
	}

	/**
	 * Returns the current thread's object-cache add mode, or the default mode
	 * when no add mode is active.
	 *
	 * @return the current add mode
	 */
	private static int getObjectCacheAddMode() {
		return OARuntime.get().threadLocalService().getObjectCacheAddMode();
	}


	/**
	 * Sets the object-cache add mode for the current thread and updates the
	 * global counter when transitioning between active and default modes.
	 *
	 * @param mode the add mode to assign
	 */
	private static void setObjectCacheAddMode(int mode) {
		OARuntime.get().threadLocalService().setObjectCacheAddMode(mode);
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
	private static OAObjectSerializer getObjectSerializer() {
		return OARuntime.get().threadLocalService().getObjectSerializer();
	}

	/**
	 * Sets the object serializer for the current thread and updates global
	 * serializer counters.
	 *
	 * @param si the serializer to assign, or null to clear it
	 */
	private static void setObjectSerializer(OAObjectSerializer si) {
		// LOG.finer("OAObjectSerializer="+(si != null));
		OARuntime.get().threadLocalService().setObjectSerializer(si);
	}

	/**
	 * Returns whether the specified thread-local instance has message
	 * suppression enabled.
	 *
	 * @param ti the thread-local instance
	 * @return true if suppressed, otherwise false
	 */
	private static boolean isSuppressCSMessages() {
		return OARuntime.get().threadLocalService().isSuppressCSMessages();
	}

	/**
	 * Enables or disables suppression of client/server messages for the
	 * current thread.
	 *
	 * @param b true to enable suppression, false to disable
	 */
	private static void setSuppressCSMessages(boolean b) {
		OARuntime.get().threadLocalService().setSuppressCSMessages(b);
	}

	/**
	 * Returns whether the current thread is in a deleting state.
	 *
	 * @return true if deleting is active for this thread
	 */
	private static boolean isDeleting() {
		return OARuntime.get().threadLocalService().isDeleting();
	}

	/**
	 * Returns whether the current thread is deleting the specified object.
	 *
	 * @param obj the object to check
	 * @return true if this thread is deleting the object
	 */
	private static boolean isDeleting(Object obj) {
		return OARuntime.get().threadLocalService().isDeleting(obj);
	}

	/**
	 * Returns whether the given thread-local instance is deleting the specified
	 * object.
	 *
	 * @param ti  the thread-local instance
	 * @param obj the object to check
	 * @return true if the instance is deleting the object
	 */
	private static boolean isThreadDeleting(Object obj) {
		return OARuntime.get().threadLocalService().isThreadDeleting(obj);
	}


	/**
	 * Adds or removes the specified object from the global deleting map and
	 * updates the deleting state for the current thread.
	 *
	 * @param obj the object to update
	 * @param b   true to mark as deleting, false to unmark
	 */
	private static void setDeleting(Object obj, boolean b) {
		OARuntime.get().threadLocalService().setDeleting(obj, b);
	}

	/**
	 * Returns whether the specified flag object exists in the current
	 * thread-local flag list.
	 *
	 * @param obj the flag object
	 * @return true if present
	 */
	private static boolean isFlag(Object obj) {
		return OARuntime.get().threadLocalService().isFlag(obj);
	}
	
	/**
	 * Adds the specified flag object to the current thread-local flag list.
	 *
	 * @param obj the flag object to add
	 */
	private static void setFlag(Object obj) {
		OARuntime.get().threadLocalService().setFlag(obj);
	}

	/**
	 * Removes the specified flag object from the current thread-local flag list.
	 *
	 * @param obj the flag to remove
	 */
	private static void removeFlag(Object obj) {
		OARuntime.get().threadLocalService().removeFlag(obj);
	}

	/**
	 * Acquires a lock on the specified object with a maximum number of wait
	 * attempts before force-acquiring the lock.
	 *
	 * @param object        the object to lock
	 * @param maxWaitTries  maximum wait attempts before taking the lock
	 */
	private static void lock(Object object, int maxWaitTries) {
		OARuntime.get().threadLocalService().lock(object, maxWaitTries);
	}

	/**
	 * Acquires a lock on the specified object using a default maximum wait
	 * threshold.
	 *
	 * @param object the object to lock
	 */
	private static void lock(Object object) {
		OARuntime.get().threadLocalService().lock(object);
	}

	/**
	 * Returns whether the current thread holds any locks.
	 *
	 * @return true if one or more locks are held
	 */
	private static boolean hasLock() {
		return OARuntime.get().threadLocalService().hasLock();
	}

	/**
	 * Returns whether the current thread holds a lock on the specified object.
	 *
	 * @param obj the object to check
	 * @return true if the lock is held
	 */
	private static boolean hasLock(Object obj) {
		return OARuntime.get().threadLocalService().hasLock(obj);
	}

	/**
	 * Returns all lock objects currently held by the thread.
	 *
	 * @return an array of locked objects, or null if none
	 */
	private static Object[] getLocks() {
		return OARuntime.get().threadLocalService().getLocks();	
	}

	/**
	 * Returns whether any thread currently holds a lock on the specified object.
	 *
	 * @param object the object to check
	 * @return true if locked by any thread
	 */
	private static boolean isLocked(Object object) {
		return OARuntime.get().threadLocalService().isLocked(object);
	}

	/**
	 * Returns whether the current thread is the owner of the lock for the
	 * specified object.
	 *
	 * @param object the object to check
	 * @return true if the current thread owns the lock
	 */
	private static boolean isLockOwner(Object object) {
		return OARuntime.get().threadLocalService().isLockOwner(object);
	}

	/**
	 * Returns the thread-local OAThreadLocal instance for the current thread,
	 * creating one if necessary.
	 *
	 * @return the thread-local instance
	 */
	private static OAThreadLocal getOAThreadLocal() {
		return OARuntime.get().threadLocalService().getThreadLocal(true);
	}

	/**
	 * Returns the number of detected deadlocks encountered during lock
	 * acquisition.
	 *
	 * @return the deadlock count
	 */
	private static int getDeadlockCount() {
		return OARuntime.get().threadLocalService().getDeadlockCount();
	}

	/**
	 * Releases all locks currently held by the thread-local instance for the
	 * current thread.
	 */
	private static void releaseAllLocks() {
		OARuntime.get().threadLocalService().releaseAllLocks();
	}

	/**
	 * Releases the lock held by the current thread on the specified object.
	 *
	 * @param object the object to unlock
	 */
	private static void unlock(Object object) {
		OARuntime.get().threadLocalService().unlock(object);
	}

	// HubListenerTree uses this to ignore dependent property changes caused by add/remove objects from hubMerger.hubMaster
	/**
	 * Returns whether any thread is currently modifying hub-merger state.
	 *
	 * @return true if hub-merger updates are active
	 */
	private static boolean isHubMergerChanging() {
		return OARuntime.get().threadLocalService().isHubMergerChanging();
	}

	/**
	 * Enables or disables hub-merger change tracking for the current thread.
	 *
	 * @param b true to enable, false to disable
	 */
	private static void setHubMergerChanging(boolean b) {
		OARuntime.get().threadLocalService().setHubMergerChanging(b);
	}


	/**
	 * Registers a callback to be executed once hub-merger changes finish for
	 * the current thread.
	 *
	 * @param cb the callback to register
	 */
    private static void addHubMergerCallback(OAThreadLocalHubMergerCallback cb) {
		OARuntime.get().threadLocalService().addHubMergerCallback(cb);
    }
    
    /**
     * Enables or disables recording of undoable property changes for the
     * current thread.
     *
     * @param b true to enable, false to disable
     */
	private static void setCreateUndoablePropertyChanges(boolean b) {
		OARuntime.get().threadLocalService().setCreateUndoablePropertyChanges(b);
	}

	/**
	 * Returns whether undoable property change recording is enabled for the
	 * current thread.
	 *
	 * @return true if active
	 */
	private static boolean getCreateUndoablePropertyChanges() {
		return OARuntime.get().threadLocalService().getCreateUndoablePropertyChanges();
	}

	/**
	 * Begins a compound undoable sequence with the specified name for the
	 * current thread.
	 *
	 * @param compoundName the descriptive name for the compound edit
	 */
	private static void startUndoable(String compoundName) {
		OARuntime.get().threadLocalService().startUndoable(compoundName);
	}

	/**
	 * Completes the current compound undoable sequence for the thread.
	 */
	private static void endUndoable() {
		OARuntime.get().threadLocalService().endUndoable();
	}

	/**
	 * Convenience wrapper for starting a compound undoable sequence using the
	 * specified name.
	 *
	 * @param compoundName the compound edit name
	 */
	private static void startCompoundUndoable(String compoundName) {
		OARuntime.get().threadLocalService().startCompoundUndoable(compoundName);
	}

	private static boolean isCreatingCompoundUndoable() {
		return OARuntime.get().threadLocalService().isCreatingCompoundUndoable();
	}
	
	/**
	 * Convenience wrapper that ends the current compound undoable sequence
	 * for the thread.
	 */
	private static void endCompoundUndoable() {
		OARuntime.get().threadLocalService().endCompoundUndoable();
	}

	/**
	 * Registers the specified sibling helper for the current thread.
	 *
	 * @param sh the sibling helper
	 * @return true if added, false if already present
	 */
	private static boolean addSiblingHelper(OASiblingHelper sh) {
		return OARuntime.get().threadLocalService().addSiblingHelper(sh);
	}

	/**
	 * Removes the specified sibling helper from the current thread's list.
	 *
	 * @param sh the sibling helper to remove
	 */
	private static void removeSiblingHelper(OASiblingHelper sh) {
		OARuntime.get().threadLocalService().removeSiblingHelper(sh);
	}

	/**
	 * Returns the list of sibling helpers associated with the current thread,
	 * or null if none exist.
	 *
	 * @return the list of sibling helpers, or null
	 */
	private static ArrayList<OASiblingHelper> getSiblingHelpers() {
		return OARuntime.get().threadLocalService().getSiblingHelpers();
	}

	/**
	 * Returns whether the current thread has any registered sibling helpers.
	 *
	 * @return true if one or more sibling helpers exist
	 */
	private static boolean hasSiblingHelpers() {
		return OARuntime.get().threadLocalService().hasSiblingHelpers();
	}

	/**
	 * Removes all sibling helpers from the current thread.
	 */
	private static void clearSiblingHelpers() {
		OARuntime.get().threadLocalService().clearSiblingHelpers();
	}

	/**
	 * Logs a throttled warning message when sufficient time has passed since
	 * the previous log for the same category.
	 *
	 * @param msg    the message to log
	 * @param msLast the timestamp of the previous logged message
	 * @return the updated timestamp
	 */
	private static long throttleLOG(String msg, long msLast) {
		return OARuntime.get().threadLocalService().throttleLOG(msg, msLast);
	}

	/**
	 * Returns a formatted dump of all stack traces for all threads.
	 *
	 * @return the formatted stack trace dump
	 */
	private static String getAllStackTraces() {
		return OARuntime.get().threadLocalService().getAllStackTraces();
	}

	/**
	 * Returns the current thread’s stack trace formatted as a string.
	 *
	 * @return the stack trace for the current thread
	 */
	private static String getThreadDump() {
		return OARuntime.get().threadLocalService().getThreadDump();
	}

	/**
	 * Sets the status message for the current thread-local instance.
	 *
	 * @param msg the status message
	 */
	private static void setStatus(String msg) {
		OARuntime.get().threadLocalService().setStatus(msg);
	}

	/**
	 * Assigns the remote request information for the current thread.
	 *
	 * @param ri the request information
	 */
	private static void setRemoteRequestInfo(RequestInfo ri) {
		OARuntime.get().threadLocalService().setRemoteRequestInfo(ri);
	}

	/**
	 * Returns the remote request information for the current thread.
	 *
	 * @return the RequestInfo instance, or null if none exists
	 */
	private static RequestInfo getRemoteRequestInfo() {
		return OARuntime.get().threadLocalService().getRemoteRequestInfo();
	}

	/**
	 * Enables or disables sending of messages from OARemoteThread instances.
	 *
	 * @param b true to enable sending, false to disable
	 * @return the previous send-messages state
	 */
	private static boolean setSendMessages(boolean b) {
		return OARuntime.get().threadLocalService().setSendMessages(b);
	}

	/**
	 * Assigns or clears an object used to notify the current thread when
	 * remote-thread operations need to resume.
	 *
	 * @param obj the object to assign, or null to clear
	 */
	private static void setNotifyObject(Object obj) {
		OARuntime.get().threadLocalService().setNotifyObject(obj);
	}

	/**
	 * Notifies the thread waiting on the thread-local notify object, if one
	 * exists, and clears the notify reference.
	 */
	private static void notifyWaitingThread() {
		OARuntime.get().threadLocalService().notifyWaitingThread();
	}

	/**
	 * Returns the recursive trigger count for the current thread.
	 *
	 * @return the trigger count
	 */
	private static int getRecursiveTriggerCount() {
		return OARuntime.get().threadLocalService().getRecursiveTriggerCount();
	}


	/**
	 * Sets the recursive trigger count for the current thread-local instance.
	 *
	 * @param x the trigger count to assign
	 */
	private static void setRecursiveTriggerCount(int x) {
		OARuntime.get().threadLocalService().setRecursiveTriggerCount(x);
	}

	/**
	 * Returns the hub-listener tree depth for the current thread. Returns zero
	 * when no depth has been recorded.
	 *
	 * @return the hub-listener tree count
	 */
	private static int getHubListenerTreeCount() {
		return OARuntime.get().threadLocalService().getHubListenerTreeCount();
	}


	/**
	 * Increments or decrements the hub-listener tree depth for the current thread.
	 *
	 * @param b true to increment, false to decrement
	 */
	private static void setHubListenerTree(boolean b) {
		OARuntime.get().threadLocalService().setHubListenerTree(b);
	}

	/**
	 * Sets the property name to ignore during tree-listener processing for the
	 * current thread.
	 *
	 * @param prop the property name to ignore
	 */
	private static void setIgnoreTreeListenerProperty(String prop) {
		OARuntime.get().threadLocalService().setIgnoreTreeListenerProperty(prop);
	}

	/**
	 * Returns the property name currently ignored by tree listeners for the
	 * thread.
	 *
	 * @return the ignored property name
	 */
	private static String getIgnoreTreeListenerProperty() {
		return OARuntime.get().threadLocalService().getIgnoreTreeListenerProperty();
	}

	/**
	 * Returns the number of OA sync events recorded for the current thread.
	 *
	 * @return the sync event count
	 */
	private static int getOASyncEventCount() {
		return OARuntime.get().threadLocalService().getOASyncEventCount();
	}

	/**
	 * Increments the OA sync event count for the current thread.
	 */
	private static void incrOASyncEventCount() {
		OARuntime.get().threadLocalService().incrOASyncEventCount();
	}

	/**
	 * Returns the most recent HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the latest HubEvent or null
	 */
	private static HubEvent getCurrentHubEvent() {
		return OARuntime.get().threadLocalService().getCurrentHubEvent();
	}

	/**
	 * Returns whether the supplied HubEvent is currently active for the thread.
	 *
	 * @param he the HubEvent to check
	 * @return true if the event is active
	 */
	private static boolean isOpenHubEvent(HubEvent he) {
		return OARuntime.get().threadLocalService().isOpenHubEvent(he);
	}

	/**
	 * Returns the oldest HubEvent for the current thread, or null when none
	 * exist.
	 *
	 * @return the earliest HubEvent or null
	 */
	private static HubEvent getOldestHubEvent() {
		return OARuntime.get().threadLocalService().getOldestHubEvent();
	}

	/**
	 * Adds the supplied HubEvent to the current thread’s active-event list.
	 *
	 * @param he the HubEvent to add
	 */
	private static void addHubEvent(HubEvent he) {
		OARuntime.get().threadLocalService().addHubEvent(he);
	}

	/**
	 * Removes the supplied HubEvent from the current thread’s active-event list
	 * and updates global counters.
	 *
	 * @param he the HubEvent to remove
	 */
	private static void removeHubEvent(HubEvent he) {
		OARuntime.get().threadLocalService().removeHubEvent(he);
	}

	/**
	 * Returns whether the current thread is in the process of sending one or more
	 * HubEvents.
	 *
	 * @return true if event sending is active
	 */
	private static boolean isSendingEvent() {
		return OARuntime.get().threadLocalService().isSendingEvent();
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
	private static boolean hasSentCalcPropertyChange(Hub thisHub, OAObject thisObj, String propertyName) {
		return OARuntime.get().threadLocalService().hasSentCalcPropertyChange(thisHub, thisObj, propertyName);
	}

	/**
	 * Returns the context object associated with the current thread.
	 *
	 * @return the context object
	 */
	private static Object getContext() {
		return OARuntime.get().threadLocalService().getContext();
	}

	/**
	 * Assigns the context object for the current thread.
	 *
	 * @param context the context to assign
	 */
	private static void setContext(Object context) {
		OARuntime.get().threadLocalService().setContext(context);
	}

	
	
	/**
	 * Sets the admin flag for the current thread-local instance.
	 *
	 * @param b true to enable admin mode, false to disable
	 * @return the previous admin flag value
	 */
	private static boolean setAdmin(boolean b) {
		return OARuntime.get().threadLocalService().setAdmin(b);
	}

	/**
	 * Convenience wrapper that sets the admin flag for the current thread-local
	 * instance.
	 *
	 * @param b true to enable admin mode
	 * @return the previous admin flag value
	 */
	private static boolean setIsAdmin(boolean b) {
		return OARuntime.get().threadLocalService().setIsAdmin(b);
	}


	/**
	 * Returns whether the current thread-local instance is in admin mode.
	 *
	 * @return true if admin mode is enabled
	 */
	private static boolean isAdmin() {
		return getIsAdmin();
	}

	/**
	 * Returns whether the current thread-local instance is in admin mode.
	 *
	 * @return true if admin mode is enabled
	 */
	private static boolean getIsAdmin() {
		return OARuntime.get().threadLocalService().getIsAdmin();
	}


	/**
	 * Copies selected state fields from the supplied thread-local instance into
	 * the current thread-local instance.
	 *
	 * @param tl the thread-local instance to copy from
	 */
	private static void initialize(OAThreadLocal tl) {
		OARuntime.get().threadLocalService().initialize(tl);
	}

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
	private static OAJson getOAJackson() {
		return OARuntime.get().threadLocalService().getOAJackson();
	}

	/**
	 * Assigns the supplied OAJson instance to the current thread-local instance
	 * and returns the previous value.
	 *
	 * @param jackson the new OAJson instance or null
	 * @return the previous OAJson instance
	 */
	private static OAJson setOAJackson(OAJson jackson) {
		return OARuntime.get().threadLocalService().setOAJackson(jackson);
	}
	
	/**
	 * Registers the supplied hub as one that should not be auto-adjusted for the
	 * current thread.
	 *
	 * @param hub the hub to register
	 */
	private static void addDontAdjustHub(Hub hub) {
		OARuntime.get().threadLocalService().addDontAdjustHub(hub);
	}

	/**
	 * Removes the supplied hub from the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to remove
	 */
	private static void removeDontAdjustHub(Hub hub) {
		OARuntime.get().threadLocalService().removeDontAdjustHub(hub);
	}

	
	/**
	 * Returns whether the supplied hub is eligible for auto-adjustment based on
	 * the current thread-local do-not-adjust list.
	 *
	 * @param hub the hub to check
	 * @return true if auto-adjustment is allowed
	 */
	private static boolean getCanAdjustHub(Hub hub) {
		return OARuntime.get().threadLocalService().getCanAdjustHub(hub);
	}

	/**
	 * Returns whether the current thread-local instance is marked as a sync
	 * thread.
	 *
	 * @return true if sync-thread mode is enabled
	 */
	private static boolean isSyncThread() {
		return OARuntime.get().threadLocalService().isSyncThread();
	}

	/**
	 * Enables or disables sync-thread mode for the current thread-local
	 * instance.
	 *
	 * @param b true to enable, false to disable
	 * @return the previous sync-thread flag value
	 */
	private static boolean setSyncThread(boolean b) {
		return OARuntime.get().threadLocalService().setSyncThread(b);
	}
	

	/**
	 * Returns whether the current thread is actively performing a refresh
	 * operation.
	 *
	 * @return true if refreshing
	 */
	private static boolean isRefreshing() {
		return OARuntime.get().threadLocalService().isRefreshing();
	}


	/**
	 * Enables or disables refresh mode for the current thread-local instance.
	 *
	 * @param b true to increment, false to decrement
	 */
	private static void setRefreshing(boolean b) {
		OARuntime.get().threadLocalService().setRefreshing(b);
	}


	/**
	 * Returns the hub used for fast-loading operations for the current thread,
	 * or null if none is assigned.
	 *
	 * @return the fast-loading hub or null
	 */
	private static Hub getFastLoadingHub() {
		return OARuntime.get().threadLocalService().getFastLoadingHub();
	}

	/**
	 * Convenience check that returns true when the hub has fast-loading enabled.
	 * This is determined by consulting {@link Hub#getFastLoading()} on the
	 * supplied hub instance.
	 *
	 * @param h the hub to inspect
	 * @return true if the hub reports fast-loading enabled, otherwise false
	 */
	private static boolean isFastLoadingHub(Hub h) {
		return OARuntime.get().threadLocalService().isFastLoadingHub(h);
	}
	
	/**
	 * Assigns or clears the fast-loading hub for the current thread.
	 *
	 * @param hub the hub to assign or null to clear
	 */
	private static void setFastLoadingHub(Hub hub) {
		OARuntime.get().threadLocalService().setFastLoadingHub(hub);
	}

	/**
	 * Returns the process assigned to the current thread-local instance.
	 *
	 * @return the process for the current thread, or null if none is set
	 */
	private static OAProcess getProcess() {
		return OARuntime.get().threadLocalService().getProcess();
	}

	/**
	 * Assigns the specified process to the current thread-local instance.
	 *
	 * @param process the process to assign
	 */
	private static void setProcess(OAProcess process) {
		OARuntime.get().threadLocalService().setProcess(process);
	}
}
