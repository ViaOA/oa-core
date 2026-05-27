package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAConverterBooleanTest {

    @ParameterizedTest
    @ValueSource(strings = { "true", "TRUE", "yes", "Y", "t" })
    void standardTrueVocabularyConvertsTrue(String value) {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "false", "FALSE", "no", "N", "f" })
    void standardFalseVocabularyConvertsFalse(String value) {
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, value));
    }

    @Test
    void numericZeroAndNonZeroConversion() {
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, 0));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, 1));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, -1));
    }

    @Test
    void numericStringZeroOneAndNegativeOneConversion() {
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, "0"));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "1"));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "-1"));
    }

    @Test
    void signedZeroStringsCurrentlyConvertTrue() {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "+0"));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "-0"));
    }

    @Test
    void signedZeroBooleanContract() {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "+0.00"));
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "-0.00"));
    }

    @Test
    void emptyAndNullConvertFalse() {
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, ""));
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, null));
    }

    @Test
    void customBooleanFormatSupportsTrueFalseAndNullTokens() {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "yes", "yes;no;maybe"));
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, "no", "yes;no;maybe"));
        assertNull(OAConverter.convert(Boolean.class, "maybe", "yes;no;maybe"));
        assertEquals("maybe", OAConverter.toString((Boolean) null, "yes;no;maybe"));
    }

    @Test
    void unknownStringWithoutFormatCurrentlyConvertsTrue() {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, "maybe"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "maybe", "x", "z", "true-ish" })
    void unknownBooleanStringsUseCurrentNonEmptyStringTruthiness(String value) {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, value));
    }

    @ParameterizedTest
    @ValueSource(strings = { " ", " false", "no ", " yes" })
    void whitespaceBooleanContract(String value) {
        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "TrUe", "YeS", "FaLsE", "No" })
    void mixedCaseBooleanVocabularyIsCaseInsensitive(String value) {
        boolean expected = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes");

        assertEquals(Boolean.valueOf(expected), OAConverter.convert(Boolean.class, value));
    }

    @Test
    void helperInvalidBooleanThrowsWhenCustomFormatDoesNotMatch() {
        assertThrows(IllegalArgumentException.class, () -> OAConverter.toBoolean("maybe", "yes;no"));
    }
}
