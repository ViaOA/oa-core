/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

    private static final String ELLIPSIS = "...";

    private OATextAlign() {
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public static String padStart(String value, int width) {
        return padStart(value, width, ' ');
    }

    public static String padStart(String value, int width, char padChar) {
        return alignRight(value, width, padChar);
    }

    public static String padEnd(String value, int width) {
        return padEnd(value, width, ' ');
    }

    public static String padEnd(String value, int width, char padChar) {
        return alignLeft(value, width, padChar);
    }

    // Alignment core with optional ellipsis

    public static String alignLeft(String value, int width, char padChar) {
        return alignLeft(value, width, padChar, false);
    }

    public static String alignLeft(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.LEFT, padChar, ellipsis);
    }

    public static String alignRight(String value, int width, char padChar) {
        return alignRight(value, width, padChar, false);
    }

    public static String alignRight(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.RIGHT, padChar, ellipsis);
    }

    public static String alignCenter(String value, int width, char padChar) {
        return alignCenter(value, width, padChar, false);
    }

    public static String alignCenter(String value, int width, char padChar, boolean ellipsis) {
        return align(value, width, Align.CENTER, padChar, ellipsis);
    }

    // =========================================================================
    // Core Internal Alignment
    // =========================================================================

    public static enum Align { LEFT, RIGHT, CENTER }

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

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }


    public static String leftPad(String value, int width, char padChar) {
        return padStart(value, width, padChar);
    }

    /** @deprecated Use {@link #padEnd(String,int,char)} */
    @Deprecated
    public static String rightEnd(String value, int width, char padChar) {
        return padEnd(value, width, padChar);
    }

    public static String align(String value, int width, boolean bAlignLeft, char padChar) {
        return bAlignLeft ? alignLeft(value, width, padChar) : alignRight(value, width, padChar);
    }

    
	public static String left(String value, int amount) {
		return OATextFilter.substring(value, 0, amount);
	}

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
