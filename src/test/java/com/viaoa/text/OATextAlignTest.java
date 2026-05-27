package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextAlignTest {

    @Test
    void alignLeftPadsOrTruncatesToRequestedWidth() {
        assertEquals("abc  ", OATextAlign.alignLeft("abc", 5, ' '));
        assertEquals("abc", OATextAlign.alignLeft("abcdef", 3, ' '));
        assertEquals("", OATextAlign.alignLeft("abc", 0, ' '));
        assertEquals("     ", OATextAlign.alignLeft(null, 5, ' '));
    }

    @Test
    void alignRightPadsOrTruncatesToRequestedWidth() {
        assertEquals("  abc", OATextAlign.alignRight("abc", 5, ' '));
        assertEquals("def", OATextAlign.alignRight("abcdef", 3, ' '));
        assertEquals("", OATextAlign.alignRight("abc", 0, ' '));
        assertEquals(".....", OATextAlign.alignRight(null, 5, '.'));
    }

    @Test
    void alignCenterPadsOrTruncatesToRequestedWidth() {
        assertEquals(" abc ", OATextAlign.alignCenter("abc", 5, ' '));
        assertEquals("bcd", OATextAlign.alignCenter("abcdef", 3, ' '));
        assertEquals("", OATextAlign.alignCenter("abc", 0, ' '));
    }

    @Test
    void ellipsisTruncationRespectsRequestedWidth() {
        assertEquals("Cu...", OATextAlign.alignLeft("CustomerName", 5, ' ', true));
        assertEquals("...me", OATextAlign.alignRight("CustomerName", 5, ' ', true));
        assertEquals("...", OATextAlign.alignLeft("CustomerName", 3, ' ', true));
        assertEquals("Cu", OATextAlign.alignLeft("CustomerName", 2, ' ', true));
    }

    @Test
    void padStartAndPadEndAddAmountNotTargetWidthByCurrentContract() {
        assertEquals("  abc", OATextAlign.padStart("abc", 2));
        assertEquals("abc  ", OATextAlign.padEnd("abc", 2));
        assertEquals("xxabc", OATextAlign.padStart("abc", 2, 'x'));
        assertEquals("abcxx", OATextAlign.padEnd("abc", 2, 'x'));
    }

    @Test
    void leftRightAndCenterSubstringHelpersUseCurrentSubstringContract() {
        assertEquals("abc", OATextAlign.left("abcdef", 3));

        // Current OATextAlign.right passes start and amount into OATextFilter.substring(start, end),
        // so amount is treated as an end index. This documents current behavior.
        assertEquals("", OATextAlign.right("abcdef", 2));

        // Same current-contract issue for center helper.
        assertEquals("c", OATextAlign.center("abcdef", 2));
    }

    @Test
    void unicodeEmojiAreNotSplitByAlignmentCore() {
        String emoji = "\uD83D\uDE00";
        assertEquals(emoji, OATextAlign.alignLeft(emoji + "x", 1, ' '));
        assertEquals(" " + emoji, OATextAlign.alignRight(emoji, 2, ' '));
    }
}
