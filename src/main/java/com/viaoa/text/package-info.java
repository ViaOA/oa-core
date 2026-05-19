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
 * <!-- OA Text Responsibility Chart -->
 *
 * <p>
 * The {@code com.viaoa.text} package provides modular text functionality organized
 * by clear domain responsibilities. Each class handles a specific concern so that
 * text processing remains predictable, maintainable, and discoverable.
 * </p>
 *
 * <table border="1" cellpadding="4" cellspacing="0" summary="OA Text Responsibility Chart">
 *   <tr>
 *     <th>Concern</th>
 *     <th>Description</th>
 *     <th>Examples</th>
 *     <th>Module</th>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Sanitizing Input</b></td>
 *     <td>Validates whether input is safe/usable</td>
 *     <td>{@code isEmpty()}, {@code notEmpty()}, {@code safeTrim()}, {@code toNonNull()}</td>
 *     <td>{@link com.viaoa.text.OATextSanitize}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Character Classification</b></td>
 *     <td>Detects which types of characters appear in text</td>
 *     <td>{@code hasDigits()}, {@code isAlpha()}, {@code isAlphanumeric()}</td>
 *     <td>{@link com.viaoa.text.OATextChars}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Text Comparison & Matching</b></td>
 *     <td>Partial or full matching between text values</td>
 *     <td>{@code isEqual()}, {@code contains()}, {@code indexOf()},
 *         {@code startsWith()}, {@code endsWith()}</td>
 *     <td>{@link com.viaoa.text.OATextCompare}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Tokenizing / Parsing</b></td>
 *     <td>Splits and processes structured text</td>
 *     <td>{@code fieldAt()}, {@code count()}, {@code parseLine()}, {@code csv()}</td>
 *     <td>{@link com.viaoa.text.OATextTokenizer}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Escape / Encode / Decode</b></td>
 *     <td>Makes content safe for HTML, XML, JSON, etc.</td>
 *     <td>{@code convertToXml()}, {@code escapeHTML()}, {@code escapeJSON()}</td>
 *     <td>{@link com.viaoa.text.OATextEscape}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Format Interpretation</b></td>
 *     <td>Identifies or produces a formatted representation</td>
 *     <td>{@code isDate()}, {@code isNumber()}, {@code mask()}, {@code fmt()}</td>
 *     <td>{@link com.viaoa.text.OATextFormat}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Grammar & Semantics</b></td>
 *     <td>Applies linguistic rules to words</td>
 *     <td>{@code toPlural()}, {@code toSingular()}, {@code toTitleCase()}</td>
 *     <td>{@link com.viaoa.text.OATextGrammar}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Alignment & Layout</b></td>
 *     <td>Adjusts visible positioning and column width</td>
 *     <td>{@code padStart()}, {@code truncate()}, {@code alignCenter()}</td>
 *     <td>{@link com.viaoa.text.OATextAlign}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Wrapping</b></td>
 *     <td>Breaks text into lines based on width rules</td>
 *     <td>{@code wrap()}, whitespace/hyphenation breaking</td>
 *     <td>{@link com.viaoa.text.OATextLineWrap}</td>
 *   </tr>
 *
 *   <tr>
 *     <td><b>Misc. Utilities</b></td>
 *     <td>Rare helpers not belonging to any other category</td>
 *     <td>{@code repeat()}, {@code reverse()}</td>
 *     <td>{@link com.viaoa.text.OATextUtil}</td>
 *   </tr>
 * </table>
 *
 * <p>
 * The {@link com.viaoa.lang.OAString} class acts as the primary public facade,
 * delegating functionality to the appropriate module in this package.
 * </p>
 */
package com.viaoa.text;


