/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import com.viaoa.object.OAThreadLocalDelegate;

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
	private final int transactionLevel;
	private final ArrayList<OATransactionListener> al = new ArrayList<OATransactionListener>();

	private boolean bUseBatch;

	private boolean bAllowWritesIfDsIsReadonly;

	/*  java.sql.Connection isolation levels
	    java.sql.Connection.X<br>
	    TRANSACTION_NONE - level not set - some databases (ex: Derby) will throw and exception<br>
	    TRANSACTION_READ_UNCOMMITTED - data changed by transaction will be used by other transactions that read<br>
	    TRANSACTION_READ_COMMITTED - data changed by transaction is not "seen" until commited.  Other transactions will read "old" data.<br>
	    TRANSACTION_REPEATABLE_READ - prevents others from writing<br>
	    TRANSACTION_SERIALIZABLE - prevents others from reading & writing<br>
	*/
	public OATransaction(int transactionLevel) {
		this.transactionLevel = transactionLevel;
	}

	public OATransaction() {
		this(java.sql.Connection.TRANSACTION_READ_COMMITTED);
	}

	public int getTransactionIsolationLevel() {
		return transactionLevel;
	}

	public void setUseBatch(boolean b) {
		this.bUseBatch = b;
	}

	public boolean getUseBatch() {
		return this.bUseBatch;
	}

	public void setAllowWritesIfDsIsReadonly(boolean b) {
		bAllowWritesIfDsIsReadonly = b;
	}

	public boolean getAllowWritesIfDsIsReadonly() {
		return bAllowWritesIfDsIsReadonly;
	}

	public void start() {
		OAThreadLocalDelegate.setTransaction(this);
	}

	public boolean isStarted() {
		return OAThreadLocalDelegate.getTransaction() == this;
	}

	public void rollback() {
		try {
			for (OATransactionListener tl : al) {
				tl.rollback(this);
			}
		} finally {
			OAThreadLocalDelegate.setTransaction(null);
		}
	}

	public void commit() {
		try {
			for (OATransactionListener tl : al) {
				tl.commit(this);
			}
		} finally {
			OAThreadLocalDelegate.setTransaction(null);
		}
	}

	public void addTransactionListener(OATransactionListener tl) {
		if (!al.contains(tl)) {
			al.add(tl);
		}
	}

	public void removeTransactionListener(OATransactionListener tl) {
		al.remove(tl);
	}

	// used by TransactionListeners to "store" information.
	private HashMap hm = new HashMap();

	public void put(Object key, Object value) {
		hm.put(key, value);
	}

	public Object get(Object key) {
		return hm.get(key);
	}

	public Object remove(Object key) {
		return hm.remove(key);
	}

	public void executeOpenBatches() {
		for (OATransactionListener tl : al) {
			tl.executeOpenBatches(this);
		}
	}

}
