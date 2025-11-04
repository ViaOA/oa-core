package com.viaoa.text;

/**
 * Grammar utilities for converting Java identifiers and simple English words into
 * human-readable display names and forms. This includes:
 *
 * <ul>
 *   <li>Converting camelCase / mixedCase to display names with spaces and title casing
 *   <li>Converting plural ↔ singular forms using simple English suffix rules
 *   <li>Creating possessive words ("car" → "car's", "class" → "class'")
 *   <li>Title-casing of sentences and identifiers
 *   <li>Producing short names based on uppercase characters and consonants
 * </ul>
 *
 * <p>
 * This class operates on ASCII letter rules (A-Z/a-z) and includes simple fall-backs for
 * ambiguous English grammar situations. Unicode grammar and language-specific rules
 * will be handled in a future phase.
 *
 * <p><b>Design Notes</b>
 * <ul>
 *   <li>Null inputs return a non-null safe default (usually empty string)
 *   <li>Case of appended suffixes is preserved (e.g., "Box" → "Boxes")
 *   <li>Behavior is backwards-compatible with existing OA applications
 * </ul>
 *
 */
public class OATextGrammar {


	/**
	 * Used to convert a String that uses CamelCase notation to a titled, space separated String. The first char and all letter chars
	 * following non-letter characters will be converted to uppercase. Words will be separated using space character.
	 * <p>
	 * Example: "yourNameTest" converts to "Your Name Test" <br>
	 * Example: "USAmerica" converts to "US America" <br>
	 * Example: "v.via" converts to "V.Via"
	 *
	 * @param value String to convert
	 * @return new String that is titled case, with spaces to separate words. If value is null, then a blank "" is returned.
	 */
	public static String getDisplayName(String value) {
		if (value == null) {
			return "";
		}
		int x = value.length();

		StringBuilder sb = new StringBuilder(x + 3);

		char c;
		char cLast = 0;
		char cNext = 0;

		for (int i = 0; i < x; i++) {
			c = (cNext > 0) ? cNext : value.charAt(i);
			if (i + 1 < x) {
				cNext = value.charAt(i + 1);
			} else {
				cNext = 0;
			}

			if (i == 0) {
				if (Character.isLowerCase(c)) {
					c = Character.toUpperCase(c);
				}
			} else if (c == '_') {
				c = ' ';
			} else if (cLast == '_') {
				if (Character.isLowerCase(c)) {
					c = Character.toUpperCase(c);
				}
			} else if (Character.isUpperCase(c)) {
				if (!Character.isUpperCase(cLast)) {
					sb.append(" ");
				} else {
					if (cNext > 0 && Character.isLowerCase(cNext)) {
						sb.append(" ");
					}
				}
			}
			sb.append(c);
			cLast = c;
		}
		return new String(sb);
	}

	public static String createDisplayName(String value) {
		return getDisplayName(value);
	}

	public static String convertToDisplayName(String value) {
		return getDisplayName(value);
	}
	

	/**
	 * Converts a String that is plural to singular.<br>
	 * Converts end characters: "hes" to "h", "ses" to "s", "zzes" to "zz", "ies" to "y", "s" to "". This is the reverse method of
	 * makePlural.
	 *
	 * @return new String. If s is null, then a blank "" is returned.
	 * @see #makePlural
	 */
	public static String makeSingular(String str) {
		if (str == null) {
			return "";
		}
		int x = str.length();
		if (x < 2) {
			return str;
		}
		boolean bUpper = Character.isUpperCase(str.charAt(x - 1));

		String test = str.toUpperCase();
		if (test.charAt(x - 1) != 'S') {
			return str;
		}
		char ch = test.charAt(x - 2);
		if (ch == 'A' || ch == 'I' || ch == 'O' || ch == 'U' || ch == 'Y' || ch == 'S') {
			return str;
		}
		if (test.endsWith("HES")) {
			return str.substring(0, x - 2);
		}
		if (test.endsWith("SES")) {
			return str.substring(0, x - 2);
		}
		if (test.endsWith("ZZES")) {
			return str.substring(0, x - 2);
		}
		if (test.endsWith("IES")) {
			return str.substring(0, x - 3) + (bUpper ? "Y" : "y");
		}
		if (test.endsWith("XES")) {
			return str.substring(0, x - 2);
		}
		return str.substring(0, x - 1);
	}
	
	public static String getAorAn(String s) {
		if (s == null || s.length() == 0) {
			return "a";
		}
		char ch = Character.toLowerCase(s.charAt(0));
		if ("aeiou".indexOf(ch) < 0) {
			return "a";
		}
		return "an";
	}

