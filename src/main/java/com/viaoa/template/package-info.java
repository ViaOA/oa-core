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
 * Provides the core template-processing engine used throughout OA for dynamic
 * string generation, HTML assembly, and metadata-driven code generation.
 *
 * <p>
 * The primary class in this package, {@link com.viaoa.template.OATemplate},
 * implements a lightweight, high-performance parser for evaluating template
 * expressions of the form <code><%= ... %></code>. Templates may include:
 * </p>
 *
 * <ul>
 *   <li>Property lookups on {@link com.viaoa.object.OAObject} instances</li>
 *   <li>Conditional blocks (<code>if</code>, <code>ifnot</code>,
 *       <code>ifequals</code>, comparisons)</li>
 *   <li>Iteration using <code>foreach</code> on {@link com.viaoa.hub.Hub}</li>
 *   <li>Formatted output using OA formatting conventions</li>
 *   <li>Template variables referenced using <code>$name</code></li>
 * </ul>
 *
 * <p>
 * Templates are parsed into a tree of lightweight nodes, allowing extremely
 * fast evaluation with minimal allocations. This architecture enables OA-Web
 * to render HTML fragments efficiently and allows OABuilder to generate large
 * volumes of code using metadata-driven templates.
 * </p>
 *
 * <p>
 * The template engine itself is non-reflective and delegates all property-path
 * resolution to callers (such as OAPropertyPath), enabling the parser to
 * remain compact while supporting complex object-graph navigation.
 * </p>
 *
 * <p>
 * This package contains only foundational template-processing classes and does
 * not impose any UI or web-layer dependencies.
 * </p>
 */
package com.viaoa.template;