/* CODEX Invariants


1. Text Formatting Contracts

  TEXT-FORMAT-001 — Format Helpers Must Produce Deterministic Text
  Contract statement: Given the same input value, format string, locale-relevant assumptions, and OA settings, text
  formatting helpers must produce the same output.
  Rationale: OA text utilities feed generated UI labels, reports, logs, templates, and code generation.
  Source locations: com.viaoa.text.* formatting classes/utilities.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFormatSameInputSameOutput(), testFormatKnownDateNumberPatterns()
  Spec target section: Text Utilities / Formatting Semantics

  TEXT-FORMAT-002 — Formatting Must Not Silently Return Misleading Output On Invalid Pattern
  Contract statement: If a format pattern cannot be applied, the utility must either return a clearly defined
  fallback or fail visibly; it must not silently produce unrelated output.
  Rationale: Silent wrong output is worse than visible failure in generated UI/reports.
  Source locations: formatter helpers, template/text generation utilities.
  Known related CODEX findings: swallowed-exception wrong-output risks were part of the scan.
  Suggested unit tests: testInvalidFormatPatternUsesDefinedFallback(),
  testInvalidFormatPatternDoesNotReturnMisleadingValue()
  Spec target section: Text Utilities / Formatting Failure Semantics

  2. Display Name / Grammar Contracts

  TEXT-GRAMMAR-001 — Display Name Generation Is Stable And Human-Oriented
  Contract statement: Converting property/class/code names into display text must produce stable, readable labels
  without losing meaningful word boundaries.
  Rationale: OA model designer, generated UI, and reports depend on predictable labels.
  Source locations: display-name/name-conversion helpers in com.viaoa.text.*.
  Known related CODEX findings: none observed.
  Suggested unit tests: testCamelCaseToDisplayName(), testAcronymDisplayNamePreservedByContract()
  Spec target section: Text Utilities / Display Name Semantics

  TEXT-GRAMMAR-002 — Singular/Plural/Article Helpers Must Follow Defined OA Grammar Rules
  Contract statement: Grammar helpers must consistently apply OA’s defined singular, plural, capitalization, and
  article rules.
  Rationale: Generated UI text and code generation use these helpers for names and messages.
  Source locations: grammar/name conversion utilities.
  Known related CODEX findings: none observed.
  Suggested unit tests: testPluralizeRegularWord(), testPluralizeConfiguredIrregularWord(),
  testArticleForVowelSoundByContract()
  Spec target section: Text Utilities / Grammar Semantics

  TEXT-GRAMMAR-003 — Code/Text Name Conversion Must Preserve Legal Identifiers Where Required
  Contract statement: Helpers that generate code identifiers from text must return valid identifiers or a defined
  fallback.
  Rationale: OA code generation cannot emit invalid Java/property names.
  Source locations: code/text generation helper classes.
  Known related CODEX findings: none observed.
  Suggested unit tests: testTextToJavaIdentifierRemovesInvalidChars(),
  testEmptyTextToIdentifierUsesDefinedFallback()
  Spec target section: Text Utilities / Code Generation Names

  3. Whitespace / Normalization Contracts

  TEXT-NORMALIZE-001 — Whitespace Normalization Must Be Predictable
  Contract statement: Whitespace normalization helpers must define whether they trim, collapse, preserve line
  breaks, or preserve internal spacing.
  Rationale: Text utilities are used in generated UI, templates, search, and reports where whitespace changes are
  observable.
  Source locations: whitespace/trim/normalize helpers.
  Known related CODEX findings: none observed.
  Suggested unit tests: testNormalizeWhitespaceCollapsesRunsWhenConfigured(), testTrimPreservesInternalSpacing()
  Spec target section: Text Utilities / Whitespace Semantics

  TEXT-NORMALIZE-002 — Empty Result Must Be Distinguished From Null When Contract Requires
  Contract statement: Normalization helpers must consistently distinguish null input from an empty normalized string
  where the API contract says they differ.
  Rationale: OA often treats null, empty, and blank differently for display/query/filter behavior.
  Source locations: null/empty/string normalization helpers.
  Known related CODEX findings: null/empty handling was a scan focus.
  Suggested unit tests: testNormalizeNullReturnsConfiguredNullOrEmpty(), testNormalizeBlankStringByContract()
  Spec target section: Text Utilities / Null Empty Normalization

  4. Escaping / Unescaping Contracts

  TEXT-ESCAPE-001 — Escape/Unescape Round Trip Must Preserve Text
  Contract statement: For supported character sets and escape modes, unescape(escape(text)) must equal the original
  text.
  Rationale: Templates, generated code, HTML/XML/CSV-like text, and persistence helpers rely on reversible escaping.
  Source locations: escaping/unescaping helpers in com.viaoa.text.*.
  Known related CODEX findings: escaping/unescaping wrong-output risks were scanned.
  Suggested unit tests: testEscapeUnescapeRoundTripAscii(),
  testEscapeUnescapeRoundTripQuotesBackslashesAndNewlines()
  Spec target section: Text Utilities / Escaping Semantics

  TEXT-ESCAPE-002 — Escaping Must Not Double-Escape Already Escaped Text Unless Requested
  Contract statement: Escape helpers must define and honor whether input is raw text or may already contain escaped
  sequences.
  Rationale: Double escaping produces visible wrong output in templates, HTML, generated code, and UI.
  Source locations: escaping helpers.
  Known related CODEX findings: none observed.
  Suggested unit tests: testEscapeRawTextEscapesReservedChars(), testEscapeAlreadyEscapedTextByContract()
  Spec target section: Text Utilities / Escaping Idempotence

  TEXT-ESCAPE-003 — Unescape Must Not Swallow Invalid Escape Into Wrong Text
  Contract statement: Invalid or incomplete escape sequences must use a defined fallback or remain literal; they
  must not silently become unrelated characters.
  Rationale: Wrong text output is a correctness bug for generated content.
  Source locations: unescaping helpers.
  Known related CODEX findings: malformed parsing/wrong-output risks were scanned.
  Suggested unit tests: testIncompleteEscapeRemainsLiteralByContract(), testInvalidEscapeDoesNotDropCharacters()
  Spec target section: Text Utilities / Unescape Failure Semantics

  5. Sanitization Contracts

  TEXT-SANITIZE-001 — Sanitization Removes Or Encodes Only The Targeted Unsafe Content
  Contract statement: Sanitizers must remove/encode the content they are designed to handle while preserving safe
  text.
  Rationale: Over-sanitizing loses user data; under-sanitizing emits unsafe/generated-invalid text.
  Source locations: sanitization helpers.
  Known related CODEX findings: sanitization mistakes were scan targets.
  Suggested unit tests: testSanitizePreservesSafeText(), testSanitizeRemovesTargetUnsafeCharacters()
  Spec target section: Text Utilities / Sanitization Semantics

  TEXT-SANITIZE-002 — Sanitization Must Be Context-Specific
  Contract statement: HTML, XML, regex, Java string, SQL-like, filename, and display sanitization rules must not be
  treated as interchangeable.
  Rationale: Each output context has different reserved characters and escaping rules.
  Source locations: context-specific escape/sanitize helpers.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHtmlSanitizeDoesNotUseJavaStringEscapes(), testRegexEscapeEscapesRegexMetacharacters()
  Spec target section: Text Utilities / Contextual Sanitization

  6. Tokenizer / Regex Contracts

  TEXT-TOKENIZER-001 — Tokenizer Must Always Advance Or Terminate
  Contract statement: Tokenizer/parsing helpers must consume input, advance position, or terminate on every loop
  iteration.
  Rationale: Infinite loops are high-risk utility bugs.
  Source locations: tokenizer/parser utilities in com.viaoa.text.*.
  Known related CODEX findings: infinite-loop/tokenizer risks were part of the scan.
  Suggested unit tests: testTokenizerTerminatesOnEmptyInput(), testTokenizerTerminatesOnRepeatedDelimiter(),
  testTokenizerTerminatesOnMalformedToken()
  Spec target section: Text Utilities / Tokenizer Progress

  TEXT-TOKENIZER-002 — Delimiters Must Be Matched According To Documented Rules
  Contract statement: Tokenizers must consistently define whether delimiters are literal, escaped, nested, quoted,
  or regex-based.
  Rationale: Templates and generated text fail when delimiter matching is ambiguous.
  Source locations: tokenizer/template parsing helpers.
  Known related CODEX findings: delimiter/index bugs were scan targets.
  Suggested unit tests: testTokenizerParsesQuotedDelimiter(), testTokenizerHandlesEscapedDelimiter(),
  testTokenizerHandlesMissingClosingDelimiterByContract()
  Spec target section: Text Utilities / Tokenizer Delimiters

  TEXT-REGEX-001 — Regex Helpers Must Escape Literal Input Before Regex Use
  Contract statement: Helpers that build regex from literal text must quote/escape regex metacharacters unless the
  API explicitly accepts regex input.
  Rationale: Treating user/model text as regex changes matching behavior and can produce wrong replacements.
  Source locations: regex helper methods/classes.
  Known related CODEX findings: regex helper risks were part of scan focus.
  Suggested unit tests: testRegexLiteralDotMatchesDotOnly(), testRegexLiteralBracketEscaped()
  Spec target section: Text Utilities / Regex Semantics

  7. Line Wrap / Alignment Contracts

  TEXT-WRAP-001 — Line Wrapping Must Respect Width Without Losing Characters
  Contract statement: Wrapping helpers must not drop, duplicate, or reorder characters while enforcing the
  configured width as closely as the contract allows.
  Rationale: Reports, generated UI text, emails, and logs depend on readable and complete wrapped text.
  Source locations: line wrapping/alignment helpers.
  Known related CODEX findings: substring/index bugs were scan targets.
  Suggested unit tests: testWrapPreservesAllCharacters(), testWrapLongWordByContract(),
  testWrapAtWhitespaceWhenPossible()
  Spec target section: Text Utilities / Line Wrap Semantics

  TEXT-ALIGN-001 — Alignment/Padding Must Produce Defined Width For Normal Input
  Contract statement: Left/right/center padding helpers must produce strings of the requested width unless the input
  is longer and the contract says to preserve or truncate.
  Rationale: Fixed-width text/report output requires deterministic alignment.
  Source locations: padding/alignment helpers.
  Known related CODEX findings: off-by-one/index issues were scan targets.
  Suggested unit tests: testPadLeftProducesRequestedWidth(), testPadRightProducesRequestedWidth(),
  testCenterPadHandlesOddPadding()
  Spec target section: Text Utilities / Alignment Semantics

  8. Text Comparison Contracts

  TEXT-COMPARE-001 — Text Comparison Must Define Case And Null Semantics
  Contract statement: Text comparison helpers must define whether comparison is case-sensitive, case-insensitive,
  trimmed, null-safe, or locale-sensitive.
  Rationale: Sorting/filter/search behavior depends on consistent comparison semantics.
  Source locations: comparison/search helper methods.
  Known related CODEX findings: none observed.
  Suggested unit tests: testCompareNullByContract(), testCompareCaseInsensitiveByContract(),
  testCompareTrimmedByContract()
  Spec target section: Text Utilities / Comparison Semantics

  TEXT-COMPARE-002 — Search/Contains Helpers Must Not Treat Missing Text As Found
  Contract statement: Search helpers must not return success for missing needles/patterns except where wildcard/
  empty semantics explicitly define that behavior.
  Rationale: False positives corrupt filtering, formatting, and template expansion.
  Source locations: search/contains/match helpers.
  Known related CODEX findings: false-success/wrong-output risks were scanned.
  Suggested unit tests: testContainsMissingTextReturnsFalse(), testEmptyNeedleBehaviorByContract()
  Spec target section: Text Utilities / Search Semantics

  9. Null / Empty Handling Contracts

  TEXT-NULL-001 — Null Input Behavior Must Be Explicit Per API
  Contract statement: Every text helper must consistently define whether null input returns null, empty string,
  false, zero, or throws.
  Rationale: OA uses text utilities broadly; inconsistent null behavior creates NPEs or wrong generated output.
  Source locations: all com.viaoa.text.* helpers.
  Known related CODEX findings: null handling was a primary scan focus.
  Suggested unit tests: testNullInputReturnsDefinedValueForFormatHelpers(),
  testNullInputReturnsDefinedValueForEscapeHelpers()
  Spec target section: Text Utilities / Null Semantics

  TEXT-EMPTY-001 — Empty And Blank Strings Must Follow Defined Semantics
  Contract statement: Empty string and whitespace-only string behavior must be explicit and consistent for
  formatting, comparison, tokenization, and normalization APIs.
  Rationale: OA display, query, and template paths frequently distinguish blank from absent.
  Source locations: null/empty/string helpers.
  Known related CODEX findings: empty-string edge cases were scan targets.
  Suggested unit tests: testEmptyStringByContract(), testBlankStringByContract(),
  testWhitespaceOnlyTokenizationByContract()
  Spec target section: Text Utilities / Empty String Semantics

  TEXT-FAILURE-001 — Text Utilities Must Not Silently Produce Wrong Output
  Contract statement: When a text utility cannot parse, format, tokenize, escape, or wrap according to contract, it
  must return a defined fallback or fail visibly.
  Rationale: Silent wrong text output can break generated code, UI, reports, and templates.
  Source locations: parsers, tokenizers, formatters, escaping helpers.
  Known related CODEX findings: swallowed exception / silent wrong-output risks were scan targets.
  Suggested unit tests: testParserFailureUsesDefinedFallback(), testTokenizerFailureDoesNotReturnPartialSuccess(),
  testFormatterFailureDoesNotReturnMisleadingText()
  Spec target section: Text Utilities / Failure Semantics

  10. Test Coverage Matrix

  Formatting:

  - testFormatSameInputSameOutput
  - testFormatKnownDateNumberPatterns
  - testInvalidFormatPatternUsesDefinedFallback
  - testInvalidFormatPatternDoesNotReturnMisleadingValue

  Display/grammar/code names:

  - testCamelCaseToDisplayName
  - testAcronymDisplayNamePreservedByContract
  - testPluralizeRegularWord
  - testPluralizeConfiguredIrregularWord
  - testTextToJavaIdentifierRemovesInvalidChars
  - testEmptyTextToIdentifierUsesDefinedFallback

  Whitespace/null/empty:

  - testNormalizeWhitespaceCollapsesRunsWhenConfigured
  - testTrimPreservesInternalSpacing
  - testNormalizeNullReturnsConfiguredNullOrEmpty
  - testNormalizeBlankStringByContract
  - testEmptyStringByContract
  - testWhitespaceOnlyTokenizationByContract

  Escaping/sanitization:

  - testEscapeUnescapeRoundTripAscii
  - testEscapeUnescapeRoundTripQuotesBackslashesAndNewlines
  - testEscapeAlreadyEscapedTextByContract
  - testIncompleteEscapeRemainsLiteralByContract
  - testInvalidEscapeDoesNotDropCharacters
  - testSanitizePreservesSafeText
  - testSanitizeRemovesTargetUnsafeCharacters
  - testRegexEscapeEscapesRegexMetacharacters

  Tokenizer/regex:

  - testTokenizerTerminatesOnEmptyInput
  - testTokenizerTerminatesOnRepeatedDelimiter
  - testTokenizerTerminatesOnMalformedToken
  - testTokenizerParsesQuotedDelimiter
  - testTokenizerHandlesEscapedDelimiter
  - testTokenizerHandlesMissingClosingDelimiterByContract
  - testRegexLiteralDotMatchesDotOnly

  Wrap/alignment:

  - testWrapPreservesAllCharacters
  - testWrapLongWordByContract
  - testWrapAtWhitespaceWhenPossible
  - testPadLeftProducesRequestedWidth
  - testPadRightProducesRequestedWidth
  - testCenterPadHandlesOddPadding

  Comparison/failure:

  - testCompareNullByContract
  - testCompareCaseInsensitiveByContract
  - testContainsMissingTextReturnsFalse
  - testEmptyNeedleBehaviorByContract
  - testParserFailureUsesDefinedFallback
  - testTokenizerFailureDoesNotReturnPartialSuccess
  - testFormatterFailureDoesNotReturnMisleadingText


*/


