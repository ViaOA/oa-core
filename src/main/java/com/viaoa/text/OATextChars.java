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


/*qqqqqqqqqqqqq
CODEX

 13. Medium - OATextChars.upper / OATextChars.lower
     Concrete failure: under Turkish default locale, upper("id") can produce "İD" instead of "ID".
     Expected: infrastructure identifiers and path/query tokens use locale-stable casing.
     Actual: JVM default locale affects results.
     Fix direction: use Locale.ROOT.




*/

/**
 * Low-level character classification and transformation helpers.
 * <p>
 * This class focuses on inspecting and converting individual characters,
 * including:
 * <ul>
 *   <li>Digit, letter, whitespace, and punctuation checks</li>
 *   <li>Case conversion (upper/lower)</li>
 *   <li>Alphanumeric rule enforcement</li>
 *   <li>Safe handling of {@code null} and empty inputs</li>
 * </ul>
 *
 * <p>All methods operate on basic Java {@code char} or single-character
 * {@code String} values without performing higher-level grammar or layout
 * logic. More complex transformations should be handled in higher-order
 * modules such as:
 * <ul>
 *   <li>{@link OATextFilter} for removal rules</li>
 *   <li>{@link OATextFormat} for string rewriting</li>
 *   <li>{@link OATextSanitize} for null-safe cleanup</li>
 * </ul>
 *
 * <p>Part of the {@code com.viaoa.text} subsystem in OA 4.0, this class
 * provides foundational character evaluation utilities that support
 * tokenization, validation, formatting, and UI input processing.</p>
 *
 */
public class OATextChars {
	
	/**
	 * Determines whether the given string contains at least one digit
	 * ('0'–'9').
	 * <p>
	 * If {@code word} is {@code null}, {@code false} is returned.
	 *
	 * @param word the string to examine
	 * @return {@code true} if any character is a digit; otherwise {@code false}
	 */
	public static boolean hasDigits(String word) {
	    if (word == null) return false;
	    for (char ch : word.toCharArray()) {
	        if (Character.isDigit(ch)) return true;
	    }
	    return false;
	}	

	/**
	 * Converts the first character of the given string to lowercase if
	 * it is uppercase. If {@code s} is {@code null}, {@code null} is
	 * returned. If the string has a single character, it is converted
	 * directly; otherwise only the leading character is modified.
	 *
	 * @param s the text to modify
	 * @return a string with the first character converted to lowercase,
	 *         or the original string if no change is needed
	 */
	public static String makeFirstCharLower(String s) {
		if (s == null) {
			return null;
		}
		int x = s.length();
		if (x > 0) {
			char ch = s.charAt(0);
			char ch2 = Character.toLowerCase(ch);
			if (ch != ch2) {
				if (x == 1) {
					s = "" + ch2;
				} else {
					s = ch2 + s.substring(1);
				}
			}
		}
		return s;
	}
	
	/**
	 * Converts the leading sequence of uppercase characters to lowercase.
	 * <p>
	 * The scan continues until a non-uppercase character is encountered,
	 * or until the final character is reached. Only the initial run of
	 * uppercase characters is modified.
	 * <p>
	 * Example: {@code "GSMRServer"} becomes {@code "gsmrServer"}.
	 *
	 * @param s the text to modify; {@code null} returns {@code null}
	 * @return the string with its initial uppercase sequence converted to
	 *         lowercase, or the original string if no change occurs
	 */
	public static String makeFirstUpperCharsLower(String s) {
		if (s == null) {
			return null;
		}
		int x = s.length();
		StringBuilder sb = null;
		for (int i = 0; i < x; i++) {
			char ch = s.charAt(i);
			char ch2 = (i + 1 == x ? 0 : s.charAt(i + 1));

			if (Character.isUpperCase(ch) && (i == 0 || (ch2 == 0 || Character.isUpperCase(ch2)))) {
				if (sb == null) {
					sb = new StringBuilder(x);
				}
				sb.append(Character.toLowerCase(ch));
			} else {
				if (sb != null) {
					sb.append(s.substring(i));
				}
				break;
			}
		}
		if (sb != null) {
			return new String(sb);
		}
		return s;
	}

	/**
	 * Converts the first character of the given string to uppercase if
	 * it is lowercase. If {@code s} is {@code null}, {@code null} is
	 * returned. If the string has a single character, it is converted
	 * directly; otherwise only the leading character is modified.
	 *
	 * @param s the text to modify
	 * @return a string with the first character converted to uppercase,
	 *         or the original string if no change is needed
	 */
	public static String makeFirstCharUpper(String s) {
		if (s == null) {
			return null;
		}
		int x = s.length();
		if (x > 0) {
			char ch = s.charAt(0);
			char ch2 = Character.toUpperCase(ch);
			if (ch != ch2) {
				if (x == 1) {
					s = "" + ch2;
				} else {
					s = ch2 + s.substring(1);
				}
			}
		}
		return s;
	}

	/**
	 * Converts the entire string to uppercase. If {@code value} is
	 * {@code null}, {@code null} is returned.
	 *
	 * @param value the text to convert
	 * @return the uppercase version of {@code value}, or {@code null}
	 *         if {@code value} is null
	 */
	public static String upper(String value) {
		if (value == null) {
			return null;
		}
		return value.toUpperCase();
	}

	/**
	 * Converts the entire string to lowercase. If {@code value} is
	 * {@code null}, {@code null} is returned.
	 *
	 * @param value the text to convert
	 * @return the lowercase version of {@code value}, or {@code null}
	 *         if {@code value} is null
	 */
	public static String lower(String value) {
		if (value == null) {
			return null;
		}
		return value.toLowerCase();
	}
	
	
}
