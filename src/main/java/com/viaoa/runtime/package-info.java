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
package com.viaoa.runtime;


/*qqqqqqqq
CODEX

  3. Top Runtime Invariants Excluding Thread-Local Details
  - OARuntime is the only public runtime service entry point.
  - The default graph is a single instance for the lifetime of the runtime.
  - createGraph(pkg) is idempotent.
  - graph(pkg) never silently returns default after package graph creation has failed.
  - Graph lookup by class follows a documented class-canonicalization rule.
  - Runtime graph helper caches are invalidated on all graph lifecycle changes, including failures/resets.
  - Datasource registry order is deterministic.
  - Datasource registry lifecycle is explicit and test-resettable.
  - Runtime direct package has no JDBC/Jackson/REST/Web/vendor dependencies.
  - UI-thread detection remains a replaceable hook, not a hard runtime dependency.

  4. Runtime Test Plan

  - OARuntimeDefaultGraphTest: default graph singleton across graph(), defaultGraph(), graph(""), createGraph("").
  - OARuntimeGraphFailureTest: failed createGraph(pkg) cannot be bypassed by prior helper-cache fallback.
  - OARuntimeGraphRetryResetTest: graph exception cache behavior is either permanent by contract or resettable.
  - OARuntimeGraphCanonicalClassTest: direct OAObject class, subclass, proxy/helper subclass, and cross-package
    subclass route correctly.
  - OARuntimeGraphNullContractTest: all null overloads behave consistently.
  - OADataSourceServiceOrderTest: register order, getLast behavior, disabled datasource skipping, setPosition.
  - OADataSourceServiceConcurrencyTest: concurrent register/unregister/reorder does not corrupt deterministic
    registry state.
  - OADataSourceServiceLifecycleTest: runtime/test reset clears or preserves datasource registry by explicit
    contract.


*/
