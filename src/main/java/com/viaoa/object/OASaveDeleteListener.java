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
package com.viaoa.object;

/**
 * Listener interface for receiving lifecycle notifications when
 * {@link OAObject}s are inserted, updated, or deleted.
 *
 * <p>These callbacks are triggered by persistence delegates during
 * {@code save()} and {@code delete()} operations, allowing applications
 * to react to data-change events without subclassing the model.</p>
 *
 * <p><b>Callbacks</b>:
 * <ul>
 *   <li>{@link #onInsert(OAObject)} — called after an object is first persisted.</li>
 *   <li>{@link #onUpdate(OAObject)} — called after an existing record is modified.</li>
 *   <li>{@link #onDelete(OAObject)} — called after an object is removed.</li>
 * </ul>
 *
 * <p>These hooks are used by auditing, synchronization, and UI refresh logic.</p>
 */
public interface OASaveDeleteListener {
	public void onInsert(OAObject obj);
	public void onUpdate(OAObject obj);
	public void onDelete(OAObject obj);
}
