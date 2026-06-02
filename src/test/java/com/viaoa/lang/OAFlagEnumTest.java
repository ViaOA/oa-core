package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OAFlagEnum.
 */
public class OAFlagEnumTest {

    @Test
    public void valuesTest() {
        // enum exposes the three expected values in declaration order
        assertArrayEquals(new OAFlagEnum[] { OAFlagEnum.False, OAFlagEnum.True, OAFlagEnum.Either }, OAFlagEnum.values());
    }

    @Test
    public void valueOfTest() {
        // valueOf resolves each enum name
        assertSame(OAFlagEnum.False, OAFlagEnum.valueOf("False"));
        assertSame(OAFlagEnum.True, OAFlagEnum.valueOf("True"));
        assertSame(OAFlagEnum.Either, OAFlagEnum.valueOf("Either"));
    }
}
