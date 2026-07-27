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
package com.viaoa.runtime.thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.viaoa.hub.*;
import com.viaoa.lang.Tuple3;
import com.viaoa.oa.OA;
import com.viaoa.oa.sibling.OASiblingHelper;
import com.viaoa.object.OAObject;
import com.viaoa.process.OAProcess;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.session.OASessionUser;
import com.viaoa.transaction.OATransaction;

/**
 * Thread-scoped state container used internally by OA to manage execution context
 * and operational flags on a per-thread basis.
 *
 * <p>This holds lightweight, mutable metadata including:
 * <ul>
 *   <li>Object OA model loading and deletion state</li>
 *   <li>Serialization modes</li>
 *   <li>User/session context and admin privileges</li>
 *   <li>Hub event traversal depth & suppression flags</li>
 *   <li>Undo/calc change batching</li>
 *   <li>Distributed sync and process tracking</li>
 *   <li>Thread participation in object locking</li>
 * </ul>
 *
 * <p>Instances are automatically created on-demand by
 * {@link OAThreadLocalDelegate} and should never be accessed directly by
 * application code. All external interaction must go through delegate APIs,
 * ensuring proper reference counting and fast-path optimizations.
 *
 * @author vvia
 * @see OAThreadLocalDelegate
 */
public class OAThreadLocal {

	/**
	 * The name of the thread that owns this context instance. Assigned at
	 * construction time and used for diagnostic and logging purposes.
	 */
	private String threadName;
	
	/**
	 * Optional status message associated with the thread’s current OA activity.
	 * Used for debugging, performance tracing, or monitoring long-running tasks.
	 */
	private String status;
	
	/**
	 * Time value used for tracking duration of thread-scoped operations. Its
	 * specific interpretation depends on the OA subsystem using it.
	 */
	private long time;

	/**
	 * Stack-like array used to track objects currently in the process of being
	 * deleted. Prevents cyclic delete cascades and suppresses redundant events.
	 */
	private Object[] deleting;

	// current mode for used by OAObjectCache
	// see: OAObjectCacheDelegate for list of mode
	/**
	 * Controls how OAObjectCacheDelegate adds objects to the cache. A value of
	 * {@code 0} means the delegate will fall back to its default add mode.
	 */
	private int cacheAddMode; // 0 means that it has not been set and will use OAObjectCacheDelegate.DefaultAddMode

	/**
	 * The active transaction for this thread. Used to group object changes,
	 * manage commit/rollback behavior, and propagate transactional events.
	 */
	protected OATransaction transaction;

	/**
	 * The serializer currently active on this thread. Used when serializing
	 * objects for remote calls, sync messages, or internal wrappers.
	 */
	private List<OAObjectSerializer> alObjectSerializer;

	// flag to know if hub events can be ignored, since hubMerger is doing an internal operation.
	//      Otherwise, there would be a lot of extra unneeded events.
	//      used by HubMerger and HubListenerTree
	/**
	 * Counter used to suppress Hub events while HubMerger performs internal
	 * operations. Prevents recursive or duplicate event propagation.
	 */
	private int hubMergerChangingCount;

	/**
	 * Counter indicating that HubEventDelegate is actively dispatching an
	 * event. Ensures calc-property events are emitted only once per change.
	 */
	protected int sendingEvent; // HubEventDelegate is sending an event.  Used so that calcPropertyEvents (see HubListenerTree) are only sent out once

	/**
	 * Tracks how deeply HubListenerTree processing is nested on this thread.
	 * Used to avoid reentrant listener traversal and redundant notifications.
	 */
	private int hubListenerTreeCount; // tracks how deep listeners are for a single listener

	/**
	 * When set, identifies a property whose tree-listener callbacks should be
	 * temporarily ignored. Used during complex merge and update operations.
	 */
	private String ignoreTreeListenerProperty;

	/**
	 * Counter indicating that objects are currently being loaded from a
	 * DataSource. While non-zero, verification, listeners, sync events, and
	 * certain Hub behaviors are suppressed.
	 */
	protected boolean loading;

