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
import java.util.HashSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;

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
			b = treeObject.contains(oaObj.guid);
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
			treeObject.add(oaObj.guid);
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
