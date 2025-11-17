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
/**
 * Transaction management for OA's thread-local execution model.
 * <p>
 * The classes in this package provide lightweight transactional semantics for
 * operations performed within a single thread. A transaction:
 * <ul>
 *   <li>is created and started explicitly,</li>
 *   <li>is associated with the current thread via
 *       {@link com.viaoa.object.OAThreadLocalDelegate},</li>
 *   <li>notifies registered {@link com.viaoa.transaction.OATransactionListener}
 *       instances on commit or rollback,</li>
 *   <li>supports batch modes for datasources that optimize multi-row
 *       operations,</li>
 *   <li>can override datasource read-only restrictions when appropriate.</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * OATransaction tx = new OATransaction();
 * tx.setUseBatch(true);
 * tx.start();
 * try {
 *     // perform work that involves datasources or OAObjects
 *     tx.commit();
 * }
 * catch (Exception e) {
 *     tx.rollback();
 * }
 * }</pre>
 *
 * <h2>Listeners</h2>
 * Datasources and other subsystems register a listener with the active
 * transaction. Listeners receive:
 * <ul>
 *   <li>{@code commit}</li>
 *   <li>{@code rollback}</li>
 *   <li>{@code executeOpenBatches}</li>
 * </ul>
 * allowing them to finalize their work atomically.
 *
 * <p>
 * Transactions in OA are intentionally simple: they track only in-memory
 * behavior and event sequencing. Database-level transaction boundaries are
 * controlled by the underlying datasource implementations.
 */
package com.viaoa.transaction;
