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

/* CODEX Invariants

Below is a fuller invariant list I would use as the hardening checklist for OA core.

  Graph Runtime

  1. OAGraph initialization is atomic.
  2. No caller can observe a partially initialized graph.
  3. Failed graph initialization leaves the graph retryable or permanently failed, but never falsely initialized.
  4. Every graph service is initialized exactly once per graph instance.
  5. Services that depend on each other have a deterministic initialization order.
  6. A graph service cannot be used before its required dependencies exist.
  7. Global/static graph state cannot accidentally mix objects from different graph/runtime instances.
  8. Shutdown/close, if supported, releases services, listeners, executors, queues, and caches deterministically.
  9. Runtime context is restored after temporary context changes, even on exception.
  10. Thread-local graph/runtime state is never leaked across unrelated work.

  OAObject Identity

  1. Every live OAObject has exactly one logical identity for its class/key.
  2. Two different live objects for the same class/key cannot both be canonical cached objects.
  3. Object key mutation updates all indexes atomically.
  4. Failed key mutation leaves the old key/index state intact.
  5. Temporary/new-object keys cannot collide with persisted-object keys.
  6. An object’s identity is stable while it is inside hash/index/cache structures.
  7. Equality/cache lookup/serialization identity all agree on the same key model.
  8. Object identity is not dependent on mutable non-key properties.
  9. A deserialized object resolves to the canonical cached instance when required.
  10. Object identity transitions are observable in a consistent order.

  Object Cache

  1. Cache add is atomic: object is either fully indexed or not cached.
  2. Cache remove is atomic: object is removed from every index.
  3. Cache update never leaves stale old-key entries.
  4. Cache lookup by object key and by alternate/business key return the same canonical object.
  5. Cache listeners are notified only after cache state is internally consistent.
  6. Cache listener failure does not corrupt cache indexes.
  7. Weak-reference cache structures are periodically cleaned or cannot grow unbounded.
  8. Cache listeners/triggers have deterministic unregister paths.
  9. Cache visit cannot throw halfway and leave internal traversal state corrupted.
  10. Cache operations are thread-safe against concurrent add/remove/update/lookup.

  Object Lifecycle Flags

  1. If isNew == false, the object has either been saved or intentionally represents durable datasource state.
  2. If isChanged == false, no unsaved local changes are pending.
  3. If isDeleted == false, the object is valid for normal cache/hub use.
  4. A failed save cannot clear changed.
  5. A failed insert cannot clear new.
  6. A failed delete cannot mark the object deleted unless partial-delete semantics are explicit.
  7. A failed restore from deleted cannot mark the object not-deleted.
  8. Lifecycle flag changes are ordered before/after events consistently.
  9. Lifecycle events reflect committed in-memory state, not intended future state.
  10. Lifecycle state cannot advance past datasource durability unless explicitly documented.

  Property Changes

  1. Before-change events fire before the value changes.
  2. After-change events fire after the value changes.
  3. If validation fails, the property value and all derived state remain unchanged.
  4. Old/new values in events match actual object state transition.
  5. Calculated/dependent properties fire after their dependencies are consistent.
  6. Recursive property changes cannot cause infinite trigger recursion.
  7. Property changes caused by remote sync are distinguishable where needed.
  8. Property changes during loading do not incorrectly mark objects dirty unless intended.
  9. Property change events are not lost because of listener mutation during dispatch.
  10. Listener exceptions cannot leave the object half-updated.

  Hub Membership

  1. A Hub contains only objects compatible with its object class.
  2. An object appears at most once in a Hub unless duplicate membership is explicitly supported.
  3. Hub.size, indexed access, iteration, and contains agree.
  4. Add/remove events reflect completed membership changes.
  5. Failed add/remove leaves Hub membership unchanged.
  6. Object-to-Hub backreferences agree with Hub membership.
  7. Removing an object from a Hub removes the matching object-to-Hub reference.
  8. Clearing a Hub removes all object-to-Hub references.
  9. Hub iteration is safe against expected event-driven mutations or fails predictably.
  10. Hub active object always refers to an object in the Hub, or is null/pos -1.

  Hub Active Object

  1. Active object and active position agree.
  2. Active position is never outside the Hub bounds except the explicit null position.
  3. Active-object change events fire after AO state is internally consistent.
  4. Shared active-object state is either shared by identity or fully independent.
  5. Link-to-Hub active-object calculations cannot produce stale AO values.
  6. Changing AO updates dependent detail Hubs exactly once.
  7. AO updates cannot recurse indefinitely through shared/detail/link Hubs.
  8. Failed AO update leaves previous AO state intact.
  9. AO state is thread-safe relative to Hub membership changes.
  10. AO sharing honors the caller’s explicit share/non-share setting.

  Hub Sharing

  1. Shared Hub graphs are acyclic.
  2. A Hub cannot share with itself.
  3. A Hub cannot share data with an incompatible object class.
  4. Shared Hubs point to one canonical main shared Hub.
  5. getMainSharedHub always terminates.
  6. Shared child lists do not contain stale strong references.
  7. Weak shared-Hub references are cleaned without corrupting traversal.
  8. Changing a Hub’s shared master detaches it from the old master exactly once.
  9. All Hubs sharing data see the same membership ordering.
  10. All shared Hubs update listener caches when sharing topology changes.

  Detail Hubs / Master-Detail

  1. A detail Hub’s master object and master Hub agree.
  2. A detail Hub contains exactly the objects reachable through its master link.
  3. Changing master AO refreshes detail Hub contents consistently.
  4. Unloaded detail links have clear lazy-load semantics.
  5. Failed detail load does not clear valid existing detail contents unless explicitly intended.
  6. Detail Hub link metadata is validated before listener/controller installation.
  7. Recursive detail relationships cannot loop forever.
  8. Removing from a detail Hub updates the reverse/master link consistently.
  9. Adding to a detail Hub updates the reverse/master link consistently.
  10. Detail Hub lifecycle does not leak listeners after the owning Hub/controller is abandoned.

  Hub Controllers

  1. Any controller that installs listeners has close().
  2. close() is idempotent.
  3. close() removes every listener, trigger, dependent property registration, and background task it created.
  4. Controller finalizers are not required for correctness.
  5. APIs that create live controllers either return them or bind their lifetime to an owning object.
  6. Controller construction is atomic: failed construction unregisters partial listeners.
  7. Controller state cannot be updated after close.
  8. Source Hub replacement detaches from old source before attaching to new source.
  9. Controller-created result Hubs remain correct while the controller is live.
  10. Repeated creation of controllers does not create unbounded listener growth.

  Hub Filtering / Sorting / Merging / Flattening

  1. Filtered Hubs reflect source Hub membership after every source add/remove/change.
  2. Sort order is deterministic and stable for equal keys where required.
  3. Comparator/property failures do not corrupt Hub ordering.
  4. Merged Hubs do not duplicate objects unless explicitly allowed.
  5. Flattened Hubs terminate on recursive graphs.
  6. Source listener removal happens when filter/merger/flattened controller closes.
  7. Dependent-property filters unregister dependent listeners/triggers.
  8. Background rebuilds cannot leave permanent “loading” flags.
  9. Rebuild cancellation leaves the previous valid result or a clearly empty result, not partial state.
  10. Result Hub events are ordered consistently with source changes.

  Save Semantics

  1. If save() returns normally, all required datasource operations succeeded.
  2. If datasource save fails after retries, the caller receives an exception.
  3. Save retry does not duplicate inserts or relationship updates.
  4. Save clears changed only after durable success.
  5. Save clears new only after durable insert success.
  6. Save cascades references in a deterministic order.
  7. Failed cascade save reports which object failed or preserves enough context.
  8. Save does not fire after-save events unless save actually succeeded.
  9. Save does not send remote/sync messages for changes that failed durability.
  10. Concurrent save of the same object cannot interleave into inconsistent state.

  Delete Semantics

  1. If delete() returns normally, datasource delete and in-memory delete agree.
  2. Failed datasource delete does not remove the object from live Hubs.
  3. Failed delete does not mark object deleted unless explicitly partial.
  4. Delete cascades are deterministic and cycle-safe.
  5. Cascade delete either completes consistently or exposes partial failure.
  6. Many-to-many cleanup happens only after the main delete is safe, or is rollback-safe.
  7. Delete fires after-delete events only after the object is actually deleted.
  8. Delete removes object from cache exactly once.
  9. Delete removes object from all Hubs consistently.
  10. Delete notifications to clients occur only after server-side state is consistent.

  Lazy Loading / Select

  1. A link marked loaded has actually completed loading successfully.
  2. Failed lazy load does not mark the link loaded.
  3. Concurrent lazy load for the same link coalesces or resolves consistently.
  4. Select cancellation releases datasource resources.
  5. Select fetch state cannot deadlock or spin forever.
  6. loadAllData completion flag means all data was loaded successfully.
  7. Background loading counters always decrement.
  8. Stop/cancel cannot leave loaders permanently running.
  9. Loader waitUntilDone() always terminates after completion/cancel.
  10. Lazy loading does not create duplicate cached objects.

  Triggers

  1. A trigger is either fully registered or not registered.
  2. Failed trigger registration rolls back counters and dependent triggers.
  3. Removing a trigger removes all dependent triggers.
  4. Trigger counters equal the actual registered trigger count.
  5. Background trigger execution preserves event ordering where required.
  6. Trigger queues are bounded or have backpressure/monitoring.
  7. Trigger listener exceptions do not kill trigger infrastructure.
  8. Recursive triggers have a maximum depth or cycle guard.
  9. Server-side-only trigger context is restored after execution.
  10. Trigger close/removal is idempotent and thread-safe.

  Events / Listeners

  1. Listener lists can be modified during dispatch without losing current event correctness.
  2. Listener dispatch order is deterministic where behavior depends on order.
  3. Listener exceptions are either isolated or explicitly fail the operation.
  4. Before-event listeners can veto only before state changes.
  5. After-event listeners cannot observe partial internal state.
  6. Weak listener structures do not leak stale entries unbounded.
  7. Strong listener structures have explicit removal paths.
  8. Event source, object, old value, new value, and Hub position are accurate.
  9. Remote/sync-generated events do not echo indefinitely.
  10. Event suppression is scoped and restored after exception.

  Thread Safety

  1. Every shared mutable structure has a clear synchronization owner.
  2. Check-then-act operations on shared state are atomic.
  3. Every counter increment has a guaranteed decrement.
  4. Every wait has a corresponding notify, timeout, or cancellation path.
  5. No lock is held while calling arbitrary user code unless explicitly required.
  6. Lock ordering is consistent across object/cache/Hub services.
  7. Background workers cannot outlive their owning service silently.
  8. Shutdown waits for or cancels background work deterministically.
  9. Volatile/atomic fields are used consistently with compound invariants.
  10. Concurrent close/use either succeeds safely or fails predictably.

  Serialization

  1. Serialized identity matches cache identity.
  2. Deserialization cannot create duplicate canonical objects unless explicitly detached.
  3. Lazy/unloaded references serialize according to a defined policy.
  4. Circular object graphs serialize without infinite recursion.
  5. Partial serialization failure does not mutate source objects.
  6. Remote serialization preserves object key, new/changed/deleted state only when intended.
  7. JSON and Java serialization agree on identity/reference semantics where required.
  8. Deserialized Hubs preserve ordering.
  9. Deserialized Hub AO state is valid relative to membership.
  10. Serialization does not accidentally trigger lazy loads unless explicitly requested.

  Remote / Sync / Replication Facing

  1. Local state changes are published remotely only after local invariants hold.
  2. Remote-applied changes do not echo back endlessly.
  3. Request/response correlation uses stable object/session identity.
  4. Message ordering preserves causality for object property and Hub membership changes.
  5. Reconnect never reuses stale session state incorrectly.
  6. Duplicate remote messages are idempotent where retries are possible.
  7. Remote delete/save conflicts resolve deterministically.
  8. Sync messages cannot refer to objects not yet known without a defined resolution path.
  9. Replication preserves object identity and relationship ordering.
  10. Failed remote apply leaves local state unchanged or explicitly marked conflicted.

  Resource Lifecycle

  1. Every executor has an owner and close path.
  2. Every queue has a bound, drain policy, or owner close path.
  3. Every listener registration has a matching unregister.
  4. Every cache listener has a matching unregister.
  5. Every trigger has a matching remove.
  6. Every datasource select/cursor closes on success, failure, and cancel.
  7. Every temporary Hub/controller can be garbage-collected after use.
  8. Finalizers are cleanup fallback only, never primary correctness.
  9. Abandoned dead/disconnected sessions do not matter, but live instances must recover or fail closed.
  10. Repeated API use does not cause unbounded live memory growth.

  Testing-Oriented Meta-Invariants

  1. Every public lifecycle method has tests for success, failure, and exception during listener callback.
  2. Every async component has tests for stop/cancel before start, during run, and after completion.
  3. Every cache/key mutation has tests for collision and rollback.
  4. Every controller has tests proving close() removes listeners.
  5. Every save/delete path has tests where datasource throws.
  6. Every Hub sharing/detail path has tests for cycles or recursive object models.
  7. Every trigger path has tests for dependent calculated properties.
  8. Every remote-facing state transition has ordering tests.
  9. Every weak-reference cleanup path has a stress/leak test.
  10. Every “should never happen” state has an assertion, exception, or recovery path.

  qqqqqqqqqqqqq more from code reviews
  
  E. Top Runtime Invariants

  - OARuntime is the only public runtime service entry point.
  - Graph lookup returns the most specific registered package graph, otherwise default graph.
  - sendSyncMessages defaults true and is only changed through OAThreadLocalService.
  - startServerOnly/endServerOnly are balanced and restore previous sendSyncMessages.
  - Remote threads start every request from clean thread-local state.
  - Remote request info has one canonical source.
  - Thread-local scoped counters never go negative.
  - Global fast-path counters match real per-thread state.
  - Hub event stack is push/pop balanced.
  - Locks held by a thread-local are released or explicitly detected before thread reuse.
  - Runtime core has no UI/Jackson/JDBC/Web dependencies.

 F. Test Plan Outline

  - OARuntimeGraphTest: default graph, package graph, subpackage graph, helper cache invalidation.
  - OADataSourceServiceTest: registration order, getLast precedence, disabled datasource skip, setPosition.
  - OAThreadLocalSendSyncTest: default true, explicit false/true, nested server-only restore, underflow
    guard.
  - OAThreadLocalCounterTest: loading/refreshing/hub-merger/tree/undo counters balanced and never negative.
  - OAThreadLocalLifecycleTest: clear(), context/admin/process/replication source cleanup behavior.
  - OARemoteThreadResetTest: reused remote thread starts clean across requests.
  - OARemoteRequestInfoTest: remote thread and thread-local request info stay consistent.
  - OAThreadContextPropagationTest: OAThread restores context and cleans up on exceptions.
  - OAThreadLocalLockTest: reentrant lock, competing lock, release-all, deadlock release behavior.
  - OAThreadLocalHubEventTest: max event depth, add/remove balance, calc-property dedupe reset.
  - RuntimeBoundaryTest: no forbidden imports in runtime packages.

 G. Looks Sound

  - OARuntime as static entry point over runtime-owned services is aligned with OA 4.0 direction.
  - Graph ownership belongs in runtime and currently avoids moved-module dependencies.
  - Datasource service depends only on datasource contracts, not JDBC/REST implementations.
  - Serialization state uses OAObjectSerializer, which is the right abstraction boundary.
  - Remote-thread service centralizes remote-thread questions instead of scattering instanceof
    OARemoteThread.
  - sendSyncMessages is now centralized in OAThreadLocalService; the main remaining work is proving
    balanced scopes and reset behavior.





*/









