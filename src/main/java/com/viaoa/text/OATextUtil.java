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

import java.awt.Color;

/*qqqqqqqqqqqqq
CODEX

 - file/class/method: OATextUtil.createPath
  - concrete failure scenario: Empty path segments are included, producing invalid OA property paths.
  - example input: createPath("Order", "", "Customer")
  - expected result: Order.Customer
  - actual or likely result: Order..Customer
  - why it matters to OA: OAPath/property path construction can generate paths that fail parsing or resolve
    incorrectly.
  - fix direction: Skip empty strings the same way null segments are skipped.


   Concrete failure: odd-length input "ABC" throws StringIndexOutOfBoundsException; invalid hex like "GG" silently
     produces a bogus byte because Character.digit returns -1.
     Expected: reject invalid/odd hex deterministically.
     Actual: crash or corrupt bytes.
     Fix direction: validate even length and require both nibbles >= 0.


 11. Medium - OATextUtil.makeJavaIdentifier
     Concrete failure: makeJavaIdentifier("1abc") returns "1abc", which is not a valid Java identifier.
     Expected: first character must satisfy Character.isJavaIdentifierStart.
     Actual: only isJavaIdentifierPart is checked.
     Fix direction: handle index 0 separately, replacing or prefixing with _.

 4. Medium - OATextUtil.colorToHex / OAString.colorToHex
     Scenario: method emits #RRGGBBAA while converter/docs expect #RRGGBB.
     Example: OAString.colorToHex(new Color(1,2,3)) returns #010203FF.
     Impact: color string round-trips through OAConverterColor can reinterpret the bytes and corrupt the color.

  13. Medium - OATextUtil.concat
     Scenario: null separator is concatenated as literal "null".
     Example: OAString.concat("a", "b", null) returns "anullb".
     Impact: shared string/path/query composition can leak "null" into generated text.
  14. Medium - OATextUtil.parseInt
     Scenario: integer overflow silently wraps.
     Example: OAString.parseInt("2147483648") returns -2147483648.
     Impact: parsed IDs, sizes, or numeric tokens can be corrupted without failure.

  18. Medium - OATextUtil.getBegin / getEnd
     Scenario: substring helpers can split surrogate pairs.
     Example: OAString.getFirst("😀x", 1) returns an unpaired high surrogate.
     Impact: Unicode text can be corrupted by shared first/last helpers.



*/

/**
 * Utility methods for text composition, lightweight formatting, and
 * identifier/path construction used throughout OA-core.
 *
 * <p><b>Key responsibilities:</b>
 * <ul>
 *   <li>Safe concatenation helpers that avoid returning {@code null}</li>
 *   <li>Simple substring helpers (first/last N characters)</li>
 *   <li>Legacy ASCII integer parsing (first numeric run only)</li>
 *   <li>Color and byte-array ↔ hex string conversion</li>
 *   <li>Java identifier sanitization</li>
 *   <li>Creation of OA property path strings (with Hub filter support)</li>
 * </ul>
 *
 * <p><b>Design notes:</b>
 * <ul>
 *   <li>All string-returning methods avoid returning {@code null} unless explicitly documented</li>
 *   <li>Backward-compatible with existing OA usage and expectations</li>
 *   <li>Performance-first, with minimal allocations for common composition patterns</li>
 * </ul>
 *
 * <p><b>Behavior references:</b>
 * <ul>
 *   <li>{@link #parseInt(String)} stops parsing on first non-digit after numeric run</li>
 *   <li>{@link #convertToLikeSearch(String)} translates {@code * → %} and ensures a trailing {@code %}</li>
 *   <li>{@link #createPath(String...)} preserves Hub filter syntax when segment starts with ':'</li>
 * </ul>
 *
 */
public class OATextUtil {

	/**
	 * Appends {@code append} to {@code orig}, inserting a single space when
	 * {@code orig} is non-empty. Delegates to {@link #concat(String, String, String, boolean)}
	 * with a space separator.
	 *
	 * @param orig   original text (may be null)
	 * @param append value to append (may be null)
	 * @return concatenated string, never {@code null}
	 */
	public static String append(String orig, String append) {
		return concat(orig, append, " ");
	}

