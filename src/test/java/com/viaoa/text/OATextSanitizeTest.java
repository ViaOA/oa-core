package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextSanitize. */
public class OATextSanitizeTest {
    @Test
    public void defaultStringTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.defaultString("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.defaultString(null));
        // custom default
        assertEquals("x", OATextSanitize.defaultString(null, "x"));
    }

    @Test
    public void notNullTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.notNull("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.notNull(null));
        // custom default
        assertEquals("x", OATextSanitize.notNull(null, "x"));
    }

    @Test
    public void toNotNullStringTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.toNotNullString("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.toNotNullString(null));
    }

    @Test
    public void nonNullTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.nonNull("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.nonNull(null));
        // custom default
        assertEquals("x", OATextSanitize.nonNull(null, "x"));
    }

    @Test
    public void toNonNullTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.toNonNull("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.toNonNull(null));
        // custom default
        assertEquals("x", OATextSanitize.toNonNull(null, "x"));
    }

    @Test
    public void getNonNullTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.getNonNull("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.getNonNull(null));
        // custom default
        assertEquals("x", OATextSanitize.getNonNull(null, "x"));
    }

    @Test
    public void convertToNonNullTest() {
        // non-null value returned
        assertEquals("abc", OATextSanitize.convertToNonNull("abc"));
        // null defaults to empty
        assertEquals("", OATextSanitize.convertToNonNull(null));
        // custom default
        assertEquals("x", OATextSanitize.convertToNonNull(null, "x"));
    }

    @Test
    public void toStringTest() {
        // object converted to string
        assertEquals("123", OATextSanitize.toString(123));
        // string returned as string
        assertEquals("abc", OATextSanitize.toString("abc"));
        // null becomes empty string
        assertEquals("", OATextSanitize.toString(null));
    }

    @Test
    public void isEmptyTest() {
        // null is empty
        assertTrue(OATextSanitize.isEmpty(null));
        // empty string is empty
        assertTrue(OATextSanitize.isEmpty(""));
        // trim option treats spaces as empty
        assertTrue(OATextSanitize.isEmpty("   ", true));
        // no trim option keeps spaces non-empty
        assertFalse(OATextSanitize.isEmpty("   ", false));
        // nonempty value
        assertFalse(OATextSanitize.isEmpty("abc"));
    }

    @Test
    public void notEmptyTest() {
        // nonempty value
        assertTrue(OATextSanitize.notEmpty("abc"));
        // empty string
        assertFalse(OATextSanitize.notEmpty(""));
        // null value
        assertFalse(OATextSanitize.notEmpty(null));
    }

    @Test
    public void isNotEmptyTest() {
        // alias for notEmpty
        assertTrue(OATextSanitize.isNotEmpty("abc"));
        assertFalse(OATextSanitize.isNotEmpty(""));
        assertFalse(OATextSanitize.isNotEmpty(null));
    }

    @Test
    public void isNotNullAndNotEmptyTest() {
        // nonempty value
        assertTrue(OATextSanitize.isNotNullAndNotEmpty("abc"));
        // empty value
        assertFalse(OATextSanitize.isNotNullAndNotEmpty(""));
        // null value
        assertFalse(OATextSanitize.isNotNullAndNotEmpty(null));
    }
}
