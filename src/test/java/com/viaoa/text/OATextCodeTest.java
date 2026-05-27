package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextCodeTest {

    @Test
    void getPropertyNameStripsCommonBeanPrefixes() {
        assertEquals("name", OATextCode.getPropertyName("getName"));
        assertEquals("active", OATextCode.getPropertyName("isActive"));
        assertEquals("children", OATextCode.getPropertyName("hasChildren"));
        assertEquals("name", OATextCode.getPropertyName("setName"));
        assertEquals("name", OATextCode.getPropertyName("name"));
    }

    @Test
    void getPropertyNameCanPreserveCaseWhenRequested() {
        assertEquals("Name", OATextCode.getPropertyName("getName", false));
        assertEquals("Active", OATextCode.getPropertyName("isActive", false));
        assertEquals("Name", OATextCode.getPropertyName("Name", false));
    }

    @Test
    void acronymPropertyNameCurrentlyDecapitalizesFirstLetter() {
        assertEquals("uRL", OATextCode.getPropertyName("getURL"));
        assertEquals("URL", OATextCode.getPropertyName("getURL", false));
    }
}
