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

/*qqqqqqqqqqq
CODEX

 14. Medium - OATextFilter.convert
     Concrete failure: convert("abcabc", "a", "X", false, true, 0, -1) returns only the replaced prefix, likely "X",
     instead of "Xbcabc".
     Expected: replace first match and append the remaining text.
     Actual: loop breaks without appending the suffix.
     Fix direction: when bFirstOnly matches, append the untouched remainder; also honor first-only when replacement is
     empty.

  15. Medium - OATextFilter.convert
     Scenario: negative startPos throws.
     Example: OAString.convert("abc", "a", "x", false, false, -1, -1) throws StringIndexOutOfBoundsException.
     Impact: range-based replacement is not edge-safe for caller-provided indexes.

 16. Medium - OATextFilter.removeEndingChars
     Scenario: negative amount throws.
     Example: OATextFilter.removeEndingChars("abc", -1) throws StringIndexOutOfBoundsException.
     Impact: shared trimming helper can fail on unchecked count values.
*/


/**
 * Utility methods for filtering or transforming text by removing characters or
 * substrings that are not desired for display, storage, or parsing.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Removing characters based on blacklist or whitelist rules</li>
 *   <li>Condensing or stripping leading/trailing whitespace</li>
 *   <li>Filtering characters to alphanumeric-only content</li>
 *   <li>Ensuring file name safety by allowing only valid characters</li>
 *   <li>Substring-safe indexing to prevent {@link IndexOutOfBoundsException}</li>
 *   <li>Simple substring replacement utilities</li>
 * </ul>
 *
 * <p>This class focuses on filtering and removal — without performing formatting,
 * tokenization, casing rules, or grammar modifications. Each method is null-safe
 * and optimized to minimize intermediate allocations when possible.</p>
 *
 * <p>Part of the {@code com.viaoa.text} family of classes providing clearer
 * separation of string concerns in OA 4.0.</p>
 *
 */
public class OATextFilter {
	
	/**
	 * Removes all characters in {@code chars} from the input {@code value}.
	 * <p>
	 * Delegates to {@link #stripChars(String, String, boolean)} with
	 * {@code bKeepChars == false}.
	 *
	 * @param value the text to filter; may be null
	 * @param chars characters to remove
	 * @return a filtered string, or {@code null} if {@code value} is null
	 */
	public static String strip(String value, String chars) {
		return stripChars(value, chars, false);
	}

	
	/**
	 * Keeps only the characters that appear in {@code chars}, removing all others.
	 * <p>
	 * Delegates to {@link #stripChars(String, String, boolean)} with
	 * {@code bKeepChars == true}.
	 *
	 * @param value the text to filter; may be null
	 * @param chars the whitelist of valid characters
	 * @return a filtered string, or {@code null} if {@code value} is null
	 */
	public static String accept(String value, String chars) {
		return stripChars(value, chars, true);
	}

	/**
	 * Core filtering mechanism supporting both blacklist and whitelist behavior.
	 * <ul>
	 *   <li>If {@code bKeepChars == false}, removes any character that appears in {@code chars}.</li>
	 *   <li>If {@code bKeepChars == true}, keeps only characters that appear in {@code chars}.</li>
	 * </ul>
	 * Optimizes lookups for ASCII characters using a boolean mask before falling
	 * back to {@link String#indexOf(int)} for non-ASCII values.
	 *
	 * @param value      the text to filter; returned unchanged if null
	 * @param chars      the character set to check against; if null or empty, {@code value} is returned
	 * @param bKeepChars whether to keep (true) or remove (false) characters found in {@code chars}
	 * @return a filtered string
	 */
	protected static String stripChars(String value, String chars, boolean bKeepChars) {
	    if (value == null) return null;
	    if (chars == null || chars.length() == 0) return value;

	    int len = value.length();
	    int clen = chars.length();
	    StringBuilder sb = new StringBuilder(len);

	    // Optimization: ASCII lookup table + flag for unicode presence
	    boolean[] mask = new boolean[128];
	    boolean hasAscii = false;
	    for (int i = 0; i < clen; i++) {
	        char c = chars.charAt(i);
	        if (c < 128) {
	            mask[c] = true;
	            hasAscii = true;
	        }
	    }

	    for (int i = 0; i < len; i++) {
	        char c = value.charAt(i);
	        boolean inSet = (c < 128) ? mask[c] : chars.indexOf(c) >= 0;

	        if (bKeepChars) {
	            if (inSet) sb.append(c);
	        } else {
	            if (!inSet) sb.append(c);
	        }
	    }
	    return sb.toString();
	}
		
