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
package com.viaoa.text;

import java.util.HashMap;
import java.util.Map;

import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqq
CODEX

 - file/class/method: OATextEscape.escapeJson
  - concrete failure scenario: JSON generated for UI/typeahead becomes invalid when value contains an apostrophe.
  - example input: O'Brien
  - expected result: O'Brien or O\u0027Brien inside JSON string
  - actual or likely result: O\'Brien, which is not a valid JSON escape
  - why it matters to OA: OAUITypeAheadController builds JSON using OAString.escapeJson; user data with apostrophes
    can break JSON parsing.
  - fix direction: Do not escape ' for JSON, or encode it as \u0027.


 - file/class/method: OATextEscape.convertToXml(..., bCData=true)
  - concrete failure scenario: CDATA output is broken when text contains the CDATA terminator.
  - example input: abc]]>def
  - expected result: CDATA-safe output, e.g. split ]]]]><![CDATA[>
  - actual or likely result: raw abc]]>def inside <![CDATA[...]]>, prematurely closing CDATA
  - why it matters to OA: OAXMLWriter.printCDATA uses this path; serialized XML can become malformed or corrupt
    following content.
  - fix direction: In CDATA mode, detect ]]> and split or encode it safely.


  - file/class/method: OATextEscape.convertFromHtml
  - concrete failure scenario: Round-trip HTML decoding double-decodes escaped entities.
  - example input: &amp;lt;
  - expected result: &lt;
  - actual or likely result: <
  - why it matters to OA: Literal text that looks like an entity can become markup after decode, affecting UI display
    and serialized text.
  - fix direction: Decode &amp; last, or use a single-pass entity decoder.

  - file/class/method: OATextEscape.unescapeJson
  - concrete failure scenario: Literal escaped backslash sequences are converted into control characters.
  - example input: escaped JSON text representing literal \n: \\n
  - expected result: \n as two characters
  - actual or likely result: newline character
  - why it matters to OA: JSON/string round-trips can corrupt stored display text, paths, or serialized values
    containing backslash escapes.
  - fix direction: Use a single-pass JSON unescaper that consumes escape sequences in order.

 15. Medium - OATextEscape.escapeJs
     Concrete failure: embedded-HTML mode leaves backslashes as single backslashes, so "C:\\temp" can become a JS
     string containing \t.
     Expected: literal backslashes survive JS parsing.
     Actual: JS escape sequences can be created accidentally.
     Fix direction: escape backslash as \\, \x5C, or \u005C for JS string context.

 1. Medium - OATextEscape.convertTextToHtml
     Scenario: plain text containing both < and > is treated as already-HTML and returned unescaped.
     Example: OAString.convertTextToHtml("1 < 2 > 0", false) returns 1 < 2 > 0.
     Impact: UI display can emit raw markup-like text instead of safe HTML text.

 2. Medium - OATextEscape.getHtmlAttributeMap
     Scenario: quoted attribute values are returned with quote characters included.
     Example: <input type="text" data-x='a b'> returns {type="text", data-x='a b'}.
     Impact: callers expecting parsed HTML attribute values get corrupted values with delimiters included

 6. Medium - OATextEscape.escape
     Scenario: public escaping helper throws on null input.
     Example: OAString.escape(null) throws NullPointerException.
     Impact: inconsistent with surrounding OA text escaping helpers that return null or empty safely; nullable
     serialized/UI text paths can fail unexpectedly.

  12. Medium - OATextEscape.decodeIllegalXml
     Scenario: literal marker-looking text is decoded as an illegal XML character marker.
     Example: OAString.decodeIllegalXml("<OAXML#65/>") returns "A".
     Impact: XML round-trips can corrupt legitimate text containing OA marker syntax.

 20. Medium - OATextEscape.isLegalXml / convertToXml
     Scenario: unpaired surrogate characters are treated as legal XML and emitted unchanged.
     Example: OAString.isLegalXml("\uD800") returns true; convertToXml returns the invalid surrogate.
     Impact: XML serialization can produce malformed XML for invalid Unicode input.


