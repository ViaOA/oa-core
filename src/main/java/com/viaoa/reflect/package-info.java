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
 * Reflection utilities used throughout OA for runtime property access,
 * method discovery, path traversal, type inspection, conversion, and
 * metadata integration.
 *
 * <h2>Overview</h2>
 *
 * This package provides the reflection layer used by OA runtime services
 * to dynamically discover and invoke model properties and methods.
 * It is a foundational package used by object metadata, property paths,
 * filters, queries, serialization, graph traversal, bindings, and
 * generated model code.
 *
 * <h2>Primary Responsibilities</h2>
 * <ul>
 *   <li>Method discovery and invocation</li>
 *   <li>Property getter and setter resolution</li>
 *   <li>Dotted property-path traversal</li>
 *   <li>Hub navigation and active-object resolution</li>
 *   <li>Primitive and wrapper type handling</li>
 *   <li>Runtime type inspection utilities</li>
 *   <li>String-to-type conversion support</li>
 *   <li>Classpath and class discovery utilities</li>
 * </ul>
 *
 * <h2>Property Paths</h2>
 *
 * Reflection services support OA property paths such as:
 *
 * <pre>
 * employee.department.region.name
 * invoice.customer.lastName
 * store.registers.activeObject.invoice.total
 * </pre>
 *
 * Hub properties may be traversed using the active object when
 * appropriate for OA runtime navigation.
 *
 * <h2>OAObject Integration</h2>
 *
 * Reflection utilities understand OAObject semantics including:
 * <ul>
 *   <li>Primitive-null tracking</li>
 *   <li>Metadata-driven property access</li>
 *   <li>Hub link traversal</li>
 *   <li>Generated model property conventions</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * Public utilities in this package are intended to be stateless and
 * safely reusable across threads. Any cached reflection metadata must
 * remain deterministic and class-specific.
 *
 * <h2>Package Invariants</h2>
 *
 * <ul>
 *   <li>Reflection lookup must be deterministic for identical inputs.</li>
 *   <li>Property resolution must follow documented OA getter/setter conventions.</li>
 *   <li>Reflection must not silently invoke an incorrect member.</li>
 *   <li>Primitive-wrapper compatibility must follow documented OA semantics.</li>
 *   <li>Property-path traversal must preserve OAObject and Hub semantics.</li>
 *   <li>Invocation failures must preserve underlying causes.</li>
 *   <li>Reflection behavior must remain consistent with OA metadata and path services.</li>
 * </ul>
 *
 * @see com.viaoa.reflect.OAReflect
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.path
 * @see com.viaoa.filter
 */
package com.viaoa.reflect;

//CODEX unit tests 20260527