	/**
	 * Appends {@code append} to {@code orig} using the supplied separator.
	 * The separator is inserted only when {@code orig} is non-empty.
	 *
	 * @param orig   original text (may be null)
	 * @param append value to append (may be null)
	 * @param sep    separator inserted between values
	 * @return concatenated string, never {@code null}
	 */
	public static String append(String orig, String append, String sep) {
		return concat(orig, append, sep, true);
	}

	/**
	 * Prepends {@code prepend} to {@code orig}, optionally inserting a separator
	 * before the original text when it is non-empty.
	 *
	 * @param orig    original text (may be null)
	 * @param prepend value to put before {@code orig}
	 * @param sep     separator inserted before {@code orig} when it has content
	 * @return resulting string with {@code prepend} placed first
	 */
	public static String prepend(String orig, String prepend, String sep) {
		if (orig == null) {
			orig = "";
		}
		else if (sep != null && orig.length() > 0) {
			orig = sep + orig;
		}
		orig = prepend + orig;
		return orig;
	}
    
	/**
	 * Concatenates {@code value} to {@code toText} using a single space as the
	 * separator. If {@code value} is null or empty, the original {@code toText}
	 * is returned, with null converted to an empty string.
	 *
	 * @param toText existing text (may be null)
	 * @param value  value to append (may be null)
	 * @return concatenated string, never {@code null}
	 */
	public static String concat(String toText, String value) {
		return concat(toText, value, " ", true);
	}

	/**
	 * Concatenates {@code value} to {@code toText} using {@code sepChar} as the
	 * separator. The {@code value} is converted with {@code toString()} when
	 * non-null.
	 *
	 * @param toText existing text (may be null)
	 * @param value  value to append (may be null)
	 * @param sepChar separator to insert between values
	 * @return concatenated string, never {@code null}
	 */
	public static String concat(String toText, Object value, String sepChar) {
		String strValue = value == null ? null : value.toString();
		return concat(toText, strValue, sepChar, false);
	}

	/**
	 * Concatenates {@code value} to {@code toText} using {@code sepChar} as the
	 * separator. If {@code value} is null or empty, {@code toText} is returned,
	 * with null converted to an empty string.
	 *
	 * @param toText existing text (may be null)
	 * @param value  value to append (may be null)
	 * @param sepChar separator to insert between values
	 * @return concatenated string, never {@code null}
	 */
	public static String concat(String toText, String value, String sepChar) {
		return concat(toText, value, sepChar, false);
	}

	/**
	 * Core concatenation helper that appends {@code value} to {@code toText}
	 * using {@code sepChar} as the separator.
	 * <ul>
	 *   <li>If {@code bForce} is false and {@code value} is null or empty,
	 *       returns {@code toText} (null becomes empty string).</li>
	 *   <li>Otherwise {@code value} is coerced to non-null and appended.</li>
	 *   <li>When {@code toText} is null or empty, no separator is added.</li>
	 * </ul>
	 *
	 * @param toText existing text (may be null)
	 * @param value  value to append (may be null)
	 * @param sepChar separator to insert between values
	 * @param bForce whether to append even when {@code value} is null/empty
	 * @return concatenated string, never {@code null}
	 */
	public static String concat(String toText, String value, String sepChar, boolean bForce) {
		if (!bForce && (value == null || value.length() == 0)) {
			if (toText == null) return "";  // always return non-null, so a null check does not have be used.
			return toText;
		}
		if (value == null) {
			value = "";
		}
		if (toText == null || toText.length() == 0) {
			toText = value;
		} else {
			toText += sepChar;
			toText += value;
		}
		return toText;
	}

    
	/**
	 * Converts a {@link Color} to a 6-digit RGB hex string prefixed with {@code "#"}.
	 *
	 * @param color color to convert (may be null)
	 * @return hex string such as {@code "#00FFCC"}, or null if {@code color} is null
	 */
	public static String colorToHex(Color color) {
		if (color == null) {
			return null;
		}
		String colorStr = String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
		return colorStr;
	}
    
