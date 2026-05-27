package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OAConverterFinalCoverageTest {

    @Test
    void getConverterNullReturnsNullAndPrimitivesUseWrapperConverters() {
        assertNull(OAConverter.getConverter(null));
        assertSame(OAConverter.getConverter(Integer.class), OAConverter.getConverter(int.class));
        assertSame(OAConverter.getConverter(Boolean.class), OAConverter.getConverter(boolean.class));
        assertSame(OAConverter.getConverter(Character.class), OAConverter.getConverter(char.class));
    }

    @Test
    void convertNullTargetReturnsNull() {
        assertNull(OAConverter.convert(null, "value"));
        assertNull(OAConverter.convert(null, "value", "fmt"));
    }

    @Test
    void centralInvalidInputReturnsNullForRepresentativeConverters() {
        assertNull(OAConverter.convert(Integer.class, "not-a-number"));
        assertNull(OAConverter.convert(BigDecimal.class, new Object()));
        assertNull(OAConverter.convert(java.util.Date.class, "not-a-date", "yyyy-MM-dd"));
    }

    @Test
    void isEmptyAndIsNotEmptyUseCollectionAndStringSemanticsOnly() {
        assertTrue(OAConverter.isEmpty(null));
        assertTrue(OAConverter.isEmpty(""));
        assertTrue(OAConverter.isEmpty(" "));
        assertFalse(OAConverter.isEmpty(" ", false));
        assertTrue(OAConverter.isEmpty(new int[0]));
        assertFalse(OAConverter.isEmpty(new int[] { 0 }));
        assertTrue(OAConverter.isEmpty(Collections.emptyList()));
        assertFalse(OAConverter.isEmpty(Collections.singletonList("x")));
        assertTrue(OAConverter.isEmpty(Collections.emptyMap()));

        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        assertFalse(OAConverter.isEmpty(map));

        assertFalse(OAConverter.isEmpty(0));
        assertFalse(OAConverter.isEmpty(0.0d));
        assertFalse(OAConverter.isEmpty(Boolean.FALSE));
        assertFalse(OAConverter.isEmpty('\0'));
        assertTrue(OAConverter.isNotEmpty("x"));
        assertFalse(OAConverter.isNotEmpty(" "));
    }

    @Test
    void roundDelegatesToCurrentMathBehavior() {
        assertEquals(1.23d, OAConverter.round(1.234d, 2));
        assertEquals(1.24d, OAConverter.round(1.235d, 2));
    }

    @Test
    void oaConvAliasCoversRepresentativeHelpers() {
        assertEquals(OAConverter.toInt("42"), OAConv.toInt("42"));
        assertEquals(OAConverter.toLong("42"), OAConv.toLong("42"));
        assertEquals(OAConverter.toDouble("4.25"), OAConv.toDouble("4.25"));
        assertEquals(OAConverter.toBigDecimal("4.25"), OAConv.toBigDecimal("4.25"));
        assertEquals(OAConverter.isEmpty(" "), OAConv.isEmpty(" "));
        assertEquals(OAConverter.round(1.234d, 2), OAConv.round(1.234d, 2));
    }
}
