package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.*;

import com.viaoa.text.OATextLineWrap;

public class OATextLineWrapTest {

    @Test
    public void testBasicAsciiWrap() {
        List<String> out = this.wrap("Hello world test", 5, "|");
        assertEquals(Arrays.asList("Hello", "world", "test"), out);

        out = this.wrap("Hello world test", 6, "|");
        assertEquals(Arrays.asList("Hello", "world", "test"), out);

        out = this.wrap("Hello world test", 10, "|");
        assertEquals(Arrays.asList("Hello", "world test"), out);

        out = this.wrap("Hello worldIsAlongOneHere test", 10, "|");
        assertEquals(Arrays.asList("Hello", "worldIsAl-", "ongOneHere", "test"), out);
    }

    @Test
    public void testBreakOnWhitespace() {
        List<String> out = this.wrap("abc def ghi", 6, "|");
        assertEquals(Arrays.asList("abc", "def", "ghi"), out);

        out = this.wrap("abc def ghi", 7, "|");
        assertEquals(Arrays.asList("abc def", "ghi"), out);
    }

    @Test
    public void testBreakOnDash() {
        OATextLineWrap w = new OATextLineWrap(6, "|");
        String out = w.wrapToString("abc-def-ghi");
        assertEquals("abc-|def-|ghi", out);
    }
    
    @Test
    public void testHyphenateLongWord() {
        List<String> out = this.wrap("supercali", 5, "|");
        // Hyphen inserted
        assertEquals("supe-", out.get(0));
        assertEquals("rcali", out.get(1));
    }
    @Test
    public void testHyphenateLongWord2() {
        List<String> out = this.wrap("supercalid", 5, "|");
        // Hyphen inserted
        assertEquals("supe-", out.get(0));
        assertEquals("rca-", out.get(1));
        assertEquals("lid", out.get(2));
    }

    @Test
    public void testSmartEllipsisCountingWidth() {
        String s = "This sentence will wrap and truncate nicely";
        String out = this.wrapToString(s, 10, "|", 2);
        String[] rows = out.split("\\|");
        assertEquals(2, rows.length);       // truncated to 2 rows
        assertTrue(rows[1].endsWith("..."));// ellipsis present
        assertTrue(rows[1].length() <= 10); // ellipsis count in width
    }

    @Test
    public void testSmartEllipsisAvoidFourDots() {
        String s = "Hello. This will be truncated.";
        String out = this.wrapToString(s, 10, "|", 2);
        String[] rows = out.split("\\|");
        assertFalse(rows[1].endsWith("....")); // never 4 dots
    }

    @Test
    public void testNoWhitespaceMidWordBreak() {
        String s = "abcdefghij12345"; 
        String out = this.wrapToString(s, 10, "|", 2);
        assertEquals("abcdefghi-|j12345", out);
        
//qqqqqqq add more that create other situations        
    }

    @Test
    public void testUnicodeEmojiSafeWrap() {
        String s = "👍👍👍👍👍hello"; // emojis count as 1 code point each
        List<String> out = this.wrap(s, 4, "|");
        assertEquals("👍👍👍👍", out.get(0));
        assertEquals("👍he-", out.get(1)); 
        assertEquals("llo", out.get(2)); 
    }

    @Test
    public void testUnicodeHyphenationSafe() {
        String s = "😀😀😀😀😀😀ABCDE";
        List<String> out = new OATextLineWrap(4, "|").wrap(s);
        assertEquals("😀😀😀😀", out.get(0));
        assertEquals("😀😀A-", out.get(1));
        assertEquals("BCDE", out.get(2));
    }

    @Test
    public void testHTMLHelper() {
        String s = "Hello world this is HTML test";
        String html = this.wrapHTML(s, 10, 3);
        assertEquals("Hello<BR>world this<BR>is HTML...", html);
    }

    @Test
    public void testMaxRowsNoTruncation() {
        String s = "a b c d";
        List<String> out = this.wrap(s, 4, "|", 3);
        // only 2 rows needed, so no ellipsis
        assertEquals(Arrays.asList("a b", "c d"), out);
    }

    @Test
    public void testMutableConfigAPI() {
        OATextLineWrap w = new OATextLineWrap(5, "|")
                .withMaxRows(2)
                .withMinSegmentLen(2);
        String out = w.wrapToString("1234567890");
        assertFalse(out.contains("..."));
        assertEquals(2, out.split("\\|").length);
        assertEquals("12345|67890", out);
    }

    @Test
    public void testNullString() {
        List<String> out = this.wrap(null, 10, "|");
        assertTrue(out.isEmpty());
    }

    @Test
    public void testEmptyString() {
        List<String> out = this.wrap("", 10, "|");
        assertTrue(out.isEmpty());
    }

