package com.viaoa.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;
import org.junit.Ignore;

public class OAConverterTest {

    // =========================
    // Numeric Tests
    // =========================
    @Test
    public void testDoubleToBigDecimalPrecision() {
        double d = 0.1;
        BigDecimal bd = OAConverter.toBigDecimal(d);
        assertEquals(BigDecimal.valueOf(d), bd);
    }

    @Test
    public void testStringToBigDecimal() {
        BigDecimal bd = OAConverter.toBigDecimal("123.45");
        assertEquals(new BigDecimal("123.45"), bd);
    }

    @Test(expected = Exception.class)
    public void testInvalidStringToBigDecimalThrows() {
        OAConverter.toBigDecimal("ABC");
    }

    @Test
    public void testIntToLong() {
        assertEquals(123L, OAConverter.toLong(123));
    }

    @Test
    public void testNullToIntReturnsZero() {
        assertEquals(0, OAConverter.toInt(null));
    }

    @Test
    public void testConvertToIntFromString() {
        assertEquals(42, OAConverter.toInt("42"));
    }

    @Test(expected = Exception.class)
    public void testInvalidIntThrows() {
        OAConverter.toInt("x9");
    }


    // =========================
    // Boolean Tests
    // =========================
    @Test
    public void testBooleanTrueValues() {
        assertTrue(OAConverter.toBoolean("true"));
        assertTrue(OAConverter.toBoolean("Y"));
        assertTrue(OAConverter.toBoolean("yes"));
        assertTrue(OAConverter.toBoolean("1"));
    }

    @Test
    public void testBooleanFalseValues() {
        assertFalse(OAConverter.toBoolean("false"));
        assertFalse(OAConverter.toBoolean("no"));
        assertFalse(OAConverter.toBoolean("0"));
    }

    // @Test(expected = Exception.class)
    public void testInvalidBooleanThrows() {
        OAConverter.toBoolean("maybe");
    }


    // =========================
    // Date / Time Tests
    // =========================
    @Test
    public void testLocalDateConversionExactFormat() {
        LocalDate d = LocalDate.of(2025, 10, 29);
        String s = OAConverter.toString(d);

        assertEquals("10/29/2025", s);

        LocalDate parsed = OAConverter.convert(LocalDate.class, s);
        assertEquals(d, parsed);
    }

    @Test
    public void testLocalTimeConversionExactFormat() {
        LocalTime t = LocalTime.of(13, 45, 30);
        String s = OAConverter.toString(t, "HH:mm:ss");

        assertEquals("13:45:30", s);

        LocalTime parsed = OAConverter.convert(LocalTime.class, s);
        assertEquals(t, parsed);
    }

    @Test
    public void testLocalDateTimeConversionExactFormat() {
        LocalDateTime dt = LocalDateTime.of(2025, 10, 29, 13, 45, 30);
        String s = OAConverter.toString(dt, "MM/dd/yyyy HH:mm:ss");

        assertEquals("10/29/2025 13:45:30", s);

        LocalDateTime parsed = OAConverter.convert(LocalDateTime.class, s);
        assertEquals(dt, parsed);
    }


    // =========================
    // String Conversion Tests
    // =========================
    @Test
    public void testNullToString() {
        assertEquals("", OAConverter.toString(null));
    }

    @Test
    public void testNumberToString() {
        assertEquals("123", OAConverter.toString(123));
    }


    // =========================
    // Empty Checks
    // =========================
    @Test
    public void testNullIsEmpty() {
        assertTrue(OAConverter.isEmpty(null));
    }

    @Test
    public void testWhitespaceEmpty() {
        assertTrue(OAConverter.isEmpty("   "));
        assertTrue(OAConverter.isEmpty("   ", true));
        assertFalse(OAConverter.isEmpty("   ", false));
    }
    
    @Test
    public void testStringNotEmpty() {
        assertFalse(OAConverter.isEmpty("x"));
    }

    @Test
    public void testArrayEmpty() {
        assertTrue(OAConverter.isEmpty(new Object[]{}));
    }

    @Test
    public void testArrayNotEmpty() {
        assertFalse(OAConverter.isEmpty(new Object[]{1}));
    }
}
