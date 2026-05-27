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

//CODEX unit tests <todo>

/* CODEX Invariants

IO-READ-001 — Complete Read Semantics
Contract statement:
Read operations must not report success unless the required bytes, text, lines, resource content, archive entries,
or serialized payload were actually read, or EOF/partial-read behavior is explicitly part of the method contract.
Rationale:
OA I/O helpers support config, generated resources, serialization payloads, remote/sync traffic, replication logs,
and runtime tooling; incomplete reads can corrupt downstream runtime state.
Source scope:
OAFile.readResourceTextFile(...), readTextFile(...), OAFindFile.findAll(...), findFile(...), findZip(...),
OACompressWrapper readObject boundary.
Related CODEX findings:
OACompressWrapper compressed payload has no explicit read boundary; OAFile text resource byte-to-char conversion can
corrupt multi-byte data.
Suggested unit tests:
ioReadTextFileReadsCompleteFile(), ioReadResourceTextFileReturnsAllLines(),
ioCompressedWrapperReadDoesNotConsumeFollowingStreamData().
Spec target section:
I/O Runtime / Read Semantics.

IO-WRITE-001 — Complete Write Semantics
Contract statement:
Write operations must not report success unless all required bytes, text, copied content, resource content, or
serialized payload data were accepted and written according to the method contract.
Rationale:
OA writes generated files, config-like files, serialized payloads, compressed payloads, and runtime artifacts;
incomplete writes must not look valid.
Source scope:
OAFile.copyTo(...), copy(...), copyResourceToFile(...), writeTextFile(...), OACompressWrapper writeObject boundary.
Related CODEX findings:
direct text writes can leave partial final files; OACompressWrapper lacks explicit compressed payload boundary.
Suggested unit tests:
ioWriteTextFileWritesCompleteData(), ioCopyWritesAllSourceBytes(), ioCompressedWrapperWritesBoundedPayload().
Spec target section:
I/O Runtime / Write Semantics.

IO-COMMIT-001 — Output Commit Boundary
Contract statement:
I/O operations must define when output becomes committed and must not expose partial output as the committed result
before required write, flush, close, and replace steps complete.
Rationale:
Failed I/O must not corrupt last-known-good config, generated files, replication artifacts, serialized payloads, or
runtime resources.
Source scope:
OAFile.copyTo(...), copy(...), writeTextFile(...), renameTo(...), rmDir(...), removeDir(...), delTree(...).
Related CODEX findings:
copy deletes destination before replacement succeeds; writeTextFile writes directly to final file; renameTo and
delete-tree paths can ignore failed commit results.
Suggested unit tests:
ioCopyFailurePreservesExistingDestination(), ioWriteTextFileFailureDoesNotCommitPartialFile(),
ioRenameFailureIsVisible().
Spec target section:
I/O Runtime / Commit Semantics.

IO-FLUSH-001 — Flush, Finish, and Close Ordering
Contract statement:
Flush, compression finish, stream close, and optional durability operations must occur in the correct order before
an I/O operation is considered complete.
Rationale:
File writes, serialization, and compression depend on stream finalization; missing finalization can truncate output
or corrupt object stream boundaries.
Source scope:
OAFile.copy(...), writeTextFile(...), OACompressWrapper writeObject/readObject boundary.
Related CODEX findings:
compressed wrapper boundary requires proper finish/length handling; direct final-file writes need complete close
before commit.
Suggested unit tests:
ioWriteTextFileFlushesBeforeReturning(), ioCompressedWrapperFinishesDeflaterBeforeCommit(),
ioCopyClosesOutputBeforeCommit().
Spec target section:
I/O Runtime / Flush and Finish Semantics.

IO-CLOSE-001 — Owned Resource Cleanup
Contract statement:
Streams, readers, writers, zip streams, file streams, compression resources, and other I/O resources opened by OA
must be closed or ended on success and failure unless ownership is explicitly transferred.
Rationale:
Leaked file handles, archive streams, or compression resources can block reloads, generated-file replacement, jar
scans, remote traffic, and long-running runtime cleanup.
Source scope:
OAFile.copy(...), copyResourceToFile(...), readResourceTextFile(...), readTextFile(...), writeTextFile(...);
OAFindFile.findZip(...); OACompressWrapper serialization boundary.
Related CODEX findings:
many OAFile helpers close only on success; OAFindFile.findZip does not close zip/file streams; OACompressWrapper
does not explicitly release Deflater/Inflater.
Suggested unit tests:
ioCopyClosesStreamsOnWriteFailure(), ioFindZipClosesArchiveStream(), ioCompressWrapperEndsInflaterDeflater().
Spec target section:
I/O Runtime / Resource Ownership and Cleanup Semantics.

IO-RESOURCE-001 — Resource Lookup Semantics
Contract statement:
Resource lookup must resolve the intended classpath, archive, file, or resource target deterministically and must
distinguish missing resources from failed reads.
Rationale:
OA tooling and runtime code read templates, config fragments, generated resources, and archive contents; wrong or
partial lookup creates incorrect downstream runtime state.
Source scope:
OAFile.copyResourceToFile(...), readResourceTextFile(...), readTextFile(Class,...), OAFindFile.findAll(...),
findZip(...).
Related CODEX findings:
resource and archive streams need cleanup; zip scan errors can be printed and ignored.
Suggested unit tests:
ioMissingResourceReturnsDocumentedResult(), ioUnreadableResourceFailsVisibly(), ioArchiveReadFailureIsObservable().
Spec target section:
I/O Runtime / Resource Lookup Semantics.

IO-PATH-001 — File and Resource Path Semantics
Contract statement:
Filesystem path handling and classpath resource path handling must be deterministic and must not silently convert
between incompatible naming rules.
Rationale:
OA uses both platform file paths and classpath resource names; mixing separator or normalization rules can resolve
the wrong target.
Source scope:
OAFile.convertFileName(...), getFileName(...), getDirectoryName(...), getExtension(...), mkdirsForFile(...),
copyResourceToFile(...), readTextFile(Class,...), OAFindFile.
Related CODEX findings:
none observed beyond resource resolution/cleanup; classloader/package path issues are cross-package related.
Suggested unit tests:
ioConvertFileNamePreservesExpectedFilesystemPath(), ioResourcePathUsesClasspathSeparators(),
ioMkdirsForFileCreatesOnlyParentDirectories().
Spec target section:
I/O Runtime / Path and Resource Name Semantics.

IO-ENCODING-001 — Explicit Text Encoding Boundary
Contract statement:
Encoding and charset behavior must be explicit where persisted, generated, resource, serialized, logged, or compared
text depends on exact content.
Rationale:
Platform-default encodings and byte-to-char conversion cause cross-platform drift and can corrupt non-ASCII runtime
or tooling text.
Source scope:
OAFile.readResourceTextFile(...), readTextFile(...), writeTextFile(...).
Related CODEX findings:
readTextFile(Class,...) casts bytes to chars; text helpers use platform default charset.
Suggested unit tests:
ioReadTextResourceUtf8RoundTripsNonAscii(), ioWriteTextFileUsesDocumentedCharset(),
ioReadWriteTextFileConsistentAcrossDefaultCharsets().
Spec target section:
I/O Runtime / Encoding Semantics.

IO-TEMP-001 — Staged Write and Replace Semantics
Contract statement:
Temp-file or staged write-and-replace behavior must protect committed output from partial writes and failed
replacements when the method contract implies committed file replacement.
Rationale:
Config files, generated files, serialized artifacts, and runtime outputs should not be destroyed by failed writes.
Source scope:
OAFile.copy(...), writeTextFile(...), renameTo(...), copyResourceToFile(...).
Related CODEX findings:
copy deletes destination first; writeTextFile writes directly to final file; rename failure can be ignored.
Suggested unit tests:
ioTempFileReplacePreservesOldFileOnFailure(), ioAtomicCopyReplacesOnlyAfterFullWrite(),
ioFailedRenameDoesNotClaimCommit().
Spec target section:
I/O Runtime / Temp and Replace Semantics.

IO-FAIL-001 — I/O Failure Visibility
Contract statement:
I/O failures must be caller-visible or observable and must not silently appear successful; boolean/no-op APIs must
clearly distinguish expected “not found” from actual failure.
Rationale:
Hidden I/O failures can corrupt persisted files, miss resources, lose generated output, truncate payloads, or leave
stale runtime state.
Source scope:
OAFile.copyTo(...), copy(...), copyResourceToFile(...), renameTo(...), rmDir(...), removeDir(...), delTree(...),
writeTextFile(...), OAFindFile.findAll(...), findFile(...), findZip(...), OACompressWrapper.
Related CODEX findings:
copyTo swallows exceptions; renameTo and delTree ignore failed results; copy(String,String) returns on null input.
Suggested unit tests:
ioCopyFailureExposesCause(), ioDeleteFailureThrowsOrReportsIOException(),
ioBooleanCopyDoesNotHidePartialDestinationState().
Spec target section:
I/O Runtime / Failure and False-Success Prevention.

IO-PARTIAL-001 — Partial Progress Visibility
Contract statement:
Partial reads, writes, copies, deletes, renames, archive scans, or compression operations must either be externally
visible as incomplete or be rolled back to the prior committed state.
Rationale:
OA runtime persistence and transport boundaries must not treat partially completed I/O as complete semantic input or
output.
Source scope:
OAFile.copyTo(...), copy(...), writeTextFile(...), renameTo(...), delTree(...), OAFindFile.findAll(...),
OACompressWrapper.
Related CODEX findings:
direct final-file writes and delete-before-copy can expose partial output; findAll can leave state after exception.
Suggested unit tests:
ioPartialCopyDoesNotLookComplete(), ioPartialWriteLeavesObservableFailure(),
ioFindAllFailureDoesNotExposePartialSuccessAsComplete().
Spec target section:
I/O Runtime / Partial Progress Semantics.

IO-RETRY-001 — Retry After Failure Correctness
Contract statement:
Retry after failed I/O must not reuse corrupted streams, partial files, stale helper state, invalid compression
state, or incomplete search results.
Rationale:
OA runtime and tooling may retry after transient filesystem, archive, classpath, or network-backed I/O failures.
Source scope:
OAFile.copy(...), writeTextFile(...), copyResourceToFile(...), OAFindFile.findAll(...), OACompressWrapper.
Related CODEX findings:
direct final-file writes can leave corrupted retry state; OAFindFile.findAll leaves instance state after exception.
Suggested unit tests:
ioCopyRetryAfterFailureUsesValidDestinationState(), ioWriteRetryAfterFailureReplacesPartialOutput(),
ioFindAllClearsStateAfterFailure().
Spec target section:
I/O Runtime / Retry Semantics.

IO-STATE-001 — Mutable Helper State Consistency
Contract statement:
I/O helper instance state used for searches, paths, buffers, names, or payload boundaries must be initialized,
committed, and cleared consistently across success and failure.
Rationale:
Stale helper state can cause later calls to return wrong files, duplicate search results, or corrupted stream
boundaries.
Source scope:
OAFindFile.findAll(...), findFile(...), findZip(...), OAFile instance methods, OACompressWrapper object payload.
Related CODEX findings:
OAFindFile.findAll retains mutable state after exception.
Suggested unit tests:
ioFindAllDoesNotRetainResultsAcrossCalls(), ioFindAllClearsStateAfterException(),
ioCompressWrapperPayloadStateIsPerObject().
Spec target section:
I/O Runtime / Helper State Semantics.

IO-SERIAL-001 — Serialization and Compression Payload Boundary
Contract statement:
Serialization and compression I/O boundaries must be explicit and must not consume or emit bytes outside the owning
object’s serialized payload.
Rationale:
OA remote, sync, replication, and serialization streams can contain multiple adjacent objects and fields; boundary
corruption breaks request/response decoding and replay.
Source scope:
OACompressWrapper.writeObject/readObject serialization behavior; integration with serialize, remote, sync, and repl
ication consumers.
Related CODEX findings:
compressed payload has no explicit length boundary and can over-read the parent object stream.
Suggested unit tests:
ioCompressedWrapperDoesNotConsumeFollowingObject(), ioCompressedWrapperRoundTripsMultipleAdjacentValues(),
ioCompressedWrapperFailureLeavesParentStreamDetectablyInvalid().
Spec target section:
I/O Runtime / Serialization Boundary Semantics.

IO-BINARY-001 — Binary Versus Text Boundary
Contract statement:
Binary I/O operations and text I/O operations must remain distinct, with explicit conversion boundaries when bytes
become characters or characters become bytes.
Rationale:
Compression, serialization, archives, and copied files require byte fidelity, while text helpers require charset-
aware decoding and encoding.
Source scope:
OAFile.copy(...), copyResourceToFile(...), readResourceTextFile(...), readTextFile(...), writeTextFile(...),
OACompressWrapper, OAFindFile.findZip(...).
Related CODEX findings:
text resource byte-to-char conversion corrupts multi-byte data; text helpers use platform default charset.
Suggested unit tests:
ioBinaryCopyPreservesExactBytes(), ioTextReadUsesDocumentedDecoding(), ioTextWriteUsesDocumentedEncoding().
Spec target section:
I/O Runtime / Binary and Text Boundary Semantics.

IO-CONCURRENT-001 — Concurrent I/O Correctness
Contract statement:
Concurrent I/O must not corrupt shared files, helper state, buffers, streams, search results, or compression
boundaries; shared helper instances must be synchronized or documented as single-use.
Rationale:
OA tooling and runtime code may read, write, search, serialize, and copy resources concurrently.
Source scope:
OAFile static helpers; OAFindFile mutable search state; OACompressWrapper payload boundaries; shared final files/
resources.
Related CODEX findings:
OAFindFile.findAll retains mutable state on failure; direct writes/copies have no coordination around final files.
Suggested unit tests:
ioConcurrentFindAllOnSameInstanceIsRejectedOrSafe(), ioConcurrentWriteSameFileDoesNotProduceMixedOutputByContract(),
ioConcurrentCompressedWrappersPreserveStreamBoundaries().
Spec target section:
I/O Runtime / Concurrency Semantics.

IO-INTERRUPT-001 — Interruption, Timeout, and Disconnect Boundary
Contract statement:
I/O behavior under interruption, timeout, EOF, disconnect, or truncated input must be deterministic and must not
report successful completion unless the operation contract explicitly allows that terminal state.
Rationale:
OA remote, sync, replication, datasource, and tooling operations can cross unreliable storage or transport
boundaries.
Source scope:
OAFile read/write/copy helpers; OAFindFile archive scanning; OACompressWrapper read/write boundary; cross-package
transport/storage consumers.
Related CODEX findings:
compressed payload boundary and partial read/write findings illustrate truncated-stream risk.
Suggested unit tests:
ioTruncatedCompressedPayloadFailsVisibly(), ioUnexpectedEOFDoesNotReturnSuccessfulPayload(),
ioInterruptedIoDoesNotCommitPartialOutput().
Spec target section:
I/O Runtime / Terminal Stream State Semantics.

IO-CLEANUP-001 — Delete and Directory Cleanup Semantics
Contract statement:
Directory and tree deletion helpers must report or throw when required deletions fail and must not silently claim
cleanup success.
Rationale:
Failed cleanup can leave stale generated files, old runtime artifacts, bad replication logs, or mixed output
directories.
Source scope:
OAFile.rmDir(...), removeDir(...), delTree(...).
Related CODEX findings:
delTree ignores failed delete results.
Suggested unit tests:
ioDeleteTreeFailureIsVisible(), ioRemoveDirDeletesExpectedChildrenOnly(),
ioCleanupDoesNotSilentlyLeaveRequiredFile().
Spec target section:
I/O Runtime / Cleanup Semantics.

IO-BOUNDARY-001 — I/O Success Versus Object Graph Success
Contract statement:
Successful I/O completion only establishes file, stream, resource, or payload boundary success; it must not imply
successful Object Graph mutation, datasource commit, serialization semantic validity, sync application, replication
replay, or runtime initialization.
Rationale:
I/O is a transport and persistence boundary, while semantic runtime success belongs to the consuming package.
Source scope:
OAFile; OAFindFile; OACompressWrapper; integration boundaries with serialize, config, datasource, remote, comm,
sync, replication, object, hub, cache, classloader, and graph packages.
Related CODEX findings:
compression boundary affects remote; charset and partial write risks affect config/tooling; cleanup risks affect
repeated runtime operations.
Suggested unit tests:
ioConfigFileWriteSuccessDoesNotImplyConfigSemanticValidity(),
ioCompressedPayloadReadSuccessDoesNotImplySyncApplySuccess(), ioFileCopySuccessDoesNotImplyDatasourceCommit().
Spec target section:
I/O Runtime / Runtime Boundary Semantics.

IO-INTEGRATION-001 — Cross-Package I/O Compatibility
Contract statement:
I/O behavior must remain compatible with serialize, config, datasource, replication transaction logs, load/save,
classloader, remote, comm, sync, cache, object, hub, and graph runtime contracts.
Rationale:
I/O helpers are foundational; silent helper failures propagate into configuration, generated model loading,
transaction logs, remote payloads, and runtime artifacts.
Source scope:
OAFile; OAFindFile; OACompressWrapper; cross-package consumers in config, serialize, replication, remote,
classloader, datasource, and runtime/tooling.
Related CODEX findings:
compressed payload boundary affects remote; charset and partial-write risks affect config/tooling; resource cleanup
risks affect long-running operations.
Suggested unit tests:
ioConfigFileWriteUsesSafeCommit(), ioReplicationLogIoFailureIsVisible(),
ioRemoteCompressedPayloadBoundaryPreserved().
Spec target section:
I/O Runtime / Cross-Package Integration Semantics.

*/
