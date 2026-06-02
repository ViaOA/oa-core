package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextSoundex. */
public class OATextSoundexTest {
    @Test
    public void soundexTest() {
        // documented example
        assertEquals("V525", OATextSoundex.soundex("Vincent"));
        // documented short example
        assertEquals("V000", OATextSoundex.soundex("Via"));
        // null returns zero code
        assertEquals("0000", OATextSoundex.soundex(null));
        // empty returns zero code
        assertEquals("0000", OATextSoundex.soundex(""));
        // case-insensitive behavior
        assertEquals(OATextSoundex.soundex("Smith"), OATextSoundex.soundex("smith"));
        // result is always four characters
        assertEquals(4, OATextSoundex.soundex("Robert").length());
    }
}