*/

/**
 * Encoding and escaping utilities for HTML, XML, JavaScript, and JSON text.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Escaping markup characters for HTML and XML</li>
 *   <li>Encoding illegal XML control characters</li>
 *   <li>Basic unescape and reverse-transform operations</li>
 *   <li>JavaScript and JSON-escape helpers for safe inline usage</li>
 *   <li>Highlighting search matches within text using tag wrappers</li>
 * </ul>
 *
 * <p>All methods in this class are null-safe and operate only on text content.
 * Higher-level layout and formatting features should be delegated to
 * {@link OATextFormat}, and structural parsing to
 * {@link OATextTokenizer}.</p>
 *
 * <p>This class ensures that OA applications safely encode text for browser,
 * markup, and serialization environments where characters such as '&', '&lt;',
 * '&gt;', and quotes require escape sequences.</p>
 *
 */
public class OATextEscape {

	
	/**
	 * Converts HTML entity codes (e.g., &amp;amp;, &amp;quot;, &amp;lt;) into their
	 * corresponding characters. If the input is {@code null}, {@code null} is
	 * returned. Only known entities are replaced; all others remain unchanged.
	 *
	 * @param html the HTML-encoded string
	 * @return a decoded string or {@code null} if the input is null
	 */
	public static String convertFromHtml(String html) {
		if (html == null) {
			return null;
		}

		if (html.indexOf('&') >= 0) {
			html = OATextFilter.convert(html, "&amp;", "&");
			html = OATextFilter.convert(html, "&quot;", "\"");
			html = OATextFilter.convert(html, "&apos;", "'");
			html = OATextFilter.convert(html, "&lt;", "<");
			html = OATextFilter.convert(html, "&gt;", ">");
		}
		return html;
	}

	/**
	 * Converts raw text into HTML-safe text unless the value already appears to
	 * contain HTML markup. Uses {@link #convertToXml(String, boolean, boolean, boolean)}
	 * to escape characters and optionally wraps the result in &lt;html&gt; tags.
	 *
	 * @param value       the text to convert; {@code null} yields an empty string
	 * @param bAddHTMLTag whether to wrap the result in &lt;html&gt; tags
	 * @return HTML-safe text, possibly wrapped in an HTML element
	 */
	public static String convertTextToHtml(String value, boolean bAddHTMLTag) {
		if (value == null) {
			return "";
		}
		String s2 = value.toLowerCase();
		if (s2.indexOf("<html") >= 0) {
			return value;
		}
		if (s2.indexOf("<") >= 0 && s2.indexOf(">") >= 0) {
			return value;
		}
		// if (s2.indexOf("<br>") >= 0) return value;
		if (s2.indexOf("&amp;") >= 0) {
			return value;
		}

		value = convertToXml(value, false, true, true);

		if (bAddHTMLTag) {
			value = "<html>" + value + "</html>";
		}
		return value;
	}
	
	/**
	 * Delegates to {@link #convertToXml(String, boolean, boolean)} with settings
	 * appropriate for HTML escaping.
	 *
	 * @param value the text to convert
	 * @return an HTML-safe version of the text
	 */
	public static String convertToHtml(String value) {
		return convertToXml(value, false, true);
	}
	

	/**
	 * Converts text into XML-safe form, escaping markup characters and encoding
	 * illegal XML characters using internal encoding rules. Delegates to the
	 * more detailed overload.
	 *
	 * @param value  the text to convert
	 * @param bCData whether the value will be placed inside a CDATA block
	 * @return XML-safe text, or an empty string if value is null
	 */
	public static String convertToXml(String value, boolean bCData) {
		return convertToXml(value, bCData, false);
	}

