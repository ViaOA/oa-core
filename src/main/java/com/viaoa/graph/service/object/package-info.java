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

/*qqqqqqqqqqqqqqq
 CODEX
 
 G. Top Object-Service Invariants

  - OBJ-ROLE: single-user, server, and client roles are distinct.
  - OBJ-ID: every live OAObject has one stable GUID after initialization.
  - OBJ-KEY: object identity comparison is deterministic across GUID/id-only forms.
  - OBJ-CACHE: cache add/resolve never creates duplicate authoritative objects for the same identity.
  - OBJ-SAVE: failed saves preserve new/changed/deleted state.
  - OBJ-DELETE: delete cleanup removes object and key references consistently.
  - OBJ-SER: deserialization resolves to the cached authoritative instance.
  - OBJ-SYNC: sync hooks are only invoked when the required sync service exists.
  - OBJ-NULLS: primitive-null masks exist before primitive-null mutation.

  H. Test Plan Outline

  - Role matrix tests: single-user, server, client for save/delete/cache refresh/serialization.
  - Save failure tests: datasource save failure, saveWithoutReferences failure, internal object-only save failure.
  - Identity tests: GUID-only, ID-only, mixed GUID/id keys, duplicate cache add, deserialize duplicate merge.
  - Delete cleanup tests: object reference, key reference, mixed key reference, loaded-only finder path.
  - Initialization tests: primitive-null objects through constructor, after-loading, and deserialization paths.
  - Serialization tests: normal stream and remote stream behavior with no sync client, sync client, and sync server.
  - Cache refresh tests: local datasource refresh vs client remote delegation.

 
 
 */


/**
 * Object-service machinery (sub-services).
 *
 * <p>This package contains internal sub-services that together implement the Object
 * functionality for a single {@code OAGraph} instance.</p>
 *
 * <p>These classes are not intended to be constructed directly. They are created,
 * wired, and managed by the owning coordinator ({@code OAObjectService}) in the
 * parent package.</p>
 *
 * <p>Sub-services may declare required outcomes via protected abstract "dependency hooks".
 * These hooks are implemented by the owner/coordinator and are intentionally used to:
 * <ul>
 *   <li>keep sub-services from depending on other services directly</li>
 *   <li>centralize coordination and lifecycle management</li>
 *   <li>prevent service-locator coupling</li>
 * </ul>
 * </p>
 */
package com.viaoa.graph.service.object;