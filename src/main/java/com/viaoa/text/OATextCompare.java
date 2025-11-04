package com.viaoa.text;

import com.viaoa.util.OACompare;

public class OATextCompare {

	public static boolean isEqual(String s, String s2, boolean bIgnoreCase) {
	    return isEqual(s, s2, bIgnoreCase, false);
	}

	public static boolean isEqual(String s, String s2) {
		return isEqual(s, s2, false, false);
	}

    public static boolean isEqualIgnoreCase(String s, String s2) {
        return isEqual(s, s2, true, false);
    }
	
    public static boolean isEqualNullEqualsBlank(String s, String s2) {
        return isEqual(s, s2, false, true);
    }
	
    public static boolean isEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
        if (s == s2) {
            return true;
        }
        if (s == null || s2 == null) {
            if (bNullEqualsBlank) {
                if (OATextSanitize.isEmpty(s) && OATextSanitize.isEmpty(s2)) return true;
            }
            return false;
        }
        if (bIgnoreCase) {
            return s.equalsIgnoreCase(s2);
        }
        return s.equals(s2);
    }
	
    public static boolean isNotEqual(String s, String s2) {
        return !isEqual(s, s2, false, false);

    }
    public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase) {
        return !isEqual(s, s2, bIgnoreCase, false);
    }
    public static boolean isNotEqual(String s, String s2, boolean bIgnoreCase, boolean bNullEqualsBlank) {
        return !isEqual(s, s2, bIgnoreCase, bNullEqualsBlank);
    }
    public static boolean isNotEqualNullEqualsBlank(String s, String s2) {
        return !isEqual(s, s2, false, true);
    }
	
	public static boolean equals(String s1, String s2) {
		return isEqual(s1, s2, false, false);
	}
	public static boolean equals(String s1, String s2, boolean bIgnoreCase) {
		return isEqual(s1, s2, bIgnoreCase, false);
	}

	public static boolean notEquals(String s1, String s2) {
		return !isEqual(s1, s2, false, false);
	}
	public static boolean notEquals(String s1, String s2, boolean bIgnoreCase) {
		return !isEqual(s1, s2, bIgnoreCase, false);
	}
	
	public static boolean isLike(String s, String s2) {
		return OACompare.isLike(s, s2);
	}
	
	public static int compare(String s1, String s2) {
		if (s1 == s2) {
			return 0;
		}
		if (s1 == null) {
			return -1;
		}
		return s1.compareTo(s2);
	}
	
	
	public static int indexOf(String value, String searchValue) {
		return indexOf(value, searchValue, 0, false);
	}
	
	public static int indexOf(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false);
	}

	public static int indexOf(String value, String searchValue, boolean bIgnoreCase) {
		return indexOf(value, searchValue, 0, bIgnoreCase);
	}
	
	public static boolean contains(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		return indexOf(value, searchValue, startPos, bIgnoreCase) >= 0;
	}
	
	public static boolean contains(String value, String searchValue, int startPos) {
		return indexOf(value, searchValue, startPos, false) >= 0;
	}

	public static boolean contains(String value, String searchValue) {
		return indexOf(value, searchValue, 0, false) >= 0;
	}

	
	
	public static int indexOf(String value, String searchValue, int startPos, boolean bIgnoreCase) {
		if (value == null || searchValue == null) {
			return -1;
		}
		if (startPos >= searchValue.length()) {
			return -1;
		}
		if (startPos < 0) {
			startPos = 0;
		}
		if (bIgnoreCase) {
			return value.toLowerCase().indexOf(searchValue.toLowerCase(), startPos);
		}
		return value.indexOf(searchValue, startPos);
	}
	
	public static int lastIndexOf(String value, String searchValue) {
		return lastIndexOf(value, searchValue, false);
	}


	public static int lastIndexOf(String value, String searchValue, boolean bIgnoreCase) {
		if (value == null || searchValue == null) {
			return -1;
		}
		if (bIgnoreCase) {
			return value.toLowerCase().lastIndexOf(searchValue.toLowerCase());
		}
		return value.lastIndexOf(searchValue);
	}

	
	public static boolean startsWith(String value, String searchValue) {
		return startsWith(value, searchValue, false);
	}
	
	public static boolean startsWith(String value, String searchValue, boolean bIgnoreCase) {
		int x = indexOf(value, searchValue, 0, bIgnoreCase);
		return x == 0;
	}

	public static boolean endsWith(String value, String searchValue) {
		return endsWith(value, searchValue, false);
	}
	
	public static boolean endsWith(String value, String searchValue, boolean bIgnoreCase) {
		if (value == null) {
			return false;
		}
		if (searchValue == null) {
			return false;
		}
		return value.endsWith(searchValue);
	}
	
	
	public static String appendIfMissing(String value, String searchValue) {
		return appendIfMissing(value, searchValue, false);
	}

	public static String appendIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		if (searchValue == null) {
			return value;
		}
		if (!OATextCompare.endsWith(value, searchValue, bIgnoreCase)) {
			if (value == null) {
				return searchValue;
			}
			return value + searchValue;
		}
		return value;
	}


	public static String prefixIfMissing(String value, String searchValue) {
		if (searchValue == null) {
			return value;
		}
		if (!OATextCompare.startsWith(value, searchValue)) {
			if (value == null) {
				return searchValue;
			}
			return searchValue + value;
		}
		return value;
	}

	public static String prefixIfMissing(String value, String searchValue, boolean bIgnoreCase) {
		if (searchValue == null) {
			return value;
		}
		if (!OATextCompare.startsWith(value, searchValue, bIgnoreCase)) {
			if (value == null) {
				return searchValue;
			}
			return searchValue + value;
		}
		return value;
	}

	
}