	/**
	 * Converts {@code txt} into a Java identifier-compatible string by replacing
	 * any character that is not a {@link Character#isJavaIdentifierPart(char)}
	 * with an underscore.
	 *
	 * @param txt input text (may be null)
	 * @return sanitized identifier, or null if {@code txt} is null
	 */
	public static String makeJavaIdentifier(String txt) {
		if (txt == null) {
			return null;
		}
		int x = txt.length();
		StringBuilder sb = null;
		for (int i = 0; i < x; i++) {
			char ch = txt.charAt(i);
			if (!Character.isJavaIdentifierPart(ch)) {
				if (sb == null) {
					sb = new StringBuilder(x);
					if (i > 0) {
						sb.append(txt.substring(0, i));
					}
				}
				ch = '_';
			}
			if (sb != null) {
				sb.append(ch);
			}
		}
		if (sb == null) {
			return txt;
		}
		return new String(sb);
	}


	/**
	 * Returns at most the last {@code len} characters from {@code text}.
	 *
	 * @param text source text (may be null)
	 * @param len  number of characters to return; {@code len <= 0} yields {@code ""}
	 * @return last {@code len} characters, entire string if shorter, or null when {@code text} is null
	 */
	public static String getEnd(String text, int len) {
		if (text == null) {
			return null;
		}
		if (len <= 0) return "";
		int x = text.length();
		if (x <= len) {
			return text;
		}
		String s = text.substring(x - len);
		return s;
	}

	/**
	 * Alias for {@link #getEnd(String, int)}.
	 *
	 * @param text source text (may be null)
	 * @param len  number of characters to return
	 * @return last {@code len} characters or null when {@code text} is null
	 */
	public static String getLast(String text, int len) {
		return getEnd(text, len);
	}

	/**
	 * Returns at most the first {@code len} characters from {@code text}.
	 *
	 * @param text source text (may be null)
	 * @param len  number of characters to return; {@code len <= 0} yields {@code ""}
	 * @return first {@code len} characters, entire string if shorter, or null when {@code text} is null
	 */
	public static String getBegin(String text, int len) {
		if (text == null) {
			return null;
		}
		if (len <= 0) return "";
		int x = text.length();
		if (x <= len) {
			return text;
		}
		String s = text.substring(0, len);
		return s;
	}

	/**
	 * Alias for {@link #getBegin(String, int)}.
	 *
	 * @param text source text (may be null)
	 * @param len  number of characters to return
	 * @return first {@code len} characters or null when {@code text} is null
	 */
	public static String getFirst(String text, int len) {
		return getBegin(text, len);
	}


	/**
	 * Parses the first contiguous numeric run in {@code val} into an int.
	 * <ul>
	 *   <li>Skips non-digit characters until a digit or leading {@code '-'} is found.</li>
	 *   <li>Accumulates digits until a non-digit is encountered.</li>
	 *   <li>Applies a negative sign if a leading {@code '-'} was seen.</li>
	 * </ul>
	 *
	 * @param val input text (may be null)
	 * @return parsed integer value, or 0 when no digits are found
	 */
	public static int parseInt(String val) {
		int x = 0;
		if (val == null) {
			return x;
		}
		boolean bStarted = false;
		boolean bNeg = false;
		int len = val.length();
		for (int i = 0; i < len; i++) {
			char c = val.charAt(i);
			if (Character.isDigit(c)) {
				x *= 10;
				x += c - '0';
				bStarted = true;
			} else {
				if (bStarted) {
					break;
				}
				if (c == '-') {
					bNeg = true;
					bStarted = true;
				}
			}
		}
		if (bNeg) {
			x *= -1;
		}
		return x;
	}
	
	/**
	 * Converts a value into a pattern suitable for a SQL LIKE search.
	 * <ul>
	 *   <li>Replaces all '{@code *}' characters with '{@code %}'.</li>
	 *   <li>If no '{@code %}' is present after replacement, appends one.</li>
	 * </ul>
	 *
	 * @param s input text (may be null)
	 * @return transformed pattern, or null when {@code s} is null
	 */
	public static String convertToLikeSearch(String s) {
		if (s == null) {
			return s;
		}
		s = s.replace("*", "%");
		if (s.indexOf('%') < 0) {
			s += "%";
		}
		return s;
	}

