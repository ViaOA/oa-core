package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class OATextFinalCoverageTest {

    @Test
    void utilConcatDocumentsNullSeparatorCurrentBehavior() {
        assertEquals("anullb", OATextUtil.concat("a", "b", null));
        assertEquals("a b", OATextUtil.concat("a", "b"));
        assertEquals("", OATextUtil.concat(null, null));
    }

    @Test
    void utilColorToHexIncludesAlphaByCurrentContract() {
        assertEquals("#010203FF", OATextUtil.colorToHex(new Color(1, 2, 3)));
        assertEquals("#01020304", OATextUtil.colorToHex(new Color(1, 2, 3, 4)));
        assertNull(OATextUtil.colorToHex(null));
    }

    @Test
    void utilMakeJavaIdentifierAllowsLeadingDigitByCurrentContract() {
        assertEquals("a_b", OATextUtil.makeJavaIdentifier("a-b"));
        assertEquals("1abc", OATextUtil.makeJavaIdentifier("1abc"));
        assertNull(OATextUtil.makeJavaIdentifier(null));
    }

    @Test
    void utilLikeSearchConvertsStarAndAddsTrailingPercent() {
        assertEquals("abc%", OATextUtil.convertToLikeSearch("abc"));
        assertEquals("ab%c", OATextUtil.convertToLikeSearch("ab*c"));
        assertEquals("ab%c%", OATextUtil.convertToLikeSearch("ab*c*"));
        assertNull(OATextUtil.convertToLikeSearch(null));
    }

    @Test
    void tokenizerCountAndDcountContractsAreDistinct() {
        assertEquals(2, OATextTokenizer.count("a,b,c", ","));
        assertEquals(3, OATextTokenizer.dcount("a,b,c", ","));
        assertEquals(3, OATextTokenizer.countMatches("a,b,c", ","));
    }

    @Test
    void tokenizerFieldAtUsesZeroBasedWrapperOverLegacyPickFields() {
        assertEquals("a", OATextTokenizer.fieldAt("a,b,c", ",", 0));
        assertEquals("b", OATextTokenizer.fieldAt("a,b,c", ",", 1));
        assertEquals("b,c", OATextTokenizer.fieldAt("a,b,c", ",", 1, -1));
        assertNull(OATextTokenizer.fieldAt("a,b,c", ",", 5));
    }

    @Test
    void formatCamelAndHungarianConversionsAreDeterministic() {
        assertEquals("yourNameTest", OATextFormat.convertToCamelCase("your name test"));
        assertEquals("YourNameTest", OATextFormat.convertToCamelCase("Your_name_test"));
        assertEquals("a1_2B", OATextFormat.convertToHungarian("a1 2 b"));
        assertNull(OATextFormat.convertToCamelCase(null));
    }
}