	/**
	 * Converts a String to plural.
	 * <ul>
	 * <li>If str ends in "es" then no change is made.
	 * <li>If str ends in "s" then add "es".
	 * <li>If str ends in "zz" then add "s".
	 * <li>If str ends is an "h", "z", "x" then add "es".
	 * <li>If str ends in a vowel + "y", then add "s".
	 * <li>If str ends in a nonvowel + "y", then convert "y" to "ies".
	 * <li>All others have an "s" added.
	 * </ul>
	 * <p>
	 * Note: case will be matched, whatever characters are appended will match the case of the String. This is the reverse method of
	 * makeSingular.
	 *
	 * @return new plural String. If s is null, then a blank "" is returned.
	 * @see #makeSingular
	 */
	public static String makePlural(String str) {
		if (str == null) {
			return "";
		}
		int x = str.length();
		if (x == 0) {
			return str;
		}
		char ch = str.charAt(x - 1);
		boolean bUpper = Character.isUpperCase(ch);
		ch = Character.toUpperCase(ch);
		char ch2 = 0;
		if (x > 1) {
			ch2 = Character.toUpperCase(str.charAt(x - 2));
		}

		if (ch == 'S') {
			if (ch2 == 'E') {
				return str;
			}
			return str + (bUpper ? "ES" : "es");
		}

		if (ch == 'Z' && ch2 == 'Z') {
			return str + (bUpper ? "S" : "s");
		}

		if (ch2 == 'T' && ch == 'H') {
			return str + 's';
		}
		if (ch == 'H' || ch == 'Z' || ch == 'X') {
			return str + (bUpper ? "ES" : "es");
		}

		if (ch == 'Y') {
			if (ch2 == 'A' || ch2 == 'E' || ch2 == 'I' || ch2 == 'O' || ch2 == 'U') {
				return str + (bUpper ? "S" : "s");
			}
			return str.substring(0, x - 1) + (bUpper ? "IES" : "ies");
		}
		return str + (bUpper ? "S" : "s");
	}

	public static String makePossessive(String str) {
		if (str == null) {
			return "";
		}
		int x = str.length();
		if (x == 0) {
			return str;
		}
		char ch = str.charAt(x - 1);
		boolean bUpper = Character.isUpperCase(ch);

		if (ch == 'S' || ch == 's') {
			return str + "'";
		}

		return str + "'" + (bUpper ? "S" : "s");
	}

	/**
	 * Converts a String to possissive by adding "'s" or "'".
	 */
	public static String getPossessive(String str) {
		if (str == null) {
			return "";
		}
		int x = str.length();
		if (x == 0) {
			return str;
		}
		char ch = str.charAt(x - 1);

		if (ch == 'S' || ch == 's') {
			return str + "'";
		}
		return str + (Character.isUpperCase(ch) ? "'S" : "'s");
	}

	/**
	 * Converts first letter in each word to uppercase.
	 *
	 * @return new String.
	 */
	public static String getTitle(String s) {
		if (s == null) {
			return "";
		}

		String s2 = s.toUpperCase();
		boolean bAllUpper = s2.equals(s);

		int x = s.length();
		if (x == 0) {
			return s;
		}
		boolean b = true;
		StringBuilder sb = new StringBuilder(s.length());
		
		for (int i = 0; i < x; i++) {
			char ch = s.charAt(i);
			if (Character.isLetter(ch)) {
				if (b) {
					ch = Character.toUpperCase(ch);
					b = false;
				} else {
					if (bAllUpper) {
						ch = Character.toLowerCase(ch);
					}
				}
			} else {
				b = true;
			}
			sb.append(ch);
		}
		return sb.toString();
	}
	
	public static String getTitle(String s, String basedOn) {
		if (s == null) {
			return "";
		}

		String s2 = s.toUpperCase();
		boolean bAllUpper = s2.equals(s);

		int x = s.length();
		if (x == 0) {
			return s;
		}
		boolean bConvert = true;
		StringBuilder sb = new StringBuilder(s.length());
		int cnt = 0;
		for (int i = 0; i < x; i++) {
			char ch = s.charAt(i);
			if (Character.isLetter(ch)) {
				if (bConvert) {
					char chHold = ch;
					ch = Character.toUpperCase(ch);
					cnt++;
					if (basedOn != null && i < basedOn.length()) {
						char ch2 = basedOn.charAt(i);
						if (ch != ch2) {
							bConvert = false;
							if (cnt > 1) {
								ch = chHold;
							}
						}
					} else {
						bConvert = false;
					}
				} else {
					if (bAllUpper) {
						ch = Character.toLowerCase(ch);
					}
				}
			} else {
				bConvert = true;
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	public static String getShortName(final String name, final int max) {
		if (OATextSanitize.isEmpty(name)) {
			return "";
		}

		final int x = name.length();
		String shortName = "";

		int cnt = 0;
		for (int i = 0; i < x; i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				cnt++;
			}
		}

		int cnt2 = 0;
		for (int i = 0; i < x && shortName.length() < max; i++) {
			char c = name.charAt(i);
			if (i == 0 || Character.isUpperCase(c)) {
				shortName += Character.toLowerCase(c);
			} else if ("aeiou".indexOf(c) < 0 && cnt2++ < (max - cnt)) {
				shortName += c;
			}
		}
		return shortName;
	}
}
