package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;

class OASerializeContextTest {

    @Test
    void hasWrittenAndMarkWrittenUseObjectIdentity() {
        OASerializeContext context = new OASerializeContext();
        Item first = new Item(1);
        Item second = new Item(1);

        assertFalse(context.hasWritten(first));
        context.markWritten(first);

        assertTrue(context.hasWritten(first));
        assertFalse(context.hasWritten(second));
        context.markWritten(null);
        assertFalse(context.hasWritten(null));
    }

    @Test
    void includeNullsGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertFalse(context.getIncludeNulls());
        context.setIncludeNulls(true);
        assertTrue(context.getIncludeNulls());
    }

    @Test
    void includeCalculatedGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertFalse(context.getIncludeCalculated());
        context.setIncludeCalculated(true);
        assertTrue(context.getIncludeCalculated());
    }

    @Test
    void includeTransientGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertFalse(context.getIncludeTransient());
        context.setIncludeTransient(true);
        assertTrue(context.getIncludeTransient());
    }

    @Test
    void includeReferencesGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertTrue(context.getIncludeReferences());
        context.setIncludeReferences(false);
        assertFalse(context.getIncludeReferences());
    }

    @Test
    void writeKeysGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertTrue(context.getWriteKeys());
        context.setWriteKeys(false);
        assertFalse(context.getWriteKeys());
    }

    @Test
    void writeGuidGetterSetterRoundTrip() {
        OASerializeContext context = new OASerializeContext();

        assertTrue(context.getWriteGuid());
        context.setWriteGuid(false);
        assertFalse(context.getWriteGuid());
    }

    @Test
    void maxDepthDepthAndMaxReachedUseConfiguredLimit() {
        OASerializeContext context = new OASerializeContext();

        assertEquals(20, context.getMaxDepth());
        assertEquals(0, context.getDepth());
        assertFalse(context.isMaxDepthReached());

        context.setMaxDepth(2);
        context.pushDepth();
        assertEquals(1, context.getDepth());
        assertFalse(context.isMaxDepthReached());
        context.pushDepth();
        assertEquals(2, context.getDepth());
        assertTrue(context.isMaxDepthReached());

        context.popDepth();
        context.popDepth();
        context.popDepth();
        assertEquals(0, context.getDepth());

        context.setMaxDepth(-1);
        context.pushDepth();
        assertFalse(context.isMaxDepthReached());
    }
}