    @Test
    public void testMultiCharSeparator() {
        List<String> out = this.wrap("abcd efgh ijkl", 4, " / ");
        assertEquals(Arrays.asList("abcd", "efgh", "ijkl"), out);
    }

    @Test
    public void testMinSegmentLenExactBoundary() {
        OATextLineWrap w =
            new OATextLineWrap(5, "|").withMinSegmentLen(3);
        List<String> out = w.wrap("abcde");
        assertEquals(Arrays.asList("abcde"), out);
    }

    @Test
    public void testMultipleSpacesTrimBehavior() {
        List<String> out = this.wrap("abc   def", 4, "|");
        assertEquals(Arrays.asList("abc", "def"), out);
    }

    @Test
    public void testTabsAndNewlinesAsWhitespace() {
        List<String> out = this.wrap("abc\tdef\nghi", 4, "|");
        assertEquals(Arrays.asList("abc", "def", "ghi"), out);
    }

    @Test
    public void testTrailingWhitespaceIgnored() {
        List<String> out = this.wrap("abc def   ", 4, "|");
        assertEquals(Arrays.asList("abc", "def"), out);
    }

    @Test
    public void testNoHyphenationOnNumbers() {
        List<String> out = this.wrap("123456789", 4, "|");
        assertFalse(out.get(0).endsWith("-"));
    }

    @Test
    public void testWidthOneWithMinSegmentLenOverride() {
        OATextLineWrap w =
            new OATextLineWrap(1, "|").withMinSegmentLen(2);
        List<String> out = w.wrap("abcd");
        // assertEquals(Arrays.asList("ab-", "cd"), out);
    }

    @Test
    public void testNoSeparatorProvided() {
        List<String> out = this.wrap("abcd efgh", 4, "");
        assertEquals(Arrays.asList("abcd", "efgh"), out);
    }

    @Test
    public void testSmartEllipsisCountsExactBoundary() {
        String s = "abcdefghij123";
        String out = this.wrapToString(s, 5, "|", 2);
        String[] rows = out.split("\\|");
        assertEquals(2, rows.length);
        assertTrue(rows[1].endsWith("..."));
        assertTrue(rows[1].length() <= 10);
    }

    @Test
    public void testEmojiOnlyStringNoHyphens() {
        String s = "😀😀😀😀😀😀";
        List<String> out = this.wrap(s, 3, "|");
        assertEquals("😀😀😀", out.get(0));
        assertEquals("😀😀😀", out.get(1));
    }

    @Test
    public void testBreakPrefersWhitespaceOverDash() {
        List<String> out = this.wrap("abc-def ghi", 6, "|");
        assertEquals(Arrays.asList("abc-", "def", "ghi"), out);
    }

    @Test
    public void testWhitespaceNextToDash() {
        List<String> out =
            this.wrap("abc- def ghi", 6, "|");
        assertEquals(Arrays.asList("abc-", "def", "ghi"), out);
    }

    @Test
    public void testConfigFluentAPIChaining() {
        String out = new OATextLineWrap(6, "|")
            .withMaxRows(1)
            .withMinSegmentLen(3)
            .wrapToString("ABC DEF GHI");
        assertTrue(out.endsWith("..."));
    }

    @Test
    public void testNoBreakCharInLongWordNoHyphenIfShorterThanMinSegment() {
        String out = new OATextLineWrap(10, "|")
            .withMinSegmentLen(5)
            .wrapToString("abcd");
        assertEquals("abcd", out);
    }


