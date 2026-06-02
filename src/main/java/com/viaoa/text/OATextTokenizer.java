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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*qqqqqqqqqqqqq
CODEX

  - file/class/method: OATextTokenizer.maskPassword
  - concrete failure scenario: Default password masking is case-sensitive despite default helpers intending case-
    insensitive matching.
  - example input: maskPassword("Password", "secret")
  - expected result: *****
  - actual or likely result: secret
  - why it matters to OA: Logs/debug output can leak secrets when field names use common capitalization.
  - fix direction: Lowercase name and word when !bCaseSensitive, not when bCaseSensitive.


 - file/class/method: OATextTokenizer.csv / OATextTokenizer.parseLine
  - concrete failure scenario: CSV values written with doubled quotes do not parse back to the original value.
  - example input: value a"b writes as "a""b"; parsing "a""b"
  - expected result: a"b
  - actual or likely result: a""b
  - why it matters to OA: CSV export/import paths can corrupt quoted text.
  - fix direction: In quoted fields, treat doubled quotes as one literal quote.



 7. Medium - OATextTokenizer.countMatches
     Scenario: returns delimited field count instead of match count.
     Example: OAString.countMatches("a,b,c", ",") returns 3; OAString.count("a,b,c", ",") returns 2.
     Impact: callers using countMatches for separator/match counts get off-by-one results.
  8. Medium - OATextTokenizer.tokenize / OATextEscape.getHtmlAttributeMap
     Scenario: tab/newline whitespace is not treated as an attribute delimiter.
     Example: OAString.getHtmlAttributeMap("<input\ttype=\"text\"\tdisabled>") returns corrupted keys like "text" and
     ="text".
     Impact: HTML/UI attribute parsing breaks for valid whitespace-separated tags.
  9. Medium - OATextTokenizer.getCssMap
     Scenario: CSS declarations with normal spaces after semicolons produce bogus entries and quoted values remain
     quoted.
     Example: OAString.getCssMap("color: red; font-family: 'Times New Roman';") returns {=, color=red, font-
     family='Times New Roman'}.
     Impact: UI style parsing can create empty keys and incorrect CSS values.
  10. Medium - OATextTokenizer.csv
     Scenario: values that begin or end with quotes are not wrapped after internal quotes are doubled.
     Example: OAString.csv(null, "\"abc") returns ""abc, not a valid quoted CSV field.
     Impact: CSV export can emit malformed rows for quoted text.


1. file/class/method: src/main/java/com/viaoa/text/OATextTokenizer.java / OATextTokenizer.parseLine

  exact execution path: parseLine("\"a\" ,b", ',', true) reads the closing quote and sets lastQpos, then sees the
  whitespace before the comma and resets lastQpos to -1. When the comma is reached, the parser thinks the quoted
  value is still open and treats the comma as part of the quoted field. The result is a single corrupted field
  instead of ["a", "b"].

  why it is an obvious concrete bug: the method contract says leading/trailing whitespace for each column is
  trimmed, and quoted values are supported. A normal CSV-style value with whitespace after the closing quote before
  the delimiter is parsed incorrectly.

  minimal fix: once a closing quote is seen, preserve that closed-quote state across trailing whitespace until
  delimiter/EOL. Do not reset lastQpos on whitespace after the closing quote.

  suggested test: assert parseLine("\"a\" ,b", ',', true) returns two fields: "a" and "b"; also test "\"a\" "
  returns "a" without quotes.

*/

/**
 * Tokenization and field-extraction utilities for delimited text.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Counting and extracting values separated by a delimiter</li>
 *   <li>Backward-compatible PICK-style field parsing (1-based)</li>
 *   <li>Modern zero-based field access helpers</li>
 *   <li>CSV-style parsing including quote handling</li>
 *   <li>CSS-style name/value tokenization</li>
 *   <li>Password masking using name heuristics</li>
 * </ul>
 *
 * <p>Methods in this class operate without modifying the input text and
 * are designed to be null-safe. Variants exist for both single-character
 * and multi-character delimiters. Certain {@code field()} overloads are
 * deprecated in OA 4.0 in favor of {@code fieldAt()} which uses
 * zero-based indexing.</p>
 *
 * <p>Higher-level filtering and sanitization rules are not handled here and
 * should be delegated to {@link OATextFilter} or {@link OATextSanitize}.
 * Formatting and display handling should be delegated to
 * {@link OATextFormat}.</p>
 *
 * <p>This class is optimized for common business text parsing scenarios
 * including CSV rows, HTML/CSS attribute value splitting, log processing,
 * and structured protocol or messaging formats.</p>
 *
 * @since OA 4.0
 */

