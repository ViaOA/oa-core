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
package com.viaoa.transaction;

import java.util.ArrayList;
import java.util.HashMap;

import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;

/**
 * Thread-local transaction mechanism used by OA datasources and other
 * subsystems that require coordinated commit/rollback sequencing.
 * <p>
 * An {@code OATransaction} is explicitly started and then becomes associated
 * with the current thread via
 * {@link com.viaoa.object.OAThreadLocalDelegate#setTransaction}. Code running
 * in that thread can retrieve the active transaction and register listeners
 * that should be notified at commit or rollback time.
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>define a JDBC-style isolation level for the transaction,</li>
 *   <li>coordinate batch-mode operations across datasources,</li>
 *   <li>optionally allow writes even when datasources are marked read-only,</li>
 *   <li>notify registered {@link OATransactionListener} instances,</li>
 *   <li>provide a simple key/value map for listeners to store temporary data.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #start()} — associates this transaction with the current thread,</li>
 *   <li>application performs work (e.g., datasource operations),</li>
 *   <li>{@link #commit()} or {@link #rollback()} — notifies all listeners and
 *       clears thread-local state.</li>
 * </ol>
 *
 * <h2>Listeners</h2>
 * Registered listeners (datasources, batching subsystems, etc.) receive:
 * <ul>
 *   <li>{@code commit(this)}</li>
 *   <li>{@code rollback(this)}</li>
 *   <li>{@code executeOpenBatches(this)}</li>
 * </ul>
 * providing a consistent transaction boundary.
 *
 * <h2>Thread Association</h2>
 * Transactions never span threads. Each thread must start and end its own
 * transaction using {@link #start()}, and {@link #isStarted()} determines
 * whether this transaction is the one currently bound to the thread.
 */
public class OATransaction {
	/**
	 * Isolation level assigned to this transaction. Corresponds to one of the
	 * standard {@link java.sql.Connection} isolation constants and determines
	 * visibility and locking behavior for transactional operations.
	 */
	private final int transactionLevel;
	
	/**
	 * Collection of registered {@link OATransactionListener} instances that will
	 * be notified on commit, rollback, and open-batch execution events.
	 */
	private final ArrayList<OATransactionListener> al = new ArrayList<OATransactionListener>();

	/**
	 * Flag indicating whether batch mode is enabled for this transaction. When
	 * true, datasources and listeners may defer writes until batch execution.
	 */
	private boolean bUseBatch;

	/**
	 * Indicates whether write operations should be allowed even when a datasource
	 * is marked read-only. Used to override default safety checks.
	 */
	private boolean bAllowWritesIfDsIsReadonly;

	/*  java.sql.Connection isolation levels
	    java.sql.Connection.X<br>
	    TRANSACTION_NONE - level not set - some databases (ex: Derby) will throw and exception<br>
	    TRANSACTION_READ_UNCOMMITTED - data changed by transaction will be used by other transactions that read<br>
	    TRANSACTION_READ_COMMITTED - data changed by transaction is not "seen" until commited.  Other transactions will read "old" data.<br>
	    TRANSACTION_REPEATABLE_READ - prevents others from writing<br>
	    TRANSACTION_SERIALIZABLE - prevents others from reading & writing<br>
	*/
	/**
	 * Creates a new transaction with the specified JDBC-style isolation level.
	 *
	 * @param transactionLevel the desired transaction isolation level as defined
	 *        by {@link java.sql.Connection} constants.
	 */
	public OATransaction(int transactionLevel) {
		this.transactionLevel = transactionLevel;
	}

	/**
	 * Creates a new transaction using {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}
	 * as the default isolation level.
	 */
	public OATransaction() {
		this(java.sql.Connection.TRANSACTION_READ_COMMITTED);
	}

	/**
	 * Returns the configured transaction isolation level.
	 *
	 * @return the JDBC-style isolation level assigned to this transaction.
	 */
	public int getTransactionIsolationLevel() {
		return transactionLevel;
	}

