package com.viaoa.text;

import com.viaoa.util.OAConverter;

/**
 * Text sanitization helpers that ensure safe use of String values throughout
 * OA.
 * <p>
 * Goals:
 * <ul>
 * <li>Never return {@code null} for String values</li>
 * <li>Optional trimming rules for whitespace</li>
 * <li>Delegation to {@link OAConverter} for structured types</li>
 * <li>Unified handling of {@code null}, empty, and blank strings</li>
 * </ul>
 *
 * <p>
 * This class does not enforce formatting rules, only <em>safe cleanup</em> so
 * text can be reliably displayed, logged, or persisted.
 * </p>
 *
 */
public class OATextSanitize {

	/**
	 * Returns {@code value} if non-null, otherwise returns an empty string.
	 *
	 * @param value the input string (may be null)
	 * @return {@code value} or {@code ""} if value is null
	 */
	public static String defaultString(String str) {
		return defaultString(str, "");
	}

	/**
	 * Returns {@code value} if non-null, otherwise the provided
	 * {@code defaultValue}.
	 *
	 * @param value        the input string (may be null)
	 * @param defaultValue returned when {@code value} is null
	 * @return non-null string value
	 */
	public static String defaultString(String str, String strIfNull) {
		if (str == null) {
			return strIfNull;
		}
		return str;
	}

	/**
	 * Returns a non-null string representation, substituting {@code ""} if the
	 * input is null.
	 *
	 * @param s the input string
	 * @return a non-null string
	 */
	public static String notNull(String str, String strIfNull) {
		if (str == null) {
			return strIfNull;
		}
		return str;
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String notNull(String str) {
		return notNull(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String toNotNullString(String str) {
		return defaultString(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String nonNull(String str) {
		return defaultString(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String nonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String toNonNull(String str) {
		return defaultString(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String toNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String getNonNull(String str) {
		return defaultString(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String getNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String convertToNonNull(String str) {
		return defaultString(str, "");
	}

	/**
	 * Alias of {@link #defaultString(String)}.
	 */
	public static String convertToNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	/**
	 * Converts an Object to a non-null string using {@link Object#toString()}, or
	 * empty string if input is null.
	 *
	 * @param obj arbitrary object
	 * @return non-null string
	 */
	public static String toString(Object obj) {
		if (obj == null) {
			return "";
		}
		if (obj instanceof String)
			return (String) obj;
		return OAConverter.toString(obj);
	}

	/**
	 * Determines if a value is null, empty, or (optionally) whitespace-only.
	 * <p>
	 * If {@code value} is not a String,
	 * {@link OAConverter#isEmpty(Object, boolean)} is used.
	 *
	 * @param obj   the input value
	 * @param bTrim if true, whitespace is ignored
	 * @return true if value is considered empty
	 */
	public static boolean isEmpty(Object obj, boolean bTrim) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			if (bTrim) {
				if (((String) obj).trim().isEmpty()) {
					return true;
				}
			} else {
				if (((String) obj).isEmpty()) {
					return true;
				}
			}
		} else {
			return OAConverter.isEmpty(obj, bTrim);
		}
		return false;
	}

	/**
	 * {@link #isEmpty(Object)}.
	 */
	public static boolean isEmpty(Object obj) {
		return isEmpty(obj, false);
	}

	/**
	 * Logical negation of {@link #isEmpty(Object)}.
	 *
	 * @param obj input
	 * @return true if not empty
	 */
	public static boolean notEmpty(Object obj) {
		return !isEmpty(obj, false);
	}

	/**
	 * Logical negation of {@link #isEmpty(Object)}.
	 *
	 * @param obj input
	 * @return true if not empty
	 */
	public static boolean isNotEmpty(Object obj) {
		return !isEmpty(obj, false);
	}

	/**
	 * Logical negation of {@link #isEmpty(Object)}.
	 *
	 * @param obj input
	 * @return true if not empty
	 */
	public static boolean isNotNullAndNotEmpty(Object obj) {
		return !isEmpty(obj, false);
	}
}