public class OATextTokenizer {
	
	/**
	 * Counts the number of occurrences of {@code sep} within {@code str}.
	 * A null string, empty string, or null separator returns 0.
	 *
	 * @param str the text to search
	 * @param sep the substring to match
	 * @return number of occurrences of {@code sep}
	 */
	public static int count(String str, String sep) {
		if (str == null || str.length() == 0 || sep == null) return 0;

		int x = sep.length();
		if (x == 0) return 0;
		
		int cnt = 0;
		int pos = 0;
		for (;; cnt++) {
			pos = str.indexOf(sep, pos);
			if (pos < 0) {
				break;
			}
			pos += x;
		}
		return cnt;
	}
	
	
	/**
	 * Delegates to {@link #dcount(String, String)} to compute the number of
	 * delimited values implied by {@code sep}.
	 *
	 * @param str input text
	 * @param sep delimiter substring
	 * @return count of matches (value count)
	 */
	public static int countMatches(String str, String sep) {
		return dcount(str, sep);
	}

	/**
	 * Delegates to {@link #dcount(String, char)} to compute the number of
	 * delimited values implied by the separator character.
	 *
	 * @param str input text
	 * @param sep delimiter character
	 * @return count of matches (value count)
	 */
	public static int countMatches(String str, char sep) {
		return dcount(str, sep);
	}
	
	/**
	 * Returns the number of values separated by {@code sep} within {@code str}.
	 * Consecutive separators still count as distinct value positions.
	 * <p>
	 * A null or empty input returns 0.
	 *
	 * @param str the text to parse
	 * @param sep delimiter substring
	 * @return number of delimited values
	 */
	public static int dcount(String str, String sep) {
		if (str == null || str.length() == 0) {
			return 0;
		}
		return count(str, sep) + 1;
	}

	/**
	 * Returns the number of values separated by {@code sep}. Converts the
	 * separator character into a string and delegates to
	 * {@link #dcount(String, String)}.
	 *
	 * @param str the text to parse
	 * @param sep delimiter character
	 * @return number of delimited values
	 */
	public static int dcount(String str, char sep) {
		if (str == null || str.length() == 0 || sep == 0) {
			return 0;
		}
		return count(str, sep + "") + 1;
	}

	
	
	/**
	 * Deprecated. Retrieves a single field from {@code str} using 1-based field
	 * numbering. Delegates to {@link #field(String, String, int, int)} with
	 * {@code amt = 1}.
	 *
	 * @param str the text to parse
	 * @param sep delimiter substring
	 * @param beg 1-based starting field index
	 * @return extracted field or null if not found
	 */
	@Deprecated
	public static String field(String str, String sep, int beg) {
		return field(str, sep, beg, 1);
	}
	
	/**
	 * Used to retrieve a portion of a String based on a separator value.
	 * Note: this is '1' based, not '0' based.
	 * Matches old PICK string method.
	 *
	 * @param str String to parse
	 * @param sep seperator wihin str
	 * @param beg field to find, where first field is <b>1</b>
	 * @param amt number of fields to return, -1 for all after the beg
	 * @return string value of field if begin position exists, else null if not found
	 */
	@Deprecated
	public static String field(final String str, final String sep, final int beg, final int amt) {
		if (str == null) {
			return null;
		}
		
		if (beg < 1 || amt == 0) {
			return null;
		}
		if (sep == null || sep.length() == 0) {
			if (beg == 1) return str;
			return null;
		}

		int pos = 0;
		int beginPos = -1;
		int endPos = str.length();
		if (beg == 1) {
			beginPos = 0;
		}

		for (int i = 2;; i++) {
			pos = str.indexOf(sep, pos);
			if (pos < 0) {
				break;
			}
			if (i == beg) {
				beginPos = pos + sep.length();
				endPos = str.length();
			}
			if (beginPos >= 0) {
				if (amt == -1) {
					break;
				}
				if (i == beg + amt) {
					endPos = pos;
					break;
				}
			}
			pos += sep.length();
		}
		if (beginPos < 0) {
			return null;
		}
		if (beginPos >= endPos) {
			return "";
		}
		return str.substring(beginPos, endPos);
	}
	
	/**
	 * Used to retrieve a portion of a String based on a separator value.
	 *
	 * @see #field(String,String,int,int)
	 */
	@Deprecated
	public static String field(String str, char sep, int beg) {
		return field(str, sep + "", beg, 1);
	}
	