	/**
	 * Used by OASync to know if sync message should be sent to others. 
	 */
	protected boolean sendSyncMessages = true;

	/** 
	 * Used to track OAThreadLocal.startServerOnly and endServerOnly processing.
	 */
	protected int cntStartServerOnly;
	/**
	 * Runtime state field used by OA services for sendSyncMessagesHold.
	 */
	protected boolean sendSyncMessagesHold;
	
	/**
	 * Flag to know that an object key property is being assigned
	 */
	// protected int assigningObjectKey;
	// use OAObjectDSDelegate.setAssigningId(..)

	// 20110104
	/**
	 * The set of object-level locks held by this thread. Accessed only through
	 * OAThreadLocalDelegate, which applies the appropriate read/write locking.
	 */
	private volatile Object[] locks;

	/**
	 * Indicates whether this thread is currently blocked while attempting to
	 * acquire the final lock in its lock chain.
	 */
	private boolean bIsWaitingOnLock; // used on last lock - which is the only one that this could be waiting on.

	/**
	 * Generic array of thread-scoped flags used internally by OA subsystems to
	 * store lightweight state without allocating dedicated fields.
	 */
	private Object[] flags;

	/**
	 * Indicates whether property changes performed on this thread should be
	 * captured as undoable operations by OAUndoableManager.
	 */
	private boolean createUndoablePropertyChanges;

	/**
	 * Optional name assigned to a compound undoable operation, grouping a
	 * sequence of property changes under a single undoable entry.
	 */
	private String compoundUndoableName;
	
	/**
	 * Array of pending calc-property event descriptors. These are queued while
	 * changes are being processed and dispatched once calc-event suppression
	 * rules allow them.
	 */
	private Tuple3<Hub, OAObject, String>[] calcPropertyEvents;

	/**
	 * Creates a new thread-local context instance and initializes the
	 * threadName field using the current thread's name.
	 */
	public OAThreadLocal() {
		this.setThreadName(Thread.currentThread().getName());
	}

	/**
	 * Returns the Time value.
	 *
	 * @return the Time value
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the Time value.
	 * @param time the Time value
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Returns the Transaction value.
	 *
	 * @return the Transaction value
	 */
	public OATransaction getTransaction() {
		return transaction;
	}

	/**
	 * Sets the Transaction value.
	 * @param transaction the Transaction value
	 */
	public void setTransaction(OATransaction transaction) {
		this.transaction = transaction;
	}

	/**
	 * Returns the Loading value.
	 *
	 * @return the Loading value
	 */
	public boolean getLoading() {
		return loading;
	}

	/**
	 * Sets the Loading value.
	 * @param loading the Loading value
	 */
	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	/**
	 * Returns the CacheAddMode value.
	 *
	 * @return the CacheAddMode value
	 */
	public int getCacheAddMode() {
		return cacheAddMode;
	}

	/**
	 * Sets the CacheAddMode value.
	 * @param cacheAddMode the CacheAddMode value
	 */
	public void setCacheAddMode(int cacheAddMode) {
		this.cacheAddMode = cacheAddMode;
	}

	/**
	 * Returns the ObjectSerializers value.
	 *
	 * @return the ObjectSerializers value
	 */
	public List<OAObjectSerializer> getObjectSerializers() {
		return alObjectSerializer;
	}

	/**
	 * Performs the addObjectSerializer runtime operation.
	 * @param objectSerializer the operation value
	 */
	public void addObjectSerializer(OAObjectSerializer objectSerializer) {
		if (alObjectSerializer == null) alObjectSerializer = new ArrayList();
		alObjectSerializer.add(objectSerializer);
	}
	
	/**
	 * Removes the supplied runtime value.
	 * @param objectSerializer the value to remove
	 * @return removal result
	 */
	public boolean removeObjectSerializer(OAObjectSerializer objectSerializer) {
		if (alObjectSerializer == null) return false;
		return alObjectSerializer.remove(objectSerializer);
	}
	

	/**
	 * Returns the SendSyncMessages value.
	 *
	 * @return the SendSyncMessages value
	 */
	public boolean getSendSyncMessages() {
		return sendSyncMessages;
	}

