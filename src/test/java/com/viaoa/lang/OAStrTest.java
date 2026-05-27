package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAStrTest {

    @Test
    void oastrIsCompatibilitySubclassOfOAString() {
        assertInstanceOf(OAString.class, new OAStr());
    }

    @Test
    void inheritedStaticHelpersMatchOAStringBehavior() {
        assertEquals(OAString.trim("  value  "), OAStr.trim("  value  "));
        assertEquals(OAString.convert("a-b-c", "-", "."), OAStr.convert("a-b-c", "-", "."));
        assertTrue(OAStr.isEqual("Alpha", "alpha", true));
    }
}
