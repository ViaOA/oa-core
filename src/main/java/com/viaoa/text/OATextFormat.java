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

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/**
 * Flexible, string formatting and masking engine. Primarily used for
 * business UI text layout, numeric conversion, and masked entry display.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Field justification: left, right, or center</li>
 *   <li>Width sizing and custom pad characters</li>
 *   <li>Decimal control and rounding for numeric values</li>
 *   <li>Comma separators and currency decoration</li>
 *   <li>Data masks using '#' insertion rules</li>
 *   <li>Optional leading or trailing ellipsis when text exceeds space</li>
 * </ul>
 *
 * <p>If no alignment directive is found, this class can automatically detect
 * number and date formats and delegate to:
 * <ul>
 *   <li>{@link OAConv} for numeric parsing and formatting</li>
 *   <li>{@link OADateTime} for date/time conversion</li>
 * </ul>
 *
 */
public class OATextFormat {

	/**
	 * Regular expression used to detect the presence of date/time formatting
	 * characters within a format string. Enables automatic delegation to
	 * {@link OADateTime} when alignment directives are not present.
	 */
	private static final Pattern DATE_PATTERN = Pattern.compile(".*[yMdHhmsS].*");
	
	/**
	 * Regular expression used to detect numeric-related formatting characters
	 * such as digits, '$', ',', or '#'. Enables automatic numeric conversion
	 * when alignment directives are not present.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern.compile(".*[0-9,$#].*");
	

	
	/**
	 * Used to format/mask Strings using a "Pick like" format/mask String.
	 * <p>
	 * Also supports formats for Date/Times (see OADateTime) and Numbers (see OAConverterNumber)
	 * <p>
	 * <b>Formatting Strings</b>
	 *
	 * <pre>

	 Example:  fmt(str,"12 L2.,$0(MASK)");

	 Format description for "12 L2,$0(MASK)":
	     12 = width - not required.
	          will pad with spaces if pad character is not defined.
	          if width is not included, then length of String is not restricted.
	 ' ' = trailing blanks that will be added to the end of formatted String.
	     L = L, R, or C justified
	     2 = decimal places - can only be ONE digit.  Rounding will be used.
	     . = if value has to be truncated, then "..." will be used.  Only used with "L" or "R" justified.
	     , = if you want commas to seperate numbers
	     $ = dollar sign, only if 'R' justified  puts it in first char
	     0 = any pad character - default space. Dont put this 1 after L/R, since
	         that position is used for the amount of decimal places.
	     Mask = must be in "()".  Use # character to have actual characters inserted,
	            all other characters in mask will be inserted.

	 Examples:

	 fmt("1234.5", "R4,")
	     "R4," = align right, 4 decimal places with comma seperators.
	     output: "1,234.5000"

	 fmt("123.5", "R00")
	     "R00" = align right, 0 decimal places (causes rounding), pad with '0' character
	     output: "123"

	 fmt("123.5", "8R00")
	     "8R00" = 8 width to fill,
	     output: "00000123"

	 fmt("123.5", "8 R00")
	     "8 R00" = 8 width, append one space, right justified, 0 decimal places, '0' fill
	     output: "00000123 "

	 fmt("1231231234","13  R((###)###-####)")
	     "13  R((###)###-####)" = 13 width, append 2 spaces, right justified, mask to use.
	          Note: the mask must be put into () and use # to denote where to insert the
	          characters within the supplied String.
	     output: "(123)123-1234  "
	 fmt("CustomerName", "8L.")
	      output: "Custo..."
	 fmt("CustomerName", "7R.")
	      output: "...omer"
	 * </pre>
	 */
	public static String fmt(String str, String format) {
		String s = _fmt(str, format);
		return s;
	}

	
	/**
	 * Internal formatting engine implementing alignment, padding, decimal handling,
	 * masking, truncation, comma separators, currency prefixing, and ellipsis rules.
	 * <p>
	 * Behavior includes:
	 * <ul>
	 *   <li>Automatic numeric/date/time detection when no alignment directive exists</li>
	 *   <li>Interpretation of width, justification (L/R/C), decimals, commas, '$', pad chars</li>
	 *   <li>Mask parsing using '#' placeholders for character insertion</li>
	 *   <li>Optional leading/trailing ellipsis when truncation is required</li>
	 *   <li>Centering logic when justification = 'C'</li>
	 * </ul>
	 *
	 * @param strOrig the original value to format
	 * @param format  the formatting or masking expression
	 * @return the formatted string
	 */
	private static String _fmt(final String strOrig, final String format) {
		if (format == null || format.length() == 0) {
			return strOrig;
		}
		String str = strOrig;

		// see if format is for a data/time
		String s = format.toLowerCase();
		int x = s.length();
		boolean bAlignment = false;
		boolean bLetters = false;
		for (int i = 0; i < x; i++) {
			char c = s.charAt(i);
			if (c == 'l' || c == 'r' || c == 'c') {
				bAlignment = true;
				break;
			}
			if (Character.isLetter(c)) {
				bLetters = true;
			}
		}
		
		if (!bAlignment) {
			if (bLetters) {
				if (DATE_PATTERN.matcher(format).matches()) {
					// try date
					try {
						OADateTime dt = new OADateTime(str);
						return dt.toString(format);
					} catch (Exception e) {
					}
				}
			} else if (str != null && NUMBER_PATTERN.matcher(format).matches()) {
				// try number
				try {
					Number num = OAConv.toDouble(str);
					return OAConv.toString(num, format);
				} catch (Exception e) {
				}
			}
		}

		// see if format is for a number
		int i, j, k, l, blanks = 0, len = 0;
		char lr = 0;
		char testc, charPad = ' ';
		int deci = 0;
		boolean comma = false, dollar = false;
		boolean deci_flag = false;
		boolean bDots = false;
		String test, test1;

		if (str == null) {
			str = "";
		}

		x = format.length();

		// find L or R and format number
		for (i = 0, len = 0; i < x && lr == 0; i++) {
			testc = Character.toUpperCase(format.charAt(i));
			if ("RLC".indexOf(testc) < 0) {
				continue;
			}

			lr = testc;
			test = OAString.fieldAt(format, format.charAt(i), 0);
			if (test == null) {
				test = "";
			}
			test1 = OAString.fieldAt(test, ' ', 0);
			if (test1 == null) {
				test1 = "";
			}
			/* length plus spaces  ex: "10 L" */
			blanks = test.length() - test1.length();
			try {
				len = (test1 == null || test1.length() == 0) ? 0 : Integer.parseInt(test1);
			} catch (Exception e) {
				len = 0;
			}
		}

		if (lr == 0) {
			lr = 'L';
			test1 = OAString.field(format, ' ', 0);
			if (test1 == null) {
				test1 = "";
			}
			i = test1.length();
			if (i > 0) {
				try {
					len = Integer.parseInt(test1);
				} catch (Exception e) {
					len = 0;
				}
			}
			for (; i < x && format.charAt(i) == ' '; i++) {
				blanks++;
			}
		}

		// check for decimals
		if (i < format.length()) {
			if (Character.isDigit(format.charAt(i))) {
				deci = (format.charAt(i++) - '0');
				deci_flag = true;
			}
		}

		for (; i < format.length() && format.charAt(i) != '('; i++) {
			switch (format.charAt(i)) {
			case ',':
				comma = true;
				break;
			case '$':
				dollar = true;
				break;
			case '.':
				if ((lr == 'L' || lr == 'R') && !bDots) {
					bDots = true;
					break;
				}
			default:
				charPad = format.charAt(i);
			}
		}

		if (deci_flag || comma) {
			double d = 0.0;
			try {
				d = OAConv.toDouble(str);
				s = "";
				if (deci_flag) {
					s = "";
					for (j = 0; j < deci; j++) {
						if (j == 0) {
							s += ".";
						}
						s += "0";
					}
				}
				if (comma) {
					s = "#,###" + s;
				} else {
					s = "#" + s;
				}
				str = OAConv.toString(Double.valueOf(d), s);
			} catch (Exception e) {
			}
		}

		// create mask
		j = format.indexOf("(");
		if (j >= 0) {
			test = format.substring(j + 1);
			j = test.length();

			if (test.charAt(j - 1) == ')') {
				test = test.substring(0, (--j));
			}

			if (lr == 'R') {
				for (i = 0, k = 0; i < j; i++) {
					testc = test.charAt(i);
					if (testc == '#') {
						k++;
					}
				}
				k = (k - str.length());
				if (k > 0) {
					str = OAString.pad(str, k, false, ' ');
				}
			}

			String newString = "";
			for (i = k = l = 0; i < j; i++, k++) {
				testc = test.charAt(i);

				if (testc == '#') {
					if (str.length() > l) {
						newString += str.charAt(l++);
					} else {
						newString += ' ';
					}
				} else {
					newString += testc;
				}
			}
			str = newString;
		}

		if (dollar && lr == 'R') {
			str = '$' + str;
		}

		/* format */
		i = str.length();
		x = (Math.abs(i - len)) / 2;

		if (i > len) {
			if (len != 0) {
				if (lr == 'R') {
                    if (bDots && len > 3) {
                        str = "..." + str.substring( (i - len) + 3);
                    } else {
                        str = str.substring(i - len);
                    }
				} else {
					if (lr == 'L') {
						if (bDots && len > 3) {
							str = str.substring(0, len - 3) + "...";
						} else {
							str = str.substring(0, len);
						}
					} else { // 'C'
						str = str.substring(x, x + len);
					}
				}
			}
		} else {
			for (j = 0; i < len; i++, j++) {
				if (lr == 'R' || (lr == 'C' && j < x)) {
					str = charPad + str;
				} else {
					if (!bDots) {
						str += charPad;
					}
				}
			}
		}
		for (i = 0; i < blanks; i++) {
			str += " ";
		}

		return str;
	}

	
	/**
	 * Calculates the number of decimal places in a numeric string. Validates that
	 * all characters after the decimal point are digits; otherwise returns zero.
	 *
	 * @param num                   the numeric string to inspect
	 * @param bIgnoreTrailingZeros  whether trailing zeros should be excluded
	 * @return the count of decimal digits based on the rules
	 */
	public static int getNumberOfDecimalPlaces(String num, boolean bIgnoreTrailingZeros) {
		if (num == null) return 0;
		int x = num.length();
		if (x == 0) return 0;
		boolean bSkipZeros = true;
		int cnt = 0;
		for (int i=x-1; i>=0; i--) {
			char ch = num.charAt(i);
			if (ch == '.') break;
			if (!Character.isDigit(ch)) return 0;
			if (bSkipZeros) { 
				if (ch == '0' && bIgnoreTrailingZeros) continue;
				bSkipZeros = false;
			}
			cnt++;
		}
		return cnt;
	}

