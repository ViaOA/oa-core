package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Product;

class OAQueryTokenManagerTest implements OAQueryTokenType {

    @Test
    void setQueryInitializesScannerAndRejectsNullByCurrentContract() {
        OAQueryTokenManager manager = new OAQueryTokenManager();
        manager.setQuery(Item.P_Name + " = 'Brake Pad'");

        assertEquals(VARIABLE, manager.getNext().type);
        assertThrows(NullPointerException.class, () -> manager.setQuery(null));
    }

    @Test
    void getNextTokenizesIdentifiersPathsNumbersStringsAndParameters() {
        OAQueryTokenManager manager = manager(Item.P_Name + " = 'Brake Pad' and " + Product.P_QuantityOnHand + " >= -12.5 or "
                + Item.P_Code + " = ?");

        assertToken(manager.getNext(), VARIABLE, Item.P_Name);
        assertToken(manager.getNext(), EQUAL, "=");
        assertToken(manager.getNext(), STRINGSQ, "Brake Pad");
        assertToken(manager.getNext(), AND, "and");
        assertToken(manager.getNext(), VARIABLE, Product.P_QuantityOnHand);
        assertToken(manager.getNext(), GE, ">=");
        assertToken(manager.getNext(), NUMBER, "-12.5");
        assertToken(manager.getNext(), OR, "or");
        assertToken(manager.getNext(), VARIABLE, Item.P_Code);
        assertToken(manager.getNext(), EQUAL, "=");
        assertToken(manager.getNext(), QUESTION, "?");
        assertToken(manager.getNext(), EOF, "");
    }

    @Test
    void getNextTokenizesAllSupportedOperatorsAndSeparators() {
        OAQueryTokenManager manager = manager("(a,b) != c && d || e > f >= g < h <= i == j");

        assertTypes(manager, SEPERATORBEGIN, VARIABLE, COMMA, VARIABLE, SEPERATOREND, NOTEQUAL, VARIABLE, AND,
                VARIABLE, OR, VARIABLE, GT, VARIABLE, GE, VARIABLE, LT, VARIABLE, LE, VARIABLE, EQUAL, VARIABLE, EOF);
    }

    @Test
    void getNextTokenizesKeywordsCaseInsensitivelyUnderDefaultLocale() {
        Locale hold = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            OAQueryTokenManager manager = manager("is null like notlike in and or");

            assertTypes(manager, EQUAL, NULL, LIKE, NOTLIKE, IN, AND, OR, EOF);
        } finally {
            Locale.setDefault(hold);
        }
    }

    @Test
    void getNextTokenizesQuotedEscapedAndPassthruLiterals() {
        OAQueryTokenManager manager = manager("'single' \"double\" {escaped} PASS[lower(name) = 'smith']THRU");

        assertToken(manager.getNext(), STRINGSQ, "single");
        assertToken(manager.getNext(), STRINGDQ, "double");
        assertToken(manager.getNext(), STRINGESC, "escaped");
        assertToken(manager.getNext(), PASSTHRU, "lower(name) = 'smith'");
        assertToken(manager.getNext(), EOF, "");
    }

    @Test
    void getNextRejectsIllegalCharacterAndCurrentContractReturnsBareBangOperator() {
        assertThrows(RuntimeException.class, () -> manager("@").getNext());
        assertToken(manager("! abc").getNext(), OPERATOR, "!");
        assertThrows(RuntimeException.class, () -> new OAQueryTokenizer().convertToTokens("! abc"));
    }

    @Test
    void getNextCurrentContractAcceptsUnterminatedQuotedAndPassthruInput() {
        OAQueryToken stringManager = manager("'Brake Pad").getNext();
        assertEquals(STRINGSQ, stringManager.type);
        assertEquals("Brake Pad", stringManager.value);

        OAQueryToken passThru = manager("PASS[lower(name)").getNext();
        assertEquals(PASSTHRU, passThru.type);
        assertEquals("lower(name)", passThru.value);
    }

    @Test
    void getNextCurrentContractDoesNotTokenizeAngleBracketAsNotEqual() {
        OAQueryTokenManager manager = manager(Item.P_Code + " <> 'BP1'");

        assertToken(manager.getNext(), VARIABLE, Item.P_Code);
        assertToken(manager.getNext(), LT, "<");
        assertToken(manager.getNext(), GT, ">");
    }

    private static OAQueryTokenManager manager(String query) {
        OAQueryTokenManager manager = new OAQueryTokenManager();
        manager.setQuery(query);
        return manager;
    }

    private static void assertTypes(OAQueryTokenManager manager, int... types) {
        List<Integer> actual = new ArrayList<>();
        for (int type : types) {
            OAQueryToken token = manager.getNext();
            actual.add(token.type);
            assertEquals(type, token.type, "token value=" + token.value + " actual types=" + actual);
        }
    }

    private static void assertToken(OAQueryToken token, int type, String value) {
        assertEquals(type, token.type);
        assertEquals(value, token.value);
    }
}
