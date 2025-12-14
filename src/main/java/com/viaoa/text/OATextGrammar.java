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
	 * Converts a camelCase or mixed-case identifier into a display name with
	 * spaces and title casing.
	 * <ul>
	 *   <li>First character is uppercased if it is a letter.</li>
	 *   <li>Underscores are converted to spaces; the letter after an underscore is uppercased.</li>
	 *   <li>When an uppercase letter follows a lowercase letter, a space is inserted.</li>
	 *   <li>For runs of uppercase letters, a space is inserted before the last
	 *       uppercase when followed by a lowercase letter (e.g., "USAmerica" → "US America").</li>
	 * </ul>
	 *
	 * @param value the identifier to convert; null returns an empty string
	 * @return a human-readable display name
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

	/**
	 * Convenience method that delegates to {@link #getDisplayName(String)}.
	 *
	 * @param value the identifier to convert; null returns an empty string
	 * @return a human-readable display name
	 */
	public static String createDisplayName(String value) {
		return getDisplayName(value);
	}

	/**
	 * Convenience method that delegates to {@link #getDisplayName(String)}.
	 *
	 * @param value the identifier to convert; null returns an empty string
	 * @return a human-readable display name
	 */
	public static String convertToDisplayName(String value) {
		return getDisplayName(value);
	}
	

	/**
	 * Converts a plural word to its singular form using simple English suffix
	 * rules. Case of the returned text matches the input.
	 * <ul>
	 *   <li>If the word does not end in 's' or is too short, it is returned unchanged.</li>
	 *   <li>Handles suffixes: "hes", "ses", "zzes", "ies", and "xes" using
	 *       specialized removal or substitution rules.</li>
	 *   <li>Words ending in 'ies' become 'y' or 'Y' based on the original case.</li>
	 *   <li>All other words ending in 's' simply have the last 's' removed.</li>
	 * </ul>
	 *
	 * @param str the plural word; null returns an empty string
	 * @return a singular form of the word, or the original if no rule applies
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
	
	/**
	 * Returns the article "a" or "an" for the supplied word based on its first
	 * character.
	 * <ul>
	 *   <li>If the word is null or empty, {@code "a"} is returned.</li>
	 *   <li>If the first character (case-insensitive) is a, e, i, o, or u,
	 *       returns {@code "an"}; otherwise {@code "a"}.</li>
	 * </ul>
	 *
	 * @param s the word to examine
	 * @return "a" or "an" depending on the starting letter
	 */
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
	 * Converts a singular word to its plural form using simple English suffix
	 * rules. Appended characters preserve the original case (upper or lower).
	 * <ul>
	 *   <li>If the word ends in "es" (case-insensitive), it is returned unchanged.</li>
	 *   <li>If it ends in "s", appends "es".</li>
	 *   <li>If it ends in "zz", appends "s".</li>
	 *   <li>If it ends in "th", appends "s".</li>
	 *   <li>If it ends in 'h', 'z', or 'x', appends "es".</li>
	 *   <li>If it ends with vowel + 'y', appends "s".</li>
	 *   <li>If it ends with consonant + 'y', replaces 'y' with "ies".</li>
	 *   <li>All other endings simply append 's'.</li>
	 * </ul>
	 *
	 * @param str the singular word; null returns an empty string
	 * @return the plural form of the word
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

	/**
	 * Creates a possessive form of the supplied word.
	 * <ul>
	 *   <li>If the word ends with 's' or 'S', appends an apostrophe (').</li>
	 *   <li>Otherwise appends an apostrophe followed by 's' or 'S' based on the
	 *       case of the last character.</li>
	 * </ul>
	 *
	 * @param str the base word; null returns an empty string
	 * @return the possessive form, such as "car's" or "class'"
	 */
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
	 * Alternate possessive helper using a slightly different case rule than
	 * {@link #makePossessive(String)}.
	 * <ul>
	 *   <li>If the word ends with 's' or 'S', appends an apostrophe (').</li>
	 *   <li>Otherwise appends "'S" for uppercase endings or "'s" for lowercase.</li>
	 * </ul>
	 *
	 * @param str the base word; null returns an empty string
	 * @return the possessive form of the word
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
	 * Title-cases a string by uppercasing the first letter of each word and
	 * optionally lowercasing the remaining letters.
	 * <ul>
	 *   <li>A "word" starts after any non-letter character.</li>
	 *   <li>If the entire input is uppercase, non-initial letters are converted
	 *       to lowercase to avoid all-caps output.</li>
	 *   <li>Non-letter characters are preserved.</li>
	 * </ul>
	 *
	 * @param s the text to convert; null returns an empty string
	 * @return the title-cased string
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
	
	/**
	 * Title-cases a string but only continues capitalizing leading letters while
	 * they still match the pattern implied by {@code basedOn}.
	 * <ul>
	 *   <li>Behavior is similar to {@link #getTitle(String)} for the initial
	 *       letters.</li>
	 *   <li>For each letter position, the capitalized form is compared against
	 *       the corresponding character in {@code basedOn} when available.</li>
	 *   <li>Once a mismatch is detected, subsequent letters are no longer forced
	 *       to uppercase, except for the all-uppercase-to-lowercase normalization.</li>
	 *   <li>If the input is entirely uppercase, non-initial letters may be
	 *       lowercased.</li>
	 * </ul>
	 *
	 * @param s        the text to convert; null returns an empty string
	 * @param basedOn  reference string used to control how long capitalization is applied
	 * @return the title-cased string with behavior influenced by {@code basedOn}
	 */
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

	/**
	 * Produces a short, lowercased name derived from a longer identifier, using
	 * uppercase letters and consonants to build an abbreviation up to a maximum
	 * length.
	 * <ul>
	 *   <li>If {@code name} is null or empty, returns an empty string.</li>
	 *   <li>First character is always included (lowercased).</li>
	 *   <li>All subsequent uppercase letters are included (lowercased).</li>
	 *   <li>Additional consonants are included until {@code max} is reached,
	 *       with the amount limited based on the number of uppercase letters.</li>
	 * </ul>
	 *
	 * @param name the source name; may be null or empty
	 * @param max  maximum length of the resulting short name
	 * @return a compact short name, or an empty string if {@code name} is blank
	 */
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
