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

/*qqqqqqq
CODEX


 H. Top Hub-Service Invariants

  - HUB-CS-ROLE: single-user/server/client routing is explicit.
  - HUB-MEMBERSHIP: vector membership and OAObject hub references stay balanced.
  - HUB-AO: AO changes update detail hubs before after-AO events.
  - HUB-DETAIL: detail/master links stay consistent across add/remove/AO changes.
  - HUB-SHARE: shared hubs share only intended data/data-active structures.
  - HUB-EVENT: after-events only fire after successful state mutation.
  - HUB-SELECT: fetch/load/refresh restore select flags on failure.
  - HUB-DELETE: failed delete-all does not leave partial graph corruption.
  - HUB-SER: serialization side effects are deterministic and bounded.

 I. Test Plan Outline

  Role matrix tests for add/remove/insert/move/sort/deleteAll/refresh in single-user, server, and client modes.

  Lifecycle tests for add/remove/clear/deleteAll with owned, M2M, detail, shared, filtered, sorted, and recursive
  hubs.

  Event tests asserting order and non-firing on failed mutation.

  Select tests for fetch contention, refresh failure, select order null/empty, partially loaded Hubs, and selectAll
  cache registration.

  Serialization tests for partially loaded Hubs and duplicate Hub readResolve membership.

  J. Looks Sound

  The package boundary is clean: Hub runtime behavior remains in core and does not leak format/UI/vendor modules.
  The service separation is mostly appropriate: parent orchestration, child semantic services, and abstract parent
  hooks are a good OA 4.0 shape. The main remaining risk is stale sync-role assumptions and missing failure-state
  invariants, not package placement.

*/

package com.viaoa.graph.service.hub;
