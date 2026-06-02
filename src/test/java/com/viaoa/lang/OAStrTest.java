package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OAStr.
 */
public class OAStrTest {

    @Test
    public void inheritanceTest() {
        // OAStr is a direct subclass of OAString
        assertTrue(OAString.class.isAssignableFrom(OAStr.class));

        // static methods inherited from OAString remain available through OAStr
        assertEquals(OAString.trimSpaces(" abc "), OAStr.trimSpaces(" abc "));
    }
}