	/**
	 * Used to retrieve a portion of a String based on a separator value.
	 *
	 * @see #field(String,String,int,int)
	 */
	@Deprecated
	public static String field(String str, char sep, int beg, int amt) {
		return field(str, sep + "", beg, amt);
	}
	
	
	/**
	 * Zero-based wrapper for the deprecated 1-based {@code field()} method.
	 * Converts {@code beg} to {@code beg+1} for backward compatibility.
	 *
	 * @param str text to parse
	 * @param sep delimiter substring
	 * @param beg zero-based field index
	 * @return extracted field or null if not found
	 */
	public static String fieldAt(String str, String sep, int beg) {
		return field(str, sep, beg+1);
	}

	/**
	 * Zero-based modernized version of the deprecated 1-based multi-field
	 * extraction. Delegates to {@link #field(String, String, int, int)}
	 * using {@code beg+1}.
	 *
	 * @param str text to parse
	 * @param sep delimiter substring
	 * @param beg zero-based starting field index
	 * @param amt number of fields to return; -1 for remaining
	 * @return extracted substring
	 */
	public static String fieldAt(final String str, final String sep, final int beg, final int amt) {
		return field(str, sep, beg+1, amt);
	}
	
	/**
	 * Zero-based wrapper for character-delimiter field extraction.
	 * Delegates to the string-based method after converting {@code sep}.
	 *
	 * @param str text to parse
	 * @param sep delimiter character
	 * @param beg zero-based field index
	 * @return extracted field or null when not found
	 */
	public static String fieldAt(String str, char sep, int beg) {
		return field(str, sep + "", beg+1, 1);
	}

	/**
	 * Zero-based wrapper for multi-field extraction using a character
	 * delimiter. Delegates to the string-based version after converting
	 * {@code sep}.
	 *
	 * @param str text to parse
	 * @param sep delimiter character
	 * @param beg zero-based starting field index
	 * @param amt number of fields to return
	 * @return substring representing the requested fields
	 */
	public static String fieldAt(String str, char sep, int beg, int amt) {
		return field(str, sep + "", beg+1, amt);
	}


	/**
	 * Convenience wrapper for {@link #maskPassword(String, String, String, boolean, String...)}
	 * using a default mask of {@code "*****"} and common password-related keywords.
	 *
	 * @param name key or attribute name
	 * @param val  value associated with {@code name}
	 * @return masked value if {@code name} indicates a password; otherwise {@code val}
	 */
	public static String maskPassword(String name, String val) {
		String s = maskPassword(name, val, "*****", false, "password", "pw", "pass");
		return s;
	}

	/**
	 * Convenience wrapper that allows specifying the mask string while using
	 * default case-insensitive matching rules.
	 *
	 * @param name          key or attribute name
	 * @param val           associated value
	 * @param passwordReturn mask returned when name appears to denote a password
	 * @param words         list of substrings signaling a password
	 * @return masked or unmodified value
	 */
	public static String maskPassword(String name, String val, String passwordReturn, String... words) {
		String s = maskPassword(name, val, passwordReturn, false, words);
		return s;
	}

	/**
	 * Convenience wrapper that uses {@code "*****"} as the mask value and performs
	 * case-insensitive matching against supplied words.
	 *
	 * @param name  key or attribute name
	 * @param val   associated value
	 * @param words substrings identifying password fields
	 * @return masked or unmodified value
	 */
	public static String maskPassword(String name, String val, String... words) {
		String s = maskPassword(name, val, "*****", false, words);
		return s;
	}

	/**
	 * Evaluates {@code name} against a set of password-indicator words.
	 * If any word is contained within {@code name}, returns {@code maskValue};
	 * otherwise returns {@code value}.
	 *
	 * @param name           field name (null returns {@code value})
	 * @param value          original value
	 * @param maskValue      substitution when name indicates a password
	 * @param bCaseSensitive whether comparison should be case-sensitive
	 * @param words          substrings used to identify password names
	 * @return masked or unmodified value
	 */
	public static String maskPassword(String name, String value, String maskValue, boolean bCaseSensitive, String... words) {
		if (name == null || words == null) {
			return value;
		}
		if (!bCaseSensitive) {
			name = name.toLowerCase();
		}
		for (String word : words) {
			if (word == null) {
				continue;
			}
			if (!bCaseSensitive) {
				word = word.toLowerCase();
			}
			boolean b = name.indexOf(word) >= 0;
			if (b) {
				return maskValue;
			}
		}
		return value;
	}


