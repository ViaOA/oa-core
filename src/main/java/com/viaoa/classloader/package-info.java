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
package com.viaoa.classloader;

/* CODEX Invariants

CLASSLOAD-RESOLVE-001 — Intended Class Resolution
Contract statement:
Class resolution must return the intended Class instance for OA runtime, generated model, tooling, metadata,
annotation, reflection, datasource, serialization, and Object Graph use.
Rationale:
OA metadata and graph behavior are keyed by Java Class identity; resolving the wrong class breaks annotations,
paths, datasource mappings, serialization, and generated blueprint/runtime alignment.
Source scope:
OAClassLoader.loadClass(...); OAClassUtil.getClassName(...); OAClassUtil.getPackageName(...); ClassModifier;
integration with metadata, annotation, reflect, model, object, graph, datasource, serialization, sync, and
replication packages.
Related CODEX findings:
target class bytes are loaded only from the system classloader resource path.
Suggested unit tests:
classloaderTargetClassLoadedFromConfiguredSourceLoader(), classloaderGeneratedModelClassResolvesToIntendedClass(),
classloaderResolutionPreservesOAObjectTypeIdentity().
Spec target section:
Classloader Runtime / Class Resolution Semantics.

CLASSLOAD-RESOURCE-001 — Class Resource Resolution
Contract statement:
Class and resource lookup must use deterministic OA package/path naming semantics and the configured classloader
boundary.
Rationale:
Generated model classes and tooling resources may live outside the system classpath; OA must resolve them
consistently in app, test, builder, module, and generated-code environments.
Source scope:
OAClassLoader.loadClass(...), class-name-to-resource conversion, OAClassUtil.getPackageName(...),
OAClassUtil.getClassName(...).
Related CODEX findings:
target bytes are read with ClassLoader.getSystemResourceAsStream(...), bypassing context/application loader
resources.
Suggested unit tests:
classloaderClassResourceResolvedFromContextOrConfiguredLoader(),
classloaderClassNameConvertedToResourcePathCorrectly(), classloaderMissingClassResourceFailsVisibly().
Spec target section:
Classloader Runtime / Resource Resolution Semantics.

CLASSLOAD-IDENTITY-001 — Stable Class Identity
Contract statement:
Class identity must remain stable anywhere OA metadata, annotations, reflection, datasource mapping, serialization,
object cache behavior, and graph routing depend on Class equality.
Rationale:
Loading the same logical model class through different loaders can split metadata, cache, serialization, datasource,
and graph identity.
Source scope:
OAClassLoader.loadClass(...); metadata/annotation/reflect/serialize/datasource/graph consumers.
Related CODEX findings:
dependency delegation and target-source loader issues can create or expose class identity drift.
Suggested unit tests:
classloaderSameLoaderReturnsSameClassInstance(), classloaderMetadataBuiltForLoadedClassMatchesRuntimeClass(),
classloaderSerializedClassIdentityCompatibleWithRuntimeLoader().
Spec target section:
Classloader Runtime / Class Identity Semantics.

CLASSLOAD-DELEGATE-001 — Deterministic Delegation
Contract statement:
Parent-first, child-first, context-loader, and system-loader delegation behavior must be explicit and deterministic;
target child-definition behavior must be scoped to the intended target class.
Rationale:
Generated classes often depend on OA runtime/model/helper classes; dependency resolution through the wrong loader
can produce ClassNotFoundException or duplicate runtime type identities.
Source scope:
OAClassLoader.loadClass(...), ClassLoader delegation behavior, parent/context/application loader boundaries.
Related CODEX findings:
non-target class resolution delegates only to findSystemClass(...), bypassing normal parent/context delegation.
Suggested unit tests:
classloaderNonTargetClassDelegatesToParentLoader(), classloaderTargetClassUsesDocumentedChildFirstRule(),
classloaderDependencyClassResolvesFromApplicationLoader().
Spec target section:
Classloader Runtime / Delegation Semantics.

CLASSLOAD-DUP-001 — Duplicate Definition Prevention
Contract statement:
Duplicate class definition in the same loader must be prevented or must fail visibly without corrupting loader
state.
Rationale:
Duplicate definition creates LinkageError and can leave classloader state ambiguous under concurrent or retry use.
Source scope:
OAClassLoader.loadClass(...), clazz cache, defineClass(...).
Related CODEX findings:
concurrent loadClass calls can both see clazz == null and both call defineClass.
Suggested unit tests:
classloaderConcurrentLoadDefinesTargetOnlyOnce(), classloaderSecondLoadReturnsCachedClass(),
classloaderDuplicateDefineFailureDoesNotPoisonLoaderState().
Spec target section:
Classloader Runtime / Duplicate Definition Semantics.

CLASSLOAD-CACHE-001 — Loaded-Class State Correctness
Contract statement:
Loaded-class/cache state must represent only successfully defined or resolved classes; failed, missing,
incompatible, or partial loads must not be retained as successful entries.
Rationale:
OA metadata and tooling consumers must be able to retry failed class discovery without receiving stale or half-
initialized class state.
Source scope:
OAClassLoader.clazz, OAClassLoader.loadClass(...), cross-package metadata/class caches.
Related CODEX findings:
duplicate/concurrency risk can affect cached class state; clazz is assigned after defineClass success.
Suggested unit tests:
classloaderFailedLoadDoesNotSetCachedClass(), classloaderSuccessfulLoadCachesClass(),
classloaderRetryAfterMissingResourceCanSucceedWithNewSource().
Spec target section:
Classloader Runtime / Loaded-Class Cache Semantics.

CLASSLOAD-FAIL-001 — Class Load Failure Visibility
Contract statement:
Failed class or resource loading must be visible to the caller and must not silently appear successful or fall back
to a different class/resource outside the loader contract.
Rationale:
Silent wrong class/resource resolution corrupts metadata, annotations, reflection, serialization, datasource
mappings, and generated tooling output.
Source scope:
OAClassLoader.loadClass(...), OAClassUtil, ClassModifier, converter/reflect integrations that load classes.
Related CODEX findings:
missing resource and IO failures are visible, but wrong loader-source and delegation behavior can fail valid OA
deployments or return unintended sources.
Suggested unit tests:
classloaderMissingTargetClassThrowsClassNotFound(), classloaderIOExceptionDuringClassReadPreservesCause(),
classloaderWrongLoaderFallbackDoesNotReturnDifferentClassSilently().
Spec target section:
Classloader Runtime / Failure and False-Success Prevention.

CLASSLOAD-RETRY-001 — Retry After Failed Load
Contract statement:
Retry after failed class or resource loading must not reuse corrupted loader state, leaked streams, partially read
byte arrays, stale cached results, or failed semantic metadata.
Rationale:
OA tooling/model loading can retry after generation, classpath, module, or deployment changes; retry must either
work cleanly or fail clearly.
Source scope:
OAClassLoader.loadClass(...), clazz cache, resource stream handling, metadata/annotation consumers.
Related CODEX findings:
resource stream not closed on success/failure; duplicate definition race can leave retry behavior dependent on
previous partial state.
Suggested unit tests:
classloaderRetryAfterIOExceptionDoesNotReusePartialBytes(),
classloaderRetryAfterClassNotFoundUsesFreshResourceLookup(), classloaderFailedConcurrentLoadDoesNotPoisonNextLoad().
Spec target section:
Classloader Runtime / Retry Semantics.

CLASSLOAD-RESOURCE-CLEANUP-001 — Resource Stream Cleanup
Contract statement:
Resource streams opened during class or resource loading must be closed by the loader unless ownership is explicitly
transferred.
Rationale:
Unclosed streams can retain jar/file handles, generated model artifacts, or classloader-related resources in long-
running tooling, test, builder, or server processes.
Source scope:
OAClassLoader.loadClass(...), InputStream resource lookup.
Related CODEX findings:
class resource stream is never closed.
Suggested unit tests:
classloaderLoadClassClosesResourceStreamOnSuccess(), classloaderLoadClassClosesResourceStreamOnReadFailure(),
classloaderRepeatedClassLoadDoesNotLeakResourceHandles().
Spec target section:
Classloader Runtime / Resource Cleanup Semantics.

CLASSLOAD-LEAK-001 — Loader Lifetime Boundary
Contract statement:
Classloader and loaded-class references must not be retained longer than intended where that would prevent unloading
or leak generated, test, plugin, or module model classes.
Rationale:
Generated model tooling, tests, plugins, and app reloads may create short-lived loaders; retained Class references
can hold metadata graphs and runtime state alive.
Source scope:
OAClassLoader.clazz; consumers storing loaded classes/metadata; metadata, annotation, reflect, serialization,
runtime, and tooling caches.
Related CODEX findings:
none observed directly in package; risk belongs to cross-package loader/cache ownership.
Suggested unit tests:
classloaderTemporaryLoaderCanBeGarbageCollectedAfterUse(), classloaderMetadataCacheClearReleasesGeneratedLoader(),
classloaderFailedLoadDoesNotRetainLoader().
Spec target section:
Classloader Runtime / Loader Lifetime Semantics.

CLASSLOAD-CONCURRENT-001 — Concurrent Loading Correctness
Contract statement:
Concurrent class/resource loading must not corrupt loaded-class caches, define the same class twice, expose stale
reads, leak streams, or return inconsistent class identities.
Rationale:
OA runtime and tooling can run metadata discovery, generated model loading, serialization type resolution, and
annotation scanning concurrently.
Source scope:
OAClassLoader.loadClass(...), clazz field, defineClass(...), resource stream handling.
Related CODEX findings:
unsynchronized target loading can duplicate defineClass.
Suggested unit tests:
classloaderConcurrentTargetLoadsReturnSameClass(), classloaderConcurrentTargetLoadDoesNotThrowLinkageError(),
classloaderConcurrentDependencyLoadUsesStableDelegation().
Spec target section:
Classloader Runtime / Concurrency Semantics.

CLASSLOAD-PACKAGE-001 — Package and Class Name Semantics
Contract statement:
Package-name and class-name extraction must return semantically correct values for packaged, default-package,
nested, generated, and dynamically loaded classes.
Rationale:
OA graph/package routing, generated reports, metadata lookup, datasource mapping, resource lookup, and tooling
decisions can depend on package and class strings.
Source scope:
OAClassUtil.getClassName(...), OAClassUtil.getPackageName(...).
Related CODEX findings:
default-package classes return their class name as package name.
Suggested unit tests:
classloaderGetPackageNameForDefaultPackageClass(), classloaderGetPackageNameForPackagedClass(),
classloaderGetPackageNameForNestedClassUsesDeclaringPackage().
Spec target section:
Classloader Runtime / Package and Class Name Semantics.

CLASSLOAD-METADATA-001 — Metadata Construction Boundary
Contract statement:
Java class-load success must not imply OA runtime-semantic validity; loaded classes must still satisfy annotation,
metadata, model, object, datasource, serialization, and graph contracts before being treated as executable OA
blueprints.
Rationale:
A Class can load successfully while being missing required annotations, incompatible with OAObject expectations, or
semantically invalid for runtime metadata.
Source scope:
OAClassLoader; ClassModifier; OAClassUtil; integration with metadata, annotation, model, reflect, object, graph,
datasource, serialization, sync, and replication packages.
Related CODEX findings:
none observed directly; target source-loader and identity findings can cause metadata mismatch.
Suggested unit tests:
classloaderLoadedClassStillRequiresValidMetadata(),
classloaderInvalidModelClassDoesNotBecomeRuntimeValidByLoading(),
classloaderLoadedClassAnnotationsVisibleToMetadata().
Spec target section:
Classloader Runtime / Metadata Construction Boundary Semantics.

CLASSLOAD-SERIAL-001 — Serialization Type Resolution
Contract statement:
Serialization and deserialization must resolve class names through the intended OA runtime loader boundary and must
preserve compatible class identity with runtime metadata.
Rationale:
Serialized Object Graph payloads, remote calls, sync messages, and replication data depend on resolving the same
runtime class identity used by metadata and graph services.
Source scope:
OAClassLoader; OAClassUtil; serialization/deserialization consumers; remote/sync/replication boundaries.
Related CODEX findings:
class identity drift and wrong delegation can break serialized class compatibility.
Suggested unit tests:
classloaderSerializedClassResolvesWithExpectedLoader(), classloaderDeserializedObjectClassMatchesMetadataClass(),
classloaderRemotePayloadDoesNotResolveSystemClassWhenContextClassExpected().
Spec target section:
Classloader Runtime / Serialization Type Resolution Semantics.

CLASSLOAD-DYNAMIC-001 — Dynamic Model and Plugin Loading Lifecycle
Contract statement:
Dynamic, generated, plugin, module, or test model loading must have explicit lifecycle boundaries for source loader,
class identity, metadata publication, failure rollback, and cleanup.
Rationale:
OA builder/codegen and runtime extension workflows can load short-lived or regenerated classes; partial publication
creates metadata and graph identity drift.
Source scope:
OAClassLoader; ClassModifier; generated model discovery; OABuilder/codegen/tooling integration; metadata/runtime
publication boundaries.
Related CODEX findings:
target source loader and loader lifetime findings illustrate lifecycle risk.
Suggested unit tests:
classloaderGeneratedModelReloadDoesNotMixOldAndNewClassIdentity(),
classloaderFailedDynamicLoadDoesNotPublishMetadata(), classloaderPluginUnloadReleasesClassloaderReferences().
Spec target section:
Classloader Runtime / Dynamic Loading Lifecycle Semantics.

CLASSLOAD-REFLECT-001 — Reflection and Annotation Compatibility
Contract statement:
Classes loaded through com.viaoa.classloader must remain compatible with OA reflection and annotation discovery for
the same runtime class identity.
Rationale:
Metadata construction, path/query evaluation, property access, serialization, datasource mapping, and generated
runtime behavior depend on reflection and annotations of the loaded class.
Source scope:
ClassModifier extends OAReflect; OAClassLoader; OAClassUtil; integration with annotation, reflect, metadata, path,
query, datasource, and object packages.
Related CODEX findings:
loader mismatch can make annotations/reflection visible on one Class identity but not the runtime Class identity.
Suggested unit tests:
classloaderLoadedClassWorksWithOAReflect(), classloaderLoadedModelAnnotationsVisibleToRuntimeMetadata(),
classloaderClassModifierUsesLoadedClassIdentity().
Spec target section:
Classloader Runtime / Reflection and Annotation Semantics.

CLASSLOAD-PARTIAL-001 — Partial Discovery Visibility
Contract statement:
If class discovery or loading fails after some classes are loaded, the result must be reported as partial/incomplete
and must not be published as a complete generated model or runtime type set.
Rationale:
Partial type discovery can create missing metadata, broken paths, incomplete datasource mappings, and serialization
failures.
Source scope:
OAClassLoader; future discovery APIs; metadata/model/codegen integration boundaries.
Related CODEX findings:
resource/delegation failures can fail valid deployments; no structured discovery result currently exists.
Suggested unit tests:
classloaderPartialModelDiscoveryIsReportedIncomplete(),
classloaderFailedClassInModelSetPreventsCompletePublication(),
classloaderLoadedSubsetDoesNotClaimFullRuntimeValidity().
Spec target section:
Classloader Runtime / Partial Progress Semantics.

CLASSLOAD-BOUNDARY-001 — Class Load Success Versus OA Runtime Success
Contract statement:
Successful Java class loading only establishes that a Class was resolved; it must not imply successful OA metadata
construction, datasource mapping, serialization compatibility, graph registration, sync/replication compatibility,
or executable blueprint validity.
Rationale:
Class loading is a type-discovery boundary, while OA runtime correctness is established by consuming metadata/
runtime packages.
Source scope:
OAClassLoader; OAClassUtil; ClassModifier; integration boundaries with metadata, annotation, reflect, model, object,
graph, datasource, serialization, sync, replication, config, and tooling packages.
Related CODEX findings:
target source-loader, dependency delegation, duplicate definition, and package-name extraction issues illustrate
boundary risks.
Suggested unit tests:
classloaderLoadSuccessDoesNotImplyMetadataValidity(), classloaderLoadSuccessDoesNotImplyDatasourceMappingValidity(),
classloaderLoadSuccessDoesNotImplySerializationCompatibility().
Spec target section:
Classloader Runtime / Runtime Boundary Semantics.

CLASSLOAD-INTEGRATION-001 — Cross-Package Type Discovery Compatibility
Contract statement:
Classloader behavior must remain compatible with metadata, annotation discovery, reflection, model generation,
object/runtime graph routing, datasource mapping, serialization/deserialization, sync, replication, config, and
OABuilder/codegen contracts.
Rationale:
Classloading is a root dependency for many OA runtime truths; loader mismatch can make otherwise correct metadata
and object graph code fail.
Source scope:
OAClassLoader; OAClassUtil; ClassModifier; cross-package consumers in metadata, annotation, reflect, serialize,
runtime, datasource, model/codegen/tooling.
Related CODEX findings:
target source-loader, dependency delegation, duplicate definition, stream cleanup, and package-name extraction
issues illustrate this invariant.
Suggested unit tests:
classloaderLoadedModelClassAnnotationsVisibleToMetadata(),
classloaderLoadedClassSerializesAndDeserializesWithExpectedLoader(),
classloaderGeneratedModelLoadsForOABuilderAndRuntime().
Spec target section:
Classloader Runtime / Cross-Package Integration Semantics.

*/


