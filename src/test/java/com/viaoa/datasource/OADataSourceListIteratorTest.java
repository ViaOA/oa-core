package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class OADataSourceListIteratorTest {

    @Test
    void constructorAcceptsNullList() {
        OADataSourceListIterator it = new OADataSourceListIterator(null);

        assertFalse(it.hasNext());
        assertNull(it.next());
    }

    @Test
    void hasNextAndNextIterateInListOrder() {
        OADataSourceListIterator it = new OADataSourceListIterator(Arrays.asList("one", "two"));

        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertFalse(it.hasNext());
        assertNull(it.next());
    }
}
