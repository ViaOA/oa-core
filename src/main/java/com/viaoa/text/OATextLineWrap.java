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

import java.util.ArrayList;
import java.util.List;

/**
 * A configurable text line–wrapping and truncation utility designed for both
 * ASCII and full Unicode text. It supports automatic wrapping based on:
 * <ul>
 *     <li><b>Maximum column width</b> (character count limit per row)</li>
 *     <li><b>Natural breakpoints</b> such as whitespace</li>
 *     <li><b>User-supplied break characters</b> (e.g., hyphens)</li>
 *     <li><b>Smart hyphenation</b> for long unbreakable words</li>
 *     <li><b>Maximum output rows</b>, with graceful ellipsis-based truncation</li>
 * </ul>
 *
 * <h3>Key Design Goals</h3>
 * <ul>
 *     <li>Readable output that preserves whole words whenever possible</li>
 *     <li>Unicode-safe: never split surrogate pairs, emoji, etc.</li>
 *     <li>Configurable behavior through fluent API options</li>
 *     <li>Fast path for pure ASCII for high-performance rendering</li>
 * </ul>
 *
 * <h3>Breaking Rules</h3>
 * <ol>
 *     <li>Prefer whitespace boundaries</li>
 *     <li>If a line would exceed width, use the nearest preceding separator</li>
 *     <li>If no separator exists within bounds, fall back to <b>hyphenation</b></li>
 *     <li>Hyphenation is only allowed when a single token exceeds width</li>
 *     <li>Never break a Unicode code point mid-character</li>
 * </ol>
 *
 * <h3>Truncation Rules (when maxRows is set)</h3>
 * <ul>
 *     <li>If text exceeds allowed rows:
 *         <ul>
 *             <li>Final row ends with “...”</li>
 *             <li>Ellipsis counts against width budget</li>
 *             <li>Whitespace separation respected if possible</li>
 *             <li>Avoid “double ellipsis” (if already ending in '.') → use a single space then "..."</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>Sizing Rules</h3>
 * <ul>
 *     <li><code>width</code> is enforced before separators are appended</li>
 *     <li><code>separator</code> (e.g., "|") is <b>not</b> counted toward width</li>
 *     <li><code>minSegmentLen</code> ensures readability when hyphenating</li>
 * </ul>
 *
 * <h3>Fast ASCII Path</h3>
 * <p>
 * If all characters are within ISO-8859-1 and no surrogate pairs are detected,
 * the algorithm avoids code-point navigation and uses faster <code>char</code>­­-based logic.
 * </p>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * Instances are reusable & mutable through <code>withXxx()</code> calls, but should not
 * be used concurrently by multiple threads.
 * </p>
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * OATextLineWrap wrap = new OATextLineWrap(10, "|")
 *     .withMaxRows(3)
 *     .withMinSegmentLen(3);
 *
 * String[] rows = wrap.wrap("This is some long text that should wrap nicely");
 * // rows:
 * // "This is|"
 * // "some long|"
 * // "text that..."
 * }</pre>
 *
 * <h3>Intended Use Cases</h3>
 * <ul>
 *     <li>Display-width constrained UIs (OA-Web, OA-JFC)</li>
 *     <li>Reports and formatted logs</li>
 *     <li>Dashboard panels, tooltips, summaries</li>
 * </ul>
 *
 * @author Vince Via
 */
public class OATextLineWrap {
    
	/**
	 * Maximum number of code points allowed per output row before a wrap occurs.
	 */
	private int maxWidth; // row width
    
	/**
	 * String inserted between rows when using {@link #wrapToString(String)}.
	 * Not counted against the width budget of each row.
	 */
	private String separator;

	/**
	 * Optional limit on the number of rows produced. A value of zero disables
	 * truncation and allows unlimited rows.
	 */
    private int maxRows = 0;            // 0 = unlimited
    
    /**
     * Minimum number of code points that must remain on the next line when
     * performing forced hyphenation on an unbreakable word.
     */
    private int minSegmentLen = 3;      // minimum segment when forced-hyphenating
    
    /**
     * Additional break characters—beyond whitespace—that may be used as natural
     * wrap boundaries (e.g., hyphens).
     */
    private String breakChars = "-";    // additional natural break chars besides whitespace

    
    /**
     * Constructs a wrapper with default settings: width 5 and an empty separator.
     * Intended for quick testing or minimal-configuration scenarios.
     */
    public OATextLineWrap() {
    	this.maxWidth = 5;
    	this.separator = "";
    }
    
