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

/* CODEX Invariants

ID: QUERY-SEMANTIC-001
  Contract statement: Query criteria must preserve the semantic intent expressed by OA property paths, metadata,
  parameters, and operators.
  Rationale: OA queries drive datasource selects, in-memory filters, Hub contents, and object matching. A parsed query
  must mean the same thing throughout OA.
  Source locations: OAQuery, OAQueryTokenizer, OAQueryTokenManager, OAQueryToken, OAQueryTokenType, consumers such as
  OAQueryFilter and select/datasource layers.
  Related CODEX findings: OR/AND precedence drift in OAQueryFilter; malformed/trailing tokens accepted by filter
  consumer.
  Suggested unit tests: testQueryCriteriaMeaningIsPreservedAcrossParserAndFilter,
  testEquivalentDatasourceAndMemoryQuerySemantics
  Spec target section: Query Runtime / Criteria Semantics

  ID: QUERY-TOKEN-001
  Contract statement: Tokenization must classify every supported query operator, literal, keyword, separator, and
  placeholder into the correct token type.
  Rationale: All downstream query behavior depends on token identity. A wrong token type changes filter/query meaning
  before metadata or datasource code sees it.
  Source locations: OAQueryTokenManager.getNext(), OAQueryTokenType, OAQueryToken.isOperator()
  Related CODEX findings: NOTLIKE not included in isOperator(); IS NOT NULL tokenization fragile; <> not-equal not
  recognized.
  Suggested unit tests: testTokenizesAllComparisonOperators, testTokenizesLikeAndNotLikeOperators,
  testTokenizesIsNullAndIsNotNull
  Spec target section: Query Runtime / Tokenization

  ID: QUERY-PARSE-001
  Contract statement: Parsing must consume the complete query expression or fail visibly.
  Rationale: Accepting only a leading valid fragment silently broadens or narrows results.
  Source locations: OAQueryTokenizer.convertToTokens(...), OAQueryTokenizer.evaluate(), OAQueryFilter.parseBlock() as
  a consumer.
  Related CODEX findings: trailing valid/invalid tokens can be ignored by OAQueryFilter.
  Suggested unit tests: testParserRejectsTrailingTokens, testFilterParserRejectsLeadingExpressionWithGarbageTail
  Spec target section: Query Runtime / Parse Completeness

  ID: QUERY-PARSE-002
  Contract statement: Malformed quoted, escaped, or passthrough query blocks must fail visibly rather than becoming
  valid partial tokens.
  Rationale: Strings and passthrough blocks can materially change query results. Unterminated input must not become a
  different predicate.
  Source locations: OAQueryTokenManager.getNext()
  Related CODEX findings: unterminated quoted strings accepted; unterminated PASS[...]THRU accepted.
  Suggested unit tests: testRejectsUnterminatedSingleQuotedString, testRejectsUnterminatedDoubleQuotedString,
  testRejectsUnterminatedPassthruBlock
  Spec target section: Query Runtime / Literal Parsing

  ID: QUERY-PATH-001
  Contract statement: Query property/path references must resolve according to OA metadata and OAPath semantics.
  Rationale: Wrong path resolution returns wrong objects, wrong Hub contents, and wrong datasource rows.
  Source locations: query tokens consumed by OAQueryFilter, OAPath, datasource/select query conversion layers.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testQueryPathResolvesMetadataProperty, testQueryPathRejectsUnknownProperty,
  testQueryPathMatchesOAPathTraversal
  Spec target section: Query Runtime / Path Resolution

  ID: QUERY-METADATA-001
  Contract statement: Query interpretation must honor OA metadata for property type, ID/key behavior, link
  cardinality, and datasource mapping.
  Rationale: Metadata is the runtime truth for how criteria map to objects and persistence.
  Source locations: OAQueryToken, OAQueryTokenizer, consumers in filter/select/datasource packages.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testQueryUsesMetadataPropertyTypeForComparison, testQueryUsesIdMetadataForObjectKeyCriteria,
  testQueryRejectsMetadataMismatch
  Spec target section: Query Runtime / Metadata Alignment

  ID: QUERY-PARAM-001
  Contract statement: Positional parameters must bind in query order, exactly once each, preserving value identity and
  intended type.
  Rationale: Wrong parameter order or reuse changes query meaning and can return incorrect rows or objects.
  Source locations: OAQueryTokenType.QUESTION, OAQueryTokenManager.getNext(), consumer binding in OAQueryFilter and
  datasource/select layers.
  Related CODEX findings: missing ? args can become literal "?" in OAQueryFilter.
  Suggested unit tests: testQuestionParametersBindInOrder, testMissingParameterFailsVisibly,
  testExtraParameterFailsOrIsExplicitlyContracted
  Spec target section: Query Runtime / Parameter Binding

  ID: QUERY-PARAM-002
  Contract statement: Collection/list parameters used for IN (?) must preserve membership semantics and must not
  silently expand to the wrong comparison shape.
  Rationale: OA uses IN (?) for object keys, sibling/reference loading, and bulk selects. Wrong expansion causes
  missing or duplicate results.
  Source locations: OAQueryTokenizer.evaluateB2(), OAQueryTokenType.IN, consumers in OAQueryFilter and datasource/
  select layers.
  Related CODEX findings: parser has special handling comments for composite IN shapes.
  Suggested unit tests: testInListParameterPreservesValues, testCompositeInObjectKeyParameterPreservesColumnOrder,
  testEmptyInListReturnsFalseOrDocumentedEmptyResult
  Spec target section: Query Runtime / IN Parameter Semantics

  ID: QUERY-NULL-001
  Contract statement: Null comparisons must have explicit, consistent semantics across tokenization, in-memory
  filtering, and datasource conversion.
  Rationale: null criteria are core for object state, optional references, and datasource values. Inconsistent null
  semantics cause memory-vs-database result drift.
  Source locations: OAQueryTokenType.NULL, OAQueryTokenManager.getNext(), OAQueryTokenType.EQUAL,
  OAQueryTokenType.NOTEQUAL
  Related CODEX findings: IS NOT NULL handling fragile/broken.
  Suggested unit tests: testEqualsNullSemantics, testNotEqualsNullSemantics,
  testIsNullAndIsNotNullSemanticsMatchDatasourceAndFilter
  Spec target section: Query Runtime / Null Semantics

  ID: QUERY-COMPARE-001
  Contract statement: Query comparison operators must map to OA comparison semantics consistently for equality,
  inequality, greater/less, like/not-like, and membership.
  Rationale: OA query results must agree with Hub filters, datasource filtering, and object matching.
  Source locations: OAQueryTokenType, OAQueryToken.isOperator(), OAQueryTokenizer.evaluateB(), consumers in
  OAQueryFilter.
  Related CODEX findings: NOTLIKE not treated as operator; <> not-equal not recognized.
  Suggested unit tests: testAllComparisonOperatorsProduceExpectedTokenAndFilter,
  testNotLikeSemanticsMatchLikeNegation, testNotEqualOperatorAliasesMatch
  Spec target section: Query Runtime / Comparison Semantics

  ID: QUERY-CONVERT-001
  Contract statement: Conversion before comparison must preserve semantic value and target-property intent.
  Rationale: Query strings often compare dates, numbers, booleans, enums, and object keys. Conversion drift can
  silently include or exclude wrong objects.
  Source locations: query tokens and values from OAQueryTokenManager; consumers in converter/filter/datasource layers.
  Related CODEX findings: boolean literals can differ between JDBC conversion and OAQueryFilter.
  Suggested unit tests: testBooleanLiteralQueryMatchesDatasourceAndFilter, testNumericLiteralPreservesPrecision,
  testDateLiteralPreservesDateSemantics
  Spec target section: Query Runtime / Conversion Semantics

  ID: QUERY-STRING-001
  Contract statement: String literal parsing must preserve intended characters, including embedded quotes and escape
  forms, without adding or removing semantic characters.
  Rationale: Query values often represent user data, codes, names, and IDs. Literal corruption causes wrong matches.
  Source locations: OAQueryTokenManager.getNext(), OAQueryTokenizer.evaluateD(), OAQueryTokenizer.evaluateE()
  Related CODEX findings: unterminated strings accepted; quote-handling paths need coverage.
  Suggested unit tests: testSingleQuotedStringLiteral, testDoubleQuotedStringLiteral, testEmbeddedSingleQuoteLiteral,
  testEscapedStringLiteral
  Spec target section: Query Runtime / String Literal Semantics

  ID: QUERY-ORDER-001
  Contract statement: Ordering and grouping behavior must be deterministic wherever OA depends on sorted or grouped
  query results.
  Rationale: Hub loading, UI display, datasource selection, and repeatable tests depend on stable ordering.
  Source locations: com.viaoa.query parser if order/group syntax is introduced; select/datasource layers currently own
  most order behavior.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testQueryOrderByProducesDeterministicOrder, testQueryGroupSemanticsAreExplicitOrRejected
  Spec target section: Query Runtime / Ordering and Grouping

  ID: QUERY-STATE-001
  Contract statement: Query tokenizer/parser instances must not leak token, position, vector, or query text state
  across conversions.
  Rationale: Query objects can be reused, and stale parse state can create wrong tokens or partial results.
  Source locations: OAQueryTokenizer.convertToTokens(...), OAQueryTokenManager.setQuery(...), OAQueryTokenManager.pos,
  OAQueryTokenizer.vec, OAQueryTokenizer.token
  Related CODEX findings: none observed.
  Suggested unit tests: testTokenizerReuseDoesNotLeakPriorQueryState,
  testTokenManagerResetClearsPriorPositionAndBuffer
  Spec target section: Query Runtime / Parser State Isolation

  ID: QUERY-STATE-002
  Contract statement: Query result/criteria objects must not reuse stale criteria, parameter values, paths, or
  compiled filter state across retry or reparse.
  Rationale: OA select/filter retry behavior must represent the current query, not a previous one.
  Source locations: OAQuery.parse(...), OAQueryTokenizer.convertToTokens(...), consumer query/filter/select classes.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testReparseWithDifferentParametersUsesNewValues, testFailedParseDoesNotPoisonNextParse
  Spec target section: Query Runtime / Query Reuse

  ID: QUERY-ITERATOR-001
  Contract statement: Query iteration resources must be owned and closed by select/datasource layers on success,
  failure, and cancellation.
  Rationale: Query parsing feeds datasource execution; runtime correctness depends on result iterators not leaking
  resources.
  Source locations: com.viaoa.query produces criteria; resource ownership is in select/datasource packages.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testQueryIteratorClosesOnExhaustion, testQueryIteratorClosesOnException,
  testCancelledQueryReleasesResources
  Spec target section: Query Runtime / Iterator Resource Semantics

  ID: QUERY-FAIL-001
  Contract statement: Query failures must fail visibly and must not silently degrade into broader, narrower, empty, or
  misleading results.
  Rationale: Silent wrong query results are production data correctness bugs.
  Source locations: OAQueryTokenizer.evaluate(), OAQueryTokenManager.getNext(), OAQuery.parse(...)
  Related CODEX findings: unterminated literals accepted; trailing tokens ignored by consumer; missing params can
  become literal "?".
  Suggested unit tests: testMalformedQueryThrows, testMissingRequiredOperandThrows,
  testInvalidTokenDoesNotReturnPartialCriteria
  Spec target section: Query Runtime / Failure Semantics

  ID: QUERY-FAIL-002
  Contract statement: Partial query setup is allowed only when the caller receives a visible exception and no compiled
  query/filter is exposed as successful.
  Rationale: A partially compiled query that appears valid can corrupt Hub/select/filter state.
  Source locations: OAQueryTokenizer.convertToTokens(...), OAQuery.parse(...), consumer constructors.
  Related CODEX findings: consumer can accept partial parse output in some cases.
  Suggested unit tests: testFailedTokenizationDoesNotReturnTokenVector, testFailedFilterParseDoesNotExposeUsableFilter
  Spec target section: Query Runtime / Partial Setup

  ID: QUERY-DS-001
  Contract statement: Datasource, select, filter, finder, metadata/path, and Hub loading consumers must interpret the
  same query token stream consistently.
  Rationale: OA must avoid memory-vs-database drift for the same query text.
  Source locations: OAQueryTokenizer, OAQueryTokenManager, OAQueryTokenType, consumers in OAQueryFilter, select,
  datasource, path, metadata.
  Related CODEX findings: boolean literal handling drift; OR/AND precedence drift; null handling drift.
  Suggested unit tests: testSameQueryReturnsSameObjectsFromDatasourceAndObjectCache,
  testSameQueryMatchesHubFilterAndSelectResults
  Spec target section: Query Runtime / Cross-Package Compatibility

  ID: QUERY-IDENTITY-001
  Contract statement: Query results must preserve OA object identity: the same datasource identity/key must map to the
  authoritative cached OAObject instance where applicable.
  Rationale: Duplicate objects for the same identity corrupt graph/cache/Hub semantics.
  Source locations: com.viaoa.query criteria generation; select/datasource/cache consumers.
  Related CODEX findings: none observed in com.viaoa.query itself.
  Suggested unit tests: testQueryResultUsesCachedObjectIdentity, testQueryDoesNotDuplicateObjectForSameKey
  Spec target section: Query Runtime / Identity Semantics

  Suggested package-level spec summary

  - com.viaoa.query owns OA’s lightweight query lexical and parse contracts.
  - It translates query text into stable token streams that select, datasource, filter, finder, metadata/path, and Hub
    code can interpret consistently.
  - It must preserve semantic intent for property paths, operators, literals, nulls, parameters, and IN expressions.
  - It must reject malformed or incomplete query text visibly.
  - It must never silently accept partial expressions, unterminated literals, unknown operator forms, or stale parser
    state as successful queries.
  - Parameter binding must preserve order, identity, type, and collection semantics.
  - Null, boolean, numeric, date/time, and string comparison behavior must remain consistent with com.viaoa.compare
    and com.viaoa.converter.
  - Datasource-backed query behavior and in-memory filter behavior must not drift for the same OA query.
  - Query parsing should remain resource-neutral; iterator/resource ownership belongs to select/datasource execution
    layers.
  - Future unit tests should cover tokenization, parse completeness, parameter binding, null semantics, operator
    aliases, memory-vs-datasource consistency, and parser reuse after failure.



*/




