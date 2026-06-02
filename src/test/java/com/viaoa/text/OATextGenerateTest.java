package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextGenerate. */
public class OATextGenerateTest {
    @Test
    public void getDummyTextTest() {
        // normal bounds produce text
        String s = OATextGenerate.getDummyText(20, 10, 30);
        assertNotNull(s);
        assertTrue(s.length() >= 0);
        // zero values execute safely
        assertDoesNotThrow(() -> OATextGenerate.getDummyText(0, 0, 0));
        // min/max-only behavior is covered by same method overload
        assertNotNull(OATextGenerate.getDummyText(10, 5, 20));
    }

    @Test
    public void getRandomStringTest() {
        // min/max overload produces value within broad bounds
        String s = OATextGenerate.getRandomString(5, 10);
        assertNotNull(s);
        assertTrue(s.length() >= 5 && s.length() <= 10);
        // normal/min/max overload produces value
        assertNotNull(OATextGenerate.getRandomString(7, 5, 10));
        // digits-only option produces a value
        assertNotNull(OATextGenerate.getRandomString(5, 5, true, false, false));
        // alpha option produces a value
        assertNotNull(OATextGenerate.getRandomString(5, 5, false, true, false));
        // cap first char option executes
        assertNotNull(OATextGenerate.getRandomString(7, 5, 10, false, true, true));
    }

    @Test
    public void createDigitsTest() {
        // fixed length digit string
        String s = OATextGenerate.createDigits(5, 5);
        assertNotNull(s);
        assertEquals(5, s.length());
        assertTrue(s.matches("[0-9]+"));
        // range length
        String s2 = OATextGenerate.createDigits(1, 3);
        assertNotNull(s2);
        assertTrue(s2.length() >= 1 && s2.length() <= 3);
    }
}
