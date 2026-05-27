package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANumberWrapperTest {

    @Test
    void integerDefaultStartsUnsetAtZero() {
        OAInteger value = new OAInteger();

        assertEquals(0, value.get());
        assertFalse(value.isSet());
        assertEquals(1, value.add());
        assertFalse(value.isSet());
        assertEquals(0, value.subtract());
        assertFalse(value.isSet());
    }

    @Test
    void integerSetAndConstructorMarkValueAsSet() {
        OAInteger value = new OAInteger(5);

        assertTrue(value.isSet());
        assertEquals(8, value.add(3));
        assertEquals(6, value.subtract(2));

        value.set(-4);
        assertEquals(-4, value.get());
        assertTrue(value.isSet());
    }

    @Test
    void integerBinaryStringsUseFixedWidthTwosComplement() {
        assertEquals(32, OAInteger.getAsBinary(0).length());
        assertEquals("00000000000000000000000000000101", OAInteger.getAsBinary(5));
        assertEquals("11111111111111111111111111111111", OAInteger.getAsBinary(-1));
        assertEquals(64, OAInteger.getAsBinary(0L).length());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111", OAInteger.getAsBinary(-1L));
    }

    @Test
    void doubleDefaultStartsUnsetAtZero() {
        OADouble value = new OADouble();

        assertEquals(0.0d, value.get());
        assertFalse(value.isSet());
        assertEquals(1.0d, value.add());
        assertFalse(value.isSet());
        assertEquals(0.0d, value.subtract());
        assertFalse(value.isSet());
    }

    @Test
    void doubleSetAndConstructorMarkValueAsSet() {
        OADouble value = new OADouble(2.5d);

        assertTrue(value.isSet());
        assertEquals(3.75d, value.add(1.25d));
        assertEquals(1.75d, value.subtract(2));

        value.set(-0.5d);
        assertEquals(-0.5d, value.get());
        assertTrue(value.isSet());
    }
}
