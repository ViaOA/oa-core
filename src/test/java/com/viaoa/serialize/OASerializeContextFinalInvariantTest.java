package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASerializeContextFinalInvariantTest {

    static class Item extends OAObject {
    }

    @Test
    void depthStateMustBeBalancedByCallerForOperationReuse() {
        OASerializeContext ctx = new OASerializeContext();

        ctx.pushDepth();
        ctx.pushDepth();

        assertEquals(2, ctx.getDepth());

        ctx.popDepth();
        ctx.popDepth();

        assertEquals(0, ctx.getDepth());
    }

    @Test
    void maxDepthCanBeChangedMidOperationAndIsImmediatelyReflected() {
        OASerializeContext ctx = new OASerializeContext();

        ctx.setMaxDepth(10);
        ctx.pushDepth();
        ctx.pushDepth();
        assertFalse(ctx.isMaxDepthReached());

        ctx.setMaxDepth(2);
        assertTrue(ctx.isMaxDepthReached());

        ctx.setMaxDepth(3);
        assertFalse(ctx.isMaxDepthReached());
    }

    @Test
    void writtenTrackingPersistsUntilNewContextCreatedCurrentContract() {
        OASerializeContext ctx = new OASerializeContext();
        Item item = new Item();

        ctx.markWritten(item);

        assertTrue(ctx.hasWritten(item));

        ctx.popDepth();
        ctx.setIncludeNulls(true);

        assertTrue(ctx.hasWritten(item));
    }

    @Test
    void flagsAreIndependentFromDepthAndVisitedTracking() {
        OASerializeContext ctx = new OASerializeContext();
        Item item = new Item();

        ctx.markWritten(item);
        ctx.pushDepth();

        ctx.setIncludeNulls(true);
        ctx.setIncludeCalculated(true);
        ctx.setIncludeTransient(true);
        ctx.setIncludeReferences(false);
        ctx.setWriteKeys(false);
        ctx.setWriteGuid(false);

        assertTrue(ctx.hasWritten(item));
        assertEquals(1, ctx.getDepth());

        ctx.setIncludeNulls(false);
        ctx.setIncludeCalculated(false);
        ctx.setIncludeTransient(false);
        ctx.setIncludeReferences(true);
        ctx.setWriteKeys(true);
        ctx.setWriteGuid(true);

        assertTrue(ctx.hasWritten(item));
        assertEquals(1, ctx.getDepth());
    }

/*qqqqqqqqq    
    @Test
    void identityMapDoesNotUseEqualsEvenIfSubclassOverridesEquals() {
        class EqualItem extends Item {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof EqualItem;
            }

            @Override
            public int hashCode() {
                return 1;
            }
        }

        OASerializeContext ctx = new OASerializeContext();
        EqualItem a = new EqualItem();
        EqualItem b = new EqualItem();

        ctx.markWritten(a);

        assertTrue(ctx.hasWritten(a));
        assertFalse(ctx.hasWritten(b));
    }
*/    
}
