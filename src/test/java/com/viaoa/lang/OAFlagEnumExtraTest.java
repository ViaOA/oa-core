package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFlagEnumExtraTest {

    @Test
    void enumConstantsRemainInStableDeclarationOrder() {
        assertArrayEquals(new OAFlagEnum[] { OAFlagEnum.False, OAFlagEnum.True, OAFlagEnum.Either }, OAFlagEnum.values());
        assertEquals(0, OAFlagEnum.False.ordinal());
        assertEquals(1, OAFlagEnum.True.ordinal());
        assertEquals(2, OAFlagEnum.Either.ordinal());
    }

    @Test
    void valueOfUsesExactEnumNames() {
        assertEquals(OAFlagEnum.False, OAFlagEnum.valueOf("False"));
        assertEquals(OAFlagEnum.True, OAFlagEnum.valueOf("True"));
        assertEquals(OAFlagEnum.Either, OAFlagEnum.valueOf("Either"));
        assertThrows(IllegalArgumentException.class, () -> OAFlagEnum.valueOf("TRUE"));
    }
}
