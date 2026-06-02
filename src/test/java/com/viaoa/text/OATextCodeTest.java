package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextCode. */
public class OATextCodeTest {
    @Test
    public void getPropertyNameTest() {
        // getter prefix is removed and first char is lowered
        assertEquals("name", OATextCode.getPropertyName("getName"));
        // boolean getter prefix is removed
        assertEquals("active", OATextCode.getPropertyName("isActive"));
        // has prefix is removed
        assertEquals("children", OATextCode.getPropertyName("hasChildren"));
        // setter prefix is removed
        assertEquals("name", OATextCode.getPropertyName("setName"));
        // no prefix leaves value unchanged
        assertEquals("name", OATextCode.getPropertyName("name"));
        // optional lower-casing can be disabled
        assertEquals("Name", OATextCode.getPropertyName("getName", false));
        // current acronym behavior is characterized
        assertEquals("uRL", OATextCode.getPropertyName("getURL"));
    }
}
