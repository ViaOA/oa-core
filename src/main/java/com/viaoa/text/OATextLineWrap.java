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
    private int maxWidth; // row width
    private String separator;

    private int maxRows = 0;            // 0 = unlimited
    private int minSegmentLen = 3;      // minimum segment when forced-hyphenating
    private String breakChars = "-";    // additional natural break chars besides whitespace

    
    public OATextLineWrap() {
    	this.maxWidth = 5;
    	this.separator = "";
    }
    
    public OATextLineWrap(int width, String separator) {
        if (width <= 0) throw new IllegalArgumentException("width must be > 0");
        this.maxWidth = width;
        this.separator = (separator == null) ? "" : separator;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int v) {
        if (v < 1) throw new IllegalArgumentException("maxWidth must be > 0");
        this.maxWidth = v;
    }

    public OATextLineWrap withMaxWidth(int v) {
        setMaxWidth(v);
        return this;
    }

    public int getMaxRows() {
        return maxRows;
    }
    
    public void setMaxRows(int v) {
        if (v < 1) throw new IllegalArgumentException("maxRows must be > 0");
        this.maxRows = v;
    }

    public OATextLineWrap withMaxRows(int v) {
        setMaxRows(v);
        return this;
    }

    public int getMinSegmentLen() {
        return minSegmentLen;
    }

    public void setMinSegmentLen(int v) {
        if (v < 1) throw new IllegalArgumentException("minSegmentLen must be > 0");
        this.minSegmentLen = v;
    }

    public OATextLineWrap withMinSegmentLen(int v) {
        setMinSegmentLen(v);
        return this;
    }

    public String getBreakChars() {
        return breakChars;
    }

    public void setBreakChars(String v) {
        this.breakChars = (v != null) ? v : "";
    }

    public OATextLineWrap withBreakChars(String v) {
        setBreakChars(v);
        return this;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String v) {
        this.separator = (v != null) ? v : "";
    }

    public OATextLineWrap withSeparator(String v) {
        setSeparator(v);
        return this;
    }
    

    /** Wraps text into rows (rows do not include the separator). */
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

    /** Returns rows joined with the configured separator (no trailing separator at the end). */
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
     * Normal row emission (not final truncation row).
     * Uses last-seen-break tracking while scanning forward by code points:
     *  - Track last whitespace or configured break char (e.g., '-')
     *  - On limit: break at last-seen; if none: forced hyphenation with minSegmentLen (append '-')
     * Returns new char-position for next row.
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
     * Final row emission when truncation is required (maxRows hit and text remains).
     * Produces a row that fits within width, then appends a smart ellipsis "..." that
     * counts against width. If the cut is mid-word and space is available, inserts a
     * space before ellipsis. Avoids duplicate period before "...".
     * Returns new char-position (end of text or where truncation finished).
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
     * Greedy final row (when NOT truncating): pack whole tokens (words separated by whitespace)
     * into a single row up to width. No hyphenation.
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

    private static boolean isWhitespace(int cp) {
        return Character.isWhitespace(cp);
    }

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


    private static String appendHyphen(String text) {
        if (text == null || text.isEmpty()) return "";
        int lastCp = text.codePointBefore(text.length());
        return (lastCp == '-') ? text : text + "-";
    }
    
    
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

    private static int getCodePointCount(String text, int startChar, int endChar) {
        return text.codePointCount(startChar, endChar);
    }

    private static int advanceByCodePoints(String text, int startChar, int count) {
        return text.offsetByCodePoints(startChar, count);
    }

    private static String rstrip(String text) {
        int i = text.length();
        while (i > 0) {
            int cp = text.codePointBefore(i);
            if (!Character.isWhitespace(cp)) break;
            i -= Character.charCount(cp);
        }
        return (i == text.length()) ? text : text.substring(0, i);
    }

    private static String trimToCodePointWidth(String text, int maxCp) {
        int cps = text.codePointCount(0, text.length());
        if (cps <= maxCp) return text;
        int end = text.offsetByCodePoints(0, maxCp);
        return text.substring(0, end);
    }
}
