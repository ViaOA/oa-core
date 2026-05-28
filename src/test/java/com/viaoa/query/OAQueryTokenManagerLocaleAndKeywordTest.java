package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OAQueryTokenManagerLocaleAndKeywordTest implements OAQueryTokenType {
    private final Locale originalLocale = Locale.getDefault();

    @AfterEach void restoreLocale() { Locale.setDefault(originalLocale); }

    @Test void keywordTokenizationIsStableUnderTurkishLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        assertType("in", IN);
        assertType("IN", IN);
        assertType("like", LIKE);
        assertType("notlike", NOTLIKE);
        assertType("is", EQUAL);
        assertType("null", NULL);
        assertType("and", AND);
        assertType("or", OR);
    }

    @Test void trueAndFalseCurrentlyRemainVariablesOrExplicitBooleanTokensByContract() {
        OAQueryToken t = token("true");
        assertTrue(t.type == VARIABLE || t.type == TRUE);
        OAQueryToken f = token("false");
        assertTrue(f.type == VARIABLE || f.type == FALSE);
    }

    @Test void keywordsEmbeddedInIdentifiersRemainVariables() {
        assertVar("inValue");
        assertVar("likeName");
        assertVar("anderson");
        assertVar("orValue");
        assertVar("nullValue");
        assertVar("order.in");
    }

    @Test void mixedCaseKeywordsAreRecognized() {
        assertType("In", IN);
        assertType("LiKe", LIKE);
        assertType("NoTLike", NOTLIKE);
        assertType("NuLl", NULL);
        assertType("AnD", AND);
        assertType("oR", OR);
    }

    private static OAQueryToken token(String s) {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery(s);
        return tm.getNext();
    }
    private static void assertType(String s, int type) { assertEquals(type, token(s).type, s); }
    private static void assertVar(String s) {
        OAQueryToken t = token(s);
        assertEquals(VARIABLE, t.type);
        assertEquals(s, t.value);
    }
}