	/**
	 * Returns whether the supplied string can be converted to a {@link Double}
	 * using {@link OAConverter}. Rejects empty or whitespace-only input and values
	 * that parse to NaN or infinity.
	 *
	 * @param str the text to evaluate
	 * @return true if it represents a valid number
	 */
	public static boolean isNumber(String str) {
	    if (str == null || str.length() == 0) return false;

	    str = str.trim();
	    try {
	        Double d = (Double) OAConverter.convert(Double.class, str);
	        return d != null && !d.isNaN() && !d.isInfinite();
	    } catch (Exception ex) {
	        return false;
	    }
	}

	
	/**
	 * Determines whether the supplied string represents a whole integer. Delegates
	 * to {@link OAConverter} to attempt conversion to {@link Long}.
	 *
	 * @param str the text to evaluate
	 * @return true if the string converts to a non-null Long
	 */
	public static boolean isInteger(String str) {
	    if (str == null || str.length() == 0) return false;

	    str = str.trim();
	    try {
	        Long l = (Long) OAConverter.convert(Long.class, str);
	        return l != null;
	    } catch (Exception ex) {
	        return false;
	    }
	}
	
	/**
	 * Indicates whether the string can be converted into an {@link OADate}.
	 * Delegates to {@link OAConverter}.
	 *
	 * @param s the text to evaluate
	 * @return true if conversion to OADate succeeds
	 */
	public static boolean isDate(String s) {
	    if (s == null || s.length() == 0) return false;
	    try {
	        return OAConverter.convert(OADate.class, s.trim()) != null;
	    } catch (Exception ex) {
	        return false;
	    }
	}