/* CODEX Invariants

1. Template Runtime Contracts

  TEMPLATE-RUNTIME-001 — Deterministic Render Output
  Contract statement: For the same template text, root object or Hub state, supplied properties, and template
  settings, rendering must produce the same output.
  Rationale: OA templates are used for UI fragments, reports, and code generation; nondeterministic output breaks
  repeatable application behavior.
  Source locations: OATemplate.process(...), OATemplate.generate(...), OATemplate._generate(...).
  Known related CODEX findings: mutable render state such as hmForEachCounter and cntInDataGrid can affect reusable
  instances.
  Suggested unit tests: templateRenderIsDeterministicForSameInputs(),
  templateRenderDoesNotLeakStateBetweenSequentialCalls().
  Spec target section: Template Runtime / Deterministic Output.

  TEMPLATE-RUNTIME-002 — Render Cancellation Is Explicit
  Contract statement: stopProcessing() must cause an active render to return the explicit cancellation result, not
  partial output that appears successful.
  Rationale: Callers need a reliable way to distinguish completed output from cancelled output.
  Source locations: OATemplate.stopProcessing(), OATemplate.process(...), OATemplate._generate(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: templateStopProcessingReturnsCancelled(), templateNestedGenerateHonorsCancellation().
  Spec target section: Template Runtime / Cancellation Semantics.

  2. Parsing / Token Recognition Contracts

  TEMPLATE-PARSE-001 — Token Boundaries Are Exact
  Contract statement: Template directives must be recognized only by exact directive syntax, not by accidental
  substrings inside property names.
  Rationale: A valid property such as friend or foreachCount must not be misclassified as a control token.
  Source locations: OATemplate.parseTokens(...).
  Known related CODEX findings: broad end detection can classify property names as End; foreach prefix detection can
  classify property names as ForEach.
  Suggested unit tests: templatePropertyNamedFriendRendersValue(),
  templatePropertyNameStartingWithForeachRendersProperty().
  Spec target section: Template Runtime / Token Recognition.

  TEMPLATE-PARSE-002 — Block Directives Require Matching End Tokens
  Contract statement: Block directives such as format, foreach, if, comparison blocks, and ifnot must either consume a
  matching end token or record a parse error.
  Rationale: Missing or misplaced block terminators otherwise produce silent wrong output.
  Source locations: OATemplate.parseA(...), OATemplate.parseB(...), OATemplate.parseC(...), Token.hasEndToken().
  Known related CODEX findings: malformed missing %> and unexpected root-level end can fail to mark parse errors.
  Suggested unit tests: templateMissingBlockEndSetsParseError(), templateUnexpectedRootEndSetsParseError().
  Spec target section: Template Runtime / Block Parsing.

  TEMPLATE-PARSE-003 — Parse Error State Remains Observable
  Contract statement: Once a parsed template contains parse errors, getHasParseError() must report that state
  consistently until the template text is reset or reparsed.
  Rationale: Cached parse trees must not hide malformed template state on later renders.
  Source locations: OATemplate.process(...), OATemplate.createTree(...), OATemplate.getHasParseError().
  Known related CODEX findings: cached rootTreeNode with reset parseErrorCnt can make parse errors disappear.
  Suggested unit tests: cachedTemplatePreservesParseErrorStateAcrossProcesses().
  Spec target section: Template Runtime / Parse Error Reporting.

  3. Object / Property Substitution Contracts

  TEMPLATE-SUBST-001 — Property Paths Resolve Against The Active Render Object
  Contract statement: Each property token must resolve against the current object established by the active root,
  foreach row, or matrix cell.
  Rationale: Wrong active-object routing silently renders values from the wrong graph location or blanks.
  Source locations: OATemplate._generate(...), OATemplate.getValue(...), OATemplate.getProperty(...).
  Known related CODEX findings: matrix-backed foreach can pass null to non-property child blocks that still need the
  row object.
  Suggested unit tests: foreachPropertyUsesCurrentObject(), matrixForeachIfUsesCurrentRowObject().
  Spec target section: Template Runtime / Substitution Semantics.

  TEMPLATE-SUBST-002 — Hub Properties Render By Contracted Aggregate Semantics
  Contract statement: A direct Hub-valued property used as a value must render using the template contract, currently
  Hub size, while foreach must iterate its elements.
  Rationale: Templates must distinguish “show this Hub value” from “iterate this Hub.”
  Source locations: OATemplate.getValue(...), OATemplate._generate(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: templateHubPropertyRendersSize(), templateForeachIteratesHubElements().
  Spec target section: Template Runtime / Hub Substitution.

  TEMPLATE-SUBST-003 — Multi-Hop Hub Paths Concatenate Deterministically
  Contract statement: Property paths containing Hub traversal must collect found terminal values in traversal order
  using a deterministic separator.
  Rationale: UI/report output must not vary or drop values when paths cross many-links.
  Source locations: OATemplate.getProperty(...), OAFinder usage.
  Known related CODEX findings: none observed.
  Suggested unit tests: templateHubPathConcatenatesTerminalValuesInOrder().
  Spec target section: Template Runtime / Path Substitution.

  4. Formatting Contracts

  TEMPLATE-FORMAT-001 — Explicit Formats Apply To Typed Values
  Contract statement: When a template supplies an explicit format for a typed value, that format must be applied
  before lossy string conversion.
  Rationale: Date, time, boolean, and number formats must render according to OA format semantics, not as post-
  processed strings.
  Source locations: OATemplate.getValue(...), OAConv.toString(...), OAString.format(...).
  Known related CODEX findings: explicit object property formats can be ignored during typed conversion and later
  applied to the converted string.
  Suggested unit tests: templateObjectPropertyUsesExplicitTypedFormat(), templateDatePropertyUsesTemplateFormat().
  Spec target section: Template Runtime / Formatting Semantics.

  TEMPLATE-FORMAT-002 — Format Blocks Transform Their Child Output Once
  Contract statement: A format block must apply block formatting and output conversion once to the block result.
  Rationale: Double conversion/highlighting changes visible output and can corrupt generated text.
  Source locations: OATemplate._generate(...), OATemplate.getOutputText(...).
  Known related CODEX findings: output conversion can be applied once to child values and again to the enclosing
  format block.
  Suggested unit tests: templateFormatBlockAppliesOutputConversionOnce().
  Spec target section: Template Runtime / Format Block Semantics.

  TEMPLATE-FORMAT-003 — Command Aggregate Formatting Is Argument-Stable
  Contract statement: Aggregate commands such as #count, #counter, and #sum must parse operands and format arguments
  consistently with their generation code.
  Rationale: Report totals and counters must not silently use the wrong property or format.
  Source locations: OATemplate.parseA(...), OATemplate._generate(...).
  Known related CODEX findings: #sum parsing does not assign the child property and format arguments consistently.
  Suggested unit tests: templateSumUsesHubPropertyValuePropertyAndFormat(), templateCounterUsesProvidedFormat().
  Spec target section: Template Runtime / Aggregate Commands.

  5. Null / Empty Handling Contracts

  TEMPLATE-NULL-001 — Missing Values Render Blank Unless Explicitly Erroring
  Contract statement: Missing object properties, null property values, null roots, or unavailable optional values must
  render as blank unless the directive explicitly defines an error result.
  Rationale: OA templates are often used for optional UI/report fields; null must not crash normal rendering.
  Source locations: OATemplate.getValue(...), OATemplate.getProperty(...), OATemplate._generate(...).
  Known related CODEX findings: new OATemplate().process() can throw through template.toLowerCase() after
  createTree(null) normalizes to empty.
  Suggested unit tests: emptyTemplateInstanceRendersBlank(), missingPropertyRendersBlank().
  Spec target section: Template Runtime / Null Semantics.

  TEMPLATE-NULL-002 — Internal Property Null Semantics Are Explicit
  Contract statement: The template contract must define whether $name missing and $name supplied as null are
  equivalent or distinguishable.
  Rationale: Conditional blocks and defaults depend on knowing whether a caller intentionally supplied null.
  Source locations: OATemplate.setProperty(...), OATemplate.getValue(...).
  Known related CODEX findings: setProperty(name, null) removes the property, making missing and null
  indistinguishable.
  Suggested unit tests: templateInternalNullPropertyContractIsStable(),
  templateMissingInternalPropertyContractIsStable().
  Spec target section: Template Runtime / Internal Property Semantics.

  6. Escaping Contracts

  TEMPLATE-ESCAPE-001 — Encoded Template Delimiters Decode Before Parsing
  Contract statement: HTML-encoded template delimiters intended as real directives must be decoded before token
  parsing.
  Rationale: Templates stored inside HTML/XML contexts must still support OA directives.
  Source locations: OATemplate.createTree(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: templateDecodesEncodedDirectiveDelimitersBeforeParsing().
  Spec target section: Template Runtime / Escaped Delimiter Semantics.

  TEMPLATE-ESCAPE-002 — Output Text Conversion Is Deterministic And Non-Recursive
  Contract statement: fromText/toText conversion and highlight output must be applied according to a single
  deterministic pass per output segment.
  Rationale: Repeated conversion can amplify text, while missed conversion creates inconsistent rendered output.
  Source locations: OATemplate.getOutputText(...), OATemplate.setOutputTextConversion(...),
  OATemplate.setHiliteOutputText(...).
  Known related CODEX findings: format blocks can double-apply output conversion.
  Suggested unit tests: templateOutputConversionAppliesOncePerSegment(), templateHiliteAppliesConsistently().
  Spec target section: Template Runtime / Output Conversion.

  7. Matrix / Generation Contracts

  TEMPLATE-MATRIX-001 — Matrix Rows Preserve Object Graph Alignment
  Contract statement: Matrix-backed foreach rendering must preserve the relationship between root row objects and
  detail path objects for every generated row.
  Rationale: Matrix output is used for table/grid report generation; wrong row alignment produces wrong reports.
  Source locations: OATemplate.createMatrix(...), OATemplate._generate(...), OAMatrix.createGrid(...),
  OAMatrix._populateGridRows(...).
  Known related CODEX findings: matrix-backed foreach can pass null for non-property child blocks; missing property-
  to-column mapping can throw.
  Suggested unit tests: matrixForeachAlignsRootAndDetailValues(), matrixForeachNestedBlockUsesCurrentRowObject().
  Spec target section: Template Runtime / Matrix Generation.

  TEMPLATE-MATRIX-002 — Matrix Public Accessors Honor Bounds Contracts
  Contract statement: Matrix accessors must return null for out-of-range rows or columns and must not throw for
  documented invalid positions.
  Rationale: Template/report generation should degrade predictably when probing optional matrix cells.
  Source locations: OAMatrix.getObject(...), OAMatrix.getRealObject(...), OAMatrix.getColumn(...).
  Known related CODEX findings: getObject(validRow, -1) throws instead of returning null.
  Suggested unit tests: matrixGetObjectNegativeColumnReturnsNull(), matrixGetRealObjectOutOfRangeReturnsNull().
  Spec target section: Template Runtime / Matrix Access.

  TEMPLATE-MATRIX-003 — Matrix Row Counting Terminates
  Contract statement: Matrix row-count operations must terminate for root and child columns and must match the row
  count generated by createGrid().
  Rationale: Row counts drive render loops; hangs or mismatches block output or skip rows.
  Source locations: OAMatrix.getRowCount(), OAMatrix.getRowCount(Column), OAMatrix.getRowCount(Column, OAObject),
  OAMatrix.createGrid().
  Known related CODEX findings: getRowCount(Column) loop never advances col for child columns.
  Suggested unit tests: matrixGetRowCountForChildColumnDoesNotHang(), matrixRowCountMatchesCreatedGridSize().
  Spec target section: Template Runtime / Matrix Row Semantics.

  TEMPLATE-MATRIX-004 — Matrix Root Columns Must Not Overwrite Each Other
  Contract statement: If multiple root columns are supported, each root’s generated rows must be appended or otherwise
  combined by explicit contract; if unsupported, additional roots must be rejected.
  Rationale: Silent row overwrites create wrong tabular output.
  Source locations: OAMatrix.addColumn(...), OAMatrix.createGrid(...).
  Known related CODEX findings: createGrid() resets row position for each root column.
  Suggested unit tests: matrixMultipleRootColumnsDoNotOverwriteRows().
  Spec target section: Template Runtime / Matrix Root Semantics.

  8. Callback / Custom Substitution Contracts

  TEMPLATE-CUSTOM-001 — Include Expansion Uses Caller-Defined Include Text
  Contract statement: Include directives must resolve through getIncludeText(name) and must preserve surrounding
  template text.
  Rationale: OA templates rely on subclass hooks for shared fragments such as headers, footers, and generated code
  sections.
  Source locations: OATemplate.preprocess(...), OATemplate.getIncludeText(...).
  Known related CODEX findings: include recursion tracking is global for the whole preprocess pass, so reusing the
  same include twice is treated as recursive.
  Suggested unit tests: templateIncludeExpandsSubclassText(), templateSameIncludeCanBeUsedTwiceWhenNotRecursive().
  Spec target section: Template Runtime / Include Semantics.

  TEMPLATE-CUSTOM-002 — Include Recursion Detection Tracks Active Stack Only
  Contract statement: Include recursion detection must prevent active recursive cycles without rejecting independent
  repeated includes.
  Rationale: Templates commonly reuse header/footer fragments multiple times.
  Source locations: OATemplate.preprocess(...).
  Known related CODEX findings: active include names are never removed after expansion.
  Suggested unit tests: templateRecursiveIncludeIsReported(), templateRepeatedNonRecursiveIncludeIsAllowed().
  Spec target section: Template Runtime / Include Recursion.

  9. Reusable State Contracts

  TEMPLATE-STATE-001 — Template Parsing Cache Must Match Template Text
  Contract statement: A cached parse tree must be invalidated whenever template text changes and reused only for the
  same template text.
  Rationale: Template instances are reusable; stale parse trees produce wrong output.
  Source locations: OATemplate.setTemplate(...), OATemplate.process(...), OATemplate.rootTreeNode.
  Known related CODEX findings: none observed.
  Suggested unit tests: setTemplateInvalidatesParsedTree().
  Spec target section: Template Runtime / Reusable Template State.

  TEMPLATE-STATE-002 — Render-Local State Must Be Restored After Render
  Contract statement: Per-render state such as data-grid depth and foreach counters must not leak into later renders
  after normal completion, cancellation, or exception.
  Rationale: Reused template instances must not carry one render’s traversal state into another.
  Source locations: OATemplate.cntInDataGrid, OATemplate.hmForEachCounter, OATemplate._generate(...).
  Known related CODEX findings: cntInDataGrid++ lacks finally restoration; hmForEachCounter is mutable instance state.
  Suggested unit tests: templateDataGridStateRestoredAfterException(),
  templateCounterStateDoesNotLeakBetweenRenders().
  Spec target section: Template Runtime / Render State.

  TEMPLATE-STATE-003 — Root Selection Cache Must Remain Valid For Current Inputs
  Contract statement: When rendering with two possible roots, cached root-class selection must only be reused if it is
  valid for the current pair of root objects.
  Rationale: Cached templates can be reused with different object pairs; stale root routing causes wrong property
  output.
  Source locations: OATemplate.process(...), OATemplate.classChoosen, OATemplate.ppSample.
  Known related CODEX findings: classChoosen is cached after one render and reused without validating current roots.
  Suggested unit tests: templateTwoRootSelectionRecomputesForDifferentRootPair().
  Spec target section: Template Runtime / Root Routing State.

  10. Failure / Silent Wrong-Output Contracts

  TEMPLATE-FAIL-001 — Normal Template Errors Must Be Visible
  Contract statement: Syntax and structural template errors must either be rendered as explicit error text, recorded
  in parse state, or thrown; they must not silently produce successful wrong output.
  Rationale: Silent template failure is expensive to diagnose and can generate bad UI, reports, or source code.
  Source locations: OATemplate.parseA(...), OATemplate.parseTokens(...), OATemplate.getHasParseError().
  Known related CODEX findings: malformed tokens and unexpected end tokens can be hidden.
  Suggested unit tests: templateMalformedTokenIsObservableError(), templateMissingEndDoesNotSilentlySucceed().
  Spec target section: Template Runtime / Error Visibility.

  TEMPLATE-FAIL-002 — Normal Optional Data Absence Must Not Crash Rendering
  Contract statement: Missing optional object values, null roots, empty Hubs, or unsupported optional matrix columns
  must not crash normal rendering unless the template construct requires them by contract.
  Rationale: OA templates frequently render partial object graphs.
  Source locations: OATemplate.getValue(...), OATemplate._generate(...), OAMatrix.getObject(...).
  Known related CODEX findings: null template, row-tag preprocessing shape assumptions, null sibling helper addition,
  missing matrix mapping.
  Suggested unit tests: templateNullRootWithOptionalPropertyRendersBlank(), templateEmptyForeachRendersNoRows().
  Spec target section: Template Runtime / Optional Data Handling.

  TEMPLATE-FAIL-003 — Control Directives Must Not Produce False Positives
  Contract statement: Conditional and loop directives must render children only when their explicit condition or
  iteration source says they should.
  Rationale: False-positive rendering creates wrong business text, generated code, or UI content.
  Source locations: OATemplate.parseB(...), OATemplate._generate(...).
  Known related CODEX findings: fixed IfNotEquals missing switch branch; conditional argument parsing limitations for
  values with spaces.
  Suggested unit tests: templateIfNotEqualsSuppressesEqualValue(),
  templateIfEqualsWithConfiguredOperandSemanticsIsStable().
  Spec target section: Template Runtime / Control Flow Semantics.

  11. Test Coverage Matrix

  Template Runtime / Deterministic Output
  Tests: templateRenderIsDeterministicForSameInputs, templateRenderDoesNotLeakStateBetweenSequentialCalls.

  Template Runtime / Token Recognition
  Tests: templatePropertyNamedFriendRendersValue, templatePropertyNameStartingWithForeachRendersProperty.

  Template Runtime / Block Parsing
  Tests: templateMissingBlockEndSetsParseError, templateUnexpectedRootEndSetsParseError.

  Template Runtime / Substitution Semantics
  Tests: foreachPropertyUsesCurrentObject, templateHubPathConcatenatesTerminalValuesInOrder.

  Template Runtime / Formatting Semantics
  Tests: templateObjectPropertyUsesExplicitTypedFormat, templateDatePropertyUsesTemplateFormat,
  templateFormatBlockAppliesOutputConversionOnce.

  Template Runtime / Null Semantics
  Tests: emptyTemplateInstanceRendersBlank, missingPropertyRendersBlank, templateInternalNullPropertyContractIsStable.

  Template Runtime / Escaping Semantics
  Tests: templateDecodesEncodedDirectiveDelimitersBeforeParsing, templateOutputConversionAppliesOncePerSegment.

  Template Runtime / Matrix Generation
  Tests: matrixForeachAlignsRootAndDetailValues, matrixGetObjectNegativeColumnReturnsNull,
  matrixGetRowCountForChildColumnDoesNotHang, matrixMultipleRootColumnsDoNotOverwriteRows.

  Template Runtime / Include Semantics
  Tests: templateIncludeExpandsSubclassText, templateSameIncludeCanBeUsedTwiceWhenNotRecursive,
  templateRecursiveIncludeIsReported.

  Template Runtime / Reusable State
  Tests: setTemplateInvalidatesParsedTree, templateDataGridStateRestoredAfterException,
  templateTwoRootSelectionRecomputesForDifferentRootPair.

  Template Runtime / Error Visibility
  Tests: templateMalformedTokenIsObservableError, templateMissingEndDoesNotSilentlySucceed,
  templateIfNotEqualsSuppressesEqualValue.

*/



