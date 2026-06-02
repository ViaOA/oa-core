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
 * Provides parsing and lexical analysis utilities for OA's object query language.
 * <p>
 * The {@code com.viaoa.datasource.query} package converts high-level, object-based
 * query strings (such as {@code "customer.lastName = 'Smith' and active = true"})
 * into structured {@link com.viaoa.query.OAQueryToken} sequences.
 * These tokens are then translated by the active {@link com.viaoa.datasource.OADataSource}
 * implementation into the appropriate native query language — typically SQL,
 * REST parameters, or distributed filter expressions.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a unified, lightweight parser for OA's object query syntax.</li>
 *   <li>Enable model-driven queries without requiring SQL or REST syntax knowledge.</li>
 *   <li>Serve as the first step in query transformation pipelines used by
 *       {@link com.viaoa.datasource.jdbc.OADataSourceJDBC} and other DataSources.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.query.OAQueryTokenizer} —
 *       converts query text into token streams.</li>
 *   <li>{@link com.viaoa.query.OAQueryTokenManager} —
 *       performs lexical scanning and classification of query symbols.</li>
 *   <li>{@link com.viaoa.query.OAQueryToken} —
 *       represents an individual token (operator, literal, or keyword).</li>
 *   <li>{@link com.viaoa.query.OAQueryTokenType} —
 *       defines the constants used for token categorization.</li>
 * </ul>
 *
 * <h2>Supported Query Features</h2>
 * <ul>
 *   <li>Comparison operators: =, !=, &lt;, &lt;=, &gt;, &gt;=</li>
 *   <li>Logical operators: AND, OR, NOT</li>
 *   <li>String literals, escape sequences, and quoted identifiers</li>
 *   <li>Parentheses and nested sub-expressions</li>
 *   <li>Pattern matching and set membership ({@code LIKE}, {@code IN (...)})</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * String query = "customer.age >= 18 and (status = 'A' or creditLimit > 500)";
 * OAQueryTokenizer qt = new OAQueryTokenizer();
 * Vector<OAQueryToken> tokens = qt.convertToTokens(query);
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Lightweight, no external dependencies.</li>
 *   <li>Non-thread-safe by design — instantiate per query.</li>
 *   <li>Used internally by OADataSource implementations for dynamic query translation.</li>
 * </ul>
 *
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 * @see com.viaoa.query.OAQueryTokenizer
 */
package com.viaoa.query;

//CODEX unit tests 20260528