/* CODEX Invariants

REFLECT-LOOKUP-001 — Deterministic Member Lookup
Contract statement:
Given the same class, member name, argument types, strict/lenient mode, accessibility policy, and runtime classpath,
OA reflection must resolve the same method, property accessor, or no-match result every time.
Rationale:
Reflection is a runtime authority for OA paths, metadata interpretation, generated blueprint execution,
serialization, binding, query/path evaluation, filters, and graph traversal. Nondeterministic lookup invokes wrong
runtime behavior.
Source scope:
OAReflect.getMethod overloads, OAReflect.getMethods overloads, executeMethod, getPropertyValue, setPropertyValue.
Related CODEX findings:
OAReflect.getMethod overload ambiguity and Class.getMethods order concerns.
Suggested unit tests:
testMethodLookupFindsExactPublicMethod, testMethodLookupIsDeterministicAcrossCalls,
testOverloadResolutionIndependentOfMethodArrayOrder.
Spec target section:
Reflection Runtime / Member Lookup Semantics

REFLECT-NAME-001 — Member Name Matching Semantics
Contract statement:
Method and property name matching must follow a defined case-sensitivity and ambiguity rule. Reflection must not
silently choose a wrong method or property when names differ only by case or are similarly named.
Rationale:
Generated models and blueprints can contain related property names. Reflection-order-dependent name matching
corrupts path, binding, serializer, and metadata behavior.
Source scope:
OAReflect.getMethod, OAReflect.getMethods, property getter/setter resolution paths.
Related CODEX findings:
Case-insensitive ambiguity reviewed; wrong-member fallback risks noted.
Suggested unit tests:
testMethodLookupCaseSensitiveByContract, testAmbiguousCaseMethodLookupFailsOrUsesDefinedRule,
testSimilarPropertyNameNotUsedAsFallback.
Spec target section:
Reflection Runtime / Name Resolution

REFLECT-PROPERTY-001 — OA Property Accessor Rules
Contract statement:
Getter and setter discovery must follow OA property rules: getX, boolean isX where allowed, setter compatibility by
logical property name and value type, and only documented fallback forms.
Rationale:
OAObject paths, generated UI, serialization, filters, queries, and graph metadata depend on consistent Java member-
to-property interpretation.
Source scope:
OAReflect.getMethods, getPropertyValue, setPropertyValue, boolean getter/setter property derivation.
Related CODEX findings:
Boolean isX getters are not resolved for property paths; boolean setter name mismatch concerns.
Suggested unit tests:
testGetterDiscoveryFindsGetProperty, testBooleanIsGetterResolved, testBooleanGetGetterPrecedenceByContract,
testIsActiveMapsToSetActive.
Spec target section:
Reflection Runtime / Property Accessor Semantics

REFLECT-PROPERTY-002 — Missing Property Failure Boundary
Contract statement:
If a requested getter, setter, or property path segment is missing, reflection must return the documented no-member
result or throw through the strict API path. It must not substitute a similarly named property or unrelated method.
Rationale:
Silent wrong-property access corrupts UI, filters, serialization, generated code, and graph navigation.
Source scope:
OAReflect.getMethods, getPropertyValue, setPropertyValue, strict/lenient property lookup paths.
Related CODEX findings:
Empty path segments can be treated as toString; missing/wrong-property fallback risks reviewed.
Suggested unit tests:
testMissingGetterReturnsDefinedFailure, testStrictMissingPropertyThrows, testLenientMissingPropertyReturnsNull,
testMalformedPathSegmentDoesNotResolveToToString.
Spec target section:
Reflection Runtime / Property Failure Semantics

REFLECT-METHOD-001 — Exact Match Preference
Contract statement:
When overloaded methods exist, exact parameter type matches must be preferred over primitive-wrapper compatible,
assignable, superclass/interface, or generic Object matches.
Rationale:
Specific overloads encode intended domain behavior. Invoking a broader overload changes executable blueprint
semantics.
Source scope:
OAReflect.getMethod overloads, argument matching logic, isEqualEvenIfWrapper.
Related CODEX findings:
Argument matching and overload selection issues in getMethod(Class,String,int,Object[]).
Suggested unit tests:
testExactOverloadBeatsObjectOverload, testExactPrimitiveWrapperOverloadByContract,
testExactInterfaceImplementationOverloadByContract.
Spec target section:
Reflection Runtime / Overload Resolution

REFLECT-METHOD-002 — Ambiguous Overload Handling
Contract statement:
If multiple overloads are equally valid for the provided arguments, reflection must use a documented tie-breaker or
fail visibly; it must not rely on Class.getMethods ordering.
Rationale:
JVM method order is not a semantic contract. Arbitrary overload choice can invoke wrong business logic or mutate
wrong state.
Source scope:
OAReflect.getMethod overloads, null argument matching, assignable argument matching.
Related CODEX findings:
Null arguments resolve to first case-insensitive method returned by Class.getMethods.
Suggested unit tests:
testAmbiguousOverloadFailsOrUsesDefinedRule, testNullArgumentAmbiguousOverloadFailsOrUsesDefinedRule,
testOverloadResolutionIndependentOfMethodArrayOrder.
Spec target section:
Reflection Runtime / Ambiguous Overloads

REFLECT-ASSIGN-001 — Primitive Wrapper Compatibility
Contract statement:
Reflection assignability must treat primitive types and their wrapper classes as compatible where Java invocation
would allow boxing/unboxing, and must reject null for primitive parameters.
Rationale:
Generated code and property setters commonly pass wrapper values for primitive properties. Null-to-primitive
matching causes invocation failure or wrong overload selection.
Source scope:
OAReflect.getMethod overloads, OAReflect.isEqualEvenIfWrapper, OAReflect.setPropertyValue.
Related CODEX findings:
Argument matching requires exact runtime class equality; primitive/wrapper compatibility reviewed.
Suggested unit tests:
testIntegerMatchesIntParameter, testBooleanMatchesBooleanPrimitiveParameter,
testNullArgumentDoesNotMatchIntParameter, testNullArgumentMatchesReferenceParameter.
Spec target section:
Reflection Runtime / Primitive Wrapper Assignability

REFLECT-ASSIGN-002 — Assignable Type Semantics
Contract statement:
Reflection method matching must support interface and superclass assignability where Java invocation supports it,
without confusing assignability with conversion.
Rationale:
OA models and runtime services often expose methods through interfaces or base classes. Reflection should invoke
compatible callable members without inventing type conversions.
Source scope:
OAReflect.getMethod overloads, isEqualEvenIfWrapper, type matching logic.
Related CODEX findings:
getMethod exact runtime class equality rejects interface/superclass-compatible arguments.
Suggested unit tests:
testStringMatchesCharSequenceParameter, testSubclassMatchesSuperclassParameter,
testInterfaceArgumentMatchesInterfaceParameter.
Spec target section:
Reflection Runtime / Assignability Semantics

REFLECT-CONVERT-001 — Conversion Boundary Separation
Contract statement:
Reflection matching must not silently perform semantic type conversion unless the API explicitly calls OA converter
logic. Conversion from strings or other external values must remain a documented boundary.
Rationale:
Reflection selects callable members; converters own semantic coercion. Blurring the boundary can choose lossy
setters or hide invalid input.
Source scope:
OAReflect.convertParameterFromString, OAReflect.setPropertyValue(String), OAReflect.setPropertyValue(Object),
OAConverter integration.
Related CODEX findings:
Numeric assignability treats Number subclasses as compatible; setter/type conversion risks reviewed.
Suggested unit tests:
testLongDoesNotSilentlyMatchIntWhenLossyByContract, testStringParameterConversionUsesOAConverter,
testReflectionOnlyLookupDoesNotConvertStringToNumber.
Spec target section:
Reflection Runtime / Conversion Boundaries

REFLECT-NUMERIC-001 — Numeric Matching Must Not Invent Lossy Compatibility
Contract statement:
Numeric wrappers and numeric primitives must be matched only when Java invocation or documented OA conversion
supports the assignment; broad Number-family compatibility must not select a method that Method.invoke will reject
or that loses precision silently.
Rationale:
Wrong numeric matching can corrupt model values or fail after lookup has reported success.
Source scope:
OAReflect.getMethod(Class,String,Class), OAReflect.isEqualEvenIfWrapper, setPropertyValue.
Related CODEX findings:
All Number subclasses are treated as parameter-compatible in isEqualEvenIfWrapper.
Suggested unit tests:
testLongDoesNotMatchIntegerParameterWithoutConversion, testBigDecimalDoesNotMatchDoubleParameterWithoutConversion,
testDocumentedNumericConversionUsesConverterBoundary.
Spec target section:
Reflection Runtime / Numeric Assignability

REFLECT-BOOLEAN-001 — Boolean Property Semantics
Contract statement:
Boolean properties must define whether isX, getX, or both are valid getters, which takes precedence, and which
setter name represents the same logical property.
Rationale:
Boolean properties are common in generated models, path expressions, UI binding, filters, and serialization. Naming
drift breaks live graph semantics.
Source scope:
OAReflect.getMethods, getter lookup, setter lookup, property name derivation for boolean methods.
Related CODEX findings:
Boolean isX getters are not resolved for property paths; boolean isX setter mismatch reviewed.
Suggested unit tests:
testBooleanIsGetterResolved, testBooleanGetGetterPrecedenceByContract, testIsActiveMapsToSetActive,
testBooleanPropertyNamedIsActiveByContract.
Spec target section:
Reflection Runtime / Boolean Property Semantics

REFLECT-HIERARCHY-001 — Class Hierarchy And Interface Lookup
Contract statement:
Method and property lookup must include inherited public members, interface-declared members, and subclass overrides
according to Java/OA rules, preferring the actual runtime class implementation.
Rationale:
OA generated models and services use base classes, interfaces, and overrides. Runtime behavior must honor domain-
specific subclass implementations.
Source scope:
OAReflect.getMethod, OAReflect.getMethods, executeMethod, getPropertyValue, setPropertyValue.
Related CODEX findings:
Hierarchy and interface lookup behavior reviewed.
Suggested unit tests:
testInheritedGetterResolved, testInheritedMethodResolved, testInterfaceGetterResolvedFromImplementation,
testSubclassOverrideInvoked.
Spec target section:
Reflection Runtime / Hierarchy Semantics

REFLECT-ACCESS-001 — Accessibility Policy
Contract statement:
Reflection helpers must define whether they use only public members or may access non-public members. Access policy
must be applied consistently during lookup and invocation.
Rationale:
OA generated/domain models need predictable access rules. Accidental private access or accidental exclusion changes
runtime semantics.
Source scope:
OAReflect.getMethod, getMethods, executeMethod, getPropertyValue, setPropertyValue.
Related CODEX findings:
Accessibility behavior reviewed.
Suggested unit tests:
testPrivateMethodNotInvokedByPublicOnlyContract, testAccessibleNonPublicMethodInvokedWhenAllowed,
testAccessPolicyIsConsistentForLookupAndInvocation.
Spec target section:
Reflection Runtime / Accessibility

REFLECT-STATIC-001 — Static And Instance Boundaries
Contract statement:
Static method lookup/invocation and instance method lookup/invocation must be distinct. Instance property/path
access must not fall back to static methods unless explicitly allowed by the API contract.
Rationale:
OA property and graph traversal normally operate on object instances. Static fallback can return global or unrelated
values.
Source scope:
OAReflect.getMethod, executeMethod, getPropertyValue, getMethods.
Related CODEX findings:
Static/instance handling reviewed.
Suggested unit tests:
testInstanceLookupDoesNotReturnStaticMethodUnlessAllowed, testStaticLookupInvokesStaticMethodByContract,
testPathPropertyDoesNotUseStaticFallback.
Spec target section:
Reflection Runtime / Static Instance Semantics

REFLECT-INVOKE-001 — Invocation Completion Truth
Contract statement:
A reflected get, set, or method invocation must be reported as successful only after the intended callable completed
successfully. Failed invocation must not be treated as successful property access or mutation.
Rationale:
Reflection can read or mutate OAObject state, fire events, compute calculated values, or drive serialization. False-
success invocation corrupts runtime state.
Source scope:
OAReflect.executeMethod, getPropertyValue, getPropertyValueAsString, setPropertyValue.
Related CODEX findings:
Getter/setter exception swallowing and invocation completion risks reviewed.
Suggested unit tests:
testGetterExceptionDoesNotReturnStaleValue, testSetterExceptionDoesNotClaimSuccess,
testExecuteMethodExceptionDoesNotReportSuccess.
Spec target section:
Reflection Runtime / Invocation Completion

REFLECT-INVOKE-002 — Invocation Failure Preserves Cause
Contract statement:
If reflected invocation fails, thrown or reported errors must preserve enough cause and member context for callers
to diagnose the underlying failure.
Rationale:
Path, metadata, serializer, binding, and generated-code failures need traceable diagnostics. Losing cause hides
runtime correctness issues.
Source scope:
OAReflect.executeMethod, getPropertyValue, getPropertyValueAsString, setPropertyValue.
Related CODEX findings:
Exception wrapping/context loss reviewed.
Suggested unit tests:
testInvocationExceptionPreservesCause, testSetterExceptionPreservesCause, testGetterExceptionIncludesMemberContext.
Spec target section:
Reflection Runtime / Invocation Failure

REFLECT-PRIMITIVE-001 — Primitive Null Semantics
Contract statement:
OA primitive-null behavior must be applied before invoking primitive getters when the contract requires null-
preserving reads, and null assignment to primitive OAObject properties must follow the documented setter/setNull/
event semantics.
Rationale:
OA tracks primitive null separately from Java primitive defaults. Reflection must not trigger side effects or bypass
lifecycle semantics unexpectedly.
Source scope:
OAReflect.getPropertyValue, OAReflect.setPropertyValue, OAObject primitive-null integration.
Related CODEX findings:
Primitive OAObject getter invoked before checking primitive-null flag; null assigned to primitive OAObject property
calls setNull without invoking setter.
Suggested unit tests:
testPrimitiveNullGetterDoesNotInvokeGetterWhenContractRequiresNull,
testPrimitiveNullAssignmentFollowsSetterOrSetNullContract, testPrimitiveNullPropertyNameDerivedOnlyForSetterMethods.
Spec target section:
Reflection Runtime / Primitive Null Semantics

REFLECT-PRIMITIVE-002 — Empty Primitive Defaults
Contract statement:
Empty primitive/default value synthesis must match Java primitive defaults and documented wrapper behavior;
unsupported wrapper defaults must not be silently promised and then return null.
Rationale:
OAObject, remote, and dynamic invocation paths can synthesize primitive defaults on failure/no-response paths. Wrong
defaults can report false runtime state.
Source scope:
OAReflect.getEmptyPrimitive.
Related CODEX findings:
Boolean primitive default is true; documentation says wrapper classes are supported but implementation handles only
primitives.
Suggested unit tests:
testEmptyPrimitiveBooleanIsFalse, testEmptyPrimitiveNumericDefaultsMatchJava,
testWrapperDefaultBehaviorMatchesDocumentedContract.
Spec target section:
Reflection Runtime / Primitive Default Semantics

REFLECT-CACHE-001 — Reflection Metadata Cache Identity
Contract statement:
Cached reflection results must be keyed by the correct class, member name, argument signature, access policy, and
lookup mode. Cached members from one class or signature must not be reused for incompatible classes or calls.
Rationale:
Reflection metadata is shared infrastructure. Wrong cache identity invokes wrong methods or reads/writes wrong
properties.
Source scope:
Reflection caches if present, OAReflect method/property lookup results, cached Method[] path arrays consumed by
path/binding/filter code.
Related CODEX findings:
Cached method array correctness and class-specific cache behavior reviewed.
Suggested unit tests:
testCachedGetterSpecificToClass, testCachedMethodSpecificToParameterTypes,
testCachedPropertyPathDoesNotCrossClassBoundary.
Spec target section:
Reflection Runtime / Cache Identity

REFLECT-CACHE-002 — Cached Metadata Reuse Safety
Contract statement:
Discovered Method, Method[], property, and classpath metadata returned for reuse must be immutable by convention or
safely copied/owned so caller mutation cannot corrupt later reflection behavior.
Rationale:
Paths, filters, serializers, bindings, and generated code can reuse reflection metadata across runtime operations
and threads.
Source scope:
OAReflect.getMethods, getMethod, cached lookup results, class scanning results.
Related CODEX findings:
Cache/reuse behavior reviewed.
Suggested unit tests:
testCachedMethodReuseStableAcrossCalls, testCachedPropertyPathReuseThreadSafeByContract,
testCallerMutationDoesNotCorruptCachedMethodArrayIfContractRequiresCopy.
Spec target section:
Reflection Runtime / Cache Reuse

REFLECT-THREAD-001 — Shared Reflection Thread Safety
Contract statement:
Reflection helper state and cached metadata must be immutable, safely published, synchronized, or method-local when
shared across threads.
Rationale:
Reflection is used by graph runtime, Hubs, bindings, serializers, queries, filters, and background operations. Races
in metadata lookup can invoke wrong methods or expose partial state.
Source scope:
OAReflect static helpers, reflection caches if present, cached Method[]/class scan results.
Related CODEX findings:
Cached/reuse thread-safety behavior reviewed.
Suggested unit tests:
testConcurrentMethodLookupIsStable, testConcurrentPropertyLookupDoesNotCorruptCache,
testConcurrentCachedMethodReuseStable.
Spec target section:
Reflection Runtime / Thread Safety

REFLECT-CLASS-001 — Classpath And Class Discovery Semantics
Contract statement:
Class and OAObject class discovery must return deterministic, de-duplicated logical class names for the requested
package scope, and classpath lookup failures must fail visibly or return documented fallback values.
Rationale:
Metadata building, code generation, model discovery, and runtime graph setup depend on stable class discovery.
Source scope:
OAReflect.getClasses, OAReflect.getOAObjectClasses, OAReflect.getClassPath.
Related CODEX findings:
getClassPath dereferences null resource; getOAObjectClasses can return duplicate class names from multiple classpath
roots.
Suggested unit tests:
testGetClassPathHandlesMissingResourceByContract, testGetOAObjectClassesDeduplicatesLogicalClassNames,
testGetClassesReturnsDeterministicPackageResults.
Spec target section:
Reflection Runtime / Class Discovery

REFLECT-MODE-001 — Strict And Lenient Lookup Modes
Contract statement:
Reflection APIs that support strict or lenient behavior must honor that mode consistently: strict lookup fails
visibly, while lenient lookup returns only documented no-match values.
Rationale:
Callers choose strict validation or optional lookup depending on runtime context. Mode drift hides errors or raises
unexpected failures.
Source scope:
OAReflect.getMethods(Class,String,boolean), getMethods(Class,String,Class,boolean), getMethod overloads where no-
match behavior is defined.
Related CODEX findings:
Invalid path/property behavior and strict/lenient missing member behavior reviewed.
Suggested unit tests:
testStrictMissingMethodThrows, testLenientMissingMethodReturnsNull, testStrictMissingPropertyThrows,
testLenientMissingPropertyReturnsNull.
Spec target section:
Reflection Runtime / Lookup Mode Semantics

REFLECT-FAIL-001 — No Silent Wrong Invocation
Contract statement:
If the intended method, property, constructor, or class cannot be resolved unambiguously, OA reflection must fail
visibly or return a defined no-match result rather than invoking a plausible but wrong member.
Rationale:
Wrong invocation can mutate object state, trigger events, read wrong data, publish wrong serializer output, or
corrupt generated runtime behavior.
Source scope:
OAReflect.getMethod, getMethods, executeMethod, getPropertyValue, setPropertyValue, class discovery helpers.
Related CODEX findings:
False-success wrong-method behavior, empty path toString fallback, and similar setter name risks reviewed.
Suggested unit tests:
testAmbiguousMethodDoesNotInvokeWrongMethod, testSimilarSetterNameNotInvokedAsFallback,
testMalformedPropertyPathDoesNotInvokeToString, testWrongMethodFailureModeByContract.
Spec target section:
Reflection Runtime / False-Success Prevention

REFLECT-STATE-001 — No Partial Reflection Commit
Contract statement:
Failed lookup, conversion, invocation, classpath discovery, or property mutation must not leave caller-visible state
partially committed as if reflection succeeded.
Rationale:
Reflection may mutate OAObjects, derived state, primitive-null flags, and event-facing properties. Partial-progress
false success breaks lifecycle and graph consistency.
Source scope:
OAReflect.setPropertyValue, convertParameterFromString, executeMethod, getPropertyValue, getClassPath/
getOAObjectClasses failure paths.
Related CODEX findings:
Primitive-null setter bypass and failed invocation completion concerns.
Suggested unit tests:
testSetterConversionFailureDoesNotMutateProperty, testFailedSetterInvocationDoesNotClaimSuccess,
testFailedClassDiscoveryDoesNotReturnPartialSuccessAsComplete.
Spec target section:
Reflection Runtime / Partial Progress Semantics

REFLECT-INTEGRATION-001 — Metadata And Path Integration Contract
Contract statement:
Reflection behavior used by OAObjectInfo, OAPath, queries, filters, bindings, serializers, graph services, and
generated blueprints must preserve the same property/method semantics those packages expose as runtime contracts.
Rationale:
Reflection is the executable bridge between Java model classes and OA graph metadata. Drift causes path/query/
binding/serialization mismatch.
Source scope:
OAReflect.getMethods, getMethod, getPropertyValue, setPropertyValue, convertParameterFromString, integration with
path/object/metadata/filter/query/serialize packages.
Related CODEX findings:
Boolean getter path failure, empty path segment behavior, primitive-null getter/setter behavior, and assignability
issues all illustrate boundary risks.
Suggested unit tests:
testOAPathReflectionGetterResolutionMatchesOAReflect, testMetadataPropertyAccessorMatchesReflectionLookup,
testSerializerAndPathUseSameGetterSemantics, testQueryPathReflectionMatchesMetadataProperty.
Spec target section:
Reflection Runtime / Cross-Package Metadata Contracts

REFLECT-DETERMINISM-001 — Same Inputs Produce Same Reflection Result
Contract statement:
For the same class, member request, arguments, metadata assumptions, access policy, strict/lenient mode, and runtime
classpath, OA reflection must produce the same member, same value, same mutation, or same visible failure.
Rationale:
OA reflection is an AI-readable and runtime-readable semantic contract over executable blueprints. Determinism is
required for digital twin runtime behavior, generated code, graph services, and tests.
Source scope:
All public behavior in OAReflect.
Related CODEX findings:
Method order ambiguity, classpath duplicate discovery, default-locale/case ambiguity, and broad numeric matching can
threaten deterministic reflection behavior.
Suggested unit tests:
testSameMethodLookupSameResultRepeatedly, testSamePropertyLookupSameResultRepeatedly,
testSameInvalidLookupFailsConsistently, testSameClassDiscoveryReturnsStableResults.
Spec target section:
Reflection Runtime / Determinism

*/