	/**
	 * Sets the SendSyncMessages value.
	 * @param b the SendSyncMessages value
	 */
	public void setSendSyncMessages(boolean b) {
		this.sendSyncMessages = b;
	}

	/**
	 * Returns the SendSyncMessagesHold value.
	 *
	 * @return the SendSyncMessagesHold value
	 */
	public boolean getSendSyncMessagesHold() {
		return sendSyncMessagesHold;
	}

	/**
	 * Sets the SendSyncMessagesHold value.
	 * @param b the SendSyncMessagesHold value
	 */
	public void setSendSyncMessagesHold(boolean b) {
		this.sendSyncMessagesHold = b;
	}
	
	/**
	 * Increments the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int incStartServerOnly() {
		return ++cntStartServerOnly;
	}
	/**
	 * Decrements the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int decStartServerOnly() {
		if (cntStartServerOnly == 0) return 0; 
		return --cntStartServerOnly;
	}
	
	/**
	 * Returns the Deleting value.
	 *
	 * @return the Deleting value
	 */
	public Object[] getDeleting() {
		return deleting;
	}

	/**
	 * Sets the Deleting value.
	 * @param deleting the Deleting value
	 */
	public void setDeleting(Object[] deleting) {
		this.deleting = deleting;
	}

	/**
	 * Returns the Flags value.
	 *
	 * @return the Flags value
	 */
	public Object[] getFlags() {
		return flags;
	}

	/**
	 * Sets the Flags value.
	 * @param flags the Flags value
	 */
	public void setFlags(Object[] flags) {
		this.flags = flags;
	}

	/**
	 * Returns the Locks value.
	 *
	 * @return the Locks value
	 */
	public Object[] getLocks() {
		return locks;
	}

	/**
	 * Sets the Locks value.
	 * @param locks the Locks value
	 */
	public void setLocks(Object[] locks) {
		this.locks = locks;
	}

	/**
	 * Returns the WaitingOnLock value.
	 *
	 * @return the WaitingOnLock value
	 */
	public boolean getWaitingOnLock() {
		return bIsWaitingOnLock;
	}

	/**
	 * Sets the WaitingOnLock value.
	 * @param bIsWaitingOnLock the WaitingOnLock value
	 */
	public void setWaitingOnLock(boolean bIsWaitingOnLock) {
		this.bIsWaitingOnLock = bIsWaitingOnLock;
	}

	/**
	 * Returns the ThreadName value.
	 *
	 * @return the ThreadName value
	 */
	public String getThreadName() {
		return threadName;
	}

	/**
	 * Sets the ThreadName value.
	 * @param threadName the ThreadName value
	 */
	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	/**
	 * Returns the HubMergerChangingCount value.
	 *
	 * @return the HubMergerChangingCount value
	 */
	public int getHubMergerChangingCount() {
		return hubMergerChangingCount;
	}

	/**
	 * Increments the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int incHubMergerChangingCount() {
		return ++this.hubMergerChangingCount;
	}
	/**
	 * Decrements the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int decHubMergerChangingCount() {
		return --this.hubMergerChangingCount;
	}
	
	
	/**
	 * Returns the CompoundUndoableName value.
	 *
	 * @return the CompoundUndoableName value
	 */
	public String getCompoundUndoableName() {
		return compoundUndoableName;
	}

	/**
	 * Sets the CompoundUndoableName value.
	 * @param compoundUndoableName the CompoundUndoableName value
	 */
	public void setCompoundUndoableName(String compoundUndoableName) {
		this.compoundUndoableName = compoundUndoableName;
	}

	/**
	 * Returns whether CreateUndoablePropertyChanges is active for the current runtime context.
	 *
	 * @return {@code true} if CreateUndoablePropertyChanges is active
	 */
	public boolean isCreateUndoablePropertyChanges() {
		return createUndoablePropertyChanges;
	}

	/**
	 * Sets the CreateUndoablePropertyChanges value.
	 * @param createUndoablePropertyChanges the CreateUndoablePropertyChanges value
	 */
	public void setCreateUndoablePropertyChanges(boolean createUndoablePropertyChanges) {
		this.createUndoablePropertyChanges = createUndoablePropertyChanges;
	}

