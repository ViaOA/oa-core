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
 */
package com.viaoa.func;


//CODEX unit tests <todo>

/* CODEX Invariants

FUNC-SCOPE-001 — OA Expression Helper Authority
Contract statement:
com.viaoa.func defines OA path-aware function semantics for reusable runtime expressions over OAObjects, Hubs,
property paths, templates, and string sinks; it is not a general Java functional-composition framework.
Rationale:
OAFunction is used by templates, reports, generated UI expressions, calculated values, and runtime helper code where
graph/path semantics matter.
Source scope:
OAFunction; StringCallback; integration boundaries with OAFinder, OAObject, Hub, OAConv, OACompare, OATemplate.
Related CODEX findings:
package-info notes broad functional framing should be narrowed to OA expression/helper behavior.
Suggested unit tests:
functionPackageActsAsOAExpressionHelperLayer(), functionHelpersUseOARuntimePathSemantics().
Spec target section:
Function Runtime / Package Responsibility Semantics.

FUNC-DETERMINISM-001 — Deterministic Function Results
Contract statement:
For the same root object or Hub, property path, loaded graph state, conversion rules, comparison rules, and template
engine behavior, a function call must return the same result.
Rationale:
Templates, reports, calculated runtime values, dynamic UI expressions, and generated helper code require stable
expression output.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), length(...), template(...).
Related CODEX findings:
none observed directly; package-info notes live mutation and lazy loading can change source graph state during
evaluation.
Suggested unit tests:
functionCountIsDeterministicForStableGraph(), functionSumIsDeterministicForStableGraph(),
functionTemplateIsDeterministicForStableInput().
Spec target section:
Function Runtime / Deterministic Execution Semantics.

FUNC-TRAVERSE-001 — OA Graph Traversal Semantics
Contract statement:
Aggregate functions over OAObjects and Hubs must evaluate targets using OA graph traversal semantics through
OAFinder and OAObject property access, not ad hoc collection traversal.
Rationale:
Callers expect OA path traversal, Hub traversal, object identity, lazy-load behavior, and finder semantics to match
the rest of OA runtime behavior.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), OAFinder integration, OAObject.getProperty(...).
Related CODEX findings:
package-info notes terminal property access and OAFinder dependency should be explicit.
Suggested unit tests:
functionCountUsesOAFinderTraversal(), functionSumTraversesHubRootsThroughOAFinder(),
functionAggregateTerminalPropertyUsesOAObjectPropertyAccess().
Spec target section:
Function Runtime / Graph Traversal Semantics.

FUNC-COUNT-001 — Count Reached Objects
Contract statement:
count functions must count each OAObject reached by the supplied traversal path, not each non-null terminal property
value.
Rationale:
Object graph cardinality and non-null property cardinality are distinct runtime meanings.
Source scope:
OAFunction.count(OAObject, String), OAFunction.count(Hub, String), OAFinder.isUsed(...).
Related CODEX findings:
package-info notes count object-vs-value semantics are not obvious from method name.
Suggested unit tests:
functionCountCountsReachedObjectsNotTerminalValues(), functionCountEmptyTraversalReturnsZero(),
functionCountHubRootsCountsAllReachedObjects().
Spec target section:
Function Runtime / Count Semantics.

FUNC-SUM-001 — Numeric Aggregate Semantics
Contract statement:
sum functions must aggregate terminal values that are convertible under OA conversion semantics and must define
whether invalid or non-convertible terminal values are skipped, diagnosed, or fail visibly.
Rationale:
Silent wrong totals in reports, templates, and calculated values can corrupt business/runtime decisions.
Source scope:
OAFunction.sum(...), OAConv.toDouble(...), OADouble accumulator.
Related CODEX findings:
OAFunction.java CODEX notes conversion exceptions are swallowed; package-info notes invalid numeric values are
silently skipped.
Suggested unit tests:
functionSumAggregatesNumericTerminalValues(), functionSumInvalidNumericValueHasDefinedBehavior(),
functionSumNullTerminalValuesFollowAggregateContract().
Spec target section:
Function Runtime / Numeric Aggregate Semantics.

FUNC-NUMERIC-001 — Numeric Precision Boundary
Contract statement:
Numeric aggregate functions that return double must make precision boundaries explicit; precision-sensitive currency
or BigDecimal semantics must not be implied unless specifically provided.
Rationale:
OA reports and calculated values can involve persisted decimal/currency values where double conversion may be
insufficient.
Source scope:
OAFunction.sum(...), OAConv.toDouble(...), OADouble.
Related CODEX findings:
package-info notes BigDecimal/currency sums can lose precision through double conversion.
Suggested unit tests:
functionSumDoublePrecisionBoundaryIsDocumented(), functionSumBigDecimalPrecisionBehaviorIsDefined(),
functionSumDoesNotImplyCurrencySafePrecision().
Spec target section:
Function Runtime / Numeric Precision Semantics.

FUNC-COMPARE-001 — Min/Max Comparison Semantics
Contract statement:
min and max functions must compare terminal values using OA comparison semantics and must define whether comparison
failures are skipped, diagnosed, or fail visibly.
Rationale:
Mixed or non-comparable values must not silently produce misleading min/max results.
Source scope:
OAFunction.min(...), OAFunction.max(...), OACompare.compare(...).
Related CODEX findings:
OAFunction.java CODEX notes comparison exceptions are swallowed; package-info notes silent wrong max/min risk.
Suggested unit tests:
functionMaxUsesOACompareSemantics(), functionMinUsesOACompareSemantics(),
functionMaxInvalidComparableValueHasDefinedBehavior().
Spec target section:
Function Runtime / Comparison Aggregate Semantics.

FUNC-NOVALUE-001 — No-Value Versus Real Value
Contract statement:
Functions must define distinct return semantics for no root, empty path, empty traversal, all-null values, and real
values such as numeric zero or empty string.
Rationale:
Reports and UI expressions must distinguish “no value” from a real value that happens to equal zero or empty text.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), length(...), template(...).
Related CODEX findings:
OAFunction.java CODEX notes max/min no-value behavior can return 0 instead of null; package-info notes no-value
ambiguity.
Suggested unit tests:
functionMaxNullRootNoValueContract(), functionMinEmptyHubNoValueContract(),
functionSumEmptyTraversalReturnsDocumentedNeutralValue(),
functionCountEmptyTraversalReturnsDocumentedNeutralValue().
Spec target section:
Function Runtime / No-Value Semantics.

FUNC-NULL-001 — Null Root and Empty Path Semantics
Contract statement:
Null root inputs and null/empty property paths must return a documented neutral value or fail visibly according to
the function family; they must not fail accidentally or produce misleading success.
Rationale:
OA expression helpers are commonly used from optional template/report/UI contexts where missing roots or paths can
occur.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), length(...), template(...).
Related CODEX findings:
OAFunction.java CODEX notes length(OAObject, String) lacks null checks while other helpers guard null roots/paths.
Suggested unit tests:
functionLengthNullObjectHasDefinedBehavior(), functionLengthNullPathHasDefinedBehavior(),
functionAggregateNullRootUsesDocumentedNeutralValue().
Spec target section:
Function Runtime / Null and Empty Input Semantics.

FUNC-TERMINAL-001 — Null Terminal Value Semantics
Contract statement:
Aggregate and length functions must define whether null terminal property values are skipped, counted, treated as
zero, or treated as no-value.
Rationale:
Totals, min/max values, and length calculations differ significantly depending on null terminal handling.
Source scope:
OAFunction.sum(...), min(...), max(...), length(...), OAObject.getProperty(...).
Related CODEX findings:
package-info notes null terminal values are implicitly skipped in sum/min/max and should be documented.
Suggested unit tests:
functionSumSkipsOrHandlesNullTerminalByContract(), functionMinAllNullValuesHasDefinedResult(),
functionLengthNullTerminalReturnsZero().
Spec target section:
Function Runtime / Terminal Value Semantics.

FUNC-LENGTH-001 — String Length Semantics
Contract statement:
length functions must count only actual String terminal values; null and non-String values must contribute zero
unless another conversion policy is explicitly defined.
Rationale:
Callers must not assume toString() conversion or implicit formatting behavior when computing text length.
Source scope:
OAFunction.length(OAObject, String), OAFunction.length(Hub, String), OAObject.getProperty(...).
Related CODEX findings:
OAFunction.java CODEX notes length null-root/path behavior gap; package-info notes non-String length behavior is
clear but should be a contract.
Suggested unit tests:
functionLengthStringReturnsCharacterCount(), functionLengthNonStringReturnsZero(),
functionLengthHubSumsOnlyStringValues().
Spec target section:
Function Runtime / String Length Semantics.

FUNC-TEMPLATE-001 — Template Delegation Semantics
Contract statement:
template functions must delegate template parsing and rendering semantics to OATemplate and must not reinterpret
template syntax.
Rationale:
Template output must remain consistent whether callers use OAFunction.template or OATemplate directly.
Source scope:
OAFunction.template(OAObject, String), OAFunction.template(Hub, String), OATemplate.setTemplate(...),
OATemplate.process(...).
Related CODEX findings:
none observed.
Suggested unit tests:
functionTemplateObjectMatchesDirectOATemplateProcessing(), functionTemplateHubMatchesDirectOATemplateProcessing(),
functionTemplateNullInputUsesDocumentedNeutralValue().
Spec target section:
Function Runtime / Template Delegation Semantics.

FUNC-FAIL-001 — Function Failure Visibility
Contract statement:
Function execution failures from traversal, conversion, comparison, template processing, or property access must
either propagate, be recorded, or be explicitly documented as lenient skipped values; they must not silently produce
false-success output.
Rationale:
Silent wrong expression output is a production correctness risk for reports, templates, calculated values, and
runtime graph-derived decisions.
Source scope:
OAFunction.sum(...), min(...), max(...), template(...), length(...), OAFinder, OAConv, OACompare, OATemplate.
Related CODEX findings:
OAFunction.java CODEX notes sum/min/max swallow exceptions; package-info highlights silent wrong report/template
totals and max/min values.
Suggested unit tests:
functionSumConversionFailureHasDefinedVisibility(), functionMinComparisonFailureHasDefinedVisibility(),
functionTemplateFailurePropagatesOrIsObservable().
Spec target section:
Function Runtime / Failure and False-Success Prevention.

FUNC-SIDE-001 — Read-Oriented Function Behavior
Contract statement:
OAFunction helpers must not intentionally mutate OAObject, Hub, graph, transaction, sync, or replication state; any
lazy loading caused by traversal must follow OAFinder and object graph load contracts.
Rationale:
Expression helpers used in templates, reports, and calculated values should not create hidden graph mutations beyond
documented traversal/load behavior.
Source scope:
OAFunction static methods; OAFinder traversal boundary; OAObject.getProperty(...).
Related CODEX findings:
package-info notes traversal can trigger lazy loading even though helper methods are otherwise read-oriented.
Suggested unit tests:
functionAggregatesDoNotMutateHubMembership(), functionAggregatesDoNotSetObjectProperties(),
functionTraversalLazyLoadBehaviorFollowsFinderContract().
Spec target section:
Function Runtime / Side-Effect Semantics.

FUNC-ACCUM-001 — Per-Call Accumulator Isolation
Contract statement:
Function accumulator state must be local to each invocation and must not be shared across calls, roots, Hubs, or
threads.
Rationale:
Reports, templates, and UI expressions may run concurrently or reentrantly; cross-call contamination would corrupt
calculated values.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), local OAInteger, OADouble, Object[] accumulators.
Related CODEX findings:
none observed.
Suggested unit tests:
functionConcurrentCallsDoNotShareCountAccumulator(), functionConcurrentCallsDoNotShareSumAccumulator(),
functionReentrantAggregateCallsRemainIndependent().
Spec target section:
Function Runtime / Reentrancy and Accumulator Semantics.

FUNC-CONCURRENT-001 — Live Graph Concurrency Boundary
Contract statement:
OAFunction methods are stateless per call, but they do not provide snapshot locking for source OAObjects or Hubs;
callers that require consistent snapshot semantics must provide stable graph state.
Rationale:
Concurrent Hub/object mutation during aggregation can produce nondeterministic values unless coordinated by the
caller or owning runtime package.
Source scope:
OAFunction methods; Hub iteration in length(Hub, String); OAFinder traversal over live graph state.
Related CODEX findings:
package-info notes static functions are mostly reentrant but source Hubs/OAObjects are live mutable state.
Suggested unit tests:
functionStaticHelpersDoNotUseSharedMutableState(),
functionHubMutationDuringEvaluationUsesDocumentedLiveTraversalSemantics().
Spec target section:
Function Runtime / Concurrency and Snapshot Semantics.

FUNC-LIFETIME-001 — No Hidden Reference Retention
Contract statement:
OAFunction helper calls must not retain root objects, Hubs, templates, traversal state, accumulators, or callback
references after returning.
Rationale:
Expression helpers should not create memory leaks or unexpected lifecycle ownership over Object Graph state.
Source scope:
OAFunction static methods; temporary OAFinder/OATemplate/accumulator objects.
Related CODEX findings:
none observed.
Suggested unit tests:
functionCallDoesNotRetainRootObjectAfterReturn(), functionCallDoesNotRetainHubAfterReturn(),
functionTemplateHelperDoesNotRetainTemplateState().
Spec target section:
Function Runtime / Reference Lifetime Semantics.

FUNC-CALLBACK-001 — StringCallback Sink Contract
Contract statement:
StringCallback is a minimal string sink contract only; accumulation, ordering, synchronization, null-message
behavior, and exception handling belong to the implementation or caller contract.
Rationale:
The interface must not imply hidden listener, observer, lifecycle, or thread-safety behavior.
Source scope:
StringCallback.add(String).
Related CODEX findings:
package-info notes null-message and exception semantics are unspecified.
Suggested unit tests:
stringCallbackNullMessageContractIsDocumentedByImplementation(),
stringCallbackExceptionPropagationBelongsToCaller(), stringCallbackDoesNotImposeThreadSafety().
Spec target section:
Function Runtime / Callback Sink Semantics.

FUNC-INTEGRATION-001 — Finder, Converter, Compare, and Template Alignment
Contract statement:
Function behavior must remain aligned with OAFinder path traversal, OAConv conversion, OACompare ordering, and
OATemplate rendering contracts.
Rationale:
Query, filter, template, report, and generated runtime code should not produce different answers for the same graph/
value semantics depending on which helper path is used.
Source scope:
OAFunction.count(...), sum(...), min(...), max(...), template(...); OAFinder, OAConv, OACompare, OATemplate.
Related CODEX findings:
package-info notes OAFunction depends heavily on these packages and should reference those contracts explicitly.
Suggested unit tests:
functionTraversalMatchesFinderSemantics(), functionSumMatchesConverterNumericSemantics(),
functionMinMaxMatchCompareSemantics(), functionTemplateMatchesTemplatePackageSemantics().
Spec target section:
Function Runtime / Cross-Package Semantic Alignment.

FUNC-CONTEXT-001 — Runtime Context Neutrality
Contract statement:
com.viaoa.func must not install, suppress, or restore transaction, sync, replication, security, graph, or
ThreadLocal runtime context unless an API explicitly states that behavior.
Rationale:
Function helpers may be called from many runtime contexts; context ownership belongs to the caller or higher-level
runtime package.
Source scope:
OAFunction; StringCallback; integration boundaries with transaction, sync, replication, security, graph, object,
hub, template, query, and path packages.
Related CODEX findings:
none observed.
Suggested unit tests:
functionHelpersDoNotModifyOAThreadLocalState(), functionHelpersDoNotChangeTransactionContext(),
functionHelpersDoNotSuppressSyncContext().
Spec target section:
Function Runtime / Runtime Context Boundary Semantics.

FUNC-BOUNDARY-001 — Function Success Versus Object Graph Success
Contract statement:
Successful function execution only establishes calculated expression output; it must not imply successful Object
Graph mutation, datasource persistence, transaction commit, serialization, sync, replication, or trigger completion.
Rationale:
Functions expose graph-derived values, but semantic runtime operation success is owned by the consuming package.
Source scope:
OAFunction public API; StringCallback; integration boundaries with object, hub, graph, datasource, transaction,
serialization, sync, replication, trigger, query, path, and template packages.
Related CODEX findings:
none observed beyond false-success expression-output concerns.
Suggested unit tests:
functionResultDoesNotImplyGraphMutationSuccess(), functionResultDoesNotImplyDatasourceCommitSuccess(),
functionFailureDoesNotPublishSemanticRuntimeSuccess().
Spec target section:
Function Runtime / Object Graph Boundary Semantics.

*/
