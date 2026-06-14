package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class OADataSourceIteratorTest {

    @Test
    void defaultGetQueryReturnsNull() {
        OADataSourceIterator<String> it = iterator("a");

        assertNull(it.getQuery());
    }

    @Test
    void defaultGetQuery2ReturnsNull() {
        OADataSourceIterator<String> it = iterator("a");

        assertNull(it.getQuery2());
    }

    @Test
    void defaultGetSiblingHelperReturnsNull() {
        OADataSourceIterator<String> it = iterator("a");

        assertNull(it.getSiblingHelper());
    }

    @Test
    void defaultRemoveIsNoOp() {
        OADataSourceIterator<String> it = iterator("a");

        assertDoesNotThrow(() -> it.remove());
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
    }

    private static OADataSourceIterator<String> iterator(String... values) {
        List<String> list = Arrays.asList(values);
        Iterator<String> delegate = list.iterator();
        return new OADataSourceIterator<String>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String next() {
                return delegate.next();
            }
        };
    }
}
