package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class OAConverterStringTest {

    @Test
    void toStringNullReturnsEmptyString() {
        assertEquals("", OAConverter.toString(null));
    }

    @Test
    void convertStringClassNullReturnsEmptyString() {
        assertEquals("", OAConverter.convert(String.class, null));
    }

    @Test
    void numericToStringUsesNumberConverter() {
        assertEquals("42", OAConverter.convert(String.class, 42));
        assertEquals("4.25", OAConverter.convert(String.class, 4.25d));
    }

    @Test
    void booleanToStringUsesBooleanConverter() {
        assertEquals("true", OAConverter.convert(String.class, true));
        assertEquals("false", OAConverter.convert(String.class, false));
    }

    @Test
    void characterToStringUsesCharacterConverter() {
        assertEquals("x", OAConverter.convert(String.class, 'x'));
    }

    @Test
    void byteArrayToStringUsesUtf8() {
        byte[] bytes = "abc\u20ac".getBytes(StandardCharsets.UTF_8);

        assertEquals("abc\u20ac", OAConverter.convert(String.class, bytes));
    }

    @Test
    void explicitNumericFormatMaskIsApplied() {
        assertEquals("0042", OAConverter.convert(String.class, 42, "0000"));
    }

    @Test
    void explicitFormatBypassesAssignableFastPath() {
        String value = new String("CustomerName");

        assertEquals("Custo...", OAConverter.convert(String.class, value, "8L."));
    }

    @Test
    void explicitFormatUsesConverterFormatting() {
        assertEquals("0042", OAConverter.convert(String.class, 42, "0000"));
        assertEquals("yes", OAConverter.convert(String.class, true, "yes;no;maybe"));
    }

    @Test
    void centralToStringNeverReturnsNull() {
        assertNotNull(OAConverter.toString(null));
        assertNotNull(OAConverter.toString(new Object()));
        assertNotNull(OAConverter.toString((Boolean) null, "yes;no;maybe"));
    }
}