	/**
	 * Replaces each occurrence of character {@code c} in {@code value} with the
	 * provided replacement string. Case-sensitive.
	 *
	 * @param value   the text to modify
	 * @param c       the character to replace
	 * @param replace the replacement string; null is treated as an empty string
	 * @return the converted string
	 */
	public static String convert(String value, char c, String replace) {
		return convert(value, c + "", replace, false);
	}

	/**
	 * Performs case-insensitive replacement of all occurrences of {@code search}
	 * within {@code line}.
	 * <p>
	 * Delegates to the core {@link #convert(String, String, String, boolean)} with
	 * {@code bIgnoreCase == true}.
	 *
	 * @param line    the text to modify; may be null
	 * @param search  the substring to find
	 * @param replace the replacement string; null becomes an empty string
	 * @return the converted string
	 */
	public static String convertIgnoreCase(String line, String search, String replace) {
		return convert(line, search, replace, true);
	}

	/**
	 * Replaces each case-sensitive occurrence of {@code search} in {@code line}.
	 * <p>
	 * Delegates to {@link #convert(String, String, String, boolean)} with
	 * {@code bIgnoreCase == false}.
	 *
	 * @param line    the text to modify
	 * @param search  the substring to replace
	 * @param replace the replacement string
	 * @return a modified string, or {@code null} if {@code line} is null
	 */
	public static String convert(String line, String search, String replace) {
		return convert(line, search, replace, false);
	}

	/**
	 * Removes every character from {@code line} that appears in {@code search}.
	 *
	 * @param line   the text to filter; may be null
	 * @param search the characters to remove; if null, {@code line} is returned
	 * @return the filtered string
	 */
	public static String removeCharacters(String line, String search) {
		if (line == null || search == null) {
			return line;
		}
		StringBuilder sb = new StringBuilder(line.length());
		int x = line.length();
		for (int i = 0; i < x; i++) {
			char ch = line.charAt(i);
			if (search.indexOf(ch) < 0) {
				sb.append(ch);
			}
		}
		return new String(sb);
	}

	/**
	 * Removes every character from {@code line} that does not appear in the
	 * {@code keep} set.
	 *
	 * @param line the text to filter; may be null
	 * @param keep the whitelist of characters; if null, {@code line} is returned
	 * @return a filtered string containing only characters in {@code keep}
	 */
	public static String removeOtherCharacters(String line, String keep) {
		if (line == null || keep == null) {
			return line;
		}
		StringBuilder sb = new StringBuilder(line.length());
		int x = line.length();
		for (int i = 0; i < x; i++) {
			char ch = line.charAt(i);
			if (keep.indexOf(ch) >= 0) {
				sb.append(ch);
			}
		}
		return new String(sb);
	}
	
	/**
	 * Removes all non-digit characters from {@code line}.
	 * <p>
	 * Delegates to {@link #removeNonDigits(String, boolean)} with dot-allowance disabled.
	 *
	 * @param line the text to filter
	 * @return a string containing only digits, or {@code null} if {@code line} is null
	 */
	public static String removeNonDigits(String line) {
		return removeNonDigits(line, false);
	}

	/**
	 * Removes all characters from {@code line} except digits, optionally allowing
	 * the decimal point character '.'.
	 *
	 * @param line      the text to filter; may be null
	 * @param bAllowDot whether '.' should be considered valid
	 * @return a filtered string
	 */
	public static String removeNonDigits(String line, boolean bAllowDot) {
		if (line == null) {
			return line;
		}
		StringBuilder sb = new StringBuilder(line.length());
		int x = line.length();
		for (int i = 0; i < x; i++) {
			char ch = line.charAt(i);
			if (Character.isDigit(ch) || (bAllowDot && ch == '.')) {
				sb.append(ch);
			}
		}
		return new String(sb);
	}
	
	
	public static final String OtherFileNameChars = "_-. \\/";

	/**
	 * Removes characters that are not permitted in filename values. Allows
	 * alphanumeric characters and symbols listed in {@link #OtherFileNameChars}.
	 * Also allows a colon (:) only when located at index 1 (Windows drive letter).
	 *
	 * @param line the filename text to sanitize
	 * @return a cleaned filename string, or {@code null} if {@code line} is null
	 */
	public static String removeNonFileNameChars(String line) {
		if (line == null) {
			return line;
		}
		StringBuilder sb = new StringBuilder(line.length());
		int x = line.length();
		for (int i = 0; i < x; i++) {
			char ch = line.charAt(i);
			boolean b = (ch == ':' && i == 1);
			if (!b) {
				b = (Character.isDigit(ch) || Character.isLetter(ch) || OtherFileNameChars.indexOf(ch) >= 0);
			}
			if (b) {
				sb.append(ch);
			}
		}
		return new String(sb);
	}

	
	/**
	 * Delegates to the full-parameter convert method, specifying that replacements
	 * should not be limited to the first match and the entire string range should
	 * be scanned.
	 *
	 * @param line        the text to modify
	 * @param search      the substring to replace
	 * @param replace     the replacement value; null becomes ""
	 * @param bIgnoreCase whether matching is case-insensitive
	 * @return the modified string, or {@code null} if {@code line} is null
	 */
	public static String convert(String line, String search, String replace, boolean bIgnoreCase) {
		return convert(line, search, replace, bIgnoreCase, false, 0, -1);
	}

