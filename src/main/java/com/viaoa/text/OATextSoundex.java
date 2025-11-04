package com.viaoa.text;

import java.util.Locale;

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
	 */
	/**
	 * Generates a 4-character Soundex code (U.S. Census standard).
	 * Non-letter characters are ignored.
	 *
	 * If input is null/blank → "0000"
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
