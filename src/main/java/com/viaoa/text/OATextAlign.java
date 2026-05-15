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

/*qqqqqqqqqqqqq
CODEX

 - file/class/method: OATextAlign.right, OATextAlign.center, OATextFilter.substring
  - concrete failure scenario: Right/center substring helpers throw for normal truncation.
  - example input: OAString.right("abcdef", 2)
  - expected result: ef
  - actual or likely result: StringIndexOutOfBoundsException
  - why it matters to OA: Shared UI/display formatting helpers can fail on common truncation paths.
  - fix direction: Treat the third substring parameter consistently as length, or pass start + amount from right/
    center.

 17. Medium - OATextAlign.padStart / padEnd
     Scenario: surrogate pairs are over-padded because width is computed with value.length() before code-point
     alignment.
     Example: OAString.padStart("😀", 1) produces two leading spaces instead of one.
     Impact: Unicode UI/text formatting widths become wrong.


1. file/class/method: src/main/java/com/viaoa/text/OATextAlign.java / OATextAlign.leftPad

  exact execution path: leftPad("abc", 5, ' ') calls padStart("abc", 5, ' '); padStart treats 5 as pad amount,
  computes target width as 5 + value.length(), and returns "     abc".

  why it is an obvious concrete bug: leftPad documents width as the target result width, but it produces a result of
  value.length() + width. A caller expecting width 5 gets width 8.

  minimal fix: have leftPad call alignRight(value, width, padChar) directly, or change the contract if the old OA
  behavior intentionally means “pad amount”.

  suggested test: assert leftPad("abc", 5, ' ').equals("  abc").







*/


/**
 * Text alignment and padding utility supporting full Unicode (code points) and
 * optional truncation with smart ellipsis behavior.
 *
 * <p>This class is pure static and stateless.</p>
 *
 * @apiNote This class is the recommended API for text alignment in OA.
 *          OAString delegates to this implementation for backward compatibility.
 *          API may evolve slightly in future OA releases as Unicode support expands,
 *          but core behavior will remain stable.
 *
 * @implSpec All width calculations and substring operations are code point–based.
 *           Surrogate pairs and emoji will never be split.
 */
public final class OATextAlign {

	/**
	 * Literal sequence appended to truncated text when ellipsis mode is enabled.
	 * <p>
	 * The visible width of the truncated result reserves enough space for this
	 * sequence so the final string length never exceeds the requested width.
	 */
    private static final String ELLIPSIS = "...";

    private OATextAlign() {
    }

    /**
     * Delegates to {@link #padStart(String, int, char)} using a space character
     * as the default padding.
     *
     * @param value the text to be padded; {@code null} is treated as an empty string
     * @param amount number of spaces to prepend.
     * @return the padded string with leading spaces added as needed
     */
    public static String padStart(String value, int amount) {
        return padStart(value, amount, ' ');
    }

    /**
     * Delegates to {@link #alignRight(String, int, char)} to pad on the left
     * until the requested width is reached.
     *
     * @param value   the text to be padded; {@code null} is treated as an empty string
     * @param amount number of padChars to prepend
     * @param padChar the character used to fill leading positions
     * @return the padded string with the original text right aligned
     */
    public static String padStart(String value, int amount, char padChar) {
    	int x = amount + (value == null ? 0 : value.length());
        return alignRight(value, x, padChar);
    }

    /**
     * Delegates to {@link #padEnd(String, int, char)} using a space character
     * as the default padding.
     *
     * @param value the text to be padded; {@code null} is treated as an empty string
     * @param amount number of spaces to append 
     * @return the padded string with trailing spaces added as needed
     */
    public static String padEnd(String value, int amount) {
        return padEnd(value, amount, ' ');
    }

    /**
     * Delegates to {@link #alignLeft(String, int, char)} to pad on the right
     * until the requested width is reached.
     *
     * @param value   the text to be padded; {@code null} is treated as an empty string
     * @param amount the number of padChars to end to value
     * @param padChar the character used to fill trailing positions
     * @return the padded string with the original text left aligned
     */
    public static String padEnd(String value, int amount, char padChar) {
    	int x = amount + (value == null ? 0 : value.length());
        return alignLeft(value, x, padChar);
    }

