package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.config.OAProperties;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateBasicRuntimeTest {

    static class Item extends OAObject {
    }

    @Test
    void defaultConstructorWithNoTemplateRendersBlankNotNullPointerDesiredContract() {
        OATemplate<Item> t = new OATemplate<>();

        assertDoesNotThrow(() -> t.process());
        assertEquals("", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void nullTemplateSetRendersBlankAndKeepsTemplateNull() {
        OATemplate<Item> t = new OATemplate<>();
        t.setTemplate(null);

        assertNull(t.getTemplate());
        assertEquals("", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void emptyAndLiteralTemplatesRenderDeterministically() {
        OATemplate<Item> empty = new OATemplate<>("");
        assertEquals("", empty.process());
        assertEquals("", empty.process());

        OATemplate<Item> literal = new OATemplate<>("hello world");
        assertEquals("hello world", literal.process());
        assertEquals("hello world", literal.process());
    }

    @Test
    void setTemplateInvalidatesParsedTreeAndParseErrorState() {
        OATemplate<Item> t = new OATemplate<>("<%=if $x%>missing end");

        t.process();
        assertTrue(t.getHasParseError());

        t.setTemplate("ok");
        assertEquals("ok", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void internalDollarPropertyRendersAndCanBeChanged() {
        OATemplate<Item> t = new OATemplate<>("Hello <%= $name %>");

        t.setProperty("name", "Vince");
        assertEquals("Hello Vince", t.process());

        t.setProperty("$name", "OA");
        assertEquals("Hello OA", t.process());
    }

    @Test
    void internalNullPropertyRemovesPropertyCurrentContract() {
        OATemplate<Item> t = new OATemplate<>("[<%= $name %>]");

        t.setProperty("name", "Vince");
        assertEquals("[Vince]", t.process());

        t.setProperty("name", null);
        assertEquals("[]", t.process());
    }

    @Test
    void externalPropertiesResolveDollarValuesWhenInternalMissing() {
        OATemplate<Item> t = new OATemplate<>("<%= $name %>");
        OAProperties props = new OAProperties();
        props.put("name", "External");

        assertEquals("External", t.process(null, props));
    }

    @Test
    void internalPropertiesOverrideExternalProperties() {
        OATemplate<Item> t = new OATemplate<>("<%= $name %>");
        OAProperties props = new OAProperties();
        props.put("name", "External");
        t.setProperty("name", "Internal");

        assertEquals("Internal", t.process(null, props));
    }

    @Test
    void missingInternalOrExternalPropertyRendersBlank() {
        OATemplate<Item> t = new OATemplate<>("[<%= $missing %>]");

        assertEquals("[]", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void htmlTemplateWithNoForeachDoesNotCrashRowTagPreprocess() {
        OATemplate<Item> t = new OATemplate<>("<html><body>Hello</body></html>");

        assertEquals("<html><body>Hello</body></html>", t.process());
    }

    @Test
    void malformedHtmlForeachDoesNotThrowAndReportsParseErrorDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<html><table><tr><td><%=foreach children%></td></tr></table></html>");

        assertDoesNotThrow(() -> t.process());
        assertTrue(t.getHasParseError());
    }

    @Test
    void stopProcessingBeforeProcessCancelsCurrentRender() {
        OATemplate<Item> t = new OATemplate<>("hello");

        t.stopProcessing();

        assertEquals("cancelled", t.process());
    }
}
