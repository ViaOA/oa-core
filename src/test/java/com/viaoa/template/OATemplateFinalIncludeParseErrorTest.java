package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateFinalIncludeParseErrorTest {

    static class Item extends OAObject {
    }

    static class IncludeTemplate extends OATemplate<Item> {
        @Override
        protected String getIncludeText(String name) {
            if ("a".equals(name)) return "A";
            if ("b".equals(name)) return "B";
            if ("bad".equals(name)) return "<%=if $x%>bad";
            if ("loop".equals(name)) return "<%=include loop%>";
            return super.getIncludeText(name);
        }
    }

    @Test
    void includeExpansionThatCreatesParseErrorIsObservable() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("x<%=include bad%>y");

        t.process();

        assertTrue(t.getHasParseError());
    }

    @Test
    void repeatedIndependentIncludesOfDifferentNamesWork() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include a%><%=include b%><%=include a%>");

        assertEquals("ABA", t.process());
    }

    @Test
    void recursiveIncludeDoesNotHangAndReportsText() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include loop%>");

        String s = assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), t::process);

        assertTrue(s.contains("recursive include"));
    }

    @Test
    void malformedTokenDoesNotClearPreviousParseErrorFalseSuccessDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=if $x%>bad");

        t.process();
        assertTrue(t.getHasParseError());

        t.setTemplate("before <%= $x after");
        t.process();

        assertTrue(t.getHasParseError(),
            "malformed token missing %> must not look parse-clean");
    }

    @Test
    void unexpectedEndInsideIncludedTextIsObservableDesiredContract() {
        IncludeTemplate t = new IncludeTemplate() {
            @Override
            protected String getIncludeText(String name) {
                return "<%=end%>";
            }
        };
        t.setTemplate("<%=include anything%>");

        t.process();

        assertTrue(t.getHasParseError());
    }
}