    // Alignment core with optional ellipsis

    /**
     * Delegates to {@link #alignLeft(String, int, char, boolean)} with ellipsis
     * behavior disabled.
     *
     * @param value   the text to be aligned; {@code null} is treated as an empty string
     * @param width   the target width of the result
     * @param padChar the character used to fill any extra positions on the right
     * @return the left-aligned and padded string
     */
    public static String alignLeft(String value, int width, char padChar) {
        return alignLeft(value, width, padChar, false);
    }

    /**
     * Delegates to {@link #align(String, int, Align, char, boolean)} using
     * {@link Align#LEFT} alignment.
     *
     * @param value    the text to be aligned; {@code null} is treated as an empty string
     * @param width    the target width of the result
     * @param padChar  the character used to fill any extra positions on the right
     * @param ellipsis whether overflow should be truncated with an ellipsis sequence
     * @return the left-aligned string, padded or truncated to the requested width
     */
    public static String alignLeft(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.LEFT, padChar, ellipsis);
    }

    /**
     * Delegates to {@link #alignRight(String, int, char, boolean)} with ellipsis
     * behavior disabled.
     *
     * @param value   the text to be aligned; {@code null} is treated as an empty string
     * @param width   the target width of the result
     * @param padChar the character used to fill any extra positions on the left
     * @return the right-aligned and padded string
     */
    public static String alignRight(String value, int width, char padChar) {
        return alignRight(value, width, padChar, false);
    }

    /**
     * Delegates to {@link #align(String, int, Align, char, boolean)} using
     * {@link Align#RIGHT} alignment.
     *
     * @param value    the text to be aligned; {@code null} is treated as an empty string
     * @param width    the target width of the result
     * @param padChar  the character used to fill any extra positions on the left
     * @param ellipsis whether overflow should be truncated with an ellipsis sequence
     * @return the right-aligned string, padded or truncated to the requested width
     */
    public static String alignRight(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.RIGHT, padChar, ellipsis);
    }

    /**
     * Delegates to {@link #alignCenter(String, int, char, boolean)} with ellipsis
     * behavior disabled.
     *
     * @param value   the text to be aligned; {@code null} is treated as an empty string
     * @param width   the target width of the result
     * @param padChar the character used to fill any extra positions on both sides
     * @return the centered and padded string
     */
    public static String alignCenter(String value, int width, char padChar) {
        return alignCenter(value, width, padChar, false);
    }

    /**
     * Delegates to {@link #align(String, int, Align, char, boolean)} using
     * {@link Align#CENTER} alignment.
     *
     * @param value    the text to be aligned; {@code null} is treated as an empty string
     * @param width    the target width of the result
     * @param padChar  the character used to fill any extra positions on both sides
     * @param ellipsis whether overflow should be truncated with an ellipsis sequence
     * @return the centered string, padded or truncated to the requested width
     */
    public static String alignCenter(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.CENTER, padChar, ellipsis);
    }

    // =========================================================================
    // Core Internal Alignment
    // =========================================================================

    /**
     * Supported alignment modes used by the core {@link #align(String, int, Align, char, boolean)}
     * method.
     * <ul>
     *   <li>{@link #LEFT} – pads on the right</li>
     *   <li>{@link #RIGHT} – pads on the left</li>
     *   <li>{@link #CENTER} – distributes padding on both sides</li>
     * </ul>
     */
    public static enum Align { LEFT, RIGHT, CENTER }

    /**
     * Core alignment routine used by all public alignment helpers.
     * <p>
     * If the value is {@code null}, it is treated as an empty string. When the
     * requested width is less than one, an empty string is returned. Width
     * comparisons are based on Unicode code points rather than UTF-16 units so
     * surrogate pairs and emoji are never split.
     * <ul>
     *   <li>If the value exactly matches the requested width, it is returned as is.</li>
     *   <li>If the value is longer than the requested width, it is either truncated
     *       or truncated with ellipsis depending on the {@code ellipsis} flag.</li>
     *   <li>If the value is shorter than the requested width, padding is added
     *       according to the selected {@code alignment}.</li>
     * </ul>
     *
     * @param value      the text to be aligned; {@code null} is treated as an empty string
     * @param width      the target width of the result in Unicode code points
     * @param alignment  alignment mode controlling where padding is applied
     * @param padChar    the character used to fill extra positions
     * @param ellipsis   whether long values should be truncated with an ellipsis sequence
     * @return the aligned string, padded or truncated to the requested width
     */
    public static String align(String value, int width, Align alignment, char padChar, boolean ellipsis) {
        if (width < 1) return "";
        if (value == null) value = "";

        int lenCp = value.codePointCount(0, value.length());
        if (lenCp == width) return value;

        // Overflow → truncate
        if (lenCp > width) {
            return ellipsis
                ? truncateWithEllipsis(value, width)
                : substringCp(value, width, alignment);
        }

        // Underflow → pad
        int padTotal = width - lenCp;

        switch (alignment) {
            case RIGHT:
                return repeat(padChar, padTotal) + value;
            case CENTER:
                int leftPad = padTotal / 2;
                int rightPad = padTotal - leftPad;
                return repeat(padChar, leftPad) + value + repeat(padChar, rightPad);
            default: // LEFT
                return value + repeat(padChar, padTotal);
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Truncates the given value to the specified width and appends the ellipsis
     * sequence so that the final width does not exceed the requested value.
     * <p>
     * The available space is computed by subtracting the length of the ellipsis
     * sequence from the requested width. The base portion is then taken from the
     * left side of the string using code point–safe substring operations. If the
     * base portion ends with a period, that period is removed to avoid duplicate
     * punctuation before appending the ellipsis sequence.
     *
     * @param value the text to be truncated; must not be {@code null}
     * @param width the total width including the ellipsis sequence
     * @return a truncated string with an ellipsis appended
     */
    private static String truncateWithEllipsis(String value, int width) {
        int avail = width - ELLIPSIS.length();
        if (avail < 1) {
            return ELLIPSIS.substring(0, width);
        }
        String base = substringCp(value, avail, Align.LEFT);

        // Avoid duplicate periods before ellipsis
        if (!base.isEmpty() && base.endsWith(".")) {
            base = substringCp(base, base.codePointCount(0, base.length()) - 1, Align.LEFT);
        }

        return base + ELLIPSIS;
    }

    /**
     * Returns a substring of the given value using Unicode code point positions
     * instead of raw UTF-16 indices.
     * <p>
     * When the requested width is greater than or equal to the number of code
     * points in {@code value}, the original string is returned. Otherwise, a
     * substring of the requested width is taken based on the supplied alignment:
     * <ul>
     *   <li>{@link Align#LEFT} – from the start of the string</li>
     *   <li>{@link Align#RIGHT} – from the end of the string</li>
     *   <li>{@link Align#CENTER} – centered within the original string</li>
     * </ul>
     *
     * @param value the source text; must not be {@code null}
     * @param width the number of code points to include
     * @param align the alignment used to choose the substring region
     * @return a code point–safe substring of the requested width
     */
    private static String substringCp(String value, int width, Align align) {
        int lenCp = value.codePointCount(0, value.length());
        if (width >= lenCp) return value;

        switch (align) {
            case RIGHT: {
                int start = value.offsetByCodePoints(0, lenCp - width);
                return value.substring(start);
            }
            case CENTER: {
                int offset = (lenCp - width) / 2;
                int start = value.offsetByCodePoints(0, offset);
                int end = value.offsetByCodePoints(start, width);
                return value.substring(start, end);
            }
            default: // LEFT
                int end = value.offsetByCodePoints(0, width);
                return value.substring(0, end);
        }
    }

    /**
     * Creates a new string by repeating the given character a specified number
     * of times.
     * <p>
     * If {@code count} is less than or equal to zero, an empty string is returned.
     *
     * @param c     the character to repeat
     * @param count the number of times to repeat the character
     * @return a string consisting of {@code count} copies of {@code c},
     *         or an empty string if {@code count} is non-positive
     */
    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }


    /**
     * Delegates to {@link #padStart(String, int, char)}.
     *
     * @param value   the text to be padded; {@code null} is treated as an empty string
     * @param width   the target width of the result
     * @param padChar the character used to fill leading positions
     * @return the padded string with the original text right aligned
     */
    public static String leftPad(String value, int width, char padChar) {
        return padStart(value, width, padChar);
    }

    /** @deprecated Use {@link #padEnd(String,int,char)} */
    @Deprecated
    public static String rightEnd(String value, int width, char padChar) {
        return padEnd(value, width, padChar);
    }

    /**
     * Aligns the given value either left or right based on the {@code bAlignLeft}
     * flag. This is a convenience method that delegates to
     * {@link #alignLeft(String, int, char)} or
     * {@link #alignRight(String, int, char)} accordingly.
     *
     * @param value      the text to be aligned; {@code null} is treated as an empty string
     * @param width      the target width of the result
     * @param bAlignLeft {@code true} to left align, {@code false} to right align
     * @param padChar    the character used to fill extra positions
     * @return the aligned string padded to the requested width
     */
    public static String align(String value, int width, boolean bAlignLeft, char padChar) {
        return bAlignLeft ? alignLeft(value, width, padChar) : alignRight(value, width, padChar);
    }

    
    /**
     * Returns the leftmost portion of the given text using
     * {@link OATextFilter#substring(String, int, int)}.
     * <p>
     * If {@code value} is {@code null}, {@code null} is returned. If the string
     * is shorter than or equal to {@code amount}, it is returned unchanged.
     *
     * @param value  the text to extract from
     * @param amount the number of characters to return
     * @return the leftmost {@code amount} characters, or the entire string
     *         if shorter; {@code null} if {@code value} is null
     */
	public static String left(String value, int amount) {
		return OATextFilter.substring(value, 0, amount);
	}

	/**
	 * Returns the rightmost portion of the given text using
	 * {@link OATextFilter#substring(String, int, int)}.
	 * <p>
	 * If {@code value} is {@code null}, {@code null} is returned. If the string
	 * is shorter than or equal to {@code amount}, it is returned unchanged.
	 *
	 * @param value  the text to extract from
	 * @param amount the number of characters to return
	 * @return the rightmost {@code amount} characters, or the entire string
	 *         if shorter; {@code null} if {@code value} is null
	 */
	public static String right(String value, int amount) {
		if (value == null) {
			return null;
		}
		int len = value.length();
		if (len <= amount) {
			return value;
		}
		return OATextFilter.substring(value, len - amount, amount);
	}

	/**
	 * Extracts a centered substring of the specified length using
	 * {@link OATextFilter#substring(String, int, int)}.
	 * <p>
	 * If {@code value} is {@code null}, {@code null} is returned. If
	 * {@code amount} is less than one, an empty string is returned. If the
	 * string is shorter than or equal to {@code amount}, the original value
	 * is returned unchanged.
	 * <p>
	 * When centering, the starting offset is computed from half the string
	 * length minus half the requested amount. If this offset is negative,
	 * zero is used instead.
	 *
	 * @param value  the text to extract from
	 * @param amount the number of characters to return
	 * @return a centered substring of {@code amount} characters, the whole
	 *         string if shorter, an empty string if {@code amount} < 1,
	 *         or {@code null} if {@code value} is null
	 */
	public static String center(String value, int amount) {
		if (value == null) {
			return null;
		}
		if (amount < 1) {
			return "";
		}
		int len = value.length();
		if (len <= amount) {
			return value;
		}

		int midPos = len / 2;
		int start = midPos - (amount / 2);
		if (start < 0) start = 0;
		return OATextFilter.substring(value, start, amount);
	
	}


}