	/**
	 * Delegates to {@link #convertToXml(String, boolean, boolean, boolean)} with
	 * default newline handling rules based on HTML mode.
	 *
	 * @param value   the text to convert
	 * @param bCData  true if used inside CDATA
	 * @param bIsHtml true if converting for HTML output
	 * @return the converted XML text
	 */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml) {
		return convertToXml(value, bCData, bIsHtml, !bIsHtml);
	}

	/**
	 * Core XML escaping routine. Escapes markup characters, optionally replaces
	 * newline sequences with &lt;br&gt; for HTML mode, and encodes characters
	 * illegal in XML using {@link #encodeIllegalXml(char, boolean)}.
	 * <ul>
	 *   <li>If {@code value} is null, returns an empty string.</li>
	 *   <li>Illegal XML characters (ASCII&lt;32 except tab/CR/LF) are encoded.</li>
	 *   <li>Optional CR/LF preservation controlled by {@code bLeaveCRLF}.</li>
	 * </ul>
	 *
	 * @param value      the text to convert
	 * @param bCData     whether the output will be inside a CDATA block
	 * @param bIsHtml    whether HTML-specific newline conversion is enabled
	 * @param bLeaveCRLF whether CR/LF should be preserved
	 * @return the escaped XML string
	 */
	public static String convertToXml(String value, boolean bCData, boolean bIsHtml, boolean bLeaveCRLF) {
		if (value == null) {
			return "";
		}

		int x = value.length();
		StringBuilder sb = new StringBuilder(x);
		for (int i = 0; i < x; i++) {
			char ch = value.charAt(i);
			char chNext = (i + 1 == x) ? 0 : value.charAt(i + 1);
			char chPrev = (i == 0) ? 0 : value.charAt(i - 1);

			if (!bCData) {
				switch (ch) {
				case '&':
					sb.append("&amp;");
					continue;
				case '"':
					sb.append("&quot;");
					continue;
				case '\'':
					sb.append("&apos;");
					continue;
				case '<':
					sb.append("&lt;");
					continue;
				case '>':
					sb.append("&gt;");
					continue;
				case '\n':
					if (bIsHtml) {
						if (chPrev != '\r') {
							sb.append("<br>");
						}
					}
					if (!bLeaveCRLF) {
						continue;
					}
					break;
				case '\r': {
					if (bIsHtml && chNext == '\n') {
						sb.append("<br>");
					}
					if (!bLeaveCRLF) {
						continue;
					}
					break;
				}
				}
			}

			switch (ch) {
			case 9:
			case 10:
			case 13:
				sb.append(ch);
				continue;
			}

			if (ch < 32) { // illegal in XML, create special tag
				sb.append(encodeIllegalXml(ch, !bCData));
			} else {
				sb.append(ch);
			}
		}
		return new String(sb);
	}

	/**
	 * Delegates to {@link #convertToXml(String, boolean, boolean)} using
	 * non-CDATA and non-HTML mode.
	 *
	 * @param value the text to convert
	 * @return the converted XML string
	 */
	public static String convertToXml(String value) {
		return convertToXml(value, false, false);
	}

	
	/**
	 * Encodes illegal XML characters by routing through
	 * {@link #convertToXml(String, boolean, boolean)} in CDATA mode.
	 * Illegal characters are replaced using internal XML-encoding tags.
	 *
	 * @param value the text to encode
	 * @return encoded XML-safe text
	 */
	public static String encodeIllegalXml(String value) {
		return convertToXml(value, true, false);
	}

	/**
	 * Encodes an illegal XML character into a special marker tag of the form
	 * {@code <OAXML#NNN/>}, where {@code NNN} is the integer value of the
	 * character. When {@code bConvertLTGT} is true, &lt; and &gt; are escaped
	 * as HTML entities.
	 *
	 * @param ch           the character to encode
	 * @param bConvertLTGT whether to convert &lt; and &gt; to entities
	 * @return the encoded marker string
	 */
	public static String encodeIllegalXml(char ch, boolean bConvertLTGT) {
		if (bConvertLTGT) {
			return "&lt;OAXML#" + ((int) ch) + "/&gt;";
		}
		return "<OAXML#" + ((int) ch) + "/>"; // used in [CDATA]
	}


	/**
	 * Determines whether the supplied string contains only characters legal
	 * in XML content. Illegal characters include markup symbols, CR/LF, and
	 * any character with value < 32 except tab.
	 *
	 * @param value the text to examine
	 * @return {@code true} if all characters are XML-legal; otherwise {@code false}
	 */
	public static boolean isLegalXml(String value) {
		if (value == null) {
			return false;
		}
		int x = value.length();
		for (int i = 0; i < x; i++) {
			char ch = value.charAt(i);
			switch (ch) {
			case '&':
			case '"':
			case '\'':
			case '<':
			case '>':
			case 10:
			case 13:
				return false;
			case 9:
				break;
			default:
				if (ch < 32) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Reverses encoding performed by {@link #encodeIllegalXml(char, boolean)}
	 * and converts marker tags (e.g., &lt;OAXML#123/&gt;) back to their literal
	 * character values.
	 * <p>
	 * Iteratively scans the string and replaces each tag. If parsing fails for
	 * a particular tag, the character is skipped without throwing an exception.
	 *
	 * @param value the encoded string
	 * @return a decoded string, or {@code null} if the input is null
	 */
	public static String decodeIllegalXml(String value) {
		if (value == null) {
			return null;
		}
		int pos = 0;
		for (;;) {
			int apos = value.indexOf("<OAXML#", pos);
			if (apos < 0) {
				break;
			}
			int bpos = value.indexOf("/>", apos + 7);
			if (bpos > 0 && bpos > apos + 7) {
				int ch = ' ';
				try {
					ch = Integer.parseInt(value.substring(apos + 7, bpos));
				} catch (Exception e) {
				}
				value = value.substring(0, apos) + ((char) ch) + value.substring(bpos + 2);
			}
			pos = apos + 1;
		}
		return value;
	}

	/**
	 * Escapes characters for safe JavaScript/JSON string usage. Replaces
	 * backslashes, quotes, and common control characters with their escape
	 * sequences. Does not escape all non-printing characters.
	 *
	 * @param raw the input text
	 * @return the escaped string
	 */
	public static String escape(String raw) {
		String escaped = raw;
		escaped = escaped.replace("\\", "\\\\");
		escaped = escaped.replace("\"", "\\\"");
		escaped = escaped.replace("\b", "\\b");
		escaped = escaped.replace("\f", "\\f");
		escaped = escaped.replace("\n", "\\n");
		escaped = escaped.replace("\r", "\\r");
		escaped = escaped.replace("\t", "\\t");
		// TODO: escape other non-printing characters using uXXXX notation
		return escaped;
	}


	
	/**
	 * Delegates to {@link #escapeJs(String, char, boolean)} with HTML-embedded
	 * JavaScript disabled.
	 *
	 * @param text         the JavaScript snippet or literal
	 * @param jsQuoteChar  the quote character that must be escaped
	 * @return escaped JavaScript text
	 */
	public static String escapeJs(final String text, final char jsQuoteChar) {
	    return escapeJs(text, jsQuoteChar, false);
	}

	/**
	 * Escapes content for safe embedding in JavaScript, optionally adjusting
	 * escaping rules when the code is embedded inside HTML attributes.
	 * <ul>
	 *   <li>Escapes newline, backslash, and the active quote character.</li>
	 *   <li>HTML-sensitive escaping converts quotes to hex codes.</li>
	 *   <li>Returns an empty string for null input.</li>
	 * </ul>
	 *
	 * @param text                      the text to escape
	 * @param jsQuoteChar               the quote type in use (' or ")
	 * @param bIsJsCodeEmbeddedInHtml   whether HTML-safe escaping is required
	 * @return the escaped JavaScript string
	 */
	public static String escapeJs(final String text, final char jsQuoteChar, final boolean bIsJsCodeEmbeddedInHtml) {
        if (text == null) return "";
        final int x = text.length();
        StringBuilder sb = null;

        for (int i = 0; i < x; i++) {
            char ch = text.charAt(i);

            if (ch == '\r' || ch == '\n' || ch == '\\' || ch == jsQuoteChar || (bIsJsCodeEmbeddedInHtml && (ch == '\'' || ch == '\"'))) {
                if (sb == null) {
                    sb = new StringBuilder(x + 4);
                    if (i > 0) sb.append(text.substring(0, i));
                }

                if (ch == '\'') {
                    if (bIsJsCodeEmbeddedInHtml) sb.append("\\x27"); // x27 = "\'"
                    //was if (bIsJsCodeEmbeddedInHtml) sb.append("\\x5Cx27");  //  x5C = "\"   x27 = "\'"
                    else sb.append("\\" + jsQuoteChar);
                }
                else if (ch == '\"') {
                    if (bIsJsCodeEmbeddedInHtml) sb.append("\\x22"); // x22 = "\""
                    //was if (bIsJsCodeEmbeddedInHtml) sb.append("\\x5Cx22");  //  x5C = "\"   x22 = "\""
                    else sb.append("\\" + jsQuoteChar);
                }
                else if (ch == '<') {
                    sb.append("&lt;");
                }
                else if (ch == '>') {
                    sb.append("&gt;");
                }
                else if (ch == '\n') {
                    if (!bIsJsCodeEmbeddedInHtml) sb.append("\\n");
                    else sb.append("\\n"); //  \n
                    //was else sb.append("\\x5Cn"); //  \n
                }
                else if (ch == '\r') {
                    // no-op
                }
                else if (ch == '\\') {
                    if (!bIsJsCodeEmbeddedInHtml) sb.append("\\\\");
                    else sb.append("\\");
                    //was: else sb.append("\\x5C\\x5C"); // x5C = "\"
                }
            }
            else {
                if (sb != null) sb.append(ch);
            }
        }

        if (sb == null) {
            return text;
        }
        return sb.toString();
    }
	
	
	
	/**
	 * Escapes a string for safe JSON encoding by creating a new buffer and
	 * delegating to {@link #escapeJson(String, StringBuffer)}.
	 *
	 * @param s the string to escape
	 * @return the escaped JSON text, or {@code null} if input is null
	 */
	public static String escapeJson(String s) {
		if (s == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		escapeJson(s, sb);
		return sb.toString();
	}

	/**
	 * Escapes characters per JSON rules, writing output into the supplied
	 * buffer. Handles escape sequences for quotation marks, backslashes, and
	 * control characters, and uses \\uXXXX notation for non-printing and
	 * extended Unicode ranges.
	 *
	 * @param s  the text to escape; ignored if null
	 * @param sb the destination buffer
	 */
	public static void escapeJson(String s, StringBuffer sb) {
	    if (s == null) return;
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
            case '\'':
                sb.append("\\\'");
                break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			/*
			case '/':
				sb.append("\\/");
				break;
			*/
			default:
				// http://www.unicode.org/versions/Unicode5.1.0/
				if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}
	}
	
	/**
	 * Tokenizes an HTML tag into attribute name/value pairs using
	 * {@link OATextTokenizer#tokenize(String, char, boolean, boolean, char, char, char)}.
	 * <p>
	 * The tag name is skipped and all remaining tokens are interpreted as
	 * attribute keys and their corresponding values. Missing values default
	 * to empty strings.
	 *
	 * @param htmlTag the raw HTML tag
	 * @return a map of attribute names to values; never null
	 */
	public static Map<String, String> getHtmlAttributeMap(String htmlTag) {
		Map<String, String> map = new HashMap<String, String>();
		if (htmlTag == null) {
			return map;
		}
		String[] ss = OATextTokenizer.tokenize(htmlTag, '=', true, true, '<', '>', (char) 0);

		for (int i = 1; i < ss.length; i++) { //skip first value, "tag name"
			String s1 = ss[i];

			if (i + 1 == ss.length) {
				map.put(s1, "");
				break;
			}
			String s2 = ss[++i];
			if (s2.equals("=")) {
				if (i + 1 == ss.length) {
					map.put(s1, "");
					break;
				}
				s2 = ss[++i];
			} else {
				map.put(s1, "");
				i--;
			}
			map.put(s1, s2);
		}
		return map;
	}

	/**
	 * Reverses JSON escaping by converting escape sequences (e.g., \\n, \\t,
	 * \\\", \\\\) back to their literal characters. Uses {@link OAString#convert(String, String, String)}
	 * for sequential unescaping.
	 *
	 * @param s the escaped JSON string
	 * @return the unescaped result
	 */
	public static String unescapeJson(String s) {
		s = OAString.convert(s, "\\\"", "\"");
		s = OAString.convert(s, "\\\\", "\\");
		s = OAString.convert(s, "\\b", "\b");
		s = OAString.convert(s, "\\f", "\f");
		s = OAString.convert(s, "\\n", "\n");
		s = OAString.convert(s, "\\r", "\r");
		s = OAString.convert(s, "\\t", "\t");
		s = OAString.convert(s, "\\/", "/");

		return s;
	}

	/**
	 * Highlights all occurrences of {@code search} within {@code line} by wrapping
	 * matches with {@code beginTag} and {@code endTag}. Matching is sequential and
	 * allows case-insensitive scanning.
	 * <ul>
	 *   <li>If either {@code line} or {@code search} is null, returns {@code line}.</li>
	 *   <li>Does not allocate a buffer until the first match is found.</li>
	 *   <li>Handles overlapping and partial matches by backtracking as needed.</li>
	 * </ul>
	 *
	 * @param line        the text to scan
	 * @param search      the substring to highlight
	 * @param beginTag    the prefix inserted before each match
	 * @param endTag      the suffix inserted after each match
	 * @param bIgnoreCase whether matching is case-insensitive
	 * @return a new string with highlighted regions or the original if no matches found
	 */
	public static String hilite(String line, String search, String beginTag, String endTag, boolean bIgnoreCase) {
		if (line == null || search == null) {
			return line;
		}

		final int searchLength = search.length();
		if (searchLength == 0) {
			return line;
		}
		if (bIgnoreCase) {
			search = search.toLowerCase();
		}

		final int lineLength = line.length();
		StringBuilder sb = null; // dont allocate until first match is found
		char c = 0, origChar = 0;

		for (int i = 0, j = 0;; i++) {
			if (i < lineLength) {
				origChar = c = line.charAt(i);
				if (bIgnoreCase) {
					c = Character.toLowerCase(c);
				}
				if (c == search.charAt(j)) {
					j++;
					if (j == searchLength) {
						if (sb == null) {
							sb = new StringBuilder(lineLength + (lineLength / 10));
							int e = (i - j) + 1;
							if (e > 0) {
								sb.append(line.substring(0, e));
							}
						}
						sb.append(beginTag);
						/*
						Search="Vi"
						i=6
						i: 0123456789
						   VinceViNce
						j:      12
						*/
						int b = (i - j) + 1;
						sb.append(line.substring(b, b + j));
						sb.append(endTag);
						j = 0;
					}
					continue;
				}
			}
			if (j > 0) {
				if (sb != null) {
					// go back to previously matched chars
					int b = i - j;
					/*
					Search="Vix"
					i=7
					i: 0123456789
					   VinceViNce
					j:      12
					*/
					sb.append(line.substring(b, b + 1));
				}
				i -= j; // start at last checking point, loop with inc i by +1
				j = 0;
			} else {
				if (i >= lineLength) {
					break;
				}
				if (sb != null) {
					sb.append(origChar);
				}
			}
		}
		if (sb == null) {
			return line;
		}
		return new String(sb);
	}
}
