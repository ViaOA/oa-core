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

/*qqqqqqqqqqqqq
 CODEX
 
 
 22. Medium - OATextSoundex.soundex
     Scenario: leading non-letters are preserved instead of ignored.
     Example: OATextSoundex.soundex("1Smith") returns 1253.
     Impact: phonetic matching for names with punctuation/digits prefixes produces invalid Soundex keys.
 
 
 */


/**
 * Utility for generating U.S. Census Soundex codes to support matching of
 * phonetically similar names (e.g., "Vincent" → "V523").
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Always returns a 4-character code using A–Z and digits</li>
 *   <li>Padding with '0' if insufficient consonants found</li>
 *   <li>Case-insensitive (normalized to uppercase)</li>
 *   <li>Non-alphabetic characters ignored</li>
 *   <li>Duplicate adjacent codes are collapsed</li>
 * </ul>
 *
 * <p><b>Null/empty input returns "0000"</b>
 *
 * <p>Examples:
 * <pre>{@code
 * soundex("Vincent") → "V523"
 * soundex("Via")     → "V000"
 * soundex(null)      → "0000"
 * }</pre>
 *
 */
public class OATextSoundex {
	/**
	 * Soundex is used for creating a code that is used to find similar words.
	 * <p>
	 * 20100417 now using more advanced algorithm see: http://www.archives.gov/genealogy/census/soundex.html
	 * <p>
	 * This code is set to 4 char value - padded with char '0'.<br>
	 * If word == NULL then sndx = "0000"
	 *
	 * <pre>
	 use first letter
	 exclude "AEHIOUWY" or any char that is not a letter
	 exclude all duplicates
	 '0' pad to 4 chars             Note: this will also use digits

	 From: "BFPVCGJKQSZXDTLMNR"
	 To  : "111122222222334556"

	 EXAMPLE:
	 soundex(sndx,"Vincent")  sndx = "V523"
	 soundex(sndx,"Via")      sndx = "V000"
	 * </pre>
	 *
	 * @param word String to create a soundex code for.
	 * @return new String that is soundex code for word. If word is null,then "0000" is returned.
	 * 
	 * <p>Standards reference:
	 * <a href="http://www.archives.gov/genealogy/census/soundex.html">
	 * U.S. National Archives Soundex Rules</a>
	 * 
	 */
	
	
	/**
	 * Generates a 4-character U.S. Census Soundex code for the supplied word.
	 * <p>
	 * The algorithm:
	 * <ul>
	 *   <li>Returns {@code "0000"} for null or blank input.</li>
	 *   <li>Normalizes input to uppercase.</li>
	 *   <li>Preserves the first letter verbatim.</li>
	 *   <li>Converts subsequent letters to Soundex digits, ignoring vowels and
	 *       non-letters.</li>
	 *   <li>Skips duplicate adjacent codes.</li>
	 *   <li>Pads the result with {@code '0'} as needed to reach four characters.</li>
	 * </ul>
	 *
	 * @param word the input text to encode; may be null
	 * @return a 4-character Soundex code, or {@code "0000"} for null/blank input
	 */
	public static String soundex(String word) {
	    if (word == null) return "0000";
	    word = word.trim();
	    if (word.isEmpty()) return "0000";

	    word = word.toUpperCase(Locale.ROOT);

	    char[] result = { word.charAt(0), '0', '0', '0' };
	    char lastCode = _getSoundexCode(result[0]);
	    int idx = 1;

	    for (int i = 1; i < word.length() && idx < 4; i++) {
	        char code = _getSoundexCode(word.charAt(i));
	        if (code <= 1) {  // 0: skip, 1: vowel/hw -> skip but reset duplicate blocker
	            lastCode = 0;
	            continue;
	        }
	        if (code != lastCode) {
	            result[idx++] = code;
	        }
	        lastCode = code;
	    }
	    return new String(result);
	}

	/**
	 * Returns the Soundex classification code for a given uppercase character.
	 * <p>
	 * Mapping:
	 * <ul>
	 *   <li>B, F, P, V → '1'</li>
	 *   <li>C, G, J, K, Q, S, X, Z → '2'</li>
	 *   <li>D, T → '3'</li>
	 *   <li>L → '4'</li>
	 *   <li>M, N → '5'</li>
	 *   <li>R → '6'</li>
	 *   <li>A, E, I, O, U, Y, H, W → 1 (vowel/ignored class)</li>
	 *   <li>All others → 0 (non-letter or skipped)</li>
	 * </ul>
	 *
	 * @param ch uppercase character to classify
	 * @return Soundex code character, or 1/0 indicating skip rules
	 */
	private static char _getSoundexCode(char ch) {
	    switch (ch) {
	        case 'B': case 'F': case 'P': case 'V': return '1';
	        case 'C': case 'G': case 'J': case 'K': case 'Q': case 'S': case 'X': case 'Z': return '2';
	        case 'D': case 'T': return '3';
	        case 'L': return '4';
	        case 'M': case 'N': return '5';
	        case 'R': return '6';
	        case 'A': case 'E': case 'I': case 'O': case 'U': case 'Y': case 'H': case 'W': return 1;
	        default: return 0;
	    }
	}

}
