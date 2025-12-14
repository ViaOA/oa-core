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

import java.util.Locale;

import com.viaoa.util.OACompare;

/**
 * Utility class for comparing and matching {@link String} values.
 * <p>
 * This supports:
 * <ul>
 * <li>Null-safe equality checks</li>
 * <li>Case-insensitive and blank-aware comparisons</li>
 * <li>Substring search (indexOf / contains)</li>
 * <li>Start/End matching</li>
 * <li>Wildcard matching using {@code isLike()}</li>
 * <li>Prefix/Suffix enforcement helpers</li>
 * </ul>
 *
 * <p>
 * All operations are static and null-safe. Methods that include a
 * {@code boolean bIgnoreCase} parameter treat case using {@link Locale#ROOT} to
 * avoid locale-specific inconsistencies (e.g., Turkish dotted-I).
 * </p>
 *
 * <p>
 * This class is intended to be the centralized comparison API for OA text
 * handling and is used extensively by {@link OAString} convenience methods and
 * other {@code com.viaoa.text.*} modules.  
 * </p>
 *
 */
public class OATextCompare {

	/**
	 * Null-safe equality check with optional case-insensitivity.
	 * <p>
	 * Delegates to {@link #isEqual(String, String, boolean, boolean)} with
	 * {@code bNullEqualsBlank} set to {@code false}.
	 *
	 * @param s           the first string
	 * @param s2          the second string
	 * @param bIgnoreCase whether to compare case-insensitively
	 * @return {@code true} if the strings are equal under the rules; otherwise {@code false}
	 */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase) {
		return isEqual(s, s2, bIgnoreCase, false);
	}

	/**
	 * Convenience method that delegates to
	 * {@link #isEqual(String, String, boolean, boolean)} with both optional
	 * flags disabled.
	 *
	 * @param s  the first string
	 * @param s2 the second string
	 * @return {@code true} if values are equal; otherwise {@code false}
	 */
	public static boolean isEqual(String s, String s2) {
		return isEqual(s, s2, false, false);
	}

	/**
	 * Delegates to {@link #isEqual(String, String, boolean, boolean)} with
	 * case-insensitive comparison enabled and null/blank equivalence disabled.
	 *
	 * @param s  the first string
	 * @param s2 the second string
	 * @return {@code true} if values match ignoring case
	 */
	public static boolean isEqualIgnoreCase(String s, String s2) {
		return isEqual(s, s2, true, false);
	}

	/**
	 * Delegates to {@link #isEqual(String, String, boolean, boolean)} with
	 * null and empty-string treated as equivalent.
	 *
	 * @param s  the first string
	 * @param s2 the second string
	 * @return {@code true} if values are equal or both blank/null
	 */
	public static boolean isEqualNullEqualsBlank(String s, String s2) {
		return isEqual(s, s2, false, true);
	}

	/**
	 * Performs full-control equality comparison with rules for case
	 * sensitivity and null/blank equivalence.
	 * <ul>
	 *   <li>If both references are identical, returns {@code true}.</li>
	 *   <li>If either value is {@code null}, returns {@code true} only if
	 *       {@code bNullEqualsBlank} is enabled and both are considered blank
	 *       by {@link OATextSanitize#isEmpty(String)}.</li>
	 *   <li>If case is ignored, uses {@link String#equalsIgnoreCase(String)}.</li>
	 *   <li>Otherwise, performs exact equality.</li>
	 * </ul>
	 *
	 * @param s                the first value
	 * @param s2               the second value
	 * @param bIgnoreCase      whether to ignore case using {@link Locale#ROOT}
	 * @param bNullEqualsBlank whether null and empty-string are considered equal
	 * @return {@code true} if the values satisfy the comparison rules
	 */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		if (s == s2) {
			return true;
		}
		if (s == null || s2 == null) {
			if (bNullEqualsBlank) {
				if (OATextSanitize.isEmpty(s) && OATextSanitize.isEmpty(s2))
					return true;
			}
			return false;
		}
		if (bIgnoreCase) {
			return s.equalsIgnoreCase(s2);
		}
		return s.equals(s2);
	}

	/**
	 * Alias of {@link #isEqual(String, String, boolean, boolean)} using
	 * default comparison rules.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return {@code true} if the values are equal
	 */
	public static boolean equals(String s1, String s2) {
		return isEqual(s1, s2, false, false);
	}

	/**
	 * Alias of {@link #isEqual(String, String, boolean, boolean)} with
	 * optional case-insensitivity and no null/blank equivalence.
	 *
	 * @param s1          the first string
	 * @param s2          the second string
	 * @param bIgnoreCase whether to compare ignoring case
	 * @return {@code true} if values are equal under the rules
	 */
	public static boolean equals(String s1, String s2, boolean bIgnoreCase) {
		return isEqual(s1, s2, bIgnoreCase, false);
	}

	/**
	 * Logical negation of {@link #isEqual(String, String, boolean, boolean)}
	 * using default rules.
	 *
	 * @param s  the first string
	 * @param s2 the second string
	 * @return {@code true} if values are not equal
	 */
	public static boolean isNotEqual(String s, String s2) {
		return !isEqual(s, s2, false, false);
	}

	/**
	 * Logical negation of {@link #isEqual(String, String, boolean, boolean)}
	 * using optional case-insensitivity.
	 *
	 * @param s           the first string
	 * @param s2          the second string
	 * @param bIgnoreCase whether to ignore case
	 * @return {@code true} if values are not equal
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase) {
		return !isEqual(s, s2, bIgnoreCase, false);
	}

	/**
	 * Logical negation of {@link #isEqual(String, String, boolean, boolean)}
	 * using full comparison-rule control.
	 *
	 * @param s                the first string
	 * @param s2               the second string
	 * @param bIgnoreCase      whether to ignore case
	 * @param bNullEqualsBlank whether null equals blank
	 * @return {@code true} if values are not equal under the rules
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return !isEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/**
	 * Logical negation of {@link #isEqual(String, String, boolean, boolean)}
	 * treating null and blank as equivalent.
	 *
	 * @param s  the first string
	 * @param s2 the second string
	 * @return {@code true} if values are not equal under null-equals-blank rules
	 */
	public static boolean isNotEqualNullEqualsBlank(String s, String s2) {
		return !isEqual(s, s2, false, true);
	}

	/**
	 * Alias of {@link #isNotEqual(String, String, boolean, boolean)} using
	 * default comparison rules.
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return {@code true} if values are not equal
	 */
	public static boolean notEquals(String s1, String s2) {
		return !isEqual(s1, s2, false, false);
	}

	/**
	 * Alias of {@link #isNotEqual(String, String, boolean, boolean)} with
	 * optional case-insensitivity.
	 *
	 * @param s1          the first string
	 * @param s2          the second string
	 * @param bIgnoreCase whether to ignore case
	 * @return {@code true} if values differ under the rules
	 */
	public static boolean notEquals(String s1, String s2, boolean bIgnoreCase) {
		return !isEqual(s1, s2, bIgnoreCase, false);
	}

	/**
	 * Performs wildcard pattern matching following
	 * {@link OACompare#isLike(String, String)} rules.
	 * <ul>
	 *   <li>{@code %} matches any sequence</li>
	 *   <li>{@code _} matches a single character</li>
	 * </ul>
	 *
	 * @param s  the text to test
	 * @param s2 the pattern containing wildcards
	 * @return {@code true} if {@code s} matches the pattern
	 */
	public static boolean isLike(String s, String s2) {
		return OACompare.isLike(s, s2);
	}

	/**
	 * Null-safe lexicographic comparison following the semantics of
	 * {@link String#compareTo(String)}.
	 * <ul>
	 *   <li>Returns 0 if both values are identical or both null.</li>
	 *   <li>Null values sort before non-null values.</li>
	 *   <li>Otherwise delegates to {@code s1.compareTo(s2)}.</li>
	 * </ul>
	 *
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return a negative number, positive number, or zero per lexicographic rules
	 */
	public static int compare(String s1, String s2) {
		if (s1 == s2) return 0;
		if (s1 == null && s2 == null) return 0;
		if (s1 == null) return -1;
		if (s2 == null) return 1;
		return s1.compareTo(s2);
	}

	/**
	 * Searches for the first occurrence of a substring within a string beginning at
	 * a specified index.
	 * <p>
	 * This method is null-safe:
	 * <ul>
	 * <li>If {@code value} is {@code null}, {@code -1} is returned</li>
	 * <li>If {@code searchValue} is {@code null}, {@code -1} is returned</li>
	 * </ul>
	 *
	 * <p>
	 * If {@code startPos} is negative, it is treated as {@code 0}. If
	 * {@code startPos} is beyond the end of {@code value}, then {@code -1} is
	 * returned immediately.
	 * </p>
	 *
	 * <p>
	 * When {@code bIgnoreCase} is {@code true}, case-insensitive matching is
	 * performed using {@link Locale#ROOT} to avoid locale-influenced rules (e.g.,
	 * Turkish dotted-I).
	 * </p>
	 *
	 * @param value       the string to search within
	 * @param searchValue the substring to find
	 * @param startPos    zero-based starting index for the search; negative values
	 *                    treated as {@code 0}
	 * @param bIgnoreCase if {@code true}, performs case-insensitive comparison
	 *                    using {@code Locale.ROOT}
	 * @return the index of the first match at or after {@code startPos}, or
	 *         {@code -1} if not found
	 */
	public static int indexOf(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		if (value == null || searchValue == null) {
			return -1;
		}
		if (startPos >= value.length()) {
			return -1;
		}
		if (startPos < 0) {
			startPos = 0;
		}
		if (bIgnoreCase) {
			return value.toLowerCase(Locale.ROOT).indexOf(searchValue.toLowerCase(Locale.ROOT), startPos);
		}
		return value.indexOf(searchValue, startPos);
	}

	/**
	 * Delegates to {@link #indexOf(String, String, int, boolean)} beginning at
	 * index {@code 0} with case-sensitive matching.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to locate
	 * @return the index of the first match, or {@code -1} if not found
	 */
	public static int indexOf(String value, String searchValue) {
		return indexOf(value, searchValue, 0, false);
	}

	/**
	 * Delegates to {@link #indexOf(String, String, int, boolean)} with
	 * case-sensitive matching.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to locate
	 * @param startPos    the index to begin scanning
	 * @return the index of the first match at or after {@code startPos}, or {@code -1}
	 */
	public static int indexOf(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false);
	}

	/**
	 * Delegates to {@link #indexOf(String, String, int, boolean)} beginning at
	 * index {@code 0} with optional case-insensitivity.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to locate
	 * @param bIgnoreCase whether to ignore case
	 * @return the index of the first occurrence, or {@code -1}
	 */
	public static int indexOf(String value, String searchValue, boolean bIgnoreCase) {
		return indexOf(value, searchValue, 0, bIgnoreCase);
	}

	/**
	 * Returns the last occurrence of {@code searchValue} within {@code value}
	 * using case-sensitive matching.
	 * <p>
	 * Delegates to {@link #lastIndexOf(String, String, boolean)} with
	 * {@code bIgnoreCase} set to {@code false}.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to locate
	 * @return the index of the last occurrence, or {@code -1} if not found
	 */
	public static int lastIndexOf(String value, String searchValue) {
		return lastIndexOf(value, searchValue, false);
	}

	/**
	 * Returns the last occurrence of {@code searchValue} within {@code value}
	 * with optional case-insensitive matching.
	 * <ul>
	 *   <li>If either string is null, returns {@code -1}.</li>
	 *   <li>Case-insensitive matching lowercases both strings using {@link Locale#ROOT}.</li>
	 * </ul>
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to locate
	 * @param bIgnoreCase whether to ignore case
	 * @return the last matching index, or {@code -1}
	 */
	public static int lastIndexOf(String value, String searchValue, boolean bIgnoreCase) {
		if (value == null || searchValue == null) {
			return -1;
		}
		if (bIgnoreCase) {
			return value.toLowerCase(Locale.ROOT).lastIndexOf(searchValue.toLowerCase(Locale.ROOT));
		}
		return value.lastIndexOf(searchValue);
	}

	/**
	 * Determines whether {@code searchValue} occurs within {@code value}
	 * at or after the specified starting position.
	 * <p>
	 * Delegates to {@link #indexOf(String, String, int, boolean)} and checks
	 * for a non-negative result.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to find
	 * @param startPos    the starting index (negative values treated as zero)
	 * @param bIgnoreCase whether to ignore case
	 * @return {@code true} if the substring occurs; otherwise {@code false}
	 */
	public static boolean contains(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return indexOf(value, searchValue, startPos, bIgnoreCase) >= 0;
	}

	/**
	 * Determines whether {@code searchValue} occurs within {@code value}
	 * at or after the given starting position using case-sensitive matching.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to find
	 * @param startPos    the starting index (negative values treated as zero)
	 * @return {@code true} if found; otherwise {@code false}
	 */
	public static boolean contains(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false) >= 0;
	}

	/**
	 * Determines whether {@code searchValue} occurs anywhere in {@code value}
	 * using case-sensitive matching.
	 * <p>
	 * Returns {@code false} if either argument is null.
	 *
	 * @param value       the text to search
	 * @param searchValue the substring to find
	 * @return {@code true} if the substring exists; otherwise {@code false}
	 */
	public static boolean contains(String value, String searchValue) {
		if (value == null || searchValue == null)
			return false;
		return indexOf(value, searchValue, 0, false) >= 0;
	}

	/**
	 * Checks whether {@code value} begins with {@code searchValue}
	 * using case-sensitive matching.
	 * <p>
	 * Returns {@code false} if either argument is null.
	 *
	 * @param value       the text to examine
	 * @param searchValue the expected prefix
	 * @return {@code true} if {@code value} starts with {@code searchValue}
	 */
	public static boolean startsWith(String value, String searchValue) {
		return startsWith(value, searchValue, false);
	}

	/**
	 * Checks whether {@code value} begins with {@code searchValue},
	 * allowing optional case-insensitive comparison.
	 * <ul>
	 *   <li>Returns {@code false} if either argument is null.</li>
	 *   <li>Case-insensitive matching uses {@link String#regionMatches(boolean, int, String, int, int)}.</li>
	 * </ul>
	 *
	 * @param value       the text to examine
	 * @param searchValue the expected prefix
	 * @param bIgnoreCase whether to ignore case
	 * @return {@code true} if {@code value} starts with the given prefix
	 */
	public static boolean startsWith(String value, String searchValue, boolean bIgnoreCase) {
		if (value == null || searchValue == null) {
		    return false;
		}
		if (bIgnoreCase)
			return value.regionMatches(true, 0, searchValue, 0, searchValue.length());
		return value.startsWith(searchValue);
	}

	/**
	 * Checks whether {@code value} ends with {@code searchValue}
	 * using case-sensitive comparison.
	 *
	 * @param value       the text to examine
	 * @param searchValue the expected suffix
	 * @return {@code true} if {@code value} ends with {@code searchValue}
	 */
	public static boolean endsWith(String value, String searchValue) {
		return endsWith(value, searchValue, false);
	}

	/**
	 * Checks whether {@code value} ends with {@code searchValue},
	 * allowing optional case-insensitive comparison.
	 * <ul>
	 *   <li>Returns {@code false} if either argument is null.</li>
	 *   <li>Case-insensitive matching lowercases both strings using {@link Locale#ROOT}.</li>
	 * </ul>
	 *
	 * @param value       the text to examine
	 * @param searchValue the expected suffix
	 * @param bIgnoreCase whether to ignore case
	 * @return {@code true} if the suffix matches
	 */
	public static boolean endsWith(String value, String searchValue, boolean bIgnoreCase) {
		if (value == null) {
			return false;
		}
		if (searchValue == null) {
			return false;
		}

		if (bIgnoreCase) {
			return value.toLowerCase(Locale.ROOT).endsWith(searchValue.toLowerCase(Locale.ROOT));
		}
		return value.endsWith(searchValue);
	}

	/**
	 * Ensures that {@code value} ends with {@code searchValue}. If the suffix
	 * is already present, the original value is returned. If {@code value} is
	 * null, the suffix is returned.
	 *
	 * @param value       the base string
	 * @param searchValue the required suffix
	 * @return a string ending with the specified suffix
	 */
	public static String appendIfMissing(String value, String searchValue) {
		return appendIfMissing(value, searchValue, false);
	}

	/**
	 * Ensures that {@code value} ends with {@code searchValue}, allowing
	 * optional case-insensitive matching.
	 * <ul>
	 *   <li>If {@code searchValue} is null, {@code value} is returned unchanged.</li>
	 *   <li>If {@code value} is null, returns {@code searchValue}.</li>
	 *   <li>If the suffix is missing, appends it.</li>
	 * </ul>
	 *
	 * @param value       the base text
	 * @param searchValue the suffix to enforce
	 * @param bIgnoreCase whether suffix comparison ignores case
	 * @return a string that ends with the suffix
	 */
	public static String appendIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		if (searchValue == null) {
			return value;
		}
		if (!endsWith(value, searchValue, bIgnoreCase)) {
			if (value == null) {
				return searchValue;
			}
			return value + searchValue;
		}
		return value;
	}

	/**
	 * Ensures that {@code value} begins with {@code searchValue} using
	 * case-sensitive matching.
	 * <ul>
	 *   <li>If {@code searchValue} is null, returns {@code value}.</li>
	 *   <li>If {@code value} is null, returns {@code searchValue}.</li>
	 *   <li>If the prefix is missing, prepends it.</li>
	 * </ul>
	 *
	 * @param value       the base string
	 * @param searchValue the required prefix
	 * @return a string beginning with the specified prefix
	 */
	public static String prefixIfMissing(String value, String searchValue) {
		if (searchValue == null)
			return value;
		if (value == null)
			return searchValue;
		if (startsWith(value, searchValue))
			return value;
		return searchValue + value;
	}

	/**
	 * Ensures that {@code value} begins with {@code searchValue}, allowing
	 * optional case-insensitive matching.
	 * <ul>
	 *   <li>If {@code searchValue} is null, returns {@code value}.</li>
	 *   <li>If {@code value} is null, returns {@code searchValue}.</li>
	 *   <li>If the prefix is missing, prepends it.</li>
	 * </ul>
	 *
	 * @param value       the base string
	 * @param searchValue the required prefix
	 * @param bIgnoreCase whether prefix comparison ignores case
	 * @return a string that begins with the prefix
	 */
	public static String prefixIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		if (searchValue == null)
			return value;
		if (value == null)
			return searchValue;
		if (startsWith(value, searchValue, bIgnoreCase))
			return value;
		return searchValue + value;
	}

}
