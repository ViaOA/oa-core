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
 * Core package for OA (Object Automation), a model-driven, executable
 * object-graph framework designed for building large-scale, distributed,
 * real-time enterprise applications.
 * <p>
 * OA provides a complete end-to-end architecture centered around a richly
 * instrumented domain model. Rather than assembling dozens of external
 * frameworks, OA offers a coherent, tightly integrated platform where:
 * <ul>
 *   <li>the domain model is the application,</li>
 *   <li>object graphs are live, observable, and distributed,</li>
 *   <li>UI, server, datasource, and remote layers are all synchronized
 *       automatically,</li>
 *   <li>application logic emerges naturally through object relationships,
 *       Hubs, and metadata.</li>
 * </ul>
 *
 * <h2>Core Architectural Components</h2>
 *
 * <h3>OAObject / OAObjectGraph</h3>
 * Rich domain objects with:
 * <ul>
 *   <li>identity and GUID management,</li>
 *   <li>change tracking and edit state,</li>
 *   <li>lazy loading,</li>
 *   <li>metadata (properties, links, calculations),</li>
 *   <li>serialization with property-path control,</li>
 *   <li>graph traversal and visiting.</li>
 * </ul>
 *
 * <h3>Hub&lt;T&gt;</h3>
 * OA’s observable collection:
 * <ul>
 *   <li>master/detail relationships,</li>
 *   <li>active object (cursor) tracking,</li>
 *   <li>sharing and linking between controllers,</li>
 *   <li>filters, sorters, matchers, and live indexing,</li>
 *   <li>distributed sync of collection changes.</li>
 * </ul>
 *
 * <h3>Property Paths</h3>
 * A uniform dot-notation language used everywhere, including:
 * <ul>
 *   <li>filters and queries,</li>
 *   <li>templates,</li>
 *   <li>detail/sibling loading,</li>
 *   <li>JSON/XML serialization,</li>
 *   <li>UI binding,</li>
 *   <li>datasource column mapping.</li>
 * </ul>
 *
 * <h3>Datasources</h3>
 * Pluggable datasource implementations:
 * <ul>
 *   <li>JDBC (SQL databases),</li>
 *   <li>REST,</li>
 *   <li>Client/Server,</li>
 *   <li>ObjectCache,</li>
 *   <li>Multiplexer remote datasource,</li>
 *   <li>in-memory and hybrid combinations.</li>
 * </ul>
 * All datasources follow a unified API for select, iterator, insert, update,
 * and delete operations with cascade-aware behavior.
 *
 * <h3>Distributed Sync</h3>
 * OA includes a full multiplexer-based remote method invocation system:
 * <ul>
 *   <li>server → client broadcast of object and hub changes,</li>
 *   <li>client → server updates with edit-level granularity,</li>
 *   <li>remote object loading with depth/sibling rules,</li>
 *   <li>per-client sessions tracking GUIDs and locks,</li>
 *   <li>file transfer subsystem,</li>
 *   <li>real-time conflict detection.</li>
 * </ul>
 *
 * <h3>Templates</h3>
 * {@code OATemplate} provides a lightweight templating engine based on
 * property paths to generate:
 * <ul>
 *   <li>HTML,</li>
 *   <li>emails,</li>
 *   <li>documents,</li>
 *   <li>custom text formats.</li>
 * </ul>
 *
 * <h3>UI Framework Integration</h3>
 * {@code com.viaoa.uicontroller} provides MVC binding between:
 * <ul>
 *   <li>domain objects (OAObject),</li>
 *   <li>Hubs (collections),</li>
 *   <li>UI widgets across different frameworks.</li>
 * </ul>
 * Hubs define the live state, controllers simply bind UI widgets to hubs.
 *
 * <h3>JSON Serialization</h3>
 * {@code com.viaoa.json} and {@code com.viaoa.json.jackson} integrate with
 * Jackson to provide object-graph-aware serialization with identity and depth
 * management. Supports:
 * <ul>
 *   <li>full graph,</li>
 *   <li>partial graph,</li>
 *   <li>property-path-driven serialization,</li>
 *   <li>OA temporal types.</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * OA is intentionally:
 * <ul>
 *   <li><b>minimal</b> – few classes, little configuration, no XML, no heavy
 *       frameworks;</li>
 *   <li><b>model-driven</b> – the domain model defines behavior through
 *       metadata;</li>
 *   <li><b>executable</b> – the architecture is embodied in live objects and
 *       Hubs, not code generation glue;</li>
 *   <li><b>deterministic</b> – consistent object identity, consistent ordering,
 *       predictable sync behavior;</li>
 *   <li><b>observable</b> – changes flow automatically through the system;</li>
 *   <li><b>distributed-ready</b> – built from day one for multi-client sync.</li>
 * </ul>
 *
 * <p>
 * OA’s goal is to turn domain modeling into application logic, and application
 * logic into a live, distributed, synchronized object graph with minimal code.
 */
package com.viaoa;
