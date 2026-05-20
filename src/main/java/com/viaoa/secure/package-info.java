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
 * <p>
 */
package com.viaoa.secure;

/* CODEX Invariants

SECURE-SCOPE-001 — Runtime Security Boundary Authority
Contract statement:
com.viaoa.secure defines OA runtime security and secure-value semantics for authorization, visibility, identity
context, permission boundaries, and security-sensitive encoding/crypto helpers.
Rationale:
OA security controls protected Object Graph behavior across UI, query/select, datasource, serialization, remote,
sync, replication, and tooling paths.
Source scope:
Base64; OAEncryption; package-level security contracts; future authorization, permission, context, and visibility
services under com.viaoa.secure.
Related CODEX findings:
package-info notes current package has crypto/encoding helpers and future authorization authority.
Suggested unit tests:
securePackageSeparatesAuthorizationFromCryptoHelpers(), secureRuntimeBoundaryContractsAreExplicit().
Spec target section:
Security Runtime / Package Responsibility Semantics.

SECURE-AUTH-001 — Deterministic Authorization Decisions
Contract statement:
Authorization decisions must be deterministic for the same user, session, object identity, property/link/path,
permission, operation, graph, and runtime context.
Rationale:
UI enablement, datasource filtering, query/path access, serialization exposure, remote calls, and runtime actions
must agree on the same access decision.
Source scope:
future authorization services; integration points with runtime, object, hub, metadata, path, query/select,
datasource, serialization, remote, sync, and replication packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityAuthorizationDecisionIsDeterministicForSameContext(),
securitySameUserObjectPathPermissionReturnsStableDecision().
Spec target section:
Security Runtime / Authorization Semantics.

SECURE-DENY-001 — False-Allow Prevention
Contract statement:
Failed, incomplete, ambiguous, exception-throwing, or unresolvable authorization evaluation must not silently allow
access.
Rationale:
False allow is the highest-risk security correctness failure because it exposes protected runtime graph state or
operations.
Source scope:
future authorization/permission evaluators; failure-channel examples in Base64.decode(...) and OAEncryption helpers.
Related CODEX findings:
Base64 malformed input uses poor failure channel; OAEncryption null/failure ambiguity notes.
Suggested unit tests:
securityAuthorizationFailureDoesNotAllowAccess(), securityPermissionResolverExceptionDeniesOrFailsVisibly(),
securityUnknownPermissionDoesNotDefaultAllow().
Spec target section:
Security Runtime / False-Allow Prevention.

SECURE-ALLOW-001 — False-Deny Prevention
Contract statement:
Valid access must not be incorrectly denied because of stale user context, stale permission cache, stale object
state, wrong session identity, or wrong object/key resolution.
Rationale:
False deny breaks legitimate workflows and can make UI, query, serialization, and remote behavior inconsistent.
Source scope:
future permission evaluator/cache; integration with user/session context, object identity/cache, graph, datasource,
and query filters.
Related CODEX findings:
none observed.
Suggested unit tests:
securityPermissionCacheInvalidationPreventsFalseDeny(), securityRoleChangeAllowsNewlyGrantedAccess(),
securityValidObjectIdentityDoesNotDenyWrongInstance().
Spec target section:
Security Runtime / False-Deny Prevention.

SECURE-USER-001 — User Identity Scope
Contract statement:
Security checks must use the intended OA user identity and must not drift across requests, sessions, threads,
callbacks, queued work, remote calls, sync, replication, or replay contexts.
Rationale:
Access decisions are meaningful only when the user identity is stable and correctly scoped to the runtime operation.
Source scope:
future user/security context service; integration with runtime, remote, sync, replication, queue, process,
ThreadLocal, transaction, and graph services.
Related CODEX findings:
none observed.
Suggested unit tests:
securityUserIdentityDoesNotLeakAcrossThreads(), securityRemoteCallUsesCallerIdentity(),
securityQueuedWorkDoesNotReusePriorUserIdentity().
Spec target section:
Security Runtime / User Identity Semantics.

SECURE-SESSION-001 — Session Isolation
Contract statement:
Session identity and session-scoped security state must be isolated per active session and cleaned up when the
session closes, disconnects, or is invalidated.
Rationale:
Stale session state can authorize the wrong user, deny the right user, or leak access across remote/runtime
participants.
Source scope:
future session/security context service; remote/session lifecycle integration.
Related CODEX findings:
none observed.
Suggested unit tests:
securityClosedSessionCannotAuthorizeAccess(), securitySessionPermissionStateDoesNotLeakToNextSession(),
securityDisconnectClearsSessionContext().
Spec target section:
Security Runtime / Session Semantics.

SECURE-ROLE-001 — Role and Group Resolution
Contract statement:
Role, group, and inherited membership resolution must use the intended security model and must not silently fall
back to misleading defaults.
Rationale:
Role/group ambiguity can produce both false allows and false denies across UI, query, serialization, remote, and
datasource paths.
Source scope:
future role/group resolver; metadata/tooling integration where roles or groups are model-defined.
Related CODEX findings:
none observed.
Suggested unit tests:
securityMissingRoleFailsVisiblyOrDenies(), securityGroupMembershipResolutionIsDeterministic(),
securityInheritedRoleResolutionIsStable().
Spec target section:
Security Runtime / Role and Group Semantics.

SECURE-PERM-001 — Permission State Semantics
Contract statement:
Permission lookup must distinguish unknown, absent, denied, inherited, conditionally granted, and explicitly granted
states according to a deterministic precedence contract.
Rationale:
Security code must not collapse unknown into allow or deny without an explicit rule.
Source scope:
future permission resolver/cache and model; cross-package access checks.
Related CODEX findings:
none observed.
Suggested unit tests:
securityUnknownPermissionDoesNotDefaultAllow(), securityExplicitDenyOverridesInheritedAllow(),
securityExplicitAllowRequiresResolvedPermission().
Spec target section:
Security Runtime / Permission Semantics.

SECURE-OBJECT-001 — Object-Level Access Semantics
Contract statement:
Object-level permissions must apply to the intended OAObject identity, lifecycle state, graph ownership, cache
instance, and datasource identity.
Rationale:
Duplicate, stale, deleted, detached, or wrong-graph object identity can authorize or deny the wrong runtime object.
Source scope:
future object authorization service; integration with OAObject, metadata, cache, graph, datasource, load,
serialization, sync, and replication.
Related CODEX findings:
none observed.
Suggested unit tests:
securityObjectPermissionUsesObjectKeyIdentity(), securityDeletedObjectPermissionUsesDeletedLifecycleState(),
securityWrongGraphObjectDoesNotReusePermissionDecision().
Spec target section:
Security Runtime / Object Access Semantics.

SECURE-PROP-001 — Property-Level Access Semantics
Contract statement:
Property-level permissions must apply to the intended metadata property and must not resolve by display name, stale
alias, wrong case, or wrong reflected member unless explicitly contracted.
Rationale:
Property permissions drive UI visibility/editing, serialization exposure, query/select access, template output, and
datasource filtering.
Source scope:
future property authorization service; integration with metadata, annotation, reflect, object, path, query/select,
serialization, and UI/tooling.
Related CODEX findings:
none observed.
Suggested unit tests:
securityPropertyPermissionUsesMetadataPropertyName(), securityWrongPropertyNameDoesNotSilentlyAllow(),
securityPropertyPermissionConsistentAcrossUiQueryAndSerialization().
Spec target section:
Security Runtime / Property Access Semantics.

SECURE-LINK-001 — Link and Relationship Access Semantics
Contract statement:
Link-level permissions must apply to the intended OALinkInfo relationship, cardinality, reverse link, owner/detail
semantics, and target object visibility.
Rationale:
Relationship permissions affect Hub membership visibility, detail traversal, serialization, query filters, remote
access, and cascade/runtime operations.
Source scope:
future link authorization service; integration with metadata, object, hub, path, query/select, serialization, graph,
sync, and replication.
Related CODEX findings:
none observed.
Suggested unit tests:
securityLinkPermissionUsesMetadataLinkIdentity(), securityDeniedDetailLinkHidesRelatedObjectsByContract(),
securityReverseLinkPermissionMatchesRelationshipSemantics().
Spec target section:
Security Runtime / Link and Relationship Access Semantics.

SECURE-PATH-001 — Path-Based Access Semantics
Contract statement:
Path-based permissions must follow OAPath and metadata traversal semantics and must not silently resolve to the
wrong object, link, or property.
Rationale:
Path permissions affect detail objects, Hub traversal, query filters, serialization, templates, projections, and UI
binding.
Source scope:
future path authorization service; integration with path, metadata, object, hub, query/select, datasource,
serialization, and graph packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityPathPermissionFollowsMetadataLinks(), securityInvalidPermissionPathFailsClosed(),
securityPathPermissionDoesNotResolveWrongTerminalProperty().
Spec target section:
Security Runtime / Path Access Semantics.

SECURE-OP-001 — Operation Authorization Semantics
Contract statement:
Create, read, update, delete, save, validate, select, query, serialize, remote invoke, sync apply, and replication
replay operations must each use the intended authorization boundary before protected effects are exposed.
Rationale:
Object/property visibility is not enough; mutating and distributed operations require operation-level authority.
Source scope:
future operation authorization service; integration with object, hub, datasource, select, query, transaction,
serialization, remote, sync, replication, and graph packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securitySaveRequiresUpdatePermission(), securityDeleteRequiresDeletePermission(),
securityRemoteInvokeRequiresOperationPermission(), securityReplicationApplyUsesTrustedAuthorityBoundary().
Spec target section:
Security Runtime / Operation Authorization Semantics.

SECURE-CACHE-001 — Permission Cache Scope
Contract statement:
Cached permission decisions must be scoped by user, session, permission, operation, object identity, property/link/
path, graph/runtime context, lifecycle state, and relevant role/group version.
Rationale:
Under-scoped caches can leak permissions between users, sessions, objects, graphs, or lifecycle states.
Source scope:
future permission cache/context services.
Related CODEX findings:
none observed.
Suggested unit tests:
securityPermissionCacheScopedByUserAndObject(), securityPermissionCacheScopedBySessionAndGraph(),
securityPermissionCacheInvalidatedOnRoleChange().
Spec target section:
Security Runtime / Permission Cache Semantics.

SECURE-VISIBILITY-001 — Hidden, Missing, Unloaded, and Unauthorized Distinction
Contract statement:
Security behavior must distinguish hidden, inaccessible, unloaded, missing, deleted, and unauthorized state wherever
callers depend on that distinction.
Rationale:
Collapsing these states can leak existence, hide load failures, produce false denies, or allow unauthorized access
to appear as normal absence.
Source scope:
future visibility services; integration with object, hub, load, datasource, path, query/select, serialization, UI,
remote, and graph packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityUnauthorizedObjectDoesNotLookLikeLoadedEmptyByContract(),
securityHiddenPropertySemanticsDifferFromMissingProperty(), securityUnloadedReferenceDoesNotBypassPermissionCheck().
Spec target section:
Security Runtime / Visibility State Semantics.

SECURE-TL-001 — Security Context Restoration
Contract statement:
Any security context set through ThreadLocal or runtime context must be restored with try/finally on success,
denial, failure, exception, cancellation, and remote/queued execution paths.
Rationale:
OA uses reusable worker, queue, remote, sync, replication, process, and datasource threads where context leakage can
cross users, sessions, or requests.
Source scope:
future security context service; integration with OAThreadLocal/runtime/remote/queue/process/sync/replication/
transaction contexts.
Related CODEX findings:
none observed.
Suggested unit tests:
securityThreadLocalRestoredAfterSuccess(), securityThreadLocalRestoredAfterException(),
securityThreadLocalRestoredAfterDeniedAccess().
Spec target section:
Security Runtime / Context Cleanup Semantics.

SECURE-FAIL-001 — Security Failure Visibility
Contract statement:
Security failures must be caller-visible or observable and must not silently appear successful, valid, authorized,
decoded, decrypted, or verified.
Rationale:
Silent security failure can cause unauthorized exposure, invalid credential handling, corrupted secure values, or
misleading operational state.
Source scope:
Base64.decode(...), OAEncryption.getHash(...), getSHAHash(...), getMD5Hash(...), encrypt(...), decrypt(...), future
authorization services.
Related CODEX findings:
hash failures returning null/stdout; Base64 decode failure channel; encryption/decryption ambiguity notes.
Suggested unit tests:
securityFailureIsVisible(), securityDecodeFailureDoesNotLookLikeValidCredential(),
securityHashFailureDistinguishesNullInputFromProviderFailure().
Spec target section:
Security Runtime / Failure Visibility Semantics.

SECURE-PARTIAL-001 — Partial Security Operation Visibility
Contract statement:
If a protected operation fails after partial security evaluation or partial runtime side effects, incompleteness
must be observable and must not be reported as authorized semantic success.
Rationale:
Security checks may guard datasource, serialization, remote, sync, replication, or graph operations where partial
effects must not masquerade as complete authorized behavior.
Source scope:
future authorization services; integration boundaries with datasource, transaction, object, hub, serialization,
remote, sync, replication, and graph packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityPartialAuthorizationFailureDoesNotPublishAuthorizedSuccess(),
securityDeniedUpdateDoesNotPartiallyMutateObject(),
securityFailedSerializationPermissionDoesNotExposePartialPayloadAsComplete().
Spec target section:
Security Runtime / Partial Progress Semantics.

SECURE-CONCURRENT-001 — Concurrent Security Correctness
Contract statement:
Concurrent security checks must not corrupt shared permission state, user/session context, caches, crypto helper
state, or decision ordering.
Rationale:
OA runtime services execute security-sensitive paths concurrently across UI, datasource, remote, sync, queue,
replication, and process threads.
Source scope:
future permission cache/context services; Base64 and OAEncryption static helpers.
Related CODEX findings:
none observed for concurrency; static crypto helpers must remain stateless or safely published.
Suggested unit tests:
securityConcurrentAuthorizationUsesCorrectUserContext(), securityConcurrentPermissionCacheMutationIsSafe(),
securityConcurrentCryptoHelpersAreStateless().
Spec target section:
Security Runtime / Concurrency Semantics.

SECURE-ENCODING-001 — Encoding Is Not Security
Contract statement:
Base64 encoding/decoding must be treated as data representation only, with explicit charset and invalid-input
behavior; it must not be treated as encryption, hashing, or authorization.
Rationale:
Confusing encoding with security can expose credentials or protected values and can corrupt persisted/configured
text across platforms.
Source scope:
Base64.encode(...), Base64.decode(...), String encode/decode overloads.
Related CODEX findings:
Base64 string methods use platform default charset; malformed decode uses poor failure channel; non-Base64/non-ASCII
handling concerns.
Suggested unit tests:
securityBase64IsEncodingOnly(), securityBase64StringUsesDocumentedCharset(),
securityBase64InvalidInputFailsVisibly().
Spec target section:
Security Runtime / Encoding Contract Semantics.

SECURE-CRYPTO-001 — Crypto Purpose Separation
Contract statement:
Encoding, reversible encryption, password hashing, digest/checksum hashing, UUID generation, and integrity
verification must be distinct contracts with explicit purpose, format, and failure behavior.
Rationale:
OA 4.0 must avoid confusing reversible encryption with password hashing, MD5/SHA digests with credential storage, or
UUID generation with authorization.
Source scope:
Base64; OAEncryption.getHash(...), getSHAHash(...), getMD5Hash(...), encrypt(...), decrypt(...), getUUID(...).
Related CODEX findings:
OAEncryption notes distinguish legacy DES, password hashing, digest/checksum, reversible encryption, and versioning.
Suggested unit tests:
securityPasswordHashApiDoesNotUseReversibleEncryption(), securityMD5MarkedChecksumOnly(),
securityUUIDGenerationNotUsedAsAuthorizationToken().
Spec target section:
Security Runtime / Crypto Contract Separation.

SECURE-LEGACY-001 — Legacy Encryption Compatibility Boundary
Contract statement:
Legacy reversible encryption behavior must be explicitly scoped as compatibility behavior and must not be treated as
security-grade protection for new secrets unless the API contract provides authenticated modern encryption, key
derivation, versioning, and failure visibility.
Rationale:
Legacy deterministic DES/default-key behavior can support old data compatibility but is unsafe as a modern
protection boundary.
Source scope:
OAEncryption.encrypt(...), decrypt(...), getCipher(), getSecretKey(...).
Related CODEX findings:
DES/ECB use; deterministic encryption with no IV/nonce; unauthenticated ciphertext; hard-coded default key; password
key derivation truncation; no salt/KDF iterations; no format version marker.
Suggested unit tests:
securityLegacyEncryptionMarkedCompatibilityOnly(), securityDefaultKeyEncryptionNotUsedForPasswords(),
securityEncryptedStringFormatHasDefinedVersionContract().
Spec target section:
Security Runtime / Legacy Encryption Semantics.

SECURE-HASH-001 — Hash and Digest Semantics
Contract statement:
Hash helpers must distinguish credential password hashing from non-password digest/checksum usage, must define
charset/null/failure behavior, and must return fixed-format output where format is part of the contract.
Rationale:
Hash ambiguity can lead to insecure credential storage, comparison mismatches, and false-valid hashes for failed
input.
Source scope:
OAEncryption.getHash(...), getSHAHash(...), getMD5Hash(...).
Related CODEX findings:
unsalted fast SHA-256 for password-style paths; getHash failure returns null/stdout; MD5 exposed without checksum-
only contract; MD5 hex padding bug; null input can return empty-digest path.
Suggested unit tests:
securityShaHashNotPasswordHashByContract(), securityHashFailureDoesNotReturnValidLookingValue(),
securityMD5HexIsFixedLength(), securityMD5NullInputDoesNotHashEmptyUpdateSilently().
Spec target section:
Security Runtime / Hash and Digest Semantics.

SECURE-INTEGRATION-001 — Cross-Layer Access Consistency
Contract statement:
Security decisions must remain consistent across UI/action enablement, datasource/query filtering, object/property
access, Hub/link visibility, serialization exposure, remote access, sync, replication, and tooling.
Rationale:
A user must not be denied in one layer but allowed by another for the same secured operation.
Source scope:
future security integration with runtime, object, hub, metadata, path, select/query, serialization, datasource,
remote, sync, replication, and tooling packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityUiAndQueryPermissionDecisionsMatch(), securitySerializationRespectsPropertyPermission(),
securityRemoteAccessUsesSameObjectPermissionAsLocal().
Spec target section:
Security Runtime / Cross-Package Consistency.

SECURE-BOUNDARY-001 — Transport Success Versus Authorized Runtime Success
Contract statement:
Transport success, remote invocation success, datasource operation success, serialization success, and Object Graph
operation success must remain distinct from authorized semantic success.
Rationale:
A message can be delivered or an operation can run technically while still being unauthorized or only partially
authorized.
Source scope:
future authorization services; integration boundaries with remote, comm, datasource, serialization, sync,
replication, transaction, object, hub, and graph packages.
Related CODEX findings:
none observed.
Suggested unit tests:
securityTransportSuccessDoesNotImplyAuthorization(), securityDatasourceSuccessRequiresAuthorizedOperation(),
securityRemoteInvocationSuccessRequiresAuthorizedSemanticOperation().
Spec target section:
Security Runtime / Runtime Boundary Semantics.

*/


