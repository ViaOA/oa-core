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

/* CODEX Invariants

1. Package Summary

  com.viaoa.func is a small OA expression/helper package. In OA 4.0 terms, it provides reusable graph-aware functions
  for templates, reports, dynamic UI expressions, and generated/runtime helper code.

  Current contents:

  - OAFunction: static helper methods for counting, summing, min/max, template processing, and string length over
    OAObject, Hub, and OA property paths.
  - StringCallback: minimal callback/sink interface for accepting string output.
  - package-info.java: package-level description of graph/path-based functional helpers.

  This package is not a general Java functional framework. It is closer to an OA expression standard library.

  2. Core Concepts

  - Function/callback abstraction: a helper operation applied to an OAObject, Hub, property path, or string callback.
  - Predicate/filter behavior: not directly present in this package; OAFunction delegates traversal/filter mechanics
    to OAFinder.
  - Mapper/transform behavior: not directly exposed; OAFunction extracts values via OAObject.getProperty, OAFinder,
    OACompare, OAConv, and OATemplate.
  - No-op/default behavior: null roots or empty paths generally return neutral values such as 0, null, or empty
    traversal output.
  - Null handling: inconsistent today; most methods guard null roots/paths, while length(OAObject, String) does not.
  - Exception propagation: template methods propagate template exceptions; aggregate methods currently swallow
    conversion/compare exceptions.
  - Generic type expectations: OAFunction uses raw Hub and Object results, relying on OA runtime property metadata and
    converters.
  - Side-effect expectations: aggregate functions should be observational/read-only except for lazy-load effects
    caused by path traversal.
  - Thread-safety assumptions: methods create per-call local state and are mostly reentrant, but traversal operates on
    live mutable OAObjects/Hubs and depends on their thread-safety contracts.

  3. Invariants

  A. Functional Contract Invariants

  1. FUNC-CONTRACT-001: Aggregate functions must operate over OA graph traversal semantics
     Invariant: count, sum, min, and max must evaluate values using OAObject/Hub traversal semantics, not ad hoc
     collection semantics.
     Why it matters: callers expect OA path traversal, lazy-load behavior, and object graph identity semantics.
     Locations: OAFunction.count, sum, min, max; dependency on OAFinder.
     Confidence: High.
     Gaps: terminal property access uses obj.getProperty(pp), so nested terminal semantics should be documented.
  2. FUNC-CONTRACT-002: Count must count reached target objects, not non-null terminal values
     Invariant: count counts each object reached by OAFinder for the supplied path.
     Why it matters: distinguishes object graph cardinality from property-value cardinality.
     Locations: OAFunction.count(OAObject,String), count(Hub,String).
     Confidence: High.
     Gaps: method name does not clarify whether it counts objects or values.
  3. FUNC-CONTRACT-003: Sum must aggregate numeric terminal values only
     Invariant: sum must add numeric-convertible terminal values and ignore or fail on non-convertible values
     according to explicit contract.
     Why it matters: templates/reports depend on totals being trustworthy.
     Locations: OAFunction.sum overloads, OAConv.toDouble.
     Confidence: Medium.
     Gaps: current behavior silently skips conversion failures.
  4. FUNC-CONTRACT-004: Min/max must compare terminal values using OA comparison semantics
     Invariant: min and max must use OA comparison semantics and must not silently return misleading values after
     comparison failure.
     Why it matters: dynamic UI/report expressions depend on stable ordering.
     Locations: OAFunction.min, max, OACompare.compare.
     Confidence: Medium.
     Gaps: current behavior silently skips comparison failures.
  5. FUNC-CONTRACT-005: Template functions delegate fully to OATemplate
     Invariant: template methods must use OATemplate processing semantics and return the processed string from the
     template engine.
     Why it matters: template expression behavior should be centralized.
     Locations: OAFunction.template(OAObject,String), template(Hub,String), OATemplate.
     Confidence: High.
     Gaps: none observed.

  B. Null-Handling Invariants

  6. FUNC-NULL-001: Null root inputs must produce documented neutral output
     Invariant: null OAObject or Hub inputs must return a documented neutral value without unexpected exceptions.
     Why it matters: template/report helpers are often used in optional graph contexts.
     Locations: most OAFunction methods.
     Confidence: Medium.
     Gaps: length(OAObject,String) lacks root/path null checks.
  7. FUNC-NULL-002: No-value aggregate result must be distinguishable from numeric zero
     Invariant: min/max no-value cases should return null, while numeric aggregates may return 0 only when that is the
     documented neutral sum/count.
     Why it matters: 0 can be a real value and is also the wrong type for date/string/object min/max.
     Locations: OAFunction.min, max.
     Confidence: Low/Medium.
     Gaps: public min/max overloads return 0 for null/empty inputs.
  8. FUNC-NULL-003: Null terminal values must be skipped consistently
     Invariant: aggregate functions must define whether null terminal values are skipped, counted, or treated as zero.
     Why it matters: report totals and min/max values differ significantly depending on null policy.
     Locations: OAFunction.sum, min, max, length.
     Confidence: Medium.
     Gaps: behavior is implicit, not specified.

  C. Type-Safety / Generic Invariants

  9. FUNC-TYPE-001: Raw Hub inputs must be validated at runtime
     Invariant: when accepting raw Hub, functions must only process elements that are OAObject or safely ignore
     others.
     Why it matters: prevents ClassCastException from mixed/incorrect Hub content.
     Locations: OAFunction.count(Hub,String), sum(Hub,...), min/max(Hub,...), length(Hub,String).
     Confidence: Medium.
     Gaps: OAFinder handles Hub traversal; length(Hub,String) explicitly checks instanceof OAObject.
  10. FUNC-TYPE-002: Numeric precision contract must be explicit
     Invariant: numeric functions must specify whether they are double-precision helpers or precision-preserving OA
     numeric helpers.
     Why it matters: currency and BigDecimal values can lose precision through double.
     Locations: OAFunction.sum, OADouble, OAConv.toDouble.
     Confidence: Low/Medium.
     Gaps: current API returns double; precision limitations are not explicit.
  11. FUNC-TYPE-003: Min/max return type is terminal property type or null
     Invariant: min/`1. Package Summary

  com.viaoa.func is a small OA expression/helper package. In OA 4.0 terms, it provides reusable functions for
  evaluating values across OAObject graphs and Hub collections, mainly for templates, reports, generated UI
  expressions, and dynamic runtime values.

  Current package contents:

  - OAFunction: static helper methods for count, sum, min, max, template processing, and string length.
  - StringCallback: simple callback/sink interface for receiving string output.
  - package-info.java: package-level description.

  This package is not a general Java functional-composition library. Its current role is an OA path-aware expression
  helper layer built on OAFinder, OAObject, Hub, OAConv, OACompare, and OATemplate.

  2. Core Concepts

  - Function/callback abstraction: OAFunction methods are static functions; StringCallback.add(String) is a one-method
    callback sink.
  - Predicate/filter behavior: no direct predicate interface exists here; traversal filtering is delegated to
    OAFinder.
  - Mapper/transform behavior: no general mapper abstraction exists; property values are extracted through
    OAObject.getProperty.
  - No-op/default behavior: many functions return 0 or null for null/empty input.
  - Null handling: each function defines its own current null behavior; not fully consistent.
  - Exception propagation: template functions propagate template/runtime exceptions; numeric/compare aggregators
    currently swallow conversion/compare exceptions.
  - Generic type expectations: no public generics in OAFunction; StringCallback accepts String.
  - Side-effect expectations: OAFunction should behave like read-only evaluation over live OA graph state, but
    traversal can trigger lazy loading via OAFinder.
  - Thread-safety assumptions: methods are stateless per call, but they operate over live Hub/OAObject graphs whose
    own thread-safety/lazy-load semantics apply.

  3. Invariants

  A. Functional Contract Invariants

  1. FUNC-CONTRACT-001: OAFunction methods must be deterministic for the same graph state
     Invariant: Given the same root object/hub, property path, loaded graph state, and converter/compare behavior, a
     function must return the same result.
     Why it matters: templates, reports, and generated UI expressions rely on stable values.
     Locations: OAFunction.count, sum, max, min, length, template.
     Confidence: Medium.
     Gaps: lazy loading and live Hub mutation can change traversal results during evaluation.
  2. FUNC-CONTRACT-002: Aggregate functions must distinguish no-value from real value
     Invariant: aggregate functions should have explicit return semantics for “no root”, “empty traversal”, “all null
     values”, and real numeric/comparable zero.
     Why it matters: 0 and null mean different things in reporting and UI expressions.
     Locations: OAFunction.sum, max, min, count, length.
     Confidence: Low/Medium.
     Gaps: max/min currently mix 0 and null for no-value cases.
  3. FUNC-CONTRACT-003: Count must count reached objects, not property values
     Invariant: count counts each object visited at the terminal traversal point according to OAFinder, not non-null
     terminal property values.
     Why it matters: callers must know whether count(order.items.amount) counts items or non-null amounts.
     Locations: OAFunction.count(OAObject,String), count(Hub,String).
     Confidence: Medium.
     Gaps: package docs should make object-count semantics explicit.
  4. FUNC-CONTRACT-004: Template functions delegate all expression semantics to OATemplate
     Invariant: OAFunction.template must not reinterpret template syntax; it creates an OATemplate, sets text, and
     processes object/hub.
     Why it matters: template behavior must remain consistent across direct OATemplate and helper use.
     Locations: OAFunction.template(OAObject,String), template(Hub,String), OATemplate.
     Confidence: High.
     Gaps: null/empty template returns null before delegation.

  B. Null-Handling Invariants

  5. FUNC-NULL-001: Null-root behavior must be explicit and consistent by function family
     Invariant: functions must clearly define what happens when root OAObject or Hub is null.
     Why it matters: expression helpers are commonly used in UI/report contexts where missing roots are normal.
     Locations: all OAFunction overloads.
     Confidence: Medium.
     Gaps: most methods guard null; length(OAObject,String) does not.
  6. FUNC-NULL-002: Empty or null property path must not produce misleading success
     Invariant: null/empty property paths must return a documented default or fail visibly; behavior must be
     consistent by operation.
     Why it matters: invalid generated expressions should not silently produce wrong output.
     Locations: OAFunction.sum/max/min/count/length.
     Confidence: Low/Medium.
     Gaps: most methods check OAString.isEmpty(pp); length(OAObject,String) lacks this check.
  7. FUNC-NULL-003: Null property values must be skipped or counted according to documented aggregate contract
     Invariant: aggregate functions must state whether null terminal values are ignored, treated as zero, or treated
     as no-value.
     Why it matters: report totals and min/max values depend on null semantics.
     Locations: OAFunction.sum/max/min/length.
     Confidence: Medium.
     Gaps: null values are skipped in sum/min/max; this should be documented.

  C. Type-Safety / Generic Invariants

  8. FUNC-TYPE-001: Numeric functions must only aggregate values convertible under OA conversion rules
     Invariant: sum may aggregate only values that OAConv.toDouble can convert, or must fail visibly/diagnose
     unconvertible values.
     Why it matters: silent conversion mismatch produces wrong totals.
     Locations: OAFunction.sum, OAConv.
     Confidence: Low/Medium.
     Gaps: conversion exceptions are swallowed.
  9. FUNC-TYPE-002: Min/max functions must use OA comparison semantics consistently
     Invariant: max/min must compare values using OACompare and must not silently ignore comparison failure unless
     lenient mode is documented.
     Why it matters: mixed or non-comparable values can produce false max/min results.
     Locations: OAFunction.max/min, OACompare.
     Confidence: Low/Medium.
     Gaps: comparison exceptions are swallowed.
  10. FUNC-TYPE-003: String length must only count actual String values
     Invariant: length contributes only values that are instances of String; non-String values contribute 0.
     Why it matters: callers must not assume toString() length behavior.
     Locations: OAFunction.length.
     Confidence: High.
     Gaps: behavior is clear in code but should be documented as contract.
  11. FUNC-TYPE-004: StringCallback accepts string messages and imposes no ownership or threading contract
     Invariant: StringCallback.add(String) is only a sink contract; implementations own accumulation, synchronization,
     and error behavior.
     Why it matters: prevents assuming callback implementations are safe or side-effect free.
     Locations: StringCallback.
     Confidence: High.
     Gaps: no explicit null-message or exception contract.

  D. Exception Propagation Invariants

  12. FUNC-EXC-001: Expression helper failures must not be swallowed unless lenient semantics are explicit
     Invariant: exceptions from conversion, comparison, traversal, and template processing must either propagate or be
     explicitly documented as skipped/lenient.
     Why it matters: silent wrong report/template output is worse than visible failure in production.
     Locations: OAFunction.sum/max/min/template.
     Confidence: Low.
     Gaps: sum/max/min swallow exceptions; template propagates.
  13. FUNC-EXC-002: Callback exceptions belong to callback owner
     Invariant: StringCallback does not define whether add exceptions propagate, are swallowed, or are aggregated;
     callers must treat it as fail-fast unless a specific implementation says otherwise.
     Why it matters: output pipelines need predictable failure behavior.
     Locations: StringCallback.
     Confidence: Medium.
     Gaps: contract is underspecified.

  E. Side-Effect / Reentrancy Invariants

  14. FUNC-SIDE-001: OAFunction methods should not mutate the graph intentionally
     Invariant: OAFunction helpers must be read-oriented and must not intentionally modify OAObject/Hub state.
     Why it matters: templates/reports/calculated values should not create side effects.
     Locations: OAFunction static methods.
     Confidence: Medium.
     Gaps: OAFinder traversal can lazy-load references, which is a runtime side effect outside direct mutation.
  15. FUNC-SIDE-002: Traversal side effects must follow OAFinder lazy-load semantics
     Invariant: any loading caused by path traversal must be governed by OAFinder and graph/load contracts.
     Why it matters: expression evaluation can become expensive or mutate loaded-state visibility.
     Locations: OAFunction.count/sum/max/min, OAFinder.
     Confidence: Medium.
     Gaps: OAFunction does not expose loaded-only/strict traversal options.
  16. FUNC-REENTRANT-001: Per-call accumulator state must not be shared across invocations
     Invariant: accumulators used by OAFunction must be local to each call.
     Why it matters: avoids cross-call contamination under concurrent or reentrant use.
     Locations: local OAInteger, OADouble, Object[] in OAFunction.
     Confidence: High.
     Gaps: none observed.

  F. Thread-Safety / Concurrency Invariants

  17. FUNC-THREAD-001: Static functions must be stateless and safe for concurrent callers
     Invariant: OAFunction must not use shared mutable static state.
     Why it matters: helpers can be used concurrently by reports/UI/background tasks.
     Locations: OAFunction.
     Confidence: High.
     Gaps: actual graph objects/hubs are external live state and may change concurrently.
  18. FUNC-THREAD-002: Thread-safety of source Hub/OAObject is external
     Invariant: OAFunction does not lock root hubs/objects; callers must provide stable graph state if consistent
     snapshot semantics are required.
     Why it matters: concurrent Hub mutation during aggregation can produce nondeterministic values.
     Locations: OAFunction loops/traversal through OAFinder; length(Hub,String) direct iteration.
     Confidence: Medium.
     Gaps: snapshot vs live traversal semantics are not documented here.

  G. Lifecycle / Reference Retention Invariants

  19. FUNC-LIFE-001: OAFunction must not retain root objects, hubs, templates, or callbacks after return
     Invariant: helper calls must not store references beyond method scope.
     Why it matters: prevents expression helpers from creating memory leaks.
     Locations: OAFunction methods.
     Confidence: High.
     Gaps: none observed.
  20. FUNC-LIFE-002: StringCallback lifetime is owned by caller
     Invariant: package does not register, retain, or deregister StringCallback; any lifecycle belongs to the code
     that passes it around.
     Why it matters: avoids hidden listener-like leak assumptions.
     Locations: StringCallback.
     Confidence: High.
     Gaps: no implementations in package.

  H. Integration Invariants

  21. FUNC-INTEGRATION-001: OAFunction must remain compatible with OAFinder path semantics
     Invariant: path traversal behavior in OAFunction must align with OAFinder for recursion, lazy loading, cycle
     prevention, and filter handling.
     Why it matters: aggregate values must match finder/path behavior used elsewhere.
     Locations: OAFunction.count/sum/max/min, OAFinder.
     Confidence: Medium.
     Gaps: OAFunction does not expose OAFinder options such as use-only-loaded-data.
  22. FUNC-INTEGRATION-002: Numeric and comparison behavior must align with converter/compare contracts
     Invariant: OAFunction.sum must follow com.viaoa.converter numeric semantics; max/min must follow
     com.viaoa.compare ordering semantics.
     Why it matters: query/filter/report/template behavior should agree.
     Locations: OAFunction.sum, OAFunction.max/min, OAConv, OACompare.
     Confidence: Medium.
     Gaps: double aggregation may violate precision expectations for BigDecimal/currency.
  23. FUNC-INTEGRATION-003: Template helpers must align with template package contracts
     Invariant: OAFunction.template must produce the same output as direct OATemplate.process.
     Why it matters: generated code and templates should not differ based on helper path.
     Locations: OAFunction.template, OATemplate.
     Confidence: High.
     Gaps: none observed beyond null shortcut behavior.
  24. FUNC-INTEGRATION-004: Functions must not assume transaction, sync, replication, or trigger context
     Invariant: OAFunction does not install or restore OAThreadLocal, transaction, sync, or graph context.
     Why it matters: callers in runtime services must manage context explicitly.
     Locations: OAFunction, package-level behavior.
     Confidence: High.
     Gaps: none observed.

  4. Listener / Callback Semantics

  com.viaoa.func has only one explicit callback type: StringCallback.

  - It is not classified as BEFORE/DURING/AFTER.
  - It has no built-in ordering, aggregation, cleanup, or exception policy.
  - Its exception behavior is implementation/caller-owned.

  Alignment:

  - OAFunction does not invoke external callback interfaces.
  - StringCallback is minimal and does not impose hidden listener behavior.

  Gaps:

  - If StringCallback is used as an observer, caller code must decide whether exceptions stop processing or are
    aggregated.
  - Null message handling is unspecified.
  - Thread-safety of callback implementations is unspecified.

  5. Failure Modes

  - Null OAObject passed to length causes unexpected failure.
  - Null result from max/min is ambiguous with no comparable values.
  - Integer 0 result from max/min null root is wrong type for date/string/object max/min.
  - Invalid numeric value is silently skipped in sum.
  - Invalid comparison is silently skipped in min/max.
  - BigDecimal/currency sum loses precision through double conversion.
  - Concurrent Hub mutation during aggregation produces nondeterministic results.
  - Lazy load during expression evaluation changes runtime loaded state.
  - Callback exception from StringCallback.add propagates unexpectedly if caller assumes observer behavior.
  - Callback exception is swallowed unexpectedly if an implementation hides it.
  - Recursive or expensive property paths create unexpectedly high traversal cost.
  - Function used inside trigger/calculated property causes reentrant lazy-load/event side effects.

  6. Test Recommendations

  - testLengthNullObjectReturnsZeroOrDefinedFailure
  - testLengthNullPathReturnsZero
  - testMaxNullRootReturnsNull
  - testMinNullRootReturnsNull
  - testMaxEmptyHubReturnsNull
  - testMinAllNullValuesReturnsNull
  - testSumInvalidNumericValueStrictModeFailsOrLenientModeReportsSkipped
  - testMaxInvalidComparableValueFailsOrReportsSkipped
  - testSumBigDecimalPrecisionContract
  - testCountCountsObjectsNotNonNullTerminalValues
  - testTemplateNullRootReturnsNull
  - testTemplateMatchesDirectOATemplateProcessing
  - testConcurrentFunctionCallsDoNotShareAccumulatorState
  - testHubMutationDuringFunctionUsesDocumentedLiveTraversalSemantics
  - testStringCallbackNullMessageContract
  - testStringCallbackExceptionPropagationContract

  7. Hardening Recommendations

  - Normalize null/no-value behavior across aggregate functions.
  - Add strict and lenient variants for sum/min/max, or document current lenient skipping explicitly.
  - Add BigDecimal/currency-safe numeric functions.
  - Add optional diagnostics for skipped conversion/compare values.
  - Document that count counts reached objects, not non-null terminal values.
  - Document that OAFunction traversal may lazy-load through OAFinder.
  - Add path validation or strict-path variants for generated code.
  - Clarify StringCallback.add null and exception semantics.
  - Add useOnlyLoadedData variants if these functions are used in triggers/sync/replay contexts.
  - Update package-level docs to state this is an OA expression helper package, not a general functional programming
    abstraction.

  8. Open Questions

  - Should max/min return null or 0 for null roots and empty paths?
  - Should sum treat invalid values as zero, skip them, or fail?
  - Are financial/currency sums expected to be supported here, or should callers use math/converter APIs directly?
  - Should OAFunction offer loaded-only traversal to avoid lazy-load side effects?
  - Should template/report callers get diagnostics for skipped aggregate values?
  - Is StringCallback allowed to receive null messages?
  - Should StringCallback.add exceptions propagate, be logged, or be aggregated by callers?
  - Is this package intended to grow into OA’s expression standard library, or remain a minimal legacy helper layer?



qqqqqqqqqqqqqq other

A. Architectural Assessment

  com.viaoa.func is much smaller than the prompt’s broad “functional subsystem” framing. It is primarily an
  expression/helper package around OA object graph traversal, aggregation, and template processing.

  The model is conceptually useful: OAFunction gives templates/reports/UI code compact aggregate helpers over OA
  paths. StringCallback is a minimal sink interface and has no obvious correctness issue.

  The main weakness is semantic consistency: null/no-value behavior, strict vs lenient conversion, and numeric
  precision are implicit. For runtime expression infrastructure, those contracts should be explicit.

  B. Invariant Risk Areas

  - Aggregates must distinguish “no value” from numeric zero.
  - Conversion/compare failure must not silently produce misleading aggregate results.
  - Null handling must be consistent across all helper methods.
  - Numeric precision must match OA reporting/datasource expectations.
  - OAFunction depends heavily on OAFinder, OACompare, OAConv, and OATemplate; its contracts should reference those
    packages.

  C. Top Production Risks

  1. Silent wrong report/template totals from ignored conversion failures.
  2. Silent wrong max/min values from ignored comparison failures.
  3. Precision loss in currency/BigDecimal sums.
  4. Wrong-type 0 returned from max/min no-value cases.
  5. NPE from length(OAObject, String) where other helpers tolerate null roots.

  D. Hardening Recommendations

  - Define strict vs lenient aggregate behavior.
  - Make null/no-value return contracts consistent.
  - Add BigDecimal/currency-safe aggregation support.
  - Add diagnostics for skipped values in lenient mode.
  - Add tests around null roots, empty paths, invalid numeric values, mixed comparison types, and BigDecimal sums.

qqqqqqqqqqqq other (discussion)

 Useful additions:

  - Precision-safe numeric functions
      - sumDecimal, avgDecimal, minNumber, maxNumber
      - explicit currency/BigDecimal behavior
  - Common aggregates
      - avg, countNonNull, countDistinct
      - first, last, any, all, none
      - distinct, maybe returning Set/List
  - String helpers over paths
      - join(hub, path, delimiter)
      - concat, contains, startsWith, isEmpty, isNotEmpty
  - Date/time aggregates
      - minDate, maxDate
      - daysBetween, age, durationSum if OA date/time types are common in templates/reports
  - Strict/lenient modes
      - strict: throw on invalid path/conversion/compare
      - lenient: skip invalid values but record/log diagnostics
  - Path-safe helpers
      - functions that use OAPath/metadata instead of direct obj.getProperty(pp) where nested terminal paths are
        allowed
      - consistent null/unloaded behavior
  - Template/report helpers
      - format(obj, path, format)
      - value(obj, path, defaultValue)
      - ifValue, coalesce
  - Hub/object selection helpers
      - findFirst, findAll, filter, exists
      - probably thin wrappers over OAFinder/filters

  What I would not put here:

  - Threading/async helpers
  - Transaction/sync-aware callbacks
  - General-purpose Java Function/Predicate abstractions
  - Heavy query/select logic

  The package should feel like OA’s small expression standard library: safe, deterministic, path-aware functions for
  templates, reports, calculated UI values, and generated code.



*/
