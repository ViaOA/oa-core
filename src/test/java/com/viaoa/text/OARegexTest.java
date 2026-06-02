package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Internal tests for OARegex constants. */
public class OARegexTest {
    @Test
    public void regexConstantsTest() {
        // URL regex compiles
        assertDoesNotThrow(() -> Pattern.compile(OARegex.Regex_URL));
        // digits regex accepts only digits with whitespace
        assertTrue(" 123 ".matches(OARegex.Regex_Digits));
        assertFalse("12a".matches(OARegex.Regex_Digits));
        // integer regex accepts signs
        assertTrue(" -123 ".matches(OARegex.Regex_Integer));
        assertFalse("1.2".matches(OARegex.Regex_Integer));
        // decimal regex compiles if present
        assertNotNull(OARegex.Regex_Decimal);
        assertDoesNotThrow(() -> Pattern.compile(OARegex.Regex_Decimal));
    }
}
