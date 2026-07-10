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
 * Provides the core context and permission-management subsystem for OA-based
 * applications. <p>
 *
 * The classes in this package define how an application's thread-local context
 * is associated with a logged-in OAObject, a Hub representing the active user,
 * and an {@link com.viaoa.session.OASessionAccess.OAContextAccess} instance that governs visibility
 * and enabled/disabled access across an OAObject graph. <p>
 *
 * Features include:
 * <ul>
 *   <li>Thread-local context identity.</li>
 *   <li>Context-bound user object and user Hub.</li>
 *   <li>Context-specific permission rules using OAUserAccess.</li>
 *   <li>Admin, super-admin, and “edit processed” rule resolution.</li>
 *   <li>Property-path–based inclusion testing for complex graphs.</li>
 * </ul>
 *
 * This package is used throughout OA's object-graph traversal, UI binding,
 * callbacks, and security enforcement layers to define how objects and
 * properties behave relative to user or system context.
 */

package com.viaoa.session;

//CODEX unit tests <todo>

/* CODEX Invariants

GRAPH-CONTEXT-001 — Graph Context Authority
Contract statement:
com.viaoa.graph.context is the semantic authority for graph-scoped runtime context: current context identity,
context user object, context Hub, and context-specific access policy must be resolved through this package before
context-dependent graph behavior is evaluated.
Rationale:
Object, Hub, datasource, transaction, remote, sync, replication, trigger, callback, and security behavior can depend
on runtime context, but this package owns the context binding contract.
Source scope:
package-info.java, OAContext, OAUserAccess.
Related CODEX findings:
Existing package-info has no Invariants block; OAContext and OAUserAccess comments identify missing/
vague context cleanup, lifetime, and access-evaluation contracts.
Suggested unit tests:
Verify context object, context Hub, and context user-access lookup are the authority used by representative context-
dependent operations.
Spec target section:
Graph-scoped runtime context authority.

GRAPH-CONTEXT-002 — Deterministic Context Lookup
Contract statement:
For the same context key and registered context state, context object, context Hub, user access policy, admin state,
super-admin state, and edit-processed state must resolve deterministically.
Rationale:
Runtime context participates in authorization, UI binding, graph traversal, and server/client behavior;
nondeterministic lookup would make graph behavior unstable.
Source scope:
OAContext.
Related CODEX findings:
CTX-ACCESS-LIFETIME-DETERMINISTIC notes that weakly held context registrations need an explicit lifetime contract.
Suggested unit tests:
Verify repeated lookups for null/default and explicit context keys return stable object, Hub, user-access, admin,
super-admin, and edit-processed results while the registration is valid.
Spec target section:
Deterministic runtime-context behavior.

GRAPH-CONTEXT-003 — Null Context Semantics
Contract statement:
A null context key represents the default graph/runtime context and must be normalized consistently for set, get,
remove, admin, super-admin, edit-processed, Hub, object, and user-access operations.
Rationale:
The default context is used by server and thread-local runtime behavior; inconsistent null handling can leak policy
or fail cleanup.
Source scope:
OAContext.
Related CODEX findings:
CTX-REMOVE-NULL-CLEARS-ALL identifies inconsistent null/default cleanup semantics.
Suggested unit tests:
Verify set/get/remove behavior for null context and explicit context keys across context object, context Hub, and
user-access registration.
Spec target section:
Default/null context semantics.

GRAPH-CONTEXT-004 — Context Cleanup Completeness
Contract statement:
Removing a context must clear all context-owned bindings for that key, including context Hub/object and context
user-access policy, and must be safe and idempotent for default and explicit context keys.
Rationale:
Context cleanup is a lifecycle boundary; stale context bindings can incorrectly influence later graph operations.
Source scope:
OAContext.removeContext, removeContextHub, setContextObject, setContextHub, setContextUserAccess.
Related CODEX findings:
CTX-REMOVE-NULL-CLEARS-ALL notes cleanup can leave default user-access state behind.
Suggested unit tests:
Verify repeated cleanup removes all bindings, does not throw for null/default context, and leaves later lookups
empty.
Spec target section:
Context lifecycle and cleanup semantics.

GRAPH-CONTEXT-005 — Context Registration Lifetime
Contract statement:
A registered context binding has a defined lifetime: either it remains semantically active until explicit removal,
or it is weak/caller-owned and may expire when no caller-held strong reference remains; whichever policy is used
must be deterministic and observable through lookup returning the registered value or null.
Rationale:
Context identity and access policy must not disappear unpredictably from the perspective of callers who rely on
context-dependent graph behavior.
Source scope:
OAContext context Hub and OAUserAccess registration.
Related CODEX findings:
CTX-ACCESS-LIFETIME-DETERMINISTIC identifies weak-reference context lifetime ambiguity.
Suggested unit tests:
Verify context binding retention or expiration behavior according to the documented ownership policy, including GC-
pressure scenarios if weak ownership is intended.
Spec target section:
Context ownership and lifetime semantics.

GRAPH-CONTEXT-006 — ThreadLocal Context Binding
Contract statement:
Methods that use the current runtime context must resolve it from the runtime ThreadLocal context at the time of the
call, and scoped context changes must be restored by the owner of that scope after normal or exceptional completion.
Rationale:
Context-dependent behavior must be isolated between threads, requests, remote calls, sync messages, replication
operations, and nested graph operations.
Source scope:
OAContext no-argument lookup and access methods using OARuntime.thread().getContext().
Related CODEX findings:
None specific beyond package-level context role; root and graph invariants require runtime-context restoration.
Suggested unit tests:
Verify no-argument context methods use the active runtime context and nested scoped context changes restore prior
state after exceptions.
Spec target section:
ThreadLocal/runtime-context restoration.

GRAPH-CONTEXT-007 — Admin And Super-Admin Evaluation
Contract statement:
Admin, super-admin, and edit-processed evaluation must use the configured property paths on the context object,
apply documented server/default-context rules, and return false when required context object or property-path state
is unavailable unless the server/default-context rule explicitly permits access.
Rationale:
These flags are graph-context authority signals and must not silently grant access due to missing context or missing
path data.
Source scope:
OAContext adminPath, superAdminPath, allowEditProcessedPath, isAdmin, isSuperAdmin,
getAllowEditProcessed.
Related CODEX findings:
No direct CODEX finding; methods expose context-user property-path semantics.
Suggested unit tests:
Verify configured property paths, blank paths, missing context objects, default server context, client context,
admin override, and super-admin override.
Spec target section:
User/runtime authority context semantics.

GRAPH-CONTEXT-008 — User Access Default Semantics
Contract statement:
OAUserAccess must begin each enabled/visible decision from its configured default value, then apply explicit class,
property, path, negative-rule, and child-access rules in a deterministic order.
Rationale:
Access rules are runtime policy contracts; deterministic rule precedence is required for predictable visibility and
enabled-state behavior.
Source scope:
OAUserAccess constructors, addEnabled/addNotEnabled/addVisible/addNotVisible, addUserAccess, getEnabled, getVisible.
Related CODEX findings:
UA-PACKAGE-SCOPE-CONSISTENT and UA-CONFIGURE-BEFORE-PUBLISH identify missing clarity around rule evaluation and
shared rule state.
Suggested unit tests:
Verify default-only behavior, positive rules, negative overrides, property-specific rules, path rules, and child
OAUserAccess chaining.
Spec target section:
Access policy evaluation semantics.

GRAPH-CONTEXT-009 — Visible And Enabled Scope Consistency
Contract statement:
Package-scoped access evaluation must apply a coherent and documented inclusion policy for both enabled and visible
checks, including how classes inside and outside the valid package are handled.
Rationale:
Package scoping is a context-level access boundary and should not grant or deny different categories of access
accidentally.
Source scope:
OAUserAccess.setValidPackage, getEnabled, getVisible.
Related CODEX findings:
UA-PACKAGE-SCOPE-CONSISTENT identifies divergent enabled and visible package-scope behavior.
Suggested unit tests:
Verify package-valid behavior for classes inside and outside the package with explicit allow and deny rules for both
enabled and visible checks.
Spec target section:
Package-scoped access semantics.

GRAPH-CONTEXT-010 — Negative Rule Precedence
Contract statement:
When positive and negative rules both match the same class, property, object, Hub, or path context, negative
enabled/visible rules must deterministically override positive enabled/visible rules at the same evaluation layer
before child access policies are applied.
Rationale:
Access-control behavior must be predictable and conservative when explicit deny rules exist.
Source scope:
OAUserAccess class/property/path rule evaluation.
Related CODEX findings:
UA-PACKAGE-SCOPE-CONSISTENT references explicit deny rules and package scoping.
Suggested unit tests:
Verify class, property, and path positive/negative conflicts for enabled and visible checks, including child policy
overrides.
Spec target section:
Access rule precedence semantics.

GRAPH-CONTEXT-011 — Path-Based Access Semantics
Contract statement:
Path-based access rules must evaluate whether the target object or property participates in the configured root
object or root Hub property path using metadata-backed OAPath semantics, including direct root match, Hub active-
object match, forward traversal, and reverse/common-master traversal where supported.
Rationale:
Context access rules are used to express graph-relative authority, not only class-level permissions.
Source scope:
OAUserAccess.UserAccess, getIsInSamePath, path-based addEnabled/addNotEnabled/addVisible/addNotVisible
methods.
Related CODEX findings:
UA-EMPTY-PATH-NO-THROW and UA-REVERSE-PATH-BOUNDS identify missing path-boundary contracts.
Suggested unit tests:
Verify object-root and Hub-root path rules, direct root match, active-object match, forward traversal, reverse/
common-master traversal, and only-end-property behavior.
Spec target section:
Metadata/path-driven access semantics.

GRAPH-CONTEXT-012 — Invalid Or Empty Path Failure Boundary
Contract statement:
Access rules with null, empty, scalar-only, invalid, or non-traversable paths must have deterministic behavior:
either they are rejected visibly during rule registration or they evaluate as a documented no-traversal rule without
throwing during later access checks.
Rationale:
Invalid context policy should not fail unpredictably during unrelated runtime authorization checks.
Source scope:
OAUserAccess path-based rule registration and getIsInSamePath.
Related CODEX findings:
UA-EMPTY-PATH-NO-THROW identifies delayed failures for empty or scalar property paths.
Suggested unit tests:
Verify null, empty, scalar-only, invalid, and non-traversable paths for enabled and visible rule registration/
evaluation.
Spec target section:
Invalid context-policy failure semantics.

GRAPH-CONTEXT-013 — Path Traversal Bounds
Contract statement:
Forward and reverse path evaluation must respect metadata path segment bounds and terminate deterministically
without index errors or infinite traversal, even when paths include multi-hop, calculated, cast, private, or non-one
relationships.
Rationale:
Context access checks must be safe for generated model metadata and complex object graphs.
Source scope:
OAUserAccess.getIsInSamePath.
Related CODEX findings:
UA-REVERSE-PATH-BOUNDS identifies reverse traversal bound assumptions.
Suggested unit tests:
Verify multi-hop, one-to-one, one-to-many, calculated, cast, and reverse-link path scenarios do not throw and return
deterministic access results.
Spec target section:
Path traversal termination and bounds.

GRAPH-CONTEXT-014 — Configure-Before-Publish Access Policy
Contract statement:
An OAUserAccess instance shared through OAContext must either be configured before publication and treated as stable
during concurrent reads, or provide documented synchronization/snapshot semantics for concurrent mutation and
evaluation.
Rationale:
Shared mutable policy state can affect runtime access decisions across threads and requests.
Source scope:
OAUserAccess rule collections, OAContext.setContextUserAccess.
Related CODEX findings:
UA-CONFIGURE-BEFORE-PUBLISH identifies mutable unsynchronized rule collections used during permission checks.
Suggested unit tests:
Verify configured-before-publish access objects evaluate consistently; if concurrent mutation is supported, verify
stable snapshot or synchronized behavior.
Spec target section:
Thread-safety and shared policy semantics.

GRAPH-CONTEXT-015 — Context Isolation
Contract statement:
Context state must be isolated by context key and thread/runtime context so that one user, request, remote call,
sync message, replication operation, or graph operation cannot observe or inherit another context unless explicitly
bound by the caller.
Rationale:
Context leakage can cause incorrect authority, visibility, and graph behavior across runtime participants.
Source scope:
OAContext context maps and no-argument ThreadLocal context methods.
Related CODEX findings:
CTX-REMOVE-NULL-CLEARS-ALL and CTX-ACCESS-LIFETIME-DETERMINISTIC identify cleanup and lifetime risks relevant to
isolation.
Suggested unit tests:
Verify multiple context keys and multiple thread contexts do not share context object, Hub, or user-access state
unintentionally.
Spec target section:
Context isolation and distributed runtime context correctness.

GRAPH-CONTEXT-016 — False-Success Prevention
Contract statement:
Context-dependent operations must not report or imply successful authorization, visibility, enabled-state, admin-
state, or edit-processed-state evaluation when required context, metadata, path, object, Hub, or policy state is
missing, invalid, expired, or unsafe.
Rationale:
Access decisions are security- and behavior-relevant; false success can expose or enable graph state incorrectly.
Source scope:
OAContext and OAUserAccess.
Related CODEX findings:
UA-PACKAGE-SCOPE-CONSISTENT, UA-EMPTY-PATH-NO-THROW, and CTX-ACCESS-LIFETIME-DETERMINISTIC identify ambiguity that
can lead to unsafe success.
Suggested unit tests:
Verify missing context, expired weak references, invalid paths, null objects, null classes, invalid Hubs, and
unsupported policy state fail visibly or return documented conservative defaults.
Spec target section:
False-success prevention.

GRAPH-CONTEXT-017 — Partial-Progress Visibility
Contract statement:
When context registration, cleanup, or access evaluation fails after partial work, the resulting state must remain
observable and consistent enough for callers to distinguish successful registration/evaluation, absent context, and
failed or partially cleaned context.
Rationale:
Context state controls graph authority and must not be left in an invisible partially updated condition.
Source scope:
OAContext set/remove methods, OAUserAccess rule registration and evaluation.
Related CODEX findings:
CTX-REMOVE-NULL-CLEARS-ALL identifies partial cleanup visibility risk.
Suggested unit tests:
Verify exceptions during context operations leave lookup results and access decisions consistent with the actual
completed work.
Spec target section:
Partial-progress visibility.

GRAPH-CONTEXT-018 — Select Integration Boundary
Contract statement:
Context user-access integration with selects must either deterministically apply access constraints to selection
behavior or report that no selection constraint was applied; it must not silently imply datasource-level
authorization filtering when none occurred.
Rationale:
Context policy can affect graph visibility and datasource-backed views, but selection filtering has a distinct
boundary from in-memory access checks.
Source scope:
OAUserAccess.updateSelect, getExtraWhereClause.
Related CODEX findings:
OAUserAccess comments mark select/where-clause support as placeholder behavior.
Suggested unit tests:
Verify updateSelect and extra-where behavior clearly report no-op or applied constraints, and callers can
distinguish the two.
Spec target section:
Datasource/select context boundary.

GRAPH-CONTEXT-019 — Observable Context Semantics
Contract statement:
Graph operations that observe or call into context-dependent behavior must see context state that is internally
consistent for the operation’s lifecycle stage, including user object, active Hub user, access policy, and admin/
super-admin flags.
Rationale:
Context participates in observable/callable graph semantics; observers and callbacks must not see contradictory
authority state.
Source scope:
OAContext and OAUserAccess.
Related CODEX findings:
Existing package-info describes use by traversal, UI binding, callbacks, and security enforcement.
Suggested unit tests:
Verify context-dependent callbacks, traversal, and binding checks observe consistent context state during context
changes and cleanup.
Spec target section:
Observable/callable graph context semantics.

GRAPH-CONTEXT-020 — AI-Readable Runtime Authority Contract
Contract statement:
The graph context package must expose and document enough semantic behavior for generated tests, runtime
verification, OAi/MCP clients, and future graph tooling to infer context identity, access policy, valid/invalid
context state, and failure boundaries without depending on implementation internals.
Rationale:
Runtime context is part of the executable enterprise blueprint contract and must be understandable as a callable
graph authority layer.
Source scope:
package-info.java, OAContext, OAUserAccess.
Related CODEX findings:
Existing package-info lacks a CODEX invariant block; source CODEX comments identify several implicit contracts.
Suggested unit tests:
Verify package-level invariant coverage maps to public context binding, access evaluation, cleanup, failure, and
ThreadLocal behavior.
Spec target section:
AI-readable architecture readiness.

*/


