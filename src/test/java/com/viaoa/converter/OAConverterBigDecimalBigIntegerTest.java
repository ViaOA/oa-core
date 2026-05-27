package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAConverterBigDecimalBigIntegerTest {

    @Test
    void nullReturnsZero() {
        assertEquals(BigDecimal.ZERO, OAConverter.convert(BigDecimal.class, null));
        assertEquals(BigInteger.ZERO, OAConverter.convert(BigInteger.class, null));
    }

    @Test
    void bigDecimalFromStringPreservesTypicalDecimalPrecision() {
        assertEquals(new BigDecimal("12345.6789"), OAConverter.convert(BigDecimal.class, "12345.6789"));
    }

    @Test
    void bigDecimalStringPreservesVerySmallPrecision() {
        BigDecimal value = OAConverter.convert(BigDecimal.class, "0.000000000000000000123");

        assertEquals(new BigDecimal("0.000000000000000000123"), value);
        assertEquals(21, value.scale());
    }

    @Test
    void bigDecimalStringVeryLargePrecisionCurrentlyRoutesThroughDoublePrecision() {
        BigDecimal exact = new BigDecimal("12345678901234567890.123456789");
        BigDecimal value = OAConverter.convert(BigDecimal.class, "12345678901234567890.123456789");

        assertNotEquals(exact, value);
        assertEquals(new BigDecimal("1.2345678901234567E+19"), value);
        assertEquals("12345678901234567000", value.toPlainString());
    }

    @Test
    void bigDecimalFromBigIntegerIsExact() {
        BigInteger value = new BigInteger("1234567890123456789");

        assertEquals(new BigDecimal(value), OAConverter.convert(BigDecimal.class, value));
    }

    @Test
    void bigDecimalFromDoubleUsesBigDecimalValueOfBehavior() {
        assertEquals(BigDecimal.valueOf(0.1d), OAConverter.convert(BigDecimal.class, 0.1d));
    }

    @Test
    void bigIntegerFromIntegerAndLongIsExact() {
        assertEquals(BigInteger.valueOf(42), OAConverter.convert(BigInteger.class, 42));
        assertEquals(BigInteger.valueOf(1234567890123L), OAConverter.convert(BigInteger.class, 1234567890123L));
    }

    @Test
    void bigIntegerFromDecimalInputTruncatesUsingCurrentLongValueBehavior() {
        assertEquals(BigInteger.valueOf(12), OAConverter.convert(BigInteger.class, "12.9"));
        assertEquals(BigInteger.valueOf(-12), OAConverter.convert(BigInteger.class, "-12.9"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1x", "--1" })
    void invalidBigDecimalAndBigIntegerStringsReturnNullThroughCentralConvert(String value) {
        assertNull(OAConverter.convert(BigDecimal.class, value));
        assertNull(OAConverter.convert(BigInteger.class, value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1x", "--1" })
    void invalidBigDecimalAndBigIntegerHelpersThrow(String value) {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toBigDecimal(value, null));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toBigInteger(value));
    }

    @Test
    void toBDAliasesMatchToBigDecimal() {
        assertEquals(OAConverter.toBigDecimal("123.45"), OAConverter.toBD("123.45"));
        assertEquals(OAConverter.toBigDecimal(12.345d), OAConverter.toBD(12.345d));
        assertEquals(OAConverter.toBigDecimal(12.345d, 2), OAConverter.toBD(12.345d, 2));
        assertEquals(OAConverter.toBigDecimal("123.45", null), OAConverter.toBD("123.45", null));
    }
}
