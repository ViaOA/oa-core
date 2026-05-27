package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class OARegexTest {

    @Test
    void digitIntegerDecimalAndCurrencyRegexesAcceptAndRejectKnownExamples() {
        assertTrue(Pattern.matches(OARegex.Regex_Digits, " 123 "));
        assertFalse(Pattern.matches(OARegex.Regex_Digits, "12.3"));

        assertTrue(Pattern.matches(OARegex.Regex_Integer, " -123 "));
        assertTrue(Pattern.matches(OARegex.Regex_Integer, "+123"));
        assertFalse(Pattern.matches(OARegex.Regex_Integer, "1.0"));

        assertTrue(Pattern.matches(OARegex.Regex_Decimal, "1"));
        assertTrue(Pattern.matches(OARegex.Regex_Decimal, "-1.25"));
        assertTrue(Pattern.matches(OARegex.Regex_Decimal, ".25"));
        assertFalse(Pattern.matches(OARegex.Regex_Decimal, "abc"));

        assertTrue(Pattern.matches(OARegex.Regex_Currency, "1.25"));
        assertTrue(Pattern.matches(OARegex.Regex_Currency, ".25"));
        assertFalse(Pattern.matches(OARegex.Regex_Currency, "1.234"));
    }

    @Test
    void urlRegexMatchesCommonUrlForms() {
        Pattern p = Pattern.compile(OARegex.Regex_URL);

        assertTrue(p.matcher("see https://viaoa.com/docs").find());
        assertTrue(p.matcher("go to www.viaoa.com/index.html").find());
        assertFalse(p.matcher("not a url").find());
    }
}
