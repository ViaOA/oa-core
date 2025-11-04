package com.viaoa.text;


/**
 * Character category checks
 */
public class OATextChars {

	
//qqqqqqqqqqqq isAlpha/isNumeric/isAlphanumeric   — will add later	
	
	/**
	 * Returns true if the String contains at least one digit [0-9].
	 *
	 * @param word String to test
	 * @return true if any character in word is a digit, otherwise false.
	 */
	public static boolean hasDigits(String word) {
	    if (word == null) return false;
	    for (char ch : word.toCharArray()) {
	        if (Character.isDigit(ch)) return true;
	    }
	    return false;
	}	

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
	 * Example: GSMRServer -&gt; gsmrServer
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

	public static String upper(String value) {
		if (value == null) {
			return null;
		}
		return value.toUpperCase();
	}

	public static String lower(String value) {
		if (value == null) {
			return null;
		}
		return value.toLowerCase();
	}
	
	
}