	/**
	 * Indicates whether the string can be converted into an {@link OATime}.
	 * Delegates to {@link OAConverter}.
	 *
	 * @param s the text to evaluate
	 * @return true if conversion to OATime succeeds
	 */
	public static boolean isTime(String s) {
	    if (s == null || s.length() == 0) return false;
	    try {
	        return OAConverter.convert(OATime.class, s.trim()) != null;
	    } catch (Exception ex) {
	        return false;
	    }
	}
	
	/**
	 * Indicates whether the string can be converted into an {@link OADateTime}.
	 * Delegates to {@link OAConverter}.
	 *
	 * @param s the text to evaluate
	 * @return true if conversion to OADateTime succeeds
	 */
	public static boolean isDateTime(String s) {
	    if (s == null || s.length() == 0) return false;
	    try {
	        return OAConverter.convert(OADateTime.class, s.trim()) != null;
	    } catch (Exception ex) {
	        return false;
	    }
	}	
	
	/**
	 * Applies a mask expression to the supplied value. Characters in the mask
	 * that are '#' are replaced with characters from {@code value}; all others
	 * are copied literally.
	 * <p>
	 * When right-justified, the value is padded or truncated to match the number
	 * of '#' placeholders before mask expansion begins.
	 *
	 * @param value           the text to apply the mask to; null becomes ""
	 * @param mask            the mask pattern; if null, {@code value} is returned
	 * @param bRightJustified whether value should be aligned to the right within '#' slots
	 * @return the masked output
	 */
	public static String mask(String value, String mask, boolean bRightJustified) {
		if (mask == null) {
			return value;
		}
		if (value == null) {
			value = "";
		}

		int i = 0;
		int x = value.length();

		int i2 = 0;
		int x2 = mask.length();

		if (bRightJustified) {
			int cnt = 0;
			for (; i2 < x2; i2++) {
				char c = mask.charAt(i2);
				if (c == '#') {
					cnt++;
				}
			}
			i2 = 0;
			if (cnt > x) {
				value = OATextAlign.padStart(value, cnt - x, ' ');
				x = value.length();
			} else {
				if (x > cnt) {
					value = value.substring(x - cnt);
				}
			}
		}

		StringBuilder sb = new StringBuilder(value.length() + 4);
		for (; i2 < x2; i2++) {
			char c = mask.charAt(i2);
			if (c == '#') {
				if (i < x) {
					sb.append(value.charAt(i));
					i++;
				}
				else sb.append(' ');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Converts an integer into its ordinal English representation:
	 * 1 → "1st", 2 → "2nd", 3 → "3rd", everything else ends in "th".
	 * Accounts for the special 11–13 exception case.
	 *
	 * @param x the number to convert
	 * @return the ordinal representation
	 */
	public static String toNumberString(int x) {
	    int mod100 = x % 100;
	    if (mod100 >= 11 && mod100 <= 13) {
	        return x + "th";
	    }
	    switch (x % 10) {
	        case 1: return x + "st";
	        case 2: return x + "nd";
	        case 3: return x + "rd";
	        default: return x + "th";
	    }
	}	

	/**
	 * Normalizes a phone number to contain only digits and spaces, then left pads
	 * it with spaces to a length of 10. Non-digit, non-space characters are removed.
	 *
	 * @param phone the phone number to clean
	 * @return a 10-character digit/space phone number, or null if input is null
	 */
	public static String convertToValidPhoneNumber(String phone) {
		if (phone == null) {
			return null;
		}
		int x = phone.length();
		if (x == 0) {
			return phone;
		}
		StringBuilder sb = new StringBuilder(x);
		boolean b = false;
		for (int i = 0; i < x; i++) {
			char ch = phone.charAt(i);
			if (!Character.isDigit(ch)) {
				if (ch != ' ') {
					b = true;
					continue;
				}
			}
			sb.append(ch);

		}
		x = sb.length();
		for (int i = x; i < 10; i++) {
			b = true;
			sb.insert(0, ' ');
		}
		if (b) {
			phone = sb.toString();
		}
		return phone;
	}

	/**
	 * Indents each line of a newline-separated string by prefixing {@code amt}
	 * space characters. Always returns a non-null string.
	 *
	 * @param text the text to indent; null becomes ""
	 * @param amt  number of leading spaces to add
	 * @return the indented result
	 */
	public static String indent(String text, int amt) {
		if (text == null) text = "";
		StringBuilder sb = new StringBuilder(text.length() + amt);
		String pad = OATextAlign.padStart("", amt, ' ');
		for (String s : text.split("\n")) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(pad);
			sb.append(s);
		}
		return sb.toString();
	}
	
	/**
	 * Removes leading spaces from each line of the text. Delegates to
	 * {@link #unindent(String, boolean)} using {@code false}.
	 *
	 * @param text the text to process; may be null
	 * @return the unindented form
	 */
	public static String unindent(String text) {
		return unindent(text, false);
	}

	/**
	 * Removes leading spaces from each line based on the indentation level
	 * of the first line. Useful for normalizing code blocks.
	 *
	 * @param text the text to process
	 * @return the unindented result
	 */
	public static String unindentCode(String text) {
		return unindent(text, true);
	}
	
	
	/**
	 * Removes up to a uniform number of leading spaces from each line. When
	 * {@code bBasedOnFirstLine} is true, the number of spaces removed is fixed
	 * based on the first line’s indentation.
	 *
	 * @param text               the text to process
	 * @param bBasedOnFirstLine  whether indentation should match first line
	 * @return text with leading spaces removed on each line
	 */
	public static String unindent(String text, boolean bBasedOnFirstLine) {
		StringBuilder sb = new StringBuilder(text.length());
		int max = -1;
		for (String s : text.split("\n")) {
			if (sb.length() > 0) {
				sb.append('\n');
			}

			int pos = 0;
			for (; pos < s.length() && s.charAt(pos) == ' ' && (!bBasedOnFirstLine || max < 0 || pos < max); pos++) {
				;
			}
			if (bBasedOnFirstLine && max < 0) {
				max = pos;
			}

			if (pos > 0) {
				s = s.substring(pos);
			}
			sb.append(s);
		}
		return sb.toString();
	}
	
	/**
	 * Removes all trailing whitespace characters from the string. If the string
	 * contains no trailing whitespace, it is returned unchanged.
	 *
	 * @param text the text to trim; may be null
	 * @return the trimmed string, or null if the input is null
	 */
	public static String trimEndingWhitespace(String text) {
		if (text == null) {
			return null;
		}
		int x = text.length();
		for (int i = 0; i < x; i++) {
			char c = text.charAt(x - i - 1);
			if (!Character.isWhitespace(c)) {
				if (i == 0) {
					return text;
				}
				return text.substring(0, x - i);
			}
		}
		return "";
	}

	/**
	 * Removes leading and trailing whitespace, while preserving internal single
	 * spaces. Collapses runs of whitespace into a single space without modifying
	 * non-space characters.
	 *
	 * @param text the text to trim; may be null
	 * @return the whitespace-normalized text
	 */
	public static String trimWhitespace(String text) {
		if (text == null) {
			return null;
		}
		StringBuilder sb = null;
		int x = text.length();

		char chLast = ' ';
		boolean bAddSpace = false;

		for (int i = 0; i < x; i++) {
			char ch = text.charAt(i);

			if (Character.isWhitespace(ch)) {
				if (ch == ' ') {
					if (chLast != ' ') {
						bAddSpace = true;
					}
					chLast = ch;
				}
				if (sb == null) {
					sb = new StringBuilder(x);
					if (i > 0) {
						sb.append(text.substring(0, i));
					}
				}
			} else {
				if (sb != null) {
					if (bAddSpace) {
						sb.append(' ');
						bAddSpace = false;
					}
					sb.append(ch);
				}
				chLast = ch;
			}
		}
		if (sb == null) {
			return text;
		}
		return sb.toString();
	}


	/**
	 * Default set of separator characters used by camel-case and Hungarian-case
	 * conversion routines. Any character in this set is treated as a word
	 * boundary during name transformation.
	 */
	private final static String validToCamelCaseSep = " _,.:|\t-/";

	/*
	 * Example: "Your Name Test" converts to "YourNameTest" Example: "your name test" converts to "yourNameTest" Example: "Your_name_test"
	 * converts to "YourNameTest" Example: "your.name.test" converts to "yourNameTest" first char upper/lower-case is not changed.
	 */
	/**
	 * Converts the input text to camelCase form using the default separator set.
	 * Delegates to {@link #convertToHungarian(String, String)} without altering
	 * the case of the first character.
	 *
	 * @param value the text to convert; may be null
	 * @return camelCase representation, or null if value is null
	 */
	public static String convertToCamelCase(String value) {
		return convertToHungarian(value, null);
	}

	/**
	 * Converts the input text to camelCase using the supplied separator characters.
	 * Delegates to {@link #convertToHungarian(String, String)}.
	 *
	 * @param value    the text to convert; may be null
	 * @param sepChars characters treated as separators; null uses defaults
	 * @return camelCase representation, or null if value is null
	 */
	public static String convertToCamelCase(String value, String sepChars) {
		return convertToHungarian(value, sepChars);
	}

	/**
	 * Converts a string into Hungarian-style camelCase, using the default separator
	 * set. Delegates to {@link #convertToHungarian(String, String)}.
	 *
	 * @param value the text to convert; may be null
	 * @return transformed string, or null if value is null
	 */
	public static String convertToHungarian(String value) {
		return convertToHungarian(value, null);
	}

	/**
	 * Converts text to Hungarian/camelCase by removing separators and capitalizing
	 * characters following separators. Digits immediately following other digits
	 * preserve separator behavior by inserting the separator character if the
	 * original separator was a space.
	 *
	 * <ul>
	 *   <li>Characters in {@code sepChars} are skipped but remembered as boundaries.</li>
	 *   <li>After a boundary, letters are upper-cased; consecutive digits remain grouped.</li>
	 *   <li>If {@code value} is null, returns null.</li>
	 * </ul>
	 *
	 * @param value    the text to convert
	 * @param sepChars characters treated as word boundaries; null uses defaults
	 * @return Hungarian/camelCase transformation
	 */
	public static String convertToHungarian(String value, String sepChars) {
		if (value == null) {
			return null;
		}
		if (sepChars == null) {
			sepChars = validToCamelCaseSep;
		}
		int x = value.length();
		StringBuilder sb = new StringBuilder(x);

		char chSep = 0;
		char chLast = 0;
		for (int i = 0; i < x; i++) {
			char ch = value.charAt(i);
			if (sepChars.indexOf(ch) >= 0) {
				chSep = ch;
				continue;
			}
			if (chSep > 0) {
				if (Character.isDigit(ch)) {
					if (chLast > 0 && Character.isDigit(chLast)) {
						if (chSep == ' ') {
							chSep = '_';
						}
						sb.append(chSep);
					}
				} else {
					ch = Character.toUpperCase(ch);
				}
				chSep = 0;
			}
			sb.append(ch);
			chLast = ch;
		}
		return sb.toString();
	}
	
	/**
	 * Delegates to {@link #toUTF8(String)} to convert an ISO-8859-1 encoded
	 * string into UTF-8. Provided for naming convenience.
	 *
	 * @param isoString the ISO-8859-1 text; may be null
	 * @return UTF-8 decoded string
	 */
	public static String toUtf8(String isoString) {
		return toUTF8(isoString);
	}

	/**
	 * Converts a string assumed to be encoded in ISO-8859-1 into a UTF-8 string.
	 * <ul>
	 *   <li>If the input is null or empty, it is returned unchanged.</li>
	 *   <li>If the JVM does not support ISO-8859-1 or UTF-8 (should never occur),
	 *       logs the exception and returns the original string.</li>
	 * </ul>
	 *
	 * @param isoString the ISO-8859-1 encoded text
	 * @return UTF-8 string, or original string on encoding failure
	 */
	public static String toUTF8(String isoString) {
		String utf8String = null;
		if (null != isoString && !isoString.equals("")) {
			try {
				byte[] stringBytesISO = isoString.getBytes("ISO-8859-1");
				utf8String = new String(stringBytesISO, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO: This should never happen. The UnsupportedEncodingException
				// should be propagated instead of swallowed. This error would indicate
				// a severe misconfiguration of the JVM.

				// As we can't translate just send back the best guess.
				System.out.println("UnsupportedEncodingException is: " + e.getMessage());
				utf8String = isoString;
			}
		} else {
			utf8String = isoString;
		}
		return utf8String;
	}



}


