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
package com.viaoa.cascade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/*qqqqqqqqqqqqqqqqqqqqqqq
 CODEX
 
 1. src/main/java/com/viaoa/cascade/OACascade.java / wasCascaded(Hub hub, boolean bAdd)

  Concrete bug: Hub cascade tracking uses TreeSet<Hub> even though Hub does not implement Comparable.

  Runtime scenario: normal Hub cascade entry points call cascade.wasCascaded(thisHub, true):

  - HubSaveService.saveAll(...) line 64
  - HubDeleteService.deleteAll(...) line 158
  - HubStatusService.getChanged(...) line 53

  OACascade.wasCascaded(Hub,true) creates new TreeSet<Hub>() at line 314, then calls treeHub.contains(hub) /
  treeHub.add(hub) at lines 325 and 346. A TreeSet with no comparator requires comparable elements. Hub is not
  comparable, so the first contains/add path can throw ClassCastException.

  Why this violates OA/OG cascade semantics: Hub save/delete/status cascades must use the cascade guard to prevent
  duplicate Hub traversal. Instead, normal Hub cascade processing can fail before traversal starts. That can break
  saveAll/deleteAll/getChanged and cause caller-visible failure from the guard itself, not from the actual cascade
  operation.

  Minimal fix direction: replace TreeSet<Hub> with identity-based tracking, for example Set<Hub> backed by
  IdentityHashMap, or use a comparator based on stable identity such as System.identityHashCode plus identity
  disambiguation. Since this is Hub instance traversal state, identity semantics are the safest contract.

  Suggested CODEX comment location: OACascade.java line 59 field declaration and line 314 initialization.

  Suggested regression test: testWasCascadedHubAcceptsPlainHubWithoutComparable.

  2. src/main/java/com/viaoa/cascade/OACascade.java / wasCascaded(OAObject oaObj, boolean bAdd)

  Concrete bug: locked mode does not make check-and-add atomic, so two threads can both observe the same object as not
  cascaded and both return false.

  Runtime scenario: OALoader creates new OACascade(true) at lines 694-697 and uses it during traversal at line 563.
  With concurrent traversal of the same object:

  1. Thread A takes read lock, treeObject.contains(guid) is false, releases read lock.
  2. Thread B does the same before A adds the GUID.
  3. Both threads acquire the write lock in turn and call treeObject.add(guid).
  4. Both return false, so both callers process the same object as if it had not been cascaded.

  Why this violates OA/OG cascade semantics: wasCascaded(obj,true) is the guard that prevents duplicate recursive
  processing. In locked mode, callers reasonably expect it to be safe for concurrent cascade traversal. Returning
  false twice for the same object allows duplicate load/save/find/validation side effects and can break traversal
  determinism.

  Minimal fix direction: when bAdd is true, perform the contains/add decision under one write lock and return based on
  Set.add(...). For example: initialize under write lock, then return !treeObject.add(guid) while still holding the
  write lock. Apply the same atomic pattern to Hub tracking after fixing the Hub set type.

  Suggested CODEX comment location: OACascade.java around lines 257-286.

  Suggested regression test: testLockedWasCascadedObjectAllowsOnlyOneConcurrentFirstVisitor.

  3. src/main/java/com/viaoa/cascade/OACascade.java / write-lock sections in addToOverflow, ignore,
     wasCascaded(OAObject), wasCascaded(Hub)

  Concrete bug: several write-lock acquisitions are not released in finally blocks.

  Runtime scenario: in wasCascaded(Hub,true), after line 343 acquires the write lock, line 346 can throw due to the
  TreeSet<Hub> bug above. Because unlock is at line 348 and not in finally, the lock remains held. Any later thread
  using that same locked cascade object can block permanently. Similar non-finally write-lock patterns exist at lines
  155-163, 208-216, 246-253, and 273-283.

  Why this violates OA/OG cascade semantics: cascade guards are infrastructure used inside load/save/delete/find
  paths. A guard failure must not leave the cascade object in a permanently locked state, because that turns a caller-
  visible exception into a hidden future stall/deadlock.

  Minimal fix direction: wrap every lock acquisition in try/finally, including initialization and add/update blocks.
  Fix this together with atomic check-add so the locking contract is simpler and enforceable.

  Suggested CODEX comment location: first write-lock block in OACascade.java line 154, plus wasCascaded write sections
  around lines 246 and 343.

  Suggested regression test: testLockedCascadeReleasesWriteLockWhenAddThrows.
 
 
 
 1. src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java / save(..., OACascade cascade, ..., boolean
     bCheckDepth) using OACascade overflow support

  Concrete bug: deep cascade overflow is unreachable because the object is marked cascaded before the depth check,
  then the overflow path checks the same cascade marker and refuses to add it.

  Runtime scenario:

  1. save(oaObj, rule, cascade, ..., true) enters.
  2. Line 162 calls cascade.wasCascaded(oaObj, true), which adds oaObj to the cascade visited set.
  3. If recursion depth is already over 50, line 166 enters the overflow/tail-recursion branch.
  4. Line 167 checks !cascade.wasCascaded(oaObj, false).
  5. That check is always false for this object because line 162 just added it.
  6. cascade.addToOverflow(oaObj) at line 168 never runs.
  7. Method returns, leaving the deep object marked visited but not saved or deferred.

  Why this violates OA/OG cascade semantics: the overflow list is intended to prevent stack overflow while preserving
  cascade completeness. This path silently skips required deep saves and also marks the object as already cascaded, so
  later visits in the same cascade will skip it too. That can leave deep child/detail objects unsaved while the outer
  save appears to complete.

  Minimal fix direction: perform the depth check before marking the object as cascaded, or make wasCascaded support an
  atomic “already visited vs newly marked” result that allows newly marked deep objects to be queued for overflow.
  Another option is to add to overflow unconditionally when the object was newly marked and depth exceeds the
  threshold.

  Suggested CODEX comment location: OAObjectSaveService.java around lines 162-168, with a reference to
  OACascade.addToOverflow.

  Suggested regression test: testDeepCascadeSaveQueuesOverflowObjectInsteadOfSkippingIt.

 */

