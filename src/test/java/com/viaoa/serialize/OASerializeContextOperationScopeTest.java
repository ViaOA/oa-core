package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASerializeContextOperationScopeTest {

    static class Item extends OAObject {
    }

    @Test
    void operationScopeRequiresNewContextForCleanVisitedState() {
        Item item = new Item();

        OASerializeContext op1 = new OASerializeContext();
        op1.markWritten(item);

        OASerializeContext op2 = new OASerializeContext();

        assertTrue(op1.hasWritten(item));
        assertFalse(op2.hasWritten(item));
    }

    @Test
    void depthScopeRequiresBalancedPushPopBeforeReuse() {
        OASerializeContext ctx = new OASerializeContext();

        ctx.pushDepth();
        ctx.pushDepth();

        assertEquals(2, ctx.getDepth());

        ctx.popDepth();
        ctx.popDepth();

        assertEquals(0, ctx.getDepth());

        ctx.pushDepth();

        assertEquals(1, ctx.getDepth());
    }

    @Test
    void maxDepthNegativeDisablesEvenAfterDepthGrows() {
        OASerializeContext ctx = new OASerializeContext();
        ctx.setMaxDepth(-1);

        for (int i = 0; i < 1000; i++) {
            ctx.pushDepth();
        }

        assertFalse(ctx.isMaxDepthReached());
    }

    @Test
    void maxDepthZeroReachedBeforeAnyPushAndAfterPopToZero() {
        OASerializeContext ctx = new OASerializeContext();
        ctx.setMaxDepth(0);

        assertTrue(ctx.isMaxDepthReached());

        ctx.pushDepth();
        assertTrue(ctx.isMaxDepthReached());

        ctx.popDepth();
        assertTrue(ctx.isMaxDepthReached());
    }

    @Test
    void flagsCanBeUsedAsFormatPolicyWithoutAffectingVisitedObjects() {
        OASerializeContext ctx = new OASerializeContext();
        Item item = new Item();
        ctx.markWritten(item);

        ctx.setIncludeNulls(true);
        ctx.setIncludeCalculated(true);
        ctx.setIncludeTransient(true);
        ctx.setIncludeReferences(false);
        ctx.setWriteKeys(false);
        ctx.setWriteGuid(false);

        assertTrue(ctx.hasWritten(item));
        assertTrue(ctx.getIncludeNulls());
        assertTrue(ctx.getIncludeCalculated());
        assertTrue(ctx.getIncludeTransient());
        assertFalse(ctx.getIncludeReferences());
        assertFalse(ctx.getWriteKeys());
        assertFalse(ctx.getWriteGuid());
    }
}