	/**
	 * Sets whether batch mode should be used for this transaction.
	 *
	 * @param b true to enable batch mode; false to disable it.
	 */
	public void setUseBatch(boolean b) {
		this.bUseBatch = b;
	}

	/**
	 * Indicates whether batch mode is enabled for this transaction.
	 *
	 * @return true if batch mode is enabled; otherwise false.
	 */
	public boolean getUseBatch() {
		return this.bUseBatch;
	}

	/**
	 * Specifies whether write operations should be permitted even when datasources
	 * are configured as read-only.
	 *
	 * @param b true to allow writes; false to prevent them.
	 */
	public void setAllowWritesIfDsIsReadonly(boolean b) {
		bAllowWritesIfDsIsReadonly = b;
	}

	/**
	 * Returns whether write operations are allowed when datasources are marked
	 * read-only.
	 *
	 * @return true if writes are permitted; otherwise false.
	 */
	public boolean getAllowWritesIfDsIsReadonly() {
		return bAllowWritesIfDsIsReadonly;
	}

	/**
	 * Associates this transaction with the current thread by placing it in the
	 * thread-local storage used by OA. Marks the beginning of a transactional
	 * context for the calling thread.
	 */
	public void start() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.setTransaction(this);
	}

	/**
	 * Determines whether this transaction is currently active on the calling
	 * thread.
	 *
	 * @return true if this transaction is the one bound to the thread; otherwise
	 *         false.
	 */
	public boolean isStarted() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		return srvcOAThreadLocal.getTransaction() == this;
	}

	/**
	 * Performs a rollback of the transaction. All registered listeners are
	 * notified via {@code rollback(this)}. The transaction is then cleared from
	 * thread-local storage.
	 */
	public void rollback() {
		try {
			for (OATransactionListener tl : al) {
				tl.rollback(this);
			}
		} finally {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
			srvcOAThreadLocal.setTransaction(null);
		}
	}

	/**
	 * Commits the transaction. All registered listeners are notified via
	 * {@code commit(this)}. Regardless of listener behavior, the transaction is
	 * removed from thread-local storage.
	 */
	public void commit() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		try {
			for (OATransactionListener tl : al) {
				tl.commit(this);
			}
		} finally {
			srvcOAThreadLocal.setTransaction(null);
		}
	}

	/**
	 * Registers a transaction listener if it is not already present.
	 *
	 * @param tl the listener to add.
	 */
	public void addTransactionListener(OATransactionListener tl) {
		if (!al.contains(tl)) {
			al.add(tl);
		}
	}

	/**
	 * Removes a previously registered transaction listener.
	 *
	 * @param tl the listener to remove.
	 */
	public void removeTransactionListener(OATransactionListener tl) {
		al.remove(tl);
	}

	// used by TransactionListeners to "store" information.
	/**
	 * Internal key/value store used by listeners to associate temporary data with
	 * the lifespan of this transaction.
	 */
	private HashMap<Object, Object> hm = new HashMap();

	/**
	 * Stores a key/value pair in the transaction's internal map.
	 *
	 * @param key the lookup key.
	 * @param value the associated value.
	 */
	public void put(Object key, Object value) {
		hm.put(key, value);
	}

	/**
	 * Retrieves a value from the transaction's internal map.
	 *
	 * @param key the lookup key.
	 * @return the stored value associated with the key, or null if none exists.
	 */
	public Object get(Object key) {
		return hm.get(key);
	}

	/**
	 * Removes the entry associated with the given key from the transaction's
	 * internal map.
	 *
	 * @param key the lookup key whose entry should be removed.
	 * @return the value previously associated with the key, or null if none existed.
	 */
	public Object remove(Object key) {
		return hm.remove(key);
	}

	/**
	 * Notifies all registered {@link OATransactionListener} instances to execute
	 * any batches they have open by invoking {@code executeOpenBatches(this)}.
	 */
	public void executeOpenBatches() {
		for (OATransactionListener tl : al) {
			tl.executeOpenBatches(this);
		}
	}

}
