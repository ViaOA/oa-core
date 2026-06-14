package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class OADataSourceEmptyIteratorTest {

    @Test
    void hasNextAlwaysReturnsFalse() {
        assertFalse(new OADataSourceEmptyIterator().hasNext());
    }

    @Test
    void nextReturnsNullForLegacyEmptyIteratorBehavior() {
        assertNull(new OADataSourceEmptyIterator().next());
    }

    @Test
    void getQueryReturnsNull() {
        assertNull(new OADataSourceEmptyIterator().getQuery());
    }

    @Test
    void getQuery2ReturnsNull() {
        assertNull(new OADataSourceEmptyIterator().getQuery2());
    }

    @Test
    void removeIsNoOp() {
        assertDoesNotThrow(() -> new OADataSourceEmptyIterator().remove());
    }

    @Test
    void forEachRemainingIsNoOp() {
        AtomicBoolean called = new AtomicBoolean();

        new OADataSourceEmptyIterator().forEachRemaining(obj -> called.set(true));

        assertFalse(called.get());
    }
}