/**
 * Tracks OAObjects and Hubs that have already been processed during a
 * cascading graph traversal, preventing infinite recursion and redundant
 * visitation. Used by recursive cascade logic (e.g. save/delete propagation)
 * to protect against circular references in the Object Graph.
 *
 * <p>This helper maintains separate sets for objects (by GUID) and Hubs,
 * and can operate in a thread-safe mode when requested. Depth counters
 * and an optional overflow list allow deep traversals to unwind safely
 * without causing stack overflow.</p>
 *
 * <p>No side effects are applied to the Object Graph itself. This class
 * provides lightweight runtime tracking only and does not force lazy
 * loading or modify relationship integrity.</p>
 *
 * @see OAObject
 * @see Hub
 */
public class OACascade {
	private static Logger LOG = Logger.getLogger(OACascade.class.getName());

	/**
	 * Tracks GUIDs of OAObjects that have already been visited during a cascade
	 * operation, preventing redundant processing and infinite recursion.
	 */
	private volatile TreeSet<UUID> treeObject;

	/**
	 * Tracks Hub instances encountered during cascading, ensuring each Hub is
	 * processed only once.
	 */
	private volatile TreeSet<Hub> treeHub;
	
	/**
	 * Optional read/write lock providing thread-safe access to the object
	 * tracking set when locking is enabled.
	 */
	private final ReentrantReadWriteLock rwLock;
	
	/**
	 * Optional read/write lock providing thread-safe access to the Hub
	 * tracking set when locking is enabled.
	 */
	private final ReentrantReadWriteLock rwLockHub;

	/**
	 * Current recursive depth of the cascade traversal, incremented and
	 * decremented as recursion advances and unwinds.
	 */
	private volatile int depth;

