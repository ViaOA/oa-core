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

	public void commit(OATransaction t);

	public void rollback(OATransaction t);

	public void executeOpenBatches(OATransaction t);
}
