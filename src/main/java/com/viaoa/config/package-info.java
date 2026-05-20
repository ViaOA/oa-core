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
package com.viaoa.config;

/* CODEX Invariants

CONFIG-SOURCE-001 — Configuration Source Authority
Contract statement:
Configuration lookup and load operations must resolve values from the intended source: file, stream, resource,
system property, environment source, runtime property, or explicit in-memory override according to the calling
contract.
Rationale:
Runtime mode, datasource, remote, sync, replication, logging, tooling, and graph startup behavior depend on
selecting the correct deployment configuration source.
Source scope:
OAProperties constructors, OAProperties.setFileName(...), getFileName(), load(), load(String), load(InputStream),
getProperty(...), getString(...), exists(...).
Related CODEX findings:
OAProperties load(String) missing-file no-op; fileName committed before load/save success.
Suggested unit tests:
configLoadUsesConfiguredFileSource(), configMissingRequiredSourceFailsVisibly(),
configFailedLoadDoesNotChangeAssociatedSource().
Spec target section:
Config Runtime / Source Resolution Semantics.

CONFIG-PRECEDENCE-001 — Deterministic Override Precedence
Contract statement:
When multiple configuration sources or writes are combined, precedence and override behavior must be deterministic
and observable.
Rationale:
Ambiguous precedence can select the wrong production setting for runtime role, datasource, remote endpoint, sync/
replication, or logging behavior.
Source scope:
OAProperties.load(...), put(...), setProperty(...), remove(...), clear(...), getProperty(...), keys().
Related CODEX findings:
overlay/reload ambiguity from load behavior; case-insensitive key replacement/removal drift.
Suggested unit tests:
configExplicitOverrideWinsOverLoadedValue(), configLaterOverlayPrecedenceIsDocumented(),
configCaseInsensitiveOverrideReplacesPriorKey().
Spec target section:
Config Runtime / Precedence and Override Semantics.

CONFIG-REQUIRED-001 — Required Configuration Visibility
Contract statement:
Required configuration must fail visibly when missing, unreadable, invalid, incomplete, or unresolvable; it must not
silently degrade into misleading defaults or sentinel values.
Rationale:
Required runtime settings must safely support datasource, runtime mode, remote, sync, replication, logging, and
startup behavior.
Source scope:
OAProperties.load(String), load(InputStream), getProperty(...), getString(...), getInt(...), getBoolean(...),
exists(...).
Related CODEX findings:
missing config file returns silently; invalid typed values can fall back to misleading values.
Suggested unit tests:
configRequiredMissingFileThrowsOrRecordsFailure(), configRequiredMissingPropertyFailsVisibly(),
configRequiredInvalidTypedPropertyFailsVisibly().
Spec target section:
Config Runtime / Required Value Semantics.

CONFIG-DEFAULT-001 — Explicit Optional Defaults
Contract statement:
Optional defaults must be explicit and consistently applied by defaulted getter overloads for absent or contract-
defined unusable values.
Rationale:
Defaults are valid for optional settings, but they must not hide missing required production configuration or return
unrelated sentinels.
Source scope:
OAProperties.getProperty(name, defaultValue), getString(name, strDefault), getInt(name, iDefault), getBoolean(name,
bDefault).
Related CODEX findings:
invalid getBoolean(name, default) returns false; invalid getInt(name, default) returns -1; getString(null, default)
returns null.
Suggested unit tests:
configDefaultUsedForMissingOptionalValue(), configDefaultedIntDoesNotReturnUnrelatedSentinel(),
configDefaultedBooleanDoesNotReturnUnrelatedSentinel(), configDefaultedStringHandlesNullNameByContract().
Spec target section:
Config Runtime / Default Value Semantics.

CONFIG-NULL-001 — Null, Empty, Missing, and Invalid Distinction
Contract statement:
Null keys, missing keys, empty string values, blank values, invalid values, and explicit defaults must have distinct
documented behavior where runtime decisions depend on them.
Rationale:
OA configuration often controls startup and distributed behavior; conflating blank, missing, invalid, and defaulted
settings can silently select the wrong runtime mode.
Source scope:
OAProperties.getProperty(...), getString(...), getInt(...), getBoolean(...), setProperty(...), put(...),
remove(...), exists(...).
Related CODEX findings:
defaulted getString(null, default) ignores the default.
Suggested unit tests:
configNullKeyBehaviorConsistentAcrossGetters(), configEmptyStringBooleanBehaviorDocumented(),
configExistsDistinguishesMissingFromEmptyValueByContract().
Spec target section:
Config Runtime / Null and Empty Semantics.

CONFIG-CONVERT-001 — Typed Conversion Correctness
Contract statement:
Configuration type conversion must preserve the semantic value of the configured setting and must fail visibly or
apply the defined default when conversion is invalid.
Rationale:
Wrong boolean, integer, numeric, path, timeout, port, or runtime flag conversion can misconfigure datasource,
remote, sync, replication, and runtime startup behavior.
Source scope:
OAProperties.getInt(...), getBoolean(...), getString(...), typed setProperty(...), typed put(...), OAConv/
OAConverter integration.
Related CODEX findings:
invalid integer and boolean values silently return -1/false even when defaulted overloads are used.
Suggested unit tests:
configInvalidIntUsesDefaultOrFailsByContract(), configInvalidBooleanUsesDefaultOrFailsByContract(),
configTypedSetPropertyRoundTripsValue().
Spec target section:
Config Runtime / Type Conversion Semantics.

CONFIG-LOAD-001 — Load Commit Semantics
Contract statement:
A successful load must mean the intended source was found, read, parsed, and committed according to the load
contract.
Rationale:
Runtime startup and subsystem initialization depend on configuration being complete before services are created or
exposed.
Source scope:
OAProperties.load(), load(String), load(InputStream), constructors that load from source.
Related CODEX findings:
missing file returns without failure; reload overlays stale values.
Suggested unit tests:
configLoadExistingFileCommitsProperties(), configLoadMissingFileDoesNotAppearSuccessfulForStrictLoad(),
configLoadInputStreamFailureIsVisible().
Spec target section:
Config Runtime / Load Semantics.

CONFIG-SAVE-001 — Save Commit Semantics
Contract statement:
A successful save must mean the intended output destination was written and committed according to the save
contract; failed saves must not publish misleading associated file state.
Rationale:
Runtime tooling and deployment flows must not believe a configuration file was written when output failed or only
partially completed.
Source scope:
OAProperties.save(), save(String), save(String, String), fileName state.
Related CODEX findings:
fileName can be committed before save success; output streams closed only on success.
Suggested unit tests:
configSaveExistingDestinationCommitsOutput(), configFailedSaveDoesNotPublishNewFileName(),
configSaveFailureIsVisible().
Spec target section:
Config Runtime / Save Semantics.

CONFIG-FAIL-001 — Failure and False-Success Prevention
Contract statement:
Failed load, save, conversion, lookup, or reload operations must be caller-visible or explicitly recorded and must
not silently publish partial, stale, or misleading configuration state.
Rationale:
Silent configuration failure can cause wrong datasource connections, incorrect runtime role, disabled sync,
incorrect remote endpoints, bad replication behavior, or misleading logging.
Source scope:
OAProperties.load(...), save(...), getInt(...), getBoolean(...), getString(...), getProperty(...), exists(...).
Related CODEX findings:
missing file no-op; failed load/save changes fileName; invalid typed conversions return misleading values.
Suggested unit tests:
configLoadFailurePreservesPreviousCommittedState(), configSaveFailurePreservesPreviousCommittedState(),
configInvalidConversionIsVisibleForRequiredValue().
Spec target section:
Config Runtime / Failure Visibility Semantics.

CONFIG-RELOAD-001 — Reload and Reset Snapshot Coherence
Contract statement:
Reload and reset operations must produce a coherent configuration snapshot and must not leave mixed old/new state
unless overlay behavior is explicitly requested.
Rationale:
Runtime and tooling reloads must not keep stale datasource, sync, remote, replication, or logging values after
deployment configuration changes.
Source scope:
OAProperties.load(String), load(InputStream), clear(), put(...), remove(...), keys().
Related CODEX findings:
reload overlays new properties without clearing old keys; case-insensitive null removal leaves stale key in ordered
key list.
Suggested unit tests:
configReloadClearsKeysMissingFromNewSource(), configOverlayLoadPreservesOldKeysOnlyWhenExplicit(),
configClearProducesEmptyConsistentSnapshot().
Spec target section:
Config Runtime / Reload and Reset Semantics.

CONFIG-STATE-001 — Internal State Commit Ordering
Contract statement:
Associated file name, ordered keys, stored values, and cached/indexed configuration state must be committed only
after the corresponding operation succeeds, or restored on failure.
Rationale:
Partial config setup can corrupt retry, reload, save, enumeration, and runtime visibility by changing source
identity or key state without valid content.
Source scope:
OAProperties.fileName, setFileName(...), load(String), save(String,...), put(...), remove(...), clear(), alKeys,
keys().
Related CODEX findings:
fileName committed before load/save success; stale alKeys after case-insensitive null removal.
Suggested unit tests:
configFailedLoadDoesNotPublishNewFileName(), configFailedSaveDoesNotPublishNewFileName(),
configKeyIndexMatchesCommittedProperties().
Spec target section:
Config Runtime / State Commit Semantics.

CONFIG-CASE-001 — Case-Insensitive Key Consistency
Contract statement:
Case-insensitive key behavior must preserve one logical property per case-insensitive name, with stored values,
replacement behavior, removal behavior, and key enumeration kept consistent.
Rationale:
OA config treats property names case-insensitively; duplicate case variants or stale enumeration keys can make
override, save, reporting, and lookup behavior wrong.
Source scope:
OAProperties.getProperty(...), get(...), put(...), setProperty(...), remove(...), keys(), alKeys.
Related CODEX findings:
case-insensitive null removal removes the property but leaves the old key in alKeys.
Suggested unit tests:
configCaseInsensitivePutReplacesExistingKey(), configCaseInsensitiveRemoveRemovesStoredValueAndKey(),
configKeysDoNotContainStaleCaseVariant().
Spec target section:
Config Runtime / Case-Insensitive Key Semantics.

CONFIG-KEYS-001 — Key Enumeration Snapshot Semantics
Contract statement:
Key enumeration must reflect committed configuration state and must not expose stale keys, duplicate logical keys,
or unsafe live collection races.
Rationale:
Runtime tooling, logging, diagnostics, save operations, and integration tests may enumerate keys to determine
effective configuration.
Source scope:
OAProperties.keys(), put(...), remove(...), clear(), alKeys.
Related CODEX findings:
keys() returns an enumeration over live alKeys that can race with mutation after synchronized method returns.
Suggested unit tests:
configKeyEnumerationUsesCommittedSnapshot(), configConcurrentMutationDoesNotCorruptKeyEnumeration(),
configKeysMatchStoredProperties().
Spec target section:
Config Runtime / Key Enumeration Semantics.

CONFIG-RESOURCE-001 — Config Stream Ownership and Cleanup
Contract statement:
File, resource, input, and output streams opened or owned by config operations must be closed on success and failure
unless ownership is explicitly transferred.
Rationale:
Leaked streams can retain file handles, block config updates, and destabilize reload/save loops in long-running
runtimes.
Source scope:
OAProperties.load(String), load(InputStream), save(String), save(String, String).
Related CODEX findings:
input/output streams are closed only on success.
Suggested unit tests:
configLoadClosesStreamOnParseFailure(), configSaveClosesStreamOnWriteFailure(),
configLoadStringClosesFileInputStream().
Spec target section:
Config Runtime / Resource Cleanup Semantics.

CONFIG-CONCURRENT-001 — Shared Config Publication
Contract statement:
Shared configuration state must be immutable, synchronized, or safely published; concurrent reads, reloads, saves,
and mutations must not expose partial snapshots, stale indexes, or internal collection races.
Rationale:
Runtime services may read configuration while tooling or deployment code reloads it, and inconsistent reads can
change behavior across threads.
Source scope:
OAProperties.getProperty(...), get(...), keys(), put(...), remove(...), clear(), load(...), save(...), alKeys,
inherited Properties synchronization.
Related CODEX findings:
live key enumeration race; dual state between Properties storage and alKeys.
Suggested unit tests:
configConcurrentLookupDuringMutationDoesNotThrow(), configSnapshotStableDuringReload(),
configConcurrentKeysEnumerationDoesNotExposePartialState().
Spec target section:
Config Runtime / Concurrency and Safe Publication Semantics.

CONFIG-LIFECYCLE-001 — Runtime Configuration Lifecycle
Contract statement:
Configuration initialization, load, override, reload, save, and shutdown/setup use must follow deterministic
lifecycle ordering before dependent runtime services consume settings.
Rationale:
Datasource, transaction, sync, replication, remote, serialization, metadata, graph, object, and hub services must
not start from incomplete or stale configuration.
Source scope:
OAProperties lifecycle APIs; package integration boundaries with runtime/bootstrap code and subsystem configuration
consumers.
Related CODEX findings:
partial load/save state and reload stale-value findings illustrate lifecycle ordering risk.
Suggested unit tests:
configMustBeLoadedBeforeRuntimeServiceInitialization(), configReloadPublishesCoherentSnapshotBeforeConsumersRead(),
configFailedStartupConfigPreventsDependentServiceStart().
Spec target section:
Config Runtime / Lifecycle and Startup Semantics.

CONFIG-VALIDITY-001 — Configuration Presence Versus Semantic Validity
Contract statement:
The presence of a configuration key or text value must not by itself imply runtime-semantic validity; values must be
valid for the consuming setting’s type, range, source, and subsystem contract.
Rationale:
A present but invalid port, URL, boolean, runtime role, datasource name, or sync flag can be more dangerous than a
missing value.
Source scope:
OAProperties.exists(...), getProperty(...), getString(...), getInt(...), getBoolean(...); cross-package consumers.
Related CODEX findings:
invalid typed values return misleading sentinels in defaulted getters.
Suggested unit tests:
configPresentInvalidIntIsNotSemanticallyValid(), configPresentInvalidBooleanIsNotSemanticallyValid(),
configExistsDoesNotBypassRequiredTypedValidation().
Spec target section:
Config Runtime / Semantic Validity Semantics.

CONFIG-BOUNDARY-001 — Cross-Package Configuration Boundary
Contract statement:
com.viaoa.config supplies deterministic configuration values and state visibility; consuming packages remain
responsible for validating subsystem-specific semantics and reporting operation success.
Rationale:
Configuration can authorize runtime choices, but successful config lookup does not imply successful datasource
connection, transaction setup, remote connection, sync startup, replication replay, serialization setup, or graph
initialization.
Source scope:
OAProperties; integration boundaries with runtime, datasource, transaction, remote, sync, replication,
serialization, metadata, object, hub, graph, classloader, converter, and logging packages.
Related CODEX findings:
typed conversion/default/load/reload bugs can affect runtime mode, datasource, sync, remote, and replication setup.
Suggested unit tests:
configRuntimeModeUsesStrictRequiredValue(), configDatasourceReloadDoesNotUseStaleUrl(),
configSyncInvalidBooleanFailsOrDefaultsByContract(), configLookupSuccessDoesNotImplySubsystemStartupSuccess().
Spec target section:
Config Runtime / Cross-Package Boundary Semantics.

CONFIG-OBSERVE-001 — Configuration Observability
Contract statement:
Configuration source identity, committed values, defaults used, reload state, and failure state must be observable
enough for runtime diagnostics and production support where configuration controls runtime behavior.
Rationale:
Hidden configuration state makes it difficult to diagnose wrong runtime role, datasource, remote, sync, replication,
or logging behavior.
Source scope:
OAProperties.getFileName(), keys(), getProperty(...), getString(...), getInt(...), getBoolean(...), load(...),
save(...).
Related CODEX findings:
missing source no-op, failed load/save source-state mutation, and invalid conversion sentinel findings reduce
observability.
Suggested unit tests:
configDiagnosticsExposeCommittedFileName(), configDiagnosticsExposeEffectiveKeys(),
configFailureStateIsObservableForStartupConfig().
Spec target section:
Config Runtime / Observability Semantics.

*/

