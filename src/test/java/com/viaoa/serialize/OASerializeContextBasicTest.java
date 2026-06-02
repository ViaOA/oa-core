package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASerializeContextBasicTest {

    static class Item extends OAObject {
    }

    @Test
    void defaultFlagsMatchSerializationContract() {
        OASerializeContext ctx = new OASerializeContext();

        assertFalse(ctx.getIncludeNulls());
        assertFalse(ctx.getIncludeCalculated());
        assertFalse(ctx.getIncludeTransient());
        assertTrue(ctx.getIncludeReferences());
        assertTrue(ctx.getWriteKeys());
        assertTrue(ctx.getWriteGuid());
        assertEquals(20, ctx.getMaxDepth());
        assertEquals(0, ctx.getDepth());
        assertFalse(ctx.isMaxDepthReached());
    }

    @Test
    void includeFlagsRoundTripIndependently() {
        OASerializeContext ctx = new OASerializeContext();

        ctx.setIncludeNulls(true);
        ctx.setIncludeCalculated(true);
        ctx.setIncludeTransient(true);
        ctx.setIncludeReferences(false);
        ctx.setWriteKeys(false);
        ctx.setWriteGuid(false);

        assertTrue(ctx.getIncludeNulls());
        assertTrue(ctx.getIncludeCalculated());
        assertTrue(ctx.getIncludeTransient());
        assertFalse(ctx.getIncludeReferences());
        assertFalse(ctx.getWriteKeys());
        assertFalse(ctx.getWriteGuid());

        ctx.setIncludeNulls(false);
        ctx.setIncludeCalculated(false);
        ctx.setIncludeTransient(false);
        ctx.setIncludeReferences(true);
        ctx.setWriteKeys(true);
        ctx.setWriteGuid(true);

        assertFalse(ctx.getIncludeNulls());
        assertFalse(ctx.getIncludeCalculated());
        assertFalse(ctx.getIncludeTransient());
        assertTrue(ctx.getIncludeReferences());
        assertTrue(ctx.getWriteKeys());
        assertTrue(ctx.getWriteGuid());
    }

    @Test
    void visitedTrackingUsesObjectIdentityNotEquals() {
        OASerializeContext ctx = new OASerializeContext();

        Item a = new Item();
        Item same = a;
        Item b = new Item();

        assertFalse(ctx.hasWritten(a));
        assertFalse(ctx.hasWritten(b));

        ctx.markWritten(a);

        assertTrue(ctx.hasWritten(a));
        assertTrue(ctx.hasWritten(same));
        assertFalse(ctx.hasWritten(b));
    }

    @Test
    void markWrittenNullIsSafeNoop() {
        OASerializeContext ctx = new OASerializeContext();

        assertDoesNotThrow(() -> ctx.markWritten(null));
        assertFalse(ctx.hasWritten(null));
    }

    @Test
    void distinctObjectsRemainDistinctEvenIfSameClassAndState() {
        OASerializeContext ctx = new OASerializeContext();

        Item a = new Item();
        Item b = new Item();

        ctx.markWritten(a);

        assertTrue(ctx.hasWritten(a));
        assertFalse(ctx.hasWritten(b));
    }

    @Test
    void contextInstancesDoNotShareVisitedState() {
        Item item = new Item();

        OASerializeContext a = new OASerializeContext();
        OASerializeContext b = new OASerializeContext();

        a.markWritten(item);

        assertTrue(a.hasWritten(item));
        assertFalse(b.hasWritten(item));
    }

    @Test
    void depthPushPopCannotGoBelowZero() {
        OASerializeContext ctx = new OASerializeContext();

        assertEquals(0, ctx.getDepth());

        ctx.popDepth();
        ctx.popDepth();

        assertEquals(0, ctx.getDepth());

        ctx.pushDepth();
        ctx.pushDepth();

        assertEquals(2, ctx.getDepth());

        ctx.popDepth();

        assertEquals(1, ctx.getDepth());

        ctx.popDepth();
        ctx.popDepth();

        assertEquals(0, ctx.getDepth());
    }

    @Test
    void maxDepthReachedUsesGreaterThanOrEqualBoundary() {
        OASerializeContext ctx = new OASerializeContext();
        ctx.setMaxDepth(2);

        assertFalse(ctx.isMaxDepthReached());

        ctx.pushDepth();
        assertFalse(ctx.isMaxDepthReached());

        ctx.pushDepth();
        assertTrue(ctx.isMaxDepthReached());

        ctx.pushDepth();
        assertTrue(ctx.isMaxDepthReached());

        ctx.popDepth();
        assertTrue(ctx.isMaxDepthReached());

        ctx.popDepth();
        assertFalse(ctx.isMaxDepthReached());
    }

    @Test
    void negativeMaxDepthDisablesDepthLimit() {
        OASerializeContext ctx = new OASerializeContext();
        ctx.setMaxDepth(-1);

        for (int i = 0; i < 100; i++) {
            ctx.pushDepth();
        }

        assertFalse(ctx.isMaxDepthReached());
        assertEquals(100, ctx.getDepth());
    }

    @Test
    void zeroMaxDepthMeansReachedAtRootCurrentContract() {
        OASerializeContext ctx = new OASerializeContext();
        ctx.setMaxDepth(0);

        assertTrue(ctx.isMaxDepthReached());
    }
}
