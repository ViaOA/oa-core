package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAConverterCharacterTest {

    @Test
    void singleCharacterStringConverts() {
        assertEquals(Character.valueOf('A'), OAConverter.convert(Character.class, "A"));
    }

    @Test
    void emptyStringReturnsNullAndHelperThrows() {
        assertNull(OAConverter.convert(Character.class, ""));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toChar(""));
    }

    @Test
    void multiCharacterStringReturnsNullAndHelperThrows() {
        assertNull(OAConverter.convert(Character.class, "AB"));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toChar("AB"));
    }

    @Test
    void booleanConvertsToTAndF() {
        assertEquals(Character.valueOf('T'), OAConverter.convert(Character.class, true));
        assertEquals(Character.valueOf('F'), OAConverter.convert(Character.class, false));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 65, Character.MAX_VALUE })
    void numberWithinCharRangeConverts(int value) {
        assertEquals(Character.valueOf((char) value), OAConverter.convert(Character.class, value));
    }

    @Test
    void numberOutsideCharRangeReturnsNullAndHelperThrows() {
        int value = Character.MAX_VALUE + 1;

        assertNull(OAConverter.convert(Character.class, value));
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toChar(value));
    }

    @Test
    void nullHelperReturnsCharZero() {
        assertEquals((char) 0, OAConverter.toChar(null));
    }
}
