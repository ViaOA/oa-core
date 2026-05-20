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
package com.viaoa.io;

/* CODEX Invariants

com.viaoa.io Invariants

  ID: IO-READ-001
  Contract statement: Read operations must not report success unless the required data was actually read, or EOF/
  partial-read behavior is explicitly part of the method contract.
  Rationale: OA uses I/O helpers for config, resources, generated text, remote payloads, and tooling. Silent
  incomplete reads can produce corrupted runtime/tooling state.
  Source locations: OAFile.readResourceTextFile, OAFile.readTextFile(...), OAFindFile.findZip,
  OACompressWrapper.readObject.
  Related CODEX findings: compressed wrapper has no explicit read boundary; text resource byte-to-char conversion
  corrupts multi-byte data.
  Suggested unit tests: testReadTextFileReadsCompleteFile, testReadResourceTextFileReturnsAllLines,
  testCompressedWrapperReadDoesNotConsumeFollowingStreamData.
  Spec target section: I/O Runtime / Read Semantics.

  ID: IO-WRITE-001
  Contract statement: Write operations must not report success unless all required bytes/text/object data were
  accepted by the stream and written according to the method contract.
  Rationale: OA writes generated files, config-like files, text outputs, and serialized/compressed payloads.
  Incomplete writes must not look valid.
  Source locations: OAFile.copy, OAFile.copyResourceToFile, OAFile.writeTextFile, OACompressWrapper.writeObject.
  Related CODEX findings: direct text writes can leave partial final files; compressed wrapper lacks explicit payload
  boundary.
  Suggested unit tests: testWriteTextFileWritesCompleteData, testCopyWritesAllSourceBytes,
  testCompressedWrapperWritesBoundedPayload.
  Spec target section: I/O Runtime / Write Semantics.

  ID: IO-COMMIT-001
  Contract statement: I/O operations must define when output is committed, and must not expose partial output as the
  committed result before required write/flush/close/replace steps complete.
  Rationale: Failed I/O should not corrupt the last known-good config, generated file, replication artifact, or
  runtime resource.
  Source locations: OAFile.copy, OAFile.writeTextFile, OAFile.renameTo, OAFile.delTree.
  Related CODEX findings: copy deletes destination before replacement succeeds; writeTextFile writes directly to final
  file; renameTo and delTree ignore commit failure results.
  Suggested unit tests: testCopyFailurePreservesExistingDestination, testWriteTextFileFailureDoesNotCommitPartialFile,
  testRenameFailureIsVisible.
  Spec target section: I/O Runtime / Commit Semantics.

  ID: IO-FLUSH-001
  Contract statement: Flush, finish, close, and optional durability operations must happen in the correct order before
  an operation is considered complete.
  Rationale: Serialization, compression, and file writes depend on stream finalization. Missing finish/flush can
  truncate output or corrupt object stream boundaries.
  Source locations: OAFile.writeTextFile, OAFile.copy, OACompressWrapper.writeObject.
  Related CODEX findings: compressed wrapper stream boundary requires proper finish/length handling; direct final-file
  writes need complete close before commit.
  Suggested unit tests: testWriteTextFileFlushesBeforeReturning, testCompressedWrapperFinishesDeflaterBeforeCommit,
  testCopyClosesOutputBeforeReplace.
  Spec target section: I/O Runtime / Flush and Finish Semantics.

  ID: IO-CLOSE-001
  Contract statement: Streams, readers, writers, zip streams, and compression resources opened by OA must be closed or
  ended on success and failure unless ownership is explicitly transferred.
  Rationale: Leaked file handles, zip streams, or compression native resources can block reloads, generated file
  replacement, jar scans, and remote traffic.
  Source locations: OAFile.copy, copyResourceToFile, readResourceTextFile, readTextFile, writeTextFile; OAFindFile.fi
  ndZip; OACompressWrapper.writeObject/readObject.
  Related CODEX findings: many OAFile helpers close only on success; OAFindFile.findZip never closes zip/file streams;
  OACompressWrapper does not explicitly release Deflater/Inflater.
  Suggested unit tests: testCopyClosesStreamsOnWriteFailure, testFindZipClosesArchiveStream,
  testCompressWrapperEndsInflaterDeflater.
  Spec target section: I/O Runtime / Resource Close Semantics.

  ID: IO-RESOURCE-001
  Contract statement: Resource lookup must resolve the intended classpath/archive/resource target deterministically
  and must distinguish “not found” from “failed to read.”
  Rationale: OA tooling and runtime code read templates, config fragments, generated resources, and archive contents.
  Wrong or partial resource lookup creates bad downstream state.
  Source locations: OAFile.copyResourceToFile, readResourceTextFile, readTextFile(Class,...); OAFindFile.findZip.
  Related CODEX findings: resource and archive streams need cleanup; zip scan errors are printed and ignored.
  Suggested unit tests: testMissingResourceReturnsDocumentedResult, testUnreadableResourceFailsVisibly,
  testFindFileArchiveReadFailureIsObservableByContract.
  Spec target section: I/O Runtime / Resource Lookup Semantics.

  ID: IO-RETRY-001
  Contract statement: Retry after failed I/O must not reuse corrupted stream/file/helper state, partial output, stale
  search results, or invalid compression state.
  Rationale: OA runtime/tooling often retries after transient file, classpath, or network filesystem failures.
  Retrying must not skip work or use stale state.
  Source locations: OAFile.copy, writeTextFile; OAFindFile.findAll; OACompressWrapper; config/replication consumers.
  Related CODEX findings: direct final-file writes can leave corrupted retry state; OAFindFile.findAll leaves instance
  state after exception.
  Suggested unit tests: testCopyRetryAfterFailureUsesValidDestinationState,
  testWriteRetryAfterFailureReplacesPartialOutput, testFindAllClearsStateAfterFailure.
  Spec target section: I/O Runtime / Retry Semantics.

  ID: IO-PATH-001
  Contract statement: File path and resource path handling must be deterministic and must not silently convert between
  filesystem and classpath naming rules incorrectly.
  Rationale: OA uses both platform file paths and classpath resource paths. Mixing separator rules can make tooling/
  config/resource lookup resolve the wrong target.
  Source locations: OAFile.convertFileName, getFileName, getDirectoryName, mkdirsForFile, copyResourceToFile,
  readTextFile(Class,...); OAFindFile.
  Related CODEX findings: none beyond resource resolution/cleanup; classloader/package path issues are related cross-
  package.
  Suggested unit tests: testConvertFileNamePreservesExpectedFilesystemPath,
  testResourcePathUsesSlashNotPlatformSeparator, testMkdirsForFileCreatesOnlyParentDirectories.
  Spec target section: I/O Runtime / Path and Resource Name Semantics.

  ID: IO-ENCODING-001
  Contract statement: Encoding/charset behavior must be explicit where persisted, generated, resource, serialized,
  logged, or compared text depends on exact content.
  Rationale: Platform-default encodings and byte-to-char conversion cause cross-platform drift and corrupt non-ASCII
  text.
  Source locations: OAFile.readResourceTextFile, readTextFile, writeTextFile.
  Related CODEX findings: readTextFile(Class,...) casts bytes to chars; text helpers use platform default charset.
  Suggested unit tests: testReadTextResourceUtf8RoundTripsNonAscii, testWriteTextFileUsesDocumentedCharset,
  testReadWriteTextFileConsistentAcrossDefaultCharsets.
  Spec target section: I/O Runtime / Encoding Semantics.

  ID: IO-TEMP-001
  Contract statement: Temp-file and write-replace behavior must protect committed output from partial writes and
  failed replacements.
  Rationale: Config files, generated files, and runtime artifacts should not be destroyed by failed writes.
  Source locations: OAFile.copy, OAFile.writeTextFile, OAFile.renameTo.
  Related CODEX findings: copy deletes destination first; writeTextFile writes directly to final file; rename failure
  is ignored.
  Suggested unit tests: testTempFileReplacePreservesOldFileOnFailure, testAtomicCopyReplacesOnlyAfterFullWrite,
  testFailedRenameDoesNotClaimCommit.
  Spec target section: I/O Runtime / Temp and Replace Semantics.

  ID: IO-FAIL-001
  Contract statement: I/O failure must be caller-visible or observable and must not silently appear successful.
  Boolean/no-op APIs must clearly distinguish expected “not found” from actual failure.
  Rationale: Hidden I/O failures can corrupt persisted files, miss resources, lose generated output, or leave stale
  runtime state.
  Source locations: OAFile.copyTo, copy, copyResourceToFile, renameTo, delTree, writeTextFile; OAFindFile.findFile.
  Related CODEX findings: copyTo swallows exceptions; renameTo and delTree ignore failed results; copy(String,String)
  returns on null input.
  Suggested unit tests: testCopyFailureExposesCause, testDeleteFailureThrowsIOException,
  testBooleanCopyDoesNotHidePartialDestinationState.
  Spec target section: I/O Runtime / Failure Visibility.

  ID: IO-CONCURRENT-001
  Contract statement: Concurrent I/O must not corrupt shared files, helper state, buffers, streams, search results, or
  compression boundaries. Shared helper instances must be synchronized or documented as single-use.
  Rationale: OA tooling/runtime may read/write/search resources concurrently. Shared mutable state can produce
  nondeterministic results or corrupted output.
  Source locations: OAFindFile.findAll mutable fields; OAFile static file helpers; OACompressWrapper serialization
  boundaries.
  Related CODEX findings: OAFindFile.findAll retains mutable state on failure; direct writes/copies have no
  coordination around final files.
  Suggested unit tests: testConcurrentFindAllOnSameInstanceIsRejectedOrSafe,
  testConcurrentWriteSameFileDoesNotProduceMixedOutputByContract,
  testConcurrentCompressedWrappersPreserveStreamBoundaries.
  Spec target section: I/O Runtime / Concurrency Semantics.

  ID: IO-SERIAL-001
  Contract statement: Serialization/compression I/O boundaries must be explicit and must not consume or emit bytes
  outside the owning object’s serialized payload.
  Rationale: OA remote/sync/serialization streams contain multiple adjacent objects and fields. Boundary corruption
  can break request/response decoding and replication/sync payloads.
  Source locations: OACompressWrapper.writeObject/readObject; consumers in remote multiplexer client/server.
  Related CODEX findings: compressed payload has no explicit length boundary and can over-read parent object stream.
  Suggested unit tests: testCompressedWrapperDoesNotConsumeFollowingObject,
  testCompressedWrapperRoundTripsMultipleAdjacentValues,
  testCompressedWrapperFailureLeavesParentStreamDetectablyInvalid.
  Spec target section: I/O Runtime / Serialization Boundary Semantics.

  ID: IO-INTEGRATION-001
  Contract statement: I/O behavior must remain compatible with serialize, config, datasource, replication transaction
  logs, load/save, classloader, remote/sync, and runtime contracts.
  Rationale: I/O helpers are foundational. Bad helper semantics propagate into configuration, generated model loading,
  transaction logs, remote payloads, and runtime artifacts.
  Source locations: OAFile; OAFindFile; OACompressWrapper; cross-package consumers in config, serialize, replication,
  remote, classloader, runtime/tooling.
  Related CODEX findings: compression boundary affects remote; charset and partial write risks affect config/tooling;
  cleanup risks affect repeated runtime operations.
  Suggested unit tests: testConfigFileWriteUsesSafeCommit, testReplicationLogIoFailureIsVisible,
  testRemoteCompressedPayloadBoundaryPreserved.
  Spec target section: I/O Runtime / Cross-Package Integration.

  Suggested Package-Level Spec Summary

  - com.viaoa.io provides low-level file, stream, text, resource, archive-search, and compression/serialization-
    boundary helpers for OA runtime and tooling.
  - Reads must return complete, correctly decoded data or explicitly report EOF/partial/missing behavior.
  - Writes must not report success until all required bytes/text/object data are written and finalized according to
    the method contract.
  - Commit points must be explicit; final files/resources must not be exposed as valid when writes or replacements are
    partial.
  - OA-opened streams/readers/writers/archive streams/compression resources must be closed or ended on success and
    failure.
  - Resource lookup must distinguish missing resources from read failures.
  - Retry after failed I/O must not reuse corrupted files, streams, helper state, or partial outputs.
  - File paths and classpath resource paths must use distinct deterministic normalization rules.
  - Text encoding must be explicit for persisted/generated/resource/config/log text.
  - Concurrent I/O must avoid shared mutable state corruption or document single-use behavior.
  - Serialization/compression stream boundaries must be length-bounded or otherwise exact.
  - I/O helpers must support config, serialization, replication logs, datasource/load/save, classloader, remote/sync,
    and runtime contracts without silent false-success.


*/