	/**
	 * Core substring replacement engine supporting case-insensitive matching,
	 * limiting to the first replacement, and restricting the scan range.
	 * <ul>
	 *   <li>If {@code line} or {@code search} is null/empty, {@code line} is returned.</li>
	 *   <li>Lowercases characters when {@code bIgnoreCase} is true.</li>
	 *   <li>Performs manual scanning to avoid repeated substring allocation.</li>
	 *   <li>Backtracks on partial matches to ensure correct behavior.</li>
	 * </ul>
	 *
	 * @param line        the text to modify
	 * @param search      the substring to find
	 * @param replace     the replacement text; null converted to empty
	 * @param bIgnoreCase whether search is case-insensitive
	 * @param bFirstOnly  whether only the first match should be replaced
	 * @param startPos    starting index for scanning
	 * @param endPos      exclusive end index, or -1 to scan the entire string
	 * @return the modified string
	 */
	public static String convert(final String line, String search, String replace, final boolean bIgnoreCase, final boolean bFirstOnly, final int startPos, final int endPos) {
		if (line == null || search == null || search.length() == 0) {
			return line;
		}
		if (replace == null) {
			replace = "";
		}
		
		final int xs = search.length();
		if (xs == 0) return line;

		if (bIgnoreCase) {
			search = search.toLowerCase();
		}

		final int xr = replace.length();
		final int xl = line.length();

		StringBuilder sb = null; // dont allocate until first match is found
		char c = 0, origChar = 0;
		for (int i = startPos, j = 0;; i++) {

			if (i < xl && (endPos < 0 || i < endPos)) {
				origChar = c = line.charAt(i);
				if (bIgnoreCase) {
					c = Character.toLowerCase(c);
				}
				if (c == search.charAt(j)) {
					j++;
					if (j == xs) {
						if (sb == null) {
							sb = new StringBuilder(xl + (xl / 10));
							int e = (i - j) + 1;
							if (e > 0) {
								sb.append(line.substring(0, e));
							}
						}

						if (xr > 0) {
							sb.append(replace);
							if (bFirstOnly) {
								break;
							}
						}
						j = 0;
					}
					continue;
				}
			}
			if (j > 0) {
                // i needs to be set back to next char from prev start position
                i -= j;
				if (i >= 0 && sb != null) {
					sb.append(line.charAt(i));
				}
                j = 0;
                continue;
			}
			if (i >= xl || (endPos >= 0 && i >= endPos)) {
				break;
			}
			if (sb != null) {
				sb.append(origChar);
			}
		}
		if (sb == null) {
			return line;
		}
		return new String(sb);
	}
	