	/**
	 * Returns the Status value.
	 *
	 * @return the Status value
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the Status value.
	 * @param status the Status value
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Returns the RequestInfo value.
	 *
	 * @return the RequestInfo value
	 */
	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	/**
	 * Sets the RequestInfo value.
	 * @param requestInfo the RequestInfo value
	 */
	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	/**
	 * Returns the NotifyObject value.
	 *
	 * @return the NotifyObject value
	 */
	public Object getNotifyObject() {
		return notifyObject;
	}

	/**
	 * Sets the NotifyObject value.
	 * @param notifyObject the NotifyObject value
	 */
	public void setNotifyObject(Object notifyObject) {
		this.notifyObject = notifyObject;
	}

	/**
	 * Returns the RecursiveTriggerCount value.
	 *
	 * @return the RecursiveTriggerCount value
	 */
	public int getRecursiveTriggerCount() {
		return recursiveTriggerCount;
	}

	/**
	 * Increments the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int incRecursiveTriggerCount() {
		return ++this.recursiveTriggerCount;
	}

	/**
	 * Decrements the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int decRecursiveTriggerCount() {
		return --this.recursiveTriggerCount;
	}

	
	/**
	 * Returns the HubListenerTreeCount value.
	 *
	 * @return the HubListenerTreeCount value
	 */
	public int getHubListenerTreeCount() {
		return hubListenerTreeCount;
	}

	/**
	 * Increments the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int incHubListenerTreeCount() {
		return ++this.hubListenerTreeCount;
	}
	/**
	 * Decrements the runtime counter and returns the updated value.
	 *
	 * @return the updated counter value
	 */
	public int decHubListenerTreeCount() {
		return --this.hubListenerTreeCount;
	}

	
	/**
	 * Returns the IgnoreTreeListenerProperty value.
	 *
	 * @return the IgnoreTreeListenerProperty value
	 */
	public String getIgnoreTreeListenerProperty() {
		return ignoreTreeListenerProperty;
	}

	/**
	 * Sets the IgnoreTreeListenerProperty value.
	 * @param ignoreTreeListenerProperty the IgnoreTreeListenerProperty value
	 */
	public void setIgnoreTreeListenerProperty(String ignoreTreeListenerProperty) {
		this.ignoreTreeListenerProperty = ignoreTreeListenerProperty;
	}

	/**
	 * Returns the CalcPropertyEvents value.
	 *
	 * @return the CalcPropertyEvents value
	 */
	public Tuple3<Hub, OAObject, String>[] getCalcPropertyEvents() {
		return calcPropertyEvents;
	}

	/**
	 * Sets the CalcPropertyEvents value.
	 * @param Tuple3<Hub the CalcPropertyEvents value
	 * @param OAObject the CalcPropertyEvents value
	 * @param calcPropertyEvents the CalcPropertyEvents value
	 */
	public void setCalcPropertyEvents(Tuple3<Hub, OAObject, String>[] calcPropertyEvents) {
		this.calcPropertyEvents = calcPropertyEvents;
	}

	/**
	 * Returns the Refreshing value.
	 *
	 * @return the Refreshing value
	 */
	public int getRefreshing() {
		return refreshing;
	}

	/**
	 * Sets the Refreshing value.
	 * @param refreshing the Refreshing value
	 */
	public void setRefreshing(int refreshing) {
		this.refreshing = refreshing;
	}

	// 20140121
	/**
	 * The current remote request being processed on this thread.
	 * Used by remote invocation layers to track request context and
	 * propagate caller/session information during execution.
	 */
	private RequestInfo requestInfo;

	// 20160121
	/**
	 * An auxiliary reference used by notification and coordination logic.
	 * Its specific meaning depends on the subsystem interacting with the
	 * thread’s execution state.
	 */
	private Object notifyObject;

	// 20160625
	/**
	 * Counter used to detect and prevent recursive trigger execution.
	 * Incremented when entering trigger logic and decremented upon exit
	 * to avoid infinite event loops.
	 */
	private int recursiveTriggerCount;