	/**
	 * Optional overflow list used to store objects encountered during deep
	 * cascading or when additional bookkeeping is needed.
	 */
	private volatile ArrayList<Object> alOverflow;

	/**
	 * Set of classes that should be ignored during cascade tracking; any object
	 * of a class in this set is treated as already processed.
	 */
	private volatile HashSet<Class> hsIgnore;
	
	/**
	 * Creates a new cascade-tracking instance used during recursive graph
	 * traversal operations. When {@code bUseLocks} is {@code true}, the
	 * instance initializes read/write locks to support thread-safe use
	 * across multiple threads.
	 *
	 * @param bUseLocks {@code true} to enable locking for thread-safe access,
	 *                  {@code false} for non-synchronized operation
	 */
	public OACascade(boolean bUseLocks) {
		LOG.finer("new OACascade");
		if (bUseLocks) {
			rwLock = new ReentrantReadWriteLock();
			rwLockHub = new ReentrantReadWriteLock();
		}
		else {
			rwLock = null;
			rwLockHub = null;
		}
	}

	/**
	 * Increments the current cascade depth counter. This is used to track
	 * how deep a recursive cascade operation has progressed.
	 */
	public void depthAdd() {
		depth++;
	}

	/**
	 * Decrements the current cascade depth counter. This is used when
	 * unwinding recursive cascade operations.
	 */
	public void depthSubtract() {
		depth--;
	}

	/**
	 * Returns the current depth value for the cascade operation. The depth
	 * increases as recursive traversal proceeds and decreases as it unwinds.
	 *
	 * @return the current cascade depth
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Sets the current cascade depth counter to the specified value.
	 *
	 * @param d the depth value to assign
	 */
	public void setDepth(int d) {
		this.depth = d;
	}