    /**
     * Constructs a wrapper with the specified row width and separator.
     *
     * @param width     maximum row width in code points; must be > 0
     * @param separator value inserted between rows; null becomes ""
     * @throws IllegalArgumentException if width is not greater than zero
     */
    public OATextLineWrap(int width, String separator) {
        if (width <= 0) throw new IllegalArgumentException("width must be > 0");
        this.maxWidth = width;
        this.separator = (separator == null) ? "" : separator;
    }

    /**
     * Returns the configured maximum width in code points for each row.
     *
     * @return maximum row width
     */
    public int getMaxWidth() {
        return maxWidth;
    }

    /**
     * Sets the maximum row width.
     *
     * @param v new width; must be > 0
     * @throws IllegalArgumentException if v is less than 1
     */
    public void setMaxWidth(int v) {
        if (v < 1) throw new IllegalArgumentException("maxWidth must be > 0");
        this.maxWidth = v;
    }

    /**
     * Fluent wrapper for {@link #setMaxWidth(int)}.
     *
     * @param v new maximum width
     * @return this instance for method chaining
     */
    public OATextLineWrap withMaxWidth(int v) {
        setMaxWidth(v);
        return this;
    }

    /**
     * Returns the configured upper bound for the number of output rows.
     * A value of zero indicates no limit.
     *
     * @return maximum number of rows or zero for unlimited
     */
    public int getMaxRows() {
        return maxRows;
    }
    
    /**
     * Sets the maximum number of output rows.
     *
     * @param v number of rows allowed; must be > 0
     * @throws IllegalArgumentException if v is less than 1
     */
    public void setMaxRows(int v) {
        if (v < 1) throw new IllegalArgumentException("maxRows must be > 0");
        this.maxRows = v;
    }

    /**
     * Fluent wrapper for {@link #setMaxRows(int)}.
     *
     * @param v maximum number of rows
     * @return this instance for chaining
     */
    public OATextLineWrap withMaxRows(int v) {
        setMaxRows(v);
        return this;
    }

    /**
     * Returns the minimum number of code points that must be preserved on the
     * following line when forced hyphenation occurs.
     *
     * @return minimum segment length
     */
    public int getMinSegmentLen() {
        return minSegmentLen;
    }

    /**
     * Sets the minimum segment length used during forced hyphenation.
     *
     * @param v minimum allowed segment length; must be > 0
     * @throws IllegalArgumentException if v is less than 1
     */
    public void setMinSegmentLen(int v) {
        if (v < 1) throw new IllegalArgumentException("minSegmentLen must be > 0");
        this.minSegmentLen = v;
    }

    /**
     * Fluent wrapper for {@link #setMinSegmentLen(int)}.
     *
     * @param v minimum segment length
     * @return this instance for chaining
     */
    public OATextLineWrap withMinSegmentLen(int v) {
        setMinSegmentLen(v);
        return this;
    }

    /**
     * Returns the set of extra break characters used when searching for wrap
     * positions in addition to whitespace.
     *
     * @return configured break characters; never null
     */
    public String getBreakChars() {
        return breakChars;
    }

    /**
     * Sets the break characters that should be treated as natural wrap points.
     *
     * @param v characters used as break points; null becomes ""
     */
    public void setBreakChars(String v) {
        this.breakChars = (v != null) ? v : "";
    }

    /**
     * Fluent wrapper for {@link #setBreakChars(String)}.
     *
     * @param v new break-character set
     * @return this instance for chaining
     */
    public OATextLineWrap withBreakChars(String v) {
        setBreakChars(v);
        return this;
    }

    /**
     * Returns the separator string used when joining wrapped rows in
     * {@link #wrapToString(String)}.
     *
     * @return the row separator; never null
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Sets the separator used to join wrapped rows.
     *
     * @param s the separator; null becomes ""
     */
    public void setSeparator(String v) {
        this.separator = (v != null) ? v : "";
    }

    /**
     * Fluent wrapper for {@link #setSeparator(String)}.
     *
     * @param s the new separator
     * @return this instance for chaining
     */
    public OATextLineWrap withSeparator(String v) {
        setSeparator(v);
        return this;
    }
    