	/**
	 * Removes all digit characters (0–9) from {@code value}. Does not remove
	 * decimal points or other symbols.
	 *
	 * @param value the text to filter; may be null
	 * @return a string with digits removed
	 */
	public static String stripDigits(String value) {
		if (value == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(value.length());
		int x = value.length();
		for (int i = 0; i < x; i++) {
			char c = value.charAt(i);
			if (!Character.isDigit(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts any non-ASCII characters in {@code text} to ASCII equivalents when
	 * possible (e.g., curly quotes → straight quotes, en dash → hyphen). Characters
	 * without a mapping are replaced with a space (' ').
	 *
	 * @param text the text to sanitize; may be null
	 * @return ASCII-only text, or the original text if already ASCII
	 */
	public static String convertToAscii(String text) {
		if (text == null) {
			return text;
		}

		int x = text.length();
		StringBuilder sb = null;
		for (int i = 0; i < x; i++) {
			char c = text.charAt(i);
			if (c > 127) {
				if (sb == null) {
					sb = new StringBuilder(text.length());
					if (i > 0) {
						sb.append(text.substring(0, i));
					}
				}
				switch (c) {
				case 8216:
				case 8217:
					c = '\'';
					break;
				case 8220:
				case 8221:
					c = '\"';
					break;
				case 8211:
					c = '-';
					break;
				default:
					c = ' ';
					break;
				}
			}
			if (sb != null) {
				sb.append(c);
			}
		}
		if (sb == null) return text;
		return sb.toString();
	}
	
	/**
	 * Removes {@code amt} characters from the end of {@code s}. If {@code amt}
	 * exceeds the string length, an empty string is returned.
	 *
	 * @param s   the text to modify; may be null
	 * @param amt number of characters to remove from the end
	 * @return the shortened string
	 */
	public static String removeEndingChars(String s, int amt) {
		if (s == null) {
			return null;
		}
		int x = s.length();
		if (amt >= x) {
			return "";
		}
		s = s.substring(0, x - amt);
		return s;
	}

	/**
	 * Delegates to {@link #removeLeading(String, char, int)} with no limit on how
	 * many leading characters can be removed.
	 *
	 * @param s  the text to modify
	 * @param ch the character to remove from the start
	 * @return the string without leading occurrences of {@code ch}
	 */
	public static String removeLeading(String s, char ch) {
	    return removeLeading(s, ch, 0);
	}
	
	/**
	 * Removes up to {@code maxAmount} leading occurrences of character {@code ch}
	 * from {@code s}. If {@code maxAmount} is zero or negative, all leading
	 * occurrences are removed.
	 *
	 * @param s         the text to modify; may be null
	 * @param ch        the character to strip
	 * @param maxAmount maximum number of characters to remove; 0 means unlimited
	 * @return the modified string
	 */
    public static String removeLeading(String s, char ch, int maxAmount) {
        if (s == null) return s;
        
        int x = s.length();
        int i = 0;
        for ( ; i<x; i++) {
            if (s.charAt(i) != ch) {
                break;
            }
            if (maxAmount > 0 && i >= maxAmount) break;
        }
        if (i == 0) return s;
        if (i == x) return "";
        return s.substring(i);
    }

    /**
     * Collapses whitespace by:
     * <ol>
     *   <li>Removing all leading spaces</li>
     *   <li>Reducing sequences of internal spaces to a single space</li>
     *   <li>Removing trailing spaces</li>
     * </ol>
     * Does not treat quoted text specially.
     *
     * @param line the text to trim; may be null
     * @return the condensed string
     */
	public static String trimSpaces(final String line) {
		if (line == null) return line;
		StringBuilder sb = null;
		int max = line.length();
		if (max == 0) return line;
		boolean bSpace = false;
		
		for (int i=0; i<max; i++) {
			char ch = line.charAt(i);
			
			if (ch == ' ') {
				bSpace = true;
			}
			else {
				if (bSpace) {
					bSpace = false;
					if (sb != null) sb.append(' ');
				}
				if (sb == null) {
					sb = new StringBuilder(line.length());
				}
				sb.append(ch);
			}
		}
		if (sb == null) {
			if (line.charAt(0) != ' ') return line; // no spaces
			return ""; // all spaces
		}
		return sb.toString();
	}

	/**
	 * Safely returns a substring starting at {@code pos} without throwing
	 * {@link IndexOutOfBoundsException}.
	 * <ul>
	 *   <li>If {@code s} is {@code null}, returns {@code null}.</li>
	 *   <li>If {@code pos} is greater than or equal to {@code s.length()}, returns an empty string.</li>
	 *   <li>Otherwise, delegates to {@link String#substring(int)}.</li>
	 * </ul>
	 *
	 * @param s   the source string
	 * @param pos the zero-based starting index
	 * @return the substring from {@code pos}, an empty string if {@code pos} is out of range,
	 *         or {@code null} if {@code s} is null
	 */
	public static String substring(String s, int pos) {
		if (s == null) {
			return null;
		}
		if (s.length() <= pos) {
			return "";
		}
		if (pos < 0) return "";
		return s.substring(pos);
	}

	/**
	 * Safely returns a substring in the range {@code [pos1, pos2)} without
	 * throwing {@link IndexOutOfBoundsException}.
	 * <ul>
	 *   <li>If {@code s} is {@code null}, returns {@code null}.</li>
	 *   <li>If {@code pos1} is greater than or equal to {@code s.length()}, returns an empty string.</li>
	 *   <li>If {@code pos2} is beyond the end of the string, it is clamped to {@code s.length()}.</li>
	 *   <li>Otherwise, delegates to {@link String#substring(int, int)}.</li>
	 * </ul>
	 *
	 * @param s    the source string
	 * @param pos1 the zero-based start index (inclusive)
	 * @param pos2 the zero-based end index (exclusive)
	 * @return the requested substring, an empty string if {@code pos1} is out of range,
	 *         or {@code null} if {@code s} is null
	 */
	public static String substring(String s, int pos1, int pos2) {
		if (s == null) {
			return null;
		}
		if (pos1 < 0) return "";
		if (pos1 > pos2) return "";
		
		if (s.length() <= pos1) {
			return "";
		}
		if (pos2 >= s.length()) {
			return s.substring(pos1);
		}
		return s.substring(pos1, pos2);
	}
	
	
	
}

