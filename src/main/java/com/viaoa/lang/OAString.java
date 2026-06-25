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
package com.viaoa.lang;

import java.awt.Color;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.io.OAFile;
import com.viaoa.secure.OAEncryption;
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


/*qqqqqqqqqqqqq
CODEX


1. file/class/method
     src/main/java/com/viaoa/filter/OAContainsFilter.java / isUsed
     src/main/java/com/viaoa/filter/OAIndexOfFilter.java / isUsed
     src/main/java/com/viaoa/filter/OAStartsWithFilter.java / isUsed

  exact execution path
  Use any of these filters with bIgnoreCase == true while the JVM default locale is Turkish or another locale with
  non-English case rules. The code uppercases both strings using String.toUpperCase() with the default locale before
  indexOf / startsWith.

  why it is a real filter correctness bug
  Case-insensitive OA filtering should be deterministic across runtimes. With locale-sensitive uppercasing, the same
  filter and same data can match on one server and not match on another depending on default JVM locale.

  semantic/invariant violated
  Case-insensitive filter matching must be locale-stable unless the API explicitly says it is locale-sensitive.

  minimal fix or CODEX/defer recommendation
  Use locale-neutral case folding:
  toUpperCase(Locale.ROOT) or toLowerCase(Locale.ROOT) in all three filters.

  suggested regression test
  testContainsFilterIgnoreCaseIsLocaleStable
  testIndexOfFilterIgnoreCaseIsLocaleStable
  testStartsWithFilterIgnoreCaseIsLocaleStable

==> from Chat:
That’s a legit find and easy fix.
Use: import java.util.Locale;

Then change: s.toUpperCase()
to:
s.toUpperCase(Locale.ROOT)

s.toLowerCase(Locale.ROOT)



*/

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

	/**
	 * System-dependent line separator string.
	 */
	public static final String NL = System.getProperty("line.separator");

	/**
	 * System-dependent file separator string.
	 */
	public static final String FS = File.separator;

	/**
	 * Trims leading and trailing spaces from the supplied string.
	 *
	 * @param line the string to trim
	 * @return the trimmed string
	 */
	public static String trimSpaces(final String line) {
		return OATextFilter.trimSpaces(line);
	}

	/**
	 * Trims leading and trailing spaces from the supplied string.
	 *
	 * @param line the string to trim
	 * @return the trimmed string
	 * @deprecated use {@link #trimSpaces(String)}
	 */
	@Deprecated
	public static String trim(final String line) {
		return OATextFilter.trimSpaces(line);
	}
	
	
	/**
	 * Returns the ending portion of a string.
	 *
	 * @param text the source text
	 * @param len the number of characters to return from the end
	 * @return the ending substring
	 */
	public static String getEnd(String text, int len) {
		return OATextUtil.getEnd(text, len);
	}

	/**
	 * Returns the last characters of a string.
	 *
	 * @param text the source text
	 * @param len the number of characters to return
	 * @return the resulting substring
	 */
	public static String getLast(String text, int len) {
		return OATextUtil.getLast(text, len);
	}

	/**
	 * Returns the beginning portion of a string.
	 *
	 * @param text the source text
	 * @param len the number of characters to return from the beginning
	 * @return the beginning substring
	 */
	public static String getBegin(String text, int len) {
		return OATextUtil.getBegin(text, len);
	}

	/**
	 * Returns the first characters of a string.
	 *
	 * @param text the source text
	 * @param len the number of characters to return
	 * @return the resulting substring
	 */
	public static String getFirst(String text, int len) {
		return OATextUtil.getFirst(text, len);
	}
	
	/**
	 * Converts text to an HTML-escaped representation.
	 *
	 * @param value the text to convert
	 * @return the HTML-escaped string
	 */
	public static String convertToHtml(String value) {
		return OATextEscape.convertToHtml(value);
	}

	/**
	 * Converts text to an HTML-escaped representation.
	 *
	 * @param value the text to convert
	 * @return the HTML-escaped string
	 * @deprecated use {@link #convertToHtml(String)}
	 */
	@Deprecated()
	public static String convertToHTML(String value) {
		return OATextEscape.convertToHtml(value);
	}
	
	/**
	 * Converts text to HTML, optionally wrapping it in an HTML tag.
	 *
	 * @param value the text to convert
	 * @param bAddHTMLTag flag to indicate whether an HTML tag should be added
	 * @return the converted HTML string
	 */
	public static String convertTextToHtml(String value, boolean bAddHTMLTag) {
		return OATextEscape.convertTextToHtml(value, bAddHTMLTag);
	}
	
	/**
	 * Converts text to HTML, optionally wrapping it in an HTML tag.
	 *
	 * @param value the text to convert
	 * @param bAddHTMLTag flag to indicate whether an HTML tag should be added
	 * @return the converted HTML string
	 * @deprecated use {@link #convertTextToHtml(String, boolean)}
	 */
	@Deprecated()
	public static String convertTextToHTML(String value, boolean bAddHTMLTag) {
		return OATextEscape.convertTextToHtml(value, bAddHTMLTag);
	}
	
	
	/**
	 * Converts an HTML string back to plain text.
	 *
	 * @param html the HTML string to convert
	 * @return the decoded text
	 */
	public static String convertFromHtml(String html) {
		return OATextEscape.convertFromHtml(html);
	}
	
	/**
	 * Extracts HTML attribute name/value pairs from an HTML tag string.
	 *
	 * @param htmlTag the HTML tag text
	 * @return a map of attribute names to values
	 */
	public static Map<String, String> getHtmlAttributeMap(String htmlTag) {
		return OATextEscape.getHtmlAttributeMap(htmlTag);
	}

	@Deprecated // use getHtmlAttributeMap
	public static Map<String, String> getHTMLAttributeMap(String htmlTag) {
		return OATextEscape.getHtmlAttributeMap(htmlTag);
	}
	
	/**
	 * Converts text to an XML-escaped representation.
	 *
	 * @param value the text to convert
	 * @return the XML-escaped string
	 */
	public static String convertToXml(String value) {
		return OATextEscape.convertToXml(value);
	}

	/**
	 * Converts text to XML, optionally wrapping it in a CDATA section.
	 *
	 * @param value the text to convert
	 * @param bCData flag to indicate whether CDATA should be used
	 * @return the XML string
	 */
	public static String convertToXml(String value, boolean bCData) {
		return OATextEscape.convertToXml(value, bCData);
	}

	/**
	 * Converts text to XML, with options for CDATA usage and HTML handling.
	 *
	 * @param value the text to convert
	 * @param bCData flag to indicate whether CDATA should be used
	 * @param bIsHtml flag to indicate whether the input is HTML
	 * @return the XML string
	 */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml) {
		return OATextEscape.convertToXml(value, bCData, bIsHtml);
	}

	/**
	 * Converts text to XML with additional control over line break handling.
	 *
	 * @param value the text to convert
	 * @param bCData flag to indicate whether CDATA should be used
	 * @param bIsHtml flag to indicate whether the input is HTML
	 * @param bLeaveCRLF flag to indicate whether CR/LF characters are preserved
	 * @return the XML string
	 */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml, boolean bLeaveCRLF) {
		return OATextEscape.convertToXml(value, bCData, bIsHtml, bLeaveCRLF);
	}

	/**
	 * Determines whether the supplied text is legal XML content.
	 *
	 * @param value the text to test
	 * @return true if the text is valid XML, false otherwise
	 */
	public static boolean isLegalXml(String value) {
		return OATextEscape.isLegalXml(value);
	}

	/**
	 * Decodes any illegal XML character sequences found in the supplied text.
	 *
	 * @param value the XML text to decode
	 * @return the decoded XML text
	 */
	public static String decodeIllegalXml(String value) {
		return OATextEscape.decodeIllegalXml(value);
	}

	/**
	 * Encodes illegal XML characters in the supplied text so it can be safely used in XML.
	 *
	 * @param value the text to encode
	 * @return the encoded XML text
	 */
	public static String encodeIllegalXml(String value) {
		return OATextEscape.encodeIllegalXml(value);
	}
	
	/**
	 * Replaces occurrences of a character in the supplied text with a replacement string.
	 *
	 * @param value the source text
	 * @param c the character to replace
	 * @param replace the replacement string
	 * @return the converted string
	 */
	public static String convert(String value, char c, String replace) {
		return OATextFilter.convert(value, c, replace);
	}

	/**
	 * Replaces occurrences of a search string with a replacement string, ignoring case.
	 *
	 * @param line the source text
	 * @param search the text to search for
	 * @param replace the replacement text
	 * @return the converted string
	 */
	public static String convertIgnoreCase(String line, String search, String replace) {
		return OATextFilter.convertIgnoreCase(line, search, replace);
	}

	/**
	 * Replaces occurrences of a search string with a replacement string.
	 *
	 * @param line the source text
	 * @param search the text to search for
	 * @param replace the replacement text
	 * @return the converted string
	 */
	public static String convert(String line, String search, String replace) {
		return OATextFilter.convert(line, search, replace);
	}

	/**
	 * Removes all characters found in the search string from the supplied text.
	 *
	 * @param line the source text
	 * @param search characters to remove
	 * @return the resulting string
	 */
	public static String removeCharacters(String line, String search) {
		return OATextFilter.removeCharacters(line, search);
	}

	/**
	 * Removes all characters from the supplied text except those listed to keep.
	 *
	 * @param line the source text
	 * @param keep characters to retain
	 * @return the resulting string
	 */
	public static String removeOtherCharacters(String line, String keep) {
		return OATextFilter.removeOtherCharacters(line, keep);
	}

	/**
	 * Removes all non-digit characters from the supplied text.
	 *
	 * @param line the source text
	 * @return the string containing only digits
	 */
	public static String removeNonDigits(String line) {
		return OATextFilter.removeNonDigits(line);
	}

	/**
	 * Removes all non-digit characters from the supplied text, optionally allowing dots.
	 *
	 * @param line the source text
	 * @param bAllowDot flag to allow dot characters
	 * @return the filtered string
	 */
	public static String removeNonDigits(String line, boolean bAllowDot) {
		return OATextFilter.removeNonDigits(line, bAllowDot);
	}

	/**
	 * Removes characters from the supplied text that are not valid in file names.
	 *
	 * @param line the source text
	 * @return the sanitized file-name-safe string
	 */
	public static String removeNonFileNameChars(String line) {
		return OATextFilter.removeNonFileNameChars(line);
	}

	/**
	 * Replaces occurrences of a search string with a replacement string, with optional case sensitivity.
	 *
	 * @param line the source text
	 * @param search the text to search for
	 * @param replace the replacement text
	 * @param bIgnoreCase flag to ignore case when searching
	 * @return the converted string
	 */
	public static String convert(String line, String search, String replace, boolean bIgnoreCase) {
		return OATextFilter.convert(line, search, replace, bIgnoreCase);
	}

	/**
	 * Replaces occurrences of a search string with a replacement string using detailed control options.
	 *
	 * @param line the source text
	 * @param search the text to search for
	 * @param replace the replacement text
	 * @param bIgnoreCase flag to ignore case when searching
	 * @param bFirstOnly flag to replace only the first occurrence
	 * @param startPos starting position for searching
	 * @param endPos ending position for searching
	 * @return the converted string
	 */
	public static String convert(final String line, String search, String replace, final boolean bIgnoreCase, final boolean bFirstOnly, final int startPos, final int endPos) {
		return OATextFilter.convert(line, search, replace, bIgnoreCase, bFirstOnly, startPos, endPos);
	}

	/**
	 * Converts text to camelCase.
	 *
	 * @param value the source text
	 * @return the camelCase string
	 */
	public static String convertToCamelCase(String value) {
		return OATextFormat.convertToCamelCase(value);
	}

	/**
	 * Converts text to camelCase using custom separator characters.
	 *
	 * @param value the source text
	 * @param sepChars characters used as word separators
	 * @return the camelCase string
	 */
	public static String convertToCamelCase(String value, String sepChars) {
		return OATextFormat.convertToCamelCase(value, sepChars);
	}

	/**
	 * Converts text to Hungarian notation.
	 *
	 * @param value the source text
	 * @return the converted string
	 */
	public static String convertToHungarian(String value) {
		return OATextFormat.convertToHungarian(value);
	}

	@Deprecated // use convertToHungarian
	public static String convertHungarian(String value) {
		return OATextFormat.convertToHungarian(value);
	}
	
	/**
	 * Converts text to Hungarian notation using custom separator characters.
	 *
	 * @param value the source text
	 * @param sepChars characters used as word separators
	 * @return the converted string
	 */
	public static String convertToHungarian(String value, String sepChars) {
		return OATextFormat.convertToHungarian(value, sepChars);
	}

	/**
	 * Returns an abbreviated version of a name using a default maximum length.
	 *
	 * @param name the source name
	 * @return the abbreviated name
	 */
	public static String getAbbrev(String name) {
		return getShortName(name);
	}

	/**
	 * Returns a shortened name using a default maximum length.
	 *
	 * @param name the source name
	 * @return the shortened name
	 */
	public static String getShortName(String name) {
		return getShortName(name, 3);
	}

	/**
	 * Returns a shortened version of a name limited to a maximum length.
	 *
	 * @param name the source name
	 * @param max maximum length of the result
	 * @return the shortened name
	 */
	public static String getShortName(final String name, final int max) {
		return OATextGrammar.getShortName(name, max);
	}
	
	/**
	 * Converts a value into a display-friendly name.
	 *
	 * @param value the source value
	 * @return the display name
	 */
	public static String getDisplayName(String value) {
		return OATextGrammar.getDisplayName(value);
	}

	/**
	 * Creates a display-friendly name from the supplied value.
	 *
	 * @param value the source value
	 * @return the generated display name
	 */
	public static String createDisplayName(String value) {
		return OATextGrammar.createDisplayName(value);
	}

	/**
	 * Converts a value to a display-friendly name.
	 *
	 * @param value the source value
	 * @return the display name
	 */
	public static String convertToDisplayName(String value) {
		return OATextGrammar.convertToDisplayName(value);
	}

	/**
	 * Returns the singular form of the supplied word.
	 *
	 * @param str the source word
	 * @return the singular form
	 */
	public static String getSingular(String str) {
		return OATextGrammar.makeSingular(str);
	}
	
	/**
	 * Converts a word to its singular form.
	 *
	 * @param str the source word
	 * @return the singular form
	 */
	public static String makeSingular(String str) {
		return OATextGrammar.makeSingular(str);
	}
	
	/**
	 * Returns the plural form of the supplied word.
	 *
	 * @param s the source word
	 * @return the plural form
	 */
	public static String getPlural(String s) {
		return makePlural(s);
	}

	/**
	 * Converts a word to its plural form.
	 *
	 * @param str the source word
	 * @return the plural form
	 */
	public static String makePlural(String str) {
		return OATextGrammar.makePlural(str);
	}
	
	/**
	 * Returns the appropriate indefinite article ("a" or "an") for the supplied word.
	 *
	 * @param s the source word
	 * @return "a" or "an" depending on the word
	 */
	public static String getAorAn(String s) {
		return OATextGrammar.getAorAn(s);
	}

	/**
	 * Converts a word to its possessive form.
	 *
	 * @param str the source word
	 * @return the possessive form
	 */
	public static String makePossessive(String str) {
		return OATextGrammar.makePossessive(str);
	}

	/**
	 * Returns the possessive form of the supplied word.
	 *
	 * @param str the source word
	 * @return the possessive form
	 */
	public static String getPossessive(String str) {
		return OATextGrammar.getPossessive(str);
	}

	/**
	 * Converts text to title case.
	 *
	 * @param s the source text
	 * @return the title-cased string
	 */
	public static String getTitle(String s) {
		return OATextGrammar.getTitle(s);
	}
	
	public static String getTitle(String s, String basedOn) {
		return OATextGrammar.getTitle(s, basedOn);
	}

	
	/**
	 * Delegates to {@link #getTitle(String)}.
	 *
	 * @param s the source text
	 * @return the title-cased string
	 */
	public static String getTitleCase(String s) {
		return getTitle(s);
	}

	/**
	 * Delegates to {@link #getTitle(String)}.
	 *
	 * @param s the source text
	 * @return the title-cased string
	 */
	public static String toTitleCase(String s) {
		return getTitle(s);
	}

	/**
	 * Delegates to {@link #getTitle(String)}.
	 *
	 * @param s the source text
	 * @return the title-cased string
	 */
	public static String titleCase(String s) {
		return getTitle(s);
	}

	/**
	 * Converts the first character of the string to lower case.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String makeFirstCharLower(String s) {
		return OATextChars.makeFirstCharLower(s);
	}

	/**
	 * Converts the first character of the string to lower case.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String mfcl(String s) {
		return makeFirstCharLower(s);
	}

	/**
	 * Converts the first sequence of uppercase characters in the string to lower case.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String makeFirstUpperCharsLower(String s) {
		return OATextChars.makeFirstUpperCharsLower(s);
	}

	/**
	 * Delegates to {@link #makeFirstUpperCharsLower(String)}.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String mfucl(String s) {
		return makeFirstUpperCharsLower(s);
	}

	/**
	 * Converts the first character of the string to upper case.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String makeFirstCharUpper(String s) {
		return OATextChars.makeFirstCharUpper(s);
	}

	/**
	 * Delegates to {@link #makeFirstCharUpper(String)}.
	 *
	 * @param s the source text
	 * @return the modified string
	 */
	public static String mfcu(String s) {
		return makeFirstCharUpper(s);
	}
	
	/**
	 * Extracts a field from a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @param beg the starting field index
	 * @return the extracted field
	 * @deprecated use {@link #fieldAt(String,String,int)}
	 */
	@Deprecated()
	public static String field(String str, String sep, int beg) {
		return OATextTokenizer.field(str, sep, beg);
	}
	
	/**
	 * Extracts multiple fields from a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @param beg the starting field index
	 * @param amt the number of fields to extract
	 * @return the extracted fields
	 * @deprecated use {@link #fieldAt(String,String,int,int)}
	 */
	@Deprecated()
	public static String field(final String str, final String sep, final int beg, final int amt) {
		return OATextTokenizer.field(str, sep, beg, amt);
	}
	
	/**
	 * Extracts a field from a delimited string using a character separator.
	 *
	 * @param str the source string
	 * @param sep the field separator character
	 * @param beg the starting field index
	 * @return the extracted field
	 * @deprecated use {@link #fieldAt(String,char,int)}
	 */
	@Deprecated()
	public static String field(String str, char sep, int beg) {
		return OATextTokenizer.field(str, sep, beg);
	}

	/**
	 * Extracts multiple fields from a delimited string using a character separator.
	 *
	 * @param str the source string
	 * @param sep the field separator character
	 * @param beg the starting field index, one-based
	 * @param amt the number of fields to extract
	 * @return the extracted fields
	 * @deprecated use {@link #fieldAt(String,char,int,int)}
	 */
	@Deprecated()
	public static String field(String str, char sep, int beg, int amt) {
		return OATextTokenizer.field(str, sep, beg, amt);
	}

	/**
	 * Extracts a field from a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @param beg the starting field index, zero-based
	 * @return the extracted field
	 */
	public static String fieldAt(String str, String sep, int beg) {
		return OATextTokenizer.fieldAt(str, sep, beg);
	}

	/**
	 * Extracts multiple fields from a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @param beg the starting field index, zero-based
	 * @param amt the number of fields to extract
	 * @return the extracted fields
	 */
	public static String fieldAt(final String str, final String sep, final int beg, final int amt) {
		return OATextTokenizer.fieldAt(str, sep, beg, amt);
	}
	
	/**
	 * Extracts a field from a delimited string using a character separator.
	 *
	 * @param str the source string
	 * @param sep the field separator character
	 * @param beg the starting field index
	 * @return the extracted field
	 */
	public static String fieldAt(String str, char sep, int beg) {
		return OATextTokenizer.fieldAt(str, sep, beg);
	}
	
	/**
	 * Extracts multiple fields from a delimited string using a character separator.
	 *
	 * @param str the source string
	 * @param sep the field separator character
	 * @param beg the starting field index
	 * @param amt the number of fields to extract
	 * @return the extracted fields
	 */
	public static String fieldAt(String str, char sep, int beg, int amt) {
		return OATextTokenizer.fieldAt(str, sep, beg, amt);
	}

	/**
	 * Counts the number of fields in a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @return the number of fields
	 */
	public static int count(String str, String sep) {
		return OATextTokenizer.count(str, sep);
	}

	/**
	 * Counts the number of occurrences of a separator string in the source text.
	 *
	 * @param str the source string
	 * @param sep the separator string to count
	 * @return the number of matches
	 */
	public static int countMatches(String str, String sep) {
		return OATextTokenizer.countMatches(str, sep);
	}

	/**
	 * Counts the number of occurrences of a separator character in the source text.
	 *
	 * @param str the source string
	 * @param sep the separator character to count
	 * @return the number of matches
	 */
	public static int countMatches(String str, char sep) {
		return OATextTokenizer.countMatches(str, sep);
	}

	/**
	 * Counts the number of fields in a delimited string.
	 *
	 * @param str the source string
	 * @param sep the field separator
	 * @return the number of fields
	 */
	public static int dcount(String str, String sep) {
		return OATextTokenizer.dcount(str, sep);
	}

	/**
	 * Counts the number of fields in a delimited string using a character separator.
	 *
	 * @param str the source string
	 * @param sep the field separator character
	 * @return the number of fields
	 */
	public static int dcount(String str, char sep) {
		return OATextTokenizer.dcount(str, sep);
	}

	/**
	 * Pads the beginning of a string with spaces to reach a specified length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @return the padded string
	 */
	public static String padStart(String value, int amount) {
		return pad(value, amount, false, ' ');
	}

	/**
	 * Pads the beginning of a string with spaces to reach a specified length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @return the padded string
	 */
	public static String leftPad(String value, int amount) {
		return pad(value, amount, false, ' ');
	}

	/**
	 * Pads the beginning of a string with a specified character to reach a desired length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @param padChar the character used for padding
	 * @return the padded string
	 */
	public static String padStart(String value, int amount, char padChar) {
		return pad(value, amount, false, padChar);
	}

	/**
	 * Pads the beginning of a string with a specified character to reach a desired length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @param padChar the character used for padding
	 * @return the padded string
	 */
	public static String leftPad(String value, int amount, char padChar) {
		return pad(value, amount, false, padChar);
	}

	/**
	 * Pads the end of a string with spaces to reach a specified length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @return the padded string
	 */
	public static String padEnd(String value, int amount) {
		return pad(value, amount, true, ' ');
	}

	/**
	 * Pads the end of a string with spaces to reach a specified length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @return the padded string
	 */
	public static String padRight(String value, int amount) {
		return pad(value, amount, true, ' ');
	}

	/**
	 * Pads the end of a string with a specified character to reach a desired length.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @param padChar the character used for padding
	 * @return the padded string
	 */
	public static String padEnd(String value, int amount, char padChar) {
		return pad(value, amount, true, padChar);
	}

	/**
	 * Pads a string either at the beginning or the end with a specified character.
	 *
	 * @param value the source string
	 * @param amount the total desired length
	 * @param bAddToEnd true to pad at the end, false to pad at the beginning
	 * @param padCharacter the character used for padding
	 * @return the padded string
	 */
	public static String pad(String value, int amount, boolean bAddToEnd, char padCharacter) {
		if (bAddToEnd)
			return OATextAlign.padEnd(value, amount, padCharacter);
		return OATextAlign.padStart(value, amount, padCharacter);
	}
	
	/**
	 * Aligns text to the left within a fixed width using a padding character.
	 *
	 * @param value the source string
	 * @param width the total width of the result
	 * @param charPad the character used for padding
	 * @return the left-aligned string
	 */
	public static String alignLeft(String value, int width, char charPad) {
		return OATextAlign.alignLeft(value, width, charPad);
	}

	/**
	 * Aligns text to the right within a fixed width using a padding character.
	 *
	 * @param value the source string
	 * @param width the total width of the result
	 * @param charPad the character used for padding
	 * @return the right-aligned string
	 */
	public static String alignRight(String value, int width, char charPad) {
		return OATextAlign.alignRight(value, width, charPad);
	}

	/**
	 * Centers text within a fixed width using a padding character.
	 *
	 * @param value the source string
	 * @param width the total width of the result
	 * @param charPad the character used for padding
	 * @return the centered string
	 */
	public static String alignCenter(String value, int width, char charPad) {
		return OATextAlign.alignCenter(value, width, charPad);
	}

	/**
	 * Aligns text within a fixed width either left or right.
	 *
	 * @param value the source string
	 * @param width the total width of the result
	 * @param bAlignLeft true to align left, false to align right
	 * @param charPad the character used for padding
	 * @return the aligned string
	 */
	public static String align(String value, int width, boolean bAlignLeft, char charPad) {
		return OATextAlign.align(value, width, bAlignLeft, charPad);
	}
	
	/**
	 * Formats a long value using the specified format.
	 *
	 * @param value the numeric value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(long value, String format) {
		return OAConv.toString(value, format);
	}

	/**
	 * Formats an integer value using the specified format.
	 *
	 * @param value the numeric value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(int value, String format) {
		// see which format to use
		String s = format == "" ? null : format.toUpperCase();
		if (s.indexOf('R') >= 0 || s.indexOf('L') >= 0 || s.indexOf('C') >= 0) {
			return OAString.format(Integer.toString(value), format);
		}
		return OAConv.toString(value, format);
	}

	/**
	 * Formats a double value using the specified format.
	 *
	 * @param value the numeric value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(double value, String format) {
		String s = format == "" ? null : format.toUpperCase();
		if (s.indexOf('R') >= 0 || s.indexOf('L') >= 0 || s.indexOf('C') >= 0) {
			return OAString.format(Double.toString(value), format);
		}
		return OAConv.toString(value, format);
	}

	/**
	 * Formats a boolean value using the specified format.
	 *
	 * @param value the boolean value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(boolean value, String format) {
		return OAConv.toString(value, format);
	}

	/**
	 * Formats an OADateTime value using the specified format.
	 *
	 * @param value the date-time value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(OADateTime value, String format) {
		return OAConv.toString(value, format);
	}

	/**
	 * Formats an OADate value using the global output format.
	 *
	 * @param value the date value to format
	 * @return the formatted string
	 */
	public static String format(OADate value) {
		return OAConv.toString(value, OADate.getGlobalOutputFormat());
	}

	/**
	 * Formats a string value using the specified format.
	 *
	 * @param str the string value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String format(String str, String format) {
		return fmt(str, format);
	}

	/**
	 * Delegates to {@link #fmt(String,String)}.
	 *
	 * @param str the string value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String pickFormat(String str, String format) {
		return fmt(str, format);
	}

	/**
	 * Formats a string using the specified format.
	 *
	 * @param str the string value to format
	 * @param format the format string
	 * @return the formatted string
	 */
	public static String fmt(String str, String format) {
		String s = OATextFormat.fmt(str, format);
		return s;
	}

	/**
	 * Returns the supplied string, or an empty string if the value is null.
	 *
	 * @param str the source string
	 * @return the non-null string
	 */
	public static String fmt(String str) {
		if (str == null) return "";
		return str;
	}

	/**
	 * Removes all digit characters from the supplied string.
	 *
	 * @param value the source string
	 * @return the string without digits
	 */
	public static String stripDigits(String value) {
		return OATextFilter.stripDigits(value);
	}

	/**
	 * Applies a mask pattern to the supplied value.
	 *
	 * @param value the source string
	 * @param mask the mask pattern
	 * @return the masked string
	 */
	public static String mask(String value, String mask) {
		return mask(value, mask, false);
	}

	/**
	 * Applies a mask pattern to the supplied value with optional right justification.
	 *
	 * @param value the source string
	 * @param mask the mask pattern
	 * @param bRightJustified true to right-justify the value
	 * @return the masked string
	 */
	public static String mask(String value, String mask, boolean bRightJustified) {
		return OATextFormat.mask(value, mask, bRightJustified);
	}

	/**
	 * Removes the specified characters from the supplied string.
	 *
	 * @param value the source string
	 * @param chars the characters to remove
	 * @return the stripped string
	 */
	public static String strip(String value, String chars) {
		return OATextFilter.strip(value, chars);
	}

	/**
	 * Retains only the specified characters in the supplied string.
	 *
	 * @param value the source string
	 * @param chars the characters to retain
	 * @return the filtered string
	 */
	public static String accept(String value, String chars) {
		return OATextFilter.accept(value, chars);
	}

	/**
	 * Converts a string into a valid file name.
	 *
	 * @param fileName the source file name
	 * @return the converted file name
	 */
	public static String convertFileName(String fileName) {
		return OAFile.convertFileName(fileName);
	}

	/**
	 * Converts a string into a valid file name, with optional trailing file separator.
	 *
	 * @param fileName the source file name
	 * @param bEndWithSlashChar flag indicating whether the result should end with a file separator
	 * @return the converted file name
	 */
	public static String convertFileName(String fileName, boolean bEndWithSlashChar) {
		return OAFile.convertFileName(fileName, bEndWithSlashChar);
	}

	/**
	 * Extracts the file name portion from a file path.
	 *
	 * @param filePath the full file path
	 * @return the file name component
	 */
	public static String getFileName(String filePath) {
		return OAFile.getFileName(filePath);
	}

	/**
	 * Extracts the directory portion from a file path.
	 *
	 * @param filePath the full file path
	 * @return the directory path
	 */
	public static String getDirectoryName(String filePath) {
		return OAFile.getDirectoryName(filePath);
	}

	/**
	 * Converts a Color value to a hexadecimal color string.
	 *
	 * @param color the Color to convert
	 * @return the hexadecimal color representation
	 */
	public static String colorToHex(Color color) {
		return OATextUtil.colorToHex(color);
	}

	/**
	 * Determines whether the supplied string contains any digit characters.
	 *
	 * @param word the string to test
	 * @return true if the string contains digits, false otherwise
	 */
	public static boolean hasDigits(String word) {
		return OATextChars.hasDigits(word);
	}

	/**
	 * Computes the Soundex code for the supplied word.
	 *
	 * @param word the input word
	 * @return the Soundex code
	 */
	public static String soundex(String word) {
		return OATextSoundex.soundex(word);
	}

	/**
	 * Determines whether the supplied string represents a numeric value.
	 *
	 * @param str the string to test
	 * @return true if the string is numeric, false otherwise
	 */
	public static boolean isNumber(String str) {
		return OATextFormat.isNumber(str);
	}

	/**
	 * Determines whether the supplied string represents an integer value.
	 *
	 * @param str the string to test
	 * @return true if the string is an integer, false otherwise
	 */
	public static boolean isInteger(String str) {
		return OATextFormat.isInteger(str);
	}

	/**
	 * Determines whether the supplied string represents a date.
	 *
	 * @param s the string to test
	 * @return true if the string is a date, false otherwise
	 */
	public static boolean isDate(String s) {
		return OATextFormat.isDate(s);
	}

	/**
	 * Determines whether the supplied string represents a time.
	 *
	 * @param s the string to test
	 * @return true if the string is a time, false otherwise
	 */
	public static boolean isTime(String s) {
		return OATextFormat.isTime(s);
	}

	/**
	 * Determines whether the supplied string represents a date-time value.
	 *
	 * @param s the string to test
	 * @return true if the string is a date-time, false otherwise
	 */
	public static boolean isDateTime(String s) {
		return OATextFormat.isDateTime(s);
	}

	/**
	 * Compares two strings for equality with optional case-insensitive comparison.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case during comparison
	 * @return true if the strings are equal, false otherwise
	 */
	public static boolean equals(String s1, String s2, boolean bIgnoreCase) {
		return OATextCompare.equals(s1, s2, bIgnoreCase);
	}
	
	/**
	 * Compares two strings for equality.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return true if the strings are equal, false otherwise
	 */
	public static boolean equals(String s1, String s2) {
		return OATextCompare.equals(s1, s2);
	}

	/**
	 * Determines whether two strings are not equal.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean notEquals(String s1, String s2) {
		return OATextCompare.notEquals(s1, s2);
	}

	/**
	 * Determines whether two strings are not equal with optional case-insensitive comparison.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case during comparison
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean notEquals(String s1, String s2, boolean bIgnoreCase) {
		return OATextCompare.notEquals(s1, s2, bIgnoreCase);
	}
	
	/**
	 * Converts an object to a non-null string representation.
	 *
	 * @param obj the object to convert
	 * @return the string representation
	 */
	public static String toString(Object obj) {
		return OATextSanitize.toString(obj);
	}

	/**
	 * Converts a string to a non-null string representation.
	 *
	 * @param str the source string
	 * @return the string representation
	 */
	public static String toString(String str) {
		return OATextSanitize.toString(str);
	}

	/**
	 * Converts a string to a non-null string representation using a default value if null.
	 *
	 * @param str the source string
	 * @param strIfNull the value to return if the source is null
	 * @return the resulting string
	 */
	public static String toString(String str, String strIfNull) {
		return OATextSanitize.defaultString(str, strIfNull);
	}

	/**
	 * Converts a byte array to a String using the default character set.
	 *
	 * @param bytes the byte array to convert
	 * @return the resulting string
	 */
	public static String toString(byte[] bytes) {
		return new String(bytes, Charset.defaultCharset());
	}
	
	/**
	 * Converts an integer to a formatted number string.
	 *
	 * @param x the integer value
	 * @return the formatted number string
	 */
	public static String toNumberString(int x) {
		return OATextFormat.toNumberString(x);
	}

	/**
	 * Truncates text to a single line with a maximum width.
	 *
	 * @param text the source text
	 * @param width the maximum width
	 * @return the truncated string
	 */
	public static String truncate(String text, int width) {
		OATextLineWrap wrap = new OATextLineWrap(width, "|").withMaxRows(1);
		String result = wrap.wrapToString(text);
		return result;
	}
	
	/**
	 * Delegates to {@link #truncate(String,int)}.
	 *
	 * @param orig the source text
	 * @param width the maximum width
	 * @return the truncated string
	 */
	public static String trunc(String orig, int width) {
		return truncate(orig, width);
	}

	/**
	 * Delegates to {@link #truncate(String,int)}.
	 *
	 * @param orig the source text
	 * @param width the maximum width
	 * @return the truncated string
	 */
	public static String abbreviate(String orig, int width) {
		return truncate(orig, width);
	}


	/**
	 * Generates a random string with a length between the specified bounds.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated random string
	 */
	public static String getRandomString(int min, int max) {
		return OATextGenerate.getRandomString(min, max);
	}

	/**
	 * Delegates to {@link #getRandomString(int,int)}.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated random string
	 */
	public static String createRandomString(int min, int max) {
		return OATextGenerate.getRandomString(min, max);
	}

	/**
	 * Generates a random string using a normal length with minimum and maximum bounds.
	 *
	 * @param normal preferred length
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated random string
	 */
	public static String getRandomString(int normal, int min, int max) {
		return OATextGenerate.getRandomString(normal, min, max);
	}

	/**
	 * Generates a random string with configurable character options.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @param bUseDigits whether to include digits
	 * @param bUseAlpha whether to include alphabetic characters
	 * @param bCapFirstChar whether to capitalize the first character
	 * @return the generated random string
	 */
	public static String getRandomString(int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return OATextGenerate.getRandomString(min, max, bUseDigits, bUseAlpha, bCapFirstChar);
	}

	/**
	 * Generates a random string with configurable length and character options.
	 *
	 * @param normal preferred length
	 * @param min minimum length
	 * @param max maximum length
	 * @param bUseDigits whether to include digits
	 * @param bUseAlpha whether to include alphabetic characters
	 * @param bCapFirstChar whether to capitalize the first character
	 * @return the generated random string
	 */
	public static String getRandomString(int normal, int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return OATextGenerate.getRandomString(normal, min, max, bUseDigits, bUseAlpha, bCapFirstChar);
	}

	/**
	 * Generates a random numeric string.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated numeric string
	 */
	public static String getRandomDigits(int min, int max) {
		return OATextGenerate.createDigits(min, max);
	}

	
	/**
	 * Generates sample text with a fixed length.
	 *
	 * @param len desired length
	 * @return the generated sample text
	 */
	public static String getSampleText(int len) {
		return getDummyText(len, len, len);
	}

	/**
	 * Generates sample text with configurable length bounds.
	 *
	 * @param normal preferred length
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated sample text
	 */
	public static String getSampleText(int normal, int min, int max) {
		return getDummyText(normal, min, max);
	}

	/**
	 * Generates dummy text with configurable length bounds.
	 *
	 * @param normal preferred length
	 * @param min minimum length
	 * @param max maximum length
	 * @return the generated dummy text
	 */
	public static String getDummyText(int normal, int min, int max) {
		return OATextGenerate.getDummyText(normal, min, max);
	}
	
	/**
	 * Creates a dot-separated property path from the supplied arguments.
	 *
	 * @param args path segments
	 * @return the property path string
	 */
	public static String createPropertyPath(String... args) {
		return OATextUtil.createPropertyPath(args);
	}

	/**
	 * Delegates to {@link #createPropertyPath(String...)}.
	 *
	 * @param args path segments
	 * @return the property path string
	 */
	public static String cpp(String... args) {
		return OATextUtil.createPropertyPath(args);
	}
	
	/**
	 * Creates a dot-separated property path prefixed with a class name.
	 *
	 * @param clazz the class used as a prefix
	 * @param args path segments
	 * @return the property path string
	 */
	public static String createPropertyPath(Class clazz, String... args) {
		return OATextUtil.createPropertyPath(clazz, args);
	}

	/**
	 * Delegates to {@link #createPropertyPath(Class,String...)}.
	 *
	 * @param clazz the class used as a prefix
	 * @param args path segments
	 * @return the property path string
	 */
	public static String cpp(Class clazz, String... args) {
		return createPropertyPath(clazz, args);
	}

	/**
	 * Converts a string to UTF-8 encoding.
	 *
	 * @param isoString the source string
	 * @return the UTF-8 encoded string
	 */
	public static String toUtf8(String isoString) {
		return OATextFormat.toUTF8(isoString);
	}

	/**
	 * Computes a SHA hash for the supplied input string.
	 *
	 * @param input the input text
	 * @return the hash value
	 */
	public static String getSHAHash(String input) {
		return OAEncryption.getHash(input);
	}

	/**
	 * Delegates to {@link #getSHAHash(String)}.
	 *
	 * @param input the input text
	 * @return the hash value
	 */
	public static String convertToSHAHash(String input) {
		return OAEncryption.getHash(input);
	}

	/**
	 * Inserts line breaks into text based on column width and row limits.
	 *
	 * @param text the source text
	 * @param columnWidth the maximum column width
	 * @param separator the line separator string
	 * @param maxRows maximum number of rows
	 * @return the formatted string
	 */
	public static String lineBreak(String text, int columnWidth, String separator, int maxRows) {
		OATextLineWrap wrap = new OATextLineWrap(columnWidth, separator).withMaxRows(maxRows);
		return wrap.wrapToString(text);
	}
	
	/**
	 * Inserts line breaks into text based on column width and row limits.
	 *
	 * @param text the source text
	 * @param columnWidth the maximum column width
	 * @param separator the line separator string
	 * @param maxRows maximum number of rows
	 * @return the formatted string
	 */
	public static boolean notEmpty(Object obj) {
		return OATextSanitize.notEmpty(obj);
	}

	/**
	 * Determines whether the supplied object is not empty.
	 *
	 * @param obj the object to test
	 * @return true if the object is not empty, false otherwise
	 */
	public static boolean isNotEmpty(Object obj) {
		return OATextSanitize.isNotEmpty(obj);
	}

	/**
	 * Determines whether the supplied object is not null and not empty.
	 *
	 * @param obj the object to test
	 * @return true if the object is not null and not empty, false otherwise
	 */
	public static boolean isNotNullAndNotEmpty(Object obj) {
		return OATextSanitize.isNotNullAndNotEmpty(obj);
	}

	/**
	 * Determines whether the supplied object is empty.
	 *
	 * @param obj the object to test
	 * @return true if the object is empty, false otherwise
	 */
	public static boolean isEmpty(Object obj) {
		return OATextSanitize.isEmpty(obj);
	}

	/**
	 * Determines whether the supplied object is empty, with optional trimming.
	 *
	 * @param obj the object to test
	 * @param bTrim whether to trim text before testing
	 * @return true if the object is empty, false otherwise
	 */
	public static boolean isEmpty(Object obj, boolean bTrim) {
		return OATextSanitize.isEmpty(obj, bTrim);
	}

	/**
	 * Determines whether the supplied object is null or empty.
	 *
	 * @param obj the object to test
	 * @return true if the object is null or empty, false otherwise
	 */
	public static boolean isNullOrEmpty(Object obj) {
		return OATextSanitize.isEmpty(obj, false);
	}
	
	/**
	 * Determines whether two strings are equal.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @return true if the strings are equal, false otherwise
	 */
	public static boolean isEqual(String s, String s2) {
		return OATextCompare.isEqual(s, s2, false);
	}

	/**
	 * Determines whether two strings are equal with optional case-insensitive comparison.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case
	 * @return true if the strings are equal, false otherwise
	 */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase) {
		return OATextCompare.isEqual(s, s2, bIgnoreCase);
	}

	/**
	 * Determines whether two strings are equal with optional null-equals-blank handling.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case
	 * @param bNullEqualsBlank true to treat null as blank
	 * @return true if the strings are equal, false otherwise
	 */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return OATextCompare.isEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/**
	 * Determines whether two strings are equal ignoring case.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @return true if the strings are equal ignoring case, false otherwise
	 */
	public static boolean isEqualIgnoreCase(String s, String s2) {
		return OATextCompare.isEqualIgnoreCase(s, s2);
	}

	/**
	 * Determines whether two strings are equal ignoring case.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return true if the strings are equal ignoring case, false otherwise
	 */
	public static boolean equalsIgnoreCase(String s1, String s2) {
		return OATextCompare.isEqualIgnoreCase(s1, s2);
	}

	/**
	 * Determines whether two strings are equal treating null and blank as equivalent.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @return true if the strings are considered equal, false otherwise
	 */
	public static boolean isEqualNullEqualsBlank(String s, String s2) {
		return OATextCompare.isEqualNullEqualsBlank(s, s2);
	}

	/**
	 * Determines whether two strings are not equal.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean isNotEqual(String s, String s2) {
		return OATextCompare.isNotEqual(s, s2);
	}

	/**
	 * Determines whether two strings are not equal with optional case-insensitive comparison.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase) {
		return OATextCompare.isNotEqual(s, s2, bIgnoreCase);
	}

	/**
	 * Determines whether two strings are not equal with optional null-equals-blank handling.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @param bIgnoreCase true to ignore case
	 * @param bNullEqualsBlank true to treat null as blank
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return OATextCompare.isNotEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/**
	 * Determines whether two strings are not equal treating null and blank as equivalent.
	 *
	 * @param s the first string
	 * @param s2 the second string
	 * @return true if the strings are not equal, false otherwise
	 */
	public static boolean isNotEqualNullEqualsBlank(String s, String s2) {
		return OATextCompare.isNotEqualNullEqualsBlank(s, s2);
	}
	
	/**
	 * Determines whether a string loosely matches another string.
	 *
	 * @param s the source string
	 * @param s2 the pattern string
	 * @return true if the strings are considered a match, false otherwise
	 */
	public static boolean isLike(String s, String s2) {
		return OATextCompare.isLike(s, s2);
	}
	
	/**
	 * Compares two strings lexicographically.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return a comparison result value
	 */
	public static int compare(String s1, String s2) {
		return OATextCompare.compare(s1, s2);
	}

	/**
	 * Converts a string to a valid phone number format.
	 *
	 * @param phone the source phone number
	 * @return the normalized phone number string
	 */
	public static String convertToValidPhoneNumber(String phone) {
		return OATextFormat.convertToValidPhoneNumber(phone);
	}

	/**
	 * Indents text by a specified number of spaces.
	 *
	 * @param text the source text
	 * @param amt the number of spaces to indent
	 * @return the indented text
	 */
	public static String indent(String text, int amt) {
		return OATextFormat.indent(text, amt);
	}

	/**
	 * Removes common leading indentation from the supplied text.
	 *
	 * @param text the source text
	 * @return the unindented text
	 */
	public static String unindent(String text) {
		return OATextFormat.unindent(text);
	}

	/**
	 * Removes common leading indentation from code-formatted text.
	 *
	 * @param text the source text
	 * @return the unindented code text
	 */
	public static String unindentCode(String text) {
		return OATextFormat.unindentCode(text);
	}

	/**
	 * Removes leading indentation from the supplied text with optional first-line basis.
	 *
	 * @param text the source text
	 * @param bBasedOnFirstLine true to base indentation removal on the first line
	 * @return the unindented text
	 */
	public static String unindent(String text, boolean bBasedOnFirstLine) {
		return OATextFormat.unindent(text, bBasedOnFirstLine);
	}

	    
	/**
	 * Trims trailing whitespace from each line of the supplied text.
	 *
	 * @param text the source text
	 * @return the trimmed text
	 */
	public static String trimEndingWhitespace(String text) {
		return OATextFormat.trimEndingWhitespace(text);
	}

	/**
	 * Parses a delimited line into fields, optionally supporting quoted values.
	 *
	 * @param line the source line
	 * @param sep the field separator character
	 * @param bCouldHaveQuotes true if fields may contain quoted values
	 * @return array of parsed fields
	 */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes) {
		return OATextTokenizer.parseLine(line, sep, bCouldHaveQuotes);
	}

	/**
	 * Parses a delimited line into fields with an estimated result size.
	 *
	 * @param line the source line
	 * @param sep the field separator character
	 * @param bCouldHaveQuotes true if fields may contain quoted values
	 * @param sizeEstimate estimated number of fields
	 * @return array of parsed fields
	 */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes, int sizeEstimate) {
		return OATextTokenizer.parseLine(line, sep, bCouldHaveQuotes, sizeEstimate);
	}

	/**
	 * Trims leading and trailing whitespace characters from the supplied text.
	 *
	 * @param text the source text
	 * @return the trimmed text
	 */
	public static String trimWhitespace(String text) {
		return OATextFormat.trimWhitespace(text);
	}


	/**
	 * Converts text to ASCII characters, removing or replacing non-ASCII characters.
	 *
	 * @param text the source text
	 * @return the ASCII-converted string
	 */
	public static String convertToAscii(String text) {
		return OATextFilter.convertToAscii(text);
	}


	/**
	 * Parses a CSS style string into a map of property names to values.
	 *
	 * @param style the CSS style string
	 * @return a map of CSS property names to values
	 */
	public static Map<String, String> getCssMap(String style) {
		return OATextTokenizer.getCssMap(style);
	}

	@Deprecated  // use getCssMap
	public static Map<String, String> getCSSMap(String style) {
		return OATextTokenizer.getCssMap(style);
	}
	
	/**
	 * Parses an integer value from a string.
	 *
	 * @param val the string to parse
	 * @return the parsed integer value
	 */
	public static int parseInt(String val) {
		return OATextUtil.parseInt(val);
	}


	/**
	 * Returns a non-null string, defaulting to an empty string if the value is null.
	 *
	 * @param str the source string
	 * @return a non-null string
	 */
	public static String toNonNull(String str) {
		return OATextSanitize.toNonNull(str, "");
	}

	/**
	 * Returns a non-null string, using a default value if the source is null.
	 *
	 * @param str the source string
	 * @param defaultValue the value to return if the source is null
	 * @return a non-null string
	 */
	public static String toNonNull(String str, String defaultValue) {
		return OATextSanitize.toNonNull(str, defaultValue);
	}

	/**
	 * Returns a non-null string, defaulting to an empty string if the value is null.
	 *
	 * @param str the source string
	 * @return a non-null string
	 */
	public static String getNonNull(String str) {
		return OATextSanitize.getNonNull(str);
	}

	public static String nonNull(String str) {
		return OATextSanitize.getNonNull(str);
	}
	
	/**
	 * Returns a non-null string, using a default value if the source is null.
	 *
	 * @param str the source string
	 * @param defaultValue the value to return if the source is null
	 * @return a non-null string
	 */
	public static String getNonNull(String str, String defaultValue) {
		return OATextSanitize.getNonNull(str, defaultValue);
	}

	/**
	 * Converts a string to a non-null value, defaulting to an empty string.
	 *
	 * @param str the source string
	 * @return a non-null string
	 */
	public static String convertToNonNull(String str) {
		return OATextSanitize.convertToNonNull(str);
	}

	/**
	 * Converts a string to a non-null value using a default if needed.
	 *
	 * @param str the source string
	 * @param defaultValue the value to return if the source is null
	 * @return a non-null string
	 */
	public static String convertToNonNull(String str, String defaultValue) {
		return OATextSanitize.convertToNonNull(str, defaultValue);
	}

	/**
	 * Returns a default non-null string representation.
	 *
	 * @param str the source string
	 * @return a non-null string
	 */
	public static String defaultString(String str) {
		return OATextSanitize.defaultString(str);
	}

	/**
	 * Returns a default non-null string using the specified fallback.
	 *
	 * @param str the source string
	 * @param strIfNull the value to return if the source is null
	 * @return a non-null string
	 */
	public static String defaultString(String str, String strIfNull) {
		return OATextSanitize.defaultString(str, strIfNull);
	}

	/**
	 * Returns a non-null string, defaulting to an empty string.
	 *
	 * @param s the source string
	 * @return a non-null string
	 */
	public static String notNull(String s) {
		return OATextSanitize.notNull(s);
	}

	/**
	 * Returns a non-null string using a specified fallback.
	 *
	 * @param str the source string
	 * @param strIfNull the value to return if the source is null
	 * @return a non-null string
	 */
	public static String notNull(String str, String strIfNull) {
		return OATextSanitize.notNull(str, strIfNull);
	}

	/**
	 * Returns a fallback string if the supplied string is empty.
	 *
	 * @param str the source string
	 * @param strIfEmpty the value to return if the source is empty
	 * @return the resulting string
	 */
	public static String notEmpty(String str, String strIfEmpty) {
		if (isEmpty(str)) {
			return strIfEmpty;
		}
		return str;
	}

	/**
	 * Delegates to {@link #substring(String,int)}.
	 *
	 * @param s the source string
	 * @param pos the starting position
	 * @return the substring
	 * @deprecated use {@link #substring(String,int)}
	 */
	public static String subString(String s, int pos) {
		return substring(s, pos);
	}

	/**
	 * Returns a substring starting at the specified position.
	 *
	 * @param s the source string
	 * @param pos the starting index
	 * @return the substring
	 */
	public static String substring(String s, int pos) {
		return OATextFilter.substring(s, pos);
	}

	/**
	 * Returns a substring between two positions.
	 *
	 * @param s the source string
	 * @param pos1 the starting index
	 * @param pos2 the ending index
	 * @return the substring
	 */
	public static String substring(String s, int pos1, int pos2) {
		return OATextFilter.substring(s, pos1, pos2);
	}

	/**
	 * Converts text into a valid Java identifier.
	 *
	 * @param txt the source text
	 * @return a valid Java identifier string
	 */
	public static String makeJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	@Deprecated // use makeJavaIdentifier (spelling fix)
	public static String makeJavaIndentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}
	
	
	/**
	 * Delegates to {@link #makeJavaIdentifier(String)}.
	 *
	 * @param txt the source text
	 * @return a valid Java identifier string
	 */
	public static String convertToJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	/**
	 * Delegates to {@link #makeJavaIdentifier(String)}.
	 *
	 * @param txt the source text
	 * @return a valid Java identifier string
	 */
	public static String getJavaIdentifier(String txt) {
		return OATextUtil.makeJavaIdentifier(txt);
	}

	/**
	 * Removes a specified number of characters from the end of a string.
	 *
	 * @param s the source string
	 * @param amt number of characters to remove
	 * @return the modified string
	 */
	public static String removeEndingChars(String s, int amt) {
		return OATextFilter.removeEndingChars(s, amt);
	}

	/**
	 * Appends text to an existing string using a default separator.
	 *
	 * @param orig the original string
	 * @param append the text to append
	 * @return the concatenated string
	 */
	public static String append(String orig, String append) {
		return OATextUtil.append(orig, append);
	}

	/**
	 * Appends text to an existing string using a specified separator.
	 *
	 * @param orig the original string
	 * @param append the text to append
	 * @param sep the separator string
	 * @return the concatenated string
	 */
	public static String append(String orig, String append, String sep) {
		return OATextUtil.append(orig, append, sep);
	}

	/**
	 * Prepends text to an existing string using a specified separator.
	 *
	 * @param orig the original string
	 * @param prepend the text to prepend
	 * @param sep the separator string
	 * @return the concatenated string
	 */
	public static String prepend(String orig, String prepend, String sep) {
		return OATextUtil.prepend(orig, prepend, sep);
	}

	/**
	 * Appends a value to a CSV-formatted string.
	 *
	 * @param toText the existing CSV string
	 * @param value the value to append
	 * @return the updated CSV string
	 */
	public static String csv(String toText, Object value) {
		return OATextTokenizer.csv(toText, value);
	}

	/**
	 * Concatenates a value to existing text using default options.
	 *
	 * @param toText the existing text
	 * @param value the value to append
	 * @return the concatenated string
	 */
	public static String concat(String toText, String value) {
		return concat(toText, value, " ", true);
	}

	/**
	 * Concatenates a value to existing text using a specified separator.
	 *
	 * @param toText the existing text
	 * @param value the value to append
	 * @param sepChar the separator string
	 * @return the concatenated string
	 */
	public static String concat(String toText, Object value, String sepChar) {
		return OATextUtil.concat(toText, value, sepChar);
	}

	/**
	 * Concatenates a value to existing text using a specified separator.
	 *
	 * @param toText the existing text
	 * @param value the value to append
	 * @param sepChar the separator string
	 * @return the concatenated string
	 */
	public static String concat(String toText, String value, String sepChar) {
		return OATextUtil.concat(toText, value, sepChar);
	}

	/**
	 * Concatenates a value to existing text with optional forced concatenation.
	 *
	 * @param toText the existing text
	 * @param value the value to append
	 * @param sepChar the separator string
	 * @param bForce true to force concatenation even if value is empty
	 * @return the concatenated string
	 */
	public static String concat(String toText, String value, String sepChar, boolean bForce) {
		return OATextUtil.concat(toText, value, sepChar, bForce);
	}

	/**
	 * Masks password values found in text.
	 *
	 * @param name the password field name
	 * @param val the source text
	 * @return the masked text
	 */
	public static String maskPassword(String name, String val) {
		return OATextTokenizer.maskPassword(name, val);
	}

	/**
	 * Masks password values using custom replacement and match words.
	 *
	 * @param name the password field name
	 * @param val the source text
	 * @param passwordReturn the replacement value
	 * @param words additional words to mask
	 * @return the masked text
	 */
	public static String maskPassword(String name, String val, String passwordReturn, String... words) {
		return OATextTokenizer.maskPassword(name, val, passwordReturn, words);
	}

	/**
	 * Masks password values using specified match words.
	 *
	 * @param name the password field name
	 * @param val the source text
	 * @param words words that identify password values
	 * @return the masked text
	 */
	public static String maskPassword(String name, String val, String... words) {
		return OATextTokenizer.maskPassword(name, val, words);
	}

	/**
	 * Masks password values with configurable case sensitivity and exact-match control.
	 *
	 * @param name the password field name
	 * @param value the source text
	 * @param maskValue the replacement value
	 * @param bCaseSensitive true to match case sensitively
	 * @param bMatchExact true to require exact word matches
	 * @param words words that identify password values
	 * @return the masked text
	 */
	public static String maskPassword(String name, String value, String maskValue, boolean bCaseSensitive, String... words) {
		return OATextTokenizer.maskPassword(name, value, maskValue, bCaseSensitive, words);
	}

	/**
	 * Highlights occurrences of a search string within the supplied text.
	 *
	 * @param line the source text
	 * @param search the text to highlight
	 * @return the resulting string with highlighted search text
	 */
	public static String hilite(String line, String search) {
		return hilite(line, search, "<b style='background:yellow'>", "</b>", true);
	}

	/**
	 * Highlights occurrences of a search string within the supplied text, ignoring case.
	 *
	 * @param line the source text
	 * @param search the text to highlight
	 * @return the resulting string with highlighted search text
	 */
	public static String hiliteIgnoreCase(String line, String search, String beginTag, String endTag) {
		return hilite(line, search, beginTag, endTag, true);
	}

	/**
	 * Highlights occurrences of a search string within the supplied text by surrounding
	 * each match with the specified begin and end tags.
	 *
	 * @param line the source text
	 * @param search the text to highlight
	 * @param beginTag the string inserted before each match
	 * @param endTag the string inserted after each match
	 * @return the resulting string with highlighted search text
	 */
	public static String hilite(String line, String search, String beginTag, String endTag) {
		return hilite(line, search, beginTag, endTag, false);
	}

	/**
	 * Highlights occurrences of a search string within the supplied text by surrounding
	 * each match with the specified begin and end tags.
	 *
	 * <p>The comparison can be performed either case-sensitively or case-insensitively
	 * based on the {@code bIgnoreCase} flag.
	 *
	 * @param line the source text
	 * @param search the text to highlight
	 * @param beginTag the string inserted before each matched occurrence
	 * @param endTag the string inserted after each matched occurrence
	 * @param bIgnoreCase true to ignore case when matching the search text
	 * @return the resulting string with highlighted search text
	 */
	public static String hilite(String line, String search, String beginTag, String endTag, boolean bIgnoreCase) {
		return OATextEscape.hilite(line, search, beginTag, endTag, bIgnoreCase);
	}

	/**
	 * Returns the result of {@link OATextEscape#escape(String)} for the supplied raw text.
	 *
	 * @param raw the input text to escape
	 * @return the escaped text
	 */
	public static String escape(String raw) {
		return OATextEscape.escape(raw);
	}

	/**
	 * Returns the result of {@link OATextEscape#unescapeJson(String)} for the supplied text.
	 *
	 * @param s the input text to unescape
	 * @return the unescaped text
	 */
	public static String unescapeJson(String s) {
		return OATextEscape.unescapeJson(s);
	}

	/**
	 * Returns the result of {@link OATextEscape#escapeJs(String, char)} for the supplied text.
	 *
	 * @param text the input text to escape
	 * @param jsQuoteChar the JavaScript quote character to use for escaping
	 * @return the escaped text
	 */
	public static String escapeJs(final String text, final char jsQuoteChar) {
		return OATextEscape.escapeJs(text, jsQuoteChar);
	}

	/**
	 * Returns the result of {@link OATextEscape#escapeJs(String, char, boolean)} using the supplied options.
	 *
	 * @param text the input text to escape
	 * @param jsQuoteChar the JavaScript quote character to use for escaping
	 * @param bIsJsCodeEmbeddedInHtml true if the JavaScript code is embedded in HTML
	 * @return the escaped text
	 */
	protected static String escapeJs(final String text, final char jsQuoteChar, final boolean bIsJsCodeEmbeddedInHtml) {
		return OATextEscape.escapeJs(text, jsQuoteChar, bIsJsCodeEmbeddedInHtml);
	}

	/**
	 * Returns the result of {@link OATextEscape#escapeJson(String)} for the supplied text.
	 *
	 * @param s the input text to escape as JSON
	 * @return the escaped JSON text
	 */
	public static String escapeJson(String s) {
		return OATextEscape.escapeJson(s);
	}

	public static String escapeJSON(String s) {
		return OATextEscape.escapeJson(s);
	}
	
	/**
	 * Appends the JSON-escaped form of {@code s} into the supplied {@link StringBuffer}.
	 *
	 * @param s the input text to escape as JSON
	 * @param sb the buffer to append the escaped output to
	 */
	static void escapeJson(String s, StringBuffer sb) {
		OATextEscape.escapeJson(s, sb);
	}

	/**
	 * Returns the result of {@link OATextUtil#convertToLikeSearch(String)} for the supplied text.
	 *
	 * @param s the input text to convert
	 * @return the converted text
	 */
	public static String convertToLikeSearch(String s) {
		return OATextUtil.convertToLikeSearch(s);
	}

	/**
	 * Returns the result of {@link OATextUtil#getVerticalNumberLines(int, int)} for the supplied range.
	 *
	 * @param startPos the starting position value
	 * @param endPos the ending position value
	 * @return the generated vertical number lines text
	 */
	public static String getVerticalNumberLines(int startPos, int endPos) {
		return OATextUtil.getVerticalNumberLines(startPos, endPos);
	}

	/**
	 * Returns the result of {@link OATextUtil#getVerticalHex(byte[])} for the supplied bytes.
	 *
	 * @param bs the byte array to convert
	 * @return the generated vertical hex text
	 */
	public static String getVerticalHex(byte[] bs) {
		return OATextUtil.getVerticalHex(bs);
	}

	/**
	 * Delegates to {@link #createString(char, int)}.
	 *
	 * @param repeatChar the character to repeat
	 * @param length the number of characters to generate
	 * @return the generated string
	 */
	public static String repeat(char repeatChar, int length) {
		return createString(repeatChar, length);
	}

	/**
	 * Returns the result of {@link OATextUtil#createString(char, int)} for the supplied character and length.
	 *
	 * @param repeatChar the character to repeat
	 * @param length the number of characters to generate
	 * @return the generated string
	 */
	public static String createString(char repeatChar, int length) {
		return OATextUtil.createString(repeatChar, length);
	}

	/**
	 * Returns the result of {@link OATextUtil#bytesToHex(byte[])} for the supplied bytes.
	 *
	 * @param bytes the byte array to convert
	 * @return the hex representation
	 */
	public static String bytesToHex(byte[] bytes) {
		return OATextUtil.bytesToHex(bytes);
	}

	/**
	 * Returns the result of {@link OATextUtil#hexToBytes(String)} for the supplied hex string.
	 *
	 * @param hex the hex text to convert
	 * @return the decoded bytes
	 */
	public static byte[] hexToBytes(String hex) {
		return OATextUtil.hexToBytes(hex);
	}

	/**
	 * Returns the index computed by {@link OATextCompare#indexOf(String, String, int, boolean)} starting at position {@code 0}
	 * with case-insensitive matching disabled.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @return the index value returned by the compare operation
	 */
	public static int indexOf(String value, String searchValue) {
		return OATextCompare.indexOf(value, searchValue, 0, false);
	}

	/**
	 * Returns the result of {@link OATextCompare#indexOf(String, String, int)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @param startPos the starting position for the search
	 * @return the index value returned by the compare operation
	 */
	public static int indexOf(String value, String searchValue, int startPos) {
		return OATextCompare.indexOf(value, searchValue, startPos);
	}

	/**
	 * Returns the result of {@link OATextCompare#indexOf(String, String, boolean)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @param bIgnoreCase true to ignore case during the search
	 * @return the index value returned by the compare operation
	 */
	public static int indexOf(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.indexOf(value, searchValue, bIgnoreCase);
	}

	/**
	 * Returns the result of {@link OATextCompare#indexOf(String, String, int, boolean)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @param startPos the starting position for the search
	 * @param bIgnoreCase true to ignore case during the search
	 * @return the index value returned by the compare operation
	 */
	public static int indexOf(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return OATextCompare.indexOf(value, searchValue, startPos, bIgnoreCase);
	}

	/**
	 * Returns the result of {@link OATextCompare#lastIndexOf(String, String)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @return the last index value returned by the compare operation
	 */
	public static int lastIndexOf(String value, String searchValue) {
		return OATextCompare.lastIndexOf(value, searchValue);
	}

	/**
	 * Returns the result of {@link OATextCompare#lastIndexOf(String, String, boolean)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @param bIgnoreCase true to ignore case during the search
	 * @return the last index value returned by the compare operation
	 */
	public static int lastIndexOf(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.lastIndexOf(value, searchValue, bIgnoreCase);
	}

	/**
	 * Returns the result of {@link OATextCompare#contains(String, String)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @return true if the compare operation indicates a match, false otherwise
	 */
	public static boolean contains(String value, String searchValue) {
		return OATextCompare.contains(value, searchValue);
	}
	
	/**
	 * Returns the result of {@link OATextCompare#contains(String, String, boolean)} for the supplied inputs.
	 *
	 * @param value the text to search within
	 * @param searchValue the text to search for
	 * @param bIgnoreCase true to ignore case during the comparison
	 * @return true if the compare operation indicates a match, false otherwise
	 */
	public static boolean contains(String value, String searchValue, int startPos) {
		return OATextCompare.contains(value, searchValue, startPos);
	}
	
	/**
	 * Determines whether the supplied text contains a specified search value.
	 * <p>
	 * This method checks for the presence of {@code searchValue} within {@code value},
	 * starting at the specified character position. Comparison behavior can be
	 * configured to be case-sensitive or case-insensitive.
	 * </p>
	 * <p>
	 * If {@code startPos} is less than zero, searching begins at the start of the
	 * string. If either {@code value} or {@code searchValue} is {@code null}, this
	 * method returns {@code false}.
	 * </p>
	 *
	 * @param value the source string to search within
	 * @param searchValue the string to search for
	 * @param startPos the starting character position for the search
	 * @param bIgnoreCase {@code true} to ignore case during comparison,
	 *                    {@code false} for case-sensitive matching
	 * @return {@code true} if {@code searchValue} is found within {@code value}
	 *         at or after {@code startPos}, otherwise {@code false}
	 */
	public static boolean contains(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return OATextCompare.contains(value, searchValue, startPos, bIgnoreCase);
	}

	/**
	 * Returns the leftmost portion of the supplied string.
	 * <p>
	 * If the string is longer than {@code amount}, it is truncated from the right.
	 * If shorter, it is returned unchanged.
	 * </p>
	 *
	 * @param value the source string
	 * @param amount the number of characters to return from the left
	 * @return the leftmost substring
	 */
	public static String getLeft(String value, int amount) {
		return OATextAlign.left(value, amount);
	}

	/**
	 * Alias for {@link #getLeft(String, int)}.
	 *
	 * @param value the source string
	 * @param amount the number of characters to return from the left
	 * @return the leftmost substring
	 */
	public static String left(String value, int amount) {
		return OATextAlign.left(value, amount);
	}

	/**
	 * Returns the rightmost portion of the supplied string.
	 * <p>
	 * If the string is longer than {@code amount}, it is truncated from the left.
	 * If shorter, it is returned unchanged.
	 * </p>
	 *
	 * @param value the source string
	 * @param amount the number of characters to return from the right
	 * @return the rightmost substring
	 */
	public static String getRight(String value, int amount) {
		return OATextAlign.right(value, amount);
	}

	/**
	 * Alias for {@link #getRight(String, int)}.
	 *
	 * @param value the source string
	 * @param amount the number of characters to return from the right
	 * @return the rightmost substring
	 */
	public static String right(String value, int amount) {
		return OATextAlign.right(value, amount);
	}

	/**
	 * Returns a centered portion of the supplied string.
	 * <p>
	 * If the string is longer than {@code amount}, characters are removed evenly
	 * from both ends where possible.
	 * </p>
	 *
	 * @param value the source string
	 * @param amount the desired length
	 * @return the centered substring
	 */
	public static String getCenter(String value, int amount) {
		return OATextAlign.center(value, amount);
	}
	
	/**
	 * Alias for {@link #getCenter(String, int)}.
	 *
	 * @param value the source string
	 * @param amount the desired length
	 * @return the centered substring
	 */
	public static String center(String value, int amount) {
		return OATextAlign.center(value, amount);
	}

	/**
	 * Converts all characters in the supplied string to upper case.
	 *
	 * @param value the source string
	 * @return the upper-case string
	 */
	public static String upper(String value) {
		return OATextChars.upper(value);
	}

	/**
	 * Alias for {@link #upper(String)}.
	 *
	 * @param value the source string
	 * @return the upper-case string
	 */
	public static String toUpperCase(String value) {
		return upper(value);
	}

	/**
	 * Alias for {@link #upper(String)}.
	 *
	 * @param value the source string
	 * @return the upper-case string
	 */
	public static String getUpperCase(String value) {
		return upper(value);
	}

	/**
	 * Converts all characters in the supplied string to lower case.
	 *
	 * @param value the source string
	 * @return the lower-case string
	 */
	public static String lower(String value) {
		return OATextChars.lower(value);
	}

	/**
	 * Alias for {@link #lower(String)}.
	 *
	 * @param value the source string
	 * @return the lower-case string
	 */
	public static String toLowerCase(String value) {
		return lower(value);
	}

	/**
	 * Alias for {@link #lower(String)}.
	 *
	 * @param value the source string
	 * @return the lower-case string
	 */
	public static String getLowerCase(String value) {
		return lower(value);
	}

	/**
	 * Determines whether the supplied string starts with the given search value.
	 *
	 * @param value the source string
	 * @param searchValue the prefix to test for
	 * @return {@code true} if {@code value} starts with {@code searchValue}
	 */
	public static boolean startsWith(String value, String searchValue) {
		return OATextCompare.startsWith(value, searchValue);
	}

	/**
	 * Determines whether the supplied string starts with the given search value,
	 * with optional case-insensitive comparison.
	 *
	 * @param value the source string
	 * @param searchValue the prefix to test for
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 * @return {@code true} if {@code value} starts with {@code searchValue}
	 */
	public static boolean startsWith(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.startsWith(value, searchValue, bIgnoreCase);
	}

	/**
	 * Determines whether the supplied string ends with the given search value.
	 *
	 * @param value the source string
	 * @param searchValue the suffix to test for
	 * @return {@code true} if {@code value} ends with {@code searchValue}
	 */
	public static boolean endsWith(String value, String searchValue) {
		return OATextCompare.endsWith(value, searchValue);
	}

	/**
	 * Determines whether the supplied string ends with the given search value,
	 * with optional case-insensitive comparison.
	 *
	 * @param value the source string
	 * @param searchValue the suffix to test for
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 * @return {@code true} if {@code value} ends with {@code searchValue}
	 */
	public static boolean endsWith(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.endsWith(value, searchValue, bIgnoreCase);
	}

	/**
	 * Prepends the search value to the string if it is not already present.
	 *
	 * @param value the source string
	 * @param searchValue the prefix to ensure
	 * @return the updated string
	 */
	public static String prefixIfMissing(String value, String searchValue) {
		return OATextCompare.prefixIfMissing(value, searchValue);
	}

	/**
	 * Prepends the search value to the string if it is not already present,
	 * with optional case-insensitive comparison.
	 *
	 * @param value the source string
	 * @param searchValue the prefix to ensure
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 * @return the updated string
	 */
	public static String prefixIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.prefixIfMissing(value, searchValue, bIgnoreCase);
	}
	
	/**
	 * Appends the search value to the string if it is not already present.
	 *
	 * @param value the source string
	 * @param searchValue the suffix to ensure
	 * @return the updated string
	 */
	public static String appendIfMissing(String value, String searchValue) {
		return OATextCompare.appendIfMissing(value, searchValue);
	}

	/**
	 * Appends the search value to the string if it is not already present,
	 * with optional case-insensitive comparison.
	 *
	 * @param value the source string
	 * @param searchValue the suffix to ensure
	 * @param bIgnoreCase {@code true} to ignore case during comparison
	 * @return the updated string
	 */
	public static String appendIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		return OATextCompare.appendIfMissing(value, searchValue, bIgnoreCase);
	}

	/**
	 * Returns the number of decimal places in a numeric string.
	 *
	 * @param num the numeric string to analyze
	 * @param bIgnoreTrailingZeros {@code true} to ignore trailing zeros
	 * @return the number of decimal places
	 */
	public static int getNumberOfDecimalPlaces(String num, boolean bIgnoreTrailingZeros) {
		return OATextFormat.getNumberOfDecimalPlaces(num, bIgnoreTrailingZeros);
	}

	/**
	 * Removes leading occurrences of a character from a string.
	 *
	 * @param s the source string
	 * @param ch the character to remove
	 * @return the modified string
	 */
	public static String removeLeading(String s, char ch) {
		return OATextFilter.removeLeading(s, ch);
	}

	/**
	 * Removes leading occurrences of a character from a string,
	 * up to a maximum number of removals.
	 *
	 * @param s the source string
	 * @param ch the character to remove
	 * @param maxAmount maximum number of characters to remove
	 * @return the modified string
	 */
	public static String removeLeading(String s, char ch, int maxAmount) {
		return OATextFilter.removeLeading(s, ch, maxAmount);
	}

	/**
	 * Returns the simple name of a class without package information.
	 *
	 * @param c the class to inspect
	 * @return the simple class name
	 */
	public static String getClassName(Class c) {
		if (c == null) return null;
		return c.getSimpleName();
	}
	
}

