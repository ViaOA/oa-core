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

/**
 * Listener interface used by {@link OATransaction} to notify datasources and
 * other subsystems of transactional boundaries.
 * <p>
 * When a transaction is active, a datasource retrieves the current
 * {@link OATransaction} via {@code OAThreadLocalDelegate.getTransaction()} and
 * registers a listener that will receive:
 * <ul>
 *   <li>{@link #commit(OATransaction)} — finalize operations,</li>
 *   <li>{@link #rollback(OATransaction)} — undo or discard work,</li>
 *   <li>{@link #executeOpenBatches(OATransaction)} — execute pending batch
 *       operations prior to commit.</li>
 * </ul>
 *
 * <p>
 * Implementations are free to store temporary state in the transaction’s
 * key/value map using {@link OATransaction#put(Object, Object)}.
 */
public interface OATransactionListener {

	/**
	 * Invoked when a transaction is committed. Implementations should finalize
	 * any pending operations associated with the transaction.
	 *
	 * @param t the transaction being committed.
	 */
	public void commit(OATransaction t);

	/**
	 * Invoked when a transaction is rolled back. Implementations should discard
	 * or undo work performed during the transaction.
	 *
	 * @param t the transaction being rolled back.
	 */
	public void rollback(OATransaction t);

	/**
	 * Called before commit to allow execution of any deferred or batched
	 * operations accumulated during the transaction.
	 *
	 * @param t the transaction associated with the pending batch operations.
	 */
	public void executeOpenBatches(OATransaction t);
}
