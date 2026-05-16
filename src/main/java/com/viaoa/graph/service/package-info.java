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
package com.viaoa.graph.service;


/*qqqqqqqqqqqqqqqqqqq
CODEX



 G. Top Invariants

  - GRAPH_PARENT_SERVICES_INITIALIZE_ONCE
  - GRAPH_CHILD_SERVICE_CREATION_IS_SINGLE_AND_AFTER_PARENT_INIT
  - GRAPH_CHILD_SERVICES_ARE_NOT_PUBLIC_APP_SURFACE
  - GRAPH_SYNC_ROLE_TRANSITIONS_ARE_ATOMIC
  - GRAPH_CHILD_SYNC_HOOKS_USE_PARENT_ROLE_GUARDS
  - GRAPH_REPLICATION_USES_OWNING_SYNC_SERVICE
  - GRAPH_ASYNC_TRIGGER_PRESERVES_RUNTIME_THREAD_FLAGS

  H. Test Plan Outline

  - Parent init: null dependencies, duplicate init, concurrent init/getter access.
  - Child ownership: no external package calls child service getters.
  - Sync lifecycle: concurrent role creation/start/stop.
  - Sync role guards: client/server-only operations in wrong roles.
  - Replication: cannot start without owning sync server.
  - Trigger: graph target routing, async thread-local preservation, executor lifecycle.





*/