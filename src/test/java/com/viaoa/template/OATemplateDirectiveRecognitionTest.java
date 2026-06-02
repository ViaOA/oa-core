package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateDirectiveRecognitionTest {

    static class Item extends OAObject {
    }

    @Test
    void propertyNamedFriendIsNotRecognizedAsEndDirectiveDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("[<%= $friend %>]");
        t.setProperty("friend", "Buddy");

        assertEquals("[Buddy]", t.process(),
            "normal property names containing 'end' must not be classified as End");
        assertFalse(t.getHasParseError());
    }

    @Test
    void propertyEndingWithEndIsNotRecognizedAsEndDirectiveDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("[<%= $weekend %>]");
        t.setProperty("weekend", "Saturday");

        assertEquals("[Saturday]", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void propertyStartingWithForeachIsNotRecognizedAsForeachDirectiveDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("[<%= $foreachCount %>]");
        t.setProperty("foreachCount", "7");

        assertEquals("[7]", t.process(),
            "property names beginning with foreach must not be parsed as foreach directive");
        assertFalse(t.getHasParseError());
    }

    @Test
    void exactEndDirectiveAtRootIsObservableParseErrorDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("before<%=end%>after");

        t.process();

        assertTrue(t.getHasParseError(), "unexpected root-level end should be parse-observable");
    }

    @Test
    void malformedMissingCloseTokenIsObservableParseErrorDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("before <%= $name after");

        String s = t.process();

        assertTrue(t.getHasParseError(), "malformed token missing %> should be parse-observable");
        assertTrue(s.contains("<%= $name after") || s.contains("Error"));
    }

    @Test
    void encodedDirectiveDelimitersAreDecodedBeforeParsing() {
        OATemplate<Item> t = new OATemplate<>("Hello &lt;%= $name %&gt;");
        t.setProperty("name", "Vince");

        assertEquals("Hello Vince", t.process());
    }

    @Test
    void simpleIfAndIfNotDollarPropertiesRenderExpectedChildren() {
        OATemplate<Item> t = new OATemplate<>("<%=if $flag%>Y<%=end%><%=ifnot $flag%>N<%=end%>");

        t.setProperty("flag", true);
        assertEquals("Y", t.process());

        t.setProperty("flag", false);
        assertEquals("N", t.process());
    }

    @Test
    void unmatchedIfReportsParseError() {
        OATemplate<Item> t = new OATemplate<>("<%=if $flag%>Y");

        t.process();

        assertTrue(t.getHasParseError());
    }

    @Test
    void cachedTemplatePreservesParseErrorStateAcrossProcessesDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=if $flag%>Y");

        t.process();
        assertTrue(t.getHasParseError());

        t.process();
        assertTrue(t.getHasParseError(),
            "cached parsed tree with error node must remain observable after later process calls");
    }
}
