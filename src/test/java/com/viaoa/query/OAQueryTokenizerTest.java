package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.propertypath.InvoicePP;
import com.viaoa.text.OATextUtil;

class OAQueryTokenizerTest implements OAQueryTokenType {
    private static final String GENERATED_ITEM_NAME_PATH = InvoicePP.invoiceBaskets().lineItems().product().item().name();
    private static final String P_CONSTANT_ITEM_NAME_PATH = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems
            + "." + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name;
    private static final String TEXT_UTIL_ITEM_NAME_PATH = OATextUtil.createPath(Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item, Item.P_Name);
    private static final String RAW_ITEM_NAME_PATH = "invoiceBaskets.lineItems.product.item.name";

    @Test
    void convertToTokensParsesPropertyPathComparisonsAndCanBeReused() {
        OAQueryTokenizer tokenizer = new OAQueryTokenizer();

        assertEquals(P_CONSTANT_ITEM_NAME_PATH, GENERATED_ITEM_NAME_PATH);
        assertEquals(P_CONSTANT_ITEM_NAME_PATH, TEXT_UTIL_ITEM_NAME_PATH);
        assertEquals(P_CONSTANT_ITEM_NAME_PATH.toLowerCase(), RAW_ITEM_NAME_PATH.toLowerCase());

        Vector<OAQueryToken> tokens = tokenizer.convertToTokens(GENERATED_ITEM_NAME_PATH + " = 'Brake Pad'");
        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ);
        assertEquals(GENERATED_ITEM_NAME_PATH, tokens.get(0).value);

        Vector<OAQueryToken> reused = tokenizer.convertToTokens(TEXT_UTIL_ITEM_NAME_PATH + " like 'Brake%'");
        assertTypes(reused, VARIABLE, LIKE, STRINGSQ);
        assertEquals(TEXT_UTIL_ITEM_NAME_PATH, reused.get(0).value);
    }

    @Test
    void evaluateRejectsMissingRightSideOfLogicalExpression() {
        ExposedTokenizer tokenizer = new ExposedTokenizer();
        tokenizer.prepare(Item.P_Name + " and");

        assertThrows(RuntimeException.class, tokenizer::callEvaluate);
    }

    @Test
    void evaluateAParsesAndOrExpressionsInCurrentTokenizerOrder() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens(
                Item.P_Name + " = 'Brake Pad' and " + Item.P_Code + " = 'BP1' or " + Item.P_Brand + " = 'ACME'");

        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ, AND, VARIABLE, EQUAL, STRINGSQ, OR, VARIABLE, EQUAL, STRINGSQ);
    }

    @Test
    void evaluateBParsesComparisonOperators() {
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Code + " != 'BP2'"), VARIABLE, NOTEQUAL, STRINGSQ);
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Code + " >= 'BP1'"), VARIABLE, GE, STRINGSQ);
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Code + " <= 'BP9'"), VARIABLE, LE, STRINGSQ);
    }

    @Test
    void evaluateB2ParsesInExpressionsAndQuestionParameters() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens(Item.P_Code + " in ('BP1','BP2',?)");

        assertTypes(tokens, VARIABLE, IN, SEPERATORBEGIN, STRINGSQ, COMMA, STRINGSQ, COMMA, QUESTION, SEPERATOREND);
    }

    @Test
    void evaluateCParsesParenthesizedCompoundExpressionsAndRejectsUnbalancedBrackets() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens(
                "(" + Item.P_Name + " = 'Brake Pad' or " + Item.P_Name + " = 'Rotor')");

        assertTypes(tokens, SEPERATORBEGIN, VARIABLE, EQUAL, STRINGSQ, OR, VARIABLE, EQUAL, STRINGSQ, SEPERATOREND);
        assertThrows(RuntimeException.class, () -> new OAQueryTokenizer().convertToTokens("(" + Item.P_Name + " = 'Brake Pad'"));
    }

    @Test
    void evaluateC2ParsesFunctionCalls() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens("lower(" + Item.P_Name + ") = 'brake pad'");

        assertTypes(tokens, VARIABLE, FUNCTIONBEGIN, VARIABLE, FUNCTIONEND, EQUAL, STRINGSQ);
        assertEquals("lower", tokens.get(0).value);
    }

    @Test
    void evaluateDParsesSingleQuotedStrings() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens(Item.P_Name + " = 'Brake Pad'");

        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ);
        assertEquals("Brake Pad", tokens.get(2).value);
    }

    @Test
    void evaluateEAllowsAdjacentSingleQuotedStringTokens() {
        Vector<OAQueryToken> tokens = new OAQueryTokenizer().convertToTokens(Item.P_Code + " = 'CT13''6'");

        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ, STRINGSQ);
        assertEquals("CT13", tokens.get(2).value);
        assertEquals("6", tokens.get(3).value);
    }

    @Test
    void evaluateFParsesAtomicLiteralTypesAndRejectsUnexpectedTokens() {
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Code + " = \"BP1\""), VARIABLE, EQUAL, STRINGDQ);
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Code + " = {BP1}"), VARIABLE, EQUAL, STRINGESC);
        assertTypes(new OAQueryTokenizer().convertToTokens(Product.P_QuantityOnHand + " > -12.5"), VARIABLE, GT, NUMBER);
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Brand + " = null"), VARIABLE, EQUAL, NULL);
        assertTypes(new OAQueryTokenizer().convertToTokens(Item.P_Name + " = PASS[lower(name)]THRU"), VARIABLE, EQUAL, PASSTHRU);
        assertThrows(RuntimeException.class, () -> new OAQueryTokenizer().convertToTokens(Item.P_Name + " = )"));
    }

    @Test
    void nextTokenAdvancesCurrentAndLastToken() {
        ExposedTokenizer tokenizer = new ExposedTokenizer();
        tokenizer.prepare(Item.P_Name + " = 'Brake Pad'");

        assertNull(tokenizer.lastToken);
        assertEquals(VARIABLE, tokenizer.token.type);
        tokenizer.callNextToken();
        assertEquals(VARIABLE, tokenizer.lastToken.type);
        assertEquals(EQUAL, tokenizer.token.type);
    }

    @Test
    void demonstrationMainMethodsDoNotThrow() {
        assertDoesNotThrow(() -> OAQueryTokenizer.main2(new String[0]));
        assertDoesNotThrow(() -> OAQueryTokenizer.main(new String[0]));
    }

    private static void assertTypes(Vector<OAQueryToken> tokens, int... types) {
        assertEquals(types.length, tokens.size(), tokenSummary(tokens));
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], tokens.get(i).type, "token index " + i + " value=" + tokens.get(i).value);
        }
    }

    private static String tokenSummary(Vector<OAQueryToken> tokens) {
        StringBuilder sb = new StringBuilder();
        for (OAQueryToken token : tokens) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(token.type).append(':').append(token.value);
        }
        return sb.toString();
    }

    private static class ExposedTokenizer extends OAQueryTokenizer {
        void prepare(String query) {
            tokenManager = new OAQueryTokenManager();
            vec = new Vector(20, 20);
            tokenManager.setQuery(query);
            nextToken();
        }

        void callEvaluate() {
            evaluate();
        }

        void callNextToken() {
            nextToken();
        }
    }
}
