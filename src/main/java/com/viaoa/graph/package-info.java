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
package com.viaoa.graph;


/*qqqqqqqqqqqqqqqqqqq
CODEX


 F. Top Graph Invariants

  - OAGraph verbs operate on objects/classes/Hubs owned by that graph, or explicitly reroute by runtime graph
    lookup.
  - OAGraphInternal is not application API.
  - Graph initialization is atomic.
  - Sync role transitions are atomic and contract-consistent.
  - Null-context server authority is deliberate and tested.
  - Context removal clears Hub and user-access state.
  - Trigger registration target and async lifecycle are explicit.

  G. Test Plan Outline

  - Graph lifecycle: successful init, failed init, package scan failure, repeated init.
  - Multi-graph ownership: create/get/save/delete/select/trigger with matching and foreign model classes.
  - Sync lifecycle: unconfigured/start, server/client create races, start/stop/restart, role predicates.
  - Context/user access: null context, server/client authority, remove context, Hub-rooted access rules.
  - Trigger behavior: explicit target graph, async context propagation, executor lifecycle/overload.
  - Replication: public contract decision, invalid start, role setup.

  H. Looks Sound
  The package split itself looks structurally sound. I found no direct JSON/XML/YAML/Jackson/JDBC/REST/UI
  contamination in com.viaoa.graph. The graph package mostly depends on legitimate OA kernel pieces: runtime,
  metadata, object, hub, sync/replication/remoting, serialization contracts, traversal, query/find/filter/select.
  The remaining risk is not module purity; it is tightening graph ownership, lifecycle, sync semantics, and context/
  trigger invariants.





G. Top Context/Sibling Invariants

  CTX-REMOVE-NULL-CLEARS-ALL
  CTX-ACCESS-LIFETIME-DETERMINISTIC
  UA-PACKAGE-SCOPE-CONSISTENT
  UA-EMPTY-PATH-NO-THROW
  UA-REVERSE-PATH-BOUNDS
  UA-CONFIGURE-BEFORE-PUBLISH
  SIB-SAME-THREAD-ENFORCED

  H. Test Plan Outline

  Add focused tests for null-context registration/removal, weak-vs-strong context lifetime contract, package-scoped
  enabled/visible behavior, empty/scalar access paths, reverse traversal bounds, configure-before-publish access
  rules, and cross-thread sibling-helper use.









 C. Consolidated Graph-Level Invariants

  GRAPH-OWNERSHIP-ROUTING-IS-EXPLICIT
  GRAPH-LIFECYCLE-IS-ATOMIC-AND-BOUNDED
  GRAPH-INTERNAL-APIS-ARE-NOT-APPLICATION-SURFACE
  GRAPH-SYNC-ROLE-GUARDS-ARE-CENTRALIZED
  GRAPH-ASYNC-WORK-PRESERVES-RUNTIME-CONTEXT
  GRAPH-OBJECT-HUB-MUTATIONS-STAY-BALANCED
  GRAPH-SERIALIZATION-PRESERVES-AUTHORITATIVE-IDENTITY
  GRAPH-TRIGGER-TARGET-AND-EXECUTOR-LIFECYCLE-ARE-EXPLICIT

  D. Consolidated Object-Service Invariants

  OBJ-GUID-STABLE-AFTER-INIT
  OBJ-KEY-COMPARISON-DETERMINISTIC
  OBJ-CACHE-HAS-ONE-AUTHORITATIVE-INSTANCE
  OBJ-SAVE-FAILURE-PRESERVES-STATE
  OBJ-DELETE-CLEANS-CACHE-KEYS-HUB-REFS
  OBJ-SER-READ-RESOLVE-USES-CACHE-AUTHORITY
  OBJ-SYNC-HOOKS-REQUIRE-VALID-ROLE
  OBJ-PRIMITIVE-NULL-MASKS-INITIALIZED

  E. Consolidated Hub-Service Invariants

  HUB-MEMBERSHIP-AND-OBJECT-REFS-BALANCED
  HUB-AO-DETAIL-ORDER-DETERMINISTIC
  HUB-DETAIL-MASTER-LINKS-CONSISTENT
  HUB-SHARE-LINK-SCOPE-EXPLICIT
  HUB-EVENTS-FIRE-AFTER-SUCCESSFUL-MUTATION
  HUB-SELECT-STATE-RESTORED-ON-FAILURE
  HUB-DELETEALL-FAILURE-DOES-NOT-CORRUPT-GRAPH
  HUB-SER-SIDE-EFFECTS-BOUNDED

  F. Context/Sibling Invariants

  CTX-REMOVE-NULL-CLEARS-ALL
  CTX-ACCESS-LIFETIME-DETERMINISTIC
  UA-PACKAGE-SCOPE-CONSISTENT
  UA-EMPTY-PATH-NO-THROW
  UA-REVERSE-PATH-BOUNDS
  UA-CONFIGURE-BEFORE-PUBLISH
  SIB-SAME-THREAD-ENFORCED




*/