	/**
	 * Delegates to {@link #parseLine(String, char, boolean, int)} using a default
	 * size estimate of 25.
	 *
	 * @param line             input text
	 * @param sep              delimiter character
	 * @param bCouldHaveQuotes whether quoted values may appear
	 * @return array of parsed column values, or null if invalid input
	 */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes) {
		return parseLine(line, sep, bCouldHaveQuotes, 25);
	}

	/**
	 * Parses a delimited line into column values, optionally supporting quoted
	 * sections using single or double quotes. Leading and trailing whitespace
	 * for each column is trimmed. Quote characters surrounding a value are not
	 * included in the returned tokens.
	 *
	 * @param line            input text
	 * @param sep             delimiter character
	 * @param bCouldHaveQuotes whether quote parsing is enabled
	 * @param sizeEstimate    initial array-list capacity hint
	 * @return parsed column values; empty array when {@code line} is empty
	 */
	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes, int sizeEstimate) {
		if (line == null || sep == 0) {
			return null;
		}
		if (line.length() == 0) {
			return new String[0];
		}
		ArrayList<String> alString = new ArrayList<String>(Math.max(5, sizeEstimate));

		int lineLength = line.length();
		boolean bStarted = false;
		int startPos = 0;
		char qchar = 0;
		int lastQpos = -1;
		int firstWhitespace = -1;

		for (int i = 0;; i++) {
			char ch = 0;
			if (i != lineLength) {
				ch = line.charAt(i);
				if (bCouldHaveQuotes && (ch == '\'' || ch == '\"') && ch != sep) {
					if (!bStarted) {
						qchar = ch;
						bStarted = true;
						startPos = i + 1;
						continue;
					} else {
						if (ch == qchar) {
							lastQpos = i; // might be ending pos
							continue; // continue to sep char or eol
						}
					}
				}
			}

			boolean bWhitespace;
			if (i != lineLength) {
				bWhitespace = Character.isWhitespace(ch); // (" \n\r\f\b\t".indexOf(ch) >= 0);
			} else {
				bWhitespace = false;
			}

			if (i == lineLength || ch == sep) {
				if (qchar > 0 && lastQpos < 1) {
				    if (i != lineLength) continue; // sep inside of quotes
			        startPos--;
				}
				if (i == startPos) {
					alString.add("");
				} else {
					int j;
					if (lastQpos >= startPos) {
						j = lastQpos;
					} else if (firstWhitespace >= 0) {
						j = firstWhitespace;
					} else {
						j = i;
					}

					String s = line.substring(startPos, j);
					alString.add(s);
				}
				if (i == lineLength) {
					break;
				}
				startPos = i + 1;
				bStarted = false;
				qchar = 0;
				firstWhitespace = -1;
				continue;
			}
			lastQpos = -1;
			if (!bStarted) {
				if (bWhitespace) { // skip
					startPos = i + 1;
					continue;
				}
				bStarted = true;
			} else {
				if (bWhitespace) {
					if (firstWhitespace < 0) {
						firstWhitespace = i;
					}
					continue;
				}
			}
			firstWhitespace = -1;
		}

		String[] ss = new String[0];
		ss = alString.toArray(ss);
		return ss;
	}


	/**
	 * Tokenizes {@code text} into name/value segments for CSS/HTML-style
	 * attribute parsing. Supports:
	 * <ul>
	 *   <li>quoted values</li>
	 *   <li>optional inclusion of the delimiter token</li>
	 *   <li>begin/end wrappers (e.g., '&lt;' and '&gt;')</li>
	 *   <li>end-of-value terminators (e.g., ';')</li>
	 * </ul>
	 *
	 * @param text         input text (null returns null)
	 * @param delimChar    delimiter separating name/value
	 * @param spaceIsDelim whether whitespace ends tokens
	 * @param bIncludeDelim whether to emit delimiter as separate token
	 * @param begChar      expected beginning wrapper (e.g., '<')
	 * @param endChar      expected ending wrapper (e.g., '>')
	 * @param eovChar      explicit end-of-value marker (e.g., ';')
	 * @return token array
	 */
	public static String[] tokenize(String text, char delimChar, boolean spaceIsDelim, boolean bIncludeDelim, char begChar, char endChar,
			char eovChar) {
		if (text == null) {
			return null;
		}
		int x = text.length();
		char chQuote = 0;
		ArrayList<String> al = new ArrayList<String>();
		String next = "";
		int lastPos = 0;
		boolean bStarted = false;
		boolean bParsingValue = false;
		for (int i = 0;; i++) {
			if (i == x) {
				if (i == 0 || !bStarted) {
					break;
				}
				char ch = text.charAt(i - 1);
				if (ch == endChar) {
					i--;
				}
				if (lastPos != i) {
					al.add(text.substring(lastPos, i));
				}
				break;
			}
			char ch = text.charAt(i);
			if (!bStarted) {
				if (ch == ' ' || ch == '\t') {
					lastPos++;
					continue;
				}
				if (ch != delimChar) {
					bStarted = true;
				}
			}
			if (i == 0) {
				if (ch == begChar) {
					lastPos = 1;
					continue;
				}
			}
			if (chQuote > 0) {
				if (ch == chQuote) {
					al.add(text.substring(lastPos, i + 1)); // include quotes
					chQuote = 0;
					lastPos = i + 1;
					bStarted = false;
					bParsingValue = false;
				}
				continue;
			}
			if (ch == eovChar) {
				al.add(text.substring(lastPos, i));
				lastPos = i + 1;
				bStarted = false;
				bParsingValue = false;
				continue;
			}
			if (bParsingValue && (ch == '\'' || ch == '\"')) {
				if (i == lastPos) {
					chQuote = ch;
					continue;
				}
			}
			if (ch == delimChar && (!bStarted || !bParsingValue)) {
				al.add(text.substring(lastPos, i));
				if (bIncludeDelim) {
					al.add("" + ch);
				}
				lastPos = i + 1;
				bStarted = false;
				bParsingValue = true;
			}
			if (spaceIsDelim && ch == ' ') {
				al.add(text.substring(lastPos, i));
				lastPos = i + 1;
				bStarted = false;
				if (bParsingValue) {
					bParsingValue = false;
				}
			}
		}
		String[] ss = new String[al.size()];
		al.toArray(ss);
		return ss;
	}

	/**
	 * Parses a CSS-style attribute string into a name/value map.
	 * <p>
	 * Behavior:
	 * <ul>
	 *   <li>Empty or null input returns an empty map.</li>
	 *   <li>If the string begins with a quote, that quote type is used
	 *       as the begin/end wrapper when tokenizing.</li>
	 *   <li>Delegates to {@link #tokenize(String, char, boolean, boolean, char, char, char)}
	 *       using ':' as the delimiter and ';' as the end-of-value marker.</li>
	 *   <li>Every even index in the token array is treated as a key and the next
	 *       token (if any) as its value.</li>
	 *   <li>If a key has no corresponding value token, an empty string is stored.</li>
	 * </ul>
	 *
	 * @param style CSS-style attribute text
	 * @return map of attribute names to values (never null)
	 */
	public static Map<String, String> getCssMap(String style) {
		Map<String, String> map = new HashMap<String, String>();
		if (style == null || style.length() == 0) {
			return map;
		}

		char ch = style.charAt(0);
		if (ch != '\'' && ch != '\"') {
			ch = 0;
		}

		String[] ss = tokenize(style, ':', false, false, ch, ch, ';');
		for (int i = 0; i < ss.length; i += 2) {
			String s = ss[i];
			map.put(ss[i], i + 1 == ss.length ? "" : ss[i + 1]);
		}
		return map;
	}

	/**
	 * Appends {@code value} to an existing CSV string {@code toText} using
	 * comma-separated formatting rules.
	 * <p>
	 * Behavior:
	 * <ul>
	 *   <li>Null values are converted to empty strings.</li>
	 *   <li>If the value is a String or contains ',', '\n', or '"':
	 *       <ul>
	 *         <li>Internal quotes are doubled.</li>
	 *         <li>The entire value is wrapped in quotes unless already quoted.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Whitespace is trimmed after quoting logic.</li>
	 *   <li>Delegates to {@code OATextUtil.concat(...)} for final concatenation.</li>
	 * </ul>
	 *
	 * @param toText existing CSV string (may be null)
	 * @param value  value to append (null becomes empty string)
	 * @return updated CSV string with {@code value} appended
	 */
	public static String csv(String toText, Object value) {
		if (value == null) {
			value = "";
		} else {
			boolean bIsString = value instanceof String;
			if (!bIsString) value = value.toString();

			if (bIsString || ((String) value).indexOf(',') >= 0 || ((String) value).indexOf('\n') >= 0
					|| ((String) value).indexOf('\"') >= 0) {
				value = ((String) value).replace("\"", "\"\"");

				if (!((String) value).startsWith("\"") && !((String) value).endsWith("\"")) {
					value = "\"" + ((String) value) + "\"";
				}
			}

			// value = ((String) value).replace(',', ' ');  // value should be in double quotes
			value = ((String) value).trim();
		}
		String s = OATextUtil.concat(toText, (String) value, ",", true);
		return s;
	}
}
