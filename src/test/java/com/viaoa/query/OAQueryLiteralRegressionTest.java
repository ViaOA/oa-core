package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryLiteralRegressionTest implements OAQueryTokenType {
    @Test void escapedBackslashIsPreservedInSingleQuotedLiteral() {
        Vector<OAQueryToken> t = tokens("path = 'c:\\\\tmp'");
        assertEquals(STRINGSQ, t.get(2).type);
        assertEquals("c:\\tmp", t.get(2).value);
    }

    @Test void escapedDoubleQuoteInsideDoubleQuotedLiteralIsPreserved() {
        Vector<OAQueryToken> t = tokens("name = \"a\\\"b\"");
        assertEquals(STRINGDQ, t.get(2).type);
        assertEquals("a\"b", t.get(2).value);
    }

    @Test void doubledSqlSingleQuoteSequenceParsesAsAdjacentStringTokens() {
        Vector<OAQueryToken> t = tokens("code = 'CT13''6'");
        assertEquals(STRINGSQ, t.get(2).type);
        assertEquals("CT13", t.get(2).value);
        assertEquals(STRINGSQ, t.get(3).type);
        assertEquals("6", t.get(3).value);
    }

    @Test void unterminatedLiteralsFailVisiblyAndDoNotReturnPartialTokens() {
        assertInvalid("name = 'abc");
        assertInvalid("name = \"abc");
        assertInvalid("name = {abc");
    }

    @Test void emptyStringLiteralsAreAllowedAndPreserved() {
        Vector<OAQueryToken> t1 = tokens("name = ''");
        assertEquals(STRINGSQ, t1.get(2).type);
        assertEquals("", t1.get(2).value);
        Vector<OAQueryToken> t2 = tokens("name = \"\"");
        assertEquals(STRINGDQ, t2.get(2).type);
        assertEquals("", t2.get(2).value);
    }

    static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
}
