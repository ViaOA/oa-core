package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OAInteger.
 */
public class OAIntegerTest {

    @Test
    public void constructorTest() {
        // default constructor starts with zero and is not marked as set
        OAInteger i = new OAInteger();
        assertEquals(0, i.get());
        assertFalse(i.isSet());

        // value constructor stores the value and marks it as set
        i = new OAInteger(7);
        assertEquals(7, i.get());
        assertTrue(i.isSet());
    }

    @Test
    public void getTest() {
        // get returns current value
        assertEquals(3, new OAInteger(3).get());
    }

    @Test
    public void setTest() {
        // set assigns the value and marks it as set
        OAInteger i = new OAInteger();
        i.set(4);
        assertEquals(4, i.get());
        assertTrue(i.isSet());
    }

    @Test
    public void addTest() {
        // add amount updates and returns value
        OAInteger i = new OAInteger(2);
        assertEquals(5, i.add(3));
        assertEquals(5, i.get());

        // no-arg add increments by one
        assertEquals(6, i.add());
    }

    @Test
    public void subtractTest() {
        // subtract amount updates and returns value
        OAInteger i = new OAInteger(7);
        assertEquals(4, i.subtract(3));
        assertEquals(4, i.get());

        // no-arg subtract decrements by one
        assertEquals(3, i.subtract());
    }

    @Test
    public void isSetTest() {
        // default instance is not set
        assertFalse(new OAInteger().isSet());

        // constructor with value is set
        assertTrue(new OAInteger(0).isSet());

        // calling set marks it as set
        OAInteger i = new OAInteger();
        i.set(0);
        assertTrue(i.isSet());
    }

    @Test
    public void getAsBinaryTest() {
        // int binary output is fixed width
        assertEquals(32, OAInteger.getAsBinary(0).length());
        assertEquals("00000000000000000000000000000000", OAInteger.getAsBinary(0));
        assertEquals("00000000000000000000000000000001", OAInteger.getAsBinary(1));

        // long binary output is fixed width
        assertEquals(64, OAInteger.getAsBinary(0L).length());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000001", OAInteger.getAsBinary(1L));
    }
}
