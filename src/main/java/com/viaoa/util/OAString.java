/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
package com.viaoa.util;

import java.awt.Color;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

import com.viaoa.text.OATextAlign;
import com.viaoa.text.OATextChars;
import com.viaoa.text.OATextCompare;
import com.viaoa.text.OATextEscape;
import com.viaoa.text.OATextFilter;
import com.viaoa.text.OATextFormat;
import com.viaoa.text.OATextGenerate;
import com.viaoa.text.OATextGrammar;
import com.viaoa.text.OATextLineWrap;
import com.viaoa.text.OATextSanitize;
import com.viaoa.text.OATextSoundex;
import com.viaoa.text.OATextTokenizer;
import com.viaoa.text.OATextUtil;

/**
 * Public OA text utility façade for all things String / Text.
 * <p>
 * OAString is a fully supported, stable entry point for text operations in OA.
 * Implementations are organized in the {@code com.viaoa.text} package and
 * OAString forwards to those modules to keep this surface consistent across
 * versions.
 * </p>
 * <p>
 * Primary modules:
 * </p>
 * <ul>
 * <li>{@code OATextAlign} – padding, truncation, alignment</li>
 * <li>{@code OATextChars} – Character category checks</li>
 * <li>{@code OATextCompare} – Partial or full matching between text values</li>
 * <li>{@code OATextFormat} – formatting for strings/dates/numbers</li>
 * <li>{@code OATextGrammar} – singular/plural/title case/display/etc</li>
 * <li>{@code OATextTokenizer} – parsing and field extraction</li>
 * <li>{@code OATextFilter} – strip/retain/remove character sets</li>
 * <li>{@code OATextEscape} – HTML/XML/JSON escaping and quoting</li>
 * <li>{@code OATextSanitize} – null/empty safety helpers</li>
 * <li>{@code OATextLineWrap} – wrapping, hyphenation, ellipsis</li>
 * <li>{@code OATextUtil} – general-purpose helpers</li>
 * <li>{@code OATextGenerate} – generate sample data</li>
 * </ul>
 * <p>
 * New code may use {@code com.viaoa.text} classes directly when specialized
 * control is desired; however, OAString remains the recommended convenience API
 * for most usage.
 * </p>
 *
 * @apiNote OAString is maintained as the primary public API. The delegated
 *          implementations in {@code com.viaoa.text} may evolve without
 *          breaking this surface.
 * @implNote This class delegates to the {@code com.viaoa.text} modules;
 *           delegation targets may change internally without affecting the
 *           public behavior of these methods.
 */
public class OAString {
	public static final String NL = System.getProperty("line.separator");
	public static final String FS = File.separator;

	/** @see OATextFilter#trimSpaces(String) */
	public static String trimSpaces(final String line) {
		return OATextFilter.trimSpaces(line);
	}

	/** @see OATextFilter#trimSpaces(String) */
	@Deprecated
	public static String trim(final String line) {
		return OATextFilter.trimSpaces(line);
	}
	
	
	/** @see OATextUtil#getEnd(String, int) */
	public static String getEnd(String text, int len) {
		return OATextUtil.getEnd(text, len);
	}

	/** @see OATextUtil#getLast(String, int) */
	public static String getLast(String text, int len) {
		return OATextUtil.getLast(text, len);
	}

	/** @see OATextUtil#getBegin(String, int) */
	public static String getBegin(String text, int len) {
		return OATextUtil.getBegin(text, len);
	}

	/** @see OATextUtil#getFirst(String, int) */
	public static String getFirst(String text, int len) {
		return OATextUtil.getFirst(text, len);
	}
	
	/** @see OATextEscape#convertToHtml(String) */
	public static String convertToHtml(String value) {
		return OATextEscape.convertToHtml(value);
	}

	/** @see OATextEscape#convertToHtml(String) */
	@Deprecated()
	public static String convertToHTML(String value) {
		return OATextEscape.convertToHtml(value);
	}
	
	/** @see OATextEscape#convertTextToHtml(String, boolean) */
	public static String convertTextToHtml(String value, boolean bAddHTMLTag) {
		return OATextEscape.convertTextToHtml(value, bAddHTMLTag);
	}
	
	/** @see OATextEscape#convertTextToHtml(String, boolean) */
	@Deprecated()
	public static String convertTextToHTML(String value, boolean bAddHTMLTag) {
		return OATextEscape.convertTextToHtml(value, bAddHTMLTag);
	}
	
	
	/** @see OATextEscape#convertFromHtml(String) */
	public static String convertFromHtml(String html) {
		return OATextEscape.convertFromHtml(html);
	}
	
	/** @see OATextEscape#getHtmlAttributeMap(String) */
	public static Map<String, String> getHtmlAttributeMap(String htmlTag) {
		return OATextEscape.getHtmlAttributeMap(htmlTag);
	}
	
	/** @see OATextEscape#convertToXml(String) */
	public static String convertToXml(String value) {
		return OATextEscape.convertToXml(value);
	}

	/** @see OATextEscape#convertToXml(String, boolean) */
	public static String convertToXml(String value, boolean bCData) {
		return OATextEscape.convertToXml(value, bCData);
	}

