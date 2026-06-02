package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OADouble.
 */
public class OADoubleTest {

    @Test
    public void constructorTest() {
        // default constructor starts with zero and is not marked as set
        OADouble d = new OADouble();
        assertEquals(0.0, d.get(), 0.000000001);
        assertFalse(d.isSet());

        // value constructor stores the value and marks it as set
        d = new OADouble(2.5);
        assertEquals(2.5, d.get(), 0.000000001);
        assertTrue(d.isSet());
    }

    @Test
    public void setTest() {
        // set assigns the value and marks it as set
        OADouble d = new OADouble();
        d.set(4.5);
        assertEquals(4.5, d.get(), 0.000000001);
        assertTrue(d.isSet());
    }

    @Test
    public void getTest() {
        // get returns current value
        OADouble d = new OADouble(3.25);
        assertEquals(3.25, d.get(), 0.000000001);
    }

    @Test
    public void addTest() {
        // add amount updates and returns value
        OADouble d = new OADouble(1.5);
        assertEquals(4.0, d.add(2.5), 0.000000001);
        assertEquals(4.0, d.get(), 0.000000001);

        // no-arg add increments by one
        assertEquals(5.0, d.add(), 0.000000001);
    }

    @Test
    public void subtractTest() {
        // subtract amount updates and returns value
        OADouble d = new OADouble(5.5);
        assertEquals(3.5, d.subtract(2), 0.000000001);
        assertEquals(3.5, d.get(), 0.000000001);

        // no-arg subtract decrements by one
        assertEquals(2.5, d.subtract(), 0.000000001);
    }

    @Test
    public void isSetTest() {
        // default instance is not set
        assertFalse(new OADouble().isSet());

        // constructor with value is set
        assertTrue(new OADouble(0.0).isSet());

        // calling set marks it as set
        OADouble d = new OADouble();
        d.set(0.0);
        assertTrue(d.isSet());
    }
}
