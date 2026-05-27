package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OAConverterNumberTest {

    private enum Sample {
        FIRST, SECOND
    }

    @Test
    void nullAndEmptyStringConvertToZero() {
        assertEquals(Double.valueOf(0.0d), OAConverter.convert(Number.class, null));
        assertEquals(Integer.valueOf(0), OAConverter.convert(Integer.class, ""));
    }

    @ParameterizedTest
    @CsvSource({ "42,42", "-7,-7", "4.25,4.25" })
    void validIntegerAndDecimalStringsParse(String value, double expected) {
        assertEquals(expected, OAConverter.convert(Double.class, value));
    }

    @ParameterizedTest
    @CsvSource({ "'1,234',1234", "'$1,234',1234", "' 1 234 ',1234" })
    void groupingCurrencyAndWhitespaceParsingIsSupported(String value, int expected) {
        assertEquals(Integer.valueOf(expected), OAConverter.convert(Integer.class, value));
    }

    @ParameterizedTest
    @CsvSource({ "5k,5000", "3M,3000000" })
    void numericSuffixParsingContract(String value, int expected) {
        assertEquals(Integer.valueOf(expected), OAConverter.convert(Integer.class, value));
        assertEquals(Double.valueOf(expected), OAConverter.convert(Double.class, value));
    }

    @Test
    void decimalSuffixParsingCurrentlyStopsBeforeSuffixMultiplier() {
        assertEquals(Integer.valueOf(3), OAConverter.convert(Integer.class, "3.2M"));
        assertEquals(Double.valueOf(3.2d), OAConverter.convert(Double.class, "3.2M"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1x", "--1" })
    void invalidNumericStringsReturnNullThroughCentralConvert(String value) {
        assertNull(OAConverter.convert(Integer.class, value));
        assertNull(OAConverter.convert(Double.class, value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1x", "--1" })
    void primitiveHelpersThrowOnInvalidNonNullInput(String value) {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toInt(value));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toDouble(value));
    }

    @Test
    void booleanToNumberUsesOneAndZero() {
        assertEquals(Integer.valueOf(1), OAConverter.convert(Integer.class, true));
        assertEquals(Integer.valueOf(0), OAConverter.convert(Integer.class, false));
    }

    @Test
    void characterToNumberUsesCodePoint() {
        assertEquals(Integer.valueOf(65), OAConverter.convert(Integer.class, 'A'));
    }

    @Test
    void enumToNumberUsesOrdinal() {
        assertEquals(Integer.valueOf(1), OAConverter.convert(Integer.class, Sample.SECOND));
    }

    @Test
    void narrowingConversionsUseCurrentJavaPrimitiveValueBehavior() {
        assertEquals(Byte.valueOf((byte) 130), OAConverter.convert(Byte.class, 130));
        assertEquals(Integer.valueOf(4), OAConverter.convert(Integer.class, 4.75d));
        assertEquals(Short.valueOf((short) 32768), OAConverter.convert(Short.class, 32768));
    }
}
