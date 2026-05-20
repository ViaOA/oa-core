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

com.viaoa.classloader Invariants

  ID: CLASSLOAD-RESOLVE-001
  Contract statement: Class resolution must return the intended Class instance for OA runtime, generated model,
  tooling, metadata, annotation, reflection, and serialization use.
  Rationale: OA metadata and OG behavior are keyed by Java Class identity. Resolving the wrong class or failing to
  resolve a valid generated/model class can break metadata, paths, annotations, datasource mapping, and serialization.
  Source locations: OAClassLoader.loadClass; OAClassUtil.getClassName; OAClassUtil.getPackageName; ClassModifier.
  Related CODEX findings: target class bytes are loaded only from the system classloader resource path.
  Suggested unit tests: testTargetClassLoadedFromConfiguredSourceLoader,
  testGeneratedModelClassResolvesToIntendedClass, testClassResolutionPreservesOAObjectTypeIdentity.
  Spec target section: Classloader Runtime / Class Resolution Semantics.

  ID: CLASSLOAD-RESOURCE-001
  Contract statement: Resource resolution must locate the intended class/resource using OA package/path naming
  semantics and the configured classloader boundary.
  Rationale: Generated model classes and tooling resources may live outside the system classpath. OA must resolve them
  consistently in app, test, builder, and generated-code environments.
  Source locations: OAClassLoader.loadClass, especially class-name-to-resource conversion; OAClassUtil.getPackageName.
  Related CODEX findings: target bytes are read with ClassLoader.getSystemResourceAsStream(...), bypassing context/
  application loader resources.
  Suggested unit tests: testClassResourceResolvedFromContextClassLoader,
  testClassNameConvertedToResourcePathCorrectly, testMissingClassResourceFailsVisibly.
  Spec target section: Classloader Runtime / Resource Resolution Semantics.

  ID: CLASSLOAD-IDENTITY-001
  Contract statement: Class identity must remain stable where OA metadata, annotations, reflection, serialization,
  datasource mapping, and object graph behavior depend on Class equality.
  Rationale: Loading the same logical model class through different loaders can create incompatible Class objects and
  split metadata/cache/serialization identity.
  Source locations: OAClassLoader.loadClass; metadata/annotation/reflect/serialize consumers.
  Related CODEX findings: dependency delegation and target-source loader issues can create or expose class identity
  drift.
  Suggested unit tests: testSameLoaderReturnsSameClassInstance, testMetadataBuiltForLoadedClassMatchesRuntimeClass,
  testSerializedClassIdentityCompatibleWithRuntimeLoader.
  Spec target section: Classloader Runtime / Class Identity Semantics.

  ID: CLASSLOAD-DELEGATE-001
  Contract statement: Parent-first or child-first delegation must be explicit and deterministic. Non-target
  dependencies must resolve through the intended parent/application/context loader, while target child-definition
  behavior must be clearly scoped.
  Rationale: OA-generated classes often depend on OA runtime/model/helper classes. Dependency resolution through the
  wrong loader can produce ClassNotFoundException or duplicate runtime type identities.
  Source locations: OAClassLoader.loadClass; ClassLoader delegation behavior.
  Related CODEX findings: non-target class resolution delegates only to findSystemClass(...), bypassing normal parent/
  context delegation.
  Suggested unit tests: testNonTargetClassDelegatesToParentLoader, testTargetClassUsesDocumentedChildFirstRule,
  testDependencyClassResolvesFromApplicationLoader.
  Spec target section: Classloader Runtime / Delegation Semantics.

  ID: CLASSLOAD-DUP-001
  Contract statement: Duplicate class definition in the same loader must be prevented or must fail visibly without
  corrupting loader state.
  Rationale: Duplicate definition creates LinkageError and can leave classloader state ambiguous under concurrent or
  retry use.
  Source locations: OAClassLoader.loadClass; clazz cache; defineClass.
  Related CODEX findings: concurrent loadClass calls can both see clazz == null and both call defineClass.
  Suggested unit tests: testConcurrentLoadClassDefinesTargetOnlyOnce, testSecondLoadReturnsCachedClass,
  testDuplicateDefineFailureDoesNotPoisonLoaderState.
  Spec target section: Classloader Runtime / Duplicate Definition Prevention.

  ID: CLASSLOAD-CACHE-001
  Contract statement: Loaded-class/cache state must represent only successfully defined/resolved classes. Failed or
  partial loads must not be retained as successful entries.
  Rationale: OA metadata/tooling consumers must be able to retry after a failed load and must not receive stale or
  half-initialized class state.
  Source locations: OAClassLoader.clazz; OAClassLoader.loadClass.
  Related CODEX findings: none beyond duplicate/concurrency risk; current clazz is assigned after defineClass success.
  Suggested unit tests: testFailedLoadDoesNotSetCachedClass, testSuccessfulLoadCachesClass,
  testRetryAfterMissingResourceCanSucceedWithNewLoaderOrSource.
  Spec target section: Classloader Runtime / Loaded-Class State Semantics.

  ID: CLASSLOAD-FAIL-001
  Contract statement: Failed class or resource loading must be visible to the caller and must not silently appear
  successful or fall back to the wrong class/resource.
  Rationale: Silent wrong class/resource resolution can corrupt metadata, annotations, reflection, serialization, and
  generated tooling output.
  Source locations: OAClassLoader.loadClass; OAClassUtil; converter/reflect integrations that load classes.
  Related CODEX findings: missing resource and IO failures are visible, but wrong loader-source and delegation
  behavior can fail valid OA deployments.
  Suggested unit tests: testMissingTargetClassThrowsClassNotFound, testIOExceptionDuringClassReadPreservesCause,
  testWrongLoaderFallbackDoesNotReturnDifferentClassSilently.
  Spec target section: Classloader Runtime / Failure Visibility.

  ID: CLASSLOAD-RETRY-001
  Contract statement: Retry after failed class/resource loading must not reuse corrupted loader state, leaked streams,
  partially read byte arrays, or stale cached results.
  Rationale: OA tooling/model loading may retry after generation, classpath, or deployment changes. Retry must either
  work cleanly or fail clearly.
  Source locations: OAClassLoader.loadClass; clazz cache; resource stream handling.
  Related CODEX findings: resource stream not closed on success/failure; duplicate definition race can leave retry
  behavior dependent on previous partial state.
  Suggested unit tests: testRetryAfterIOExceptionDoesNotReusePartialBytes,
  testRetryAfterClassNotFoundUsesFreshResourceLookup, testFailedConcurrentLoadDoesNotPoisonNextLoad.
  Spec target section: Classloader Runtime / Retry Semantics.

  ID: CLASSLOAD-RESOURCE-CLEANUP-001
  Contract statement: Resource streams opened during class/resource loading must be closed by the loader unless
  ownership is explicitly transferred to the caller.
  Rationale: Unclosed streams can retain jar/file handles, generated model artifacts, or classloader-related resources
  in long-running tooling or server processes.
  Source locations: OAClassLoader.loadClass, InputStream is = ClassLoader.getSystemResourceAsStream(...).
  Related CODEX findings: class resource stream is never closed.
  Suggested unit tests: testLoadClassClosesResourceStreamOnSuccess, testLoadClassClosesResourceStreamOnReadFailure,
  testRepeatedClassLoadDoesNotLeakResourceHandles.
  Spec target section: Classloader Runtime / Resource Cleanup.

  ID: CLASSLOAD-LEAK-001
  Contract statement: Classloader and loaded-class references must not be retained longer than intended where that
  would prevent unloading or leak generated/test model classes.
  Rationale: Generated model tooling, tests, and app reloads may create short-lived loaders. Long-lived references can
  prevent class unloading and retain metadata graphs.
  Source locations: OAClassLoader.clazz; consumers storing loaded classes/metadata; metadata/annotation caches.
  Related CODEX findings: none observed directly in package; risk belongs to cross-package loader/cache ownership.
  Suggested unit tests: testTemporaryClassLoaderCanBeGarbageCollectedAfterUse,
  testMetadataCacheClearReleasesGeneratedClassLoader, testClassloaderNotRetainedByFailedLoad.
  Spec target section: Classloader Runtime / Loader Lifetime Semantics.

  ID: CLASSLOAD-CONCURRENT-001
  Contract statement: Concurrent class/resource loading must not corrupt loaded-class caches, define the same class
  twice, expose stale reads, or return inconsistent class identities.
  Rationale: OA runtime/tooling may run metadata discovery or generated model loading concurrently. Classloading state
  must be thread-safe.
  Source locations: OAClassLoader.loadClass; clazz field; defineClass.
  Related CODEX findings: unsynchronized target loading can duplicate defineClass.
  Suggested unit tests: testConcurrentTargetLoadsReturnSameClass, testConcurrentTargetLoadDoesNotThrowLinkageError,
  testConcurrentDependencyLoadUsesStableDelegation.
  Spec target section: Classloader Runtime / Concurrency Semantics.

  ID: CLASSLOAD-PACKAGE-001
  Contract statement: Package-name extraction must return the actual package name or the defined no-package value,
  never the class name.
  Rationale: OA graph/package routing, generated reports, and resource lookup can depend on package strings. A bogus
  package name can route metadata/tooling to the wrong package root.
  Source locations: OAClassUtil.getPackageName.
  Related CODEX findings: default-package classes return their class name as package name.
  Suggested unit tests: testGetPackageNameForDefaultPackageClass, testGetPackageNameForPackagedClass,
  testGetPackageNameForNestedClassUsesDeclaringPackage.
  Spec target section: Classloader Runtime / Package Name Semantics.

  ID: CLASSLOAD-INTEGRATION-001
  Contract statement: Classloader behavior must remain compatible with metadata, annotation discovery, reflection,
  serialization/deserialization, runtime graph routing, OABuilder/codegen, datasource mapping, and unit-test model
  loading.
  Rationale: Classloading is a root dependency for many OA runtime truths. A loader mismatch can make otherwise
  correct metadata and object graph code fail.
  Source locations: OAClassLoader; OAClassUtil; ClassModifier; consumers in metadata, annotation, reflect, serialize,
  runtime, datasource, codegen/tooling.
  Related CODEX findings: target source-loader, dependency delegation, duplicate definition, and package-name
  extraction issues illustrate this invariant.
  Suggested unit tests: testLoadedModelClassAnnotationsVisibleToMetadata, testLoadedClassWorksWithOAReflect,
  testLoadedClassSerializesAndDeserializesWithExpectedLoader.
  Spec target section: Classloader Runtime / Cross-Package Integration.

  Suggested Package-Level Spec Summary

  - com.viaoa.classloader supports OA runtime/tooling class and resource loading, generated model discovery, class-
    name/package-name utilities, and class-level reflection extension points.
  - The package must resolve the intended Class and resource from the intended classloader boundary.
  - It must preserve stable class identity for OA metadata, annotations, reflection, object graph routing, datasource
    mapping, and serialization.
  - Delegation behavior must be explicit: parent/context/application dependencies should not accidentally fall back to
    only the system classloader.
  - Duplicate class definition must be prevented, especially under concurrent load.
  - Class/resource load failures must be visible and must not silently return the wrong class/resource.
  - Failed loads must leave retryable loader state.
  - Resource streams opened by the classloader must be closed unless ownership is explicitly transferred.
  - Loader/class references must not be retained longer than intended in generated model or test-tooling workflows.
  - Package/class-name utility methods must return semantically correct values for packaged, default-package, nested,
    and generated classes.

  Likely unit-test categories:

  - target class resolution from explicit/context/application loader
  - parent/dependency delegation behavior
  - duplicate definition and concurrent load tests
  - failed load and retry tests
  - resource stream cleanup tests
  - package/class-name extraction tests
  - metadata/annotation/reflection integration tests
  - serialization/deserialization class identity tests
  - generated model/OABuilder loading tests
  - classloader lifetime/leak tests


*/