	/**
	 * Adds an object to the overflow list. The overflow list is used to
	 * track objects encountered when traversal depth becomes large or when
	 * additional bookkeeping is required during cascading.
	 *
	 * @param obj the object to add to the overflow list
	 */
	public void addToOverflow(Object obj) {
		if (rwLock != null) {
			rwLock.writeLock().lock();
		}
		if (alOverflow == null) {
			if (alOverflow == null) alOverflow = new ArrayList<Object>();
		}
		alOverflow.add(obj);
		if (rwLock != null) {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * Returns the list of objects that were added to the overflow list
	 * during cascade traversal.
	 *
	 * @return the overflow list, or {@code null} if no objects were added
	 */
	public ArrayList<Object> getOverflowList() {
		return alOverflow;
	}

	/**
	 * Clears the overflow list, removing all objects previously added.
	 * After this call, the overflow list will be {@code null}.
	 */
	public void clearOverflowList() {
		alOverflow = null;
	}

	/**
	 * Creates a new cascade-tracking instance without enabling thread-safe
	 * locking. This constructor is intended for single-threaded traversal
	 * scenarios.
	 */
	public OACascade() {
		rwLock = null;
		rwLockHub = null;
	}

	/**
	 * 20160126 not used. confusing: remove from tree or list public void remove(OAObject oaObj) { if (treeObject != null) { if (rwLock !=
	 * null) rwLock.readLock().lock(); treeObject.remove(oaObj.guid); if (rwLock != null) rwLock.readLock().unlock(); } }
	 */

	/**
	 * Marks the specified class so that objects of that type are ignored
	 * during cascade tracking. Once a class is added, instances of that
	 * class are treated as already cascaded.
	 *
	 * @param clazz the class to ignore during cascading
	 */
	public void ignore(Class clazz) {
		if (rwLock != null) {
			rwLock.writeLock().lock();
		}
		if (hsIgnore == null) {
			if (hsIgnore == null) hsIgnore = new HashSet<Class>();
		}
		hsIgnore.add(clazz);
		if (rwLock != null) {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * Determines whether the specified object has already been processed
	 * during a cascade traversal. If {@code bAdd} is {@code true} and the
	 * object has not yet been encountered, it is added to the internal
	 * tracking set.
	 *
	 * <p>If the object's class has been marked as ignored, this method
	 * returns {@code true} without checking or modifying the tracking set.</p>
	 *
	 * @param oaObj the object to check
	 * @param bAdd  {@code true} to add the object to the tracking set if it
	 *              has not been seen before, otherwise {@code false}
	 * @return {@code true} if the object was previously cascaded or is
	 *         ignored, otherwise {@code false}
	 */
	public boolean wasCascaded(OAObject oaObj, boolean bAdd) {
		if (oaObj == null) {
			return false;
		}
		if (hsIgnore != null && hsIgnore.contains(oaObj.getClass())) {
			return true;
		}
		if (treeObject == null) {
			if (!bAdd) {
				return false;
			}
			if (rwLock != null) {
				rwLock.writeLock().lock();
			}
			if (treeObject == null) {
				treeObject = new TreeSet<UUID>();
			}
			if (rwLock != null) {
				rwLock.writeLock().unlock();
			}
		}

		boolean b;
		try {
			if (rwLock != null) {
				rwLock.readLock().lock();
			}
			b = treeObject.contains(oaObj.getGuid());
		} finally {
			if (rwLock != null) {
				rwLock.readLock().unlock();
			}
		}
		if (b) {
			return true;
		}

		if (bAdd) {
			if (rwLock != null) {
				rwLock.writeLock().lock();
			}
			treeObject.add(oaObj.getGuid());
			/*            
			if (treeObject.size() > 10000) {
			    if (throttle.check()) System.out.println((throttle.getCount())+") "+Thread.currentThread().getName()+" ********* OACascade, tree.size="+treeObject.size()+", obj="+oaObj);
			}
			*/
			if (rwLock != null) {
				rwLock.writeLock().unlock();
			}
		}
		return false;
	}
	//final OAThrottle throttle = new OAThrottle(5000);

	/**
	 * Determines whether the specified Hub has already been processed
	 * during a cascade traversal. If {@code bAdd} is {@code true} and the
	 * Hub has not yet been encountered, it is added to the internal
	 * tracking set.
	 *
	 * @param hub  the Hub to check
	 * @param bAdd {@code true} to add the Hub to the tracking set if it has
	 *             not been seen before, otherwise {@code false}
	 * @return {@code true} if the Hub was previously cascaded, otherwise
	 *         {@code false}
	 */
	public boolean wasCascaded(Hub hub, boolean bAdd) {
		if (hub == null) {
			return false;
		}

		if (treeHub == null) {
			if (!bAdd) {
				return false;
			}
			if (rwLockHub != null) {
				rwLockHub.writeLock().lock();
			}
			if (treeHub == null) treeHub = new TreeSet<Hub>();
			if (rwLockHub != null) {
				rwLockHub.writeLock().unlock();
			}
		}

		boolean b = false;
		try {
			if (rwLockHub != null) {
				rwLockHub.readLock().lock();
			}
			b = treeHub.contains(hub);
		} finally {
			if (rwLockHub != null) {
				rwLockHub.readLock().unlock();
			}
		}
		if (b) {
			return true;
		}

		if (bAdd) {
			if (rwLockHub != null) {
				rwLockHub.writeLock().lock();
			}
			treeHub.add(hub);
			if (rwLockHub != null) {
				rwLockHub.writeLock().unlock();
			}
		}
		return false;
	}

	/**
	 * Returns the number of unique objects and Hubs that have been visited
	 * during the cascade traversal. This includes all entries in both the
	 * object and Hub tracking sets.
	 *
	 * @return the total count of visited objects and Hubs
	 */
	public int getVisitCount() {
		int cnt = 0;
		if (treeObject != null) {
			cnt += treeObject.size();
		}
		if (treeHub != null) {
			cnt += treeHub.size();
		}
		return cnt;
	}
}
