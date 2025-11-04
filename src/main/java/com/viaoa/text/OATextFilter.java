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
	 * Removes characters from a String.
	 *
	 * @param value is String to strip from.
	 * @param chars values to remove from value
	 */
	public static String strip(String value, String chars) {
		return stripChars(value, chars, false);
	}

	
	/**
	 * Removes characters from a String that are not valid.
	 *
	 * @param value is String to strip from.
	 * @param chars is the characters that are valid, other characters will be removed.
	 */
	public static String accept(String value, String chars) {
		return stripChars(value, chars, true);
	}


	/**
	 * Removes or keeps characters based on bKeepChars:
	 *
	 * bKeepChars == false: remove any characters found in 'chars'
	 * bKeepChars == true:  keep only characters found in 'chars'
	 *
	 * Does not trim whitespace unless included in 'chars'.
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
	 * Used to replace one value with another within a String.
	 *
	 * @param replace the string or null that will replace every occurance of the the character "c"
	 * @see #convert(String,String,String,boolean) convert()
	 */
	public static String convert(String value, char c, String replace) {
		return convert(value, c + "", replace, false);
	}

	/**
	 * Used to replace one value with another within a String, ignoring case.
	 *
	 * @param replace the string or null that will replace every occurance of the the search string
	 * @see #convert(String,String,String,boolean) convert()
	 */
	public static String convertIgnoreCase(String line, String search, String replace) {
		return convert(line, search, replace, true);
	}

	/**
	 * Used to replace one value with another within a String.
	 *
	 * @param replace the string or null that will replace every occurance of the the search string
	 * @see #convert(String,String,String,boolean) convert()
	 */
	public static String convert(String line, String search, String replace) {
		return convert(line, search, replace, false);
	}

	/**
	 * Remove any and all search characters from a string.
	 *
	 * @param line   original data
	 * @param search characters to remove
	 * @return
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
	 * Remove any and all characters that are not in string
	 *
	 * @param line original data param search characters to keep
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
	 * Remove any and all characters that are not digits
	 *
	 * @param line original data
	 */
	public static String removeNonDigits(String line) {
		return removeNonDigits(line, false);
	}

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
	 * Remove any and all characters that are not valid in filename.
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
	 * Used to replace one value with another within a String.
	 *
	 * @param line        is String that is to be converted.
	 * @param search      is String that is to be replaced.
	 * @param replace     is replacement value to use. If null, then a blank String will be used.
	 * @param bIgnoreCase if true, then search is not case sensitive.
	 * @return new String where search String is replaced with replace String. If line is null then null is returned. If search is null then
	 *         line is returned.
	 */
	public static String convert(String line, String search, String replace, boolean bIgnoreCase) {
		return convert(line, search, replace, bIgnoreCase, false, 0, -1);
	}

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
	 * Remove digit characters from String.
	 *
	 * @param value is String to strip.
	 * @return if value=null then null, else new String with digits removed. Note: does not remove "." between digits.
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
	 * Make sure that all chars value is &lt;= 127, otherwise convert to a space char
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

	public static String removeLeading(String s, char ch) {
	    return removeLeading(s, ch, 0);
	}
	
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
	 * String trim method.<br>
	 * 1: removes leading spaces<br>
	 * 2: removes extra spaces within. (Note: even if enclosed in quotes)<br>
	 * 3: removes trailing spaces <br>
	 * <p>
	 *
	 * <pre>
	 Example:  "  this    is   a  test  "  will be: "this is a test"
	 * </pre>
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
	 * safe String substring function. Will not throw out of bounds exception.
	 *
	 * @param pos begin pos (0 based)
	 */
	public static String substring(String s, int pos) {
		if (s == null) {
			return null;
		}
		if (s.length() <= pos) {
			return "";
		}
		return s.substring(pos);
	}

	/**
	 * safe String substring function. Will not throw out of bounds exception.
	 *
	 * @param s
	 * @param pos1 begin pos (0 based)
	 * @param pos2 exclusive end pos (0 based)
	 */
	public static String substring(String s, int pos1, int pos2) {
		if (s == null) {
			return null;
		}
		if (s.length() <= pos1) {
			return "";
		}
		if (pos2 >= s.length()) {
			return s.substring(pos1);
		}
		return s.substring(pos1, pos2);
	}
	
	
	
}

