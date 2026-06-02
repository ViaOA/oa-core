package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateIncludeAndPreprocessTest {

    static class Item extends OAObject {
    }

    static class IncludeTemplate extends OATemplate<Item> {
        @Override
        protected String getIncludeText(String name) {
            if ("header".equals(name)) return "H";
            if ("footer".equals(name)) return "F";
            if ("self".equals(name)) return "<%=include self%>";
            if ("pairA".equals(name)) return "<%=include pairB%>";
            if ("pairB".equals(name)) return "<%=include pairA%>";
            return super.getIncludeText(name);
        }
    }

    @Test
    void includeExpandsSubclassTextAndPreservesSurroundingLiteral() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("A<%=include header%>B<%=include footer%>C");

        assertEquals("AHBFC", t.process());
    }

    @Test
    void repeatedNonRecursiveIncludeIsAllowedDesiredContract() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include header%>-<%=include header%>");

        assertEquals("H-H", t.process(),
            "same include used twice independently should not be treated as recursion");
    }

    @Test
    void directRecursiveIncludeIsReported() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include self%>");

        String s = t.process();

        assertTrue(s.contains("recursive include"));
    }

    @Test
    void indirectRecursiveIncludeIsReported() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include pairA%>");

        String s = t.process();

        assertTrue(s.contains("recursive include"));
    }

    @Test
    void unknownIncludeReturnsDefaultErrorText() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("<%=include missing%>");

        assertEquals(" ERROR: no text for include missing ", t.process());
    }

    @Test
    void malformedIncludeMissingEndIsParseObservableDesiredContract() {
        IncludeTemplate t = new IncludeTemplate();
        t.setTemplate("before <%=include header");

        t.process();

        assertTrue(t.getHasParseError(),
            "malformed include should be parse-observable instead of silent literal output");
    }
}
