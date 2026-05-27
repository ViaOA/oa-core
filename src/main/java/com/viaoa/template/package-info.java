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

//CODEX unit tests <todo>

/* CODEX Invariants

TEMPLATE-RUNTIME-001 — Deterministic Rendering
Contract statement:
For the same template text, root object or Hub state, supplied properties, internal properties, output-conversion
settings, and template state, rendering must produce the same output or the same explicit failure/cancellation
result.
Rationale:
OA templates are used for UI fragments, reports, generated code, and metadata-driven runtime output;
nondeterministic rendering breaks repeatability and can generate incorrect runtime artifacts.
Source scope:
OATemplate.process(...), OATemplate.generate(...), OATemplate._generate(...), OATemplate.getOutputText(...).
Related CODEX findings:
Mutable render state such as hmForEachCounter, cntInDataGrid, and cached root selection can affect reusable template
instances.
Suggested unit tests:
templateRenderIsDeterministicForSameInputs, templateRenderDoesNotLeakStateBetweenSequentialCalls.
Spec target section:
Template Runtime / Deterministic Rendering

TEMPLATE-LIFECYCLE-001 — Template Text Owns Parse Tree
Contract statement:
A parsed template tree must correspond exactly to the current template text; changing template text must invalidate
cached parse state, parse-error state, row-tag preprocessing state, sampled property paths, and any root-selection
assumptions derived from the prior template.
Rationale:
Reusable template instances must not render stale parse trees or stale routing decisions after the source template
changes.
Source scope:
OATemplate.setTemplate(...), OATemplate.getTemplate(), OATemplate.process(...), OATemplate.createTree(...),
OATemplate.rootTreeNode, OATemplate.ppSample, OATemplate.classChoosen.
Related CODEX findings:
Cached rootTreeNode and root-selection state can hide parse errors or route properties using stale assumptions.
Suggested unit tests:
setTemplateInvalidatesParsedTree, setTemplateClearsParseErrorState, setTemplateRecomputesRootSelection.
Spec target section:
Template Runtime / Parse Lifecycle

TEMPLATE-CANCEL-001 — Explicit Cancellation Result
Contract statement:
stopProcessing() must cause active rendering to stop through the documented cancellation path and return the
explicit cancellation result, not partial output that appears successful.
Rationale:
Callers need to distinguish completed rendered output from intentionally cancelled rendering.
Source scope:
OATemplate.stopProcessing(), OATemplate.process(...), OATemplate.generate(...), OATemplate._generate(...).
Related CODEX findings:
none observed.
Suggested unit tests:
templateStopProcessingReturnsCancelled, templateNestedGenerateHonorsCancellation.
Spec target section:
Template Runtime / Cancellation

TEMPLATE-PARSE-001 — Exact Directive Recognition
Contract statement:
Template directives must be recognized only by exact directive syntax after normalization; ordinary property names
must not be classified as directives because they contain or begin with directive text.
Rationale:
Valid property names such as friend or foreachCount must render as properties, not silently become end or foreach
control tokens.
Source scope:
OATemplate.parseTokens(...), OATemplate.Token, OATemplate.TagType.
Related CODEX findings:
End-token detection can classify property names containing end; foreach prefix detection can classify property names
beginning with foreach.
Suggested unit tests:
templatePropertyNamedFriendRendersValue, templatePropertyNameStartingWithForeachRendersProperty.
Spec target section:
Template Runtime / Token Recognition

TEMPLATE-PARSE-002 — Block Structure Must Be Matched Or Observable
Contract statement:
Block directives such as format, foreach, if, ifnot, comparisons, and command blocks must consume a valid matching
end token or record/render an observable template error.
Rationale:
Unmatched or misplaced blocks can otherwise produce silent wrong output in generated UI, reports, or code.
Source scope:
OATemplate.parse(...), OATemplate.parseTokens(...), OATemplate.parseA(...), OATemplate.parseB(...),
OATemplate.parseC(...), OATemplate.Token.hasEndToken().
Related CODEX findings:
Malformed missing %> and unexpected root-level end can fail to mark parse errors.
Suggested unit tests:
templateMissingBlockEndSetsParseError, templateUnexpectedRootEndSetsParseError,
templateMalformedTokenIsObservableError.
Spec target section:
Template Runtime / Block Parsing

TEMPLATE-PARSE-003 — Parse Error State Remains Observable
Contract statement:
If parsed template content contains syntax or structural errors, getHasParseError() must report that state
consistently until the template is reset or reparsed into an error-free tree.
Rationale:
Cached parse trees must not hide malformed template state on later renders.
Source scope:
OATemplate.createTree(...), OATemplate.process(...), OATemplate.getHasParseError(), OATemplate.parseErrorCnt.
Related CODEX findings:
Cached rootTreeNode with parseErrorCnt reset can make prior parse errors disappear.
Suggested unit tests:
cachedTemplatePreservesParseErrorStateAcrossProcesses, reparsingValidTemplateClearsParseErrorState.
Spec target section:
Template Runtime / Parse Error Reporting

TEMPLATE-PREPROCESS-001 — Include Expansion Preserves Template Semantics
Contract statement:
Include directives must resolve through getIncludeText(name), replace only the intended include directive, preserve
surrounding text, and produce parse-observable errors for malformed include syntax.
Rationale:
Includes are the template package’s reusable-fragment boundary for generated code, shared UI fragments, and reports.
Source scope:
OATemplate.preprocess(...), OATemplate.getIncludeText(...), OATemplate.createTree(...).
Related CODEX findings:
Include recursion tracking can treat repeated non-recursive includes as recursive.
Suggested unit tests:
templateIncludeExpandsSubclassText, templateMalformedIncludeRecordsError.
Spec target section:
Template Runtime / Include Expansion

TEMPLATE-PREPROCESS-002 — Include Recursion Tracks Active Stack
Contract statement:
Include recursion detection must reject active include cycles while allowing the same include to be used multiple
independent times in one template.
Rationale:
Templates commonly reuse headers, footers, rows, and generated fragments more than once; only true active recursion
should be blocked.
Source scope:
OATemplate.preprocess(String, ArrayList<String>), OATemplate.getIncludeText(...).
Related CODEX findings:
Active include names are not removed after expansion, so independent repeated includes can be treated as recursive.
Suggested unit tests:
templateRecursiveIncludeIsReported, templateRepeatedNonRecursiveIncludeIsAllowed.
Spec target section:
Template Runtime / Include Recursion

TEMPLATE-ROOT-001 — Active Render Object Routing
Contract statement:
Every property, condition, format block, nested foreach, command, and matrix-generated child block must resolve
against the correct active object established by the root object, foreach row, Hub iteration, matrix row, or
selected root candidate.
Rationale:
Wrong active-object routing silently renders values from the wrong graph location or incorrectly blanks conditional
output.
Source scope:
OATemplate.process(...), OATemplate._generate(...), OATemplate.getValue(...), OATemplate.getProperty(...),
OATemplate.createMatrix(...).
Related CODEX findings:
Matrix-backed foreach can pass null to non-property child blocks that still need the current row object;
classChoosen can be reused for incompatible root pairs.
Suggested unit tests:
foreachPropertyUsesCurrentObject, matrixForeachIfUsesCurrentRowObject,
templateTwoRootSelectionRecomputesForDifferentRootPair.
Spec target section:
Template Runtime / Active Object Semantics

TEMPLATE-PATH-001 — Metadata-Aware Property And Path Resolution
Contract statement:
Property and path expressions inside templates must resolve using OA runtime property/path semantics and must
distinguish syntactically valid template tokens, metadata-valid paths, and runtime-available values.
Rationale:
Templates bind to generated blueprint metadata and live object graphs; path resolution must match OAObject, Hub,
OAPath, and metadata behavior.
Source scope:
OATemplate.getValue(...), OATemplate.getProperty(...), OAPath usage, OAFinder usage.
Related CODEX findings:
Matrix path-to-column mapping can fail when property paths are missing or misaligned.
Suggested unit tests:
templatePropertyPathUsesOaPathSemantics, templateInvalidPropertyPathIsObservableByContract,
templateRuntimeUnavailablePathRendersBlankByContract.
Spec target section:
Template Runtime / Path Resolution

TEMPLATE-HUB-001 — Hub Value Versus Hub Iteration Semantics
Contract statement:
A Hub-valued property rendered as a value and a Hub used as a foreach source must have distinct, documented
behavior: value rendering uses the template’s aggregate display contract, while foreach iterates members in Hub
order.
Rationale:
Templates must distinguish “display this relationship value” from “expand this relationship into repeated output.”
Source scope:
OATemplate.getValue(...), OATemplate.getProperty(...), OATemplate._generate(...).
Related CODEX findings:
none observed.
Suggested unit tests:
templateHubPropertyRendersContractedAggregateValue, templateForeachIteratesHubElementsInOrder.
Spec target section:
Template Runtime / Hub Rendering

TEMPLATE-HUB-002 — Multi-Hop Hub Path Aggregation
Contract statement:
A property path that traverses one or more Hub/many-link segments must collect terminal values deterministically
using the package’s documented traversal order and separator.
Rationale:
Report and UI output must not vary, drop values, or reorder values when paths cross many-link relationships.
Source scope:
OATemplate.getProperty(...), OAPath, OAFinder.
Related CODEX findings:
none observed.
Suggested unit tests:
templateHubPathConcatenatesTerminalValuesInOrder, templateHubPathEmptyResultRendersBlank.
Spec target section:
Template Runtime / Hub Path Aggregation

TEMPLATE-NULL-001 — Optional Data Renders Blank By Contract
Contract statement:
Null roots, null property values, missing optional values, empty Hubs, and unavailable optional paths must render as
blank or no rows unless the directive explicitly defines an error result.
Rationale:
OA templates frequently render partial object graphs and optional fields; normal absence of data must not crash
rendering or masquerade as structural template failure.
Source scope:
OATemplate.process(...), OATemplate.getValue(...), OATemplate.getProperty(...), OATemplate._generate(...),
OAMatrix.getObject(...), OAMatrix.getRealObject(...).
Related CODEX findings:
new OATemplate().process() can throw after createTree(null) normalizes to empty; row-tag preprocessing and matrix
access can crash on optional/malformed shapes.
Suggested unit tests:
emptyTemplateInstanceRendersBlank, missingPropertyRendersBlank, templateNullRootWithOptionalPropertyRendersBlank,
templateEmptyForeachRendersNoRows.
Spec target section:
Template Runtime / Null And Optional Data

TEMPLATE-NULL-002 — Internal Property Null Semantics
Contract statement:
The template contract must define and consistently apply whether an internal property explicitly set to null is
equivalent to a missing property or distinguishable from it.
Rationale:
Conditionals, defaults, and generated output depend on whether callers can intentionally supply null values.
Source scope:
OATemplate.setProperty(...), OATemplate.getValue(...), OATemplate.propInternal.
Related CODEX findings:
setProperty(name, null) removes the property, making missing and null indistinguishable.
Suggested unit tests:
templateInternalNullPropertyContractIsStable, templateMissingInternalPropertyContractIsStable.
Spec target section:
Template Runtime / Internal Property Semantics

TEMPLATE-FORMAT-001 — Typed Formatting Before String Loss
Contract statement:
Explicit template formats for typed values must be applied at the typed value conversion boundary before lossy
string conversion, and default metadata formats must be applied only according to the documented formatting
contract.
Rationale:
Date, time, boolean, numeric, and generated-code formatting must match OA converter/format semantics.
Source scope:
OATemplate.getValue(...), OAConv.toString(...), OAString.format(...).
Related CODEX findings:
Explicit object property formats can be ignored during typed conversion and applied later to an already converted
string.
Suggested unit tests:
templateObjectPropertyUsesExplicitTypedFormat, templateDatePropertyUsesTemplateFormat,
templateNumberPropertyUsesTemplateFormat.
Spec target section:
Template Runtime / Typed Formatting

TEMPLATE-FORMAT-002 — Format Block Converts Child Output Once
Contract statement:
A format block must format and output-convert its child result exactly once according to the block contract.
Rationale:
Double conversion or missed conversion changes visible output and can corrupt generated source or report text.
Source scope:
OATemplate._generate(...), OATemplate.getOutputText(...), OATemplate.setOutputTextConversion(...),
OATemplate.setHiliteOutputText(...).
Related CODEX findings:
Output conversion can be applied once to child values and again to the enclosing format block.
Suggested unit tests:
templateFormatBlockAppliesOutputConversionOnce, templateHiliteAppliesConsistently.
Spec target section:
Template Runtime / Format Block Semantics

TEMPLATE-COMMAND-001 — Command Argument Semantics
Contract statement:
Template commands such as #count, #counter, and #sum must parse operands, value properties, and format arguments
according to a stable command grammar, and generation must use the same argument positions established during
parsing.
Rationale:
Counters and totals are report-generation primitives; wrong operand mapping silently produces incorrect totals.
Source scope:
OATemplate.parseA(...), OATemplate._generate(...), OATemplate.TagType.Command.
Related CODEX findings:
#sum parsing does not assign child property and format arguments consistently.
Suggested unit tests:
templateSumUsesHubPropertyValuePropertyAndFormat, templateCounterUsesProvidedFormat,
templateCountUsesIntendedHubSource.
Spec target section:
Template Runtime / Command Semantics

TEMPLATE-CONDITION-001 — Conditional Evaluation Is Deterministic
Contract statement:
if, ifnot, ifequals, ifnotequals, ifgt, ifgte, iflt, and iflte directives must evaluate deterministically against
the active object/property/value context and must render children only when the directive condition is satisfied.
Rationale:
False-positive or false-negative conditional rendering creates wrong business text, generated code, and UI state.
Source scope:
OATemplate.parseTokens(...), OATemplate.parseA(...), OATemplate._generate(...).
Related CODEX findings:
IfNot fallthrough is noted as intentional; IfNotEquals branch and conditional argument parsing limitations were
reviewed.
Suggested unit tests:
templateIfNotEqualsSuppressesEqualValue, templateIfEqualsWithConfiguredOperandSemanticsIsStable,
templateIfNotFallthroughBehaviorRemainsStable.
Spec target section:
Template Runtime / Conditional Semantics

TEMPLATE-ESCAPE-001 — Encoded Directive Delimiters Decode Before Parsing
Contract statement:
HTML-encoded template delimiters intended as real template directives must be decoded before token parsing, while
ordinary escaped text must remain ordinary output text.
Rationale:
Templates stored inside HTML/XML contexts must still support OA directives without corrupting literal content.
Source scope:
OATemplate.createTree(...), OATemplate.parseTokens(...).
Related CODEX findings:
none observed.
Suggested unit tests:
templateDecodesEncodedDirectiveDelimitersBeforeParsing, templateLiteralEscapedTextRemainsLiteral.
Spec target section:
Template Runtime / Escaped Delimiters

TEMPLATE-OUTPUT-001 — Output Conversion Boundary
Contract statement:
Output text conversion and highlighting must be deterministic, segment-scoped, and non-recursive for each output
segment unless explicitly contracted otherwise.
Rationale:
Repeated conversion can amplify output; missed conversion creates inconsistent visible output across literals,
properties, and format blocks.
Source scope:
OATemplate.getOutputText(...), OATemplate.setOutputTextConversion(...), OATemplate.setHiliteOutputText(...),
OATemplate._generate(...).
Related CODEX findings:
Format blocks can double-apply output conversion.
Suggested unit tests:
templateOutputConversionAppliesOncePerSegment, templateOutputConversionAppliesToLiteralAndPropertyBySameContract.
Spec target section:
Template Runtime / Output Conversion

TEMPLATE-MATRIX-001 — Matrix Defines A Metadata-Aware Tabular View
Contract statement:
OAMatrix must materialize a deterministic row/column view from root Hubs, detail links, group-by links, property
paths, and OA metadata, preserving root-to-detail alignment for every generated row.
Rationale:
Matrix-backed rendering is used for table/grid report generation; wrong alignment produces visibly incorrect
reports.
Source scope:
OAMatrix.addColumn(...), OAMatrix.addDetailColumn(...), OAMatrix.addGroupByColumn(...), OAMatrix.createGrid(...),
OAMatrix._populateGridRows(...), OATemplate.createMatrix(...).
Related CODEX findings:
Matrix-backed foreach can pass null for non-property child blocks; missing property-to-column mapping can throw.
Suggested unit tests:
matrixForeachAlignsRootAndDetailValues, matrixForeachNestedBlockUsesCurrentRowObject,
matrixMissingPathMappingIsObservable.
Spec target section:
Template Runtime / Matrix Semantics

TEMPLATE-MATRIX-002 — Matrix Column Validation
Contract statement:
Detail and group-by matrix columns must validate property paths against the correct source class and link metadata
before committing the column definition.
Rationale:
Invalid or wrongly scoped matrix paths should fail predictably during setup rather than produce late null
dereferences or wrong row structure.
Source scope:
OAMatrix.addDetailColumn(...), OAMatrix.addGroupByColumn(...), OAMatrix.verifyLinkProperty(...),
OAMatrix.getRootColumn(...), OAMatrix.getPropertyPathFromRoot(...).
Related CODEX findings:
addGroupByColumn can validate against the wrong class shape for detail/non-root columns.
Suggested unit tests:
matrixDetailColumnRequiresValidLinkPath, matrixGroupByColumnValidatesAgainstCorrectSourceClass.
Spec target section:
Template Runtime / Matrix Column Validation

TEMPLATE-MATRIX-003 — Matrix Accessors Honor Bounds Contracts
Contract statement:
Matrix public accessors must return the documented null result for out-of-range rows or columns and must not throw
for invalid positions covered by the accessor contract.
Rationale:
Template/report generation may probe optional matrix cells; invalid positions must degrade predictably.
Source scope:
OAMatrix.getColumn(...), OAMatrix.getObject(...), OAMatrix.getRealObject(...), OAMatrix.getGrid(...),
OAMatrix.getColumns(...).
Related CODEX findings:
getObject(validRow, -1) throws instead of returning null.
Suggested unit tests:
matrixGetObjectNegativeColumnReturnsNull, matrixGetRealObjectOutOfRangeReturnsNull,
matrixGetColumnOutOfRangeReturnsNull.
Spec target section:
Template Runtime / Matrix Access

TEMPLATE-MATRIX-004 — Matrix Row Counting Terminates And Matches Grid
Contract statement:
Matrix row-count operations must terminate for root and child columns and must match the row count produced by
createGrid() for the same column definitions and graph state.
Rationale:
Row counts drive render loops; hangs or mismatches can block rendering or skip rows.
Source scope:
OAMatrix.getRowCount(), OAMatrix.getRowCount(Column), OAMatrix.getRowCount(Column, OAObject), OAMatrix.createGrid().
Related CODEX findings:
getRowCount(Column) loop can fail to advance when called with child columns.
Suggested unit tests:
matrixGetRowCountForChildColumnDoesNotHang, matrixRowCountMatchesCreatedGridSize.
Spec target section:
Template Runtime / Matrix Row Counting

TEMPLATE-MATRIX-005 — Multiple Root Column Semantics
Contract statement:
If multiple root columns are accepted, their generated rows must be combined by a deterministic contract without
overwriting earlier root rows; if unsupported, multiple roots must be rejected visibly.
Rationale:
The public API allows root columns, and silent row overwrites produce missing or mixed tabular data.
Source scope:
OAMatrix.addColumn(...), OAMatrix.createGrid(...), OAMatrix.getGrid(...).
Related CODEX findings:
createGrid() resets row position for each root column, allowing later roots to overlay earlier rows.
Suggested unit tests:
matrixMultipleRootColumnsDoNotOverwriteRows, matrixMultipleRootColumnsContractIsExplicit.
Spec target section:
Template Runtime / Matrix Root Semantics

TEMPLATE-STATE-001 — Render-Local State Restoration
Contract statement:
Render-local state such as foreach counters, data-grid depth, temporary sibling helpers, active stop counters, and
recursive generation context must be restored or isolated after normal completion, cancellation, and exceptions.
Rationale:
Template instances can be reused; one render’s state must not affect later output or runtime thread context.
Source scope:
OATemplate.generate(...), OATemplate._generate(...), OATemplate.hmForEachCounter, OATemplate.cntInDataGrid,
OASiblingHelper registration through OAThreadLocalService.
Related CODEX findings:
cntInDataGrid++ lacks finally restoration; hmForEachCounter is mutable instance state.
Suggested unit tests:
templateDataGridStateRestoredAfterException, templateCounterStateDoesNotLeakBetweenRenders,
templateSiblingHelperRemovedAfterRenderFailure.
Spec target section:
Template Runtime / Render State Cleanup

TEMPLATE-THREAD-001 — Shared Template Thread-Safety Boundary
Contract statement:
Template and matrix instances with mutable parse/render/grid state must either be used by one render at a time or
have externally defined synchronization; shared reusable parsed structures must not expose partially mutated render
state across threads.
Rationale:
OA runtime rendering can occur in background, UI, web, or tooling contexts, and mutable instance state can corrupt
concurrent renders.
Source scope:
OATemplate fields rootTreeNode, parseErrorCnt, hmForEachCounter, cntInDataGrid, classChoosen; OAMatrix alColumn,
alGrid.
Related CODEX findings:
Mutable render and matrix cache state are instance-scoped.
Suggested unit tests:
templateConcurrentUseRequiresOwnerDecision, independentTemplateInstancesRenderConcurrently.
Spec target section:
Template Runtime / Threading Boundary

TEMPLATE-FAIL-001 — Template Errors Must Not Become Silent Success
Contract statement:
Invalid template syntax, invalid command grammar, invalid structural blocks, invalid matrix setup, and unrecoverable
path/metadata failures must be observable through parse state, explicit error output, exception, or documented
failure result.
Rationale:
Silent wrong-output is especially dangerous when templates generate code, reports, UI fragments, or blueprint-
derived artifacts.
Source scope:
OATemplate.createTree(...), OATemplate.preprocess(...), OATemplate.parseTokens(...), OATemplate.parse(...),
OATemplate.getHasParseError(...), OAMatrix.addDetailColumn(...), OAMatrix.addGroupByColumn(...).
Related CODEX findings:
Malformed tokens, unexpected end tokens, missing row-tag shapes, broken #sum arguments, and invalid matrix mappings
can be hidden or crash outside a controlled contract.
Suggested unit tests:
templateMalformedTokenIsObservableError, templateMissingEndDoesNotSilentlySucceed,
matrixInvalidLinkPathFailsPredictably.
Spec target section:
Template Runtime / Failure Visibility

TEMPLATE-BOUNDARY-001 — Cross-Package Semantic Boundaries
Contract statement:
Template processing must delegate and remain compatible with OAObject property semantics, Hub iteration/order
semantics, OAPath/OAFinder traversal semantics, converter/date/time formatting semantics, metadata link semantics,
runtime ThreadLocal cleanup, and graph sibling-helper behavior.
Rationale:
Templates are semantic rendering contracts over executable OA blueprints and live object graphs; boundary mismatches
create wrong output or runtime context leaks.
Source scope:
OATemplate, OAMatrix, OAObject, Hub, OAPath, OAFinder, OAConv, OADate/OADateTime/OATime, OALinkInfo,
OASiblingHelper, OARuntime/OAThreadLocalService.
Related CODEX findings:
Matrix path mapping, typed formatting, ThreadLocal sibling helper cleanup, and active-object routing findings
illustrate these boundaries.
Suggested unit tests:
templatePathRenderingMatchesOAPathContract, templateDateFormattingMatchesConverterContract,
templateSiblingHelperCleanupMatchesRuntimeContract.
Spec target section:
Template Runtime / Cross-Package Integration

*/


