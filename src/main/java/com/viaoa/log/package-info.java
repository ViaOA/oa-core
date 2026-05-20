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
package com.viaoa.log;

/* CODEX Invariants

ID: LOG-VISIBILITY-001
  Contract statement: Critical OA runtime failures must remain observable through logging or an explicit alternate
  failure channel.
  Rationale: Logs are part of production diagnosis for sync, replication, remote, queue, datasource, and runtime
  failures. Silent diagnostic loss turns real failures into untraceable production state drift.
  Source locations: OALogger, OALogUtil.disable, OALogUtil.consoleOnly, OALogUtil.getAllThreadDump,
  OALogUtil.getStackTrace.
  Related CODEX findings: stale levelRoot can leave logging disabled after reconfiguration.
  Suggested unit tests: testConsoleOnlyAfterDisableReenablesLogging,
  testCriticalLogVisibleAfterRuntimeReconfiguration.
  Spec target section: Logging / Critical Failure Visibility.

  ID: LOG-DELIVERY-001
  Contract statement: A log record accepted by the configured logger must be delivered to all intended active handlers
  unless a caller-visible or observable failure occurs.
  Rationale: OA uses logs to reconstruct runtime behavior and diagnose failures; accepted records must not disappear
  silently.
  Source locations: OALogger.setupConsoleLogger, OALogger.createIndentConsoleLogger, OALogUtil.consoleOnly,
  OALogUtil.consolePerformance.
  Related CODEX findings: none beyond configuration-state findings.
  Suggested unit tests: testConfiguredLoggerDeliversRecordToConsoleHandler,
  testReconfigurationDoesNotDropAcceptedRecord.
  Spec target section: Logging / Record Delivery Semantics.

  ID: LOG-ORDER-001
  Contract statement: Log record ordering must remain deterministic per logger/handler stream where OA relies on logs
  to diagnose lifecycle, sync, replication, remote, queue, or process behavior.
  Rationale: Out-of-order diagnostics can obscure causality in distributed/runtime failure analysis.
  Source locations: OALogger, OALogUtil, Java logging handler usage.
  Related CODEX findings: duplicate handlers can make ordering appear misleading by repeating records.
  Suggested unit tests: testSingleLoggerPreservesRecordOrder,
  testRepeatedSetupDoesNotCreateInterleavedDuplicateRecords.
  Spec target section: Logging / Ordering Semantics.

  ID: LOG-LEVEL-001
  Contract statement: Level filtering must be explicit and must not accidentally suppress required operational
  diagnostics.
  Rationale: Incorrect level setup can hide warnings/errors needed for production support.
  Source locations: OALogger.BUG, ERROR, SERVERERROR, CLIENTERROR, OALogUtil.disable, OALogUtil.consoleOnly,
  OALogger.setupConsoleLogger.
  Related CODEX findings: stale cached root level can suppress logs after disable/re-enable.
  Suggested unit tests: testCustomErrorLevelsPassWarningThreshold, testConsoleOnlyUsesRequestedLevel,
  testDisableDoesNotPoisonLaterSameLevelSetup.
  Spec target section: Logging / Level Filtering Semantics.

  ID: LOG-ROUTE-001
  Contract statement: Logger routing must send records to the intended logger name/category and handler destination
  without unintended duplication or suppression.
  Rationale: OA diagnostics depend on package/category targeting, especially for focused sync, queue, performance, and
  remote logging.
  Source locations: OALogger.getLogger(Class), OALogger.setupConsoleLogger, OALogger.createIndentConsoleLogger,
  OALogUtil.consoleOnly(Level,String), OALogUtil.consolePerformance.
  Related CODEX findings: repeated setup adds duplicate handlers; partial setup can remove old routing before new
  routing is valid.
  Suggested unit tests: testConsoleOnlyRoutesNamedLoggerOnly, testRepeatedSetupDoesNotDuplicateHandlers,
  testInvalidNamedRouteDoesNotDestroyExistingRouting.
  Spec target section: Logging / Routing Semantics.

  ID: LOG-CONFIG-001
  Contract statement: Logger configuration must be committed atomically enough that failed setup does not leave stale,
  mixed, or disabled logger state.
  Rationale: Logging setup is JVM-global; partial mutation can break observability for unrelated OA services.
  Source locations: OALogUtil.levelRoot, OALogUtil.disable, OALogUtil.consoleOnly, OALogger.setupConsoleLogger,
  OALogger.createIndentConsoleLogger.
  Related CODEX findings: consoleOnly mutates existing handlers before validating replacement inputs; levelRoot can
  become stale.
  Suggested unit tests: testConsoleOnlyValidatesBeforeMutation, testFailedConsoleOnlyKeepsPreviousHandlers,
  testLevelRootTracksActualLoggerState.
  Spec target section: Logging / Configuration State Semantics.

  ID: LOG-FLUSH-001
  Contract statement: Log writes must not be treated as durably observable before the relevant handler accepts and
  flushes data according to its contract.
  Rationale: Failure diagnostics and audit-style logs are only useful if buffered records reach their destination at
  the expected boundary.
  Source locations: handler lifecycle in OALogUtil.consoleOnly, OALogger.setupConsoleLogger,
  OALogger.createIndentConsoleLogger; future file/stream handlers.
  Related CODEX findings: removed handlers are not flushed/closed.
  Suggested unit tests: testRemovedHandlerIsFlushedBeforeClose, testShutdownFlushesPendingRecords.
  Spec target section: Logging / Flush Commit Semantics.

  ID: LOG-CLOSE-001
  Contract statement: Log handlers, files, and streams opened or owned by OA must be closed on reconfiguration,
  rollover, failure, and shutdown unless ownership is explicitly transferred.
  Rationale: Unclosed handlers leak file descriptors and can hold buffered records.
  Source locations: OALogUtil.consoleOnly handler removal loops; OALogger setup helpers.
  Related CODEX findings: consoleOnly removes handlers without closing them.
  Suggested unit tests: testConsoleOnlyClosesRemovedRootHandlers, testConsoleOnlyClosesRemovedNamedHandlers.
  Spec target section: Logging / Resource Close Semantics.

  ID: LOG-ROTATE-001
  Contract statement: Log rotation or rollover must preserve record boundaries and must not silently lose, duplicate,
  reorder, or corrupt records.
  Rationale: Production diagnosis often spans rollover boundaries, especially under long-running sync/replication/load
  processes.
  Source locations: no explicit rotation implementation currently in com.viaoa.log; applicable to future OA-owned file
  handlers.
  Related CODEX findings: none.
  Suggested unit tests: testRolloverPreservesBoundaryRecord, testRolloverClosesOldHandlerBeforePublishingToNewHandler,
  testRolloverFailureKeepsLoggingObservable.
  Spec target section: Logging / Rotation Semantics.

  ID: LOG-FAIL-001
  Contract statement: Logging failure must be visible, recoverable, or explicitly degraded; it must not silently look
  like successful observability where OA depends on logs.
  Rationale: Diagnostic infrastructure failure is itself production-relevant when logs support failure analysis,
  replay, or operational support.
  Source locations: OALogUtil.consoleOnly, OALogger.setupConsoleLogger, OALogger.createIndentConsoleLogger; future
  file/stream logging.
  Related CODEX findings: partial setup can disable existing logging before a replacement setup failure.
  Suggested unit tests: testFailedLoggerSetupLeavesObservableFailure,
  testHandlerFailureDoesNotSilentlyDropCriticalDiagnostic.
  Spec target section: Logging / Failure Visibility Semantics.

  ID: LOG-RESOURCE-001
  Contract statement: Logger resources must not be leaked or retained after replacement, disablement, rollover, or
  shutdown.
  Rationale: Long-running OA runtimes cannot accumulate stale handlers, open files, or retained logger state.
  Source locations: OALogUtil.consoleOnly, OALogUtil.consolePerformance, OALogger.setupConsoleLogger,
  OALogger.createIndentConsoleLogger.
  Related CODEX findings: repeated setup creates duplicate retained handlers; removed handlers are not closed.
  Suggested unit tests: testRepeatedConsolePerformanceDoesNotLeakHandlers,
  testRepeatedConsoleSetupReplacesOrReusesOwnedHandlers.
  Spec target section: Logging / Resource Lifecycle Semantics.

  ID: LOG-CONCURRENT-001
  Contract statement: Concurrent logging and logger reconfiguration must not corrupt handler lists, filtering state,
  routing state, or record delivery semantics.
  Rationale: OA services log from many threads while tests/runtime may reconfigure logging; shared logger state must
  be safely published.
  Source locations: static OALogUtil.levelRoot, OALogUtil.consoleOnly, OALogUtil.disable, OALogger setup helpers.
  Related CODEX findings: unsynchronized levelRoot and global handler mutation create stale/mixed-state risks.
  Suggested unit tests: testConcurrentConsoleOnlyCallsDoNotDuplicateHandlers,
  testConcurrentDisableAndConsoleOnlyEndsInConsistentState,
  testConcurrentLoggingDuringReconfigurationDoesNotThrowOrCorruptHandlers.
  Spec target section: Logging / Concurrency Semantics.

  ID: LOG-RETRY-001
  Contract statement: Retry after failed logging setup or write must not reuse corrupted handler, writer, file, or
  cached configuration state.
  Rationale: Recovery from logging failure must restore observability rather than permanently disabling or duplicating
  diagnostics.
  Source locations: OALogUtil.levelRoot, OALogUtil.consoleOnly, handler setup in OALogger.
  Related CODEX findings: stale levelRoot can prevent retry from reconfiguring logging.
  Suggested unit tests: testRetryConsoleOnlyAfterFailedSetupRebuildsHandlers,
  testRetryAfterDisableWithSameLevelReconfiguresLogger.
  Spec target section: Logging / Retry Semantics.

  ID: LOG-RECORD-001
  Contract statement: OA log-record objects must preserve the identity and command semantics needed by their owning
  log/audit/replay use case.
  Rationale: A save/delete diagnostic or transaction-style log record is misleading if it survives without the
  affected object identity.
  Source locations: OALogRecord.getObject, OALogRecord.setObject, OALogRecord.getCommand, OALogRecord.setCommand,
  @OAClass metadata.
  Related CODEX findings: OALogRecord.object is transient and not preserved across serialization-style boundaries.
  Suggested unit tests: testLogRecordPreservesCommand, testLogRecordObjectReferenceContractIsExplicit,
  testSerializedLogRecordDoesNotAppearReplayableWithoutObjectIdentity.
  Spec target section: Logging / Record Identity Semantics.

  ID: LOG-INTEGRATION-001
  Contract statement: Logging behavior must remain compatible with config, io, runtime, queue, sync, replication,
  remote, datasource, and process contracts.
  Rationale: These packages rely on logging for production diagnostics, failure visibility, and operational triage.
  Source locations: OALogUtil used by sync tests/runtime diagnostics; OALogger.getLogger(Class) used by object, hub,
  datasource, trigger, and tests.
  Related CODEX findings: stale/duplicate/missing logging affects cross-package diagnostics.
  Suggested unit tests: testRuntimePackageLoggerUsesConfiguredRoute,
  testSyncDiagnosticThreadDumpAvailableAfterLoggerSetup,
  testObjectHubDatasourceLoggersRemainObservableAfterReconfiguration.
  Spec target section: Logging / Cross-Runtime Observability.

  Suggested Package-Level Spec Summary

  com.viaoa.log is responsible for OA runtime observability and diagnostic support. It provides logger lookup, custom
  OA severity levels, console/log routing setup, thread-dump formatting, stack-trace formatting, and lightweight log-
  record modeling.

  It must guarantee:

  - Critical runtime failures remain observable.
  - Logger level/category routing is explicit and deterministic.
  - Reconfiguration does not leave stale, mixed, duplicated, or disabled logger state.
  - OA-owned handlers/resources are flushed and closed at replacement/shutdown boundaries.
  - Concurrent logging and reconfiguration do not corrupt handler state or suppress required diagnostics.
  - Retry after failed logging setup can restore observability.
  - Log record objects preserve the identity/command semantics required by their owning use case.

  It must never silently:

  - Drop critical diagnostics.
  - Duplicate records through repeated setup.
  - Suppress required logs through stale cached configuration.
  - Remove handlers without honoring close/flush ownership.
  - Treat failed logging setup as successful runtime observability.

  Likely unit-test categories:

  - logger setup/reconfiguration tests
  - disable/re-enable tests
  - duplicate-handler tests
  - handler close/flush tests
  - level/category routing tests
  - concurrent reconfiguration tests
  - failure/retry tests
  - OALogRecord identity/serialization contract tests


qqqqqqqqqqqq other

Architectural Assessment

  com.viaoa.log is small and mostly wraps java.util.logging, but it mutates JVM-global logger state. That makes setup
  idempotency, handler cleanup, and failed reconfiguration behavior important. The current model is workable for
  tests/simple bootstrap, but it is not yet production-grade for repeated runtime reconfiguration or observability
  under failure.

  Top Production Risks

  - Logging can remain disabled after a prior disable() because of stale levelRoot.
  - Repeated setup can duplicate records and mislead failure diagnosis.
  - Removed file/stream handlers are not closed.
  - Failed logger setup can partially disable existing logging.
  - OALogRecord does not preserve its affected object across serialization-style boundaries.


*/