	// 20180223
	/**
	 * Counts the number of sync events generated or processed by this
	 * thread. Used primarily for diagnostics and performance tracking.
	 */
	public int oaSyncEventCount;

	/**
	 * List of sibling-helper instances used by this thread to compute
	 * and cache sibling property-path resolutions during OA model
	 * navigation.
	 */
	public List<OASiblingHelper<?>> alSiblingHelper;

	/**
	 * Counter tracking how many times sibling lookup has been requested
	 * on this thread. Primarily used for diagnostic or tuning purposes.
	 */
	public int cntGetSiblingCalled;

	/**
	 * Stack-like list representing the HubEvent chain currently being
	 * processed by this thread. Supports nested event-processing flows.
	 */
	public ArrayList<HubEvent> alHubEvent;

	
	/**
	 * Instance of the OAModel object.isModelUserClass for each OA.
	 * ex: AppUser instance.
	 */
	public Map<OA, Hub<?>> hmModelUser;

	public Map<OA, OASessionUser<?>> hmSessionUser;

	/**
	 * Flag used by OAContext to automatically grant admin privileges
	 * for this thread. When true, OAContext.isAdmin() returns true.
	 */
	public boolean isAdmin;

	/**
	 * List of hubs that should bypass automatic adjustment of the
	 * active object during certain Hub operations, such as position
	 * recalculation.
	 */
	public Hub[] dontAdjustHubs;

	/**
	 * Counter indicating that a Hub.refresh operation is in progress.
	 * While non-zero, queries use “dirty mode” to avoid interference
	 * from in-flight refresh operations.
	 */
	private int refreshing; 
	
	/**
	 * Identifies a Hub currently undergoing fast-loading. When the
	 * loading completes and this field is reset to null, a newList
	 * event is issued.
	 */
	public Hub fastLoadingHub;

	/**
	 * The OAProcess instance currently associated with this thread.
	 * Used to track progress, cancellation state, or workflow context
	 * during multi-step operations.
	 */
	public OAProcess process;
	
	/**
	 * Array of thread-scoped callbacks used by HubMerger during
	 * merge operations. Allows per-thread customization of merge
	 * behavior.
	 */
	public OAThreadLocalHubMergerCallback[] hubMergerCallback;

	/**
	 * Runtime state field used by OA services for replicationSource.
	 */
	public String replicationSource;

	
	@SuppressWarnings("unchecked")
	/**
	 * Returns the ModelUser value.
	 *
	 * @param oa the lookup context
	 *
	 * @return the ModelUser value
	 */
	public <T extends OAObject> Hub<T> getModelUser(OA oa) {
		if (hmModelUser == null) return null;
		return (Hub<T>) hmModelUser.get(oa);
	}

	/**
	 * Sets the ModelUser value.
	 * @param oa the ModelUser value
	 * @param hub the ModelUser value
	 */
	public <T extends OAObject> void setModelUser(OA oa, Hub<T> hub) {
	    if (oa == null) return;
		if (hmModelUser == null) hmModelUser = new HashMap<>();
	    if (hub == null) hmModelUser.remove(oa);
	    else hmModelUser.put(oa, hub);
	}	

	/**
	 * Clears the runtime state tracked by this method.
	 */
	public void clearModelUser() {
		if (hmModelUser != null) hmModelUser.clear();
	}

	/**
	 * Returns the SessionUser value.
	 *
	 * @return the SessionUser value
	 */
	public OASessionUser<?> getSessionUser(OA oa) {
	    if (oa == null) return null;
	    if (hmSessionUser == null) return null;
	    return hmSessionUser.get(oa);
	}

	/**
	 * Sets the SessionUser value.
	 * @param su the SessionUser value
	 */
	public void setSessionUser(OA oa, OASessionUser<?> su) {
	    if (oa == null) return;
	    if (hmSessionUser == null) hmSessionUser = new HashMap<>();
	    if (su == null) hmSessionUser.remove(oa);
	    else hmSessionUser.put(oa, su);
	}

	public void clearSessionUser() {
	    if (hmSessionUser != null) hmSessionUser.clear();
	}
	
}