	/**
	 * Builds a two-line string that visually displays column indices.
	 * <p>
	 * The first line contains the tens digit (or space) for each position;
	 * the second line contains the ones digit. Positions are inclusive
	 * between {@code startPos} and {@code endPos}.
	 *
	 * @param startPos starting index (inclusive)
	 * @param endPos   ending index (inclusive)
	 * @return multi-line string showing vertical numeric labels
	 */
	public static String getVerticalNumberLines(int startPos, int endPos) {
		StringBuilder sb = new StringBuilder();

		for (int i = startPos; i <= endPos; i++) {
			if (i % 10 == 0) {
				int x = (i / 10);
				sb.append("" + (x % 10));
			} else {
				sb.append(' ');
			}
		}

		sb.append('\n');

		for (int i = startPos; i <= endPos; i++) {
			sb.append("" + (i % 10));
		}

		return sb.toString();
	}

	/**
	 * Produces a two-line vertical representation of hex bytes.
	 * <p>
	 * The first line contains the high nibble of each byte, and the second
	 * line contains the low nibble, using uppercase hex digits.
	 *
	 * @param bs byte array to render
	 * @return null if bs is null, else two-line hex representation
	 */
	public static String getVerticalHex(byte[] bs) {
		if (bs == null) return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bs.length; i++) {
			String hex = String.format("%02x", bs[i]).toUpperCase();
			sb.append(hex, 0, 1);
		}

		sb.append('\n');

		for (int i = 0; i < bs.length; i++) {
			String hex = String.format("%02x", bs[i]).toUpperCase();
			sb.append(hex, 1, 2);
		}
		return sb.toString();
	}

	/**
	 * Lookup table of hexadecimal characters used by {@link #bytesToHex(byte[])}
	 * for efficient byte-to-hex encoding.
	 */
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	// https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java

	/**
	 * Converts a byte array into an uppercase hex string with two characters
	 * per byte.
	 *
	 * @param bytes source byte array
	 * @return hex string representation of {@code bytes}
	 */
	public static String bytesToHex(byte[] bytes) {
		if (bytes == null) return null;
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Converts a hex string into a byte array. Each pair of hex characters
	 * is parsed into a single byte.
	 *
	 * @param hex hex string (may be null)
	 * @return decoded bytes, or null when {@code hex} is null
	 */
	public static byte[] hexToBytes(String hex) {
		if (hex == null) {
			return null;
		}
		int x = hex.length();
		byte[] bs = new byte[x / 2];

		for (int i = 0; i < x; i += 2) {
			bs[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return bs;
	}

	/**
	 * Creates a new string consisting of {@code length} repetitions of
	 * {@code repeatChar}. Uses a {@link StringBuilder} sized to the
	 * requested length.
	 *
	 * @param repeatChar the character to repeat
	 * @param length     number of repetitions to generate
	 * @return a string composed of {@code repeatChar} repeated {@code length} times
	 */
	public static String createString(char repeatChar, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(repeatChar);
		}
		return sb.toString();
	}
	
	/**
	 * Builds a dot-delimited property path from a sequence of string segments.
	 * <ul>
	 *   <li>Null {@code args} returns an empty string.</li>
	 *   <li>Null or empty elements are skipped.</li>
	 *   <li>A segment beginning with ':' is appended verbatim (used for filters).</li>
	 *   <li>Otherwise segments are joined with '.' characters.</li>
	 * </ul>
	 *
	 * @param args property path segments
	 * @return dot-delimited path string
	 */
	public static String createPath(String... args) {
		if (args == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(48);

		for (String s : args) {
			if (s == null) {
				continue;
			}

			if (sb.length() == 0) {
				sb.append(s);
			} else {
				if (s.indexOf(':') == 0) {
					sb.append(s); // filter
				} else {
					sb.append(".");
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Builds a dot-delimited property path beginning with the simple name
	 * of {@code clazz}. Remaining segments follow the same rules as
	 * {@link #createPath(String...)}.
	 *
	 * @param clazz starting class whose simple name is used as prefix
	 * @param args  additional path segments
	 * @return property path beginning with the class name
	 */
	public static String createPath(Class clazz, String... args) {
		if (args == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(48);
		for (String s : args) {
			if (s == null) {
				continue;
			}
			if (sb.length() == 0) {
				if (clazz != null) {
					sb.append("(");
					sb.append(clazz.getName());
					sb.append(")");
					sb.append(s);
				} else {
					sb.append(s);
				}
			} else {
				if (s.indexOf(':') == 0) {
					sb.append(s); // filter
				} else {
					sb.append(".");
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}

}


