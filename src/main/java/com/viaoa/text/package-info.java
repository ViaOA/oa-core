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

//CODEX unit tests 20260527

/* CODEX Invariants

TEXT-DETERMINISM-001 — Deterministic Text Transformations
Contract statement:
For the same input text, options, locale assumptions, and OA runtime settings, text helpers must produce the same
output or the same documented failure/fallback result.
Rationale:
OA text output feeds UI labels, reports, templates, logs, generated code, query/path support, serialization
boundaries, and AI-readable runtime descriptions.
Source scope:
All com.viaoa.text helpers, especially OATextFormat, OATextGrammar, OATextCompare, OATextTokenizer, OATextEscape,
OATextLineWrap, OATextUtil, IndentFormatter.
Related CODEX findings:
Package-wide findings involving wrong parsing, wrong escaping, wrong wrapping, wrong validation, corrupted
conversion, and silent wrong output.
Suggested unit tests:
testSameInputProducesSameTextOutput, testRepresentativeHelpersAreDeterministic.
Spec target section:
Text Utilities / Deterministic Runtime Semantics

TEXT-NULL-001 — Null Input Semantics
Contract statement:
Each text helper API family must define whether null input returns null, empty string, false, zero, a fallback
value, or visible failure, and must apply that rule consistently.
Rationale:
Text helpers are used throughout nullable OA display, template, logging, query, serialization, and generated-code
paths.
Source scope:
OATextSanitize, OATextEscape, OATextFormat, OATextCompare, OATextFilter, OATextTokenizer, OATextLineWrap,
OATextUtil.
Related CODEX findings:
OATextEscape.escape null input; OATextFormat.unindent null input; null handling scan findings.
Suggested unit tests:
testNullInputBehaviorByApiFamily, testNullInputReturnsDefinedValueForEscapeHelpers,
testNullInputReturnsDefinedValueForFormatHelpers.
Spec target section:
Text Utilities / Null Semantics

TEXT-EMPTY-001 — Empty And Blank Text Semantics
Contract statement:
Empty strings and whitespace-only strings must have explicit, stable behavior for formatting, comparison,
tokenization, filtering, generation, sanitization, wrapping, and normalization APIs.
Rationale:
OA distinguishes absent, empty, blank, and meaningful text in display, query, filter, metadata, and template
behavior.
Source scope:
All com.viaoa.text helpers, especially OATextSanitize.isEmpty/notEmpty, OATextTokenizer, OATextFormat, OATextCompar
e, OATextLineWrap.
Related CODEX findings:
Empty-string and blank-string edge cases were package scan targets.
Suggested unit tests:
testEmptyAndBlankBehaviorByApiFamily, testWhitespaceOnlyTokenizationByContract, testBlankComparisonSemantics.
Spec target section:
Text Utilities / Empty And Blank Semantics

TEXT-FAILURE-001 — No Silent Wrong Output
Contract statement:
When parsing, formatting, validating, tokenizing, escaping, filtering, wrapping, generating, or converting cannot
satisfy its documented contract, the helper must fail visibly or return a documented fallback; it must not silently
report success with unrelated or corrupted text.
Rationale:
Silent wrong text can break generated code, UI, reports, logs, templates, metadata paths, query text, and serialized
values.
Source scope:
All com.viaoa.text helpers.
Related CODEX findings:
Package-wide CODEX comments involving false success, swallowed failure, malformed parsing, invalid escaping, wrong
wrapping, wrong validation, and corrupted conversion.
Suggested unit tests:
testFailureDoesNotReturnPartialSuccessAcrossRepresentativeApis, testParserFailureUsesDefinedFallback,
testFormatterFailureDoesNotReturnMisleadingText.
Spec target section:
Text Utilities / Failure Semantics

TEXT-FORMAT-001 — Formatting Produces Stable Runtime Text
Contract statement:
Formatting helpers must produce stable text for the same input value, format string, mask, alignment option, locale
assumptions, and OA settings.
Rationale:
Formatted text is used in UI display, reports, logs, templates, generated source, diagnostics, and metadata-facing
output.
Source scope:
OATextFormat.fmt, OATextFormat.mask, OATextFormat.toNumberString, OATextFormat.convertToValidPhoneNumber,
IndentFormatter.format.
Related CODEX findings:
OATextFormat formatting, masking, indentation, and UTF conversion findings are regression references.
Suggested unit tests:
testFormatSameInputSameOutput, testMaskSameInputSameOutput, testIndentFormatterProducesStableOutput.
Spec target section:
Text Utilities / Formatting Semantics

TEXT-FORMAT-002 — Formatting Failure And Charset Boundaries
Contract statement:
Invalid or unsupported format, mask, charset, or conversion inputs must either fail visibly or use a documented
fallback without corrupting supported Unicode text.
Rationale:
Wrong formatted text or corrupted characters can invalidate UI, report, template, logging, and generated-code
output.
Source scope:
OATextFormat.fmt, OATextFormat.mask, OATextFormat.toUTF8, OATextFormat.toUtf8.
Related CODEX findings:
OATextFormat.toUTF8 Unicode corruption; swallowed-exception wrong-output risks.
Suggested unit tests:
testInvalidFormatPatternUsesDefinedFallback, testUtf8ConversionDoesNotCorruptUnicodeByContract.
Spec target section:
Text Utilities / Formatting Failure Semantics

TEXT-VALIDATE-001 — Validators Match Declared Semantic Types
Contract statement:
Text validators and published validation regexes must accept only values matching their declared semantic type,
including integer, decimal, currency, date, time, datetime, phone, URL, and related formats.
Rationale:
OA UI, query, converter, datasource, and validation paths can make runtime decisions from these helpers.
Source scope:
OATextFormat.isInteger, isNumber, isDate, isTime, isDateTime; OARegex constants.
Related CODEX findings:
OATextFormat.isInteger accepts decimal strings.
Suggested unit tests:
testIsIntegerRejectsDecimalString, testRegexConstantsAcceptRejectKnownExamples.
Spec target section:
Text Utilities / Validation Semantics

TEXT-GRAMMAR-001 — Display Names Are Stable And Human-Oriented
Contract statement:
Converting class names, property names, code-style identifiers, and labels into display text must produce stable,
readable labels without losing intended word boundaries, acronym meaning, or capitalization meaning.
Rationale:
OA model tooling, generated UI, reports, metadata display, and code generation depend on predictable human-readable
labels.
Source scope:
OATextGrammar.getDisplayName, createDisplayName, convertToDisplayName, getTitle, getShortName.
Related CODEX findings:
none observed.
Suggested unit tests:
testDisplayNameCamelCaseAndAcronyms, testDisplayNamePreservesWordBoundaries, testShortNameRespectsMaximumLength.
Spec target section:
Text Utilities / Display Name Semantics

TEXT-GRAMMAR-002 — OA Grammar Rules Are Stable
Contract statement:
Singular, plural, article, possessive, title, and short-name helpers must apply OA’s defined grammar rules
consistently for supported words and documented fallbacks.
Rationale:
Generated UI text, labels, reports, messages, and model tooling rely on predictable grammar.
Source scope:
OATextGrammar.makeSingular, makePlural, getAorAn, makePossessive, getPossessive, getTitle, getShortName.
Related CODEX findings:
none observed.
Suggested unit tests:
testPluralizeRegularWord, testSingularizeRegularWord, testArticleForVowelSoundByContract, testPossessiveRules.
Spec target section:
Text Utilities / Grammar Semantics

TEXT-CODE-001 — Code Names Are Metadata-Compatible
Contract statement:
Helpers that derive Java identifiers, JavaBean property names, or OA property paths must produce legal, metadata-
compatible names or use a documented fallback.
Rationale:
OA metadata, reflection, property paths, generated source, query support, and runtime binding depend on valid
identifiers and property names.
Source scope:
OATextCode.getPropertyName; OATextUtil.makeJavaIdentifier; OATextUtil.createPath.
Related CODEX findings:
OATextCode.getPropertyName JavaBean acronym handling; OATextUtil.makeJavaIdentifier leading-digit identifier;
OATextUtil.createPath empty segment.
Suggested unit tests:
testJavaBeanAcronymPropertyNamePreserved, testMakeJavaIdentifierRejectsInvalidStart,
testCreatePathSkipsEmptySegments.
Spec target section:
Text Utilities / Code And Metadata Name Semantics

TEXT-NORMALIZE-001 — Whitespace And Line Structure Semantics
Contract statement:
Whitespace, indentation, trimming, unindenting, and line-normalization helpers must define whether they trim,
collapse, preserve internal spacing, preserve line breaks, and preserve trailing empty lines.
Rationale:
Generated source, templates, reports, logs, serialized text, and display text can be corrupted by unintended
whitespace changes.
Source scope:
OATextFormat.indent, unindent, unindentCode, trimEndingWhitespace, trimWhitespace; OATextFilter.trimSpaces.
Related CODEX findings:
OATextFormat.indent/unindent trailing empty line loss; OATextFormat.unindent null handling.
Suggested unit tests:
testIndentUnindentPreservesTrailingEmptyLines, testTrimWhitespacePreservesInternalSpacingByContract,
testUnindentNullBehaviorByContract.
Spec target section:
Text Utilities / Whitespace And Line Semantics

TEXT-ESCAPE-001 — Escape Round Trips Preserve Supported Text
Contract statement:
For each context where round-trip behavior is promised, unescape(escape(text)) must preserve supported original text
without adding, dropping, reordering, double-decoding, or reinterpreting characters.
Rationale:
HTML, XML, JSON, CSV, template, persistence, and generated-code text can be corrupted by non-reversible escaping.
Source scope:
OATextEscape HTML/XML/JSON/JavaScript/illegal-XML helpers; OATextTokenizer.csv and parseLine.
Related CODEX findings:
OATextEscape.convertFromHtml double-decode; OATextEscape.unescapeJson backslash sequence corruption;
OATextTokenizer.csv/parseLine quote round-trip.
Suggested unit tests:
testJsonEscapeUnescapeRoundTripQuotesBackslashesNewlines, testHtmlEscapeDecodeDoesNotDoubleDecode,
testCsvRoundTripQuotesCommasAndWhitespace.
Spec target section:
Text Utilities / Escaping Round-Trip Semantics

TEXT-ESCAPE-002 — Escaping Is Context-Specific
Contract statement:
HTML, XML, CDATA, JSON, JavaScript, CSV, regex, and attribute escaping rules must be applied only to their intended
context and must produce syntactically valid output for that context.
Rationale:
Escaping that is valid for one output context can be invalid or corrupting in another.
Source scope:
OATextEscape.escapeJson, escapeJs, convertTextToHtml, convertToHtml, convertToXml, getHtmlAttributeMap;
OATextTokenizer.csv.
Related CODEX findings:
OATextEscape.escapeJson apostrophe escape; OATextEscape.escapeJs backslash handling; OATextEscape.convertToXml CDATA
terminator; OATextEscape.convertTextToHtml raw markup-like text; OATextEscape.getHtmlAttributeMap quoted values.
Suggested unit tests:
testEscapeJsonDoesNotEmitInvalidApostropheEscape, testEscapeJsPreservesBackslashLiteral,
testCDataSplitsTerminatorSafely, testHtmlAttributeMapParsesQuotedValuesAndWhitespace.
Spec target section:
Text Utilities / Contextual Escaping Semantics

TEXT-ESCAPE-003 — Invalid Escape Sequences Do Not Become Wrong Text
Contract statement:
Invalid, incomplete, marker-like, or unsupported escape sequences must remain literal or use a documented fallback;
they must not silently become unrelated characters.
Rationale:
Literal user, model, path, serialized, and generated text must not be corrupted during unescaping or XML/JSON
cleanup.
Source scope:
OATextEscape.unescapeJson, decodeIllegalXml, encodeIllegalXml, isLegalXml, convertToXml.
Related CODEX findings:
OATextEscape.decodeIllegalXml marker-looking literal text; OATextEscape.isLegalXml/convertToXml unpaired surrogate
handling; OATextEscape.unescapeJson literal backslash sequence handling.
Suggested unit tests:
testDecodeIllegalXmlDoesNotDecodeLiteralMarkerByContract, testUnescapeJsonPreservesLiteralBackslashN,
testUnpairedSurrogateRejectedOrEncodedByContract.
Spec target section:
Text Utilities / Invalid Escape Semantics

TEXT-SANITIZE-001 — Sanitizers And Filters Preserve Safe Text
Contract statement:
Sanitizers and filters must remove, encode, or retain only the targeted character/content classes while preserving
safe text and documented ordering.
Rationale:
Over-filtering loses user data; under-filtering emits invalid, unsafe, or malformed text.
Source scope:
OATextSanitize; OATextFilter.strip, accept, removeCharacters, removeOtherCharacters, removeNonDigits,
removeNonFileNameChars, convertToAscii, removeLeading, removeEndingChars.
Related CODEX findings:
Sanitization and filtering mistakes were scan targets.
Suggested unit tests:
testSanitizePreservesSafeTextAndRemovesOnlyTargetedChars, testFilterPreservesUntouchedTextOutsideRule,
testRemoveNonDigitsPreservesAllowedDotByContract.
Spec target section:
Text Utilities / Sanitization And Filtering Semantics

TEXT-SENSITIVE-001 — Sensitive Text Masking
Contract statement:
Sensitive-value masking must apply deterministically to intended field names using documented case-sensitivity rules
and must not leak secrets through common capitalization variants.
Rationale:
Logs and diagnostics can expose credentials if masking misses ordinary field-name variations.
Source scope:
OATextTokenizer.maskPassword.
Related CODEX findings:
OATextTokenizer.maskPassword default case-sensitive behavior.
Suggested unit tests:
testMaskPasswordDefaultCaseInsensitive, testMaskPasswordHonorsExplicitCaseSensitiveMode,
testMaskPasswordCustomWordsByContract.
Spec target section:
Text Utilities / Sensitive Text Masking

TEXT-TOKENIZER-001 — Tokenizers Always Progress Or Terminate
Contract statement:
Tokenizer and parser helpers must consume input, advance position, or terminate on every loop iteration for empty,
repeated, quoted, nested, malformed, or delimiter-heavy input.
Rationale:
Shared parser utilities must not hang in OA query, path, template, style, CSV, log, or generated text workflows.
Source scope:
OATextTokenizer.count, countMatches, dcount, field, fieldAt, parseLine, tokenize, getCssMap;
OATextEscape.getHtmlAttributeMap.
Related CODEX findings:
Infinite-loop and tokenizer risks were scan targets.
Suggested unit tests:
testTokenizerTerminatesOnEmptyInput, testTokenizerTerminatesOnRepeatedDelimiter,
testTokenizerTerminatesOnMalformedToken.
Spec target section:
Text Utilities / Tokenizer Progress

TEXT-TOKENIZER-002 — Delimiter And Field Semantics
Contract statement:
Delimiters, quotes, escaped delimiters, whitespace separators, field indexes, and field counts must follow
documented rules for each tokenizer mode.
Rationale:
CSV, CSS, HTML attributes, PICK-style fields, and generic tokenization fail when delimiter semantics are ambiguous.
Source scope:
OATextTokenizer.count, countMatches, dcount, field, fieldAt, parseLine, tokenize, getCssMap;
OATextEscape.getHtmlAttributeMap.
Related CODEX findings:
OATextTokenizer.countMatches count behavior; parseLine quoted whitespace; CSV doubled quotes; CSV leading quote;
getCssMap whitespace/quoted values; tokenize/getHtmlAttributeMap tab/newline attribute delimiters.
Suggested unit tests:
testTokenizerFieldCountsAndIndexes, testParseLineAllowsWhitespaceAfterClosingQuote,
testCssMapParsesDeclarationsWithSpacesAndQuotes, testHtmlAttributeMapParsesQuotedValuesAndWhitespace.
Spec target section:
Text Utilities / Tokenizer Delimiter Semantics

TEXT-REGEX-001 — Literal Text Versus Regex Text
Contract statement:
Helpers that treat caller input as literal text must escape regex metacharacters before regex use, while APIs that
accept regex syntax must make that boundary explicit.
Rationale:
User, model, path, query, or display text must not accidentally become regex syntax.
Source scope:
OARegex constants; OATextUtil.convertToLikeSearch; regex-oriented helper paths.
Related CODEX findings:
Regex literal escaping risks were part of scan focus.
Suggested unit tests:
testRegexLiteralEscapingForMetacharacters, testRegexLiteralDotMatchesDotOnly,
testLikeSearchEscapesRegexCharactersByContract.
Spec target section:
Text Utilities / Regex Literal Semantics

TEXT-WRAP-001 — Wrapping Preserves Text And Width Contracts
Contract statement:
Wrapping and truncation must respect configured width, separator, break-character, minimum segment, and row-limit
contracts without dropping, duplicating, reordering characters, emitting spurious rows, or splitting words when a
valid break is available.
Rationale:
Reports, UI text, emails, logs, templates, and generated output depend on readable and complete wrapped text.
Source scope:
OATextLineWrap constructors, get/set/with configuration methods, wrap, wrapToString.
Related CODEX findings:
OATextLineWrap width-1 empty rows; setMaxRows zero/unlimited behavior; final-row truncation ignores tracked break.
Suggested unit tests:
testWrapPreservesCharactersAndUsesBreaks, testWrapWidthOneProducesNoEmptyRows,
testWrapMaxRowsZeroMeansUnlimitedByContract, testWrapTruncationUsesWhitespaceBreakWhenAvailable.
Spec target section:
Text Utilities / Line Wrap Semantics

TEXT-ALIGN-001 — Alignment And Padding Width Semantics
Contract statement:
Alignment, padding, truncation, and ellipsis helpers must produce documented target widths and consistent left,
right, center, pad-start, and pad-end behavior.
Rationale:
Fixed-width UI, report, log, diagnostic, and generated text output depends on stable alignment.
Source scope:
OATextAlign.padStart, padEnd, alignLeft, alignRight, alignCenter, align, leftPad, rightEnd, left, right, center.
Related CODEX findings:
OATextAlign.leftPad width behavior; OATextAlign.padStart/padEnd surrogate width behavior; OATextAlign.right/center
truncation exception.
Suggested unit tests:
testAlignPaddingProducesTargetWidth, testLeftPadUsesTargetWidth, testRightCenterTruncationDoesNotThrow,
testPadStartUnicodeWidthByContract.
Spec target section:
Text Utilities / Alignment Semantics

TEXT-UNICODE-001 — Unicode And Character Unit Boundaries
Contract statement:
Text helpers that slice, pad, case-convert, validate, or measure text must define whether they operate on char
units, code points, or display columns, and must not corrupt surrogate pairs where the contract promises Unicode-
safe behavior.
Rationale:
OA runtime text can contain non-ASCII display, metadata, serialized, and user-entered values.
Source scope:
OATextAlign, OATextChars, OATextCompare, OATextUtil.getBegin/getEnd/getFirst/getLast, OATextLineWrap.
Related CODEX findings:
OATextAlign surrogate width behavior; OATextUtil.getBegin/getEnd surrogate split; OATextChars upper/lower locale be
havior.
Suggested unit tests:
testSubstringHelpersUnicodeBoundaryByContract, testPadStartUnicodeWidthByContract,
testLineWrapDoesNotSplitSurrogatePairByContract.
Spec target section:
Text Utilities / Unicode Semantics

TEXT-LOCALE-001 — Locale-Stable Infrastructure Text
Contract statement:
Text helpers used for infrastructure identifiers, paths, queries, matching, validation, and generated metadata must
use locale-stable casing/comparison rules unless an API explicitly documents locale-sensitive behavior.
Rationale:
Default JVM locale must not change OA identifiers, path/query tokens, property names, matching results, or generated
text.
Source scope:
OATextChars.upper/lower; OATextCompare ignore-case methods; OATextCode; OATextGrammar where identifier-like text is
transformed.
Related CODEX findings:
OATextChars.upper/lower Turkish default-locale behavior; OATextCompare ignore-case index drift after case conversio
n.
Suggested unit tests:
testUpperLowerUseLocaleStableRules, testCompareCaseInsensitiveUsesLocaleRoot,
testIdentifierGenerationUnaffectedByTurkishLocale.
Spec target section:
Text Utilities / Locale Semantics

TEXT-COMPARE-001 — Comparison And Matching Semantics
Contract statement:
Text comparison and matching helpers must explicitly define case-sensitive, case-insensitive, null, blank, trim,
wildcard, and locale behavior.
Rationale:
OA search, filter, display matching, generated text decisions, and helper comparisons must be deterministic.
Source scope:
OATextCompare.isEqual, equals, isNotEqual, notEquals, isLike, compare, contains, startsWith, endsWith,
appendIfMissing, prefixIfMissing.
Related CODEX findings:
OATextChars upper/lower default-locale casing; comparison semantics scan findings.
Suggested unit tests:
testCompareNullCaseBlankAndLikeSemantics, testCompareCaseInsensitiveUsesLocaleRoot,
testAppendPrefixIfMissingCaseBehavior.
Spec target section:
Text Utilities / Comparison Semantics

TEXT-COMPARE-002 — Search Indexes Refer To Original Text
Contract statement:
Index-returning search helpers must return positions in the original input string and must not report missing text
as found except where empty or wildcard semantics explicitly define success.
Rationale:
Callers use returned indexes for slicing, highlighting, replacement, and UI display.
Source scope:
OATextCompare.indexOf, lastIndexOf, contains; OATextEscape.hilite.
Related CODEX findings:
OATextCompare.indexOf/lastIndexOf ignore-case index drift after case conversion.
Suggested unit tests:
testIgnoreCaseIndexOfReturnsOriginalIndex, testIgnoreCaseLastIndexOfReturnsOriginalIndex,
testContainsMissingTextReturnsFalse.
Spec target section:
Text Utilities / Search And Index Semantics

TEXT-CHAR-001 — Character Classification And Case Helpers
Contract statement:
Low-level character helpers must classify and transform characters according to their documented rules, including
null/empty handling, digit detection, first-character transformations, and locale-stable casing.
Rationale:
Character helpers support identifiers, filters, comparisons, tokenization, validation, and UI input processing.
Source scope:
OATextChars.hasDigits, makeFirstCharLower, makeFirstUpperCharsLower, makeFirstCharUpper, upper, lower.
Related CODEX findings:
OATextChars.upper/lower Turkish default-locale behavior.
Suggested unit tests:
testTextCharsLocaleStableCaseAndDigitHelpers, testFirstCharHelpersHandleNullEmptyAndUnicodeByContract.
Spec target section:
Text Utilities / Character Semantics

TEXT-GENERATE-001 — Generated Text Honors Bounds And Character Options
Contract statement:
Text generation helpers must honor requested length bounds and character-set options, or fail/use a documented
fallback when the request is impossible.
Rationale:
Demo, fixture, generated, and UI placeholder text must not violate caller constraints or crash on ordinary unchecked
bounds.
Source scope:
OATextGenerate.getDummyText, getRandomString, createDigits.
Related CODEX findings:
OATextGenerate.getRandomString impossible character-set behavior; OATextGenerate.getDummyText invalid bounds.
Suggested unit tests:
testRandomStringBoundsAndCharacterClasses, testRandomStringImpossibleCharacterSetByContract,
testDummyTextBoundsAndWordBoundaryBehavior.
Spec target section:
Text Utilities / Text Generation Semantics

TEXT-SOUNDEX-001 — Soundex Produces Stable Phonetic Keys
Contract statement:
Soundex generation must return deterministic four-character phonetic keys, handle null/empty input as documented,
and ignore non-letter characters according to the Soundex rules used by OA.
Rationale:
Phonetic matching must not produce invalid keys or false matches for normal names.
Source scope:
OATextSoundex.soundex.
Related CODEX findings:
OATextSoundex.soundex leading non-letter behavior.
Suggested unit tests:
testSoundexKnownExamplesAndNonLetters, testSoundexNullEmptyReturnsZeros.
Spec target section:
Text Utilities / Phonetic Matching Semantics

TEXT-UTILITY-001 — General Utilities Preserve Target Format Semantics
Contract statement:
General text utilities must preserve documented separator, property-path, hex, color, numeric, substring, repeated-
character, append, prepend, and concat semantics without silent corruption.
Rationale:
These helpers feed OA property paths, generated text, IDs, diagnostics, byte/string round trips, color values, UI
strings, and runtime configuration text.
Source scope:
OATextUtil.append, prepend, concat, colorToHex, parseInt, bytesToHex, hexToBytes, getBegin, getEnd, getFirst,
getLast, createString, createPath.
Related CODEX findings:
OATextUtil.concat null separator; hexToBytes odd/invalid hex; colorToHex format mismatch; parseInt overflow;
getBegin/getEnd surrogate split; createPath empty segment.
Suggested unit tests:
testConcatAppendPrependSeparatorSemantics, testHexRoundTripAndRejectsInvalidHex, testParseIntOverflowByContract,
testColorToHexMatchesConverterContract, testSubstringHelpersUnicodeBoundaryByContract.
Spec target section:
Text Utilities / General Utility Semantics

TEXT-BOUNDARY-001 — Text Package Conversion Boundary
Contract statement:
Text helpers may support formatting and textual representation, but semantic type conversion authority must remain
consistent with converter, datetime, compare, path, query, template, reflect, serialization, and runtime contracts.
Rationale:
Text utilities often sit at package boundaries; they must not reinterpret runtime values in ways that conflict with
OA semantic packages.
Source scope:
OATextFormat, OATextCompare, OATextGrammar, OATextCode, OATextUtil, OATextEscape, OATextTokenizer; integration with
com.viaoa.converter, datetime, path, query, template, reflect, serialize.
Related CODEX findings:
Format conversion, color hex, date/number validation, JavaBean property-name, and property-path findings illustrate
boundary risks.
Suggested unit tests:
testTextFormattingMatchesConverterBoundary, testColorHexMatchesConverterContract,
testPropertyNameMatchesJavaBeanContract, testPathCreationMatchesOAPathContract.
Spec target section:
Text Utilities / Cross-Package Boundaries

TEXT-THREAD-001 — Stateless And Reusable Helper Semantics
Contract statement:
Static helper classes must not retain per-call mutable state, and reusable instances such as OATextLineWrap and
IndentFormatter must keep configuration state separate from operation-local state.
Rationale:
Text helpers are widely reused by runtime, logging, template, tooling, and generated-code paths that may run
concurrently.
Source scope:
OATextAlign, OATextChars, OATextCode, OATextCompare, OATextEscape, OATextFilter, OATextFormat, OATextGenerate,
OATextGrammar, OATextSanitize, OATextSoundex, OATextTokenizer, OATextUtil, OATextLineWrap, IndentFormatter.
Related CODEX findings:
Stateful line-wrap configuration and formatter usage are relevant reuse boundaries; no direct ThreadLocal findings
observed in this package.
Suggested unit tests:
testStaticHelpersHaveNoCrossCallState, testLineWrapSequentialCallsDoNotLeakRows,
testIndependentLineWrapInstancesDoNotShareState.
Spec target section:
Text Utilities / Reuse And Threading Semantics

*/


