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

com.viaoa.config Invariants

  ID: CONFIG-SOURCE-001
  Contract statement: Configuration lookup must resolve values from the intended source: file, stream, resource,
  environment, system property, runtime property, or explicit in-memory override.
  Rationale: Runtime mode, datasource, sync, remote, replication, logging, and tooling behavior depend on reading the
  correct deployment source.
  Source locations: OAProperties.load, load(String), load(InputStream), getProperty, getString, exists.
  Related CODEX findings: missing file load silently no-ops; fileName is committed before load/save success.
  Suggested unit tests: testLoadUsesConfiguredFileSource, testMissingRequiredSourceFailsVisibly,
  testFailedLoadDoesNotChangeAssociatedSource.
  Spec target section: Config Runtime / Source Resolution Semantics.

  ID: CONFIG-PRECEDENCE-001
  Contract statement: Source precedence and override behavior must be deterministic and documented wherever multiple
  config sources are combined.
  Rationale: OA applications often combine defaults, bundle properties, runtime properties, system/environment values,
  and deployment files. Ambiguous precedence can select the wrong production setting.
  Source locations: OAProperties.put, setProperty, load; higher-level resource/config aggregators that use
  OAProperties.
  Related CODEX findings: none observed directly in this package beyond overlay/reload ambiguity.
  Suggested unit tests: testExplicitOverrideWinsOverLoadedDefault, testLaterOverlayPrecedenceIsDocumented,
  testCaseInsensitiveOverrideReplacesPriorKey.
  Spec target section: Config Runtime / Precedence Semantics.

  ID: CONFIG-REQUIRED-001
  Contract statement: Required configuration must fail visibly when missing, unreadable, invalid, or unresolvable.
  Rationale: Required config should not degrade into misleading defaults for datasource, runtime mode, remote
  endpoints, sync, or replication.
  Source locations: OAProperties.load(String), getProperty, getString, getInt, getBoolean.
  Related CODEX findings: missing config file returns silently; invalid typed values can fall back to misleading
  sentinels.
  Suggested unit tests: testRequiredMissingFileThrows, testRequiredMissingPropertyThrows,
  testRequiredInvalidTypedPropertyThrows.
  Spec target section: Config Runtime / Required Value Semantics.

  ID: CONFIG-DEFAULT-001
  Contract statement: Defaults must be explicit, consistent, and must not hide missing required production
  configuration. Defaulted getter overloads must return the supplied default for absent or contract-defined unusable
  values.
  Rationale: Defaults are useful for optional settings but dangerous when they silently disable or misconfigure
  runtime services.
  Source locations: OAProperties.getProperty(name, default), getString(name, default), getInt(name, default),
  getBoolean(name, default).
  Related CODEX findings: invalid getBoolean(name, default) returns false; invalid getInt(name, default) returns -1;
  getString(null, default) returns null.
  Suggested unit tests: testDefaultUsedForMissingOptionalValue, testDefaultUsedForInvalidOptionalBooleanByContract,
  testDefaultedGetterDoesNotReturnUnrelatedSentinel.
  Spec target section: Config Runtime / Default Value Semantics.

  ID: CONFIG-NULL-001
  Contract statement: Null and empty-string handling must be consistent and target-type aware. Null keys, missing
  values, empty values, and invalid values must have distinct documented behavior where runtime decisions depend on
  them.
  Rationale: Empty strings can mean intentionally blank, missing, or invalid depending on the setting. OA runtime
  setup must not conflate these states silently.
  Source locations: OAProperties.getProperty, getString, getInt, getBoolean, put, setProperty, exists.
  Related CODEX findings: defaulted getString(null, default) ignores the default.
  Suggested unit tests: testNullKeyBehaviorConsistentAcrossGetters, testEmptyStringBooleanBehaviorDocumented,
  testExistsDistinguishesMissingFromEmptyValueByContract.
  Spec target section: Config Runtime / Null and Empty Semantics.

  ID: CONFIG-CONVERT-001
  Contract statement: Config type conversion must preserve semantic value and fail visibly or apply the defined
  default when conversion is invalid.
  Rationale: Wrong boolean, integer, path, date, or numeric conversion can select incorrect runtime mode, timeout,
  port, datasource, or replication settings.
  Source locations: OAProperties.getInt, getBoolean, typed setProperty overloads; OAConv, OAConverter.
  Related CODEX findings: invalid integer and boolean values silently return -1/false even in defaulted overloads.
  Suggested unit tests: testInvalidIntConfigDoesNotSilentlyBecomeMinusOneWhenDefaultProvided,
  testInvalidBooleanConfigDoesNotSilentlyBecomeFalseWhenDefaultProvided, testTypedSetPropertyRoundTripsValue.
  Spec target section: Config Runtime / Type Conversion Semantics.

  ID: CONFIG-LOAD-001
  Contract statement: A successful load must mean the intended config source was found, read, parsed, and committed
  according to the load contract.
  Rationale: Runtime setup depends on config load completion before services are initialized. A no-op load can leave
  stale or empty config while appearing successful.
  Source locations: OAProperties.load, load(String), load(InputStream).
  Related CODEX findings: missing file returns without failure; reload overlays stale values.
  Suggested unit tests: testLoadExistingFileCommitsProperties,
  testLoadMissingFileDoesNotAppearSuccessfulForStrictLoad, testLoadInputStreamFailureIsVisible.
  Spec target section: Config Runtime / Load Semantics.

  ID: CONFIG-FAIL-001
  Contract statement: Failed config load/save/conversion must be caller-visible or explicitly recorded. It must not
  silently publish a partially loaded, stale, or misleading configuration.
  Rationale: Silent config failure can lead to wrong datasource connections, disabled sync, incorrect remote
  endpoints, or bad logging/runtime mode.
  Source locations: OAProperties.load(String), load(InputStream), save, getInt, getBoolean.
  Related CODEX findings: missing file no-op; failed load/save changes fileName; invalid typed conversions return
  misleading values.
  Suggested unit tests: testLoadFailurePreservesPreviousCommittedState,
  testSaveFailurePreservesPreviousCommittedFileName, testInvalidConfigConversionIsVisibleForRequiredValue.
  Spec target section: Config Runtime / Failure Visibility.

  ID: CONFIG-RELOAD-001
  Contract statement: Reload/reset must produce a coherent configuration snapshot and must not leave stale keys or
  mixed old/new state unless overlay behavior is explicitly requested.
  Rationale: OA runtime/tooling may reload config after deployment changes. Stale values can keep old service settings
  active.
  Source locations: OAProperties.load(String), load(InputStream), clear, put, remove, keys.
  Related CODEX findings: reload overlays new properties without clearing old keys; case-insensitive null removal
  leaves stale key in alKeys.
  Suggested unit tests: testReloadClearsKeysMissingFromNewFile, testOverlayLoadPreservesOldKeysOnlyWhenExplicit,
  testCaseInsensitiveRemoveClearsEnumerationState.
  Spec target section: Config Runtime / Reload and Reset Semantics.

  ID: CONFIG-STATE-001
  Contract statement: Config object state such as associated file name, ordered keys, and property values must be
  committed only after the corresponding operation succeeds, or restored on failure.
  Rationale: Partial setup can corrupt future retry/load/save behavior by changing the source identity or key index
  without valid committed content.
  Source locations: OAProperties.fileName, setFileName, load(String), save(String,String), alKeys, put, remove.
  Related CODEX findings: fileName committed before load/save success; stale alKeys after case-insensitive null
  removal.
  Suggested unit tests: testFailedLoadDoesNotPublishNewFileName, testFailedSaveDoesNotPublishNewFileName,
  testKeyIndexMatchesCommittedProperties.
  Spec target section: Config Runtime / State Commit Semantics.

  ID: CONFIG-RESOURCE-001
  Contract statement: File/resource streams used for config loading and saving must be closed on success and failure
  unless ownership is explicitly transferred.
  Rationale: Leaked streams can retain file handles, block config updates, or destabilize reload/save loops in long-
  running applications.
  Source locations: OAProperties.load(String), load(InputStream), save(String,String).
  Related CODEX findings: input/output streams are closed only on success.
  Suggested unit tests: testLoadClosesStreamOnParseFailure, testSaveClosesStreamOnWriteFailure,
  testLoadStringClosesFileInputStream.
  Spec target section: Config Runtime / Resource Cleanup.

  ID: CONFIG-CONCURRENT-001
  Contract statement: Shared configuration state must be immutable, synchronized, or safely published. Concurrent
  reads, reloads, saves, and mutations must not expose stale indexes, partial snapshots, or internal collection races.
  Rationale: Runtime services may read config while deployment/tooling updates or reloads it. Nondeterministic config
  reads can change runtime behavior across threads.
  Source locations: OAProperties.getProperty, keys, put, remove, clear, alKeys, inherited Properties synchronization.
  Related CODEX findings: keys() returns an enumeration over live alKeys that can race with mutation after the
  synchronized method returns.
  Suggested unit tests: testConcurrentLookupDuringMutationDoesNotThrow, testConfigSnapshotStableDuringReload,
  testKeyEnumerationUsesSnapshot.
  Spec target section: Config Runtime / Concurrency and Publication Semantics.

  ID: CONFIG-CASE-001
  Contract statement: Case-insensitive key behavior must preserve one logical property per case-insensitive name, with
  property storage and key enumeration kept consistent.
  Rationale: OA config treats property names case-insensitively. Duplicate case variants or stale key indexes can make
  override/save/reporting behavior wrong.
  Source locations: OAProperties.getProperty, put, remove, keys, alKeys.
  Related CODEX findings: case-insensitive null removal removes the property but leaves the old key in alKeys.
  Suggested unit tests: testCaseInsensitivePutReplacesExistingKey, testCaseInsensitiveRemoveRemovesStoredValueAndKey,
  testKeysDoNotContainStaleCaseVariant.
  Spec target section: Config Runtime / Case-Insensitive Key Semantics.

  ID: CONFIG-INTEGRATION-001
  Contract statement: Config behavior must remain compatible with runtime, datasource, remote, sync, replication,
  classloader, converter, and logging contracts.
  Rationale: Configuration is the source of truth for many runtime service decisions. Bad config semantics propagate
  into production service ownership, routing, persistence, and messaging behavior.
  Source locations: OAProperties; consumers in runtime/resource/bootstrap code; converter integration through OAConv
  and OAConverter.
  Related CODEX findings: typed conversion/default/load/reload bugs can affect runtime mode, datasource, sync, remote,
  and replication setup.
  Suggested unit tests: testRuntimeModeConfigUsesRequiredStrictValue, testDatasourceConfigReloadDoesNotUseStaleUrl,
  testSyncConfigInvalidBooleanFailsOrDefaultsByContract.
  Spec target section: Config Runtime / Cross-Package Integration.

  Suggested Package-Level Spec Summary

  - com.viaoa.config provides configuration property loading, saving, lookup, type conversion, defaults, and case-
    insensitive key handling for OA runtime and tooling.
  - It must resolve values from the intended source and make source/precedence behavior deterministic.
  - Required configuration must fail visibly when missing, unreadable, invalid, or unresolvable.
  - Optional defaults must be explicit and consistently applied by defaulted getter overloads.
  - Null, empty, missing, invalid, and defaulted values must have distinct documented behavior where runtime decisions
    depend on them.
  - Typed conversion must preserve semantic value and must not silently convert invalid production config into
    misleading sentinels.
  - Load/save success must mean the source was actually read/written and committed.
  - Reload/reset must avoid stale keys and mixed old/new state unless explicit overlay behavior is requested.
  - Config streams must be closed on success and failure.
  - Shared config instances must be synchronized or safely published for concurrent runtime reads and updates.
  - Case-insensitive keys must remain consistent between stored values and key enumeration.
  - Config contracts must support runtime, datasource, remote, sync, replication, classloader, converter, and logging
    behavior without hidden false-success.

  Likely unit-test categories:

  - source resolution and missing file behavior
  - required vs optional/defaulted property behavior
  - type conversion success/failure behavior
  - null/empty/missing value semantics
  - full reload vs overlay behavior
  - save/load partial-failure behavior
  - file/resource stream cleanup
  - case-insensitive key replacement/removal/enumeration
  - concurrent read/update/reload behavior
  - runtime/datasource/sync/remote config integration tests
  - failure tests for invalid production config values


*/


