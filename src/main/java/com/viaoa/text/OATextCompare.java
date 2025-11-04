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
	 *
	 * @param s           first string
	 * @param s2          second string
	 * @param bIgnoreCase if {@code true}, performs case-insensitive comparison
	 * @return true if values are equal per rules
	 */
	public static boolean isEqual(String s, String s2, boolean bIgnoreCase) {
		return isEqual(s, s2, bIgnoreCase, false);
	}

	/** @see #isEqual(String, String, boolean, boolean) */
	public static boolean isEqual(String s, String s2) {
		return isEqual(s, s2, false, false);
	}

	/**
	 * Convenience for {@code isEqual(s, s2, true, false)}.
	 */
	public static boolean isEqualIgnoreCase(String s, String s2) {
		return isEqual(s, s2, true, false);
	}

	/**
	 * Treats {@code null} and empty-string as equal.
	 */
	public static boolean isEqualNullEqualsBlank(String s, String s2) {
		return isEqual(s, s2, false, true);
	}

	/**
	 * Full control equality check.
	 *
	 * @param s                first string
	 * @param s2               second string
	 * @param bIgnoreCase      if true, ignores letter case (Locale.ROOT)
	 * @param bNullEqualsBlank if true, null and "" are equal
	 * @return true if values satisfy the rules
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

	/** Alias of {@link #isEqual(String, String, boolean, boolean)} */
	public static boolean equals(String s1, String s2) {
		return isEqual(s1, s2, false, false);
	}

	/** Alias of {@link #isEqual(String, String, boolean, boolean)} */
	public static boolean equals(String s1, String s2, boolean bIgnoreCase) {
		return isEqual(s1, s2, bIgnoreCase, false);
	}

	/**
	 * @return logical negation of
	 *         {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean isNotEqual(String s, String s2) {
		return !isEqual(s, s2, false, false);
	}

	/**
	 * @return logical negation of
	 *         {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase) {
		return !isEqual(s, s2, bIgnoreCase, false);
	}

	/**
	 * @return logical negation of
	 *         {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
		return !isEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
	}

	/**
	 * @return logical negation of, where null String is treated as a empty/blank
	 *         String. {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean isNotEqualNullEqualsBlank(String s, String s2) {
		return !isEqual(s, s2, false, true);
	}

	/**
	 * @return logical negation of
	 *         {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean notEquals(String s1, String s2) {
		return !isEqual(s1, s2, false, false);
	}

	/**
	 * @return logical negation of
	 *         {@link #isEqual(String, String, boolean, boolean)}
	 */
	public static boolean notEquals(String s1, String s2, boolean bIgnoreCase) {
		return !isEqual(s1, s2, bIgnoreCase, false);
	}

	/**
	 * Wildcard matching using {@link OACompare#isLike(String, String)} rules.
	 * <p>
	 * Supports SQL-like patterns:
	 * <ul>
	 * <li>{@code %} = match any sequence</li>
	 * <li>{@code _} = match a single character</li>
	 * </ul>
	 */
	public static boolean isLike(String s, String s2) {
		return OACompare.isLike(s, s2);
	}

	/**
	 * Null-safe lexicographic compare following {@link String#compareTo(String)},
	 * with defined ordering for nulls.
	 *
	 * @return negative if s1&lt;s2, positive if s1&gt;s2, zero if equal
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

	/** @return index of first occurrence, or -1 */
	public static int indexOf(String value, String searchValue) {
		return indexOf(value, searchValue, 0, false);
	}

	/**
	 * @param startPos    zero-based index to begin scanning (negative treated as 0)
	 * @param bIgnoreCase case-insensitive if true, using Locale.ROOT
	 * @return index of first occurrence, or -1
	 */
	public static int indexOf(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false);
	}

	/**
	 * @param bIgnoreCase case-insensitive if true, using Locale.ROOT
	 * @return index of first occurrence, or -1
	 */
	public static int indexOf(String value, String searchValue, boolean bIgnoreCase) {
		return indexOf(value, searchValue, 0, bIgnoreCase);
	}

	/** @return last occurrence or -1 */
	public static int lastIndexOf(String value, String searchValue) {
		return lastIndexOf(value, searchValue, false);
	}

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
	 * Case-aware search starting from a position.
	 *
	 * @param bIgnoreCase if true, compare using Locale.ROOT lowercase
	 */
	public static boolean contains(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return indexOf(value, searchValue, startPos, bIgnoreCase) >= 0;
	}

	/** @return true if substring occurs */
	public static boolean contains(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false) >= 0;
	}

	/** @return true if substring occurs */
	public static boolean contains(String value, String searchValue) {
		if (value == null || searchValue == null)
			return false;
		return indexOf(value, searchValue, 0, false) >= 0;
	}

	/**
	 * Checks if value begins with searchValue. Returns false if value or searchValue is null.
	 */
	public static boolean startsWith(String value, String searchValue) {
		return startsWith(value, searchValue, false);
	}

	/**
	 * Checks if value begins with searchValue. Returns false if value or searchValue is null.
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
	 * Checks if value ends with searchValue.
	 */
	public static boolean endsWith(String value, String searchValue) {
		return endsWith(value, searchValue, false);
	}

	/**
	 * Checks if value ends with searchValue.
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
	 * Ensures suffix exists if missing.
	 */
	public static String appendIfMissing(String value, String searchValue) {
		return appendIfMissing(value, searchValue, false);
	}

	/**
	 * Ensures suffix exists if missing.
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
	 * Ensures prefix exists if missing.
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
	 * Ensures prefix exists if missing.
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
