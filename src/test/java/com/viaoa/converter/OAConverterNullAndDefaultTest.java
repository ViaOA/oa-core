package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAConverterNullAndDefaultTest {

    @Test
    void nullToStringReturnsEmptyString() {
        assertEquals("", OAConverter.toString(null));
        assertEquals("", OAConverter.convert(String.class, null));
    }

    @Test
    void nullToPrimitiveHelpersReturnsDocumentedDefaults() {
        assertEquals(0.0d, OAConverter.toDouble(null));
        assertEquals(0.0f, OAConverter.toFloat(null));
        assertEquals(0L, OAConverter.toLong(null));
        assertEquals(0, OAConverter.toInt(null));
        assertEquals((short) 0, OAConverter.toShort(null));
        assertEquals((byte) 0, OAConverter.toByte(null));
        assertFalse(OAConverter.toBoolean(null));
        assertEquals((char) 0, OAConverter.toChar(null));
    }

    @Test
    void nullToNumericObjectTargetsReturnsZero() {
        assertEquals(Double.valueOf(0.0d), OAConverter.convert(Number.class, null));
        assertEquals(Integer.valueOf(0), OAConverter.convert(Integer.class, null));
        assertEquals(Long.valueOf(0L), OAConverter.convert(Long.class, null));
        assertEquals(Short.valueOf((short) 0), OAConverter.convert(Short.class, null));
        assertEquals(Byte.valueOf((byte) 0), OAConverter.convert(Byte.class, null));
        assertEquals(Float.valueOf(0.0f), OAConverter.convert(Float.class, null));
        assertEquals(Double.valueOf(0.0d), OAConverter.convert(Double.class, null));
        assertEquals(BigDecimal.ZERO, OAConverter.convert(BigDecimal.class, null));
        assertEquals(BigInteger.ZERO, OAConverter.convert(BigInteger.class, null));
    }

    @Test
    void nullToBooleanReturnsFalse() {
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, null));
        assertEquals(Boolean.FALSE, OAConverter.convert(boolean.class, null));
        assertFalse(OAConverter.toBoolean(null));
    }

    @Test
    void nullToCharacterHelperReturnsZeroChar() {
        assertNull(OAConverter.convert(Character.class, null));
        assertNull(OAConverter.convert(char.class, null));
        assertEquals((char) 0, OAConverter.toChar(null));
    }

    @Test
    void nullToTemporalObjectTargetsReturnsNull() {
        assertNull(OAConverter.convert(OADate.class, null));
        assertNull(OAConverter.convert(OADateTime.class, null));
        assertNull(OAConverter.convert(OATime.class, null));
        assertNull(OAConverter.convert(java.util.Date.class, null));
        assertNull(OAConverter.convert(java.sql.Date.class, null));
        assertNull(OAConverter.convert(java.sql.Time.class, null));
        assertNull(OAConverter.convert(java.sql.Timestamp.class, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1x", "--1" })
    void invalidNumberHelperThrows(String value) {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toInt(value));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toLong(value));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toDouble(value));
    }

    @Test
    void invalidBooleanHelperThrowsIfCurrentContractRequires() {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toBoolean("maybe", "yes;no"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ab" })
    void invalidCharacterHelperThrowsIfCurrentContractRequires(String value) {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toChar(value));
    }
}