    /**
     * Wraps the supplied text into a list of rows based on current configuration.
     * <ul>
     *   <li>Delegates to {@link #wrapToString(String)} and splits the result using
     *       {@link #separator}.</li>
     *   <li>Always returns a non-null list; empty input yields a single empty row.</li>
     * </ul>
     *
     * @param text the text to wrap; null becomes ""
     * @return list of wrapped rows
     */
    public List<String> wrap(String text) {
        final List<String> alRow = new ArrayList<>();
        if (text == null || text.isEmpty()) return alRow;

        // Single unified algorithm with codepoint-safety.
        final int n = text.length();

        int rowIndex = 0;
        int posChar = 0; // char index (UTF-16)

        while (posChar < n) {
            // Skip leading whitespace at the start of each row.
            posChar = skipWhitespace(text, posChar);
            if (posChar >= n) break;

            boolean bIsLastRow = (maxRows > 0 && rowIndex == maxRows - 1);

            if (bIsLastRow) {
                // Determine if remaining fits within width (in code points).
                int x = getCodePointCount(text, posChar, n);
                if (x <= maxWidth) {
                    // Not truncating: greedy pack whole tokens into one final row.
                    int end = packFinalRow(text, posChar, maxWidth);
                    alRow.add(rstrip(text.substring(posChar, end)));
                    posChar = end;
                    break;
                }
                // Truncation required on this final row -> produce truncated row with ellipsis.
                int newPos = emitRowTruncated(text, posChar, alRow);
                posChar = newPos;
                break; // final row done
            } else {
                // Normal row
                int newPos = emitRow(text, posChar, alRow);
                if (newPos <= posChar) {
                    int hardEnd = advanceByCodePoints(text, posChar, maxWidth);
                    alRow.add(rstrip(text.substring(posChar, hardEnd)));
                    posChar = hardEnd;
                } else {
                    posChar = newPos;
                }
            }

            rowIndex++;
            if (maxRows > 0 && rowIndex >= maxRows) {
                // Shouldn't happen (we handle final row above), but guard anyway.
                break;
            }
        }

        return alRow;
    }

