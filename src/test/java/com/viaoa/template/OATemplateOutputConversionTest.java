package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateOutputConversionTest {

    static class Item extends OAObject {
    }

    @Test
    void outputConversionAppliesToLiteralAndPropertyOutput() {
        OATemplate<Item> t = new OATemplate<>("a <%= $x %>");
        t.setProperty("x", "a");
        t.setOutputTextConversion("a", "aa");

        assertEquals("aa aa", t.process());
    }

    @Test
    void outputConversionCanBeCleared() {
        OATemplate<Item> t = new OATemplate<>("a");
        t.setOutputTextConversion("a", "aa");

        assertEquals("aa", t.process());

        t.setOutputTextConversion(null, null);

        assertEquals("a", t.process());
    }

    @Test
    void formatBlockAppliesOutputConversionOnceDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%><%= $x %><%=end%>");
        t.setProperty("x", "a");
        t.setOutputTextConversion("a", "aa");

        assertEquals("aa", t.process().trim(),
            "property output inside format block should not be converted twice");
    }

    @Test
    void highlightOutputTextWrapsMatchingText() {
        OATemplate<Item> t = new OATemplate<>("hello world");
        t.setHiliteOutputText("world");

        String s = t.process();

        assertTrue(s.toLowerCase().contains("world"));
        assertNotEquals("hello world", s, "highlight should alter output markup when match exists");
    }

    @Test
    void nullOutputTextConversionInputIsSafe() {
        OATemplate<Item> t = new OATemplate<>("");
        t.setOutputTextConversion("x", "y");

        assertEquals("", t.process());
    }
}