	/** @see OATextEscape#convertToXml(String, boolean, boolean) */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml) {
		return OATextEscape.convertToXml(value, bCData, bIsHtml);
	}

	/** @see OATextEscape#convertToXml(String, boolean, boolean, boolean) */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml, boolean bLeaveCRLF) {
		return OATextEscape.convertToXml(value, bCData, bIsHtml, bLeaveCRLF);
	}

	/** @see OATextEscape#isLegalXml(String) */
	public static boolean isLegalXml(String value) {
		return OATextEscape.isLegalXml(value);
	}

	/** @see OATextEscape#decodeIllegalXml(String) */
	public static String decodeIllegalXml(String value) {
		return OATextEscape.decodeIllegalXml(value);
	}

	/** @see OATextEscape#encodeIllegalXml(String) */
	public static String encodeIllegalXml(String value) {
		return OATextEscape.encodeIllegalXml(value);
	}
	
	/**
	 * @see OATextFilter#convert(String, String, String, boolean, boolean, int, int)
	 */
	public static String convert(String value, char c, String replace) {
		return OATextFilter.convert(value, c, replace);
	}

	/**
	 * @see OATextFilter#convert(String, String, String, boolean, boolean, int, int)
	 */
	public static String convertIgnoreCase(String line, String search, String replace) {
		return OATextFilter.convertIgnoreCase(line, search, replace);
	}

	/**
	 * @see OATextFilter#convert(String, String, String, boolean, boolean, int, int)
	 */
	public static String convert(String line, String search, String replace) {
		return OATextFilter.convert(line, search, replace);
	}

	/** @see OATextFilter#removeCharacters(String, String) */
	public static String removeCharacters(String line, String search) {
		return OATextFilter.removeCharacters(line, search);
	}

	/** @see OATextFilter#removeOtherCharacters(String, String) */
	public static String removeOtherCharacters(String line, String keep) {
		return OATextFilter.removeOtherCharacters(line, keep);
	}

	/** @see OATextFilter#removeNonDigits(String, boolean) */
	public static String removeNonDigits(String line) {
		return OATextFilter.removeNonDigits(line);
	}

	/** @see OATextFilter#removeNonDigits(String, boolean) */
	public static String removeNonDigits(String line, boolean bAllowDot) {
		return OATextFilter.removeNonDigits(line, bAllowDot);
	}

	/** @see OATextFilter#removeNonFileNameChars(String) */
	public static String removeNonFileNameChars(String line) {
		return OATextFilter.removeNonFileNameChars(line);
	}

	/**
	 * @see OATextFilter#convert(String, String, String, boolean, boolean, int, int)
	 */
	public static String convert(String line, String search, String replace, boolean bIgnoreCase) {
		return OATextFilter.convert(line, search, replace, bIgnoreCase);
	}

	/**
	 * @see OATextFilter#convert(String, String, String, boolean, boolean, int, int)
	 */
	public static String convert(final String line, String search, String replace, final boolean bIgnoreCase, final boolean bFirstOnly, final int startPos, final int endPos) {
		return OATextFilter.convert(line, search, replace, bIgnoreCase, bFirstOnly, startPos, endPos);
	}

	/** @see OATextFormat#convertToCamelCase(String, String) */
	public static String convertToCamelCase(String value) {
		return OATextFormat.convertToCamelCase(value);
	}

	/** @see OATextFormat#convertToCamelCase(String, String) */
	public static String convertToCamelCase(String value, String sepChars) {
		return OATextFormat.convertToCamelCase(value, sepChars);
	}

	/** @see OATextFormat#convertToHungarian(String) */
	public static String convertToHungarian(String value) {
		return OATextFormat.convertToHungarian(value);
	}

	/** @see OATextFormat#convertToHungarian(String, String) */
	public static String convertToHungarian(String value, String sepChars) {
		return OATextFormat.convertToHungarian(value, sepChars);
	}

	/** Alias for {@link #getShortName(String, int)} with max=3 */	
	public static String getAbbrev(String name) {
		return getShortName(name);
	}

	/** Alias for {@link #getShortName(String, int)} with max=3 */	
	public static String getShortName(String name) {
		return getShortName(name, 3);
	}

	/** @see OATextGrammar#getShortName(String, int) */
	public static String getShortName(final String name, final int max) {
		return OATextGrammar.getShortName(name, max);
	}
	
	/** @see OATextGrammar#getDisplayName(String) */
	public static String getDisplayName(String value) {
		return OATextGrammar.getDisplayName(value);
	}

	/** @see OATextGrammar#createDisplayName(String) */
	public static String createDisplayName(String value) {
		return OATextGrammar.createDisplayName(value);
	}

	/** @see OATextGrammar#convertToDisplayName(String) */
	public static String convertToDisplayName(String value) {
		return OATextGrammar.convertToDisplayName(value);
	}

	/** @see OATextGrammar#makeSingular(String) */
	public static String getSingular(String str) {
		return OATextGrammar.makeSingular(str);
	}
	
	/** @see OATextGrammar#makeSingular(String) */
	public static String makeSingular(String str) {
		return OATextGrammar.makeSingular(str);
	}
	
	/** @see OATextGrammar#getPlural(String) */
	public static String getPlural(String s) {
		return makePlural(s);
	}

	/** @see OATextGrammar#getPlural(String) */
	public static String makePlural(String str) {
		return OATextGrammar.makePlural(str);
	}
	
	/** @see OATextGrammar#getAorAn(String) */
	public static String getAorAn(String s) {
		return OATextGrammar.getAorAn(s);
	}

	/** @see OATextGrammar#makePossessive(String) */
	public static String makePossessive(String str) {
		return OATextGrammar.makePossessive(str);
	}

	/** @see OATextGrammar#getPossessive(String) */
	public static String getPossessive(String str) {
		return OATextGrammar.getPossessive(str);
	}

	/** @see OATextGrammar#getTitle(String) */
	public static String getTitle(String s) {
		return OATextGrammar.getTitle(s);
	}
	
	/** @see OATextGrammar#getTitle(String) */
	public static String getTitleCase(String s) {
		return getTitle(s);
	}

	/** @see OATextGrammar#getTitle(String) */
	public static String toTitleCase(String s) {
		return getTitle(s);
	}

	/** @see OATextGrammar#getTitle(String) */
	public static String titleCase(String s) {
		return getTitle(s);
	}

	/** @see OATextChars#makeFirstCharLower(String) */
	public static String makeFirstCharLower(String s) {
		return OATextChars.makeFirstCharLower(s);
	}
	/** @see OATextChars#makeFirstCharLower(String) */
	public static String mfcl(String s) {
		return makeFirstCharLower(s);
	}

	/** @see OATextChars#makeFirstUpperCharsLower(String) */
	public static String makeFirstUpperCharsLower(String s) {
		return OATextChars.makeFirstUpperCharsLower(s);
	}

	/** @see OATextChars#makeFirstUpperCharsLower(String) */
	public static String mfucl(String s) {
		return makeFirstUpperCharsLower(s);
	}

	/** @see OATextChars#makeFirstCharUpper(String) */
	public static String makeFirstCharUpper(String s) {
		return OATextChars.makeFirstCharUpper(s);
	}
	/** @see OATextChars#makeFirstCharUpper(String) */
	public static String mfcu(String s) {
		return makeFirstCharUpper(s);
	}
	
	/** @see OATextTokenizer#field(String, String, int) */
	@Deprecated()
	public static String field(String str, String sep, int beg) {
		return OATextTokenizer.field(str, sep, beg);
	}
	
	/** @see OATextTokenizer#fieldAt(String, String, int, int) */
	@Deprecated()
	public static String field(final String str, final String sep, final int beg, final int amt) {
		return OATextTokenizer.field(str, sep, beg, amt);
	}
	
	/** @see OATextTokenizer#field(String, char, int) */
	@Deprecated()
	public static String field(String str, char sep, int beg) {
		return OATextTokenizer.field(str, sep, beg);
	}

	/** @see OATextTokenizer#field(String, char, int, int) */
	@Deprecated()
	public static String field(String str, char sep, int beg, int amt) {
		return OATextTokenizer.field(str, sep, beg, amt);
	}

	/** @see OATextTokenizer#fieldAt(String, String, int) */
	public static String fieldAt(String str, String sep, int beg) {
		return OATextTokenizer.fieldAt(str, sep, beg);
	}

	/** @see OATextTokenizer#fieldAt(String, String, int, int) */
	public static String fieldAt(final String str, final String sep, final int beg, final int amt) {
		return OATextTokenizer.fieldAt(str, sep, beg, amt);
	}
	
	/** @see OATextTokenizer#fieldAt(String, char, int) */
	public static String fieldAt(String str, char sep, int beg) {
		return OATextTokenizer.fieldAt(str, sep, beg);
	}
	
	/** @see OATextTokenizer#fieldAt(String, char, int, int) */
	public static String fieldAt(String str, char sep, int beg, int amt) {
		return OATextTokenizer.fieldAt(str, sep, beg, amt);
	}

	/** @see OATextTokenizer#count(String, String) */
	public static int count(String str, String sep) {
		return OATextTokenizer.count(str, sep);
	}

	/** @see OATextTokenizer#countMatches(String, String) */
	public static int countMatches(String str, String sep) {
		return OATextTokenizer.countMatches(str, sep);
	}

	/** @see OATextTokenizer#countMatches(String, String) */
	public static int countMatches(String str, char sep) {
		return OATextTokenizer.countMatches(str, sep);
	}

	/** @see OATextTokenizer#dcount(String, String) */
	public static int dcount(String str, String sep) {
		return OATextTokenizer.dcount(str, sep);
	}

	/** @see OATextTokenizer#dcount(String, String) */
	public static int dcount(String str, char sep) {
		return OATextTokenizer.dcount(str, sep);
	}

	/** @see OATextAlign#padStart(String, int, char) */
	public static String padStart(String value, int amount) {
		return pad(value, amount, false, ' ');
	}

	/** @see OATextAlign#padStart(String, int, char) */
	public static String leftPad(String value, int amount) {
		return pad(value, amount, false, ' ');
	}

	/** @see OATextAlign#padStart(String, int, char) */
	public static String padStart(String value, int amount, char padChar) {
		return pad(value, amount, false, padChar);
	}

	/** @see OATextAlign#padStart(String, int, char) */
	public static String leftPad(String value, int amount, char padChar) {
		return pad(value, amount, false, padChar);
	}

	/** @see OATextAlign#padEnd(String, int, char) */
	public static String padEnd(String value, int amount) {
		return pad(value, amount, true, ' ');
	}

	/** @see OATextAlign#padEnd(String, int, char) */
	public static String padRight(String value, int amount) {
		return pad(value, amount, true, ' ');
	}

	/** @see OATextAlign#padEnd(String, int, char) */
	public static String padEnd(String value, int amount, char padChar) {
		return pad(value, amount, true, padChar);
	}

	/** @see OATextAlign#align(String, int, OATextAlign.Align, char, boolean) */
	public static String pad(String value, int amount, boolean bAddToEnd, char padCharacter) {
		if (bAddToEnd)
			return OATextAlign.padEnd(value, amount, padCharacter);
		return OATextAlign.padStart(value, amount, padCharacter);
	}
	
	/** @see OATextAlign#alignLeft(String, int, char) */
	public static String alignLeft(String value, int width, char charPad) {
		return OATextAlign.alignLeft(value, width, charPad);
	}

	/** @see OATextAlign#alignRight(String, int, char) */
	public static String alignRight(String value, int width, char charPad) {
		return OATextAlign.alignRight(value, width, charPad);
	}

	/** @see OATextAlign#alignCenter(String, int, char) */
	public static String alignCenter(String value, int width, char charPad) {
		return OATextAlign.alignCenter(value, width, charPad);
	}
	/** @see OATextAlign#align(String, int, boolean, char) */
	public static String align(String value, int width, boolean bAlignLeft, char charPad) {
		return OATextAlign.align(value, width, bAlignLeft, charPad);
	}
	
	/** @see OATextFormat#format(String, String) */
	public static String format(long value, String format) {
		return OAConv.toString(value, format);
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(int value, String format) {
		// see which format to use
		String s = format.toUpperCase();
		if (s.indexOf('R') >= 0 || s.indexOf('L') >= 0 || s.indexOf('C') >= 0) {
			return OAString.format(Integer.toString(value), format);
		}
		return OAConv.toString(value, format);
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(double value, String format) {
		String s = format.toUpperCase();
		if (s.indexOf('R') >= 0 || s.indexOf('L') >= 0 || s.indexOf('C') >= 0) {
			return OAString.format(Double.toString(value), format);
		}
		return OAConv.toString(value, format);
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(boolean value, String format) {
		return OAConv.toString(value, format);
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(OADateTime value, String format) {
		return OAConv.toString(value, format);
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(OADate value) {
		return OAConv.toString(value, OADate.getGlobalOutputFormat());
	}

	/** @see OATextFormat#format(String, String) */
	public static String format(String str, String format) {
		return fmt(str, format);
	}

	/** @see OATextFormat#pickFormat(String, String) */
	public static String pickFormat(String str, String format) {
		return fmt(str, format);
	}

	/** @see OATextFormat#fmt(String, String) */
	public static String fmt(String str, String format) {
		String s = OATextFormat.fmt(str, format);
		return s;
	}

	/** @see OATextFormat#fmt(String, String) */
	public static String fmt(String str) {
		if (str == null) return "";
		return str;
	}

	/** @see OATextFilter#stripDigits(String) */
	public static String stripDigits(String value) {
		return OATextFilter.stripDigits(value);
	}

	/** @see OATextFormat#mask(String, String, boolean) */
	public static String mask(String value, String mask) {
		return mask(value, mask, false);
	}

	/** @see OATextFormat#mask(String, String, boolean) */
	public static String mask(String value, String mask, boolean bRightJustified) {
		return OATextFormat.mask(value, mask, bRightJustified);
	}

	/** @see OATextFilter#strip(String, String) */
	public static String strip(String value, String chars) {
		return OATextFilter.strip(value, chars);
	}

	/** @see OATextFilter#accept(String, String) */
	public static String accept(String value, String chars) {
		return OATextFilter.accept(value, chars);
	}

	/** @see OAFile#convertFileName(String) */
	public static String convertFileName(String fileName) {
		return OAFile.convertFileName(fileName);
	}

	/** @see OAFile#convertFileName(String, boolean) */
	public static String convertFileName(String fileName, boolean bEndWithSlashChar) {
		return OAFile.convertFileName(fileName, bEndWithSlashChar);
	}

	/** @see OAFile#getFileName(String) */
	public static String getFileName(String filePath) {
		return OAFile.getFileName(filePath);
	}

	/** @see OAFile#getDirectoryName(String) */
	public static String getDirectoryName(String filePath) {
		return OAFile.getDirectoryName(filePath);
	}

	/** @see OATextUtil#colorToHex(Color) */
	public static String colorToHex(Color color) {
		return OATextUtil.colorToHex(color);
	}

	/** @see OATextChars#hasDigits(String) */
	public static boolean hasDigits(String word) {
		return OATextChars.hasDigits(word);
	}

	/** @see OATextSoundex#soundex(String) */
	public static String soundex(String word) {
		return OATextSoundex.soundex(word);
	}

	/** @see OATextFormat#isNumber(String) */
	public static boolean isNumber(String str) {
		return OATextFormat.isNumber(str);
	}

	/** @see OATextFormat#isInteger(String) */
	public static boolean isInteger(String str) {
		return OATextFormat.isInteger(str);
	}

	/** @see OATextFormat#isDate(String) */
	public static boolean isDate(String s) {
		return OATextFormat.isDate(s);
	}

	/** @see OATextFormat#isTime(String) */
	public static boolean isTime(String s) {
		return OATextFormat.isTime(s);
	}

	/** @see OATextFormat#isDateTime(String) */
	public static boolean isDateTime(String s) {
		return OATextFormat.isDateTime(s);
	}

	/** @see OATextCompare#equals(String, String, boolean) */
	public static boolean equals(String s1, String s2, boolean bIgnoreCase) {
		return OATextCompare.equals(s1, s2, bIgnoreCase);
	}
	
	/** @see OATextCompare#equals(String, String) */
	public static boolean equals(String s1, String s2) {
		return OATextCompare.equals(s1, s2);
	}

	/** @see OATextCompare#notEquals(String, String) */
	public static boolean notEquals(String s1, String s2) {
		return OATextCompare.notEquals(s1, s2);
	}

	/** @see OATextCompare#notEquals(String, String, boolean) */
	public static boolean notEquals(String s1, String s2, boolean bIgnoreCase) {
		return OATextCompare.notEquals(s1, s2, bIgnoreCase);
	}
	
	/** @see OATextSanitize#toString(Object) */
	public static String toString(Object obj) {
		return OATextSanitize.toString(obj);
	}

	/** @see OATextSanitize#toString(Object) */
	public static String toString(String str) {
		return OATextSanitize.toString(str);
	}

	/** @see OATextSanitize#toString(Object) */
	public static String toString(String str, String strIfNull) {
		return OATextSanitize.defaultString(str, strIfNull);
	}

	/** @see String(byte[]) */
	public static String toString(byte[] bytes) {
		return new String(bytes, Charset.defaultCharset());
	}
	
	
	
	/** @see OATextFormat#toNumberString(int) */
	public static String toNumberString(int x) {
		return OATextFormat.toNumberString(x);
	}

	/** @see OATextLineWrap#wrapToString(String, int) */
	public static String truncate(String text, int width) {
		OATextLineWrap wrap = new OATextLineWrap(width, "|").withMaxRows(1);
		String result = wrap.wrapToString(text);
		return result;
	}
	
	/** @see OATextLineWrap#wrapToString(String, int) */
	public static String trunc(String orig, int width) {
		return truncate(orig, width);
	}

	/** @see OATextLineWrap#wrapToString(String, int) */
	public static String abbreviate(String orig, int width) {
		return truncate(orig, width);
	}


	/** @see OATextGenerate#getRandomString(int, int, int, boolean, boolean, boolean) */
	public static String getRandomString(int min, int max) {
		return OATextGenerate.getRandomString(min, max);
	}

	/** @see OATextGenerate#getRandomString(int, int, int, boolean, boolean, boolean) */
	public static String createRandomString(int min, int max) {
		return OATextGenerate.getRandomString(min, max);
	}

	/** @see OATextGenerate#getRandomString(int, int, int) */
	public static String getRandomString(int normal, int min, int max) {
		return OATextGenerate.getRandomString(normal, min, max);
	}

	/** @see OATextGenerate#getRandomString(int, int, int, boolean, boolean, boolean) */
	public static String getRandomString(int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return OATextGenerate.getRandomString(min, max, bUseDigits, bUseAlpha, bCapFirstChar);
	}

	/** @see OATextGenerate#getRandomString(int, int, int, boolean, boolean, boolean) */
	public static String getRandomString(int normal, int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return OATextGenerate.getRandomString(normal, min, max, bUseDigits, bUseAlpha, bCapFirstChar);
	}

	/** @see OATextGenerate#getRandomDigits(int, int, int, boolean, boolean, boolean) */
	public static String getRandomDigits(int min, int max) {
		return OATextGenerate.createDigits(min, max);
	}

	
	/** @see OATextGenerate#getDummyText(int, int, int) */
	public static String getSampleText(int len) {
		return getDummyText(len, len, len);
	}

	/** @see OATextGenerate#getDummyText(int, int, int) */
	public static String getSampleText(int normal, int min, int max) {
		return getDummyText(normal, min, max);
	}

	/** @see OATextGenerate#getDummyText(int, int, int) */
	public static String getDummyText(int normal, int min, int max) {
		return OATextGenerate.getDummyText(normal, min, max);
	}
	
	/** @see OATextUtil#createPropertyPath(String...) */
	public static String createPropertyPath(String... args) {
		return OATextUtil.createPropertyPath(args);
	}

	/** @see OATextUtil#createPropertyPath(String...) */
	public static String cpp(String... args) {
		return OATextUtil.createPropertyPath(args);
	}
	
	/** @see OATextUtil#createPropertyPath(Class, String...) */
	public static String createPropertyPath(Class clazz, String... args) {
		return OATextUtil.createPropertyPath(clazz, args);
	}

	/** @see OATextUtil#createPropertyPath(Class, String...) */
	public static String cpp(Class clazz, String... args) {
		return createPropertyPath(clazz, args);
	}

	/** @see OATextFormat#toUTF8(String) */
	public static String toUtf8(String isoString) {
		return OATextFormat.toUTF8(isoString);
	}

	public static String getSHAHash(String input) {
		return OAEncryption.getHash(input);
	}

	public static String convertToSHAHash(String input) {
		return OAEncryption.getHash(input);
	}

	/** @see OATextLineWrap#wrapToString(String) */
	public static String lineBreak(String text, int columnWidth, String separator, int maxRows) {
		OATextLineWrap wrap = new OATextLineWrap(columnWidth, separator).withMaxRows(maxRows);
		return wrap.wrapToString(text);
	}
	
	/** @see OATextSanitize#notEmpty(Object) */
	public static boolean notEmpty(Object obj) {
		return OATextSanitize.notEmpty(obj);
	}

	/** @see OATextSanitize#isNotEmpty(String) */
	public static boolean isNotEmpty(Object obj) {
		return OATextSanitize.isNotEmpty(obj);
	}

	/** @see OATextSanitize#isNotNullAndNotEmpty(Object) */
	public static boolean isNotNullAndNotEmpty(Object obj) {
		return OATextSanitize.isNotNullAndNotEmpty(obj);
	}

	/** @see OATextSanitize#isEmpty(Object) */
	public static boolean isEmpty(Object obj) {
		return OATextSanitize.isEmpty(obj);
	}

	/** @see OATextSanitize#isEmpty(Object, boolean) */
	public static boolean isEmpty(Object obj, boolean bTrim) {
		return OATextSanitize.isEmpty(obj, bTrim);
	}

	/** @see OATextSanitize#isEmpty(Object) */
	public static boolean isNullOrEmpty(Object obj) {
		return OATextSanitize.isEmpty(obj, false);
	}
	
	/** @see OATextCompare#isEqual(String, String) */
	public static boolean isEqual(String s, String s2) {
		return OATextCompare.isEqual(s, s2, false);
	}

	/** @see OATextCompare#isEqual(String, String, boolean) */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase) {
		return OATextCompare.isEqual(s, s2, bIgnoreCase);
	}

	/** @see OATextCompare#isEqual(String, String, boolean, boolean) */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return OATextCompare.isEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/** @see OATextCompare#isEqualIgnoreCase(String, String) */
	public static boolean isEqualIgnoreCase(String s, String s2) {
		return OATextCompare.isEqualIgnoreCase(s, s2);
	}

	/** @see OATextCompare#isEqualIgnoreCase(String, String) */
	public static boolean equalsIgnoreCase(String s1, String s2) {
		return OATextCompare.isEqualIgnoreCase(s1, s2);
	}

	/** @see OATextCompare#isEqualNullEqualsBlank(String, String) */
	public static boolean isEqualNullEqualsBlank(String s, String s2) {
		return OATextCompare.isEqualNullEqualsBlank(s, s2);
	}

	/** @see OATextCompare#isNotEqual(String, String) */
	public static boolean isNotEqual(String s, String s2) {
		return OATextCompare.isNotEqual(s, s2);
	}

	/** @see OATextCompare#isNotEqual(String, String, boolean) */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase) {
		return OATextCompare.isNotEqual(s, s2, bIgnoreCase);
	}

	/** @see OATextCompare#isNotEqual(String, String, boolean, boolean) */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return OATextCompare.isNotEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/** @see OATextCompare#isNotEqualNullEqualsBlank(String, String) */
	public static boolean isNotEqualNullEqualsBlank(String s, String s2) {
		return OATextCompare.isNotEqualNullEqualsBlank(s, s2);
	}
	
	/** @see OATextCompare#isLike(String, String) */
	public static boolean isLike(String s, String s2) {
		return OATextCompare.isLike(s, s2);
	}
	
	/** @see OATextCompare#compare(String, String) */
	public static int compare(String s1, String s2) {
		return OATextCompare.compare(s1, s2);
	}

	/** @see OATextFormat#convertToValidPhoneNumber(String) */
	public static String convertToValidPhoneNumber(String phone) {
		return OATextFormat.convertToValidPhoneNumber(phone);
	}

	/** @see OATextFormat#indent(String, int) */
	public static String indent(String text, int amt) {
		return OATextFormat.indent(text, amt);
	}

	/** @see OATextFormat#unindent(String) */
	public static String unindent(String text) {
		return OATextFormat.unindent(text);
	}

	/** @see OATextFormat#unindent(String) */
	public static String unindentCode(String text) {
		return OATextFormat.unindentCode(text);
	}

	/** @see OATextFormat#unindent(String, boolean) */
	public static String unindent(String text, boolean bBasedOnFirstLine) {
		return OATextFormat.unindent(text, bBasedOnFirstLine);
	}

	    
	/** @see OATextFormat#trimEndingWhitespace(String) */
	public static String trimEndingWhitespace(String text) {
		return OATextFormat.trimEndingWhitespace(text);
	}

	/** @see OATextTokenizer#parseLine(String, char, boolean) */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes) {
		return OATextTokenizer.parseLine(line, sep, bCouldHaveQuotes);
	}

	/** @see OATextTokenizer#parseLine(String, char, boolean) */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes, int sizeEstimate) {
		return OATextTokenizer.parseLine(line, sep, bCouldHaveQuotes, sizeEstimate);
	}

	/** @see OATextFormat#trimWhitespace(String) */
	public static String trimWhitespace(String text) {
		return OATextFormat.trimWhitespace(text);
	}


	/** @see OATextFilter#convertToAscii(String) */
	public static String convertToAscii(String text) {
		return OATextFilter.convertToAscii(text);
	}


	/** @see OATextTokenizer#getCssMap(String) */
	public static Map<String, String> getCssMap(String style) {
		return OATextTokenizer.getCssMap(style);
	}

	public static int parseInt(String val) {
		return OATextUtil.parseInt(val);
	}


	/** @see OATextSanitize#toNonNull(String, String) */
	public static String toNonNull(String str) {
		return OATextSanitize.toNonNull(str, "");
	}

	/** @see OATextSanitize#toNonNull(String, String) */
	public static String toNonNull(String str, String defaultValue) {
		return OATextSanitize.toNonNull(str, defaultValue);
	}

	/** @see OATextSanitize#getNonNull(String, String) */
	public static String getNonNull(String str) {
		return OATextSanitize.getNonNull(str);
	}

	/** @see OATextSanitize#getNonNull(String, String) */
	public static String getNonNull(String str, String defaultValue) {
		return OATextSanitize.getNonNull(str, defaultValue);
	}

	public static String convertToNonNull(String str) {
		return OATextSanitize.convertToNonNull(str);
	}

	public static String convertToNonNull(String str, String defaultValue) {
		return OATextSanitize.convertToNonNull(str, defaultValue);
	}

	/** @see OATextSanitize#defaultString(String, String) */
	public static String defaultString(String str) {
		return OATextSanitize.defaultString(str);
	}

	/** @see OATextSanitize#defaultString(String, String) */
	public static String defaultString(String str, String strIfNull) {
		return OATextSanitize.defaultString(str, strIfNull);
	}

	/** @see OATextSanitize#notNull(String) */
	public static String notNull(String s) {
		return OATextSanitize.notNull(s);
	}

	/** @see OATextSanitize#notNull(String) */
	public static String notNull(String str, String strIfNull) {
		return OATextSanitize.notNull(str, strIfNull);
	}

	/** @see OATextSanitize#notEmpty(String, String) */
	public static String notEmpty(String str, String strIfEmpty) {
		if (isEmpty(str)) {
			return strIfEmpty;
		}
		return str;
	}

	/** @deprecated use substring */
	public static String subString(String s, int pos) {
		return substring(s, pos);
	}

	/** @see OATextFilter#substring(String, int) */
	public static String substring(String s, int pos) {
		return OATextFilter.substring(s, pos);
	}

	/** @see OATextFilter#substring(String, int, int) */
	public static String substring(String s, int pos1, int pos2) {
		return OATextFilter.substring(s, pos1, pos2);
	}

	/** @see OATextUtil#makeJavaIdentifier(String) */
	public static String makeJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	/** @see OATextUtil#makeJavaIdentifier(String) */
	public static String convertToJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	/** @see OATextUtil#makeJavaIdentifier(String) */
	public static String getJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	/** @see OATextFilter#removeEndingChars(String, int) */
	public static String removeEndingChars(String s, int amt) {
		return OATextFilter.removeEndingChars(s, amt);
	}

	/** @see OATextUtil#append(String, String, String) */
	public static String append(String orig, String append) {
		return OATextUtil.append(orig, append);
	}

	/** @see OATextUtil#append(String, String, String) */
	public static String append(String orig, String append, String sep) {
		return OATextUtil.append(orig, append, sep);
	}

	public static String prepend(String orig, String prepend, String sep) {
		return OATextUtil.prepend(orig, prepend, sep);
	}

	/** @see OATextTokenizer#csv(String, Object) */
	public static String csv(String toText, Object value) {
		return OATextTokenizer.csv(toText, value);
	}

	/** @see OATextUtil#concat(String, String, String, boolean) */
	public static String concat(String toText, String value) {
		return concat(toText, value, " ", true);
	}

	/** @see OATextUtil#concat(String, String, String, boolean) */
	public static String concat(String toText, Object value, String sepChar) {
		return OATextUtil.concat(toText, value, sepChar);
	}

	/** @see OATextUtil#concat(String, String, String, boolean) */
	public static String concat(String toText, String value, String sepChar) {
		return OATextUtil.concat(toText, value, sepChar);
	}

	/** @see OATextUtil#concat(String, String, String, boolean) */
	public static String concat(String toText, String value, String sepChar, boolean bForce) {
		return OATextUtil.concat(toText, value, sepChar, bForce);
	}

	/**
	 * @see OATextTokenizer#maskPassword(String, String)
	 */
	public static String maskPassword(String name, String val) {
		return OATextTokenizer.maskPassword(name, val);
	}

	/**
	 * @see OATextTokenizer#maskPassword(String, String, String, String...)
	 */
	public static String maskPassword(String name, String val, String passwordReturn, String... words) {
		return OATextTokenizer.maskPassword(name, val, passwordReturn, words);
	}

	/**
	 * @see OATextTokenizer#maskPassword(String, String, String...)
	 */
	public static String maskPassword(String name, String val, String... words) {
		return OATextTokenizer.maskPassword(name, val, words);
	}

	/**
	 * @see OATextTokenizer#maskPassword(String, String, String, boolean, String...)
	 */
	public static String maskPassword(String name, String value, String maskValue, boolean bCaseSensitive, String... words) {
		return OATextTokenizer.maskPassword(name, value, maskValue, bCaseSensitive, words);
	}

	public static String hilite(String line, String search) {
		return hilite(line, search, "<b style='background:yellow'>", "</b>", true);
	}

	public static String hiliteIgnoreCase(String line, String search, String beginTag, String endTag) {
		return hilite(line, search, beginTag, endTag, true);
	}

	public static String hilite(String line, String search, String beginTag, String endTag) {
		return hilite(line, search, beginTag, endTag, false);
	}

	/** @see OATextEscape#hilite(String, String, String, String, boolean) */
	public static String hilite(String line, String search, String beginTag, String endTag, boolean bIgnoreCase) {
		return OATextEscape.hilite(line, search, beginTag, endTag, bIgnoreCase);
	}

	/** @see OATextEscape#escape(String) */
	public static String escape(String raw) {
		return OATextEscape.escape(raw);
	}

	/** @see OATextEscape#unescapeJson(String) */
	public static String unescapeJson(String s) {
		return OATextEscape.unescapeJson(s);
	}

	/** @see OATextEscape#escapeJs(String, char) */
	public static String escapeJs(final String text, final char jsQuoteChar) {
		return OATextEscape.escapeJs(text, jsQuoteChar);
	}

	protected static String escapeJs(final String text, final char jsQuoteChar, final boolean bIsJsCodeEmbeddedInHtml) {
		return OATextEscape.escapeJs(text, jsQuoteChar, bIsJsCodeEmbeddedInHtml);
	}

	/** @see OATextEscape#escapeJson(String) */
	public static String escapeJson(String s) {
		return OATextEscape.escapeJson(s);
	}

	static void escapeJson(String s, StringBuffer sb) {
		OATextEscape.escapeJson(s, sb);
	}

	public static String convertToLikeSearch(String s) {
		return OATextUtil.convertToLikeSearch(s);
	}

	public static String getVerticalNumberLines(int startPos, int endPos) {
		return OATextUtil.getVerticalNumberLines(startPos, endPos);
	}

	public static String getVerticalHex(byte[] bs) {
		return OATextUtil.getVerticalHex(bs);
	}

	/** @see OATextUtil#repeat(char, int) */
	public static String repeat(char repeatChar, int length) {
		return createString(repeatChar, length);
	}

	/** @see OATextUtil#createString(char, int) */
	public static String createString(char repeatChar, int length) {
		return OATextUtil.createString(repeatChar, length);
	}

	public static String bytesToHex(byte[] bytes) {
		return OATextUtil.bytesToHex(bytes);
	}

	public static byte[] hexToBytes(String hex) {
		return OATextUtil.hexToBytes(hex);
	}

	/** @see OATextCompare#indexOf(String, String) */
	public static int indexOf(String value, String searchValue) {
		return OATextCompare.indexOf(value, searchValue, 0, false);
	}

	/** @see OATextCompare#indexOf(String, String, int, boolean) */
	public static int indexOf(String value, String searchValue, int startPos) {
		return OATextCompare.indexOf(value, searchValue, startPos);
	}

	/** @see OATextCompare#indexOf(String, String, boolean) */
	public static int indexOf(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.indexOf(value, searchValue, bIgnoreCase);
	}

	/** @see OATextCompare#indexOf(String, String, int, boolean) */
	public static int indexOf(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return OATextCompare.indexOf(value, searchValue, startPos, bIgnoreCase);
	}

	/** @see OATextCompare#indexOf(String, String) */
	public static int lastIndexOf(String value, String searchValue) {
		return OATextCompare.lastIndexOf(value, searchValue);
	}

	/** @see OATextCompare#indexOf(String, String, boolean) */
	public static int lastIndexOf(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.lastIndexOf(value, searchValue, bIgnoreCase);
	}

	
	/** @see OATextCompare#contains(String, String) */
	public static boolean contains(String value, String searchValue) {
		return OATextCompare.contains(value, searchValue);
	}
	
	/** @see OATextCompare#contains(String, String, int) */
	public static boolean contains(String value, String searchValue, int startPos) {
		return OATextCompare.contains(value, searchValue, startPos);
	}
	
	/** @see OATextCompare#contains(String, String, int, boolean) */
	public static boolean contains(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return OATextCompare.contains(value, searchValue, startPos, bIgnoreCase);
	}

	/** @see OATextAlign#left(String, int) */
	public static String getLeft(String value, int amount) {
		return OATextAlign.left(value, amount);
	}

	/** @see OATextAlign#left(String, int) */
	public static String left(String value, int amount) {
		return OATextAlign.left(value, amount);
	}

	/** @see OATextAlign#alignRight(String, int, char) */
	public static String getRight(String value, int amount) {
		return OATextAlign.right(value, amount);
	}

	/** @see OATextAlign#alignRight(String, int, char) */
	public static String right(String value, int amount) {
		return OATextAlign.right(value, amount);
	}

	/** @see OATextAlign#center(String, int) */
	public static String getCenter(String value, int amount) {
		return OATextAlign.center(value, amount);
	}
	
	/** @see OATextAlign#center(String, int) */
	public static String center(String value, int amount) {
		return OATextAlign.center(value, amount);
	}

	/** @see OATextChars#upper(String) */
	public static String upper(String value) {
		return OATextChars.upper(value);
	}

	/** @see OATextChars#upper(String) */
	public static String toUpperCase(String value) {
		return upper(value);
	}

	/** @see OATextChars#upper(String) */
	public static String getUpperCase(String value) {
		return upper(value);
	}

	/** @see OATextChars#lower(String) */
	public static String lower(String value) {
		return OATextChars.lower(value);
	}

	/** @see OATextChars#lower(String) */
	public static String toLowerCase(String value) {
		return lower(value);
	}

	/** @see OATextChars#lower(String) */
	public static String getLowerCase(String value) {
		return lower(value);
	}

	/** @see OATextCompare#startsWith(String, String) */
	public static boolean startsWith(String value, String searchValue) {
		return OATextCompare.startsWith(value, searchValue);
	}

	/** @see OATextCompare#startsWith(String, String, boolean) */
	public static boolean startsWith(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.startsWith(value, searchValue, bIgnoreCase);
	}

	/** @see OATextCompare#endsWith(String, String) */
	public static boolean endsWith(String value, String searchValue) {
		return OATextCompare.endsWith(value, searchValue);
	}

	/** @see OATextCompare#endsWith(String, String, boolean) */
	public static boolean endsWith(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.endsWith(value, searchValue, bIgnoreCase);
	}

	/** @see OATextCompare#prefixIfMissing(String, String) */
	public static String prefixIfMissing(String value, String searchValue) {
		return OATextCompare.prefixIfMissing(value, searchValue);
	}

	/** @see OATextCompare#prefixIfMissing(String, String, boolean) */
	public static String prefixIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.prefixIfMissing(value, searchValue, bIgnoreCase);
	}
	
	/** @see OATextCompare#appendIfMissing(String, String) */
	public static String appendIfMissing(String value, String searchValue) {
		return OATextCompare.appendIfMissing(value, searchValue);
	}

	/** @see OATextCompare#appendIfMissing(String, String, boolean) */
	public static String appendIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.appendIfMissing(value, searchValue, bIgnoreCase);
	}

	/** @see OATextFormat#getNumberOfDecimalPlaces(String, boolean) */
	public static int getNumberOfDecimalPlaces(String num, boolean bIgnoreTrailingZeros) {
		return OATextFormat.getNumberOfDecimalPlaces(num, bIgnoreTrailingZeros);
	}

	/** @see OATextFilter#removeLeading(String, char) */
	public static String removeLeading(String s, char ch) {
		return OATextFilter.removeLeading(s, ch);
	}

	/** @see OATextFilter#removeLeading(String, char, int) */
	public static String removeLeading(String s, char ch, int maxAmount) {
		return OATextFilter.removeLeading(s, ch, maxAmount);
	}

	public static String getClassName(Class c) {
		return OAClassUtil.getClassName(c);
	}
	
}

