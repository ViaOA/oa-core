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

import java.util.ArrayList;
import java.util.List;

import com.viaoa.hub.*;
import com.viaoa.json.OAJson;
import com.viaoa.process.OAProcess;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.transaction.OATransaction;
import com.viaoa.util.Tuple3;

/**
 * Thread-scoped state container used internally by OA to manage execution context
 * and operational flags on a per-thread basis.
 *
 * <p>This holds lightweight, mutable metadata including:
 * <ul>
 *   <li>Object graph loading and deletion state</li>
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
	protected int loading;

	/**
	 * Counter controlling suppression of client/server sync messages for the
	 * duration of critical update sections or batch operations.
	 */
	protected int suppressCSMessages;

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
	 * Marks this thread as a sync-processing thread, used to modify behavior
	 * of event dispatching, object updates, or remote callback handling.
	 */
	private boolean bIsSyncThread;

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

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public OATransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(OATransaction transaction) {
		this.transaction = transaction;
	}

	public int getLoading() {
		return loading;
	}

	public void setLoading(int loading) {
		this.loading = loading;
	}

	public int getCacheAddMode() {
		return cacheAddMode;
	}

	public void setCacheAddMode(int cacheAddMode) {
		this.cacheAddMode = cacheAddMode;
	}

	public List<OAObjectSerializer> getObjectSerializers() {
		return alObjectSerializer;
	}

	public void addObjectSerializer(OAObjectSerializer objectSerializer) {
		if (alObjectSerializer == null) alObjectSerializer = new ArrayList();
		alObjectSerializer.add(objectSerializer);
	}
	
	public boolean removeObjectSerializer(OAObjectSerializer objectSerializer) {
		if (alObjectSerializer == null) return false;
		return alObjectSerializer.remove(objectSerializer);
	}
	

	public int getSuppressCSMessages() {
		return suppressCSMessages;
	}

	public void setSuppressCSMessages(int suppressCSMessages) {
		this.suppressCSMessages = suppressCSMessages;
	}

	public Object[] getDeleting() {
		return deleting;
	}

	public void setDeleting(Object[] deleting) {
		this.deleting = deleting;
	}

	public Object[] getFlags() {
		return flags;
	}

	public void setFlags(Object[] flags) {
		this.flags = flags;
	}

	public Object[] getLocks() {
		return locks;
	}

	public void setLocks(Object[] locks) {
		this.locks = locks;
	}

	public boolean getWaitingOnLock() {
		return bIsWaitingOnLock;
	}

	public void setWaitingOnLock(boolean bIsWaitingOnLock) {
		this.bIsWaitingOnLock = bIsWaitingOnLock;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public int getHubMergerChangingCount() {
		return hubMergerChangingCount;
	}

	public int incHubMergerChangingCount() {
		return ++this.hubMergerChangingCount;
	}
	public int decHubMergerChangingCount() {
		return --this.hubMergerChangingCount;
	}
	
	
	public String getCompoundUndoableName() {
		return compoundUndoableName;
	}

	public void setCompoundUndoableName(String compoundUndoableName) {
		this.compoundUndoableName = compoundUndoableName;
	}

	public boolean isCreateUndoablePropertyChanges() {
		return createUndoablePropertyChanges;
	}

	public void setCreateUndoablePropertyChanges(boolean createUndoablePropertyChanges) {
		this.createUndoablePropertyChanges = createUndoablePropertyChanges;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	public Object getNotifyObject() {
		return notifyObject;
	}

	public void setNotifyObject(Object notifyObject) {
		this.notifyObject = notifyObject;
	}

	public int getRecursiveTriggerCount() {
		return recursiveTriggerCount;
	}

	public int incRecursiveTriggerCount() {
		return ++this.recursiveTriggerCount;
	}

	public int decRecursiveTriggerCount() {
		return --this.recursiveTriggerCount;
	}

	
	public int getHubListenerTreeCount() {
		return hubListenerTreeCount;
	}

	public int incHubListenerTreeCount() {
		return ++this.hubListenerTreeCount;
	}
	public int decHubListenerTreeCount() {
		return --this.hubListenerTreeCount;
	}

	
	public String getIgnoreTreeListenerProperty() {
		return ignoreTreeListenerProperty;
	}

	public void setIgnoreTreeListenerProperty(String ignoreTreeListenerProperty) {
		this.ignoreTreeListenerProperty = ignoreTreeListenerProperty;
	}

	public Tuple3<Hub, OAObject, String>[] getCalcPropertyEvents() {
		return calcPropertyEvents;
	}

	public void setCalcPropertyEvents(Tuple3<Hub, OAObject, String>[] calcPropertyEvents) {
		this.calcPropertyEvents = calcPropertyEvents;
	}

	public boolean isSyncThread() {
		return bIsSyncThread;
	}

	public void setIsSyncThread(boolean bIsSyncThread) {
		this.bIsSyncThread = bIsSyncThread;
	}

	public int getRefreshing() {
		return refreshing;
	}

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
	 * and cache sibling property-path resolutions during object graph
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
	 * Thread-scoped context object used by OAContext. May store any
	 * value associated with the logical user/session context of this
	 * thread.
	 */
	public Object context;

	/**
	 * Flag used by OAContext to automatically grant admin privileges
	 * for this thread. When true, OAContext.isAdmin() returns true.
	 */
	public boolean isAdmin;

	/**
	 * JSON serialization/deserialization helper for this thread.
	 * Used when converting objects to/from JSON formats.
	 */
	public OAJson oajackson;

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

	//qqqqqqqqqqqqqqqqqqqvvvvvvvvv 20260403
	public String replicationSource;

}
