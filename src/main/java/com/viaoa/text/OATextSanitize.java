package com.viaoa.text;

import com.viaoa.util.OAConverter;

public class OATextSanitize {

/*	
	
if it involves null/empty cleanup

	
toSafeString, toValidXml, null safety	
	
*/	
	/**
	 * Convert to a string, if null then it returns ""
	 */
	public static String defaultString(String str) {
		return defaultString(str, "");
	}

	/**
	 * Convert to a string, if null then return strIfNull
	 */
	public static String defaultString(String str, String strIfNull) {
		if (str == null) {
			return strIfNull;
		}
		return str;
	}

	public static String notNull(String str, String strIfNull) {
		if (str == null) {
			return strIfNull;
		}
		return str;
	}
	public static String notNull(String str) {
		return notNull(str, "");
	}
	
	
	/**
	 * Convert to a string, if null then it returns ""
	 */
	public static String toString(String str) {
		return defaultString(str, "");
	}

	public static String nonNull(String str) {
		return defaultString(str, "");
	}

	public static String nonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	public static String toNonNull(String str) {
		return defaultString(str, "");
	}

	public static String toNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	public static String getNonNull(String str) {
		return defaultString(str, "");
	}

	public static String getNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	public static String convertToNonNull(String str) {
		return defaultString(str, "");
	}

	public static String convertToNonNull(String str, String defaultValue) {
		return defaultString(str, defaultValue);
	}

	
    /**
     * Null-safe toString for OA objects.
     * Converts null → "" (empty string).
     * Delegates to OAConverter for formatting.
     */
	public static String toString(Object obj) {
		if (obj == null) {
			return "";
		}
		if (obj instanceof String) return (String) obj; 
		return OAConverter.toString(obj);
	}

	
	public static boolean isEmpty(Object obj) {
		return isEmpty(obj, false);
	}

	public static boolean isEmpty(Object obj, boolean bTrim) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			if (bTrim) {
				if (((String) obj).trim().length() == 0) {
					return true;
				}
			} else {
				if (((String) obj).length() == 0) {
					return true;
				}
			}
		} 
		else {
			return OAConverter.isEmpty(obj, bTrim);
		}
		return false;
	}
	
	
	public static boolean notEmpty(Object obj) {
		return !isEmpty(obj, false);
	}
	public static boolean isNotEmpty(Object obj) {
		return !isEmpty(obj, false);
	}
	public static boolean isNotNullAndNotEmpty(Object obj) {
		return !isEmpty(obj, false);
	}

}