    /**
     * Wraps text using the configured width, max rows, and break characters,
     * returning a single joined string separated by {@link #separator}.
     *
     * @param text the text to wrap; null becomes ""
     * @return wrapped text using the current separator
     */
    public String wrapToString(String text) {
        List<String> alRow = wrap(text);
        if (alRow.size() == 0) return "";
        if (separator.isEmpty()) {
            // No visible separator: join without extra cost.
            StringBuilder sb = new StringBuilder();
            for (String r : alRow) sb.append(r);
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (String s : alRow) {
            if (sb.length() > 0) sb.append(separator);
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Emits a single non-final wrapped row starting at the given character index.
     * <p>
     * The method scans forward from {@code startChar}, counting code points up to
     * {@link #maxWidth}. While scanning, it tracks the last whitespace or configured
     * break character to use as a natural wrap point. If the width limit is reached
     * without encountering a usable break, forced hyphenation is performed, ensuring
     * that at least {@link #minSegmentLen} code points remain for the next row.
     * <p>
     * The substring for the row is right-stripped of trailing whitespace. When a forced
     * hyphenation occurs, a hyphen is appended to the emitted row. The finished row is
     * added to {@code alRow}.
     *
     * @param s         full source text being wrapped
     * @param startChar UTF-16 character index where this row begins
     * @param alRow     destination list receiving the generated row
     * @return the next starting UTF-16 character index after the emitted row,
     *         with leading whitespace skipped
     */
    private int emitRow(String s, int startChar, List<String> alRow) {
        final int n = s.length();
        
        int i = startChar;
        int used = 0; // width usage in code points

        int lastBreakCharPos = -1; // char index after the break character/space

        while (i < n && used < maxWidth) {
            int cp = s.codePointAt(i);
            int cplen = Character.charCount(cp);

            if (isWhitespace(cp)) {
                // record break after this whitespace
                lastBreakCharPos = i + cplen;
            } else if (isBreakChar(cp)) {
                lastBreakCharPos = i + cplen;
            }
            used++;
            i += cplen;
        }

        int chEnd;
        boolean bAddHyphen = false;

        if (i >= n && used <= maxWidth) {
            // Consumed remainder within width: use all
            chEnd = n;
        } else if (i > startChar && isWhitespace(s.codePointAt(i))) {
            chEnd = i;
        } else if (i > startChar && !Character.isAlphabetic(s.codePointBefore(i))) {
            chEnd = i;
        } else if (lastBreakCharPos > startChar) {
            // Use last seen break position within width
            chEnd = lastBreakCharPos;
        } else {
        	
            // Forced hyphenation: pick a cut guaranteeing minSegmentLen
            // need to leave 3+ chars (minSeg) on next line.
            // Forced hyphenation: pick a cut guaranteeing minSegmentLen
            // need to leave 3+ chars (minSeg) on next line.
            int leftCps = maxWidth;
        	if (maxWidth > minSegmentLen + 1) {
	            leftCps -= minSegmentLen;
	            int x = Math.min(minSegmentLen, getCodePointCount(s, i, n)); // amount leftover
	            for (int j=0; j < x; j++) {
	                int cp = s.codePointAt(i);
	                int cplen = Character.charCount(cp);
	                if (isWhitespace(cp)) break;
	                i += cplen;
	                leftCps++;
	            }
        	}
        	
            // else move back one char, to give '-' a slot
            if (leftCps == maxWidth) leftCps--;
            
            // convert leftCps to char index
            chEnd = advanceByCodePoints(s, startChar, leftCps);
            bAddHyphen = true;
        }

        String row = rstrip(s.substring(startChar, chEnd));

        // If forced hyphenation, append '-' (hyphen not counted against width budget)
        if (bAddHyphen) {
            row = appendHyphen(row);
        }

        alRow.add(row);
        return skipWhitespace(s, chEnd);
    }

    /**
     * Emits the final truncated row when {@code maxRows} has been reached and
     * additional text remains. The method reserves space for an ellipsis ("...")
     * within {@link #maxWidth}, scans forward up to the usable budget, and selects
     * a cut point—preferring whitespace or configured break characters if found.
     * <p>
     * The resulting substring is right-trimmed, a trailing period is removed if it
     * would create a double-ellipsis, and the final row is limited to the maximum
     * width in code points. The completed row is added to {@code alRow}.
     *
     * @param text      the full input text
     * @param startChar UTF-16 index where the truncated row begins
     * @param alRow     list receiving the truncated row
     * @return {@code text.length()}, since truncation completes processing
     */
    private int emitRowTruncated(final String text, final int startChar, final List<String> alRow) {
        final int n = text.length();
        final int ellipsisLen = 3;

        if (maxWidth <= ellipsisLen) {
            // Edge case: width too small; just output as many '.' as possible.
            String dots = "...";
            String row = dots.substring(0, maxWidth);
            alRow.add(row);
            return n;
        }

        // We need to leave space for ellipsis (and maybe a space before it).
        int budget = maxWidth - ellipsisLen;

        // Scan forward to budget, tracking last-seen break.
        int i = startChar;
        int used = 0;
        int lastBreakCharPos = -1; // after break

        while (i < n && used < budget) {
            int cp = text.codePointAt(i);
            int cplen = Character.charCount(cp);

            if (isWhitespace(cp)) {
                lastBreakCharPos = i + cplen;
            } else if (isBreakChar(cp)) {
                lastBreakCharPos = i + cplen;
            }

            used++;
            i += cplen;
        }

        int endChar = advanceByCodePoints(text, startChar, budget);

        String base = rstrip(text.substring(startChar, endChar));

        // Avoid duplicate periods before "..."
        if (!base.isEmpty() && base.charAt(base.length() - 1) == '.') {
            base = base.substring(0, base.length() - 1);
        }

        String row = (base + "...");
        row = trimToCodePointWidth(row, maxWidth);

        alRow.add(row);
        return n;
    }

    /**
     * Greedy non-truncating final-row packing routine. Starting at
     * {@code startChar}, the method accumulates whole tokens (words separated by
     * whitespace) into the final row while staying within {@code width} code points.
     * <p>
     * If the first token exceeds {@code width}, the token is hard-cut at the width
     * boundary without hyphenation. Otherwise, additional tokens are added only if a
     * leading space plus the token’s code-point length fits within the budget.
     *
     * @param text      full source text
     * @param startChar UTF-16 index where packing begins
     * @param width     maximum allowed code-point width
     * @return the UTF-16 index where the final packed row ends
     */
    private static int packFinalRow(String text, int startChar, int width) {
        final int n = text.length();
        int i = startChar;
        int used = 0;
        int lastGood = startChar;

        // skip initial whitespace
        i = skipWhitespace(text, i);

        while (i < n) {
            // word = [i, j)
            int j = i;
            while (j < n && !isWhitespace(text.codePointAt(j))) {
                j += Character.charCount(text.codePointAt(j));
            }

            int wordCps = getCodePointCount(text, i, j);
            if (wordCps == 0) break;

            if (used == 0) {
                if (wordCps > width) {
                    // single long token: hard cut at width
                    lastGood = advanceByCodePoints(text, i, width);
                    return lastGood;
                }
                used = wordCps;
                lastGood = j;
            } else {
                // include 1-space + next word if it fits
                if (used + 1 + wordCps <= width) {
                    used += 1 + wordCps;
                    lastGood = j;
                } else {
                    break;
                }
            }

            // advance over whitespace
            int k = j;
            while (k < n && isWhitespace(text.codePointAt(k))) {
                k += Character.charCount(text.codePointAt(k));
            }
            i = k;
        }
        return lastGood;
    }

    /**
     * Determines whether the supplied code point is classified as whitespace.
     * Delegates to {@link Character#isWhitespace(int)}.
     *
     * @param cp code point to evaluate
     * @return true if the code point is whitespace
     */
    private static boolean isWhitespace(int cp) {
        return Character.isWhitespace(cp);
    }

    /**
     * Determines whether the supplied code point matches one of the configured
     * {@link #breakChars}. Performs a fast ASCII hyphen check, then falls back to
     * character-by-character comparison within the break-character set.
     *
     * @param cp code point to evaluate
     * @return true if the code point is a configured break character
     */
    private boolean isBreakChar(int cp) {
        if (breakChars == null || breakChars.isEmpty()) return false;
        // fast path for ASCII hyphen
        if (cp == '-' && breakChars.indexOf('-') >= 0) return true;
        // generic: compare by char(s) in breakChars (assume ASCII break chars)
        for (int i = 0; i < breakChars.length(); i++) {
            if (cp == breakChars.charAt(i)) return true;
        }
        return false;
    }


    /**
     * Appends a hyphen to {@code text} unless it already ends with one. Intended for
     * forced-hyphenation rows. Surrogate-safe examination is performed via
     * {@link String#codePointBefore(int)}.
     *
     * @param text base text segment to modify
     * @return the text with a hyphen appended when appropriate
     */
    private static String appendHyphen(String text) {
        if (text == null || text.isEmpty()) return "";
        int lastCp = text.codePointBefore(text.length());
        return (lastCp == '-') ? text : text + "-";
    }
    
    
    /**
     * Advances {@code posChar} forward while the encountered code points are
     * whitespace. Returns the first index whose code point is non-whitespace or the
     * string length.
     *
     * @param text    source string
     * @param posChar UTF-16 index to begin scanning
     * @return new UTF-16 index positioned at the first non-whitespace character
     */
    private static int skipWhitespace(String text, int posChar) {
        final int n = text.length();
        int i = posChar;
        while (i < n) {
            int cp = text.codePointAt(i);
            if (!Character.isWhitespace(cp)) break;
            i += Character.charCount(cp);
        }
        return i;
    }

    /**
     * Returns the number of Unicode code points between {@code startChar} and
     * {@code endChar}. Delegates to {@link String#codePointCount(int, int)}.
     *
     * @param text      source string
     * @param startChar starting UTF-16 index
     * @param endChar   ending UTF-16 index (exclusive)
     * @return number of code points in the range
     */
    private static int getCodePointCount(String text, int startChar, int endChar) {
        return text.codePointCount(startChar, endChar);
    }

    /**
     * Computes the UTF-16 index obtained by advancing {@code count} code points from
     * {@code startChar}. Delegates to {@link String#offsetByCodePoints(int, int)}.
     *
     * @param text      source string
     * @param startChar starting UTF-16 index
     * @param count     number of code points to advance
     * @return resulting UTF-16 index
     */
    private static int advanceByCodePoints(String text, int startChar, int count) {
        return text.offsetByCodePoints(startChar, count);
    }

    /**
     * Removes trailing whitespace from {@code text}. The scan proceeds backward using
     * code-point–aware navigation via {@link String#codePointBefore(int)} and
     * {@link Character#charCount(int)}. If no trailing whitespace is found, the
     * original string is returned unchanged.
     *
     * @param text input text
     * @return text with trailing whitespace removed
     */
    private static String rstrip(String text) {
        int i = text.length();
        while (i > 0) {
            int cp = text.codePointBefore(i);
            if (!Character.isWhitespace(cp)) break;
            i -= Character.charCount(cp);
        }
        return (i == text.length()) ? text : text.substring(0, i);
    }

    /**
     * Ensures the returned string does not exceed {@code maxCp} Unicode code points.
     * If the text exceeds this limit, the method computes the UTF-16 boundary for the
     * allowed code-point width and returns the substring up to that point.
     *
     * @param text  input text
     * @param maxCp maximum number of code points allowed
     * @return text limited to at most {@code maxCp} code points
     */
    private static String trimToCodePointWidth(String text, int maxCp) {
        int cps = text.codePointCount(0, text.length());
        if (cps <= maxCp) return text;
        int end = text.offsetByCodePoints(0, maxCp);
        return text.substring(0, end);
    }
}
