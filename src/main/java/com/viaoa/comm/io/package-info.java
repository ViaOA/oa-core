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
 * Provides customized serialization and deserialization utilities for OA
 * applications. This package supports long-lived OA systems where object
 * models evolve over time, package names change, or obsolete classes must
 * remain readable within legacy serialized streams.
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.comm.io.OAObjectInputStream OAObjectInputStream} –
 *       Extends {@link java.io.ObjectInputStream} to dynamically remap package
 *       names, rename classes, or replace missing classes during deserialization.
 *       This enables backward compatibility across model refactoring and package
 *       restructuring.</li>
 *
 *   <li>{@link com.viaoa.comm.io.IODummy IODummy} –
 *       Placeholder OAObject created when a serialized class no longer exists.
 *       Ensures that legacy streams can still be read without introducing stale
 *       or invalid data into the object graph.</li>
 * </ul>
 *
 * <h2>Purpose</h2>
 * <p>The utilities in this package allow OA applications to safely deserialize
 * legacy data even when:</p>
 * <ul>
 *   <li>class names have been changed,</li>
 *   <li>packages have been reorganized,</li>
 *   <li>objects have been removed from the model, or</li>
 *   <li>older snapshots reference types that the current system no longer uses.</li>
 * </ul>
 *
 * <p>By preserving compatibility with historical data streams, OA supports
 * long-term application evolution without sacrificing stability or requiring
 * brittle manual migration scripts.</p>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>Safe, flexible deserialization of OAObject graphs</li>
 *   <li>Automatic handling of missing or renamed classes</li>
 *   <li>Minimal intrusion into application code</li>
 *   <li>Preservation of object graph integrity</li>
 *   <li>Support for versioned models and evolving metadata</li>
 * </ul>
 *
 * <p>This package is a foundational part of OA’s ability to evolve its models
 * over time while maintaining accessibility to archived or serialized data.</p>
 */
package com.viaoa.comm.io;