    @Test
    public void fuzzTestRandomInputs() {
        final int ITERATIONS = 5000;
        Random rand = new Random(12345); // deterministic runs
        
        for (int i = 0; i < ITERATIONS; i++) {
            // Random input string of random length 0–200
            int len = rand.nextInt(200);
            StringBuilder sb = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                int mode = rand.nextInt(5);
                switch (mode) {
                    case 0: sb.append((char) ('a' + rand.nextInt(26))); break;
                    case 1: sb.append((char) ('A' + rand.nextInt(26))); break;
                    case 2: sb.append((char) ('0' + rand.nextInt(10))); break;
                    case 3: sb.append(' '); break; // whitespace
                    default: sb.append('-'); break; // dash break char
                }
            }
            String text = sb.toString().trim();

            // Random width (but valid!)
            int width = 3 + rand.nextInt(30);

            // Key invariant tests:

            List<String> rows = this.wrap(text, width, "|");

            // 1 No row should exceed configured width
            for (String r : rows) {
                assertTrue(r.length() <= width, "Row width exceeded");
            }

            // 2 No row should be empty unless input was empty
            if (!text.isEmpty()) {
                for (String r : rows) {
                    assertFalse(r.isEmpty(), "Unexpected empty row");
                }
            }

            // 3 Should not append hyphen after whitespace-only rows
            for (String r : rows) {
                assertFalse(r.trim().isEmpty() && r.endsWith("-"), "Bad trailing hyphen on whitespace");
            }

            // 4) Reconstructing from rows should contain all visible characters (ignoring breaks & whitespace)
            String combined = String.join("", rows)
                    .replace("-", "")          // ignore forced/kept hyphens at breaks
                    .replaceAll("\\s+", "");   // ignore all whitespace (space, tab, newline)

            String originalNoBreaks = text
                    .replace("-", "")          // ignore original hyphens for this invariant
                    .replaceAll("\\s+", "");   // ignore all whitespace

            assertTrue(combined.contains(originalNoBreaks) || originalNoBreaks.contains(combined), "Content mismatch after wrapping");
            
            
        }
    }

    @Test
    public void testForcedHyphenationRespectsMinSegmentLen() {
        OATextLineWrap w = new OATextLineWrap(4, "|").withMinSegmentLen(3);
        List<String> out = w.wrap("abcdefgh");
        assertEquals(Arrays.asList("abc-", "def-", "gh"), out);
    }

    @Test
    public void testNoHyphenationOnSingleShortWord() {
        List<String> out = this.wrap("abcd", 10, "|");
        assertEquals(Arrays.asList("abcd"), out);
    }

    @Test
    public void testOnlyWhitespaceString() {
        List<String> out = this.wrap("     ", 3, "|");
        assertTrue(out.isEmpty());
    }

    @Test
    public void testSingleBreakCharAtBoundary() {
        List<String> out = this.wrap("abc-def", 4, "|");
        assertEquals(Arrays.asList("abc-", "def"), out);
    }

    @Test
    public void testBreakOnConfiguredBreakCharsNotDash() {
        OATextLineWrap w = new OATextLineWrap(5, "|").withBreakChars(",;");
        List<String> out = w.wrap("abc,def;ghi");
        assertEquals(Arrays.asList("abc,", "def;", "ghi"), out);
    }

    @Test
    public void testSurrogatePairLengthOneCodePoint() {
        String s = "😀ab😀cd😀"; // mixed emoji + ascii
        List<String> out = this.wrap(s, 3, "|");
        assertEquals("😀a-", out.get(0));
        assertEquals("b😀-", out.get(1));
        assertEquals("cd😀", out.get(2));
    }

    @Test
    public void testMaxRowsExactFitNoEllipsis() {
        String s = "abc def ghi jkl";
        List<String> out = this.wrap(s, 7, "|", 3);
        assertEquals(Arrays.asList("abc def", "ghi jkl"), out);
    }

    @Test
    public void testTruncationRemovesDotBeforeEllipsis() {
        String s = "Hello.";
        String out = this.wrapToString(s, 5, "|", 1);
        assertEquals("He...", out);
    }

    @Test
    public void testSeparatorNotCountedAgainstWidth() {
        List<String> out = this.wrap("abcdef", 3, "|");
        assertEquals(Arrays.asList("ab-", "cd-", "ef"), out);
        // Ensure internal width constraint was respected
        assertEquals(3, out.get(0).length());
        assertEquals(3, out.get(1).length());
    }

    @Test
    public void testMultipleBreakCharsPreferenceWhitespace() {
        OATextLineWrap w = new OATextLineWrap(5, "|").withBreakChars("-_,");
        List<String> out = w.wrap("ab_cd ef-gh");
        assertEquals(Arrays.asList("ab_cd", "ef-gh"), out);
    }



    
    // Helper
    public List<String> wrap(String text, int columnWidth, String separator) {
		OATextLineWrap lw = (new OATextLineWrap(columnWidth, separator));
		return lw.wrap(text);
	}

    public List<String> wrap(String text, int columnWidth, String separator, int maxRows) {
		OATextLineWrap lw = (new OATextLineWrap(columnWidth, separator)).withMaxRows(maxRows);
		return lw.wrap(text);
	}
    
	protected String wrapToString(String text, int columnWidth, String separator) {
		return wrapToString(text, columnWidth, separator, -1);
	}

	protected String wrapHTML(String text, int columnWidth, int maxRows) {
		return wrapToString(text, columnWidth, "<BR>", maxRows);
	}

	protected String wrapToString(String text, int columnWidth, String separator, int maxRows) {
		List<String> al = wrap(text, columnWidth, separator, maxRows);
		if (al == null) return "";
		int x = al.size();
		if (x == 0) return "";
		if (x == 1) return al.get(0);
		StringBuilder sb = new StringBuilder(text.length() + 32);
		for (String s : al) {
			if (sb.length() > 0) sb.append(separator);
			sb.append(s);
		}
		return sb.toString();
	}
    
    
    
}
