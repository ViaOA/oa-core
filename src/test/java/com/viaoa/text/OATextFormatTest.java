package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextFormatTest {

    @Test
    void fmtSupportsDocumentedAlignmentAndEllipsisExamples() {
        assertEquals("Custo...", OATextFormat.fmt("CustomerName", "8L."));
        assertEquals("...omer", OATextFormat.fmt("CustomerName", "7R."));
        assertEquals("00000123", OATextFormat.fmt("123.5", "8R00"));
    }

    @Test
    void fmtSupportsNumericAndMaskExamples() {
        assertEquals("1,234.5000", OATextFormat.fmt("1234.5", "R4,"));
        assertEquals("123", OATextFormat.fmt("123.5", "R00"));
        assertEquals("(123)123-1234  ", OATextFormat.fmt("1231231234", "13  R((###)###-####)"));
    }

    @Test
    void isNumberAndIntegerDocumentCurrentConverterContracts() {
        assertTrue(OATextFormat.isNumber("1.2"));
        assertTrue(OATextFormat.isInteger("123"));

        // Current behavior: Long conversion accepts decimal strings by truncating.
        assertTrue(OATextFormat.isInteger("1.2"));
    }

    @Test
    void getNumberOfDecimalPlacesHandlesTrailingZeros() {
        assertEquals(4, OATextFormat.getNumberOfDecimalPlaces("1.2300", false));
        assertEquals(2, OATextFormat.getNumberOfDecimalPlaces("1.2300", true));
        assertEquals(0, OATextFormat.getNumberOfDecimalPlaces("123", true));
        assertEquals(0, OATextFormat.getNumberOfDecimalPlaces("1.2x", true));
    }

    @Test
    void indentIsNullSafeButDropsTrailingEmptyLine() {
        assertEquals("  ", OATextFormat.indent(null, 2));
        assertEquals("  a", OATextFormat.indent("a\n", 2));
        assertEquals("  a\n  b", OATextFormat.indent("a\nb", 2));
    }

    @Test
    void unindentCurrentlyThrowsOnNull() {
        assertThrows(NullPointerException.class, () -> OATextFormat.unindent(null));
    }

    @Test
    void unindentRemovesLeadingSpaces() {
        assertEquals("a\nb", OATextFormat.unindent("  a\n    b"));
        assertEquals("a\n  b", OATextFormat.unindentCode("  a\n    b"));
    }

    @Test
    void phoneNumberNormalizationKeepsDigitsAndPadsLeft() {
        assertEquals("1234567890", OATextFormat.convertToValidPhoneNumber("(123)456-7890"));
        assertEquals("       123", OATextFormat.convertToValidPhoneNumber("123"));
        assertNull(OATextFormat.convertToValidPhoneNumber(null));
    }

    @Test
    void toUtf8DocumentsCurrentCharsetConversionBehavior() {
        assertNull(OATextFormat.toUTF8(null));
        assertNotEquals("é", OATextFormat.toUTF8("é"));
    }
}
