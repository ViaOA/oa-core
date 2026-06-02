package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateParseLifecycleTest {

    static class Item extends OAObject {
    }

    @Test
    void repeatedValidRenderDoesNotAccumulateParseErrors() {
        OATemplate<Item> t = new OATemplate<>("ok");

        assertEquals("ok", t.process());
        assertFalse(t.getHasParseError());

        assertEquals("ok", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void repeatedInvalidRenderKeepsErrorObservableDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%>x");

        t.process();
        assertTrue(t.getHasParseError());

        t.process();
        assertTrue(t.getHasParseError());
    }

    @Test
    void setTemplateSameTextStillReparsesAndClearsOldErrorThenRecreatesIt() {
        OATemplate<Item> t = new OATemplate<>("<%=if $x%>x");

        t.process();
        assertTrue(t.getHasParseError());

        t.setTemplate("<%=if $x%>x");

        t.process();
        assertTrue(t.getHasParseError());
    }

    @Test
    void setTemplateFromInvalidToValidClearsError() {
        OATemplate<Item> t = new OATemplate<>("<%=if $x%>x");

        t.process();
        assertTrue(t.getHasParseError());

        t.setTemplate("valid");

        assertEquals("valid", t.process());
        assertFalse(t.getHasParseError());
    }

    @Test
    void setTemplateFromValidToInvalidReportsNewError() {
        OATemplate<Item> t = new OATemplate<>("valid");

        assertEquals("valid", t.process());
        assertFalse(t.getHasParseError());

        t.setTemplate("<%=if $x%>x");

        t.process();
        assertTrue(t.getHasParseError());
    }

    @Test
    void processAfterStopThenResetTemplateStillCanRenderWhenStopCounterChangesOnlyByStopCallsCurrentContract() {
        OATemplate<Item> t = new OATemplate<>("hello");

        t.stopProcessing();
        assertEquals("cancelled", t.process());

        t.setTemplate("world");

        // stopProcessing is sticky for exactly one process call because process snapshots current count.
        assertEquals("world", t.process());
    }
}