/* CODEX Invariants

QUERY-SEMANTIC-001 — Query Text Is An Executable Semantic Contract
Contract statement:
An OA query expression must preserve the semantic intent expressed by property paths, metadata, operators, literals,
parameters, grouping, and runtime context from tokenization through filter/select/datasource interpretation.
Rationale:
OA queries are semantic selection contracts over generated blueprint metadata and live object graphs. They drive
datasource selects, in-memory filtering, Hub contents, object matching, projections, and runtime views.
Source scope:
OAQuery, OAQueryTokenizer, OAQueryTokenManager, OAQueryToken, OAQueryTokenType, consumers in filter/select/
datasource/path/metadata layers.
Related CODEX findings:
OR/AND precedence drift in filter consumer; malformed/trailing tokens accepted by consumer paths.
Suggested unit tests:
testQueryCriteriaMeaningIsPreservedAcrossParserAndFilter, testEquivalentDatasourceAndMemoryQuerySemantics,
testQueryTextRepresentsSameSelectionAcrossConsumers.
Spec target section:
Query Runtime / Criteria Semantics

QUERY-TOKEN-001 — Deterministic Tokenization
Contract statement:
For the same query text and tokenizer state, tokenization must produce the same ordered token stream with the same
token types, subtypes, and literal values.
Rationale:
Every downstream query operation depends on token identity and order. A wrong token changes query meaning before
metadata, filter, or datasource code can validate it.
Source scope:
OAQueryTokenManager.setQuery, OAQueryTokenManager.getNext, OAQueryToken, OAQueryTokenType,
OAQueryTokenizer.convertToTokens.
Related CODEX findings:
NOTLIKE not included in isOperator; <> not-equal not recognized; IS NOT NULL tokenization fragile.
Suggested unit tests:
testTokenizesAllComparisonOperators, testTokenizesLikeAndNotLikeOperators, testTokenizesNotEqualAliases,
testRepeatedTokenizationProducesSameTokenStream.
Spec target section:
Query Runtime / Tokenization

QUERY-TOKEN-002 — Operator Identity Semantics
Contract statement:
All supported comparison, logical, null, membership, LIKE/NOT LIKE, and alias operators must be classified
consistently as operators or structural tokens according to OAQueryTokenType.
Rationale:
Parser stages consume operators by token classification. Missing operator identity can turn a valid query into
malformed structure or silently change expression shape.
Source scope:
OAQueryToken.isOperator, OAQueryTokenType, OAQueryTokenManager.getNext, OAQueryTokenizer evaluate methods.
Related CODEX findings:
NOTLIKE is not treated as an operator by isOperator; <> is documented but not tokenized as NOTEQUAL.
Suggested unit tests:
testNotLikeIsOperator, testNotEqualBangEqualAndAngleBracketHaveSameSemantics,
testIsNullAndIsNotNullTokenizeAsNullPredicates.
Spec target section:
Query Runtime / Operator Semantics

QUERY-PARSE-001 — Complete Expression Consumption
Contract statement:
Parsing must consume the complete query expression or fail visibly. A leading valid expression followed by trailing
tokens, dangling operators, or unparsed subexpressions must not be accepted as a successful query.
Rationale:
Partial parse success silently broadens or narrows selected objects and datasource rows.
Source scope:
OAQueryTokenizer.convertToTokens, OAQueryTokenizer.evaluate, evaluateA/evaluateB/evaluateC/evaluateD/evaluateE/
evaluateF, OAQuery.parse.
Related CODEX findings:
Trailing valid/invalid tokens can be ignored by filter consumer; OAQueryTokenizer now checks EOF after top-level
evaluation.
Suggested unit tests:
testParserRejectsTrailingTokens, testParserRejectsDanglingOperator,
testParserRejectsLeadingExpressionWithGarbageTail, testParserConsumesWholeExpression.
Spec target section:
Query Runtime / Parse Completeness

QUERY-PARSE-002 — Structural Expression Validity
Contract statement:
Logical and comparison grammar must reject structurally invalid expressions such as chained comparisons, missing
operands, malformed IN lists, mismatched parentheses, or operators in illegal positions.
Rationale:
Permissive structural parsing can create token streams that downstream consumers interpret differently or
incorrectly.
Source scope:
OAQueryTokenizer.evaluateA/evaluateB/evaluateB2/evaluateC/evaluateC2/evaluateD/evaluateE/evaluateF.
Related CODEX findings:
OAQueryTokenizer CODEX comment notes operator parsing recurses into evaluateA and can accept odd invalid structures.
Suggested unit tests:
testRejectsChainedComparison, testRejectsComparisonMissingRightOperand, testRejectsMalformedInList,
testRejectsMismatchedParentheses.
Spec target section:
Query Runtime / Expression Grammar

QUERY-LITERAL-001 — Literal Preservation
Contract statement:
String, numeric, boolean, null, identifier, and passthrough literals must preserve their intended characters and
semantic value without adding, removing, truncating, or reinterpreting content.
Rationale:
Literals represent user data, codes, names, IDs, keys, flags, dates, and datasource expressions. Literal corruption
produces wrong matches.
Source scope:
OAQueryTokenManager.getNext, OAQueryTokenizer.evaluateD/evaluateE, OAQueryToken.value.
Related CODEX findings:
Unterminated quoted strings accepted; quote-handling paths need coverage.
Suggested unit tests:
testSingleQuotedStringLiteral, testDoubleQuotedStringLiteral, testEmbeddedQuoteLiteral, testEscapedStringLiteral,
testNumericLiteralTokenValuePreserved.
Spec target section:
Query Runtime / Literal Semantics

QUERY-LITERAL-002 — Unterminated Literal Failure
Contract statement:
Unterminated quoted strings, escaped strings, or passthrough blocks must fail visibly and must not become valid
partial tokens.
Rationale:
Unterminated literals change query meaning and can transform malformed text into executable selection criteria.
Source scope:
OAQueryTokenManager.getNext, passthrough scanning, quoted string scanning.
Related CODEX findings:
Unterminated quoted strings accepted at EOF; unterminated PASS[...]THRU blocks accepted at EOF.
Suggested unit tests:
testRejectsUnterminatedSingleQuotedString, testRejectsUnterminatedDoubleQuotedString,
testRejectsUnterminatedPassthruBlock.
Spec target section:
Query Runtime / Literal Failure Semantics

QUERY-LOGIC-001 — Logical Operator Semantics
Contract statement:
AND, OR, NOT, and parentheses must have deterministic precedence and grouping semantics that match the OA query
contract and downstream filter/datasource interpretation.
Rationale:
Logical precedence determines result set membership. Drift between in-memory and datasource behavior creates
mismatched Hub/query results.
Source scope:
OAQueryTokenizer evaluate methods, OAQueryTokenType logical tokens, consumers in OAQueryFilter/select/datasource
layers.
Related CODEX findings:
Existing package invariant references OR/AND precedence drift in OAQueryFilter.
Suggested unit tests:
testAndOrPrecedenceMatchesSpec, testParenthesesOverridePrecedence, testNotOperatorAppliesToIntendedExpression,
testLogicalSemanticsMatchFilterAndDatasource.
Spec target section:
Query Runtime / Logical Semantics

QUERY-PATH-001 — Metadata-Driven Property Path Semantics
Contract statement:
Property and path references in query expressions must resolve according to OAPath and OA metadata semantics,
including OAObjectInfo, OAPropertyInfo, OALinkInfo, calculated properties, link cardinality, and object key
metadata.
Rationale:
Queries select over generated blueprint metadata and object graphs. Wrong path resolution returns wrong objects,
Hubs, or datasource rows.
Source scope:
Query tokens consumed by path/filter/select/datasource layers, OAQueryToken values representing identifiers, OAPath
integration.
Related CODEX findings:
none in com.viaoa.query itself; boundary risk noted in package invariants.
Suggested unit tests:
testQueryPathResolvesMetadataProperty, testQueryPathRejectsUnknownProperty, testQueryPathMatchesOAPathTraversal,
testQueryPathUsesObjectKeyMetadata.
Spec target section:
Query Runtime / Path and Metadata Semantics

QUERY-METADATA-001 — Metadata Type Alignment
Contract statement:
Query interpretation must honor metadata-defined property types, object references, key/ID semantics, link
cardinality, calculated values, and datasource mapping before applying comparison or conversion behavior.
Rationale:
Metadata is the runtime truth for how query text maps to objects and persistence. Type drift causes wrong
comparisons and wrong datasource criteria.
Source scope:
OAQueryToken, OAQueryTokenizer output, consumers in filter/select/datasource/path/metadata packages.
Related CODEX findings:
none in com.viaoa.query itself.
Suggested unit tests:
testQueryUsesMetadataPropertyTypeForComparison, testQueryUsesIdMetadataForObjectKeyCriteria,
testQueryRejectsMetadataMismatch, testCalculatedPropertyQueryUsesMetadataType.
Spec target section:
Query Runtime / Metadata Alignment

QUERY-PARAM-001 — Positional Parameter Binding
Contract statement:
Positional parameters must bind in query order, exactly once per placeholder, preserving value identity, nullness,
type, and comparison intent.
Rationale:
Wrong parameter order, missing parameters, reused values, or ignored values change query meaning and can return
incorrect rows or objects.
Source scope:
OAQueryTokenType.QUESTION, OAQueryTokenManager.getNext, OAQueryTokenizer output, consumer binding in OAQueryFilter/
select/datasource layers.
Related CODEX findings:
Missing ? args can become literal "?" in OAQueryFilter.
Suggested unit tests:
testQuestionParametersBindInOrder, testMissingParameterFailsVisibly,
testExtraParameterFailsOrIsExplicitlyContracted, testNullParameterPreservesNullSemantics.
Spec target section:
Query Runtime / Parameter Binding

QUERY-PARAM-002 — IN Parameter And Collection Semantics
Contract statement:
Collection/list/Hub/array parameters used for IN predicates must preserve membership semantics, element order where
required, object key mapping, and composite key column ordering. Empty membership sets must have explicit false/
empty-result semantics.
Rationale:
OA uses IN predicates for object keys, sibling/reference loading, bulk selects, and object-cache filtering. Wrong
expansion causes missing, duplicate, or overly broad results.
Source scope:
OAQueryTokenizer.evaluateB2, OAQueryTokenType.IN, tokens consumed by OAQueryFilter/select/datasource layers.
Related CODEX findings:
Parser has special handling comments for composite IN shapes; empty IN semantics noted in filter package.
Suggested unit tests:
testInListParameterPreservesValues, testCompositeInObjectKeyParameterPreservesColumnOrder,
testEmptyInListReturnsFalseOrDocumentedEmptyResult, testInHubParameterUsesObjectIdentitySemantics.
Spec target section:
Query Runtime / IN Parameter Semantics

QUERY-NULL-001 — Null Predicate Semantics
Contract statement:
Null comparisons must have explicit and consistent semantics across tokenization, in-memory filtering, datasource
conversion, and object-cache evaluation. IS NULL, IS NOT NULL, = null, and != null must map to documented OA
behavior.
Rationale:
Null criteria are core to optional references, object state, datasource values, and UI filters. Inconsistent null
semantics cause memory-vs-database result drift.
Source scope:
OAQueryTokenType.NULL, OAQueryTokenManager.getNext, OAQueryTokenType.EQUAL/NOTEQUAL, consumers in filter/select/dat
asource layers.
Related CODEX findings:
IS NOT NULL handling appears fragile/broken because IS is tokenized as EQUAL while later cleanup expected VARIABLE
"IS".
Suggested unit tests:
testEqualsNullSemantics, testNotEqualsNullSemantics, testIsNullSemanticsMatchDatasourceAndFilter,
testIsNotNullSemanticsMatchDatasourceAndFilter.
Spec target section:
Query Runtime / Null Semantics

QUERY-COMPARE-001 — Comparison Operator Semantics
Contract statement:
Equality, inequality, greater/less, greater-or-equal, less-or-equal, LIKE, NOT LIKE, and membership operators must
map to OACompare-compatible semantics for supported value types.
Rationale:
Query results must agree with Hub filters, datasource filtering, object matching, and in-memory selection.
Source scope:
OAQueryTokenType, OAQueryToken.isOperator, OAQueryTokenizer.evaluateB/evaluateB2, consumers in OAQueryFilter/select
/datasource.
Related CODEX findings:
NOTLIKE not treated as operator; <> not-equal not recognized.
Suggested unit tests:
testAllComparisonOperatorsProduceExpectedTokens, testNotLikeSemanticsMatchLikeNegation,
testNotEqualOperatorAliasesMatch, testComparisonSemanticsMatchOACompare.
Spec target section:
Query Runtime / Comparison Semantics

QUERY-CONVERT-001 — Literal Conversion Semantics
Contract statement:
Conversion of query literals and parameters before comparison must preserve target-property intent and semantic
value for strings, numbers, booleans, dates/times, enums, object references, keys, and nulls.
Rationale:
Conversion drift silently includes or excludes wrong objects and can make datasource-backed and in-memory query
behavior diverge.
Source scope:
OAQueryToken literal values, OAQueryTokenizer output, converter/filter/datasource consumers.
Related CODEX findings:
Boolean literals can differ between JDBC conversion and OAQueryFilter.
Suggested unit tests:
testBooleanLiteralQueryMatchesDatasourceAndFilter, testNumericLiteralPreservesPrecision,
testDateLiteralPreservesDateSemantics, testEnumLiteralUsesTargetEnumMetadata.
Spec target section:
Query Runtime / Conversion Semantics

QUERY-PASSTHRU-001 — Passthrough Boundary Semantics
Contract statement:
Passthrough query blocks must have explicit start/end boundaries and must remain opaque to normal query parsing only
after those boundaries are validly recognized.
Rationale:
Passthrough text intentionally bypasses OA query parsing. Unterminated or partially recognized passthrough text must
not become executable content silently.
Source scope:
OAQueryTokenManager.getNext, OAQueryTokenType.PASSTHRU handling, passthrough token consumers.
Related CODEX findings:
Unterminated PASS[...]THRU blocks accepted at EOF.
Suggested unit tests:
testPassthroughTokenPreservesBody, testRejectsUnterminatedPassthruBlock,
testPassthroughDoesNotConsumeFollowingExpression.
Spec target section:
Query Runtime / Passthrough Semantics

QUERY-STATE-001 — Parser State Isolation
Contract statement:
Tokenizer and token manager instances must not leak query text, position, current token, prior token, token vector,
or partial parse state across conversions.
Rationale:
Parser reuse must represent the current query only. Stale state causes partial results or tokens from prior queries.
Source scope:
OAQueryTokenizer.convertToTokens, OAQueryTokenizer.vec/token/lastToken, OAQueryTokenManager.setQuery,
OAQueryTokenManager.pos/query.
Related CODEX findings:
none observed.
Suggested unit tests:
testTokenizerReuseDoesNotLeakPriorQueryState, testTokenManagerResetClearsPriorPositionAndBuffer,
testFailedParseDoesNotPoisonNextParse.
Spec target section:
Query Runtime / Parser State Isolation

QUERY-REUSE-001 — Parsed Query Reuse And Immutability
Contract statement:
A parsed token stream or query criteria representation must be safe to interpret repeatedly as the same query, and
reparsing a new query must not reuse stale criteria, parameter values, paths, or compiled filter state.
Rationale:
OA select/filter retry behavior must represent the current query, not a previous one or partial setup.
Source scope:
OAQuery.parse, OAQueryTokenizer.convertToTokens, token vectors, consumer query/filter/select classes.
Related CODEX findings:
none in com.viaoa.query itself; partial parse exposure noted at consumer boundary.
Suggested unit tests:
testReparseWithDifferentParametersUsesNewValues, testParsedTokenStreamStableAcrossRepeatedConsumers,
testFailedParseDoesNotExposeReusablePartialCriteria.
Spec target section:
Query Runtime / Query Reuse

QUERY-THREAD-001 — Query Definition Sharing Semantics
Contract statement:
Query parser/tokenizer instances are not shared mutable runtime state unless explicitly synchronized; parsed query
definitions intended for reuse must be immutable or safely confined by consumers.
Rationale:
Queries may be used by Hubs, filters, selects, datasource operations, projections, and background runtime services.
Shared mutable parse state can corrupt selection behavior.
Source scope:
OAQueryTokenizer mutable fields, OAQueryTokenManager mutable fields, OAQueryToken mutable public fields, token
vectors.
Related CODEX findings:
none observed.
Suggested unit tests:
testIndependentTokenizerInstancesDoNotInterfere, testConcurrentParsingUsesSeparateInstances,
testParsedQueryReuseRequiresImmutableOrDocumentedOwnership.
Spec target section:
Query Runtime / Thread Safety Semantics

QUERY-FAIL-001 — Invalid Query Failure Visibility
Contract statement:
Malformed syntax, invalid operators, unterminated literals, missing operands, unknown token forms, missing
parameters, invalid paths, or unsupported structures must fail visibly or return a documented failure; they must not
silently degrade into broader, narrower, empty, or misleading results.
Rationale:
Silent wrong query results are production data correctness bugs and can corrupt Hub contents, object-cache views,
datasource results, projections, and digital twin runtime behavior.
Source scope:
OAQueryTokenizer.evaluate, OAQueryTokenManager.getNext, OAQuery.parse, consumer construction boundaries.
Related CODEX findings:
Unterminated literals accepted; trailing tokens ignored by consumer; missing params can become literal "?"; NOTLIKE/
operator issues.
Suggested unit tests:
testMalformedQueryThrows, testMissingRequiredOperandThrows, testInvalidTokenDoesNotReturnPartialCriteria,
testMissingRequiredParameterFails.
Spec target section:
Query Runtime / Failure Semantics

QUERY-STATE-002 — No Partial Query Commit
Contract statement:
Partial query setup is allowed only when the caller receives visible failure and no token stream, filter, select
criteria, or compiled query object is exposed as successful.
Rationale:
A partially compiled query that appears valid can corrupt Hub/select/filter state and can be retried with stale
criteria.
Source scope:
OAQueryTokenizer.convertToTokens, OAQuery.parse, consumer query/filter/select constructors.
Related CODEX findings:
Consumer can accept partial parse output in some cases.
Suggested unit tests:
testFailedTokenizationDoesNotReturnTokenVector, testFailedParseDoesNotExposeUsableQuery,
testFailedFilterParseDoesNotExposeUsableFilter.
Spec target section:
Query Runtime / Partial Progress Semantics

QUERY-DS-001 — Datasource And In-Memory Consistency
Contract statement:
Datasource-backed query execution, object-cache evaluation, Hub filtering, and in-memory query filters must
interpret the same query token stream according to the same OA semantic contract unless an execution layer
explicitly documents a narrower capability.
Rationale:
OA must avoid memory-vs-database drift for the same query expression.
Source scope:
OAQueryTokenizer, OAQueryTokenManager, OAQueryTokenType, OAQueryToken, consumers in OAQueryFilter, select,
datasource, path, metadata.
Related CODEX findings:
Boolean literal handling drift; OR/AND precedence drift; null handling drift.
Suggested unit tests:
testSameQueryReturnsSameObjectsFromDatasourceAndObjectCache, testSameQueryMatchesHubFilterAndSelectResults,
testDatasourceAndMemoryNullSemanticsMatch.
Spec target section:
Query Runtime / Cross-Package Compatibility

QUERY-IDENTITY-001 — Query Result Identity Contract
Contract statement:
Query criteria that resolve OAObjects must preserve OA identity semantics: the same datasource identity/key must map
to the authoritative cached OAObject instance where applicable.
Rationale:
Duplicate OAObjects for the same identity corrupt object graph, cache, Hub, sync, and serialization semantics.
Source scope:
com.viaoa.query criteria generation and token semantics; select/datasource/cache consumers.
Related CODEX findings:
none in com.viaoa.query itself.
Suggested unit tests:
testQueryResultUsesCachedObjectIdentity, testQueryDoesNotDuplicateObjectForSameKey,
testObjectReferenceCriteriaUsesOAKeySemantics.
Spec target section:
Query Runtime / Identity Semantics

QUERY-ORDER-001 — Ordering And Grouping Boundaries
Contract statement:
Ordering, grouping, or function syntax must be either parsed with deterministic semantics or rejected/left to a
documented execution layer; unsupported order/group forms must not be silently interpreted as ordinary predicates.
Rationale:
Hub loading, UI display, projections, datasource selection, and repeatable tests depend on stable ordering behavior
when query syntax claims to support it.
Source scope:
OAQueryTokenizer, OAQueryTokenType function/group/order-related tokens, datasource/select consumers.
Related CODEX findings:
none in com.viaoa.query itself.
Suggested unit tests:
testUnsupportedOrderByIsRejectedOrDelegatedByContract, testQueryFunctionTokensRemainStructurallyValid,
testQueryGroupSemanticsAreExplicitOrRejected.
Spec target section:
Query Runtime / Ordering and Grouping

QUERY-AUTHORITY-001 — Query Package Is Lexical And Parse Authority
Contract statement:
com.viaoa.query is the package authority for OA query tokenization and parse-level expression structure. Other
packages may translate or execute query tokens, but they must preserve token meaning unless their contract
explicitly narrows supported query syntax.
Rationale:
Queries are AI-readable and runtime-readable semantic contracts over executable blueprints. Central parse authority
prevents semantic drift across filters, datasource, Hubs, selects, projections, and graph services.
Source scope:
OAQuery, OAQueryTokenizer, OAQueryTokenManager, OAQueryToken, OAQueryTokenType, integration with filter/select/
datasource/path/metadata/runtime packages.
Related CODEX findings:
Operator classification, null semantics, passthrough, literal parsing, and parser completeness findings all
illustrate boundary authority.
Suggested unit tests:
testQueryTokenStreamMeaningPreservedByFilterConsumer, testQueryTokenStreamMeaningPreservedByDatasourceConsumer,
testUnsupportedConsumerFeatureFailsInsteadOfChangingTokenMeaning.
Spec target section:
Query Runtime / Cross-Package Authority

QUERY-DETERMINISM-001 — Same Inputs Produce Same Query Meaning
Contract statement:
For the same query text, parameters, root metadata, conversion rules, parser state, and execution context, OA query
parsing and interpretation must produce the same token stream, same semantic criteria, or the same visible failure.
Rationale:
Deterministic query behavior is required for generated runtime code, digital twin views, Hubs, object graphs,
datasource selections, projections, tests, and AI-readable runtime contracts.
Source scope:
All public behavior in OAQuery, OAQueryTokenizer, OAQueryTokenManager, OAQueryToken, OAQueryTokenType, and direct
consumers.
Related CODEX findings:
Default-locale keyword uppercasing, malformed literal acceptance, and partial parse behavior can threaten
deterministic query meaning.
Suggested unit tests:
testSameQueryParsesToSameTokensRepeatedly, testKeywordTokenizationStableUnderTurkishLocale,
testSameInvalidQueryFailsConsistently, testSameQuerySameParametersProducesSameCriteria.
Spec target section:
Query Runtime / Determinism

*/



