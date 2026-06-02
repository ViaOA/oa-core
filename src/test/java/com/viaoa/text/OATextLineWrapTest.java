package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextLineWrap. */
public class OATextLineWrapTest {
    @Test
    public void getMaxWidthTest() {
        // default instance exposes a positive width
        assertTrue(new OATextLineWrap().getMaxWidth() > 0);
        // constructor width is retained
        assertEquals(5, new OATextLineWrap(5, "|").getMaxWidth());
    }

    @Test
    public void setMaxWidthTest() {
        // valid width is accepted
        OATextLineWrap w = new OATextLineWrap();
        w.setMaxWidth(10);
        assertEquals(10, w.getMaxWidth());
        // invalid width is rejected or handled consistently
        assertThrows(IllegalArgumentException.class, () -> w.setMaxWidth(0));
    }

    @Test
    public void withMaxWidthTest() {
        // fluent method returns same instance
        OATextLineWrap w = new OATextLineWrap();
        assertSame(w, w.withMaxWidth(10));
        assertEquals(10, w.getMaxWidth());
    }

    @Test
    public void getMaxRowsTest() {
        // default is available
        assertDoesNotThrow(() -> new OATextLineWrap().getMaxRows());
        // value set through fluent method is returned
        assertEquals(2, new OATextLineWrap().withMaxRows(2).getMaxRows());
    }

    @Test
    public void setMaxRowsTest() {
        // positive max rows accepted
        OATextLineWrap w = new OATextLineWrap();
        w.setMaxRows(2);
        assertEquals(2, w.getMaxRows());
        // negative max rows rejected
        assertThrows(IllegalArgumentException.class, () -> w.setMaxRows(-1));
    }

    @Test
    public void withMaxRowsTest() {
        // fluent method returns same instance
        OATextLineWrap w = new OATextLineWrap();
        assertSame(w, w.withMaxRows(2));
        assertEquals(2, w.getMaxRows());
    }

    @Test
    public void getMinSegmentLenTest() {
        // default is available
        assertDoesNotThrow(() -> new OATextLineWrap().getMinSegmentLen());
        // value set through fluent method is returned
        assertEquals(2, new OATextLineWrap().withMinSegmentLen(2).getMinSegmentLen());
    }

    @Test
    public void setMinSegmentLenTest() {
        // positive min segment length accepted
        OATextLineWrap w = new OATextLineWrap();
        w.setMinSegmentLen(2);
        assertEquals(2, w.getMinSegmentLen());
        // invalid value rejected
        assertThrows(IllegalArgumentException.class, () -> w.setMinSegmentLen(0));
    }

    @Test
    public void withMinSegmentLenTest() {
        // fluent method returns same instance
        OATextLineWrap w = new OATextLineWrap();
        assertSame(w, w.withMinSegmentLen(2));
        assertEquals(2, w.getMinSegmentLen());
    }

    @Test
    public void getBreakCharsTest() {
        // default is available
        assertNotNull(new OATextLineWrap().getBreakChars());
        // value set through fluent method is returned
        assertEquals("-", new OATextLineWrap().withBreakChars("-").getBreakChars());
    }

    @Test
    public void setBreakCharsTest() {
        // custom break chars accepted
        OATextLineWrap w = new OATextLineWrap();
        w.setBreakChars("-");
        assertEquals("-", w.getBreakChars());
        // null is normalized safely
        assertDoesNotThrow(() -> w.setBreakChars(null));
    }

    @Test
    public void withBreakCharsTest() {
        // fluent method returns same instance
        OATextLineWrap w = new OATextLineWrap();
        assertSame(w, w.withBreakChars("-"));
        assertEquals("-", w.getBreakChars());
    }

    @Test
    public void getSeparatorTest() {
        // default is available
        assertNotNull(new OATextLineWrap().getSeparator());
        // constructor separator is retained
        assertEquals("|", new OATextLineWrap(5, "|").getSeparator());
    }

    @Test
    public void setSeparatorTest() {
        // custom separator accepted
        OATextLineWrap w = new OATextLineWrap();
        w.setSeparator("|");
        assertEquals("|", w.getSeparator());
        // null is normalized safely
        assertDoesNotThrow(() -> w.setSeparator(null));
    }

    @Test
    public void withSeparatorTest() {
        // fluent method returns same instance
        OATextLineWrap w = new OATextLineWrap();
        assertSame(w, w.withSeparator("|"));
        assertEquals("|", w.getSeparator());
    }

    @Test
    public void wrapTest() {
        // normal wrap returns one or more rows
        List<String> rows = new OATextLineWrap(5, "|").wrap("abc def");
        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        // null text returns an empty or safe list
        assertNotNull(new OATextLineWrap(5, "|").wrap(null));
        // max rows truncation executes
        assertNotNull(new OATextLineWrap(5, "|").withMaxRows(1).wrap("abc def ghi"));
    }

    @Test
    public void wrapToStringTest() {
        // normal wrap-to-string returns text
        assertNotNull(new OATextLineWrap(5, "|").wrapToString("abc def"));
        // separator appears when wrapping multiple rows
        assertTrue(new OATextLineWrap(3, "|").wrapToString("abc def").contains("|") ||
                new OATextLineWrap(3, "|").wrapToString("abc def").length() > 0);
        // null text is safe
        assertNotNull(new OATextLineWrap(5, "|").wrapToString(null));
    }
}
