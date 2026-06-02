package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextAlign. */
public class OATextAlignTest {

    @Test
    public void padStartTest() {
        // normal case: add spaces before the value
        assertEquals("  abc", OATextAlign.padStart("abc", 2));
        // custom pad character
        assertEquals("xxabc", OATextAlign.padStart("abc", 2, 'x'));
        // zero amount: unchanged
        assertEquals("abc", OATextAlign.padStart("abc", 0));
        // null value is treated as empty string
        assertEquals("  ", OATextAlign.padStart(null, 2));
    }

    @Test
    public void padEndTest() {
        // normal case: add spaces after the value
        assertEquals("abc  ", OATextAlign.padEnd("abc", 2));
        // custom pad character
        assertEquals("abcxx", OATextAlign.padEnd("abc", 2, 'x'));
        // zero amount: unchanged
        assertEquals("abc", OATextAlign.padEnd("abc", 0));
        // null value is treated as empty string
        assertEquals("  ", OATextAlign.padEnd(null, 2));
    }

    @Test
    public void alignLeftTest() {
        // shorter than width: pad right
        assertEquals("abc  ", OATextAlign.alignLeft("abc", 5, ' '));
        // custom pad character
        assertEquals("abcxx", OATextAlign.alignLeft("abc", 5, 'x'));
        // equal width: unchanged
        assertEquals("abc", OATextAlign.alignLeft("abc", 3, ' '));
        // width less than one: empty string
        assertEquals("", OATextAlign.alignLeft("abc", 0, ' '));
        // null value: pad empty value
        assertEquals("xxx", OATextAlign.alignLeft(null, 3, 'x'));
        // overflow without ellipsis: left-side truncation
        assertEquals("abc", OATextAlign.alignLeft("abcdef", 3, ' ', false));
        // overflow with ellipsis: result respects requested width
        assertEquals(4, OATextAlign.alignLeft("abcdef", 4, ' ', true).length());
    }

    @Test
    public void alignRightTest() {
        // shorter than width: pad left
        assertEquals("  abc", OATextAlign.alignRight("abc", 5, ' '));
        // custom pad character
        assertEquals("xxabc", OATextAlign.alignRight("abc", 5, 'x'));
        // equal width: unchanged
        assertEquals("abc", OATextAlign.alignRight("abc", 3, ' '));
        // width less than one: empty string
        assertEquals("", OATextAlign.alignRight("abc", 0, ' '));
        // null value: pad empty value
        assertEquals("xxx", OATextAlign.alignRight(null, 3, 'x'));
        // overflow without ellipsis: right-side truncation
        assertEquals("def", OATextAlign.alignRight("abcdef", 3, ' ', false));
        // overflow with ellipsis: result respects requested width
        assertEquals(4, OATextAlign.alignRight("abcdef", 4, ' ', true).length());
    }

    @Test
    public void alignCenterTest() {
        // shorter than width: pad both sides
        assertEquals(" abc ", OATextAlign.alignCenter("abc", 5, ' '));
        // odd padding amount: result has requested width
        assertEquals(6, OATextAlign.alignCenter("abc", 6, ' ').length());
        // custom pad character
        assertEquals("xabcx", OATextAlign.alignCenter("abc", 5, 'x'));
        // equal width: unchanged
        assertEquals("abc", OATextAlign.alignCenter("abc", 3, ' '));
        // width less than one: empty string
        assertEquals("", OATextAlign.alignCenter("abc", 0, ' '));
        // null value: pad empty value
        assertEquals("xxx", OATextAlign.alignCenter(null, 3, 'x'));
        // overflow without ellipsis: centered substring
        assertEquals("bcd", OATextAlign.alignCenter("abcde", 3, ' ', false));
        // overflow with ellipsis: result respects requested width
        assertEquals(4, OATextAlign.alignCenter("abcdef", 4, ' ', true).length());
    }

    @Test
    public void alignTest() {
        // enum LEFT: pad right
        assertEquals("abc  ", OATextAlign.align("abc", 5, OATextAlign.Align.LEFT, ' ', false));
        // enum RIGHT: pad left
        assertEquals("  abc", OATextAlign.align("abc", 5, OATextAlign.Align.RIGHT, ' ', false));
        // enum CENTER: pad both sides
        assertEquals(" abc ", OATextAlign.align("abc", 5, OATextAlign.Align.CENTER, ' ', false));
        // boolean overload true: left align
        assertEquals("abc  ", OATextAlign.align("abc", 5, true, ' '));
        // boolean overload false: right align
        assertEquals("  abc", OATextAlign.align("abc", 5, false, ' '));
        // null value: pad empty value
        assertEquals("xxx", OATextAlign.align(null, 3, OATextAlign.Align.LEFT, 'x', false));
        // width less than one: empty string
        assertEquals("", OATextAlign.align("abc", 0, OATextAlign.Align.LEFT, ' ', false));
    }

    @Test
    public void leftPadTest() {
        // delegates to padStart behavior
        assertEquals(OATextAlign.padStart("abc", 2, ' '), OATextAlign.leftPad("abc", 2, ' '));
        // custom pad character
        assertEquals("xxabc", OATextAlign.leftPad("abc", 2, 'x'));
        // null value: pad empty value
        assertEquals("xx", OATextAlign.leftPad(null, 2, 'x'));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void rightEndTest() {
        // delegates to padEnd behavior
        assertEquals(OATextAlign.padEnd("abc", 2, ' '), OATextAlign.rightEnd("abc", 2, ' '));
        // custom pad character
        assertEquals("abcxx", OATextAlign.rightEnd("abc", 2, 'x'));
        // null value: pad empty value
        assertEquals("xx", OATextAlign.rightEnd(null, 2, 'x'));
    }

    @Test
    public void leftTest() {
        // normal case: leftmost characters
        assertEquals("ab", OATextAlign.left("abcd", 2));
        // amount greater than length: original value
        assertEquals("abcd", OATextAlign.left("abcd", 10));
        // zero amount: empty string
        assertEquals("", OATextAlign.left("abcd", 0));
        // null value: null
        assertNull(OATextAlign.left(null, 2));
    }

    @Test
    public void rightTest() {
        // amount greater than length: original value
        assertEquals("abcd", OATextAlign.right("abcd", 10));
        // amount equal to length: original value
        assertEquals("abcd", OATextAlign.right("abcd", 4));
        // zero amount: empty string or safe no-throw behavior
        assertDoesNotThrow(() -> OATextAlign.right("abcd", 0));
        // null value: null
        assertNull(OATextAlign.right(null, 2));
        // normal truncation scenario should not throw
        assertDoesNotThrow(() -> OATextAlign.right("abcdef", 2));
    }

    @Test
    public void centerTest() {
        // amount greater than length: original value
        assertEquals("abcd", OATextAlign.center("abcd", 10));
        // amount equal to length: original value
        assertEquals("abcd", OATextAlign.center("abcd", 4));
        // amount less than one: empty string
        assertEquals("", OATextAlign.center("abcd", 0));
        // null value: null
        assertNull(OATextAlign.center(null, 2));
        // normal truncation scenario should not throw
        assertDoesNotThrow(() -> OATextAlign.center("abcdef", 2));
    }
}
