package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class OATextGenerateSoundexFormatterTest {

    @Test
    void dummyTextLengthStaysWithinRequestedBoundsForNormalInputs() {
        String text = OATextGenerate.getDummyText(0, 20, 40);

        assertNotNull(text);
        assertTrue(text.length() >= 20);
        assertTrue(text.length() <= 40);
    }

    @Test
    void dummyTextInvalidBoundsCurrentlyThrow() {
        assertThrows(RuntimeException.class, () -> OATextGenerate.getDummyText(0, -1, -1));
    }

    @Test
    void randomStringHonorsLengthAndDigitOnlyRequest() {
        String value = OATextGenerate.getRandomString(5, 5, true, false, false);
        assertEquals(5, value.length());
        assertTrue(value.chars().allMatch(Character::isDigit));
    }

    @Test
    void randomStringNoDigitsNoAlphaCurrentlyStillGeneratesAlpha() {
        String value = OATextGenerate.getRandomString(5, 5, false, false, false);

        assertEquals(5, value.length());
        assertTrue(value.chars().anyMatch(Character::isLetter));
    }

    @Test
    void createDigitsProducesOnlyDigits() {
        String value = OATextGenerate.createDigits(10, 10);

        assertEquals(10, value.length());
        assertTrue(value.chars().allMatch(Character::isDigit));
    }

    @Test
    void soundexHandlesCommonExamplesAndNull() {
        assertEquals("0000", OATextSoundex.soundex(null));
        assertEquals("0000", OATextSoundex.soundex(""));
        String s = OATextSoundex.soundex("Vincent");
        assertEquals("V525", s); 
        assertEquals("V000", OATextSoundex.soundex("Via"));
    }

    @Test
    void soundexCurrentlyPreservesLeadingNonLetter() {
        assertEquals("1253", OATextSoundex.soundex("1Smith"));
    }

    @Test
    void indentFormatterTracksEntryAndExitIndentation() {
        IndentFormatter formatter = new IndentFormatter();

        LogRecord entry = new LogRecord(Level.FINEST, "ENTRY");
        entry.setSourceClassName("com.test.Sample");
        entry.setSourceMethodName("run");

        LogRecord message = new LogRecord(Level.INFO, "inside");
        LogRecord exit = new LogRecord(Level.FINEST, "RETURN");
        exit.setSourceClassName("com.test.Sample");
        exit.setSourceMethodName("run");

        String s1 = formatter.format(entry);
        String s2 = formatter.format(message);
        String s3 = formatter.format(exit);

        assertTrue(s1.contains("+Sample.run"));
        assertTrue(s2.startsWith("|  inside"));
        assertTrue(s3.contains("+Sample.run"));
        assertFalse(s3.startsWith("|  |  "));
    }

    @Test
    void indentFormatterMarksWarningsAndExceptions() {
        IndentFormatter formatter = new IndentFormatter();
        LogRecord record = new LogRecord(Level.WARNING, "warn");
        record.setThrown(new IllegalStateException("bad"));

        String text = formatter.format(record);

        assertTrue(text.contains("warn"));
        assertTrue(text.contains("EXCEPTION"));
        assertTrue(text.contains("WARNING"));
    }
}
