package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextFormat. */
public class OATextFormatTest {
    @Test
    public void fmtTest() {
        // null or empty format returns usable output
        assertNotNull(OATextFormat.fmt("abc", null));
        // left alignment format executes
        assertNotNull(OATextFormat.fmt("abc", "5L"));
        // right numeric format executes
        assertNotNull(OATextFormat.fmt("123.5", "8R00"));
        // mask format executes
        assertNotNull(OATextFormat.fmt("1234567890", "13  R((###)###-####)"));
    }

    @Test
    public void getNumberOfDecimalPlacesTest() {
        // integer has no decimals
        assertEquals(0, OATextFormat.getNumberOfDecimalPlaces("123", false));
        // decimal places are counted
        assertEquals(2, OATextFormat.getNumberOfDecimalPlaces("123.45", false));
        // trailing zeros can be ignored
        assertEquals(1, OATextFormat.getNumberOfDecimalPlaces("123.40", true));
        // null is safe
        assertEquals(0, OATextFormat.getNumberOfDecimalPlaces(null, false));
    }

    @Test
    public void isNumberTest() {
        // integer string
        assertTrue(OATextFormat.isNumber("123"));
        // decimal string
        assertTrue(OATextFormat.isNumber("123.45"));
        // non-number string
        assertFalse(OATextFormat.isNumber("abc"));
        // null input
        assertFalse(OATextFormat.isNumber(null));
    }

    @Test
    public void isIntegerTest() {
        // integer string
        assertTrue(OATextFormat.isInteger("123"));
        // signed integer
        assertTrue(OATextFormat.isInteger("-123"));
        // non-number string
        assertFalse(OATextFormat.isInteger("abc"));
        // null input
        assertFalse(OATextFormat.isInteger(null));
    }

    @Test
    public void isDateTest() {
        // common date format executes safely
        assertDoesNotThrow(() -> OATextFormat.isDate("01/02/2025"));
        // non-date string
        assertFalse(OATextFormat.isDate("abc"));
        // null input
        assertFalse(OATextFormat.isDate(null));
    }

    @Test
    public void isTimeTest() {
        // common time format executes safely
        assertDoesNotThrow(() -> OATextFormat.isTime("12:30"));
        // non-time string
        assertFalse(OATextFormat.isTime("abc"));
        // null input
        assertFalse(OATextFormat.isTime(null));
    }

    @Test
    public void isDateTimeTest() {
        // common datetime format executes safely
        assertDoesNotThrow(() -> OATextFormat.isDateTime("01/02/2025 12:30"));
        // non-datetime string
        assertFalse(OATextFormat.isDateTime("abc"));
        // null input
        assertFalse(OATextFormat.isDateTime(null));
    }

    @Test
    public void maskTest() {
        // left-to-right mask
        assertNotNull(OATextFormat.mask("1234567890", "(###)###-####", false));
        // right-justified mask
        assertNotNull(OATextFormat.mask("1234567890", "(###)###-####", true));
        // null value is safe
        assertDoesNotThrow(() -> OATextFormat.mask(null, "###", false));
    }

    @Test
    public void toNumberStringTest() {
        // zero
        assertNotNull(OATextFormat.toNumberString(0));
        // positive number
        assertNotNull(OATextFormat.toNumberString(123));
        // negative number
        assertNotNull(OATextFormat.toNumberString(-123));
    }

    @Test
    public void convertToValidPhoneNumberTest() {
        // strips formatting to a valid phone representation
        assertNotNull(OATextFormat.convertToValidPhoneNumber("(123) 456-7890"));
        // digits only
        assertNotNull(OATextFormat.convertToValidPhoneNumber("1234567890"));
        // null input
        assertNull(OATextFormat.convertToValidPhoneNumber(null));
    }

    @Test
    public void indentTest() {
        // indent one line
        assertEquals("  abc", OATextFormat.indent("abc", 2));
        // indent multiple lines
        assertTrue(OATextFormat.indent("a\nb", 2).contains("\n  b"));
        // zero amount unchanged
        assertEquals("abc", OATextFormat.indent("abc", 0));
    }

    @Test
    public void unindentTest() {
        // unindent one line
        assertEquals("abc", OATextFormat.unindent("  abc"));
        // unindent with first-line option
        assertEquals("abc", OATextFormat.unindent("  abc", true));
        // no leading spaces unchanged
        assertEquals("abc", OATextFormat.unindent("abc"));
    }

    @Test
    public void unindentCodeTest() {
        // code-style unindent executes
        assertNotNull(OATextFormat.unindentCode("  abc\n  def"));
        // no indent unchanged or safe
        assertNotNull(OATextFormat.unindentCode("abc"));
        // empty string
        assertNotNull(OATextFormat.unindentCode(""));
    }

    @Test
    public void trimEndingWhitespaceTest() {
        // trailing whitespace removed
        assertEquals("abc", OATextFormat.trimEndingWhitespace("abc  "));
        // internal whitespace preserved
        assertEquals("a b", OATextFormat.trimEndingWhitespace("a b  "));
        // null input
        assertNull(OATextFormat.trimEndingWhitespace(null));
    }

    @Test
    public void trimWhitespaceTest() {
        // leading and trailing whitespace removed
        assertEquals("abc", OATextFormat.trimWhitespace("  abc  "));
        // internal whitespace preserved
        assertEquals("a b", OATextFormat.trimWhitespace(" a b "));
        // null input
        assertNull(OATextFormat.trimWhitespace(null));
    }

    @Test
    public void convertToCamelCaseTest() {
        // space separated value
        assertEquals("firstName", OATextFormat.convertToCamelCase("first name"));
        // custom separator chars
        assertEquals("firstName", OATextFormat.convertToCamelCase("first_name", "_"));
        // already camel case remains usable
        assertNotNull(OATextFormat.convertToCamelCase("firstName"));
        // null input
        assertNull(OATextFormat.convertToCamelCase(null));
    }

    @Test
    public void convertToHungarianTest() {
        // space separated value
        assertEquals("firstName", OATextFormat.convertToHungarian("first name"));
        // custom separator chars
        assertEquals("firstName", OATextFormat.convertToHungarian("first_name", "_"));
        // already mixed value remains usable
        assertNotNull(OATextFormat.convertToHungarian("firstName"));
        // null input
        assertNull(OATextFormat.convertToHungarian(null));
    }

    @Test
    public void toUtf8Test() {
        // ASCII remains usable
        assertNotNull(OATextFormat.toUtf8("abc"));
        // empty string
        assertNotNull(OATextFormat.toUtf8(""));
        // null input
        assertNull(OATextFormat.toUtf8(null));
    }

    @Test
    public void toUTF8Test() {
        // deprecated/legacy capitalization overload remains usable
        assertNotNull(OATextFormat.toUTF8("abc"));
        // empty string
        assertNotNull(OATextFormat.toUTF8(""));
        // null input
        assertNull(OATextFormat.toUTF8(null));
    }
}
