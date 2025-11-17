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
 * Core object framework classes that define OA's runtime identity, event, and lifecycle model.
 * <p>
 * This package contains {@link com.viaoa.object.OAObject}, the base class for all persistent
 * and transient domain entities, along with its internal helper classes that together form
 * the core of the Object Graph runtime.
 * <p>
 * Classes prefixed with {@code OAObject*Delegate}, {@code OAObject*Helper}, or
 * {@code OAObject*Cache} are internal support classes used by {@link com.viaoa.object.OAObject}
 * to manage state, identity, event propagation, caching, cascading, and synchronization.
 * These helpers rely on package-level access to OAObject internals to maintain performance
 * and strict encapsulation—avoiding reflection or public exposure of internal fields.
 * <p>
 * External applications should interact with {@link com.viaoa.object.OAObject} and its
 * related APIs (such as {@link com.viaoa.hub.Hub}) rather than calling delegate classes
 * directly. The delegate layer is part of OA's internal implementation contract and may
 * evolve independently of the public API.
 * <p>
 * <b>Design goals:</b>
 * <ul>
 *   <li>High-performance, reflection-free internal architecture.</li>
 *   <li>Consistent object identity and referential integrity across the graph.</li>
 *   <li>Thread-safe mutation and deterministic event ordering.</li>
 *   <li>DataSource-agnostic persistence and distributed synchronization.</li>
 * </ul>
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.datasource.OADataSource
 */
package com.viaoa.object;
