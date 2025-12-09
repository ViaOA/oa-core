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
 * Core collection and event framework for OA — the {@code Hub}.
 *
 * <p>The {@code com.viaoa.hub} package defines OA’s reactive data layer,
 * centered around the {@link com.viaoa.hub.Hub Hub} class. A Hub acts as a
 * dynamic, observable collection of {@link com.viaoa.object.OAObject OAObject}
 * instances, maintaining both content and state (such as the active object)
 * and serving as the foundation for binding, synchronization, and messaging
 * across the OA runtime.</p>
 *
 * <h2>Overview</h2>
 * <p>Hubs are lightweight, observable, and linkable collections that enable
 * the OAObject Graph to connect data, UI, and services. They provide:</p>
 * <ul>
 *   <li>Master–detail relationships between Hubs (via
 *       {@link com.viaoa.hub.HubDetailDelegate HubDetailDelegate}).</li>
 *   <li>Linking to reference properties of other Hubs (via
 *       {@link com.viaoa.hub.HubLinkDelegate HubLinkDelegate}).</li>
 *   <li>Shared collections and shared active objects (via
 *       {@link com.viaoa.hub.HubShareDelegate HubShareDelegate}).</li>
 *   <li>Automated observability and event dispatching for all object and
 *       Hub-level changes (through {@link com.viaoa.hub.HubEvent HubEvent}
 *       and {@link com.viaoa.hub.HubListener HubListener}).</li>
 *   <li>Filtering, sorting, grouping, merging, and sampling mechanisms to
 *       transform and synchronize Hub contents in real time.</li>
 * </ul>
 *
 * <h2>Internal Architecture</h2>
 * <p>Each Hub maintains a set of delegate components that separate behavior
 * into specialized responsibilities:</p>
 * <ul>
 *   <li>{@link com.viaoa.hub.HubData HubData} — holds the core collection and
 *       metadata such as the active object and listener lists.</li>
 *   <li>{@link com.viaoa.hub.HubDataUnique HubDataUnique} and
 *       {@link com.viaoa.hub.HubDataActive HubDataActive} — manage Hub
 *       identity and active-object tracking across shared and detail Hubs.</li>
 *   <li>{@link com.viaoa.hub.HubDelegate HubDelegate} — the primary façade
 *       coordinating operations across all delegates.</li>
 *   <li>Other delegates provide specialized services (selecting, linking,
 *       saving, serialization, etc.), allowing modular composition of
 *       functionality.</li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Master–Detail Wiring:</b> Detail Hubs automatically mirror the
 *       collection from the master’s active object reference.</li>
 *   <li><b>Shared Hubs:</b> Multiple Hubs can share the same data and events
 *       for synchronized UIs or parallel processing.</li>
 *   <li><b>Observability:</b> All Hub and OAObject events propagate upward
 *       through listeners, enabling reactive updates and distributed
 *       synchronization.</li>
 *   <li><b>Temporary and Recursive Support:</b>
 *       {@link com.viaoa.hub.HubTemp HubTemp} provides lightweight
 *       one-object contexts, and {@link com.viaoa.hub.HubRootDelegate
 *       HubRootDelegate} manages recursion roots.</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>The Hub framework encapsulates the "observable object graph" pattern
 * central to OA. It separates collection management, event propagation,
 * and synchronization logic from the domain model while remaining fully
 * type-safe and reflection-aware. Its design emphasizes:</p>
 * <ul>
 *   <li>Minimal overhead and explicit visibility of relationships.</li>
 *   <li>Loose coupling between model, UI, and data source layers.</li>
 *   <li>Distributed, event-driven synchronization with optional persistence.</li>
 * </ul>
 *
 * @author ViaOA
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.hub.HubDelegate
 * @see com.viaoa.object.OAObject
 */
package com.viaoa.hub;
