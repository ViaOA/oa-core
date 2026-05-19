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
 * 
  * </p>
 */
package com.viaoa.reflect;


/* CODEX Invariants

1. Method Lookup Contracts

  REFLECT-METHOD-001 — Method Lookup Must Resolve The Intended Method Deterministically
  Contract statement: Given a class, method name, and argument types, OA reflection must select the same intended
  method every time.
  Rationale: OA path traversal, property access, generated code, filters, and UI binding depend on stable method
  resolution.
  Source locations: reflection utilities in com.viaoa.reflect.*, method lookup helpers.
  Known related CODEX findings: method lookup ambiguity was reviewed.
  Suggested unit tests: testMethodLookupFindsExactPublicMethod(), testMethodLookupIsDeterministicAcrossCalls()
  Spec target section: Reflection Utilities / Method Resolution Semantics

  REFLECT-METHOD-002 — Method Name Matching Must Have Defined Case Semantics
  Contract statement: Method lookup must clearly define whether names are case-sensitive or case-insensitive and
  must not silently choose the wrong method when names differ only by case.
  Rationale: Reflection order can vary; ambiguous case matching can invoke wrong behavior.
  Source locations: method lookup/name comparison helpers.
  Known related CODEX findings: case-insensitive ambiguity reviewed.
  Suggested unit tests: testMethodLookupCaseSensitiveByContract(),
  testAmbiguousCaseMethodLookupFailsOrUsesDefinedRule()
  Spec target section: Reflection Utilities / Method Name Semantics

  REFLECT-METHOD-003 — Static And Instance Method Lookup Must Be Distinct
  Contract statement: Reflection helpers must distinguish static method invocation from instance method invocation
  and must not invoke a static method as an accidental fallback for an instance path.
  Rationale: OA property/path semantics usually operate on instances; accidental static resolution causes wrong
  values.
  Source locations: invocation helpers, method lookup helpers.
  Known related CODEX findings: static/instance handling reviewed.
  Suggested unit tests: testInstanceLookupDoesNotReturnStaticMethodUnlessAllowed(),
  testStaticLookupInvokesStaticMethodByContract()
  Spec target section: Reflection Utilities / Static Instance Semantics

  2. Property Getter / Setter Contracts

  REFLECT-PROPERTY-001 — Property Getter Discovery Follows OA Property Rules
  Contract statement: A property getter must be discovered using OA getter rules: getX, boolean isX where allowed,
  and any documented fallback forms.
  Rationale: OAObject property paths, UI binding, serialization, and compare/filter utilities depend on getter
  discovery.
  Source locations: property getter lookup helpers, OA property reflection classes.
  Known related CODEX findings: getter fallback behavior reviewed.
  Suggested unit tests: testGetterDiscoveryFindsGetProperty(), testGetterDiscoveryUsesDocumentedFallbackOnly()
  Spec target section: Reflection Utilities / Getter Discovery

  REFLECT-PROPERTY-002 — Property Setter Discovery Must Match Getter Property Type
  Contract statement: A property setter must be selected based on the intended property name and compatible value
  type.
  Rationale: Wrong setter invocation can corrupt object state.
  Source locations: setter lookup helpers, property set invocation helpers.
  Known related CODEX findings: setter/type conversion issues reviewed.
  Suggested unit tests: testSetterDiscoveryFindsMatchingSetter(), testSetterDiscoveryRejectsIncompatibleSetter()
  Spec target section: Reflection Utilities / Setter Discovery

  REFLECT-PROPERTY-003 — Missing Getter/Setter Must Not Resolve To Wrong Property
  Contract statement: If a requested property getter/setter is missing, reflection must fail or return the defined
  no-member result; it must not choose a similarly named property.
  Rationale: Silent wrong-property access corrupts UI, filters, serialization, and generated code.
  Source locations: property lookup helpers.
  Known related CODEX findings: false-success wrong-member risks reviewed.
  Suggested unit tests: testMissingGetterReturnsDefinedFailure(), testSimilarPropertyNameNotUsedAsFallback()
  Spec target section: Reflection Utilities / Property Failure Semantics

  3. Primitive / Wrapper Assignability Contracts

  REFLECT-ASSIGN-001 — Primitive And Wrapper Types Are Compatible By OA Rules
  Contract statement: Reflection assignability must treat primitive types and their wrapper classes as compatible
  where Java invocation would allow boxing/unboxing.
  Rationale: Generated code and property setting frequently pass wrapper values for primitive setters.
  Source locations: type assignability helpers, setter/method matching helpers.
  Known related CODEX findings: primitive/wrapper compatibility reviewed.
  Suggested unit tests: testIntegerMatchesIntParameter(), testBooleanMatchesBooleanPrimitiveParameter()
  Spec target section: Reflection Utilities / Primitive Wrapper Assignability

  REFLECT-ASSIGN-002 — Numeric Assignability Must Not Silently Lose Unsupported Precision
  Contract statement: Numeric type conversion/matching must follow defined OA conversion rules and must not silently
  select a lossy method/setter unless explicitly allowed.
  Rationale: Wrong numeric coercion can corrupt model values.
  Source locations: assignability helpers, conversion helpers.
  Known related CODEX findings: numeric wrapper/type classification risks reviewed.
  Suggested unit tests: testLongDoesNotSilentlyMatchIntWhenLossyByContract(), testBigDecimalConversionByContract()
  Spec target section: Reflection Utilities / Numeric Assignability

  4. Overload Resolution Contracts

  REFLECT-OVERLOAD-001 — Exact Match Beats Assignable Match
  Contract statement: When overloaded methods exist, exact parameter type matches must be preferred over broader
  assignable matches.
  Rationale: Prevents invoking a generic overload when a specific overload exists.
  Source locations: overloaded method lookup helpers.
  Known related CODEX findings: overloaded method selection order reviewed.
  Suggested unit tests: testExactOverloadBeatsObjectOverload(), testExactPrimitiveWrapperOverloadByContract()
  Spec target section: Reflection Utilities / Overload Resolution

  REFLECT-OVERLOAD-002 — Ambiguous Overloads Must Not Be Chosen Arbitrarily
  Contract statement: If multiple overloads are equally valid, reflection must use a documented tie-breaker or fail
  visibly.
  Rationale: Class.getMethods() order is not a safe semantic rule.
  Source locations: method lookup helpers.
  Known related CODEX findings: reflection-order ambiguity reviewed.
  Suggested unit tests: testAmbiguousOverloadFailsOrUsesDefinedRule(),
  testOverloadResolutionIndependentOfMethodArrayOrder()
  Spec target section: Reflection Utilities / Ambiguous Overloads

  5. Boolean Property Contracts

  REFLECT-BOOLEAN-001 — Boolean Getter Semantics Are Explicit
  Contract statement: Boolean properties must define whether isX, getX, or both are valid, and which takes
  precedence.
  Rationale: OA paths and generated UI often bind boolean properties.
  Source locations: getter discovery helpers.
  Known related CODEX findings: boolean getter naming reviewed.
  Suggested unit tests: testBooleanIsGetterResolved(), testBooleanGetGetterPrecedenceByContract()
  Spec target section: Reflection Utilities / Boolean Properties

  REFLECT-BOOLEAN-002 — Boolean Setter Matches Boolean Property Name
  Contract statement: A boolean setter must match the logical property name, not accidentally include the is prefix
  unless that is the actual property.
  Rationale: isActive getter should normally map to setActive, not the wrong setter.
  Source locations: setter discovery/property name derivation helpers.
  Known related CODEX findings: boolean isX setter mismatch reviewed.
  Suggested unit tests: testIsActiveMapsToSetActive(), testBooleanPropertyNamedIsActiveByContract()
  Spec target section: Reflection Utilities / Boolean Setter Semantics

  6. Class Hierarchy / Interface Traversal Contracts

  REFLECT-HIERARCHY-001 — Lookup Includes Inherited Public Members
  Contract statement: Method/property lookup must include inherited public methods and properties according to Java/
  OA rules.
  Rationale: OA models often use base classes and interfaces.
  Source locations: method/property lookup helpers.
  Known related CODEX findings: hierarchy traversal reviewed.
  Suggested unit tests: testInheritedGetterResolved(), testInheritedMethodResolved()
  Spec target section: Reflection Utilities / Hierarchy Lookup

  REFLECT-HIERARCHY-002 — Interface Methods Are Valid Lookup Targets
  Contract statement: Interface-declared methods must be discoverable when the target class implements the
  interface.
  Rationale: OA contracts and generated model interfaces may define behavior via interfaces.
  Source locations: method lookup helpers.
  Known related CODEX findings: interface lookup behavior reviewed.
  Suggested unit tests: testInterfaceGetterResolvedFromImplementation(),
  testInterfaceMethodInvokedOnImplementation()
  Spec target section: Reflection Utilities / Interface Lookup

  REFLECT-HIERARCHY-003 — Subclass Overrides Must Be Preferred Over Superclass Methods
  Contract statement: When a subclass overrides a method/property, reflection must invoke the subclass
  implementation.
  Rationale: Domain-specific overrides must be honored.
  Source locations: method lookup/invocation helpers.
  Known related CODEX findings: none observed.
  Suggested unit tests: testSubclassOverrideInvoked(), testSuperclassFallbackOnlyWhenNoOverride()
  Spec target section: Reflection Utilities / Override Semantics

  7. Accessibility / Invocation Contracts

  REFLECT-ACCESS-001 — Accessibility Behavior Must Be Defined
  Contract statement: Reflection helpers must define whether they use only public members or may access non-public
  members.
  Rationale: OA generated/domain models need predictable access rules.
  Source locations: method/property lookup and invocation helpers.
  Known related CODEX findings: accessibility behavior reviewed.
  Suggested unit tests: testPrivateMethodNotInvokedByPublicOnlyContract(),
  testAccessibleNonPublicMethodInvokedWhenAllowed()
  Spec target section: Reflection Utilities / Accessibility

  REFLECT-INVOKE-001 — Invocation Exceptions Must Preserve Cause
  Contract statement: If reflected invocation fails, the thrown error must preserve the underlying cause enough for
  callers to diagnose failure.
  Rationale: OA path/property failures must be traceable.
  Source locations: invocation wrappers.
  Known related CODEX findings: exception wrapping/context loss reviewed.
  Suggested unit tests: testInvocationExceptionPreservesCause(), testSetterExceptionPreservesCause()
  Spec target section: Reflection Utilities / Invocation Failure

  8. Null Argument / Type Matching Contracts

  REFLECT-NULL-001 — Null Arguments Match Only Reference-Type Parameters
  Contract statement: A null argument may match non-primitive parameters but must not match primitive parameters.
  Rationale: Invoking primitive parameter methods with null causes runtime failure or wrong overload selection.
  Source locations: method lookup argument matching helpers.
  Known related CODEX findings: null argument ambiguity reviewed.
  Suggested unit tests: testNullArgumentMatchesStringParameter(), testNullArgumentDoesNotMatchIntParameter()
  Spec target section: Reflection Utilities / Null Argument Matching

  REFLECT-NULL-002 — Null Argument Overload Resolution Must Be Deterministic
  Contract statement: If null can match multiple reference overloads, reflection must use a documented rule or fail
  visibly.
  Rationale: Arbitrary overload selection can invoke wrong business logic.
  Source locations: overload resolution helpers.
  Known related CODEX findings: null overload ambiguity reviewed.
  Suggested unit tests: testNullArgumentAmbiguousOverloadFailsOrUsesDefinedRule(),
  testNullArgumentSpecificTypeHintSelectsExpectedOverload()
  Spec target section: Reflection Utilities / Null Overload Resolution

  9. Cache / Reuse Contracts

  REFLECT-CACHE-001 — Cached Reflection Results Are Class-Specific
  Contract statement: Cached methods/properties must be keyed by the correct class and lookup signature.
  Rationale: Reusing cached methods across incompatible classes invokes wrong methods.
  Source locations: reflection caches, method/property lookup caches.
  Known related CODEX findings: cached method array correctness reviewed.
  Suggested unit tests: testCachedGetterSpecificToClass(), testCachedMethodSpecificToParameterTypes()
  Spec target section: Reflection Utilities / Reflection Cache

  REFLECT-CACHE-002 — Cached Reflection Results Must Be Immutable Or Safe To Reuse
  Contract statement: Cached method/property lookup results must not be mutated by callers in a way that changes
  later behavior.
  Rationale: Reflection helpers are shared infrastructure used by paths, filters, compare, serialization, and UI
  binding.
  Source locations: cache storage/accessors.
  Known related CODEX findings: cache/reuse behavior reviewed.
  Suggested unit tests: testCachedMethodReuseStableAcrossCalls(), testCachedPropertyPathReuseThreadSafeByContract()
  Spec target section: Reflection Utilities / Cache Reuse

  10. Error / Missing Member Contracts

  REFLECT-ERROR-001 — Missing Method Behavior Must Match Strict/Lenient API Contract
  Contract statement: Strict APIs must throw clearly for missing methods; lenient APIs must return a defined null/
  false result.
  Rationale: Different OA callers need strict model validation or permissive optional lookup.
  Source locations: method lookup helpers with throw/no-throw options.
  Known related CODEX findings: invalid path/property behavior reviewed.
  Suggested unit tests: testStrictMissingMethodThrows(), testLenientMissingMethodReturnsNull()
  Spec target section: Reflection Utilities / Missing Method Semantics

  REFLECT-ERROR-002 — Missing Property Behavior Must Match Strict/Lenient API Contract
  Contract statement: Missing property lookup must not silently resolve to another property; it must follow strict/
  lenient behavior.
  Rationale: Prevents silent wrong-path/wrong-property behavior.
  Source locations: property lookup helpers.
  Known related CODEX findings: wrong-property fallback risks reviewed.
  Suggested unit tests: testStrictMissingPropertyThrows(), testLenientMissingPropertyReturnsNull()
  Spec target section: Reflection Utilities / Missing Property Semantics

  11. Failure / Silent Wrong-Method Contracts

  REFLECT-FAILURE-001 — Reflection Must Prefer Visible Failure Over Wrong Invocation
  Contract statement: If the intended member cannot be resolved unambiguously, reflection must fail or return a
  defined no-match result rather than invoke a plausible but wrong member.
  Rationale: Wrong invocation can mutate data, trigger events, or return wrong UI/filter values.
  Source locations: method/property lookup, invocation helpers.
  Known related CODEX findings: false-success wrong-method behavior reviewed.
  Suggested unit tests: testAmbiguousMethodDoesNotInvokeWrongMethod(), testSimilarSetterNameNotInvokedAsFallback()
  Spec target section: Reflection Utilities / Silent Wrong-Method Prevention

  REFLECT-FAILURE-002 — Failed Invocation Must Not Be Treated As Successful Property Set/Get
  Contract statement: If a reflected get/set invocation throws, callers must not treat the operation as completed
  successfully.
  Rationale: Property state, events, and generated logic depend on truthful completion.
  Source locations: property invocation helpers, setter/getter wrappers.
  Known related CODEX findings: exception swallowing risks reviewed.
  Suggested unit tests: testGetterExceptionDoesNotReturnStaleValue(), testSetterExceptionDoesNotClaimSuccess()
  Spec target section: Reflection Utilities / Invocation Completion Semantics

  12. Test Coverage Matrix

  Method lookup:

  - testMethodLookupFindsExactPublicMethod
  - testMethodLookupIsDeterministicAcrossCalls
  - testMethodLookupCaseSensitiveByContract
  - testAmbiguousCaseMethodLookupFailsOrUsesDefinedRule
  - testInstanceLookupDoesNotReturnStaticMethodUnlessAllowed
  - testStaticLookupInvokesStaticMethodByContract

  Property getter/setter:

  - testGetterDiscoveryFindsGetProperty
  - testGetterDiscoveryUsesDocumentedFallbackOnly
  - testSetterDiscoveryFindsMatchingSetter
  - testSetterDiscoveryRejectsIncompatibleSetter
  - testMissingGetterReturnsDefinedFailure
  - testSimilarPropertyNameNotUsedAsFallback

  Primitive/wrapper/numeric:

  - testIntegerMatchesIntParameter
  - testBooleanMatchesBooleanPrimitiveParameter
  - testLongDoesNotSilentlyMatchIntWhenLossyByContract
  - testBigDecimalConversionByContract

  Overloads/null:

  - testExactOverloadBeatsObjectOverload
  - testExactPrimitiveWrapperOverloadByContract
  - testAmbiguousOverloadFailsOrUsesDefinedRule
  - testNullArgumentMatchesStringParameter
  - testNullArgumentDoesNotMatchIntParameter
  - testNullArgumentAmbiguousOverloadFailsOrUsesDefinedRule

  Boolean:

  - testBooleanIsGetterResolved
  - testBooleanGetGetterPrecedenceByContract
  - testIsActiveMapsToSetActive
  - testBooleanPropertyNamedIsActiveByContract

  Hierarchy/interface/access:

  - testInheritedGetterResolved
  - testInheritedMethodResolved
  - testInterfaceGetterResolvedFromImplementation
  - testInterfaceMethodInvokedOnImplementation
  - testSubclassOverrideInvoked
  - testPrivateMethodNotInvokedByPublicOnlyContract

  Cache/error/failure:

  - testCachedGetterSpecificToClass
  - testCachedMethodSpecificToParameterTypes
  - testCachedMethodReuseStableAcrossCalls
  - testStrictMissingMethodThrows
  - testLenientMissingMethodReturnsNull
  - testStrictMissingPropertyThrows
  - testAmbiguousMethodDoesNotInvokeWrongMethod
  - testSetterExceptionDoesNotClaimSuccess


*/


