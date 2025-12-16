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
/**
 * Undo and redo support for OA applications.
 * <p>
 * This package provides an {@link javax.swing.undo.UndoManager}-compatible
 * implementation tailored to OA's domain model, Hubs, and property-change
 * architecture. It allows UI frameworks and controllers to perform
 * application-level undo/redo for:
 * <ul>
 *   <li>Hub operations (add, remove, insert, move),</li>
 *   <li>active-object changes,</li>
 *   <li>OAObject property changes,</li>
 *   <li>compound edit grouping,</li>
 *   <li>arbitrary custom undoable actions.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link com.viaoa.undo.OAUndoableEdit}</h3>
 * Represents a single reversible operation applied to a Hub or OAObject. It
 * implements {@link javax.swing.undo.UndoableEdit} and contains logic to undo
 * and redo:
 * <ul>
 *   <li>adding or removing an object from a Hub,</li>
 *   <li>moving or inserting objects,</li>
 *   <li>changing the active object of a Hub,</li>
 *   <li>changing an OAObject property.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.undo.OAUndoManager}</h3>
 * Extension of Swing's {@code UndoManager} with OA-specific features:
 * <ul>
 *   <li>thread-local ignore counters to suppress recursive edits,</li>
 *   <li>compound-edit support for grouping multiple operations,</li>
 *   <li>integration with {@code OAThreadLocalDelegate} to capture OAObject
 *       property changes automatically,</li>
 *   <li>global verbosity and ignore-all flags.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Typical usage involves:
 * <ol>
 *   <li>creating a global OAUndoManager,</li>
 *   <li>wrapping user actions in undoable edits,</li>
 *   <li>using compound edits for multi-step changes,</li>
 *   <li>binding Undo/Redo menu items to the OAUndoManager.</li>
 * </ol>
 *
 * <p>
 * The undo subsystem is used extensively by OA GUI and OA Web controllers to
 * provide intuitive, reversible interactions with complex object graphs.
 */
package com.viaoa.undo;
