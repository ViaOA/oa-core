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
package com.viaoa.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	 * Returns the amount of particular String within a String.
	 *
	 * @param str is String to search within.
	 * @param sep is String to search for.
	 * @return number of occurrences of sep.
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
	
	
	public static int countMatches(String str, String sep) {
		return dcount(str, sep);
	}
	public static int countMatches(String str, char sep) {
		return dcount(str, sep);
	}
	
	
	/**
	 * Used to get a count of the number of values between a separator/delimiter.
	 * <p>
	 * Note: even if there is not a value between consective separators, it is still counted as another value - in this case a blank.
	 *
	 * @param str is String to search. If null or length = 0, then 0 is returned.
	 * @param sep separator.
	 */
	public static int dcount(String str, String sep) {
		if (str == null || str.length() == 0) {
			return 0;
		}
		return count(str, sep) + 1;
	}


	public static int dcount(String str, char sep) {
		if (str == null || str.length() == 0 || sep == 0) {
			return 0;
		}
		return count(str, sep + "") + 1;
	}

	
	
	
	
	
	
	/**
	 * Used to retrieve a portion of a String based on a separator value.
	 *
	 * @see #field(String,String,int,int)
	 */
	@Deprecated(since="4.0.0")
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
	@Deprecated(since="4.0.0")
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
	@Deprecated(since="4.0.0")
	public static String field(String str, char sep, int beg) {
		return field(str, sep + "", beg, 1);
	}
	
	/**
	 * Used to retrieve a portion of a String based on a separator value.
	 *
	 * @see #field(String,String,int,int)
	 */
	@Deprecated(since="4.0.0")
	public static String field(String str, char sep, int beg, int amt) {
		return field(str, sep + "", beg, amt);
	}
	
	
	public static String fieldAt(String str, String sep, int beg) {
		return field(str, sep, beg+1);
	}
	/**
	 * '0' based modernized version in 4.0
	 */
	public static String fieldAt(final String str, final String sep, final int beg, final int amt) {
		return field(str, sep, beg+1, amt);
	}
	
	public static String fieldAt(String str, char sep, int beg) {
		return field(str, sep + "", beg+1, 1);
	}

	public static String fieldAt(String str, char sep, int beg, int amt) {
		return field(str, sep + "", beg+1, amt);
	}



	
	


	public static String maskPassword(String name, String val) {
		String s = maskPassword(name, val, "*****", false, "password", "pw", "pass");
		return s;
	}

	public static String maskPassword(String name, String val, String passwordReturn, String... words) {
		String s = maskPassword(name, val, passwordReturn, false, words);
		return s;
	}

	public static String maskPassword(String name, String val, String... words) {
		String s = maskPassword(name, val, "*****", false, words);
		return s;
	}

	/**
	 * Checks to see if name has any words in it that could make it the name of a password. If so then it will return a new value, else
	 * value is returned.
	 *
	 * @param name           name for the value
	 * @param value          the actual value for name
	 * @param maskValue      return value to use if the name is for a password
	 * @param bCaseSensitive if the check should be casesensitive
	 * @param words          words that are used to check if name is a password. Note: uses indexOf>=0 and not equals
	 * @return if name is a password, then passwordReturn else value.
	 */
	public static String maskPassword(String name, String value, String maskValue, boolean bCaseSensitive, String... words) {
		if (name == null || words == null) {
			return value;
		}
		if (bCaseSensitive) {
			name = name.toLowerCase();
		}
		for (String word : words) {
			if (word == null) {
				continue;
			}
			if (bCaseSensitive) {
				word = word.toLowerCase();
			}
			boolean b = name.indexOf(word) >= 0;
			if (b) {
				return maskValue;
			}
		}
		return value;
	}


	public static String[] parseLine(String line, char sep, boolean bCouldHaveQuotes) {
		return parseLine(line, sep, bCouldHaveQuotes, 25);
	}

	/**
	 * Strips out leading and trailing whitespace for each column. if bCouldHaveQuotes is true, then begin and end quotes will be removed;
	 * either single or double quote char.
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
	 * This will split a string based on a delimiter char, and will also take into account values that are in single or double quotes. Used
	 * to parse attributes from html tag, or name/value pairs from CSS style
	 *
	 * @param text
	 * @param delimChar     ex: '=', or ':'
	 * @param bIncludeDelim if true then the delim will be included in the tokens
	 * @param begChar       ex: '&lt;'
	 * @param endChar       ex: '&gt;'
	 * @param eovChar       end of value, ex: ';'
	 * @return
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

	public static String csv(String toText, Object value) {
		if (value == null) {
			value = "";
		} else {
			boolean bIsString = value instanceof String;
			value = value.toString();

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
