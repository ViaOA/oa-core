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
 * The {@link com.viaoa.util.OAString} class acts as the primary public facade,
 * delegating functionality to the appropriate module in this package.
 * </p>
 */
package com.viaoa.text;




























